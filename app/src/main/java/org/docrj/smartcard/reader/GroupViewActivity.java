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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.TextView;

import com.afollestad.materialdialogs.AlertDialogWrapper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.docrj.smartcard.widget.AnimatedExpandableListView;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;


public class GroupViewActivity extends ActionBarActivity {

    private static final String TAG = LaunchActivity.TAG;

    private static final String[] DEFAULT_GROUPS = SmartcardApp.GROUPS;

    // actions
    private static final String ACTION_VIEW_APP = AppListActivity.ACTION_VIEW_APP;
    private static final String ACTION_EDIT_GROUP = AppListActivity.ACTION_EDIT_GROUP;
    private static final String ACTION_COPY_GROUP = AppListActivity.ACTION_COPY_GROUP;

    // extras
    private static final String EXTRA_SELECT = AppListActivity.EXTRA_SELECT;
    private static final String EXTRA_APP_POS = AppListActivity.EXTRA_APP_POS;
    private static final String EXTRA_GROUP_POS = AppListActivity.EXTRA_GROUP_POS;
    private static final String EXTRA_GROUP_NAME = AppListActivity.EXTRA_GROUP_NAME;
    private static final String EXTRA_SOURCE_GROUP_NAME = AppListActivity.EXTRA_SOURCE_GROUP_NAME;

    // dialogs
    private static final int DIALOG_CONFIRM_DELETE = 0;

    // requests (start activity for result)
    private static final int REQUEST_COPY_GROUP = 1;

    private SharedPreferences.Editor mEditor;
    private AnimatedExpandableListView mGrpListView;
    private GroupAdapter mGrpAdapter;
    private String mGroupName;
    private boolean mReadOnly;

    // sorted group list idx
    private int mGrpPos;
    // full apps list
    List<SmartcardApp> mApps;
    // member apps list
    List<SmartcardApp> mMemberApps;
    // groups created by user (no payment/other)
    private HashSet<String> mUserGroups;
    private List<String> mSortedAllGroups;
    // batch select group idx
    private int mSelectedGrpPos;
    // app browse group idx
    private int mExpandedGrpPos;

    private TextView mNote;
    private AlertDialog mConfirmDeleteDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_view);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        mNote = (TextView) findViewById(R.id.note);
        mGrpListView = (AnimatedExpandableListView) findViewById(R.id.listView);

        // custom click handler so we can ignore and not collapse
        mGrpListView.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {
            @Override
            public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
                return true;
            }
        });

        mGrpListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition,
                                        int childPosition, long id) {
                SmartcardApp app = mGrpAdapter.getChild(groupPosition, childPosition);
                // view app
                Intent i = new Intent(GroupViewActivity.this, AppViewActivity.class);
                i.setAction(ACTION_VIEW_APP);
                i.putExtra(EXTRA_APP_POS, mApps.indexOf(app));
                startActivity(i);
                return true;
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
    public void onResume() {
        super.onResume();
        SharedPreferences ss = getSharedPreferences("prefs", Context.MODE_PRIVATE);

        Gson gson = new Gson();
        Type collectionType;
        String json = ss.getString("apps", null);
        if (json == null) {
            mApps = new ArrayList<>();
        } else {
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

        Intent intent = getIntent();
        mGroupName = intent.getStringExtra(EXTRA_GROUP_NAME);
        if (mGroupName == null) {
            mGrpPos = intent.getIntExtra(EXTRA_GROUP_POS, 0);
            mGroupName = mSortedAllGroups.get(mGrpPos);
        } else {
            mGrpPos = mSortedAllGroups.indexOf(mGroupName);
        }
        mReadOnly = Util.isDefaultGroup(mGroupName);

        // when adding or removing a group, we may need to adjust group position indices for
        // batch select and app browse activities, which apply to the sorted list of groups
        mSelectedGrpPos = ss.getInt("selected_grp_pos", 0);
        mExpandedGrpPos = ss.getInt("expanded_grp_pos", -1);

        mMemberApps = Util.findGroupMembers(mGroupName, mApps);

        GroupItem groupItem = new GroupItem();
        groupItem.groupName = mGroupName;
        groupItem.apps = mMemberApps;

        List<GroupItem> groupItems = new ArrayList<>(1);
        groupItems.add(groupItem);

        mGrpAdapter = new GroupAdapter(this);
        mGrpAdapter.setData(groupItems);
        mGrpListView.setAdapter(mGrpAdapter);

        mGrpListView.expandGroup(0);
        mGrpListView.setSelectedGroup(0);

        if (!mReadOnly) {
            mNote.setVisibility(View.GONE);
        }
    }

    @Override
    public void onStop() {
        if (mConfirmDeleteDialog != null) {
            mConfirmDeleteDialog.dismiss();
        }
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_group_view, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // handle read-only default groups
        if (mReadOnly) {
            MenuItem editMenuItem = menu.findItem(R.id.menu_edit_group);
            editMenuItem.setVisible(false);
            MenuItem delMenuItem = menu.findItem(R.id.menu_delete_group);
            delMenuItem.setVisible(false);
            MenuItem renameMenuItem = menu.findItem(R.id.menu_rename_group);
            renameMenuItem.setVisible(false);
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

            case R.id.menu_edit_group:
                editGroup();
                return true;

            case R.id.menu_copy_group:
                copyGroup();
                return true;

            case R.id.menu_delete_group:
                showDialog(DIALOG_CONFIRM_DELETE);
                return true;

            case R.id.menu_rename_group:
                renameGroup();
                return true;

            case R.id.menu_select_group:
                selectGroup();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void editGroup() {
        Intent i = new Intent(this, GroupEditActivity.class);
        i.setAction(ACTION_EDIT_GROUP);
        i.putExtra(EXTRA_GROUP_NAME, mGroupName);
        startActivity(i);
    }

    private void copyGroup() {
        NewGroupDialogFragment.show(getFragmentManager(),
                new NewGroupDialogFragment.OnNewGroupListener() {
                    @Override
                    public void onNewGroup(String name) {
                        if (Arrays.asList(DEFAULT_GROUPS).contains(name) ||
                                mUserGroups.contains(name)) {
                            Util.showToast(GroupViewActivity.this,
                                    getString(R.string.group_exists, name));
                            return;
                        }
                        Intent i = new Intent(GroupViewActivity.this, GroupEditActivity.class);
                        i.setAction(ACTION_COPY_GROUP);
                        i.putExtra(EXTRA_GROUP_NAME, name);
                        i.putExtra(EXTRA_SOURCE_GROUP_NAME, mGroupName);
                        if (getIntent().getBooleanExtra(EXTRA_SELECT, false)) {
                            i.putExtra(EXTRA_SELECT, true);
                        }
                        startActivityForResult(i, REQUEST_COPY_GROUP);
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_COPY_GROUP) {
            if (resultCode == RESULT_OK) {
                // copy-to group view created successfully, and it
                // replaces this copy-from group view in the stack
                finish();
            }
        }
    }

    private void deleteGroup() {
        removeGroup(mGroupName);
        writePrefs();
        finish();
    }

    private void renameGroup() {
        RenameGroupDialogFragment.show(getFragmentManager(),
                mGroupName,
                new RenameGroupDialogFragment.OnRenameGroupListener() {
                    @Override
                    public void onRenameGroup(String newName) {
                        if (Arrays.asList(DEFAULT_GROUPS).contains(newName) ||
                                mUserGroups.contains(newName)) {
                            Util.showToast(GroupViewActivity.this,
                                    getString(R.string.group_exists, newName));
                            return;
                        }
                        // update group name everywhere
                        String oldName = mGroupName;
                        boolean select = (mGrpPos == mSelectedGrpPos);
                        boolean expand = (mGrpPos == mExpandedGrpPos);
                        mGroupName = newName;
                        // update extra so group name will be correct when
                        // activity is paused and resumed
                        getIntent().putExtra(EXTRA_GROUP_NAME, newName);
                        removeGroup(oldName);
                        mGrpPos = addGroup(newName, select, expand);
                        writePrefs();
                        // update this group view
                        mGrpAdapter.getGroup(0).groupName = newName;
                        mGrpAdapter.notifyDataSetChanged();
                    }
                });
    }

    private void selectGroup() {
        mEditor.putInt("selected_grp_pos", mGrpPos);
        mEditor.commit();
        new Launcher(this).launch(Launcher.TEST_MODE_BATCH_SELECT, true, true);
        finish();
    }

    private void removeGroup(String name) {
        // remove from saved hash set
        mUserGroups.remove(name);
        // remove from sorted list
        mSortedAllGroups.remove(name);
        // remove from member apps
        for (SmartcardApp app : mMemberApps) {
            app.removeGroup(name);
        }
        // adjust selected group position indices as needed
        if (mGrpPos == mSelectedGrpPos) {
            // always guaranteed at least two groups: other and payment
            mSelectedGrpPos = 0;
        } else
        if (mGrpPos < mSelectedGrpPos) {
            mSelectedGrpPos--;
        }
        // adjust expanded group position index as needed
        if (mGrpPos == mExpandedGrpPos) {
            // no expanded group; all collapsed
            mExpandedGrpPos = -1;
        } else
        if (mGrpPos < mExpandedGrpPos) {
            mExpandedGrpPos--;
        }
    }

    // returns sorted list index
    private int addGroup(String name, boolean select, boolean expand) {
        // add to saved hash set
        mUserGroups.add(name);
        // add to sorted list
        mSortedAllGroups.add(name);
        Collections.sort(mSortedAllGroups, String.CASE_INSENSITIVE_ORDER);
        // add to member apps
        for (SmartcardApp app : mMemberApps) {
            app.addGroup(name);
        }
        int grpPos = mSortedAllGroups.indexOf(name);
        // adjust selected group position indices as needed
        if (select) {
            mSelectedGrpPos = grpPos;
        } else
        if (grpPos <= mSelectedGrpPos) {
            mSelectedGrpPos++;
        }
        // adjust expanded group position index as needed
        if (expand) {
            mExpandedGrpPos = grpPos;
        } else
        if (grpPos <= mExpandedGrpPos) {
            mExpandedGrpPos++;
        }
        return grpPos;
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
                        .setTitle(mGroupName)
                        .setMessage(R.string.confirm_delete_group)
                        .setPositiveButton(R.string.dialog_ok,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,
                                                        int id) {
                                        deleteGroup();
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

    private static class GroupItem {
        String groupName;
        List<SmartcardApp> apps;
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
    private static class GroupAdapter extends AnimatedExpandableListView.AnimatedExpandableListAdapter {
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
            holder.memberCount.setText("(" + groupItem.apps.size() + ")");
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
