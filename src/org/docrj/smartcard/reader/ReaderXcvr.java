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

import android.content.Context;
import android.nfc.tech.IsoDep;

public class ReaderXcvr implements Runnable {
    protected static final String TAG = "smartcard-reader";
    
    public static final int SW_NO_ERROR = 0x9000;

	public interface OnMessage {
        void onMessageSend(String message);
		void onMessageRcv(String message);
		void onOkay(String message);
		void onError(String message, boolean clearOnNext);
	}

	protected IsoDep mIsoDep;
	protected OnMessage mOnMessage;
	protected Context mContext;
	
	protected String mAid;
	protected byte[] mAidBytes;

	public ReaderXcvr(IsoDep isoDep, String aid, OnMessage onMessage) {
		this.mIsoDep = isoDep;
		this.mAid = aid;
		this.mAidBytes = hexToBytes(aid);
		this.mOnMessage = onMessage;
		this.mContext = (Context)onMessage;
	}

	protected static byte[] hexToBytes(String str) {
	    byte[] bytes = new byte[str.length() / 2];
	    for (int i = 0; i < bytes.length; i++)
	    {
	        bytes[i] = (byte) Integer.parseInt(str.substring(2 * i, 2 * i + 2), 16);
	    }
	    return bytes;
	}

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
    protected static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    protected static String bytesToHexAndAscii(byte[] data) {
        int i;
        if (data.length == 0) {
            return "";
        }
        StringBuilder _sbbuffer = new StringBuilder();
        String _hexline = "";
        String _asciiline = "";
        for (i = 0; i < data.length; i++) {
            _hexline = _hexline.concat(String.format("%02x ", data[i]));
            if (data[i] > 31 && data[i] < 127) {
                _asciiline = _asciiline.concat(String.valueOf((char) data[i]));
            } else {
                _asciiline = _asciiline.concat(".");
            }
        }
        _sbbuffer.append(_hexline);
        // don't waste space w/ ascii if we just have the two status bytes
        if (data.length > 2) {
            _sbbuffer.append("\n...\t");
            _sbbuffer.append(_asciiline);
        }
        return _sbbuffer.toString();
    }

    // build select command APDU
    protected byte[] buildSelectApdu(byte[] aid) {
        SelectApdu cmdApdu = new SelectApdu(aid);
        return cmdApdu.toBytes();
    }

	@Override
	public void run() {
	}
}
