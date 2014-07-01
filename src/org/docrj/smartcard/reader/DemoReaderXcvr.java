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

import org.docrj.smartcard.reader.R;

import android.nfc.TagLostException;
import android.nfc.tech.IsoDep;
import android.util.Log;

public class DemoReaderXcvr extends ReaderXcvr {

    public DemoReaderXcvr(IsoDep isoDep, String aid, OnMessage onMessage) {
        super(isoDep, aid, onMessage);
    }

    @Override
    public void run() {
        int messageCounter = 0;
        try {
            mIsoDep.connect();
            Log.d(TAG, "select AID: " + mAid);
            mOnMessage.onMessageSend("00 A4 04 00"
                    + String.format("%02X ", mAidBytes.length) + mAid + " 00",
                    mContext.getString(R.string.select_app));
            byte[] response = mIsoDep.transceive(buildSelectApdu(mAidBytes));
            if (new String(response).startsWith(mContext
                    .getString(R.string.select_success_prefix))) {
                mOnMessage.onMessageRcv(new String(response), null, null);
                while (mIsoDep.isConnected() && !Thread.interrupted()) {
                    String message = mContext
                            .getString(R.string.reader_msg_prefix)
                            + " "
                            + messageCounter++;
                    mOnMessage.onMessageSend(message, mContext.getString(R.string.select_app));
                    response = mIsoDep.transceive(message.getBytes());
                    mOnMessage.onMessageRcv(new String(response), null, null);
                }
                mIsoDep.close();
            } else {
                mOnMessage.onError(mContext.getString(R.string.wrong_resp_err),
                        true);
            }
        } catch (TagLostException e) {
            mOnMessage
                    .onError(mContext.getString(R.string.tag_lost_err), false);
        } catch (IOException e) {
            mOnMessage.onError(e.getMessage(), false);
        }
    }
}
