/*
 * Copyright 2014 Ryan Jones
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

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.media.AudioManager;
import android.media.SoundPool;
import android.nfc.NfcAdapter;
import android.nfc.NfcAdapter.ReaderCallback;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.Html;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ShareActionProvider;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;


public class EmvReadActivity extends Activity implements ReaderXcvr.UiCallbacks,
    MessageAdapter.OnDialog, ReaderCallback, SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = AidRouteActivity.TAG;

    private static final int NUM_RO_APPS = AidRouteActivity.NUM_RO_APPS;

    // dialogs
    private static final int DIALOG_ENABLE_NFC = AidRouteActivity.DIALOG_ENABLE_NFC;
    private static final int DIALOG_PARSED_MSG = AidRouteActivity.DIALOG_PARSED_MSG;

    // reader mode flags
    private static final int READER_FLAGS = AidRouteActivity.READER_FLAGS;

    // tap feedback values
    private static final int TAP_FEEDBACK_NONE = AidRouteActivity.TAP_FEEDBACK_NONE;
    private static final int TAP_FEEDBACK_VIBRATE = AidRouteActivity.TAP_FEEDBACK_VIBRATE;
    private static final int TAP_FEEDBACK_AUDIO = AidRouteActivity.TAP_FEEDBACK_AUDIO;

    // test modes
    private static final int TEST_MODE_AID_ROUTE = LaunchActivity.TEST_MODE_AID_ROUTE;
    private static final int TEST_MODE_EMV_READ = LaunchActivity.TEST_MODE_EMV_READ;

    private Handler mHandler;
    private Editor mEditor;
    private NfcAdapter mNfcAdapter;
    private ListView mMsgListView;
    private MessageAdapter mMsgAdapter;

    private int mMsgPos;
    private boolean mAutoClear;
    private boolean mShowMsgSeparators;
    private int mTapFeedback;

    private int mTapSound;
    private SoundPool mSoundPool;
    private Vibrator mVibrator;
    private ShareActionProvider mShareProvider;
    private ActionBar mActionBar;
    private AlertDialog mEnableNfcDialog;
    private AlertDialog mParsedMsgDialog;
    private String mParsedMsgName = "";
    private String mParsedMsgText = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        mActionBar = getActionBar();
        //View titleView = getLayoutInflater().inflate(R.layout.app_title, null);
        //mActionBar.setCustomView(titleView);
        mActionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM
                | ActionBar.DISPLAY_SHOW_HOME);

        SpinnerAdapter sAdapter = ArrayAdapter.createFromResource(this,
                R.array.test_modes, R.layout.spinner_dropdown_item_2);
        mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        mActionBar.setListNavigationCallbacks(sAdapter, new ActionBar.OnNavigationListener() {
            String[] strings = getResources().getStringArray(R.array.test_modes);

            @Override
            public boolean onNavigationItemSelected(int position, long itemId) {
                int testMode = strings[position].equals(getString(R.string.aid_route)) ?
                        TEST_MODE_AID_ROUTE : TEST_MODE_EMV_READ;
                if (testMode != TEST_MODE_EMV_READ) {
                    Intent i = new Intent(EmvReadActivity.this, AidRouteActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    startActivity(i);
                    // finish activity so it does not remain on back stack
                    finish();
                    overridePendingTransition(0, 0);
                }
                return true;
            }
        });

        mActionBar.show();

        setContentView(R.layout.activity_emv_read_layout);

        // restore transient console messages (ie. on portrait/landscape change)
        mMsgListView = (ListView) findViewById(R.id.msgListView);
        mMsgAdapter = new MessageAdapter(getLayoutInflater(),
                savedInstanceState, this);
        mMsgListView.setAdapter(mMsgAdapter);
        if (savedInstanceState != null) {
            mMsgPos = savedInstanceState.getInt("msg_pos");
            // restore console parsed message dialog
            mParsedMsgName = savedInstanceState.getString("parsed_msg_name");
            mParsedMsgText = savedInstanceState.getString("parsed_msg_text");
        }

        mHandler = new Handler();
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);

        ApduParser.init(this);

        // persistent "shared preferences" store
        SharedPreferences ss = getSharedPreferences("prefs", Context.MODE_PRIVATE);
        mEditor = ss.edit();

        // persistent settings and settings listener
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(this);

        mAutoClear = prefs.getBoolean("pref_auto_clear", true);
        mShowMsgSeparators = prefs.getBoolean("pref_show_separators", true);
        String tapFeedback = prefs.getString("pref_tap_feedback", "1");
        mTapFeedback = Integer.valueOf(tapFeedback);

        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
    }

    private void prepareViewForMode() {
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        if (key.equals("pref_auto_clear")){
            mAutoClear = prefs.getBoolean("pref_auto_clear", true);
        } else if (key.equals("pref_show_separators")) {
            mShowMsgSeparators = prefs.getBoolean("pref_show_separators", true);
            clearMessages();
        } else if (key.equals("pref_tap_feedback")) {
            String tapFeedback = prefs.getString("pref_tap_feedback", "1");
            mTapFeedback = Integer.valueOf(tapFeedback);
        }
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
                                        EmvReadActivity.this,
                                        EmvReadActivity.this,
                                        READER_FLAGS,
                                        null);
                    }
                } else {
                    if (mEnableNfcDialog == null
                            || !mEnableNfcDialog.isShowing()) {
                        showDialog(DIALOG_ENABLE_NFC);
                    }
                }
            }
        }
    };

    @SuppressWarnings("deprecation")
    @Override
    public void onResume() {
        super.onResume();
        mActionBar.setSelectedNavigationItem(TEST_MODE_EMV_READ);

        // restore mode and selected pos from prefs
        SharedPreferences ss = getSharedPreferences("prefs", Context.MODE_PRIVATE);
        prepareViewForMode();

        // this delay is a bit hacky; would be better to extend ListView
        // and override onLayout()
        mHandler.postDelayed(new Runnable() {
            public void run() {
                mMsgListView.smoothScrollToPosition(mMsgPos);
            }
        }, 50L);

        // register broadcast receiver
        IntentFilter filter = new IntentFilter(
                NfcAdapter.ACTION_ADAPTER_STATE_CHANGED);
        registerReceiver(mBroadcastReceiver, filter);

        // prompt to enable NFC if disabled
        if (!mNfcAdapter.isEnabled()) {
            showDialog(DIALOG_ENABLE_NFC);
        }

        initSoundPool();        
        
        // listen for type A tags/smartcards, skipping ndef check
        mNfcAdapter.enableReaderMode(this, this, READER_FLAGS, null);
    }

    @Override
    public void onPause() {
        super.onPause();
        // save selected pos to prefs
        writePrefs();
        unregisterReceiver(mBroadcastReceiver);
        releaseSoundPool();
        mNfcAdapter.disableReaderMode(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mEnableNfcDialog != null) {
            mEnableNfcDialog.dismiss();
        }
        if (mParsedMsgDialog != null) {
            mParsedMsgDialog.dismiss();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        clearShareMsgsIntent();
    }

    @Override
    protected void onSaveInstanceState(Bundle outstate) {
        Log.d(TAG, "saving instance state!");
        // console message i/o list
        outstate.putInt("msg_pos", mMsgListView.getLastVisiblePosition());
        outstate.putString("parsed_msg_name", mParsedMsgName);
        outstate.putString("parsed_msg_text", mParsedMsgText);
        if (mMsgAdapter != null) {
            mMsgAdapter.onSaveInstanceState(outstate);
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        AlertDialog.Builder builder = new AlertDialog.Builder(
                EmvReadActivity.this, R.style.dialog);
        final LayoutInflater li = getLayoutInflater();
        Dialog dialog = null;
        switch (id) {
        case DIALOG_ENABLE_NFC: {
            final View view = li.inflate(R.layout.dialog_enable_nfc, null);
            builder.setView(view)
                    .setCancelable(false)
                    .setIcon(R.drawable.ic_enable_nfc)
                    .setTitle(R.string.nfc_disabled)
                    .setPositiveButton(R.string.dialog_ok,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                        int id) {
                                    // take user to wireless settings
                                    startActivity(new Intent(
                                            Settings.ACTION_WIRELESS_SETTINGS));
                                }
                            })
                    .setNegativeButton(R.string.dialog_quit,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                        int id) {
                                    dialog.cancel();
                                    finish();
                                }
                            });

            mEnableNfcDialog = builder.create();
            dialog = mEnableNfcDialog;
            break;
        } // case
        case DIALOG_PARSED_MSG: {
            final View view = li.inflate(R.layout.dialog_parsed_msg, null);
            builder.setView(view)
                    .setCancelable(true)
                    .setIcon(R.drawable.ic_action_search)
                    .setTitle(R.string.parsed_msg);

            mParsedMsgDialog = builder.create();
            dialog = mParsedMsgDialog;
            break;
        }
        } // switch
        return dialog;
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        switch (id) {
        case DIALOG_ENABLE_NFC: {
            break;
        }
        case DIALOG_PARSED_MSG: {
            TextView tv = (TextView)dialog.findViewById(R.id.dialog_text);
            tv.setText(mParsedMsgText);
            dialog.setTitle(mParsedMsgName);
            break;
        }
        }
    }

    private void dismissKeyboard(View focus) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(focus.getWindowToken(), 0);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_emv_read_menu, menu);
        // get customized menu items before calling prepare
        prepareOptionsMenu();

        MenuItem item = menu.findItem(R.id.menu_share_msgs);
        mShareProvider = (ShareActionProvider) item.getActionProvider();
        // TODO: use this when android is fixed (broken in 4.4)
        //mShareProvider.setShareHistoryFileName(null);
        return true;
    }

    private void prepareOptionsMenu() {
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_clear_msgs:
            clearMessages();
            return true;

        case R.id.menu_settings:
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showToast(String text) {
        Toast toast = Toast.makeText(EmvReadActivity.this, text,
                Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER_VERTICAL, 0, -100);
        toast.show();
    }

    private void initSoundPool() {
        synchronized (this) {
            if (mSoundPool == null) {
                mSoundPool = new SoundPool(1, AudioManager.STREAM_NOTIFICATION, 0);
                mTapSound = mSoundPool.load(this, R.raw.tap, 1);
            }
        }
    }

    private void releaseSoundPool() {
        synchronized (this) {
            if (mSoundPool != null) {
                mSoundPool.release();
                mSoundPool = null;
            }
        }
    }

    private void doTapFeedback() {
        if (mTapFeedback == TAP_FEEDBACK_AUDIO) {
            mSoundPool.play(mTapSound, 1.0f, 1.0f, 0, 0, 1.0f); 
        } else if (mTapFeedback == TAP_FEEDBACK_VIBRATE) {
            long[] pattern = {0, 50, 50, 50};
            mVibrator.vibrate(pattern, -1);
        }        
    }
    
    @Override
    public void onTagDiscovered(Tag tag) {
        doTapFeedback();
        // maybe clear console or show separator, depends on settings
        if (mAutoClear) {
            clearMessages();
        } else {
            addMessageSeparator();
        }
        // get IsoDep handle and run xcvr thread
        IsoDep isoDep = IsoDep.get(tag);
        if (isoDep == null) {
            onError(getString(R.string.wrong_tag_err));
        } else {
            ReaderXcvr xcvr = new PaymentReaderXcvr(isoDep, "", this, TEST_MODE_EMV_READ);
            new Thread(xcvr).start();
        }
    }

    @Override
    public void onMessageSend(final String raw, final String name) {
        onMessage(raw, MessageAdapter.MSG_SEND, name, null);
    }

    @Override
    public void onMessageRcv(final String raw, final String name, final String parsed) {
        onMessage(raw, MessageAdapter.MSG_RCV, name, parsed);
    }

    @Override
    public void onOkay(final String message) {
        onMessage(message, MessageAdapter.MSG_OKAY, null, null);
    }

    @Override
    public void onError(final String message) {
        onMessage(message, MessageAdapter.MSG_ERROR, null, null);
    }

    @Override
    public void onSeparator() {
        addMessageSeparator();
    }

    private void onMessage(final String text, final int type, final String name, final String parsed) {
        if (mMsgAdapter != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mMsgAdapter.addMessage(text, type, name, parsed);
                    setShareMsgsIntent();
                }
            });
        }
    }

    @Override
    public void clearMessages() {
        if (mMsgAdapter != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mMsgAdapter.clearMessages();
                    clearShareMsgsIntent();
                }
            });
        }
    }

    @Override
    public void setUserSelectListener(final ReaderXcvr.UiListener callback) {
    }

    @Override
    public void onFinish() {
        // nothing yet! animation cleanup worked better elsewhere
    }

    private void addMessageSeparator() {
        if (mShowMsgSeparators && mMsgAdapter != null && mMsgAdapter.getCount() > 0) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mMsgAdapter.addSeparator();
                }
            });
        }        
    }    

    private void setShareMsgsIntent() {
        if (mMsgAdapter != null && mShareProvider != null) {
            Intent sendIntent = null;
            sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            //Log.d(TAG, mMsgAdapter.getShareMsgsHtml());
            sendIntent.putExtra(Intent.EXTRA_TEXT, Html.fromHtml(mMsgAdapter.getShareMsgsHtml()));
            // subject for emails
            String subject = getString(R.string.app_name) + ": " +
                    getString(R.string.emv_read);
            sendIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
            sendIntent.setType("text/html");
            mShareProvider.setShareIntent(sendIntent);
        }
    }

    private void clearShareMsgsIntent() {
        if (mShareProvider != null) {
            mShareProvider.setShareIntent(null);
        }
    }

    private void writePrefs() {
        mEditor.putInt("test_mode", TEST_MODE_EMV_READ);
        mEditor.commit();
    }

    private class writePrefsTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... v) {
            writePrefs();
            return null;
        }
    }
    
    @SuppressWarnings("deprecation")
    public void onDialogParsedMsg(String name, String text) {
        mParsedMsgName = name;
        mParsedMsgText = text;
        showDialog(DIALOG_PARSED_MSG);    
    }
}
