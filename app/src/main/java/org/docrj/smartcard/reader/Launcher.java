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
    static final int TEST_MODE_AID_ROUTE = 0;
    static final int TEST_MODE_EMV_READ = 1;

    final Context mContext;

    public Launcher(Context context) {
        mContext = context;
    }

    void launch(int testMode, boolean animation) {
        Class<?> cls;
        switch(testMode) {
            case TEST_MODE_AID_ROUTE:
                cls = AidRouteActivity.class;
                break;
            case TEST_MODE_EMV_READ:
                cls = EmvReadActivity.class;
                break;
            default:
                cls = AidRouteActivity.class;
                break;
        }
        Intent i = new Intent(mContext, cls);
        if (!animation) {
            i.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        }
        mContext.startActivity(i);
    }
}
