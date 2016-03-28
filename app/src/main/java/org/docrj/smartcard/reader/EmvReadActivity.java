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

import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.ShareActionProvider;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.media.AudioManager;
import android.media.SoundPool;
import android.nfc.NfcAdapter.ReaderCallback;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.ViewSwitcher;

import com.afollestad.materialdialogs.AlertDialogWrapper;

import org.docrj.smartcard.emv.EMVTerminal;


public class EmvReadActivity extends ActionBarActivity implements ReaderXcvr.UiCallbacks,
    ReaderCallback, SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = LaunchActivity.TAG;

    // dialogs
    private static final int DIALOG_ENABLE_NFC = AppSelectActivity.DIALOG_ENABLE_NFC;

    // tap feedback values
    private static final int TAP_FEEDBACK_NONE = AppSelectActivity.TAP_FEEDBACK_NONE;
    private static final int TAP_FEEDBACK_VIBRATE = AppSelectActivity.TAP_FEEDBACK_VIBRATE;
    private static final int TAP_FEEDBACK_AUDIO = AppSelectActivity.TAP_FEEDBACK_AUDIO;

    // test modes
    private static final int TEST_MODE_EMV_READ = Launcher.TEST_MODE_EMV_READ;

    private NavDrawer mNavDrawer;
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
        setContentView(R.layout.drawer_activity_emv_read);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.emv_drawer_layout);
        mNavDrawer = new NavDrawer(this, savedInstanceState, R.id.emv_read, drawerLayout, toolbar);

        ListView listView = (ListView) findViewById(R.id.msg_list);
        ViewSwitcher switcher = (ViewSwitcher) findViewById(R.id.switcher);
        mConsole = new Console(this, savedInstanceState, TEST_MODE_EMV_READ, listView, switcher);
        mNfcManager = new NfcManager(this, this);

        ApduParser.init(this);
        EMVTerminal.loadProperties(getResources());

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
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mNavDrawer.onPostCreate();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        if (key.equals("pref_auto_clear")){
            mAutoClear = prefs.getBoolean("pref_auto_clear", true);
        } else if (key.equals("pref_show_separators")) {
            mShowMsgSeparators = prefs.getBoolean("pref_show_separators", true);
            clearMessages(true);
        } else if (key.equals("pref_tap_feedback")) {
            String tapFeedback = prefs.getString("pref_tap_feedback", "1");
            mTapFeedback = Integer.valueOf(tapFeedback);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mNfcManager.onResume();
        mConsole.onResume();
        mNavDrawer.onResume();
        initSoundPool();
    }

    @Override
    public void onPause() {
        super.onPause();
        writePrefs();
        releaseSoundPool();
        mConsole.onPause();
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
        if (mNavDrawer.onBackPressed()) {
            return;
        }
        mConsole.clearShareIntent();
        super.onBackPressed();
    }

    @Override
    protected void onSaveInstanceState(Bundle outstate) {
        mNavDrawer.onSaveInstanceState(outstate);
        mConsole.onSaveInstanceState(outstate);
    }

    @SuppressWarnings("deprecation")
    @Override
    protected Dialog onCreateDialog(int id) {
        //AlertDialog.Builder builder = new AlertDialog.Builder(
        //        EmvReadActivity.this, R.style.dialog);
        AlertDialogWrapper.Builder builder = new AlertDialogWrapper.Builder(this);
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
        mConsole.setShareProvider((ShareActionProvider) MenuItemCompat.getActionProvider(item));
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean drawerOpen = mNavDrawer.isOpen();
        MenuItem item = menu.findItem(R.id.menu_share_msgs);
        item.setVisible(!drawerOpen);
        item = menu.findItem(R.id.menu_clear_msgs);
        item.setVisible(!drawerOpen);

        mConsole.setShareIntent();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mNavDrawer.onOptionsItemSelected(item)) {
            return true;
        }
        switch (item.getItemId()) {
            case R.id.menu_clear_msgs:
                clearMessages(true);
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
        clearImage();
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

    private void clearMessages(boolean showImg) {
        mConsole.clear(showImg);
    }

    private void clearImage() {
        mConsole.showImage(false);
    }

    @Override
    public void setUserSelectListener(final ReaderXcvr.UiListener callback) {
    }

    @Override
    public void onFinish(boolean err) {
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
}
