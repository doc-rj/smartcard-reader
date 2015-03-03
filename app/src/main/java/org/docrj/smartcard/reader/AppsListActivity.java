package org.docrj.smartcard.reader;

import android.support.v7.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;


public class AppsListActivity extends ActionBarActivity {

    private static final String TAG = LaunchActivity.TAG;

    private static final int NUM_RO_APPS = AidRouteActivity.NUM_RO_APPS;

    // actions
    static final String ACTION_NEW_APP = "org.docrj.smartcard.reader.action_new_app";
    static final String ACTION_VIEW_APP = "org.docrj.smartcard.reader.action_view_app";
    static final String ACTION_EDIT_APP = "org.docrj.smartcard.reader.action_edit_app";
    static final String ACTION_COPY_APP = "org.docrj.smartcard.reader.action_copy_app";

    // extras
    static final String EXTRA_APP_POS = "org.docrj.smartcard.reader.app_pos";

    private ListView mAppListView;
    private SharedPreferences.Editor mEditor;
    private ArrayList<SmartcardApp> mApps;
    private int mSelectedAppPos = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final ActionBar actionBar = getSupportActionBar();
        View titleView = getLayoutInflater().inflate(R.layout.app_title, null);
        TextView titleText = (TextView) titleView.findViewById(R.id.title);
        titleText.setText(getString(R.string.smartcard_apps));
        actionBar.setCustomView(titleView);
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM
                /*| ActionBar.DISPLAY_SHOW_HOME*/ | ActionBar.DISPLAY_HOME_AS_UP);

        setContentView(R.layout.activity_apps_list);

        final ListView listView = (ListView) findViewById(R.id.listView);
        mAppListView = listView;
        listView.setOnItemClickListener(new ListView.OnItemClickListener() {
            @SuppressWarnings("deprecation")
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int pos, long id) {
                // view app
                Intent i = new Intent(AppsListActivity.this, AppViewActivity.class);
                i.setAction(ACTION_VIEW_APP);
                i.putExtra(EXTRA_APP_POS, pos);
                startActivity(i);
            }
        });

        // persistent data in shared prefs
        SharedPreferences ss = getSharedPreferences("prefs", Context.MODE_PRIVATE);
        mEditor = ss.edit();
    }

    @Override
    public void onResume() {
        super.onResume();

        // restore persistent data
        SharedPreferences ss = getSharedPreferences("prefs", Context.MODE_PRIVATE);
        mSelectedAppPos = ss.getInt("selected_app_pos", mSelectedAppPos);

        String json = ss.getString("apps", null);
        if (json != null) {
            // deserialize list of SmartcardApp
            Gson gson = new Gson();
            Type collectionType = new TypeToken<ArrayList<SmartcardApp>>() {
            }.getType();
            mApps = gson.fromJson(json, collectionType);
        }

        AppAdapter appAdapter = new AppAdapter(this, mApps, false);
        mAppListView.setAdapter(appAdapter);
        appAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_apps_list, menu);
        return true;
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;

            case R.id.menu_new_app:
                Intent i = new Intent(this, AppEditActivity.class);
                i.setAction(ACTION_NEW_APP);
                startActivity(i);
                return true;

            case R.id.menu_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void writePrefs() {
        // serialize list of SmartcardApp
        Gson gson = new Gson();
        String json = gson.toJson(mApps);
        mEditor.putString("apps", json);
        mEditor.putInt("selected_app_pos", mSelectedAppPos);
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
