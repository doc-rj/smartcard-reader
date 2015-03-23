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

import org.docrj.smartcard.iso7816.CommandApdu;
import org.docrj.smartcard.iso7816.ResponseApdu;
import org.docrj.smartcard.iso7816.SelectApdu;
import org.docrj.smartcard.iso7816.TLVUtil;
import org.docrj.smartcard.iso7816.TLVException;
import org.docrj.smartcard.util.Util;

import android.content.Context;
import android.nfc.TagLostException;
import android.nfc.tech.IsoDep;
import android.util.Log;

public class ReaderXcvr implements Runnable {
    protected static final String TAG = "smartcard-reader";

    public interface UiCallbacks {
        // display console messages
        void onMessageSend(String raw, String name);
        void onMessageRcv(String raw, String name, String parsed);
        void onOkay(String message);
        void onError(String message);
        void onSeparator();

        // clear console messages
        void clearMessages();

        // ui listeners
        void setUserSelectListener(UiListener callback);
        
        // cleanup, if needed
        void onFinish(boolean err);
    }

    public interface UiListener {
        void onUserSelect(String aid);
    }

    public static final int SW_NO_ERROR = 0x9000;    

    protected IsoDep mIsoDep;
    protected UiCallbacks mUiCallbacks;
    protected Context mContext;

    protected String mAid;
    protected byte[] mAidBytes;

    public ReaderXcvr(IsoDep isoDep, String aid, UiCallbacks uiCallbacks) {
        this.mIsoDep = isoDep;
        this.mAid = aid;
        this.mAidBytes = Util.hexToBytes(aid);
        this.mUiCallbacks = uiCallbacks;
        this.mContext = (Context) uiCallbacks;
    }

    public ReaderXcvr(IsoDep isoDep, String aid, UiCallbacks uiCallbacks, Context context) {
        this.mIsoDep = isoDep;
        this.mAid = aid;
        this.mAidBytes = Util.hexToBytes(aid);
        this.mUiCallbacks = uiCallbacks;
        this.mContext = context;
    }

    protected static String bytesToHexAndAscii(byte[] data, boolean ascii) {
        int i;
        if (data.length == 0) {
            return "";
        }
        StringBuilder _sbbuffer = new StringBuilder();
        String _hexline = "";
        String _asciiline = "";
        for (i = 0; i < data.length; i++) {
            _hexline = _hexline.concat(String.format("%02x ", data[i]));
            if (ascii) {
            if (data[i] > 31 && data[i] < 127) {
                _asciiline = _asciiline.concat(String.valueOf((char) data[i]));
            } else {
                _asciiline = _asciiline.concat(".");
            }
            }
        }
        _sbbuffer.append(_hexline);
        // don't waste space w/ ascii if we just have the two status bytes
        if (ascii && data.length > 2) {
            _sbbuffer.append("\n...\t");
            _sbbuffer.append(_asciiline);
        }
        return _sbbuffer.toString();
    }

    // build boilerplate command APDU
    protected byte[] buildCmdApdu(CommandApdu cmdApdu) {
        return cmdApdu.toBytes();
    }

    // build select command APDU
    protected byte[] buildSelectApdu(byte[] aidBytes) {
        SelectApdu cmdApdu = new SelectApdu(aidBytes);
        return cmdApdu.toBytes();
    }

    // send command APDU, get response APDU, and display to user   
    protected ResponseApdu sendAndRcv(CommandApdu cmdApdu, boolean ascii)
            throws TagLostException, IOException {
        byte[] cmdBytes = cmdApdu.toBytes();
        String cmdStr = CommandApdu.toString(cmdBytes, cmdApdu.getLc());
        mUiCallbacks.onMessageSend(cmdStr, cmdApdu.getCommandName());
        byte[] rsp = mIsoDep.transceive(cmdBytes);
        ResponseApdu rspApdu = new ResponseApdu(rsp);
        byte[] data = rspApdu.getData();

        String parsed = null;
        String errMsg = "no error";
        try {
            if (data.length > 0) {
                parsed = TLVUtil.prettyPrintAPDUResponse(data);
            }
        } catch (TLVException e) {
            parsed = null;
            errMsg = e.getMessage();
        }

        mUiCallbacks.onMessageRcv(bytesToHexAndAscii(rsp, ascii), cmdApdu.getCommandName(), parsed);

        if (data.length > 0 && parsed == null) {
            mUiCallbacks.onError(errMsg);
        }

        /*
        Log.d(TAG, "response APDU: " + Util.bytesToHex(rsp));
        if (data.length > 0) {
            Log.d(TAG, TLVUtil.prettyPrintAPDUResponse(data));
        }
        */
        return rspApdu;
    }

    @Override
    public void run() {
    }
}
