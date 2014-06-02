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

public class PaymentReaderXcvr extends ReaderXcvr {

    private static final String PPSE_AID = "325041592E5359532E4444463031";
    private final byte[] mPpseAidBytes;

	public PaymentReaderXcvr(IsoDep isoDep, String aid, OnMessage onMessage) {
	    super(isoDep, aid, onMessage);
	    mPpseAidBytes = hexToBytes(PPSE_AID);
	}

	@Override
	public void run() {
		try {
			mIsoDep.connect();

			Log.d(TAG, "select PPSE");
			mOnMessage.onMessageSend("00 A4 04 00 " + "0E " + PPSE_AID + " 00");
			byte[] rsp = mIsoDep.transceive(buildSelectApdu(mPpseAidBytes));
            mOnMessage.onMessageRcv(bytesToHexAndAscii(rsp));
            Log.d(TAG, "select PPSE response: " + bytesToHex(rsp));

            ResponseApdu rspApdu = new ResponseApdu(rsp);
			if (rspApdu.isStatus(SW_NO_ERROR)) {
			    mOnMessage.onOkay(mContext.getString(R.string.select_ppse_ok, rspApdu.getSW1SW2()));
			} else {
			    mOnMessage.onError(mContext.getString(R.string.select_ppse_err, rspApdu.getSW1SW2(),
			        ApduParser.parse(false, rsp)), true);
			    mIsoDep.close();
			    return;
			}

			Log.d(TAG, "select app: " + mAid);
			mOnMessage.onMessageSend("00 A4 04 00 " + String.format("%02X ", mAidBytes.length) + mAid + " 00");
			rsp = mIsoDep.transceive(buildSelectApdu(mAidBytes));
			mOnMessage.onMessageRcv(bytesToHexAndAscii(rsp));
			Log.d(TAG, "select app response: " + bytesToHex(rsp));

			rspApdu = new ResponseApdu(rsp);
			if (rspApdu.isStatus(SW_NO_ERROR)) {
	             mOnMessage.onOkay(mContext.getString(R.string.select_app_ok, rspApdu.getSW1SW2()));
			} else {
                mOnMessage.onError(mContext.getString(R.string.select_app_err, rspApdu.getSW1SW2(),
                    ApduParser.parse(false, rsp)), true);
                mIsoDep.close();
                return;
            }
			mIsoDep.close();
		}
	    catch (TagLostException e) {
	        mOnMessage.onError(mContext.getString(R.string.tag_lost_err), false);
	    }
		catch (IOException e) {
			mOnMessage.onError(e.getMessage(), false);
		}
	}
}
