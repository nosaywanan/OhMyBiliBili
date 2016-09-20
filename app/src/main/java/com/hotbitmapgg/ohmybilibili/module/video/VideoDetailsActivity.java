package com.hotbitmapgg.ohmybilibili.module.video;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.flyco.tablayout.SlidingTabLayout;
import com.hotbitmapgg.ohmybilibili.R;
import com.hotbitmapgg.ohmybilibili.base.RxAppCompatBaseActivity;
import com.hotbitmapgg.ohmybilibili.entity.video.VideoDetails;
import com.hotbitmapgg.ohmybilibili.network.RetrofitHelper;
import com.hotbitmapgg.ohmybilibili.network.auxiliary.UrlHelper;
import com.hotbitmapgg.ohmybilibili.utils.LogUtil;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

/**
 * Created by hcc on 16/8/4 21:18
 * 100332338@qq.com
 * <p/>
 * 视频详情界面
 */
public class VideoDetailsActivity extends RxAppCompatBaseActivity
{

    @Bind(R.id.toolbar)
    Toolbar mToolbar;

    @Bind(R.id.collapsing_toolbar)
    CollapsingToolbarLayout mCollapsingToolbarLayout;

    @Bind(R.id.video_preview)
    ImageView mVideoPreview;

    @Bind(R.id.tab_layout)
    SlidingTabLayout mSlidingTabLayout;

    @Bind(R.id.view_pager)
    ViewPager mViewPager;

    @Bind(R.id.fab)
    FloatingActionButton mFAB;

    @Bind(R.id.app_bar_layout)
    AppBarLayout mAppBarLayout;
    @Bind(R.id.video_overlay)
    View mVideoOverlay;

    private List<Fragment> fragments = new ArrayList<>();

    private List<String> titles = new ArrayList<>();

    private static String EXTRA_AV = "extra_av";

    private int av;

    private VideoDetailsPagerAdapter mAdapter;

    private VideoDetails mVideoDetails;


    @Override
    public int getLayoutId()
    {

        return R.layout.activity_video_details;
    }

    @Override
    public void initViews(Bundle savedInstanceState)
    {

        Intent intent = getIntent();
        if (intent != null)
            av = intent.getIntExtra(EXTRA_AV, -1);

        getVideoInfo();

        mFAB.setClickable(false);
        mFAB.setBackgroundTintList(
                ColorStateList.valueOf(getResources().getColor(R.color.gray_20)));
        mFAB.setTranslationY(
                -getResources().getDimension(R.dimen.floating_action_button_size_half));
        mFAB.setOnClickListener(new View.OnClickListener()
        {

            @Override
            public void onClick(View v)
            {
                startFabAnimation();

            }
        });

        mAppBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener()
        {

            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset)
            {

                setViewsTranslation(verticalOffset);
            }
        });
    }

    private void startFabAnimation()
    {
        ObjectAnimator translateAnimator = ObjectAnimator.ofFloat(mFAB, "translationY",
                mFAB.getY() / 2);
        int centerX = (int) (mFAB.getX() + mFAB.getWidth() / 2);
        int centerY = (int) (mFAB.getY() / 2 + mFAB.getHeight() / 2);
        float startRadius = mFAB.getWidth() / 2;
        float endRadius = (float) Math.hypot(mVideoOverlay.getHeight(), mVideoOverlay.getWidth());

        Animator ripperAnimator = ViewAnimationUtils.createCircularReveal(mVideoOverlay, centerX,
                centerY, startRadius, endRadius);
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.setInterpolator(new AccelerateDecelerateInterpolator());
        animatorSet.playSequentially(translateAnimator, ripperAnimator);
        ripperAnimator.addListener(new Animator.AnimatorListener()
        {
            @Override
            public void onAnimationStart(Animator animation)
            {
                mFAB.setVisibility(View.GONE);
                mVideoOverlay.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
            }

            @Override
            public void onAnimationEnd(Animator animation)
            {
                mFAB.setVisibility(View.VISIBLE);
                mFAB.setTranslationX(0);
                mVideoOverlay.setBackgroundColor(
                        getResources().getColor(android.R.color.transparent));
                VideoPlayerActivity.launch(VideoDetailsActivity.this, av);
            }

            @Override
            public void onAnimationCancel(Animator animation)
            {

            }

            @Override
            public void onAnimationRepeat(Animator animation)
            {

            }
        });
    }

    @Override
    public void initToolBar()
    {

        setSupportActionBar(mToolbar);
        ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar != null)
            supportActionBar.setDisplayHomeAsUpEnabled(true);

        //设置还没收缩时状态下字体颜色
        mCollapsingToolbarLayout.setExpandedTitleColor(Color.TRANSPARENT);
        //设置收缩后Toolbar上字体的颜色
        mCollapsingToolbarLayout.setCollapsedTitleTextColor(Color.WHITE);

    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {

        if (item.getItemId() == android.R.id.home)
        {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }


    private void setViewsTranslation(int target)
    {

        mFAB.setTranslationY(target);
        if (target == 0)
        {
            showFAB();
        } else if (target < 0)
        {
            hideFAB();
        }
    }

    public static void launch(Activity activity, int aid)
    {

        Intent intent = new Intent(activity, VideoDetailsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(EXTRA_AV, aid);
        activity.startActivity(intent);
    }

    private void showFAB()
    {

        mFAB.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setInterpolator(new OvershootInterpolator())
                .start();
    }

    private void hideFAB()
    {

        mFAB.animate()
                .scaleX(0f)
                .scaleY(0f)
                .setInterpolator(new AccelerateInterpolator())
                .start();
    }


    public void getVideoInfo()
    {

        RetrofitHelper.getVideoDetailsApi()
                .getVideoDetails(av)
                .compose(this.<VideoDetails>bindToLifecycle())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<VideoDetails>()
                {

                    @Override
                    public void call(VideoDetails videoDetails)
                    {

                        mVideoDetails = videoDetails;
                        finishGetTask();
                    }
                }, new Action1<Throwable>()
                {

                    @Override
                    public void call(Throwable throwable)
                    {

                        mFAB.setClickable(false);
                        mFAB.setBackgroundTintList(
                                ColorStateList.valueOf(getResources().getColor(R.color.gray_20)));
                        LogUtil.all("获取视频详情失败" + throwable.getMessage());
                    }
                });
    }

    private void finishGetTask()
    {

        mFAB.setClickable(true);
        mFAB.setBackgroundTintList(ColorStateList.valueOf(getResources().
                getColor(R.color.colorPrimary)));
        mCollapsingToolbarLayout.setTitle(mVideoDetails.getTitle());

        Glide.clear(mVideoPreview);
        Glide.with(VideoDetailsActivity.this)
                .load(UrlHelper.getClearVideoPreviewUrl(mVideoDetails.getPic()))
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.bili_default_image_tv)
                .into(mVideoPreview);

        VideoInfoFragment mVideoInfoFragment = VideoInfoFragment
                .newInstance(mVideoDetails, av);
        VideoCommentFragment mVideoCommentFragment = VideoCommentFragment
                .newInstance(av);

        fragments.add(mVideoInfoFragment);
        fragments.add(mVideoCommentFragment);

        setPagerTitle(mVideoDetails.getVideo_review());
    }

    private void setPagerTitle(String num)
    {

        titles.add("简介");
        titles.add("评论" + "(" + num + ")");

        mAdapter = new VideoDetailsPagerAdapter(getSupportFragmentManager(), fragments, titles);

        mViewPager.setAdapter(mAdapter);
        mViewPager.setOffscreenPageLimit(2);
        mSlidingTabLayout.setViewPager(mViewPager);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        // TODO: add setContentView(...) invocation
        ButterKnife.bind(this);
    }

    public static class VideoDetailsPagerAdapter extends FragmentStatePagerAdapter
    {

        private List<Fragment> fragments;

        private List<String> titles;

        public VideoDetailsPagerAdapter(FragmentManager fm, List<Fragment> fragments,
                                        List<String> titles)
        {

            super(fm);
            this.fragments = fragments;
            this.titles = titles;
        }

        @Override
        public Fragment getItem(int position)
        {

            return fragments.get(position);
        }

        @Override
        public int getCount()
        {

            return fragments.size();
        }

        @Override
        public CharSequence getPageTitle(int position)
        {

            return titles.get(position);
        }
    }
}
