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
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnGroupClickListener;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.docrj.smartcard.widget.AnimatedExpandableListView;
import org.docrj.smartcard.widget.AnimatedExpandableListView.AnimatedExpandableListAdapter;

import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;


public class AppListActivity extends ActionBarActivity {

    private static final String TAG = LaunchActivity.TAG;

    private static final String[] DEFAULT_GROUPS = SmartcardApp.GROUPS;

    // actions
    static final String ACTION_VIEW_APP = "org.docrj.smartcard.reader.action_view_app";
    static final String ACTION_NEW_APP = "org.docrj.smartcard.reader.action_new_app";
    static final String ACTION_EDIT_APP = "org.docrj.smartcard.reader.action_edit_app";
    static final String ACTION_COPY_APP = "org.docrj.smartcard.reader.action_copy_app";

    static final String ACTION_VIEW_GROUP = "org.docrj.smartcard.reader.action_view_group";
    static final String ACTION_NEW_GROUP = "org.docrj.smartcard.reader.action_new_group";
    static final String ACTION_EDIT_GROUP = "org.docrj.smartcard.reader.action_edit_group";
    static final String ACTION_COPY_GROUP = "org.docrj.smartcard.reader.action_copy_group";

    // extras
    static final String EXTRA_SELECT = "org.docrj.smartcard.reader.select";
    static final String EXTRA_APP_POS = "org.docrj.smartcard.reader.app_pos";
    static final String EXTRA_GROUP_POS = "org.docrj.smartcard.reader.group_pos";
    static final String EXTRA_GROUP_NAME = "org.docrj.smartcard.reader.group_name";
    static final String EXTRA_SOURCE_GROUP_NAME = "org.docrj.smartcard.reader.source_group_name";

    private SharedPreferences.Editor mEditor;
    private AnimatedExpandableListView mGrpListView;

    // groups created by user (no payment/other)
    private HashSet<String> mGroups;
    private ArrayList<SmartcardApp> mApps;
    private GroupAdapter mGrpAdapter;
    private int mExpandedGrp = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_list);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        mGrpListView = (AnimatedExpandableListView) findViewById(R.id.listView);
        // custom click handler so we can animate
        mGrpListView.setOnGroupClickListener(new OnGroupClickListener() {
            @Override
            public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
                if (mGrpListView.isGroupExpanded(groupPosition)) {
                    mGrpListView.collapseGroupWithAnimation(groupPosition);
                    mExpandedGrp = -1;
                } else {
                    if (mExpandedGrp != groupPosition) {
                        mGrpListView.collapseGroupWithAnimation(mExpandedGrp);
                    }
                    mGrpListView.expandGroupWithAnimation(groupPosition);
                    mExpandedGrp = groupPosition;
                }
                return true;
            }
        });

        mGrpListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                // convert flat position to group or child
                long packedPos = mGrpListView.getExpandableListPosition(position);
                int type = AnimatedExpandableListView.getPackedPositionType(packedPos);
                if (type == AnimatedExpandableListView.PACKED_POSITION_TYPE_GROUP) {
                    int groupPos = AnimatedExpandableListView.getPackedPositionGroup(packedPos);
                    // first expand group if not yet expanded; this makes for a nicer
                    // transition to and from the group detail view, as this gives the
                    // group focus, and shows group expanding to match the detail view
                    if (!mGrpListView.isGroupExpanded(groupPos)) {
                        mGrpListView.performItemClick(view, position, id);
                    }
                    // start group view activity
                    Intent i = new Intent(AppListActivity.this, GroupViewActivity.class);
                    i.setAction(ACTION_VIEW_GROUP);
                    i.putExtra(EXTRA_GROUP_NAME, mGrpAdapter.getGroup(groupPos).groupName);
                    startActivity(i);
                    return true;
                }
                return false;
            }
        });

        mGrpListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
                SmartcardApp app = mGrpAdapter.getChild(groupPosition, childPosition);
                // view app
                Intent i = new Intent(AppListActivity.this, AppViewActivity.class);
                i.setAction(ACTION_VIEW_APP);
                i.putExtra(EXTRA_APP_POS, mGrpAdapter.getGroup(groupPosition).appToPosMap.get(app));
                startActivity(i);
                return true;
            }
        });

        // floating action menu and accompanying scrim
        final RelativeLayout scrim = (RelativeLayout) findViewById(R.id.scrim);
        final FloatingActionMenu fam = (FloatingActionMenu) findViewById(R.id.fam);
        fam.setClosedOnTouchOutside(true);
        fam.setOnMenuToggleListener(new FloatingActionMenu.OnMenuToggleListener() {
            @Override
            public void onMenuToggle(boolean opened) {
                if (opened) {
                    // fade in dim layout under the floating buttons
                    scrim.setVisibility(View.VISIBLE);
                    Animation fadeIn = AnimationUtils.loadAnimation(AppListActivity.this,
                            R.anim.abc_fade_in);
                    scrim.startAnimation(fadeIn);
                } else {
                    // fade out back to normal
                    Animation fadeOut = AnimationUtils.loadAnimation(AppListActivity.this,
                            R.anim.abc_fade_out);
                    scrim.startAnimation(fadeOut);
                    scrim.setVisibility(View.INVISIBLE);
                }
            }
        });

        FloatingActionButton appFab = (FloatingActionButton) findViewById(R.id.fab_app);
        appFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // start activity to add new app
                Intent i = new Intent(AppListActivity.this, AppEditActivity.class);
                i.setAction(ACTION_NEW_APP);
                startActivity(i);
                // close floating action menu
                fam.toggle(false);
            }
        });

        FloatingActionButton grpFab = (FloatingActionButton) findViewById(R.id.fab_group);
        grpFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // show new group dialog
                NewGroupDialogFragment.show(getFragmentManager(),
                        new NewGroupDialogFragment.OnNewGroupListener() {
                            @Override
                            public void onNewGroup(String name) {
                                if (Arrays.asList(DEFAULT_GROUPS).contains(name) ||
                                    mGroups.contains(name)) {
                                    Util.showToast(AppListActivity.this,
                                            getString(R.string.group_exists, name));
                                    return;
                                }
                                Intent i = new Intent(AppListActivity.this, GroupEditActivity.class);
                                i.setAction(ACTION_NEW_GROUP);
                                i.putExtra(EXTRA_GROUP_NAME, name);
                                startActivity(i);
                            }
                        });
                // close floating action menu
                fam.toggle(false);
            }
        });

        // persistent data in shared prefs
        SharedPreferences ss = getSharedPreferences("prefs", Context.MODE_PRIVATE);
        mEditor = ss.edit();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window w = getWindow();
            w.setFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS,
                    WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS |
                            WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            w.setStatusBarColor(getResources().getColor(R.color.primary_dark));
        }
    }

    @Override
    public void onPause() {
        writePrefs();
        super.onPause();
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

        json = ss.getString("groups", null);
        if (json == null) {
            mGroups = new LinkedHashSet<>();
        } else {
            collectionType = new TypeToken<LinkedHashSet<String>>() {
            }.getType();
            mGroups = gson.fromJson(json, collectionType);
        }
        mGroups.addAll(Arrays.asList(DEFAULT_GROUPS));

        List<GroupItem> groupItems = new ArrayList<>();
        for (String group : mGroups) {
            GroupItem groupItem = new GroupItem();
            groupItem.groupName = group;
            groupItem.appToPosMap = Util.mapGroupMembers(group, mApps);
            groupItem.memberApps = new ArrayList<>(groupItem.appToPosMap.keySet());
            // alphabetize member app list for each group
            Collections.sort(groupItem.memberApps, SmartcardApp.nameComparator);
            groupItems.add(groupItem);
        }
        // alphabetize group list
        Collections.sort(groupItems, GroupItem.nameComparator);

        mGrpAdapter = new GroupAdapter(this);
        mGrpAdapter.setData(groupItems);
        mGrpListView.setAdapter(mGrpAdapter);

        mExpandedGrp = ss.getInt("expanded_grp_pos", -1);
        if (mExpandedGrp != -1) {
            mGrpListView.expandGroup(mExpandedGrp);
            mGrpListView.setSelectedGroup(mExpandedGrp);
        }
    }

    @Override
    public void onBackPressed() {
        // collapse; will write to prefs on pause
        mExpandedGrp = -1;
        super.onBackPressed();
        overridePendingTransition(R.anim.abc_fade_in, R.anim.abc_fade_out);
    }

    private void writePrefs() {
        mEditor.putInt("expanded_grp_pos", mExpandedGrp);
        mEditor.commit();
    }

    private static class GroupItem {
        String groupName;
        // mapping of app to its position in the apps list
        HashMap<SmartcardApp, Integer> appToPosMap;
        List<SmartcardApp> memberApps;

        // for static reference
        static Comparator<GroupItem> nameComparator =
                new Comparator<GroupItem>() {
                    @Override
                    public int compare(GroupItem gi1, GroupItem gi2) {
                        // alphabetize in ascending order, case insensitive
                        return gi1.groupName.toLowerCase().compareTo(gi2.groupName.toLowerCase());
                    }
                };
    }

    private static class GroupHolder {
        TextView groupName;
        TextView memberCount;
    }

    private static class ChildHolder {
        ImageView appIcon;
        TextView appName;
        TextView appAid;
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
            return groupItems.get(groupPosition).memberApps.get(childPosition);
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            return childPosition;
        }

        @Override
        public View getRealChildView(int groupPosition, int childPosition,
                                     boolean isLastChild, View convertView, ViewGroup parent) {
            ChildHolder holder;
            SmartcardApp app = getChild(groupPosition, childPosition);

            if (convertView == null) {
                holder = new ChildHolder();
                convertView = inflater.inflate(R.layout.apps_list_app_item, parent, false);
                holder.appIcon = (ImageView) convertView.findViewById(R.id.app_icon);
                holder.appName = (TextView) convertView.findViewById(R.id.app_name);
                holder.appAid = (TextView) convertView.findViewById(R.id.app_aid);
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
            return convertView;
        }

        @Override
        public int getRealChildrenCount(int groupPosition) {
            return groupItems.get(groupPosition).memberApps.size();
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
            holder.memberCount.setText("(" + groupItem.memberApps.size() + ")");
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
