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
import android.os.Bundle;
import android.text.Html;
import android.widget.ListView;
import android.widget.ShareActionProvider;

public class Console implements MessageAdapter.UiCallbacks {

    private static final String TAG = LaunchActivity.TAG;

    private Activity mActivity;
    private int mTestMode;
    private ListView mListView;
    private MessageAdapter mMsgAdapter;
    private int mMsgPos;

    private ShareActionProvider mShareProvider;

    public Console(Activity activity, Bundle inState, int testMode, ListView listView) {
        mActivity = activity;
        mTestMode = testMode;
        mListView = listView;
        mMsgAdapter = new MessageAdapter(activity.getLayoutInflater(),
                inState, this);
        listView.setAdapter(mMsgAdapter);

        // restore console messages on orientation change
        if (inState != null) {
            mMsgPos = inState.getInt("msg_pos");
        }
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
        // TODO: use this when android is fixed (broken in 4.4)
        //mShareProvider.setShareHistoryFileName(null);
    }

    public void onSaveInstanceState(Bundle outstate) {
        outstate.putInt("msg_pos", mListView.getLastVisiblePosition());
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

    public void smoothScrollToPosition() {
        mListView.smoothScrollToPosition(mMsgPos);
    }

    private void setShareIntent() {
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
