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
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.mediatek.xlog.Xlog;

/**
 * Base class for a bottom bar used by the browser.
 */
public class BottomBar extends LinearLayout {

    private static final float ANIM_TITLEBAR_DECELERATE = 2.5f;
    private static final String TAG = "BottomBar";
    private Activity mOwnerActivity;
    private UiController mUiController;
    private BaseUi mBaseUi;
    private FrameLayout mParentView;
    //private TabControl mTabControl;

    private boolean mUseQuickControls;
    private boolean mUseFullScreen;
    protected LinearLayout mBottomBar;
    protected ImageView mBottomBarBack;
    protected ImageView mBottomBarForward;
    protected ImageView mBottomBarNewTab;
    protected ImageView mBottomBarTabs;
    protected TextView mBottomBarTabCount;

    protected View mFakeDimLayer; // A view to simulate back window dim effect.
    //state
    private boolean mShowing;
    private boolean mExpanded;
    private Animator mBottomBarAnimator;
    private int mParentViewHeight;

    private View mBtnContainer1, mBtnContainer2;
    protected TextView mBtnAddBookmark, mBtnBookmarkAndHistory, mBtnShare, mBtnSettings;
    protected TextView mBtnClicked = null; // remember the button clicked so it can continue the action when showMore(false) animation ends

    private boolean mCanShowMore = true;

    public BottomBar(Activity activity, UiController controller, BaseUi ui, final TabControl tabControl,
            FrameLayout parentView) {
        super(activity, null);
        mOwnerActivity = activity;
        mUiController = controller;
        mBaseUi = ui;
        //mTabControl = tabControl;
        mParentView = parentView;
        initLayout(activity);
        setupBottomBar();
    }

    private void initLayout(Context context) {
        LayoutInflater factory = LayoutInflater.from(context);
        factory.inflate(R.layout.bottom_bar, this);
        mBottomBar = (LinearLayout) findViewById(
                R.id.bottombar);
        mBottomBarBack = (ImageView) findViewById(
                R.id.back);
        mBottomBarForward = (ImageView) findViewById(
                R.id.forward);
        mBottomBarNewTab = (ImageView)findViewById(R.id.new_tab);
        mBottomBarTabs = (ImageView) findViewById(
                R.id.tabs);

        mBottomBarTabCount = (TextView) findViewById(
                R.id.tabcount);

        mBtnContainer1 = findViewById(R.id.bottombar_container_1);
        mBtnContainer2 = findViewById(R.id.bottombar_container_2);

        mBtnAddBookmark = (TextView)findViewById(R.id.btn_add_bookmark);
        mBtnBookmarkAndHistory = (TextView)findViewById(R.id.btn_bookmark_history);
		mBtnShare = (TextView)findViewById(R.id.btn_share);
        mBtnSettings = (TextView)findViewById(R.id.btn_settings);

        mBottomBarBack.setOnClickListener(new OnClickListener() {
            public void onClick(View arg0) {
                ((Controller) mUiController).onBackKey();
            }
        });
        mBottomBarBack.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View arg0) {
                Toast.makeText(mUiController.getActivity(),
                        mUiController.getActivity().getResources()
                        .getString(R.string.back), 0).show();
                return false;
            }
        });
        mBottomBarForward.setOnClickListener(new OnClickListener() {
            public void onClick(View arg0) {
                if (mUiController != null && mUiController.getCurrentTab() != null) {
                    mUiController.getCurrentTab().goForward();
                }
            }
        });
        mBottomBarForward.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View arg0) {
                Toast.makeText(mUiController.getActivity(),
                        mUiController.getActivity().getResources().getString(R.string.forward), 0).show();
                return false;
            }
        });

        mBottomBarNewTab.setOnClickListener(new OnClickListener() {
            public void onClick(View arg0) {
                mUiController.openTabToHomePage();
            }
        });
        mBottomBarNewTab.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View arg0) {
                Toast.makeText(mUiController.getActivity(),
                        mUiController.getActivity().getResources().getString(R.string.accessibility_button_newtab), 0).show();
                return false;
            }
        });

        mBottomBarTabs.setOnClickListener(new OnClickListener() {
            public void onClick(View arg0) {
                ((PhoneUi) mBaseUi).toggleNavScreen();
            }
        });
        mBottomBarTabs.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View arg0) {
                Toast.makeText(mUiController.getActivity(),
                        mUiController.getActivity().getResources().getString(R.string.tabs), 0).show();
                return false;
            }
        });

        tabCountChanged(mUiController.getTabControl().getTabCount());

        mBtnAddBookmark.setOnClickListener(new OnClickListener() {
            public void onClick(View arg0) {
                showMore(false);
                mBtnClicked = mBtnAddBookmark;
            }
        });

        mBtnBookmarkAndHistory.setOnClickListener(new OnClickListener() {
            public void onClick(View arg0) {
                showMore(false);
                mBtnClicked = mBtnBookmarkAndHistory;
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
        mBtnShare.setOnClickListener(new OnClickListener() {
            public void onClick(View arg0) {
                showMore(false);
                mBtnClicked = mBtnShare;
            }
        });
        mBtnSettings.setOnClickListener(new OnClickListener() {
            public void onClick(View arg0) {
                showMore(false);
                mBtnClicked = mBtnSettings;
            }
        });

    }

    private void setupBottomBar() {
        ViewGroup parent = (ViewGroup) getParent();
        show();
        if (parent != null) {
            parent.removeView(this);
        }
        mParentView.addView(this, makeLayoutParams());
        mBaseUi.setContentViewMarginBottom(0);
    }

    public void setFullScreen(boolean use) {
        mUseFullScreen = use;
        if (use) {
            this.setVisibility(View.GONE);
        } else {
            this.setVisibility(View.VISIBLE);
        }
    }

    public void setUseQuickControls(boolean use) {
        mUseQuickControls = use;
        if (use) {
            this.setVisibility(View.GONE);
        } else {
            this.setVisibility(View.VISIBLE);
        }
    }

    void setupBottomBarAnimator(Animator animator) {
        Resources res = mOwnerActivity.getResources();
        int duration = res.getInteger(R.integer.titlebar_animation_duration);
        animator.setInterpolator(new DecelerateInterpolator(
                ANIM_TITLEBAR_DECELERATE));
        animator.setDuration(duration);
    }

    void show() {
        //Xlog.i(TAG, "bottom bar show(), showing: " + mShowing + "IME: " + mIMEShow);
        cancelBottomBarAnimation();
        if (mUseQuickControls) {
            this.setVisibility(View.GONE);
            mShowing = false;
            return;
        } else if (!mShowing/* && !mIMEShow*/) {
            this.setVisibility(View.VISIBLE);
            float startPos = getTranslationY();
            float endPos = mBtnContainer2.getHeight();
            // it may be 0 when not measured, then we get it from R.dimen
            if (endPos == 0) endPos = mOwnerActivity.getResources().getDimension(R.dimen.bottom_bar_settings_container_height);
            Xlog.i(TAG, "bottombar show(): startPos: " + startPos+ ", endPos: " + endPos);
            this.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            mBottomBarAnimator = ObjectAnimator.ofFloat(this,
                    "translationY", startPos, endPos);
            setupBottomBarAnimator(mBottomBarAnimator);
            mBottomBarAnimator.start();
            mShowing = true;
        }
    }

    void hide() {
        //Xlog.i(TAG, "bottom bar hide(), showing: " + mShowing);
        if (mUseQuickControls || mUseFullScreen) {
            cancelBottomBarAnimation();
            float startPos = getTranslationY();
            int endPos = getBottomBarHeight();
            this.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            mBottomBarAnimator = ObjectAnimator.ofFloat(this, "translationY", startPos, endPos);
            mBottomBarAnimator.addListener(mHideBottomBarAnimatorListener);
            setupBottomBarAnimator(mBottomBarAnimator);
            mBottomBarAnimator.start();
            this.setVisibility(View.GONE);
            mShowing = false;
            return;
        } else {
            this.setVisibility(View.VISIBLE);
            cancelBottomBarAnimation();
            float startPos = getTranslationY();
            int endPos = getBottomBarHeight();
            this.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            mBottomBarAnimator = ObjectAnimator.ofFloat(this, "translationY", startPos, endPos);
            mBottomBarAnimator.addListener(mHideBottomBarAnimatorListener);
            setupBottomBarAnimator(mBottomBarAnimator);
            mBottomBarAnimator.start();
        }
        mShowing = false;
    }

    boolean isShowing() {
        return mShowing;
    }

    // expand the bottom bar to show more buttons including settings button
    void showMore(boolean expand) {
        if (!mShowing || (!mCanShowMore && expand)) return;

        cancelBottomBarAnimation();
        float startPos = getTranslationY();
        if (expand && !mExpanded){
            float endPos = 0;
            this.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            mBottomBarAnimator = ObjectAnimator.ofFloat(this, "translationY", startPos, endPos);
            setupBottomBarAnimator(mBottomBarAnimator);
            mBottomBarAnimator.addListener(mExpandAnimatorListener);
            mBottomBarAnimator.start();
            mExpanded = true;
        }else if (!expand && mExpanded){
            float endPos = mBtnContainer2.getHeight();
            this.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            mBottomBarAnimator = ObjectAnimator.ofFloat(this, "translationY", startPos, endPos);
            setupBottomBarAnimator(mBottomBarAnimator);
            mBottomBarAnimator.addListener(mExpandAnimatorListener);
            mBottomBarAnimator.start();
            mExpanded = false;
        }
    }

    boolean isExpanded() {
        return mExpanded;
    }

    void cancelBottomBarAnimation() {
        if (mBottomBarAnimator != null) {
            mBottomBarAnimator.cancel();
            mBottomBarAnimator = null;
            mBtnClicked = null;
        }
    }

    private AnimatorListener mHideBottomBarAnimatorListener = new AnimatorListener() {

        @Override
        public void onAnimationStart(Animator animation) {
        }

        @Override
        public void onAnimationRepeat(Animator animation) {
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            // update position
            onScrollChanged();
            BottomBar.this.setLayerType(View.LAYER_TYPE_NONE, null);
            if (mOwnerActivity instanceof NTStatusBarFakeDimActivity){
                ((NTStatusBarFakeDimActivity)mOwnerActivity).setStatusBarFakeDim(false);
            }
            if (mFakeDimLayer != null) mFakeDimLayer.setVisibility(View.GONE);
        }

        @Override
        public void onAnimationCancel(Animator animation) {
        }
    };

	private AnimatorListener mExpandAnimatorListener = new AnimatorListener() {
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
                ((NTStatusBarFakeDimActivity)mOwnerActivity).setStatusBarFakeDim(mExpanded);
            }
            if (mFakeDimLayer != null) mFakeDimLayer.setVisibility(mExpanded ? View.VISIBLE : View.GONE);
            if (!mExpanded){
                // Bottom bar collapsed, check if it is caused by a button click.
                if (mBtnClicked == mBtnAddBookmark){
                    ((Controller)mUiController).bookmarkCurrentPage();
                }else if (mBtnClicked == mBtnBookmarkAndHistory){
                    mUiController.bookmarksOrHistoryPicker(ComboViews.Bookmarks);
                }else if (mBtnClicked == mBtnShare){
                    mUiController.shareCurrentPage();
                }else if (mBtnClicked == mBtnSettings){
                    ((Controller)mUiController).openPreferences();
                }
            }
            mBtnClicked = null;
        }

        @Override
        public void onAnimationCancel(Animator animation) {
        }
    };

    private int getBottomBarHeight() {
        return mBottomBar.getHeight();
    }

    public boolean useQuickControls() {
        return mUseQuickControls;
    }

    private ViewGroup.MarginLayoutParams makeLayoutParams() {
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.BOTTOM;
        return params;
    }

    public void onScrollChanged() {
        if (!mShowing) {
            setTranslationY(getBottomBarHeight());
        }
    }

    public void changeBottomBarState(boolean back, boolean forward) {
        mBottomBarBack.setEnabled(back);
        mBottomBarForward.setEnabled(forward);
    }

    public void setFakeDimLayerView(View v) {
        mFakeDimLayer = v;
    }

    public void setCanShowMore(boolean enable) {
        mCanShowMore = enable;
        if (!mCanShowMore) showMore(false);
    }

	public void tabCountChanged(int tabCount) {
        if (mBottomBarTabCount != null) mBottomBarTabCount.setText(Integer.toString(tabCount));
    }

    @Override
    protected void onConfigurationChanged(Configuration config) {
        super.onConfigurationChanged(config);
        if (config.orientation == Configuration.ORIENTATION_LANDSCAPE){
            cancelBottomBarAnimation();
            setTranslationY((float)getBottomBarHeight());
            this.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            setVisibility(View.GONE);
            mShowing = false;
        }else{
            show();
        }
    }
}
