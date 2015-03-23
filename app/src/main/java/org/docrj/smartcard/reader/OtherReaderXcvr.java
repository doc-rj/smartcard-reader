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

import java.io.IOException;

import org.docrj.smartcard.iso7816.ResponseApdu;
import org.docrj.smartcard.iso7816.SelectApdu;

import android.content.Context;
import android.nfc.TagLostException;
import android.nfc.tech.IsoDep;
import android.util.Log;

public class OtherReaderXcvr extends ReaderXcvr {

    public OtherReaderXcvr(IsoDep isoDep, String aid, UiCallbacks uiCallbacks) {
        super(isoDep, aid, uiCallbacks);
    }

    public OtherReaderXcvr(IsoDep isoDep, String aid, UiCallbacks uiCallbacks, Context context) {
        super(isoDep, aid, uiCallbacks, context);
    }

    @Override
    public void run() {
        boolean err = false;
        try {
            mIsoDep.connect();

            Log.d(TAG, "select app: " + mAid);
            ResponseApdu rspApdu = sendAndRcv(new SelectApdu(mAidBytes), true);

            if (rspApdu.isStatus(SW_NO_ERROR)) {
                mUiCallbacks.onOkay(mContext.getString(R.string.select_app_ok,
                        rspApdu.getSW1SW2()));
            } else {
                mUiCallbacks.onError(
                        mContext.getString(R.string.select_app_err,
                                rspApdu.getSW1SW2(),
                                ApduParser.parse(false, rspApdu.toBytes())));
            }
            mIsoDep.close();
        } catch (TagLostException e) {
            mUiCallbacks.onError(mContext.getString(R.string.tag_lost_err));
            err = true;
        } catch (IOException e) {
            mUiCallbacks.onError(e.getMessage());
            err = true;
        }
        mUiCallbacks.onFinish(err);
    }
}
