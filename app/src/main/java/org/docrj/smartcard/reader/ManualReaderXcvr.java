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
import org.docrj.smartcard.reader.R;
import org.docrj.smartcard.util.Util;

import android.nfc.TagLostException;
import android.nfc.tech.IsoDep;
import android.util.Log;

public class ManualReaderXcvr extends OtherReaderXcvr implements ReaderXcvr.UiListener {

    final Object mDisconnectWaiter = new Object();

    public ManualReaderXcvr(IsoDep isoDep, String aid, UiCallbacks uiCallbacks) {
        super(isoDep, aid, uiCallbacks);
    }

    @Override
    public void run() {
        try {
            mIsoDep.connect();
            mUiCallbacks.clearMessages();
            mUiCallbacks.onOkay(mContext.getString(R.string.manual_connected));

            mUiCallbacks.setUserSelectListener(this);
            try {
                synchronized(mDisconnectWaiter) {
                    mDisconnectWaiter.wait();
                }
                Log.d(TAG, "exiting manual select thread!");
            } catch (InterruptedException e) {
                // should not happen
                Log.e(TAG, "interrupted exception!");
            }
        } catch (TagLostException e) {
            mUiCallbacks.onError(mContext.getString(R.string.tag_lost_err));
        } catch (IOException e) {
            mUiCallbacks.onError(e.getMessage());
        } finally {
            try {
                mIsoDep.close();
            } catch (IOException e) {
            }
        }
    }    

    @Override
    public void onUserSelect(String aid) {
        mAid = aid;
        mAidBytes = Util.hexToBytes(aid);

        ResponseApdu rspApdu = null;
        if (mIsoDep.isConnected()) {
            try {
                Log.d(TAG, "select app: " + mAid);
                rspApdu = sendAndRcv(new SelectApdu(mAidBytes), true);
            } catch (TagLostException e) {
                mUiCallbacks.onError(mContext.getString(R.string.tag_lost_err));
            } catch (IOException e) {
                mUiCallbacks.onError(e.getMessage());
            }
        } else {
            mUiCallbacks.onError(mContext.getString(R.string.tag_lost_err));
        } 

        if (rspApdu == null) {
            try {
                mIsoDep.close();
            } catch (IOException e) {
            }
            synchronized (mDisconnectWaiter) {
                mDisconnectWaiter.notify();
            }
        } else {
            if (rspApdu.isStatus(SW_NO_ERROR)) {
                mUiCallbacks.onOkay(mContext.getString(R.string.select_app_ok,
                    rspApdu.getSW1SW2()));
            } else {
                mUiCallbacks.onError(
                    mContext.getString(R.string.select_app_err,
                        rspApdu.getSW1SW2(),
                        ApduParser.parse(false, rspApdu.toBytes())));
            }
        }
    }
}
