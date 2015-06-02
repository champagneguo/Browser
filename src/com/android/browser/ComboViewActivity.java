/*
 * Copyright (C) 2014 MediaTek Inc.
 * Modification based on code covered by the mentioned copyright
 * and/or permission notice(s).
 */
/*
 * Copyright (C) 2011 The Android Open Source Project
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

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.SearchView;
import android.view.ViewGroup;
import android.os.Handler;
import android.view.View;
import android.widget.TextView;
import android.provider.BrowserContract;
import java.util.Hashtable;
import java.lang.Integer;
import android.util.Log;

import com.android.browser.UI.ComboViews;

import java.util.ArrayList;

import com.ntian.nguiwidget.util.NTSystemBarTintActivity;

public class ComboViewActivity extends NTSystemBarTintActivity implements
		CombinedBookmarksCallbacks, ViewPager.OnPageChangeListener {

	private static final String STATE_SELECTED_TAB = "tab";
	public static final String EXTRA_COMBO_ARGS = "combo_args";
	public static final String EXTRA_INITIAL_VIEW = "initial_view";

	public static final String EXTRA_OPEN_SNAPSHOT = "snapshot_id";
	public static final String EXTRA_OPEN_TITLE = "snapshot_title";
	public static final String EXTRA_OPEN_URL = "snapshot_url";
	public static final String EXTRA_OPEN_ALL = "open_all";
	public static final String EXTRA_CURRENT_URL = "url";
	private ViewPager mViewPager;
	private TabsAdapter mTabsAdapter;

	private static final String LOGTAG = "browser";
	private TextView mBookmarkPageIndicator, mHistoryPageIndicator,
			mSnapshotPageIndicator;
	private static final int INDEX_BOOKMARK_PAGE = 0;
	private static final int INDEX_HISTORY_PAGE = 1;
	private static final int INDEX_SNAPSHOT_PAGE = 2;

	// bottom buttons
	private TextView mBtnNewBookmark, mBtnNewGroup, mBtnClearHistory;
	private View mGroupAndBookmarkBtnContainer;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setResult(RESULT_CANCELED);
		Bundle extras = getIntent().getExtras();
		Bundle args = extras.getBundle(EXTRA_COMBO_ARGS);
		String svStr = extras.getString(EXTRA_INITIAL_VIEW, null);
		ComboViews startingView = svStr != null ? ComboViews.valueOf(svStr)
				: ComboViews.Bookmarks;

		setContentView(R.layout.browser_combo_view);
		mBookmarkPageIndicator = (TextView) findViewById(R.id.page_indicator_bookmarks);
		mBookmarkPageIndicator.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				if (INDEX_BOOKMARK_PAGE != mViewPager.getCurrentItem()) {
					mViewPager.setCurrentItem(INDEX_BOOKMARK_PAGE, true);
				}
			}
		});
		mHistoryPageIndicator = (TextView) findViewById(R.id.page_indicator_history);
		mHistoryPageIndicator.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				if (INDEX_HISTORY_PAGE != mViewPager.getCurrentItem()) {
					mViewPager.setCurrentItem(INDEX_HISTORY_PAGE, true);
				}
			}
		});
		// mSnapshotPageIndicator = (TextView)findViewById(R.id.);

		mViewPager = (ViewPager) findViewById(R.id.tab_view);

		final ActionBar bar = getActionBar();
		bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		if (BrowserActivity.isTablet(this)) {
			bar.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME
					| ActionBar.DISPLAY_USE_LOGO);
			bar.setHomeButtonEnabled(true);
		} else {
			bar.setDisplayOptions(0);
		}

		mTabsAdapter = new TabsAdapter(this);
		mTabsAdapter.addTab(BrowserBookmarksPage.class, args);
		mTabsAdapter.addTab(BrowserHistoryPage.class, args);
		mViewPager.setAdapter(mTabsAdapter);
		mViewPager.setOnPageChangeListener(this);
		// mTabsAdapter.addTab(bar.newTab().setText(R.string.tab_snapshots),
		// BrowserSnapshotPage.class, args);

		int currentPage = INDEX_BOOKMARK_PAGE;
		if (savedInstanceState != null) {
			currentPage = savedInstanceState.getInt(STATE_SELECTED_TAB,
					INDEX_BOOKMARK_PAGE);
			bar.setSelectedNavigationItem(currentPage);
		} else {
			switch (startingView) {
			case Bookmarks:
				mViewPager.setCurrentItem(INDEX_BOOKMARK_PAGE);
				currentPage = INDEX_BOOKMARK_PAGE;
				break;
			case History:
				mViewPager.setCurrentItem(INDEX_HISTORY_PAGE);
				currentPage = INDEX_HISTORY_PAGE;
				break;
			case Snapshots:
				mViewPager.setCurrentItem(INDEX_SNAPSHOT_PAGE);
				currentPage = INDEX_SNAPSHOT_PAGE;
				break;
			}
		}

		mBtnNewBookmark = (TextView) findViewById(R.id.btn_new_bookmark);
		mBtnNewBookmark.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				Intent i = new Intent(ComboViewActivity.this,
						AddBookmarkPage.class);
				i.putExtra(BrowserContract.Bookmarks.URL, "");
				i.putExtra(BrowserContract.Bookmarks.TITLE, "");
				startActivity(i);
			}
		});

		mBtnNewGroup = (TextView) findViewById(R.id.btn_new_group);
		mBtnNewGroup.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {

			}
		});
		mBtnClearHistory = (TextView) findViewById(R.id.btn_clear_history);
		mBtnClearHistory.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				BrowserHistoryPage bhp = ((BrowserHistoryPage) mTabsAdapter
						.getFragmentAt(INDEX_HISTORY_PAGE));
				bhp.promptToClearHistory();
			}
		});
		mGroupAndBookmarkBtnContainer = findViewById(R.id.group_and_bookmark_btn_container);
		updateBottomBtnsVisibility(currentPage);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(STATE_SELECTED_TAB, getActionBar()
				.getSelectedNavigationIndex());
	}

	@Override
	public void openUrl(String url) {
		Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
		setResult(RESULT_OK, i);
		finish();
	}

	@Override
	public void openInNewTab(String... urls) {
		Intent i = new Intent();
		i.putExtra(EXTRA_OPEN_ALL, urls);
		setResult(RESULT_OK, i);
		finish();
	}

	@Override
	public void close() {
		finish();
	}

	@Override
	public void openSnapshot(long id, String title, String url) {
		// / M: add for save page @ {
		Intent i = new Intent(this, BrowserActivity.class);
		i.setAction(Intent.ACTION_VIEW);
		i.setData(Uri.parse(url));
		i.putExtra(EXTRA_OPEN_SNAPSHOT, id);
		i.putExtra(EXTRA_OPEN_TITLE, title);
		i.putExtra(EXTRA_OPEN_URL, url);
		setResult(RESULT_CANCELED);
		startActivity(i);
		// / @ }
		finish();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.combined, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			finish();
			return true;
		} else if (item.getItemId() == R.id.preferences_menu_id) {
			String url = getIntent().getStringExtra(EXTRA_CURRENT_URL);
			Intent intent = new Intent(this, BrowserPreferencesPage.class);
			intent.putExtra(BrowserPreferencesPage.CURRENT_PAGE, url);
			startActivityForResult(intent, Controller.PREFERENCES_PAGE);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onPageScrolled(int position, float positionOffset,
			int positionOffsetPixels) {
	}

	@Override
	public void onPageSelected(int position) {
		if (position == INDEX_BOOKMARK_PAGE) {
			mBookmarkPageIndicator
					.setBackgroundResource(R.drawable.ic_first_page_selected);
			mHistoryPageIndicator
					.setBackgroundResource(R.drawable.ic_last_page_unselected);
		} else if (position == INDEX_HISTORY_PAGE) {
			mHistoryPageIndicator
					.setBackgroundResource(R.drawable.ic_last_page_selected);
			mBookmarkPageIndicator
					.setBackgroundResource(R.drawable.ic_first_page_unselected);
		}
		updateBottomBtnsVisibility(position);
	}

	@Override
	public void onPageScrollStateChanged(int state) {
	}

	/**
	 * This is a helper class that implements the management of tabs and all
	 * details of connecting a ViewPager with associated TabHost. It relies on a
	 * trick. Normally a tab host has a simple API for supplying a View or
	 * Intent that each tab will show. This is not sufficient for switching
	 * between pages. So instead we make the content part of the tab host 0dp
	 * high (it is not shown) and the TabsAdapter supplies its own dummy view to
	 * show as the tab content. It listens to changes in tabs, and takes care of
	 * switch to the correct page in the ViewPager whenever the selected tab
	 * changes.
	 */
	public static class TabsAdapter extends FragmentPagerAdapter {
		private final Context mContext;
		private final ArrayList<TabInfo> mTabs = new ArrayList<TabInfo>();
		private final Hashtable<Integer, Fragment> mFragments = new Hashtable<Integer, Fragment>();

		static final class TabInfo {
			private final Class<?> clss;
			private final Bundle args;

			TabInfo(Class<?> _class, Bundle _args) {
				clss = _class;
				args = _args;
			}
		}

		public TabsAdapter(Activity activity) {
			super(activity.getFragmentManager());
			mContext = activity;
		}

		public void addTab(Class<?> clss, Bundle args) {
			TabInfo info = new TabInfo(clss, args);
			mTabs.add(info);
			notifyDataSetChanged();
		}

		@Override
		public int getCount() {
			return mTabs.size();
		}

		@Override
		public Fragment getItem(int position) {
			Integer key = new Integer(position);
			Fragment frag = mFragments.get(key);
			if (frag == null) {
				TabInfo info = mTabs.get(position);
				frag = Fragment.instantiate(mContext, info.clss.getName(),
						info.args);
				mFragments.put(key, frag);
			}
			return frag;
		}

		public Fragment getFragmentAt(int pos) {
			return mFragments.get(new Integer(pos));
		}
	}

	private static String makeFragmentName(int viewId, int index) {
		return "android:switcher:" + viewId + ":" + index;
	}

	private void updateBottomBtnsVisibility(int pageIndex) {
		switch (pageIndex) {
		case INDEX_BOOKMARK_PAGE:
			mGroupAndBookmarkBtnContainer.setVisibility(View.VISIBLE);
			mBtnClearHistory.setVisibility(View.GONE);
			break;
		case INDEX_HISTORY_PAGE:
			mGroupAndBookmarkBtnContainer.setVisibility(View.GONE);
			mBtnClearHistory.setVisibility(View.VISIBLE);
			BrowserHistoryPage bhp = ((BrowserHistoryPage) mTabsAdapter
					.getFragmentAt(INDEX_HISTORY_PAGE));
			if (bhp != null) {
				mBtnClearHistory.setEnabled(!bhp.isHistoryEmpty());
			}
			break;
		default:
			break;
		}
	}
}
