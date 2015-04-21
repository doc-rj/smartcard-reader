/*
 * Copyright 2015 Ryan Jones
 *
 * This file is part of smartcard-reader, package org.docrj.smartcard.reader.
 *
 * smartcard-reader is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * smartcard-reader is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with smartcard-reader. If not, see <http://www.gnu.org/licenses/>.
 */

package org.docrj.smartcard.reader;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;

public class NavDrawer {
    private Activity mActivity;
    private int mResId;
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private View mParentItem;
    private View mCurrentItem;

    private final Handler mHandler = new Handler();

    public NavDrawer(Activity activity, Bundle inState,
                     int resId, DrawerLayout drawerLayout, Toolbar toolbar) {
        mActivity = activity;
        mResId = resId;
        mDrawerLayout = drawerLayout;

        // this only takes effect on Lollipop, or when using translucentStatusBar on Kitkat
        drawerLayout.setStatusBarBackgroundColor(activity.getResources().getColor(R.color.primary));
        mParentItem = drawerLayout.findViewById(resId);
        mParentItem.setActivated(true);

        mDrawerToggle = new ActionBarDrawerToggle(activity, drawerLayout,
                toolbar, R.string.app_name, R.string.app_name) {

            /** called when a drawer has settled in a completely closed state */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                // force call to onPrepareOptionsMenu()
                mActivity.invalidateOptionsMenu();
            }

            /** called when a drawer has settled in a completely open state */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                // force call to onPrepareOptionsMenu()
                mActivity.invalidateOptionsMenu();
            }
        };
        drawerLayout.setDrawerListener(mDrawerToggle);

        View appSelect = drawerLayout.findViewById(R.id.app_select);
        View batchSelect = drawerLayout.findViewById(R.id.batch_select);
        View emvRead = drawerLayout.findViewById(R.id.emv_read);
        View apps = drawerLayout.findViewById(R.id.apps);
        View settings = drawerLayout.findViewById(R.id.settings);

        appSelect.setOnClickListener(mClickListener);
        batchSelect.setOnClickListener(mClickListener);
        emvRead.setOnClickListener(mClickListener);
        apps.setOnClickListener(mClickListener);
        settings.setOnClickListener(mClickListener);

        if (inState != null) {
            if (inState.getBoolean("drawer_open")) {
                drawerLayout.openDrawer(Gravity.START|Gravity.LEFT);
            }
        }
    }

    public void onPostCreate() {
        mDrawerToggle.syncState();
    }

    public void onResume() {
        if (mCurrentItem != null) {
            mCurrentItem.setActivated(false);
        }
        mParentItem.setActivated(true);
    }

    public void onSaveInstanceState(Bundle outstate) {
        outstate.putBoolean("drawer_open", isOpen());
    }

    public boolean onBackPressed() {
        if (mDrawerLayout.isDrawerOpen(Gravity.START|Gravity.LEFT)) {
            mDrawerLayout.closeDrawers();
            return true;
        }
        return false;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        return mDrawerToggle.onOptionsItemSelected(item);
    }

    public boolean isOpen() {
        return mDrawerLayout.isDrawerOpen(Gravity.START|Gravity.LEFT);
    }

    private final View.OnClickListener mClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (view.getId() == mResId) {
                // already on it, just close nav drawer
                mDrawerLayout.closeDrawers();
                return;
            }
            mParentItem.setActivated(false);
            view.setActivated(true);
            mCurrentItem = view;

            final Intent intent = new Intent();
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            boolean finishCurrent = false;
            final int viewId = view.getId();
            switch(viewId) {
                case R.id.app_select:
                    intent.setClass(mActivity, AppSelectActivity.class);
                    finishCurrent = true;
                    break;

                case R.id.batch_select:
                    intent.setClass(mActivity, BatchSelectActivity.class);
                    finishCurrent = true;
                    break;

                case R.id.emv_read:
                    intent.setClass(mActivity, EmvReadActivity.class);
                    finishCurrent = true;
                    break;

                case R.id.apps:
                    intent.setClass(mActivity, AppListActivity.class);
                    finishCurrent = false;
                    break;

                case R.id.settings:
                    intent.setClass(mActivity, SettingsActivity.class);
                    finishCurrent = false;
                    break;
            }
            final boolean finish = finishCurrent;
            mHandler.removeCallbacksAndMessages(null);
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mActivity.startActivity(intent);
                    if (finish) {
                        mActivity.finish();
                    }
                    mActivity.overridePendingTransition(R.anim.abc_fade_in,
                            R.anim.abc_fade_out);
                }
            }, 225);

            mDrawerLayout.closeDrawers();
        }
    };
}
