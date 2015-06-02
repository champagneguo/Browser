/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.browser;

import com.android.browser.UI.ComboViews;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.mediatek.xlog.Xlog;

/**
 * Base class for a side bar used by the browser.
 */
public class SideBar extends LinearLayout {

    private static final float ANIM_TITLEBAR_DECELERATE = 2.5f;
    private static final String TAG = "SideBar";
    private Activity mOwnerActivity;
    private UiController mUiController;
    private BaseUi mBaseUi;
    private FrameLayout mParentView;

    protected LinearLayout mSideBar;

    protected View mFakeDimLayer; // A view to simulate back window dim effect.
    //state
    private boolean mShowing;
    private Animator mSideBarAnimator;
    private int mParentViewHeight;

    protected TextView mItemAddBookmark, mItemBookmarkAndHistory, mItemShare, mItemSettings;
    protected boolean mAtLeftSide = false;
    protected boolean mCanShow = false;

    protected int mDefaultWidth = 0;

    protected TextView mItemClicked = null; // remember the button clicked so it can continue the action when hide animation ends

    public SideBar(Activity activity, UiController controller, BaseUi ui, FrameLayout parentView, boolean leftSide) {
        super(activity, null);
        mOwnerActivity = activity;
        mUiController = controller;
        mBaseUi = ui;
        mParentView = parentView;
        mAtLeftSide = leftSide;
        initLayout(activity);
        setupSideBar();
    }

    private void initLayout(Context context) {
        LayoutInflater factory = LayoutInflater.from(context);
        factory.inflate(R.layout.side_bar, this);
        mSideBar = (LinearLayout)findViewById(R.id.sidebar);

        mItemAddBookmark = (TextView)findViewById(R.id.sidebar_add_bookmark);
        mItemBookmarkAndHistory = (TextView)findViewById(R.id.sidebar_bookmark_history);
		mItemShare = (TextView)findViewById(R.id.sidebar_share);
        mItemSettings = (TextView)findViewById(R.id.sidebar_settings);

        mDefaultWidth = (int)context.getResources().getDimension(R.dimen.size_bar_width);

        mItemAddBookmark.setOnClickListener(new OnClickListener() {
            public void onClick(View arg0) {
                mItemClicked = mItemAddBookmark;
                hide(true);
            }
        });

        mItemBookmarkAndHistory.setOnClickListener(new OnClickListener() {
            public void onClick(View arg0) {
                mItemClicked = mItemBookmarkAndHistory;
                hide(true);
            }
        });
        /*mBtnBookmarkAndHistory.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View arg0) {
                Toast.makeText(mUiController.getActivity(),
                        mUiController.getActivity().getResources().getString(R.string.txt_btn_bookmark_history), 0).show();
                return false;
            }
        });*/
        mItemShare.setOnClickListener(new OnClickListener() {
            public void onClick(View arg0) {
                mItemClicked = mItemShare;
                hide(true);
            }
        });
        mItemSettings.setOnClickListener(new OnClickListener() {
            public void onClick(View arg0) {
                mItemClicked = mItemSettings;
                hide(true);
            }
        });

    }

    private void setupSideBar() {
        ViewGroup parent = (ViewGroup)getParent();
        //show();
        if (parent != null) {
            parent.removeView(this);
        }
        mParentView.addView(this, makeLayoutParams());
        setVisibility(View.GONE);
    }

    void setupSideBarAnimator(Animator animator) {
        Resources res = mOwnerActivity.getResources();
        int duration = res.getInteger(R.integer.titlebar_animation_duration);
        animator.setInterpolator(new DecelerateInterpolator(ANIM_TITLEBAR_DECELERATE));
        animator.setDuration(duration);
    }

    void show(boolean withAnim) {
        Xlog.i(TAG, "side bar show(), withAnim: " + withAnim);
        cancelSideBarAnimation();
        if (mCanShow && !mShowing){
            this.setVisibility(View.VISIBLE);
            int width = getSideBarWidth();
            if (width == 0) width = mDefaultWidth;
            float startPos = mAtLeftSide ? -width : mParentView.getWidth();
            float endPos = mAtLeftSide ? 0 : (mParentView.getWidth() - width);
            Xlog.i(TAG, "sidebar show(): startPos: " + startPos+ ", endPos: " + endPos);
            if (withAnim){
                this.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                mSideBarAnimator = ObjectAnimator.ofFloat(this, "translationX", startPos, endPos);
                mSideBarAnimator.addListener(mShowHideAnimatorListener);
                setupSideBarAnimator(mSideBarAnimator);
                mSideBarAnimator.start();
            }else{
                setTranslationX(endPos);
            }
            mShowing = true;
            mItemClicked = null;
        }
    }

    void hide(boolean withAnim) {
        Xlog.i(TAG, "side bar hide(), withAnim: " + withAnim);
        if (mShowing){
            this.setVisibility(View.VISIBLE);
            cancelSideBarAnimation();
            float startPos = getTranslationX();
            int endPos = mAtLeftSide ? -getSideBarWidth() : mParentView.getWidth();
            if (withAnim){
                this.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                mSideBarAnimator = ObjectAnimator.ofFloat(this, "translationX", startPos, endPos);
                mSideBarAnimator.addListener(mShowHideAnimatorListener);
                setupSideBarAnimator(mSideBarAnimator);
                mSideBarAnimator.start();
            }else{
                setTranslationX(endPos);
                if (mOwnerActivity instanceof NTStatusBarFakeDimActivity){
                    ((NTStatusBarFakeDimActivity)mOwnerActivity).setStatusBarFakeDim(false);
                }
                if (mFakeDimLayer != null) mFakeDimLayer.setVisibility(View.GONE);
            }
            mShowing = false;
        }
    }

    boolean isShowing() {
        return mShowing;
    }

    void cancelSideBarAnimation() {
        if (mSideBarAnimator != null) {
            mSideBarAnimator.cancel();
            mSideBarAnimator = null;
        }
    }

    private AnimatorListener mShowHideAnimatorListener = new AnimatorListener() {

        @Override
        public void onAnimationStart(Animator animation) {
        }

        @Override
        public void onAnimationRepeat(Animator animation) {
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            // show or hide fake dim effect
            if (mOwnerActivity instanceof NTStatusBarFakeDimActivity){
                ((NTStatusBarFakeDimActivity)mOwnerActivity).setStatusBarFakeDim(mShowing);
            }
            if (mFakeDimLayer != null) mFakeDimLayer.setVisibility(mShowing ? View.VISIBLE : View.GONE);
            // update position
            onScrollChanged();
            SideBar.this.setLayerType(View.LAYER_TYPE_NONE, null);

	        if (mItemClicked == mItemAddBookmark){
                ((Controller)mUiController).bookmarkCurrentPage();
            }else if (mItemClicked == mItemBookmarkAndHistory){
                mUiController.bookmarksOrHistoryPicker(ComboViews.Bookmarks);
            }else if (mItemClicked == mItemShare){
                mUiController.shareCurrentPage();
            }else if (mItemClicked == mItemSettings){
                ((Controller)mUiController).openPreferences();
            }
            mItemClicked = null;
            if (!mShowing) SideBar.this.setVisibility(View.GONE);
        }

        @Override
        public void onAnimationCancel(Animator animation) {
        }
    };

    private int getSideBarWidth() {
        return mSideBar.getWidth();
    }

    private ViewGroup.MarginLayoutParams makeLayoutParams() {
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
        params.gravity = Gravity.LEFT;
        return params;
    }

    public void onScrollChanged() {
        if (!mShowing) {
            setTranslationX(mAtLeftSide ? -getSideBarWidth():mParentView.getWidth());
        }
    }

    public void setFakeDimLayerView(View v) {
        mFakeDimLayer = v;
    }

    public void setShowable(boolean showable) {
        mCanShow = showable;
        if (!showable){
            hide(false);
            this.setVisibility(View.GONE);
        }
    }

    public void setCanShow(boolean showable) {
        mCanShow = showable;
        if (!showable){
            hide(false);
            this.setVisibility(View.GONE);
        }
    }

    public boolean getCanShow() {
        return mCanShow;
    }
}
