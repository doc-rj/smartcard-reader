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

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnGroupClickListener;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.docrj.smartcard.widget.AnimatedExpandableListView;
import org.docrj.smartcard.widget.AnimatedExpandableListView.AnimatedExpandableListAdapter;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;


public class GroupEditActivity extends ActionBarActivity {

    private static final String TAG = LaunchActivity.TAG;

    private static final String[] DEFAULT_GROUPS = SmartcardApp.GROUPS;

    // group actions
    private static final String ACTION_VIEW_GROUP = AppListActivity.ACTION_VIEW_GROUP;
    private static final String ACTION_NEW_GROUP = AppListActivity.ACTION_NEW_GROUP;
    private static final String ACTION_EDIT_GROUP = AppListActivity.ACTION_EDIT_GROUP;
    private static final String ACTION_COPY_GROUP = AppListActivity.ACTION_COPY_GROUP;

    // extras
    private static final String EXTRA_SELECT = AppListActivity.EXTRA_SELECT;
    private static final String EXTRA_GROUP_NAME = AppListActivity.EXTRA_GROUP_NAME;
    private static final String EXTRA_SOURCE_GROUP_NAME = AppListActivity.EXTRA_SOURCE_GROUP_NAME;

    private SharedPreferences.Editor mEditor;
    private AnimatedExpandableListView mGrpListView;
    private GroupAdapter mGrpAdapter;

    private String mAction;
    private ArrayList<SmartcardApp> mApps;
    // groups created by user (no payment/other)
    private HashSet<String> mUserGroups;
    private List<String> mSortedAllGroups;
    // batch select group idx
    private int mSelectedGrpPos;
    private String mSelectedGrpName;
    // app browse group idx
    private int mExpandedGrpPos;
    private String mExpandedGrpName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_edit);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        findViewById(R.id.actionbar_cancel).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // "cancel"
                        finish();
                    }
                });
        findViewById(R.id.actionbar_save).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // "save"
                        saveAndFinish(false);
                    }
                });

        mGrpListView = (AnimatedExpandableListView) findViewById(R.id.listView);
        // custom click handler so we can ignore and not collapse
        mGrpListView.setOnGroupClickListener(new OnGroupClickListener() {
            @Override
            public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
                return true;
            }
        });
        mGrpListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
                CheckBox checkBox = (CheckBox) v.findViewById(R.id.check_box);
                checkBox.toggle();
                GroupItem grp = mGrpAdapter.getGroup(groupPosition);
                grp.memberCount += checkBox.isChecked() ? 1 : -1;
                grp.member[childPosition] = checkBox.isChecked();
                mGrpAdapter.notifyDataSetChanged();
                return true;
            }
        });

        // persistent data in shared prefs
        SharedPreferences ss = getSharedPreferences("prefs", Context.MODE_PRIVATE);
        mEditor = ss.edit();

        Gson gson = new Gson();
        Type collectionType;

        String json = ss.getString("apps", null);
        if (json != null) {
            collectionType = new TypeToken<ArrayList<SmartcardApp>>() {
            }.getType();
            mApps = gson.fromJson(json, collectionType);
        }

        json = ss.getString("groups", null);
        if (json == null) {
            mUserGroups = new LinkedHashSet<>();
        } else {
            collectionType = new TypeToken<LinkedHashSet<String>>() {
            }.getType();
            mUserGroups = gson.fromJson(json, collectionType);
        }

        // alphabetize, case insensitive
        mSortedAllGroups = new ArrayList<>(mUserGroups);
        mSortedAllGroups.addAll(Arrays.asList(DEFAULT_GROUPS));
        Collections.sort(mSortedAllGroups, String.CASE_INSENSITIVE_ORDER);

        // when adding or removing a group, we may need to adjust group position indices for
        // batch select and app browse activities, which apply to the sorted list of groups
        mSelectedGrpPos = ss.getInt("selected_grp_pos", 0);
        mSelectedGrpName = mSortedAllGroups.get(mSelectedGrpPos);
        mExpandedGrpPos = ss.getInt("expanded_grp_pos", -1);
        mExpandedGrpName = (mExpandedGrpPos == -1) ? "" : mSortedAllGroups.get(mExpandedGrpPos);

        Intent intent = getIntent();
        mAction = intent.getAction();

        GroupItem groupItem = new GroupItem();
        groupItem.groupName = intent.getStringExtra(EXTRA_GROUP_NAME);
        groupItem.apps = mApps;
        // alphabetize member app list for group
        Collections.sort(groupItem.apps, SmartcardApp.nameComparator);

        if (savedInstanceState == null) {
            groupItem.memberCount = 0;
            groupItem.member = new boolean[groupItem.apps.size()];
            if (mAction == ACTION_EDIT_GROUP || mAction == ACTION_COPY_GROUP) {
                String srcGroupName = intent.getStringExtra(EXTRA_SOURCE_GROUP_NAME);
                String groupName = (srcGroupName == null) ? groupItem.groupName : srcGroupName;
                int i = 0;
                for (SmartcardApp app : groupItem.apps) {
                    if (Util.isGroupMember(groupName, app)) {
                        groupItem.member[i] = true;
                        groupItem.memberCount++;
                    }
                    i++;
                }
            }
        } else {
            groupItem.memberCount = 0;
            boolean[] member = savedInstanceState.getBooleanArray("member_array");
            for (int i = 0; i < member.length; i++) {
                if (member[i]) {
                    groupItem.memberCount++;
                }
            }
            groupItem.member = member;
        }

        List<GroupItem> groupItems = new ArrayList<>(1);
        groupItems.add(groupItem);

        mGrpAdapter = new GroupAdapter(this);
        mGrpAdapter.setData(groupItems);
        mGrpListView.setAdapter(mGrpAdapter);

        mGrpListView.expandGroup(0);
        mGrpListView.setSelectedGroup(0);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window w = getWindow();
            w.setFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS,
                    WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS |
                            WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            w.setStatusBarColor(getResources().getColor(R.color.primary_dark));
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        GroupItem groupItem = mGrpAdapter.getGroup(0);
        outState.putBooleanArray("member_array", groupItem.member);
    }

    @Override
    public void onBackPressed() {
        saveAndFinish(true);
        super.onBackPressed();
    }

    private void saveAndFinish(boolean backPressed) {
        GroupItem groupItem = mGrpAdapter.getGroup(0);
        if ((mAction == ACTION_NEW_GROUP || mAction == ACTION_COPY_GROUP) &&
                groupItem.memberCount == 0) {
            Util.showToast(this, getString(R.string.empty_group));
            if (backPressed) {
                finish();
            }
            return;
        }
        int i = 0;
        for (Boolean member : groupItem.member) {
            if (member) {
                groupItem.apps.get(i).addGroup(groupItem.groupName);
            } else {
                if (mAction == ACTION_EDIT_GROUP) {
                    groupItem.apps.get(i).removeGroup(groupItem.groupName);
                }
            }
            i++;
        }
        if (mAction == ACTION_NEW_GROUP || mAction == ACTION_COPY_GROUP) {
            mUserGroups.add(groupItem.groupName);
            mSortedAllGroups.add(groupItem.groupName);
            Collections.sort(mSortedAllGroups, String.CASE_INSENSITIVE_ORDER);
            // if inserting alphabetically "smaller" group name,
            // then adjust the saved group position indices
            boolean select = getIntent().getBooleanExtra(EXTRA_SELECT, false);
            if (select) {
                mSelectedGrpPos = mSortedAllGroups.indexOf(groupItem.groupName);
            } else
            if (groupItem.groupName.compareTo(mSelectedGrpName) < 0) {
                mSelectedGrpPos++;
            }
            if (groupItem.groupName.compareTo(mExpandedGrpName) < 0) {
                mExpandedGrpPos++;
            }
        }

        writePrefs();
        Util.showToast(this, getString(R.string.group_saved));

        // show group detail view after new or copy actions
        if (mAction == ACTION_NEW_GROUP || mAction == ACTION_COPY_GROUP) {
            Intent intent = new Intent(this, GroupViewActivity.class);
            intent.setAction(ACTION_VIEW_GROUP);
            intent.putExtra(EXTRA_GROUP_NAME, groupItem.groupName);
            startActivity(intent);
            // calling activity (copy-from group view) will finish
            setResult(RESULT_OK);
        }
        finish();
    }

    private void writePrefs() {
        // serialize list of apps
        Gson gson = new Gson();
        String json = gson.toJson(mApps);
        mEditor.putString("apps", json);
        // serialize hash set of user-added groups
        json = gson.toJson(mUserGroups);
        mEditor.putString("groups", json);
        // selected group in batch select mode
        mEditor.putInt("selected_grp_pos", mSelectedGrpPos);
        // expanded group in app browse
        mEditor.putInt("expanded_grp_pos", mExpandedGrpPos);
        mEditor.commit();
    }

    private static class GroupItem {
        String groupName;
        List<SmartcardApp> apps;
        boolean[] member;
        int memberCount;
    }

    private static class GroupHolder {
        TextView groupName;
        TextView memberCount;
    }

    private static class ChildHolder {
        ImageView appIcon;
        TextView appName;
        TextView appAid;
        CheckBox checkBox;
    }

    /**
     * Adapter for our list of {@link GroupItem}s.
     */
    private static class GroupAdapter extends AnimatedExpandableListAdapter {
        private Context context;
        private LayoutInflater inflater;
        private List<GroupItem> groupItems;

        public GroupAdapter(Context context) {
            inflater = LayoutInflater.from(context);
            this.context = context;
        }

        public void setData(List<GroupItem> items) {
            groupItems = items;
        }

        @Override
        public SmartcardApp getChild(int groupPosition, int childPosition) {
            return groupItems.get(groupPosition).apps.get(childPosition);
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            return childPosition;
        }

        @Override
        public View getRealChildView(int groupPosition, int childPosition,
                                     boolean isLastChild, View convertView, ViewGroup parent) {
            ChildHolder holder;
            GroupItem grp = getGroup(groupPosition);
            SmartcardApp app = getChild(groupPosition, childPosition);

            if (convertView == null) {
                holder = new ChildHolder();
                convertView = inflater.inflate(R.layout.group_edit_app_item, parent, false);
                holder.appIcon = (ImageView) convertView.findViewById(R.id.app_icon);
                holder.appName = (TextView) convertView.findViewById(R.id.app_name);
                holder.appAid = (TextView) convertView.findViewById(R.id.app_aid);
                holder.checkBox = (CheckBox) convertView.findViewById(R.id.check_box);
                convertView.setTag(holder);
            } else {
                holder = (ChildHolder) convertView.getTag();
            }

            Drawable img;
            if (app.getType() == SmartcardApp.TYPE_PAYMENT) {
                img = context.getResources().getDrawable(R.drawable.credit_card_green);
            } else {
                img = context.getResources().getDrawable(R.drawable.credit_card_blue);
            }
            holder.appIcon.setImageDrawable(img);
            holder.appName.setText(app.getName());
            holder.appAid.setText(app.getAid());
            holder.checkBox.setChecked(grp.member[childPosition]);
            return convertView;
        }

        @Override
        public int getRealChildrenCount(int groupPosition) {
            return groupItems.get(groupPosition).apps.size();
        }

        @Override
        public GroupItem getGroup(int groupPosition) {
            return groupItems.get(groupPosition);
        }

        @Override
        public int getGroupCount() {
            return groupItems.size();
        }

        @Override
        public long getGroupId(int groupPosition) {
            return groupPosition;
        }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded,
                                 View convertView, ViewGroup parent) {
            GroupHolder holder;
            GroupItem groupItem = getGroup(groupPosition);

            if (convertView == null) {
                holder = new GroupHolder();
                convertView = inflater.inflate(R.layout.apps_list_group_item, parent, false);
                holder.groupName = (TextView) convertView.findViewById(R.id.group_name);
                holder.memberCount = (TextView) convertView.findViewById(R.id.member_count);
                convertView.setTag(holder);
            } else {
                holder = (GroupHolder) convertView.getTag();
            }

            holder.groupName.setText(groupItem.groupName);
            holder.memberCount.setText("(" + groupItem.memberCount + ")");
            return convertView;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public boolean isChildSelectable(int arg0, int arg1) {
            return true;
        }
    }
}
