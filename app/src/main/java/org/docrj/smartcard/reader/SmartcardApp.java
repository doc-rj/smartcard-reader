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

import org.docrj.smartcard.util.Util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;

public class SmartcardApp {

    public static final int TYPE_PAYMENT = 0;
    public static final int TYPE_OTHER = 1;

    static final String[] GROUPS = {
            "payment", "other"
    };

    private String mName;
    private String mAid;
    private byte[] mAidBytes;
    private int mType;
    private boolean mReadOnly;

    private HashSet<String> mGroups = new LinkedHashSet<>();

    public static Comparator<SmartcardApp> nameComparator =
            new Comparator<SmartcardApp>() {
                @Override
                public int compare(SmartcardApp app1, SmartcardApp app2) {
                    // alphabetize in ascending order, case insensitive
                    return app1.mName.toLowerCase().compareTo(app2.mName.toLowerCase());
                }
            };

    public SmartcardApp() {
    }

    public SmartcardApp(String name, String aid, int type) {
        mName = name;
        mAid = aid;
        mAidBytes = Util.hexToBytes(aid);
        mReadOnly = false;
        setType(type);
    }

    public void copy(SmartcardApp app) {
        if (app != null) {
            mName = app.getName();
            mAid = app.getAid();
            mAidBytes = app.getAidBytes();
            mReadOnly = false;
            setType(app.getType());
        }
    }

    public void setName(String name) {
        mName = name;
    }

    public void setAid(String aid) {
        mAid = aid;
    }

    public void setType(int type) {
        mType = type;
        mGroups.remove(GROUPS[Math.abs(type-1)]);
        mGroups.add(GROUPS[type]);
    }

    public void setReadOnly(boolean readOnly) {
        mReadOnly = readOnly;
    }

    public void setGroups(HashSet<String> groups) {
        mGroups = groups;
    }

    public void addGroup(String group) {
        mGroups.add(group);
    }

    public void removeGroup(String group) {
        mGroups.remove(group);
    }

    public String getName() {
        return mName;
    }

    public String getAid() {
        return mAid;
    }

    public byte[] getAidBytes() {
        return mAidBytes;
    }

    public int getType() {
        return mType;
    }

    public boolean isReadOnly() {
        return mReadOnly;
    }

    public boolean isMemberOf(String groupName) {
        for (String group : mGroups) {
            if (group.equals(groupName)) {
                return true;
            }
        }
        return false;
    }

    public HashSet<String> getGroups() {
        return mGroups;
    }

    @Override
    public boolean equals(Object obj) {
        if (!obj.getClass().equals(SmartcardApp.class)) {
            return false;
        }
        SmartcardApp app = (SmartcardApp) obj;
        return (mName.equals(app.getName()) &&
            mAid.equals(app.getAid()) &&
            mType == app.getType() &&
            mGroups.equals(app.getGroups()));
    }

    @Override
    public String toString() {
        return mName + " (" + mAid + ")";
    }

    public String toBriefString() {
        return mName;
    }
}
