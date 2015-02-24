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

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.graphics.PorterDuff;
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
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ShareActionProvider;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;


public class AidRouteActivity extends Activity implements ReaderXcvr.UiCallbacks,
    ReaderCallback, SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = LaunchActivity.TAG;

    // update all six items below when adding/removing default apps!
    static final int NUM_RO_APPS = 12;
    static final int DEFAULT_APP_POS = 0;
    static final String[] APP_NAMES = {
        "Amex", "Amex 5-Byte", "Amex 7-Byte", // American Express ExpressPay
        "Amex 8-Byte",
        "Discover",                           // Discover Zip
        "MasterCard", "MasterCard U.S.",      // MasterCard PayPass
        "Visa", "Visa Credit", "Visa Debit",  // Visa PayWave
        "Test Pay", "Test Other"
    };
    static final String[] APP_AIDS = {
        "A00000002501", "A000000025", "A0000000250107",
        "A000000025010701",
        "A0000003241010",
        "A0000000041010", "A0000000042203",
        "A0000000031010", "A000000003101001", "A000000003101002",
        "F07465737420414944", "F07465737420414944"
    };
    // all are payment type except "Test Other"
    static final int[] APP_TYPES = {
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1
    };
    static final int[] APP_READ_ONLY = {
        1, 0, 0, 0, 1, 1, 0, 1, 0, 0, 0, 0
    };

    // dialogs
    static final int DIALOG_ENABLE_NFC = 0;

    // tap feedback values
    static final int TAP_FEEDBACK_NONE = 0;
    static final int TAP_FEEDBACK_VIBRATE = 1;
    static final int TAP_FEEDBACK_AUDIO = 2;

    // test modes
    private static final int TEST_MODE_AID_ROUTE = Launcher.TEST_MODE_AID_ROUTE;
    private static final int TEST_MODE_EMV_READ = Launcher.TEST_MODE_EMV_READ;

    private Handler mHandler;
    private Editor mEditor;
    private ImageButton mManualButton;
    private NfcManager mNfcManager;
    private Console mConsole;

    private AppAdapter mAppAdapter;
    private Button mSelectButton;
    private View mSelectSeparator;

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
    private Spinner mAidSpinner;

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final ActionBar actionBar = getActionBar();
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM
                | ActionBar.DISPLAY_SHOW_HOME);
        SpinnerAdapter sAdapter = ArrayAdapter.createFromResource(this,
                R.array.test_modes, R.layout.spinner_dropdown_item_2);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        actionBar.setListNavigationCallbacks(sAdapter, new ActionBar.OnNavigationListener() {
            String[] strings = getResources().getStringArray(R.array.test_modes);

            @Override
            public boolean onNavigationItemSelected(int position, long itemId) {
                String testMode = strings[position];
                if (!testMode.equals(getString(R.string.aid_route))) {
                    new Launcher(AidRouteActivity.this).launch(testMode, false, false);
                    // finish activity so it does not remain on back stack
                    finish();
                    overridePendingTransition(0, 0);
                }
                return true;
            }
        });

        setContentView(R.layout.activity_aid_route);
        mIntro = (TextView) findViewById(R.id.intro);
        mSelectButton = (Button) findViewById(R.id.manualSelectButton);
        mSelectSeparator = findViewById(R.id.separator2);
        mSelectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSelectHaptic) {
                    v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY,
                            HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
                }
                clearMessages();
                // short delay to show cleared messages
                mHandler.postDelayed(new Runnable() {
                    public void run() {
                        onError(getString(R.string.manual_disconnected));
                    }
                }, 50L);
            }
        });

        ListView listView = (ListView) findViewById(R.id.msgListView);
        mConsole = new Console(this, savedInstanceState, TEST_MODE_AID_ROUTE, listView);
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
                // a few smartcard apps cannot be edited or deleted
                if (APP_READ_ONLY[i] == 1) {
                    app.setReadOnly(true);
                }
                mApps.add(app);
            }
            // write to shared prefs
            writePrefs();
        }

        // do not clear messages for initial selection
        mSelectInInit = true;
        mAidSpinner = (Spinner) findViewById(R.id.aid);
        mAidSpinner
                .setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent,
                            View view, int pos, long id) {
                        if (!mSelectInInit && !mManual) {
                            clearMessages();
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
        mIntro.setText(mManual ? R.string.intro_aid_route_manual : R.string.intro_aid_route);
        mSelectSeparator.setVisibility(mManual ? View.VISIBLE : View.GONE);
        mSelectButton.setVisibility(mManual ? View.VISIBLE : View.GONE);
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
        } else if (key.equals("pref_select_haptic")) {
            mSelectHaptic = prefs.getBoolean("pref_select_haptic", true);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onResume() {
        super.onResume();

        final ActionBar actionBar = getActionBar();
        actionBar.setSelectedNavigationItem(TEST_MODE_AID_ROUTE);

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
        mAppAdapter = new AppAdapter(this, mApps, false);
        mAidSpinner.setAdapter(mAppAdapter);
        mAidSpinner.setSelection(mSelectedAppPos);

        mManual = ss.getBoolean("manual", mManual);
        prepareViewForMode();

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
                AidRouteActivity.this, R.style.dialog);
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
        getMenuInflater().inflate(R.menu.activity_aid_route, menu);
        MenuItem manualMenuItem = menu.findItem(R.id.menu_manual);
        LinearLayout layout = (LinearLayout) manualMenuItem.getActionView();
        mManualButton = (ImageButton) layout.findViewById(R.id.menu_btn);
        mManualButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mManual = !mManual;

                mManualButton.setBackground(mManual ?
                    getResources().getDrawable(R.drawable.button_bg_selected_states) :
                    getResources().getDrawable(R.drawable.button_bg_unselected_states));

                prepareViewForMode();
                clearMessages();
            }
        });

        prepareOptionsMenu();

        MenuItem item = menu.findItem(R.id.menu_share_msgs);
        mConsole.setShareProvider((ShareActionProvider) item.getActionProvider());
        return true;
    }

    private void prepareOptionsMenu() {
        //boolean editEnabled = mApps.size() > NUM_RO_APPS;
        //Drawable editIcon = getResources().getDrawable(
        //        R.drawable.ic_action_edit);
        //if (!editEnabled) {
        //    editIcon.mutate().setColorFilter(Color.LTGRAY,
        //            PorterDuff.Mode.SRC_IN);
        //}
        //mEditMenuItem.setIcon(editIcon);
        //mEditMenuItem.setEnabled(editEnabled);

        mManualButton.setBackground(mManual ?
            getResources().getDrawable(R.drawable.button_bg_selected_states) :
            getResources().getDrawable(R.drawable.button_bg_unselected_states));
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_manual:
                // handled by android:actionLayout="@layout/menu_button"
                // see onCreateOptionsMenu()
                return true;

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

    private void showToast(String text) {
        Toast toast = Toast.makeText(AidRouteActivity.this, text,
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
            ReaderXcvr xcvr;
            String name = mApps.get(mSelectedAppPos).getName();
            String aid = mApps.get(mSelectedAppPos).getAid();

            if (mManual) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        StateListDrawable bg = (StateListDrawable) mSelectButton.getBackground();
                        Drawable currentBg = bg.getCurrent();
                        if (currentBg instanceof AnimationDrawable) {
                            AnimationDrawable btnAnim = (AnimationDrawable) currentBg;
                            btnAnim.stop();
                            btnAnim.start();
                        }
                    }
                });

                // manual select mode; for multiple selects per tap/connect
                // does not select ppse for payment apps unless specifically configured
                xcvr = new ManualReaderXcvr(isoDep, aid, this);
            } else if (mApps.get(mSelectedAppPos).getType() == SmartcardApp.TYPE_PAYMENT) {
                // payment, ie. always selects ppse first
                xcvr = new PaymentReaderXcvr(isoDep, aid, this, TEST_MODE_AID_ROUTE);
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
                    clearMessages();
                    // short delay to show cleared messages
                    mHandler.postDelayed(new Runnable() {
                        public void run() {
                            callback.onUserSelect(mApps.get(mSelectedAppPos).getAid());
                        }
                    }, 50L);
                } else {
                    addMessageSeparator();
                    callback.onUserSelect(mApps.get(mSelectedAppPos).getAid());
                }
            }
        });
    }

    @Override
    public void onFinish() {
        // nothing yet! animation cleanup worked better elsewhere
    }

    private void stopSelectButtonAnim() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                StateListDrawable bg = (StateListDrawable) mSelectButton.getBackground();
                Drawable currentBg = bg.getCurrent();
                if (currentBg instanceof AnimationDrawable) {
                    AnimationDrawable btnAnim = (AnimationDrawable) currentBg;
                    btnAnim.stop();
                }
            }
        });        
    }

    private void addMessageSeparator() {
        if (mShowMsgSeparators) {
            mConsole.writeSeparator();
        }
    }

    private void writePrefs() {
        // serialize list of SmartcardApp
        Gson gson = new Gson();
        String json = gson.toJson(mApps);
        mEditor.putString("apps", json);

        mEditor.putInt("selected_app_pos", mSelectedAppPos);
        mEditor.putBoolean("manual", mManual);

        mEditor.putInt("test_mode", TEST_MODE_AID_ROUTE);

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
