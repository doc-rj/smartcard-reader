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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;

import com.afollestad.materialdialogs.AlertDialogWrapper;

public class NfcManager {

    private static final String TAG = LaunchActivity.TAG;

    // reader mode flags: listen for type A (not B), skipping ndef check
    private static final int READER_FLAGS =
            NfcAdapter.FLAG_READER_NFC_A |
            NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK |
            NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS;

    // dialog
    private static final int DIALOG_ENABLE_NFC = AppSelectActivity.DIALOG_ENABLE_NFC;

    private NfcAdapter mNfcAdapter;
    private Dialog mEnableNfcDialog;
    private Activity mActivity;
    private NfcAdapter.ReaderCallback mReaderCallback;

    @SuppressWarnings("deprecation")
    public NfcManager(Activity activity, NfcAdapter.ReaderCallback readerCallback) {
        mActivity = activity;
        mReaderCallback = readerCallback;
        mNfcAdapter = NfcAdapter.getDefaultAdapter(activity);
    }

    public void onResume() {
        // register broadcast receiver
        IntentFilter filter = new IntentFilter(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED);
        mActivity.registerReceiver(mBroadcastReceiver, filter);

        if (mNfcAdapter == null || !mNfcAdapter.isEnabled()) {
            mActivity.showDialog(DIALOG_ENABLE_NFC);
        } else {
            mNfcAdapter.enableReaderMode(mActivity, mReaderCallback, READER_FLAGS, null);
        }
    }

    public void onPause() {
        mActivity.unregisterReceiver(mBroadcastReceiver);
        if (mNfcAdapter != null) {
            mNfcAdapter.disableReaderMode(mActivity);
        }
    }

    public void onStop() {
        if (mEnableNfcDialog != null) {
            mEnableNfcDialog.dismiss();
        }
    }

    public Dialog onCreateDialog(int id, AlertDialogWrapper.Builder builder, LayoutInflater li) {
        builder.setCancelable(false)
                .setIcon(R.drawable.ic_action_nfc_gray)
                .setTitle(R.string.nfc_disabled)
                .setMessage(R.string.enable_nfc)
                .setPositiveButton(R.string.dialog_ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int id) {
                                // take user to wireless settings
                                mActivity.startActivity(new Intent(
                                        Settings.ACTION_WIRELESS_SETTINGS));
                            }
                        })
                .setNegativeButton(R.string.dialog_quit,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int id) {
                                dialog.cancel();
                                mActivity.finish();
                            }
                        });

        mEnableNfcDialog = builder.create();
        return mEnableNfcDialog;
    }

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @SuppressWarnings("deprecation")
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null)
                return;
            if (action.equals(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED)) {
                int state = intent.getIntExtra(NfcAdapter.EXTRA_ADAPTER_STATE,
                        NfcAdapter.STATE_ON);
                if (state == NfcAdapter.STATE_ON
                        || state == NfcAdapter.STATE_TURNING_ON) {
                    Log.d(TAG, "state: " + state + " , dialog: "
                            + mEnableNfcDialog);
                    if (mEnableNfcDialog != null) {
                        mEnableNfcDialog.dismiss();
                    }
                    if (state == NfcAdapter.STATE_ON) {
                        mNfcAdapter
                                .enableReaderMode(
                                        mActivity,
                                        mReaderCallback,
                                        READER_FLAGS,
                                        null);
                    }
                } else {
                    if (mEnableNfcDialog == null || !mEnableNfcDialog.isShowing()) {
                        mActivity.showDialog(DIALOG_ENABLE_NFC);
                    }
                }
            }
        }
    };
}
