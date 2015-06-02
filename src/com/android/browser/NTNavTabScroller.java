/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.android.browser;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.content.res.Configuration;
import com.android.browser.view.ScrollerView;


import java.util.ArrayList;
import android.graphics.Point;

import android.util.Log;
/**
 * custom view for displaying tabs in the nav screen
 */
public class NTNavTabScroller extends ScrollerView {
    private static final String TAG = "NTNavTabScroller";
    static final int INVALID_POSITION = -1;
    static final float[] PULL_FACTOR = { 2.5f, 0.9f };

    interface OnRemoveListener {
        public void onRemovePosition(int position);
    }

    interface OnLayoutListener {
        public void onLayout(int l, int t, int r, int b);
    }

    interface OnPagesUpdatedListener {
        public void onUpdate(int totalPages, int currPage);
    }

    interface OnSnapDoneListener {
        public void onSnapDone();
    }

    //private ContentLayout mContentView;
    private FrameLayout mContentView;
    private BaseAdapter mAdapter;
    private OnRemoveListener mRemoveListener;
    private OnLayoutListener mLayoutListener;
    private OnPagesUpdatedListener mOnPagesUpdatedListener;
    //private int mGap;
    //private int mGapPosition;
    private int mOrientation;

    // after drag animation velocity in pixels/sec
    private static final float MIN_VELOCITY = 1500;
    private AnimatorSet mAnimator;

    private float mFlingVelocity;
    private boolean mNeedsScroll;
    private int mScrollPosition;

    DecelerateInterpolator mCubic;
    int mPullValue;

    private int mContentViewVisibleWidth;
    ArrayList<Point> mTabViewsTranslation = new ArrayList<Point>();
    public static final int[][] ITEM_POS_IN_PAGE_LANDSCAPE = {{48, 158}, {516, 158}, {984, 158}, {1452, 158}};
    public static final int[][] ITEM_POS_IN_PAGE_PORTRAIT = {{70, 130}, {560, 130}, {70, 854}, {560, 854}};

    private int mPageCount = 0;

    public NTNavTabScroller(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    public NTNavTabScroller(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public NTNavTabScroller(Context context) {
        super(context);
        init(context);
    }

    private void init(Context ctx) {
        mCubic = new DecelerateInterpolator(1.5f);
        //mGapPosition = INVALID_POSITION;
        setHorizontalScrollBarEnabled(false);
        setVerticalScrollBarEnabled(false);
        mContentView = new FrameLayout(ctx);
        addView(mContentView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        // ProGuard !
        //setGap(30/*getGap()*/);
        mFlingVelocity = getContext().getResources().getDisplayMetrics().density * MIN_VELOCITY;
        setFillViewport(true);
        mContentViewVisibleWidth = 0;
    }

    protected NavTabView getTabView(int pos) {
        return (NavTabView) mContentView.getChildAt(pos);
    }

    protected View getTabViewContainer() {
        return mContentView;
    }

    protected boolean isHorizontal() {
        return mOrientation == Configuration.ORIENTATION_LANDSCAPE;
    }

    public void setOrientation(int orientation) {
        super.setOrientation(LinearLayout.HORIZONTAL);
        mOrientation = orientation;
        updateTabViewsPos();
        
    }


    @Override
    protected void onMeasure(int wspec, int hspec) {
        super.onMeasure(wspec, hspec);

        if (mContentViewVisibleWidth == 0){
            mContentViewVisibleWidth = mContentView.getMeasuredWidth();
            updateTabViewsPos();
        }
        //calcPadding();
    }

    public void setAdapter(BaseAdapter adapter) {
        setAdapter(adapter, 0);
    }


    public void setOnRemoveListener(OnRemoveListener l) {
        mRemoveListener = l;
    }

    public void setOnLayoutListener(OnLayoutListener l) {
        mLayoutListener = l;
    }

    public void setOnPagesUpdatedListener(OnPagesUpdatedListener l) {
        mOnPagesUpdatedListener = l;
    }

    protected void setAdapter(BaseAdapter adapter, int selection) {
        mAdapter = adapter;
		mAdapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                handleDataChanged();
            }

            @Override
            public void onInvalidated() {
                super.onInvalidated();
            }
        });
        handleDataChanged(selection);
    }

    protected int getScrollValue() {
        return mScrollX;
    }

    protected void setScrollValue(int value) {
        scrollTo(value, mScrollY);
    }
    protected ViewGroup getContentView() {
        return mContentView;
    }

    protected int getRelativeChildTop(int ix) {
        return mContentView.getChildAt(ix).getTop() - mScrollY;
    }

    protected void handleDataChanged() {
        handleDataChanged(INVALID_POSITION);
    }

    public boolean isLandscapeMode(){
        return mOrientation == Configuration.ORIENTATION_LANDSCAPE;
    }
    private static final int NAV_PAGE_PADDING = 30; //pixels
    private static final int TAB_VIEW_COUNT_PER_PAGE = 4;

    void handleDataChanged(int newscroll) {
        int scroll = getScrollX();

        mContentView.removeAllViews();

        for (int i = 0; i < mAdapter.getCount(); i++) {
            View v = mAdapter.getView(i, null, mContentView);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            lp.gravity = (Gravity.LEFT|Gravity.TOP);
            mContentView.addView(v, lp);
        }
        updateTabViewsPos();
        if (newscroll > INVALID_POSITION) {
            newscroll = Math.min(mAdapter.getCount() - 1, newscroll);
            mNeedsScroll = true;
            mScrollPosition = newscroll;
            requestLayout();
        } else {
            setScrollX(scroll);
        }
    }

    synchronized private void updateTabViewsPos(){
        if (mContentViewVisibleWidth == 0) return;
        int[][] posArray = isLandscapeMode() ? ITEM_POS_IN_PAGE_LANDSCAPE : ITEM_POS_IN_PAGE_PORTRAIT;
        //int pageWidth = isLandscapeMode() ? 1920 : 1080;
        int pageWidth = mContentViewVisibleWidth - mContentView.getPaddingLeft() - mContentView.getPaddingRight();
        int itemPerPage = posArray.length;
        int itemCount = mContentView.getChildCount();

        mPageCount = itemCount/itemPerPage;
        if ((itemCount%itemPerPage) > 0) mPageCount++;


        int width = mPageCount * pageWidth + mContentView.getPaddingLeft() + mContentView.getPaddingLeft();
        mContentView.setLayoutParams(new FrameLayout.LayoutParams(width, ViewGroup.LayoutParams.MATCH_PARENT));
        mContentView.setMinimumWidth(width);
		mTabViewsTranslation.clear();
        for (int i = 0; i < itemCount; i++) {
            View v = mContentView.getChildAt(i);
            int currPage = i / itemPerPage;
            int indexInPage = i % itemPerPage;
            int posX = posArray[indexInPage][0] + (pageWidth * currPage);
            int posY = posArray[indexInPage][1];
            v.setTranslationX(posX);
            v.setTranslationY(posY);
            mTabViewsTranslation.add(new Point(posX, posY));
        }

        if (mOnPagesUpdatedListener != null){
            mOnPagesUpdatedListener.onUpdate(mPageCount, 0);
        }

        requestLayout();
    }

    protected void finishScroller() {
        mScroller.forceFinished(true);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (mNeedsScroll) {
            mScroller.forceFinished(true);
            snapToSelected(mScrollPosition, false);
            mNeedsScroll = false;
        }
        if (mLayoutListener != null) {
            mLayoutListener.onLayout(l, t, r, b);
            mLayoutListener = null;
        }
    }

    void clearTabs() {
        mContentView.removeAllViews();
        mTabViewsTranslation.clear();
    }

    void snapToSelected(int pos, boolean smooth) {
        if (pos < 0) return;
        View v = mContentView.getChildAt(pos);
        if (v == null) return;
        int sx = 0;
        int sy = 0;
        Point pt = mTabViewsTranslation.get(pos);
        if (pt == null){
            return;
        }

        int pageWidth = getMeasuredWidth() - getPaddingLeft() - getPaddingRight();
        int pageIndex = pt.x/pageWidth;
        sx = pageWidth * pageIndex;

        if ((sx != mScrollX) || (sy != mScrollY)) {
            if (smooth) {
                smoothScrollTo(sx,sy);
            } else {
                scrollTo(sx, sy);
            }
        }
        if (mPageChangedListener != null){
            mPageChangedListener.onPageChanged(pageIndex);
        }
    }

    int calculateScrollXForTabViewPage(int pos) {
        Point pt = mTabViewsTranslation.get(pos);
        if (pt == null){
            return mScrollX;
        }

        int pageWidth = getMeasuredWidth() - getPaddingLeft() - getPaddingRight();
        int pageIndex = pt.x/pageWidth;
        return (pageWidth * pageIndex);
    }

    protected void animateOut(View v) {
        if (v == null) return;
        animateOut(v, -mFlingVelocity);
    }

    private void animateOut(final View v, float velocity) {
        float start = v.getTranslationY(); //isLandscapeMode() ? v.getTranslationY() : v.getTranslationX();
        animateOut(v, velocity, start);
    }

    private void animateOut(final View v, float velocity, float start) {
        if ((v == null) || (mAnimator != null)) return;
        final int position = mContentView.indexOfChild(v);
        int target = 0;
        Point pt = null;
        if (position >= 0 && position < mTabViewsTranslation.size()) pt = mTabViewsTranslation.get(position);
        int originalY = (pt == null ? 0 : pt.y);
        if (velocity < 0) {
            target = originalY - getHeight();
        } else {
            target = originalY + getHeight();
        }
        int distance = target - v.getTop();
        long duration = (long) (Math.abs(distance) * 1000 / Math.abs(velocity));

        /*
        int scroll = 0;
        int translate = 0;
        int gap = v.getWidth();
        int centerView = getViewCenter(v);
        int centerScreen = getScreenCenter();
        int newpos = INVALID_POSITION;
        if (centerView < centerScreen - gap / 2) {
            // top view
            scroll = - (centerScreen - centerView - gap);
            translate = (position > 0) ? gap : 0;
            newpos = position;
        } else if (centerView > centerScreen + gap / 2) {
            // bottom view
            scroll = - (centerScreen + gap - centerView);
            if (position < mAdapter.getCount() - 1) {
                translate = -gap;
            }
        } else {
            // center view
            scroll = - (centerScreen - centerView);
            if (position < mAdapter.getCount() - 1) {
                translate = -gap;
            } else {
                scroll -= gap;
            }
        }*/
        //mGapPosition = position;
        final int pos = INVALID_POSITION; //newpos;
        ObjectAnimator trans = ObjectAnimator.ofFloat(v, TRANSLATION_Y, start, target);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(v, ALPHA, getAlpha(v,start), getAlpha(v,target));
        AnimatorSet set1 = new AnimatorSet();
        set1.playTogether(trans, alpha);
        set1.setDuration(duration);
        mAnimator = new AnimatorSet();

        ArrayList<Animator> animCollection = new ArrayList<Animator>();
        for (int i = position + 1; i < mTabViewsTranslation.size(); i++) {
            View view = mContentView.getChildAt(i);
            Point ptStart = mTabViewsTranslation.get(i);
            Point ptEnd = mTabViewsTranslation.get(i - 1);

            ObjectAnimator transX = ObjectAnimator.ofFloat(view, TRANSLATION_X, ptStart.x, ptEnd.x);
            ObjectAnimator transY = ObjectAnimator.ofFloat(view, TRANSLATION_Y, ptStart.y, ptEnd.y);
            animCollection.add(transX);
            animCollection.add(transY);
        }
        AnimatorSet set3 = new AnimatorSet();
        set3.playTogether(animCollection);
        set3.setDuration(200);

        /*ObjectAnimator trans2 = null;
        ObjectAnimator scroll1 = null;
        if (scroll != 0) {
            //if (isLandscapeMode()) {
                scroll1 = ObjectAnimator.ofInt(this, "scrollX", getScrollX(), getScrollX() + scroll);
            //} else {
            //    scroll1 = ObjectAnimator.ofInt(this, "scrollY", getScrollY(), getScrollY() + scroll);
            //}
        }
        if (translate != 0) {
            trans2 = ObjectAnimator.ofInt(this, "gap", 0, translate);
        }

        final int duration2 = 200;
        Animator set2 = null;
        if (scroll1 != null) {
            if (trans2 != null) {
                AnimatorSet set = new AnimatorSet();
                set.playTogether(scroll1, trans2);
                set.setDuration(duration2);
                //mAnimator.playSequentially(set1, set);
                set2 = set;
            } else {
                scroll1.setDuration(duration2);
                //mAnimator.playSequentially(set1, scroll1);
                set2 = scroll1;
            }
        } else {
            if (trans2 != null) {
                trans2.setDuration(duration2);
                //mAnimator.playSequentially(set1, trans2);\
                set2 = trans2;
            }
        }*/

        //if (set2 != null) mAnimator.playSequentially(set1, set3, set2);
        //else
			mAnimator.playSequentially(set1, set3);

        mAnimator.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator a) {
                if (mRemoveListener !=  null) {
                    mRemoveListener.onRemovePosition(position);
                    mAnimator = null;
                    //mGapPosition = INVALID_POSITION;
                    //mGap = 0;
                    handleDataChanged(pos);
                }
            }
        });
        mAnimator.start();
    }

    /*
    public void setGap(int gap) {
        if (mGapPosition != INVALID_POSITION) {
            mGap = gap;
            postInvalidate();
        }
    }

    public int getGap() {
        return mGap;
    }

    void adjustGap() {
        for (int i = 0; i < mContentView.getChildCount(); i++) {
            final View child = mContentView.getChildAt(i);
            adjustViewGap(child, i);
        }
    }

    private void adjustViewGap(View view, int pos) {
        if ((mGap < 0 && pos > mGapPosition)
                || (mGap > 0 && pos < mGapPosition)) {
            if (isLandscapeMode()) {
                view.setTranslationX(mGap);
            } else {
                view.setTranslationY(mGap);
            }
        }
    }
    //*/

    private int getViewCenter(View v) {
        //if (isLandscapeMode()) {
            return v.getLeft() + v.getWidth() / 2;
        //} else {
        //    return v.getTop() + v.getHeight() / 2;
        //}
    }

    private int getScreenCenter() {
        //if (isLandscapeMode()) {
            return getScrollX() + getWidth() / 2;
        //} else {
        //    return getScrollY() + getHeight() / 2;
        //}
    }

    //*

    protected View findViewAt(int x, int y) {
        x += mScrollX;
        y += mScrollY;
        final int count = mContentView.getChildCount();
        for (int i = count - 1; i >= 0; i--) {
            View child = mContentView.getChildAt(i);
            if (child.getVisibility() == View.VISIBLE) {
                if ((x >= child.getTranslationX()) && (x < child.getTranslationX() + child.getWidth())
                        && (y >= child.getTranslationY()) && (y < child.getTranslationY() + child.getHeight())) {
                    return child;
                }
            }
        }
        return null;
    }

    @Override
    protected void onOrthoDrag(View v, float distance) {
        if ((v != null) && (mAnimator == null)) {
            offsetView(v, distance);
        }
    }

    @Override
    protected void onOrthoDragFinished(View downView) {
        if (mAnimator != null) return;
        if (mIsOrthoDragged && downView != null) {
            // offset
            int index = mContentView.indexOfChild(downView);
            Point pt = null;
            if (index >= 0 && index < mTabViewsTranslation.size()) pt = mTabViewsTranslation.get(index);
            float originalY = (pt == null ? 0 : pt.y);
            float diff = downView.getTranslationY() - originalY; //isLandscapeMode() ? downView.getTranslationY() : downView.getTranslationX();
            if (Math.abs(diff) > (downView.getHeight() / 2)) {
                // remove it
                animateOut(downView, Math.signum(diff) * mFlingVelocity, downView.getTranslationY());
            } else {
                // snap back
                offsetView(downView, 0);
            }
        }
    }

    @Override
    protected void onOrthoFling(View v, float velocity) {
        if (v == null) return;
        if (mAnimator == null && Math.abs(velocity) > mFlingVelocity / 2) {
            animateOut(v, velocity);
        } else {
            offsetView(v, 0);
        }
    }

    private void offsetView(View v, float distance) {
        v.setAlpha(getAlpha(v, distance));
        int index = mContentView.indexOfChild(v);
        Point pt = null;
        if (index >= 0 && index < mTabViewsTranslation.size()) pt = mTabViewsTranslation.get(index);
        float newPosY = distance + (pt == null ? 0 : pt.y);
        //if (isLandscapeMode()) {
            v.setTranslationY(newPosY);
        //} else {
        //    v.setTranslationX(distance);
        //}
    }

    private float getAlpha(View v, float distance) {
        return 1 - (float) Math.abs(distance) / v.getHeight();
    }

    private float ease(DecelerateInterpolator inter, float value, float start,
            float dist, float duration) {
        return start + dist * inter.getInterpolation(value / duration);
    }

    /*
    @Override
    protected void onPull(int delta) {
        boolean layer = false;
        int count = 2;
        if (delta == 0 && mPullValue == 0) return;
        if (delta == 0 && mPullValue != 0) {
            // reset
            for (int i = 0; i < count; i++) {
                View child = mContentView.getChildAt((mPullValue < 0)
                        ? i
                        : mContentView.getChildCount() - 1 - i);
                if (child == null) break;
                ObjectAnimator trans = ObjectAnimator.ofFloat(child,
                                "translationX",
                                getTranslationX(),
                                0);
                ObjectAnimator rot = ObjectAnimator.ofFloat(child,
                                "rotationY",
                                getRotationY(),
                                0);
                AnimatorSet set = new AnimatorSet();
                set.playTogether(trans, rot);
                set.setDuration(100);
                set.start();
            }
            mPullValue = 0;
        } else {
            if (mPullValue == 0) {
                layer = true;
            }
            mPullValue += delta;
        }
        final int height = getWidth();
        int oscroll = Math.abs(mPullValue);
        int factor = (mPullValue <= 0) ? 1 : -1;
        for (int i = 0; i < count; i++) {
            View child = mContentView.getChildAt((mPullValue < 0)
                    ? i
                    : mContentView.getChildCount() - 1 - i);
            if (child == null) break;
            if (layer) {
            }
            float k = PULL_FACTOR[i];
            float rot = -factor * ease(mCubic, oscroll, 0, k * 2, height);
            int y =  factor * (int) ease(mCubic, oscroll, 0, k*20, height);
            //if (isLandscapeMode()) {
                child.setTranslationX(y);
            //} else {
            //    child.setTranslationY(y);
            //}
            //if (isLandscapeMode()) {
                child.setRotationY(-rot);
            //} else {
            //    child.setRotationX(rot);
            //}
        }
    }*/

    public int getPageCount(){
        return mPageCount;
    }

    public int getItemCountPerPage(){
        return isLandscapeMode() ? ITEM_POS_IN_PAGE_LANDSCAPE.length : ITEM_POS_IN_PAGE_PORTRAIT.length;
    }
}