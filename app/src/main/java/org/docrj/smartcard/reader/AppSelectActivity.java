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

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

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
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.ShareActionProvider;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.animation.Animation;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.afollestad.materialdialogs.AlertDialogWrapper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;


public class AppSelectActivity extends ActionBarActivity implements ReaderXcvr.UiCallbacks,
    ReaderCallback, SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = LaunchActivity.TAG;

    private static final String[] GROUPS = SmartcardApp.GROUPS;

    // update all five memberApps below when adding/removing default apps!
    static final int DEFAULT_APP_POS = 0;
    static final String[] APP_NAMES = {
        "Amex", "Amex 5-Byte", "Amex 7-Byte", // American Express ExpressPay
        "Amex 8-Byte",                        // ""
        "Discover",                           // Discover Zip
        "MasterCard", "MasterCard U.S.",      // MasterCard PayPass
        "Test Other", "Test Pay",             // test
        "Visa", "Visa Credit", "Visa Debit"   // Visa PayWave
    };
    static final String[] APP_AIDS = {
        "A00000002501", "A000000025", "A0000000250107",
        "A000000025010701",
        "A0000003241010",
        "A0000000041010", "A0000000042203",
        "F07465737420414944", "F07465737420414944",
        "A0000000031010", "A000000003101001", "A000000003101002",
    };
    // all are payment type except "Test Other"
    static final int[] APP_TYPES = {
        0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0
    };
    static final int[] APP_READ_ONLY = {
        1, 0, 0, 0, 1, 1, 0, 1, 1, 1, 0, 0
    };

    // dialogs
    static final int DIALOG_ENABLE_NFC = 0;

    // tap feedback values
    static final int TAP_FEEDBACK_NONE = 0;
    static final int TAP_FEEDBACK_VIBRATE = 1;
    static final int TAP_FEEDBACK_AUDIO = 2;

    // test modes
    private static final int TEST_MODE_APP_SELECT = Launcher.TEST_MODE_APP_SELECT;

    // actions
    private static final String ACTION_VIEW_APP = AppListActivity.ACTION_VIEW_APP;
    private static final String ACTION_NEW_APP = AppListActivity.ACTION_NEW_APP;

    // extras
    private static final String EXTRA_SELECT = AppListActivity.EXTRA_SELECT;
    private static final String EXTRA_APP_POS = AppListActivity.EXTRA_APP_POS;

    private NavDrawer mNavDrawer;
    private Handler mHandler;
    private Editor mEditor;
    private NfcManager mNfcManager;
    private Console mConsole;

    private boolean mAutoClear;
    private boolean mManual;
    private boolean mShowMsgSeparators;
    private int mTapFeedback;
    private boolean mSelectHaptic;

    private int mTapSound;
    private SoundPool mSoundPool;
    private Vibrator mVibrator;

    private int mSelectedAppPos = DEFAULT_APP_POS;
    private ArrayList<SmartcardApp> mApps;
    private boolean mSelectInInit;

    private TextView mIntro;
    private ViewGroup mSelectBar;
    private Button mSelectButton;
    private Spinner mAppSpinner;
    private MenuItem mManualMenuItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.drawer_activity_app_select);

        mIntro = (TextView) findViewById(R.id.intro);
        mSelectBar = (ViewGroup) findViewById(R.id.manual_select_bar);
        mSelectButton = (Button) findViewById(R.id.manual_select_button);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mNavDrawer = new NavDrawer(this, savedInstanceState, R.id.app_select, drawerLayout, toolbar);

        ListView listView = (ListView) findViewById(R.id.msg_list);
        ViewSwitcher switcher = (ViewSwitcher) findViewById(R.id.switcher);
        mConsole = new Console(this, savedInstanceState, TEST_MODE_APP_SELECT, listView, switcher);

        mHandler = new Handler();
        mNfcManager = new NfcManager(this, this);

        ApduParser.init(this);

        // persistent data in shared prefs
        SharedPreferences ss = getSharedPreferences("prefs", Context.MODE_PRIVATE);
        mEditor = ss.edit();

        // if shared prefs is empty, synchronously write defaults
        String json = ss.getString("apps", null);
        if (json == null) {
            // initialize default smartcard apps
            mApps = new ArrayList<SmartcardApp>();
            for (int i = 0; i < APP_NAMES.length; i++) {
                SmartcardApp app = new SmartcardApp(APP_NAMES[i], APP_AIDS[i], APP_TYPES[i]);
                // some smartcard apps cannot be edited or deleted
                if (APP_READ_ONLY[i] == 1) {
                    app.setReadOnly(true);
                }
                mApps.add(app);
            }
            // write default apps to persistent shared prefs
            writeAppsToPrefs();
        }

        // do not clear messages for initial selection
        mSelectInInit = true;
        mAppSpinner = (Spinner) findViewById(R.id.app);
        mAppSpinner
                .setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent,
                                               View view, int pos, long id) {
                        if (!mSelectInInit && !mManual) {
                            clearMessages(true);
                        }
                        mSelectInInit = false;
                        mSelectedAppPos = pos;
                        Log.d(TAG, "App: " + mApps.get(pos).getName()
                                + ", AID: " + mApps.get(pos).getAid());
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                    }
                });

        // persistent settings and settings listener
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(this);

        mAutoClear = prefs.getBoolean("pref_auto_clear", true);
        mShowMsgSeparators = prefs.getBoolean("pref_show_separators", true);
        String tapFeedback = prefs.getString("pref_tap_feedback", "1");
        mTapFeedback = Integer.valueOf(tapFeedback);
        mSelectHaptic = prefs.getBoolean("pref_select_haptic", true);

        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
    }

    private void prepareViewForMode() {
        mIntro.setText(mManual ? R.string.intro_app_select_manual : R.string.intro_app_select);
        if (mManual) {
            mSelectButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mSelectHaptic) {
                        v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY,
                                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
                    }
                    clearMessages(false);
                    // short delay to show cleared messages
                    mHandler.postDelayed(new Runnable() {
                        public void run() {
                            onError(getString(R.string.manual_disconnected));
                        }
                    }, 50L);
                }
            });
            if (mSelectBar.getVisibility() == View.INVISIBLE) {
                // slide select bar up and shake the button!
                mSelectBar.setVisibility(View.VISIBLE);
                Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up);
                mSelectBar.startAnimation(slideUp);
                Animation shake = AnimationUtils.loadAnimation(this, R.anim.shake);
                mSelectButton.startAnimation(shake);
            }
        } else {
            if (mSelectBar.getVisibility() == View.VISIBLE) {
                Animation slideDown = AnimationUtils.loadAnimation(this, R.anim.slide_down);
                mSelectBar.startAnimation(slideDown);
                mSelectBar.setVisibility(View.INVISIBLE);
            }
        }
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
        } else if (key.equals("pref_select_haptic")) {
            mSelectHaptic = prefs.getBoolean("pref_select_haptic", true);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // restore persistent data
        SharedPreferences ss = getSharedPreferences("prefs", Context.MODE_PRIVATE);
        String json = ss.getString("apps", null);
        // deserialize list of SmartcardApp
        Gson gson = new Gson();
        Type collectionType = new TypeToken<ArrayList<SmartcardApp>>() {}.getType();
        mApps = gson.fromJson(json, collectionType);

        mSelectedAppPos = ss.getInt("selected_app_pos", mSelectedAppPos);

        // do not clear messages for this selection on resume;
        // setAdapter and setSelection result in onItemSelected callback
        mSelectInInit = true;
        AppAdapter appAdapter = new AppAdapter(this, mApps, false);
        mAppSpinner.setAdapter(appAdapter);
        mAppSpinner.setSelection(mSelectedAppPos);

        mManual = ss.getBoolean("manual", mManual);
        prepareViewForMode();

        mNfcManager.onResume();
        mConsole.onResume();
        mNavDrawer.onResume();
        initSoundPool();
    }

    @Override
    public void onPause() {
        writePrefs();
        releaseSoundPool();
        mConsole.onPause();
        mNfcManager.onPause();
        super.onPause();
    }

    @Override
    public void onStop() {
        // dismiss enable NFC dialog
        mNfcManager.onStop();
        super.onStop();
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
        //        AidRouteActivity.this, R.style.dialog);
        AlertDialogWrapper.Builder builder = new AlertDialogWrapper.Builder(this);
        final LayoutInflater li = getLayoutInflater();
        Dialog dialog = null;
        switch (id) {
            case DIALOG_ENABLE_NFC:
                dialog = mNfcManager.onCreateDialog(id, builder, li);
                break;
        }
        return dialog;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_app_select, menu);
        mManualMenuItem = menu.findItem(R.id.menu_manual);

        prepareOptionsMenu();

        MenuItem item = menu.findItem(R.id.menu_share_msgs);
        mConsole.setShareProvider((ShareActionProvider) MenuItemCompat.getActionProvider(item));
        return true;
    }

    private void prepareOptionsMenu() {
        mManualMenuItem.setChecked(mManual);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        prepareOptionsMenu();
        boolean drawerOpen = mNavDrawer.isOpen();
        MenuItem item = menu.findItem(R.id.menu_add_app);
        item.setVisible(!drawerOpen);
        item = menu.findItem(R.id.menu_app_details);
        item.setVisible(!drawerOpen);
        mManualMenuItem.setVisible(!drawerOpen);
        item = menu.findItem(R.id.menu_share_msgs);
        item.setVisible(!drawerOpen);
        item = menu.findItem(R.id.menu_clear_msgs);
        item.setVisible(!drawerOpen);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mNavDrawer.onOptionsItemSelected(item)) {
            return true;
        }
        switch (item.getItemId()) {
            case R.id.menu_add_app: {
                // start activity to add new app
                Intent i = new Intent(this, AppEditActivity.class);
                i.setAction(ACTION_NEW_APP);
                i.putExtra(EXTRA_SELECT, true);
                startActivity(i);
                return true;
            }
            case R.id.menu_app_details: {
                // start activity to view app details
                Intent i = new Intent(this, AppViewActivity.class);
                i.setAction(ACTION_VIEW_APP);
                i.putExtra(EXTRA_APP_POS, mSelectedAppPos);
                // select app copy when copy is initiated from app details
                i.putExtra(EXTRA_SELECT, true);
                startActivity(i);
                return true;
            }
            case R.id.menu_manual: {
                boolean shouldCheck = !mManualMenuItem.isChecked();
                mManualMenuItem.setChecked(shouldCheck);
                mManual = shouldCheck;
                prepareViewForMode();
                clearMessages(true);
                return true;
            }
            case R.id.menu_clear_msgs: {
                clearMessages(true);
                return true;
            }
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
            ReaderXcvr xcvr;
            String aid = mApps.get(mSelectedAppPos).getAid();

            if (mManual) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Animation shake = AnimationUtils.loadAnimation(AppSelectActivity.this, R.anim.shake);
                        mSelectButton.startAnimation(shake);
                    }
                });
                // manual select mode; for multiple selects per tap/connect
                // does not select ppse for payment apps unless specifically configured
                xcvr = new ManualReaderXcvr(isoDep, aid, this);
            } else if (mApps.get(mSelectedAppPos).getType() == SmartcardApp.TYPE_PAYMENT) {
                // payment, ie. always selects ppse first
                xcvr = new PaymentReaderXcvr(isoDep, aid, this, TEST_MODE_APP_SELECT);
            } else {
                // other/non-payment; auto select on each tap/connect
                xcvr = new OtherReaderXcvr(isoDep, aid, this);
            }

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
        mSelectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // haptic feedback
                if (mSelectHaptic) {
                    v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY,
                            HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
                }
                // update console and do select transaction
                if (mAutoClear) {
                    clearMessages(false);
                    // short delay to show cleared messages
                    mHandler.postDelayed(new Runnable() {
                        public void run() {
                            callback.onUserSelect(mApps.get(mSelectedAppPos).getAid());
                        }
                    }, 50L);
                } else {
                    clearImage();
                    addMessageSeparator();
                    callback.onUserSelect(mApps.get(mSelectedAppPos).getAid());
                }
            }
        });
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

    private void writeAppsToPrefs() {
        // serialize list of apps
        Gson gson = new Gson();
        String json = gson.toJson(mApps);
        mEditor.putString("apps", json);
        mEditor.commit();
    }

    private void writePrefs() {
        mEditor.putInt("selected_app_pos", mSelectedAppPos);
        mEditor.putBoolean("manual", mManual);
        mEditor.putInt("test_mode", TEST_MODE_APP_SELECT);
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
