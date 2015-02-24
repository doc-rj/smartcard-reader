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
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.media.AudioManager;
import android.media.SoundPool;
import android.nfc.NfcAdapter.ReaderCallback;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ShareActionProvider;
import android.widget.SpinnerAdapter;


public class EmvReadActivity extends Activity implements ReaderXcvr.UiCallbacks,
    ReaderCallback, SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = LaunchActivity.TAG;

    // dialogs
    private static final int DIALOG_ENABLE_NFC = AidRouteActivity.DIALOG_ENABLE_NFC;

    // tap feedback values
    private static final int TAP_FEEDBACK_NONE = AidRouteActivity.TAP_FEEDBACK_NONE;
    private static final int TAP_FEEDBACK_VIBRATE = AidRouteActivity.TAP_FEEDBACK_VIBRATE;
    private static final int TAP_FEEDBACK_AUDIO = AidRouteActivity.TAP_FEEDBACK_AUDIO;

    // test modes
    private static final int TEST_MODE_AID_ROUTE = Launcher.TEST_MODE_AID_ROUTE;
    private static final int TEST_MODE_EMV_READ = Launcher.TEST_MODE_EMV_READ;

    private Handler mHandler;
    private Editor mEditor;
    private NfcManager mNfcManager;
    private Console mConsole;

    private boolean mAutoClear;
    private boolean mShowMsgSeparators;
    private int mTapFeedback;

    private int mTapSound;
    private SoundPool mSoundPool;
    private Vibrator mVibrator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final ActionBar actionBar = getActionBar();
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM
                | ActionBar.DISPLAY_SHOW_HOME);
        SpinnerAdapter sAdapter = ArrayAdapter.createFromResource(this,
                R.array.test_modes, R.layout.spinner_dropdown_item_2);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        actionBar.setSelectedNavigationItem(TEST_MODE_EMV_READ);
        actionBar.setListNavigationCallbacks(sAdapter, new ActionBar.OnNavigationListener() {
            String[] strings = getResources().getStringArray(R.array.test_modes);
            @Override
            public boolean onNavigationItemSelected(int position, long itemId) {
                String testMode = strings[position];
                if (!testMode.equals(getString(R.string.emv_read))) {
                    new Launcher(EmvReadActivity.this).launch(testMode, false, false);
                    // finish activity so it does not remain on back stack
                    finish();
                    overridePendingTransition(0, 0);
                }
                return true;
            }
        });

        setContentView(R.layout.activity_emv_read);
        ListView listView = (ListView) findViewById(R.id.msgListView);

        mConsole = new Console(this, savedInstanceState, TEST_MODE_EMV_READ, listView);
        mHandler = new Handler();
        mNfcManager = new NfcManager(this, this);

        ApduParser.init(this);

        // persistent "shared preferences"
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

    @SuppressWarnings("deprecation")
    @Override
    public void onResume() {
        super.onResume();

        final ActionBar actionBar = getActionBar();
        actionBar.setSelectedNavigationItem(TEST_MODE_EMV_READ);

        // this delay is a bit hacky; would be better to extend ListView
        // and override onLayout()
        mHandler.postDelayed(new Runnable() {
            public void run() {
                mConsole.smoothScrollToPosition();
            }
        }, 50L);

        mNfcManager.onResume();
        initSoundPool();
    }

    @Override
    public void onPause() {
        super.onPause();
        writePrefs();
        releaseSoundPool();
        mNfcManager.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        // dismiss enable NFC dialog
        mNfcManager.onStop();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        mConsole.clearShareIntent();
    }

    @Override
    protected void onSaveInstanceState(Bundle outstate) {
        Log.d(TAG, "saving instance state!");
        mConsole.onSaveInstanceState(outstate);
    }

    @SuppressWarnings("deprecation")
    @Override
    protected Dialog onCreateDialog(int id) {
        AlertDialog.Builder builder = new AlertDialog.Builder(
                EmvReadActivity.this, R.style.dialog);
        final LayoutInflater li = getLayoutInflater();
        Dialog dialog = null;
        switch (id) {
            case DIALOG_ENABLE_NFC: {
                dialog = mNfcManager.onCreateDialog(id, builder, li);
                break;
            }
        }
        return dialog;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_emv_read, menu);
        MenuItem item = menu.findItem(R.id.menu_share_msgs);
        mConsole.setShareProvider((ShareActionProvider) item.getActionProvider());
        return true;
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_clear_msgs:
            clearMessages();
            return true;

        case R.id.menu_apps:
            startActivity(new Intent(this, AppsListActivity.class));
            return true;

        case R.id.menu_settings:
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
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
        mConsole.write(raw, MessageAdapter.MSG_SEND, name, null);
    }

    @Override
    public void onMessageRcv(final String raw, final String name, final String parsed) {
        mConsole.write(raw, MessageAdapter.MSG_RCV, name, parsed);
    }

    @Override
    public void onOkay(final String message) {
        mConsole.write(message, MessageAdapter.MSG_OKAY, null, null);
    }

    @Override
    public void onError(final String message) {
        mConsole.write(message, MessageAdapter.MSG_ERROR, null, null);
    }

    @Override
    public void onSeparator() {
        addMessageSeparator();
    }

    @Override
    public void clearMessages() {
        mConsole.clear();
    }

    @Override
    public void setUserSelectListener(final ReaderXcvr.UiListener callback) {
    }

    @Override
    public void onFinish() {
        // nothing yet! animation cleanup worked better elsewhere
    }

    private void addMessageSeparator() {
        if (mShowMsgSeparators) {
            mConsole.writeSeparator();
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
}
