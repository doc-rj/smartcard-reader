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
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.ShareActionProvider;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.ViewSwitcher;

import com.afollestad.materialdialogs.AlertDialogWrapper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;


public class BatchSelectActivity extends ActionBarActivity implements ReaderXcvr.UiCallbacks,
    ReaderCallback, SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = LaunchActivity.TAG;

    private static final String[] DEFAULT_GROUPS = SmartcardApp.GROUPS;

    // actions
    private static final String ACTION_VIEW_GROUP = AppListActivity.ACTION_VIEW_GROUP;
    private static final String ACTION_NEW_GROUP = AppListActivity.ACTION_NEW_GROUP;

    // extras
    private static final String EXTRA_SELECT = AppListActivity.EXTRA_SELECT;
    private static final String EXTRA_GROUP_POS = AppListActivity.EXTRA_GROUP_POS;
    private static final String EXTRA_GROUP_NAME = AppListActivity.EXTRA_GROUP_NAME;

    // dialogs
    static final int DIALOG_ENABLE_NFC = 0;

    // tap feedback values
    static final int TAP_FEEDBACK_NONE = 0;
    static final int TAP_FEEDBACK_VIBRATE = 1;
    static final int TAP_FEEDBACK_AUDIO = 2;

    // test modes
    private static final int TEST_MODE_BATCH_SELECT = Launcher.TEST_MODE_BATCH_SELECT;

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

    // groups created by user (no payment/other)
    private HashSet<String> mGroups;
    // selected index in sorted group list
    private int mSelectedGrpPos = 0;
    private ArrayList<SmartcardApp> mApps;
    // mapping of group adapter list position to alphabetized member apps
    private HashMap<Integer, List<SmartcardApp>> mGrpToMembersMap = new HashMap<>(2);
    private boolean mSelectInInit;

    private BatchSelectGroupAdapter mGrpAdapter;
    private Spinner mGrpSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.drawer_activity_batch_select);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mNavDrawer = new NavDrawer(this, savedInstanceState, R.id.batch_select, drawerLayout, toolbar);

        ListView listView = (ListView) findViewById(R.id.msg_list);
        ViewSwitcher switcher = (ViewSwitcher) findViewById(R.id.switcher);
        mConsole = new Console(this, savedInstanceState, TEST_MODE_BATCH_SELECT, listView, switcher);
        mNfcManager = new NfcManager(this, this);

        ApduParser.init(this);

        // persistent data in shared prefs
        SharedPreferences ss = getSharedPreferences("prefs", Context.MODE_PRIVATE);
        mEditor = ss.edit();

        // do not clear messages for initial selection
        mSelectInInit = true;
        mGrpAdapter = new BatchSelectGroupAdapter(getLayoutInflater());
        mGrpSpinner = (Spinner) findViewById(R.id.group);
        mGrpSpinner
                .setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent,
                            View view, int pos, long id) {
                        if (!mSelectInInit) {
                            clearMessages(true);
                        }
                        mSelectInInit = false;
                        mSelectedGrpPos = pos;
                        String groupName = mGrpAdapter.getGroupName(pos);
                        Log.d(TAG, "group: " + groupName);
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

        // restore persistent data
        SharedPreferences ss = getSharedPreferences("prefs", Context.MODE_PRIVATE);

        Gson gson = new Gson();
        Type collectionType;
        String json = ss.getString("apps", null);
        if (json != null) {
            collectionType = new TypeToken<ArrayList<SmartcardApp>>() {
            }.getType();
            mApps = gson.fromJson(json, collectionType);
        }

        mGroups = new LinkedHashSet<>();
        json = ss.getString("groups", null);
        if (json != null) {
            collectionType = new TypeToken<LinkedHashSet<String>>() {
            }.getType();
            mGroups = gson.fromJson(json, collectionType);
        }
        mGroups.addAll(Arrays.asList(DEFAULT_GROUPS));

        // alphabetize, case insensitive
        List<String> groupList = new ArrayList<>(mGroups);
        Collections.sort(groupList, String.CASE_INSENSITIVE_ORDER);

        mSelectedGrpPos = ss.getInt("selected_grp_pos", mSelectedGrpPos);

        // do not clear messages for this selection on resume;
        // setAdapter and setSelection result in onItemSelected callback
        mSelectInInit = true;

        mGrpAdapter.clear();
        mGrpToMembersMap.clear();
        for (String group : groupList) {
            ArrayList<SmartcardApp> memberApps = Util.findGroupMembers(group, mApps);
            Collections.sort(memberApps, SmartcardApp.nameComparator);
            int pos = mGrpAdapter.addGroup(group, memberApps);
            mGrpToMembersMap.put(pos, memberApps);
        }
        mGrpSpinner.setAdapter(mGrpAdapter);
        mGrpSpinner.setSelection(mSelectedGrpPos);

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
        getMenuInflater().inflate(R.menu.activity_batch_select, menu);
        MenuItem item = menu.findItem(R.id.menu_share_msgs);
        mConsole.setShareProvider((ShareActionProvider) MenuItemCompat.getActionProvider(item));
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean drawerOpen = mNavDrawer.isOpen();
        MenuItem item = menu.findItem(R.id.menu_group_details);
        item.setVisible(!drawerOpen);
        item = menu.findItem(R.id.menu_add_group);
        item.setVisible(!drawerOpen);
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
            case R.id.menu_add_group: {
                // show new group dialog
                NewGroupDialogFragment.show(getFragmentManager(),
                        new NewGroupDialogFragment.OnNewGroupListener() {
                            @Override
                            public void onNewGroup(String name) {
                                if (Arrays.asList(DEFAULT_GROUPS).contains(name) ||
                                        mGroups.contains(name)) {
                                    Util.showToast(BatchSelectActivity.this,
                                            getString(R.string.group_exists, name));
                                    return;
                                }
                                Intent i = new Intent(BatchSelectActivity.this,
                                        GroupEditActivity.class);
                                i.setAction(ACTION_NEW_GROUP);
                                i.putExtra(EXTRA_GROUP_NAME, name);
                                i.putExtra(EXTRA_SELECT, true);
                                startActivity(i);
                            }
                        });
                return true;
            }
            case R.id.menu_group_details: {
                Intent i = new Intent(this, GroupViewActivity.class);
                i.setAction(ACTION_VIEW_GROUP);
                i.putExtra(EXTRA_GROUP_POS, mSelectedGrpPos);
                // select group copy when copy is initiated from group details
                i.putExtra(EXTRA_SELECT, true);
                startActivity(i);
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
            // two separators between taps/discoveries
            addMessageSeparator();
            addMessageSeparator();
        }
        // get IsoDep handle and run xcvr thread
        IsoDep isoDep = IsoDep.get(tag);
        if (isoDep == null) {
            onError(getString(R.string.wrong_tag_err));
        } else {
            List<SmartcardApp> memberApps = mGrpToMembersMap.get(mSelectedGrpPos);
            new Thread(new BatchReaderXcvr(isoDep, memberApps, this)).start();
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
        mEditor.putInt("selected_grp_pos", mSelectedGrpPos);
        mEditor.putInt("test_mode", TEST_MODE_BATCH_SELECT);
        mEditor.commit();
    }
}
