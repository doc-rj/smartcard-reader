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

public class SmartcardApp {

    public static final int TYPE_PAYMENT = 0;
    public static final int TYPE_OTHER = 1;

    private String mName;
    private String mAid;
    private int mType;

    public SmartcardApp(String name, String aid, int type) {
        mName = name;
        mAid = aid;
        mType = type;
    }

    public SmartcardApp clone() {
        return new SmartcardApp(mName, mAid, mType);
    }

    public void copy(SmartcardApp app) {
        if (app != null) {
            mName = app.getName();
            mAid = app.getAid();
            mType = app.getType();
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
    }

    public String getName() {
        return mName;
    }

    public String getAid() {
        return mAid;
    }

    public int getType() {
        return mType;
    }

    public String toBriefString() {
        return mName;
    }

    @Override
    public String toString() {
        return mName + " (" + mAid + ")";
    }
}
