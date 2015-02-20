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
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ListView;
import android.widget.ShareActionProvider;
import android.widget.TextView;

public class Console implements MessageAdapter.OnDialog {

    private static final String TAG = LaunchActivity.TAG;

    // dialog
    private static final int DIALOG_PARSED_MSG = AidRouteActivity.DIALOG_PARSED_MSG;

    private Activity mActivity;
    private ListView mListView;
    private MessageAdapter mMsgAdapter;
    private int mMsgPos;

    private AlertDialog mParsedMsgDialog;
    private String mParsedMsgName = "";
    private String mParsedMsgText = "";

    private ShareActionProvider mShareProvider;

    public Console(Activity activity, Bundle inState, ListView listView) {
        mActivity = activity;
        mListView = listView;
        mMsgAdapter = new MessageAdapter(activity.getLayoutInflater(),
                inState, this);
        listView.setAdapter(mMsgAdapter);
        // restore console messages on orientation change
        if (inState != null) {
            mMsgPos = inState.getInt("msg_pos");
            // restore console parsed message dialog
            mParsedMsgName = inState.getString("parsed_msg_name");
            mParsedMsgText = inState.getString("parsed_msg_text");
        }
    }

    @SuppressWarnings("deprecation")
    public void onDialogParsedMsg(String name, String text) {
        mParsedMsgName = name;
        mParsedMsgText = text;
        mActivity.showDialog(DIALOG_PARSED_MSG);
    }

    public void setShareProvider(ShareActionProvider sp) {
        mShareProvider = sp;
        // TODO: use this when android is fixed (broken in 4.4)
        //mShareProvider.setShareHistoryFileName(null);
    }

    public void onStop() {
        if (mParsedMsgDialog != null) {
            mParsedMsgDialog.dismiss();
        }
    }

    public void onSaveInstanceState(Bundle outstate) {
        outstate.putInt("msg_pos", mListView.getLastVisiblePosition());
        outstate.putString("parsed_msg_name", mParsedMsgName);
        outstate.putString("parsed_msg_text", mParsedMsgText);
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

    public Dialog onCreateDialog(int id, AlertDialog.Builder builder, LayoutInflater li) {
        final View view = li.inflate(R.layout.dialog_parsed_msg, null);
        builder.setView(view)
                .setCancelable(true)
                .setIcon(R.drawable.ic_action_search)
                .setTitle(R.string.parsed_msg);

        mParsedMsgDialog = builder.create();
        return mParsedMsgDialog;
    }

    public void onPrepareDialog(int id, Dialog dialog) {
        TextView tv = (TextView)dialog.findViewById(R.id.dialog_text);
        tv.setText(mParsedMsgText);
        dialog.setTitle(mParsedMsgName);
    }

    private void setShareIntent() {
        if (mMsgAdapter != null && mShareProvider != null) {
            Intent sendIntent = null;
            sendIntent = new Intent();
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
