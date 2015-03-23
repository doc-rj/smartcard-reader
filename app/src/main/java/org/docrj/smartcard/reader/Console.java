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
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.widget.ListView;
import android.support.v7.widget.ShareActionProvider;
import android.widget.ViewSwitcher;

public class Console implements MessageAdapter.UiCallbacks {

    private static final String TAG = LaunchActivity.TAG;

    private static final int VIEW_IMAGE = 0;
    private static final int VIEW_MESSAGES = 1;

    private Activity mActivity;
    private Editor mEditor;
    private Handler mHandler;

    private int mTestMode;
    private ListView mListView;
    private ViewSwitcher mSwitcher;

    private MessageAdapter mMsgAdapter;
    private int mMsgPos;

    private ShareActionProvider mShareProvider;

    public Console(Activity activity, Bundle inState, int testMode,
                   ListView listView, ViewSwitcher switcher) {
        mActivity = activity;
        mTestMode = testMode;
        mListView = listView;

        // persistent data in shared prefs
        SharedPreferences ss = activity.getSharedPreferences("prefs", Context.MODE_PRIVATE);
        mEditor = ss.edit();
        mHandler = new Handler();

        mMsgAdapter = new MessageAdapter(activity.getLayoutInflater(), inState, this);
        listView.setAdapter(mMsgAdapter);

        mSwitcher = switcher;
        if (switcher != null) {
            if (activity.getResources().getConfiguration().orientation ==
                    Configuration.ORIENTATION_LANDSCAPE) {
                // in landscape, switch to console messages and disable switching
                switcher.setDisplayedChild(VIEW_MESSAGES);
                mSwitcher = null;
            } else {
                if (mMsgAdapter.getCount() > 0) {
                    switcher.setDisplayedChild(VIEW_MESSAGES);
                }
            }
        }
    }

    public void onPause() {
        mEditor.putInt("msg_pos", mListView.getLastVisiblePosition());
        mEditor.commit();
    }

    public void onResume() {
        setShareIntent();
        SharedPreferences ss = mActivity.getSharedPreferences("prefs", Context.MODE_PRIVATE);
        mMsgPos = ss.getInt("msg_pos", 0);
        // this delay is a bit hacky; would be better to extend ListView
        // and override onLayout()
        mHandler.postDelayed(new Runnable() {
            public void run() {
                smoothScrollToPosition();
            }
        }, 50L);
    }

    public void onViewParsedMsg(Bundle b) {
        Intent i = new Intent(mActivity, MsgParseActivity.class);
        b.putString("activity", mActivity.getTitle().toString());
        b.putInt("test_mode", mTestMode);
        i.putExtra("parsed_msg", b);
        mActivity.startActivity(i);
    }

    public void setShareProvider(ShareActionProvider sp) {
        mShareProvider = sp;
    }

    public void onSaveInstanceState(Bundle outstate) {
        //outstate.putInt("msg_pos", mListView.getLastVisiblePosition());
        if (mMsgAdapter != null) {
            mMsgAdapter.onSaveInstanceState(outstate);
        }
    }

    public void write(final String text, final int type, final String name, final String parsed) {
        if (mMsgAdapter != null) {
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mMsgAdapter.addMessage(text, type, name, parsed);
                    setShareIntent();
                }
            });
        }
    }

    public void writeSeparator() {
        if (mMsgAdapter != null && mMsgAdapter.getCount() > 0) {
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mMsgAdapter.addSeparator();
                }
            });
        }
    }

    public boolean hasMessages() {
        return mMsgAdapter != null && mMsgAdapter.getCount() > 0;
    }

    public void clear() {
        if (mMsgAdapter != null) {
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mMsgAdapter.clearMessages();
                    clearShareIntent();
                }
            });
        }
    }

    public void clear(final boolean showImg) {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (showImg) {
                    if (mSwitcher != null) {
                        if (mSwitcher.getDisplayedChild() != VIEW_IMAGE) {
                            mSwitcher.setDisplayedChild(VIEW_IMAGE);
                        }
                    }
                    clear();
                } else {
                    clear();
                    if (mSwitcher != null) {
                        if (mSwitcher.getDisplayedChild() != VIEW_MESSAGES) {
                            mSwitcher.setDisplayedChild(VIEW_MESSAGES);
                        }
                    }
                }
            }
        });
    }

    public void showImage(final boolean show) {
        if (mSwitcher == null) {
            return;
        }
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (show) {
                    if (mSwitcher.getDisplayedChild() != VIEW_IMAGE) {
                        mSwitcher.setDisplayedChild(VIEW_IMAGE);
                    }
                } else {
                    if (mSwitcher.getDisplayedChild() != VIEW_MESSAGES) {
                        mSwitcher.setDisplayedChild(VIEW_MESSAGES);
                    }
                }
            }
        });
    }

    public void smoothScrollToPosition() {
        mListView.smoothScrollToPosition(mMsgPos);
    }

    public void setShareIntent() {
        if (mMsgAdapter != null && mShareProvider != null) {
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            //Log.d(TAG, mMsgAdapter.getShareMsgsHtml());
            sendIntent.putExtra(Intent.EXTRA_TEXT, Html.fromHtml(mMsgAdapter.getShareMsgsHtml()));
            // subject for emails
            String subject = mActivity.getString(R.string.app_name) + ": " + mActivity.getTitle();
            sendIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
            sendIntent.setType("text/html");
            mShareProvider.setShareIntent(sendIntent);
        }
    }

    public void clearShareIntent() {
        if (mShareProvider != null) {
            mShareProvider.setShareIntent(null);
        }
    }
}
