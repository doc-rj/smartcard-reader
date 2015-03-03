package org.docrj.smartcard.reader;

import android.support.v7.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.afollestad.materialdialogs.AlertDialogWrapper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;


public class AppViewActivity extends ActionBarActivity {

    private static final String TAG = LaunchActivity.TAG;

    // actions
    private static final String ACTION_VIEW_APP = AppsListActivity.ACTION_VIEW_APP;
    private static final String ACTION_EDIT_APP = AppsListActivity.ACTION_EDIT_APP;
    private static final String ACTION_COPY_APP = AppsListActivity.ACTION_COPY_APP;

    // extras
    private static final String EXTRA_APP_POS = AppsListActivity.EXTRA_APP_POS;

    // dialogs
    private static final int DIALOG_CONFIRM_DELETE = 0;

    // requests
    private static final int REQUEST_COPY_APP = 0;

    private SharedPreferences.Editor mEditor;
    private ArrayList<SmartcardApp> mApps;
    private int mSelectedAppPos;
    private int mAppPos;
    private boolean mReadOnly;

    private EditText mName;
    private EditText mAid;
    private RadioGroup mType;
    private TextView mNote;
    private AlertDialog mConfirmDeleteDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final ActionBar actionBar = getSupportActionBar();
        View titleView = getLayoutInflater().inflate(R.layout.app_title, null);
        TextView titleText = (TextView) titleView.findViewById(R.id.title);
        titleText.setText(getString(R.string.smartcard_app));
        actionBar.setCustomView(titleView);
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM
                /*| ActionBar.DISPLAY_SHOW_HOME*/ | ActionBar.DISPLAY_HOME_AS_UP);

        setContentView(R.layout.activity_app_view);

        // persistent data in shared prefs
        SharedPreferences ss = getSharedPreferences("prefs", Context.MODE_PRIVATE);
        mEditor = ss.edit();

        mSelectedAppPos = ss.getInt("selected_app_pos", 0);

        Intent intent = getIntent();
        mAppPos = intent.getIntExtra(EXTRA_APP_POS, 0);

        mName = (EditText) findViewById(R.id.app_name);
        mAid = (EditText) findViewById(R.id.app_aid);
        mType = (RadioGroup) findViewById(R.id.radio_grp_type);
        mNote = (TextView) findViewById(R.id.note);

        // view only
        mName.setFocusable(false);
        mAid.setFocusable(false);
        for (int i = 0; i < mType.getChildCount(); i++) {
            mType.getChildAt(i).setClickable(false);
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

    @SuppressWarnings("deprecation")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                Intent i = NavUtils.getParentActivityIntent(this);
                NavUtils.navigateUpTo(this, i);
                return true;

            case R.id.menu_edit_app:
                edit_app();
                return true;

            case R.id.menu_copy_app:
                copy_app();
                return true;

            case R.id.menu_delete_app:
                showDialog(DIALOG_CONFIRM_DELETE);
                return true;

            case R.id.menu_select_app:
                select_app();
                return true;

            case R.id.menu_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void edit_app() {
        Intent i = new Intent(this, AppEditActivity.class);
        i.setAction(ACTION_EDIT_APP);
        i.putExtra(EXTRA_APP_POS, mAppPos);
        startActivity(i);
    }

    private void copy_app() {
        Intent i = new Intent(this, AppEditActivity.class);
        i.setAction(ACTION_COPY_APP);
        i.putExtra(EXTRA_APP_POS, mAppPos);
        startActivityForResult(i, REQUEST_COPY_APP);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_COPY_APP) {
            if (resultCode == RESULT_OK) {
                // copy-to app view created successfully, and it
                // replaces this copy-from app view in the stack
                finish();
            }
        }
    }

    private void delete_app() {
        if (mSelectedAppPos == mAppPos) {
            mSelectedAppPos = 0;
        } else if (mSelectedAppPos > mAppPos) {
            mSelectedAppPos--;
        }
        mApps.remove(mAppPos);
        writePrefs();
        finish();
    }

    private void select_app() {
        mSelectedAppPos = mAppPos;
        mEditor.putInt("selected_app_pos", mSelectedAppPos);
        mEditor.commit();
        new Launcher(this).launch(Launcher.TEST_MODE_AID_ROUTE, true, true);
        finish();
    }

    @SuppressWarnings("deprecation")
    @Override
    protected Dialog onCreateDialog(int id) {
        //AlertDialog.Builder builder = new AlertDialog.Builder(
        //        this, R.style.dialog);
        AlertDialogWrapper.Builder builder = new AlertDialogWrapper.Builder(this);
        final LayoutInflater li = getLayoutInflater();
        Dialog dialog = null;
        switch (id) {
            case DIALOG_CONFIRM_DELETE:
                //final View view = li.inflate(R.layout.dialog_confirm_delete, null);
                builder//.setView(view)
                        .setCancelable(true)
                        .setIcon(R.drawable.ic_action_delete_gray)
                        .setTitle(mName.getText())
                        .setMessage(R.string.confirm_delete)
                        .setPositiveButton(R.string.dialog_ok,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,
                                                        int id) {
                                        delete_app();
                                    }
                                })
                        .setNegativeButton(R.string.dialog_cancel,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,
                                                        int id) {
                                        dialog.cancel();
                                    }
                                });
                mConfirmDeleteDialog = builder.create();
                dialog = mConfirmDeleteDialog;
                break;
        }
        return dialog;
    }

    private void writePrefs() {
        // serialize list of SmartcardApp
        Gson gson = new Gson();
        String json = gson.toJson(mApps);
        mEditor.putString("apps", json);
        mEditor.putInt("selected_app_pos", mSelectedAppPos);
        mEditor.commit();
    }
}
