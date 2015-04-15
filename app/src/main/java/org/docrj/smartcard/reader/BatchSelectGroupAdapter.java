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
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class BatchSelectGroupAdapter extends BaseAdapter {

    public static final int GROUP_TYPE_PAYMENT = 0;
    public static final int GROUP_TYPE_OTHER = 1;
    public static final int GROUP_TYPE_MIXED = 2;

    private static final String[] DEFAULT_GROUPS = SmartcardApp.GROUPS;

    private class Group {
        private String name;
        private int type;
        private int numMembers;

        Group(String name, int type, int numMembers) {
            this.name = name;
            this.type = type;
            this.numMembers = numMembers;
        }

        public String getName() {
            return name;
        }

        public int getType() {
            return type;
        }

        public int getNumMembers() {
            return numMembers;
        }
    };

    private LayoutInflater mLayoutInflater;
    private Context mContext;
    List<Group> mGroups = new ArrayList<>(5);

    public BatchSelectGroupAdapter(LayoutInflater layoutInflater) {
        mLayoutInflater = layoutInflater;
        mContext = layoutInflater.getContext();
    }

    public void clear() {
        mGroups.clear();
    }

    // returns array list position for the group
    public int addGroup(String name, Collection<SmartcardApp> members) {
        int type;
        if (name.equals(DEFAULT_GROUPS[SmartcardApp.TYPE_PAYMENT])) {
            type = GROUP_TYPE_PAYMENT;
        } else if (name.equals(DEFAULT_GROUPS[SmartcardApp.TYPE_OTHER])) {
            type = GROUP_TYPE_OTHER;
        } else {
            // determine type for user-added group
            int payment = 0;
            int other = 0;
            for (SmartcardApp app : members) {
                if (app.getType() == SmartcardApp.TYPE_PAYMENT) {
                    payment++;
                } else {
                    other++;
                }
            }
            if (payment > 0 && other > 0) {
                type = GROUP_TYPE_MIXED;
            } else if (payment > 0) {
                type = GROUP_TYPE_PAYMENT;
            } else {
                type = GROUP_TYPE_OTHER;
            }
        }
        return addGroup(name, type, members.size());
    }

    // returns array list position for the group
    public int addGroup(String name, int type, int numMembers) {
        Group group = new Group(name, type, numMembers);
        mGroups.add(group);
        notifyDataSetChanged();
        return mGroups.size() - 1;
    }

    public String getGroupName(int position) {
        Group group = mGroups.get(position);
        return group == null ? null : group.getName();
    }

    @Override
    public int getCount() {
        return mGroups == null ? 0 : mGroups.size();
    }

    @Override
    public Object getItem(int position) {
        return mGroups.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View item = convertView;
        if (item == null) {
            item = mLayoutInflater.inflate(R.layout.spinner_apps_list, parent, false);
        }
        Group group = mGroups.get(position);
        TextView textView = (TextView) item.findViewById(android.R.id.text1);
        textView.setText(group.getName() + " (" + group.getNumMembers() + ")");
        Drawable img;
        if (group.getType() == GROUP_TYPE_PAYMENT) {
            img = mContext.getResources().getDrawable(R.drawable.credit_card2_green);
        } else
        if (group.getType()== GROUP_TYPE_OTHER) {
            img = mContext.getResources().getDrawable(R.drawable.credit_card2_blue);
        } else {
            img = mContext.getResources().getDrawable(R.drawable.credit_card2_pink);
        }
        textView.setCompoundDrawablesWithIntrinsicBounds(img, null, null, null);
        return item;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        if (row == null) {
            row = mLayoutInflater.inflate(R.layout.spinner_dropdown_apps_list, parent, false);
        }
        Group group = mGroups.get(position);
        TextView textView = (TextView) row.findViewById(android.R.id.text1);
        textView.setText(group.getName() + " (" + group.getNumMembers() + ")");
        Drawable img;
        if (group.getType() == GROUP_TYPE_PAYMENT) {
            img = mContext.getResources().getDrawable(R.drawable.credit_card_green);
        } else
        if (group.getType()== GROUP_TYPE_OTHER) {
            img = mContext.getResources().getDrawable(R.drawable.credit_card_blue);
        } else {
            img = mContext.getResources().getDrawable(R.drawable.credit_card_pink);
        }
        textView.setCompoundDrawablesWithIntrinsicBounds(img, null, null, null);
        return row;
    }
}
