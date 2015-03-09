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
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ListView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;


public class AppsListActivity extends ActionBarActivity {

    private static final String TAG = LaunchActivity.TAG;

    // actions
    static final String ACTION_NEW_APP = "org.docrj.smartcard.reader.action_new_app";
    static final String ACTION_VIEW_APP = "org.docrj.smartcard.reader.action_view_app";
    static final String ACTION_EDIT_APP = "org.docrj.smartcard.reader.action_edit_app";
    static final String ACTION_COPY_APP = "org.docrj.smartcard.reader.action_copy_app";

    // extras
    static final String EXTRA_APP_POS = "org.docrj.smartcard.reader.app_pos";

    private ListView mAppListView;
    private ArrayList<SmartcardApp> mApps;
    private int mSelectedAppPos = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_apps_list);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        mAppListView = (ListView) findViewById(R.id.listView);
        mAppListView.setOnItemClickListener(new ListView.OnItemClickListener() {
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
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.abc_fade_in,
                R.anim.abc_fade_out);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_apps_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_new_app:
                Intent i = new Intent(this, AppEditActivity.class);
                i.setAction(ACTION_NEW_APP);
                startActivity(i);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
