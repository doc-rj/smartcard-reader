package org.docrj.smartcard.reader;

import java.io.IOException;

import org.docrj.smartcard.reader.R;

import android.content.Context;
import android.nfc.tech.IsoDep;
import android.util.Log;

public class ReaderTransceiver implements Runnable {
    private static final String TAG = "smartcard-reader";

	public interface OnMessage {
		void onMessageRcv(byte[] message);
		void onMessageSend(String message);
		void onError(byte[] message);
	}

	private IsoDep mIsoDep;
	private OnMessage mOnMessage;
	private Context mContext;
	
	private String mAid;
	private byte[] mAidBytes;

	public ReaderTransceiver(IsoDep isoDep, String aid, OnMessage onMessage) {
		this.mIsoDep = isoDep;
		this.mAid = aid;
		this.mAidBytes = hexToBytes(aid);
		this.mOnMessage = onMessage;
		this.mContext = (Context)onMessage;
	}

	static private byte[] hexToBytes(String str) {
	    byte[] bytes = new byte[str.length() / 2];
	    for (int i = 0; i < bytes.length; i++)
	    {
	        bytes[i] = (byte) Integer.parseInt(str.substring(2 * i, 2 * i + 2), 16);
	    }
	    return bytes;
	}

	// 0xA4 select file, 0x04 direct selection by DF name (for 7816-5 AID)
	private static final byte[] CLA_INS_P1_P2 = { 0x00, (byte)0xA4, 0x04, 0x00 };
	
	// build command APDU for select AID
	private byte[] createSelectAidApdu(byte[] aid) {
		byte[] result = new byte[6 + aid.length];
		System.arraycopy(CLA_INS_P1_P2, 0, result, 0, CLA_INS_P1_P2.length);
		// Lc length of data block
		result[4] = (byte)aid.length;
		// data
		System.arraycopy(aid, 0, result, 5, aid.length);
		// Le expected length of data in response
		result[result.length - 1] = 0;
		return result;
	}

	@Override
	public void run() {
		int messageCounter = 0;
		try {
			mIsoDep.connect();
			Log.d(TAG, "select AID: " + mAid);
			mOnMessage.onMessageSend("00A40400" + String.format("%02X", mAidBytes.length) + mAid + "00");
			byte[] response = mIsoDep.transceive(createSelectAidApdu(mAidBytes));
			if (new String(response).startsWith(mContext.getString(R.string.select_success_prefix))) { 
			    mOnMessage.onMessageRcv(response);
		         while (mIsoDep.isConnected() && !Thread.interrupted()) {
		             String message = mContext.getString(R.string.reader_msg_prefix) + " " + messageCounter++;
		             mOnMessage.onMessageSend(message);
		             response = mIsoDep.transceive(message.getBytes());
		             mOnMessage.onMessageRcv(response);
		         }
		         mIsoDep.close();
			} else {
			    mOnMessage.onError(mContext.getString(R.string.wrong_resp_err).getBytes());
			}
		}
		catch (IOException e) {
			mOnMessage.onError(e.getMessage().getBytes());
		}
	}
}
