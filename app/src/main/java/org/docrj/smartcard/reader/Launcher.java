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


public class Launcher {
    // test modes
    static final int TEST_MODE_APP_SELECT = 0;
    static final int TEST_MODE_BATCH_SELECT = 1;
    static final int TEST_MODE_EMV_READ = 2;

    final Context mContext;

    public Launcher(Context context) {
        mContext = context;
    }

    void launch(int testMode, boolean newTask, boolean animation) {
        Class<?> cls;
        switch(testMode) {
            case TEST_MODE_APP_SELECT:
                cls = AppSelectActivity.class;
                break;
            case TEST_MODE_BATCH_SELECT:
                cls = BatchSelectActivity.class;
                break;
            case TEST_MODE_EMV_READ:
                cls = EmvReadActivity.class;
                break;
            default:
                cls = AppSelectActivity.class;
                break;
        }
        Intent i = new Intent(mContext, cls);
        // TODO:
        if (newTask) {
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        }
        if (!animation) {
            i.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        }
        mContext.startActivity(i);
    }

    void launch(String testMode, boolean newTask, boolean animation) {
        launch(testModeToInt(testMode), newTask, animation);
    }

    public int testModeToInt(String testMode) {
        if (mContext.getString(R.string.app_select).equals(testMode)) {
            return TEST_MODE_APP_SELECT;
        } else if (mContext.getString(R.string.batch_select).equals(testMode)) {
            return TEST_MODE_BATCH_SELECT;
        } else if (mContext.getString(R.string.emv_read).equals(testMode)) {
            return TEST_MODE_EMV_READ;
        } else {
            return TEST_MODE_APP_SELECT;
        }
    }
}
