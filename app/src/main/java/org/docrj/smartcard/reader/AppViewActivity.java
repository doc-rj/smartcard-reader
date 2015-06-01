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

import android.os.Build;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.afollestad.materialdialogs.AlertDialogWrapper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;


public class AppViewActivity extends ActionBarActivity {

    private static final String TAG = LaunchActivity.TAG;

    private static final String[] DEFAULT_GROUPS = SmartcardApp.GROUPS;

    // actions
    private static final String ACTION_EDIT_APP = AppListActivity.ACTION_EDIT_APP;
    private static final String ACTION_COPY_APP = AppListActivity.ACTION_COPY_APP;

    // extras
    private static final String EXTRA_APP_POS = AppListActivity.EXTRA_APP_POS;
    private static final String EXTRA_SELECT = AppListActivity.EXTRA_SELECT;

    // dialogs
    private static final int DIALOG_CONFIRM_DELETE = 0;

    // requests
    private static final int REQUEST_EDIT_APP = 0;
    private static final int REQUEST_COPY_APP = 1;

    private SharedPreferences.Editor mEditor;
    private ArrayList<SmartcardApp> mApps;
    private int mSelectedAppPos;
    private int mAppPos;
    private boolean mReadOnly;

    private HashSet<String> mUserGroups;
    private List<String> mSortedAllGroups;
    private int mSelectedGrpPos;     // batch select group idx
    private String mSelectedGrpName;
    private int mExpandedGrpPos;     // app browse group idx
    private String mExpandedGrpName;

    private EditText mName;
    private EditText mAid;
    private RadioGroup mType;
    private TextView mGroups;
    private TextView mNote;
    private AlertDialog mConfirmDeleteDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_view);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        // persistent data in shared prefs
        SharedPreferences ss = getSharedPreferences("prefs", Context.MODE_PRIVATE);
        mEditor = ss.edit();

        mSelectedAppPos = ss.getInt("selected_app_pos", 0);

        Gson gson = new Gson();
        String json = ss.getString("groups", null);
        if (json == null) {
            mUserGroups = new LinkedHashSet<>();
        } else {
            Type collectionType = new TypeToken<LinkedHashSet<String>>() {
            }.getType();
            mUserGroups = gson.fromJson(json, collectionType);
        }

        // alphabetize, case insensitive
        mSortedAllGroups = new ArrayList<>(mUserGroups);
        mSortedAllGroups.addAll(Arrays.asList(DEFAULT_GROUPS));
        Collections.sort(mSortedAllGroups, String.CASE_INSENSITIVE_ORDER);

        // when deleting an app results in an empty group, we remove the group;
        // we may need to adjust group position indices for batch select and app
        // browse activities, which apply to the sorted list of groups
        mSelectedGrpPos = ss.getInt("selected_grp_pos", 0);
        mSelectedGrpName = mSortedAllGroups.get(mSelectedGrpPos);
        mExpandedGrpPos = ss.getInt("expanded_grp_pos", -1);
        mExpandedGrpName = (mExpandedGrpPos == -1) ? "" : mSortedAllGroups.get(mExpandedGrpPos);

        Intent intent = getIntent();
        mAppPos = intent.getIntExtra(EXTRA_APP_POS, 0);

        mName = (EditText) findViewById(R.id.app_name);
        mAid = (EditText) findViewById(R.id.app_aid);
        mType = (RadioGroup) findViewById(R.id.radio_grp_type);
        mNote = (TextView) findViewById(R.id.note);
        mGroups = (TextView) findViewById(R.id.group_list);

        // view only
        mName.setFocusable(false);
        mAid.setFocusable(false);
        for (int i = 0; i < mType.getChildCount(); i++) {
            mType.getChildAt(i).setClickable(false);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window w = getWindow();
            w.setFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS,
                    WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS |
                            WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            w.setStatusBarColor(getResources().getColor(R.color.primary_dark));
        }
    }

    @Override
    public void onResume() {
        SharedPreferences ss = getSharedPreferences("prefs", Context.MODE_PRIVATE);
        String json = ss.getString("apps", null);
        if (json != null) {
            // deserialize list of SmartcardApp
            Gson gson = new Gson();
            Type collectionType = new TypeToken<ArrayList<SmartcardApp>>() {
            }.getType();
            mApps = gson.fromJson(json, collectionType);
        }

        SmartcardApp app = mApps.get(mAppPos);
        mReadOnly = app.isReadOnly();
        mName.setText(app.getName());
        mAid.setText(app.getAid());
        mType.check((app.getType() == SmartcardApp.TYPE_OTHER) ? R.id.radio_other
                : R.id.radio_payment);
        updateGroups(mApps.get(mAppPos).getGroups());
        if (!mReadOnly) {
            mNote.setVisibility(View.GONE);
        }
        super.onResume();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mConfirmDeleteDialog != null) {
            mConfirmDeleteDialog.dismiss();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_app_view, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // handle read-only apps
        if (mReadOnly) {
            MenuItem editMenuItem = menu.findItem(R.id.menu_edit_app);
            editMenuItem.setVisible(false);
            MenuItem delMenuItem = menu.findItem(R.id.menu_delete_app);
            delMenuItem.setVisible(false);
        }
        return true;
    }

    private void updateGroups(HashSet<String> appGroups) {
        String grpText = getString(R.string.none);
        if (appGroups.size() == 0) {
            if (!mReadOnly) {
                grpText += " - " + getString(R.string.edit_to_add_groups);
            }
        } else
        if (appGroups.size() == 1) {
            grpText = appGroups.toString().replaceAll("[\\[\\]]", "");;
            if (!mReadOnly) {
                grpText += " - " + getString(R.string.edit_to_add_groups);
            }
        } else {
            grpText = appGroups.toString().replaceAll("[\\[\\]]", "");
            grpText = grpText.replace(", ", ",\n");
        }
        mGroups.setText(grpText);
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                Intent i = NavUtils.getParentActivityIntent(this);
                NavUtils.navigateUpTo(this, i);
                return true;

            case R.id.menu_edit_app:
                editApp();
                return true;

            case R.id.menu_copy_app:
                copyApp();
                return true;

            case R.id.menu_delete_app:
                showDialog(DIALOG_CONFIRM_DELETE);
                return true;

            case R.id.menu_select_app:
                selectApp();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void editApp() {
        Intent i = new Intent(this, AppEditActivity.class);
        i.setAction(ACTION_EDIT_APP);
        i.putExtra(EXTRA_APP_POS, mAppPos);
        startActivityForResult(i, REQUEST_EDIT_APP);
    }

    private void copyApp() {
        Intent i = new Intent(this, AppEditActivity.class);
        i.setAction(ACTION_COPY_APP);
        i.putExtra(EXTRA_APP_POS, mAppPos);
        if (getIntent().getBooleanExtra(EXTRA_SELECT, false)) {
            i.putExtra(EXTRA_SELECT, true);
        }
        startActivityForResult(i, REQUEST_COPY_APP);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_EDIT_APP || requestCode == REQUEST_COPY_APP) {
            if (resultCode == RESULT_OK) {
                // copy-to app view created successfully, and it
                // replaces this copy-from app view in the stack
                finish();
            }
        }
    }

    private void deleteApp() {
        // adjust selected position for app select mode
        if (mSelectedAppPos == mAppPos) {
            mSelectedAppPos = 0;
        } else if (mSelectedAppPos > mAppPos) {
            mSelectedAppPos--;
        }
        // remove app from list
        SmartcardApp app = mApps.remove(mAppPos);
        HashSet<String> groups = app.getGroups();
        // only bother to adjust groups if app was assigned to
        // more than the default other/payment group
        if (groups.size() > 1) {
            for (String group : groups) {
                if (Util.isGroupEmpty(group, mApps)) {
                    removeGroup(group);
                }
            }
        }
        writePrefs();
        finish();
    }

    private void selectApp() {
        mSelectedAppPos = mAppPos;
        mEditor.putInt("selected_app_pos", mSelectedAppPos);
        mEditor.commit();
        new Launcher(this).launch(Launcher.TEST_MODE_APP_SELECT, true, true);
        finish();
    }

    @SuppressWarnings("deprecation")
    @Override
    protected Dialog onCreateDialog(int id) {
        //AlertDialog.Builder builder = new AlertDialog.Builder(
        //        this, R.style.dialog);
        AlertDialogWrapper.Builder builder = new AlertDialogWrapper.Builder(this);
        //final LayoutInflater li = getLayoutInflater();
        Dialog dialog = null;
        switch (id) {
            case DIALOG_CONFIRM_DELETE:
                //final View view = li.inflate(R.layout.dialog_confirm_delete, null);
                builder//.setView(view)
                        .setCancelable(true)
                        .setIcon(R.drawable.ic_action_delete_gray)
                        .setTitle(mName.getText())
                        .setMessage(R.string.confirm_delete_app)
                        .setPositiveButton(R.string.dialog_ok,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,
                                                        int id) {
                                        deleteApp();
                                    }
                                })
                        .setNegativeButton(R.string.dialog_cancel,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,
                                                        int id) {
                                        dialog.cancel();
                                    }
                                });
                mConfirmDeleteDialog = (AlertDialog) builder.create();
                dialog = mConfirmDeleteDialog;
                break;
        }
        return dialog;
    }

    private void writePrefs() {
        // serialize list of apps
        Gson gson = new Gson();
        String json = gson.toJson(mApps);
        mEditor.putString("apps", json);
        // selected app in app select mode
        mEditor.putInt("selected_app_pos", mSelectedAppPos);
        // serialize hash set of user-added groups
        json = gson.toJson(mUserGroups);
        mEditor.putString("groups", json);
        // selected group in batch select mode
        mEditor.putInt("selected_grp_pos", mSelectedGrpPos);
        // expanded group in app browse
        mEditor.putInt("expanded_grp_pos", mExpandedGrpPos);
        mEditor.commit();
    }

    private void removeGroup(String name) {
        // remove from saved hash set
        mUserGroups.remove(name);
        // adjust selected group position indices as needed
        if (name.equals(mSelectedGrpName)) {
            // always guaranteed at least two groups: other and payment
            mSelectedGrpName = (mSelectedGrpPos == 0) ?
                    mSortedAllGroups.get(1) : mSortedAllGroups.get(0);
            mSelectedGrpPos = 0;
        } else
        if (name.compareTo(mSelectedGrpName) < 0) {
            mSelectedGrpPos--;
            mSelectedGrpName = mSortedAllGroups.get(mSelectedGrpPos);
        }
        // adjust expanded group position index as needed
        if (name.equals(mExpandedGrpName)) {
            // no expanded group; all collapsed
            mExpandedGrpPos = -1;
            mExpandedGrpName = "";
        } else
        if (name.compareTo(mExpandedGrpName) < 0) {
            mExpandedGrpPos--;
            mExpandedGrpName = mSortedAllGroups.get(mExpandedGrpPos);
        }
        // remove from sorted list
        mSortedAllGroups.remove(name);
    }
}
