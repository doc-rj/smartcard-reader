package org.docrj.smartcard.reader;

import org.docrj.smartcard.reader.ReaderTransceiver.OnMessage;

import android.app.Activity;
import android.content.Context;
import android.nfc.NfcAdapter;
import android.nfc.NfcAdapter.ReaderCallback;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AutoCompleteTextView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.docrj.smartcard.reader.R;

public class ReaderActivity extends Activity implements OnMessage, ReaderCallback {

	private NfcAdapter mNfcAdapter;
	private ListView mListView;
	private ReaderAdapter mMsgListAdapter;

    private String mAid;
	private AutoCompleteTextView mAidView;	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_reader);
		mListView = (ListView)findViewById(R.id.listView);
		mMsgListAdapter = new ReaderAdapter(getLayoutInflater(), savedInstanceState);
		mListView.setAdapter(mMsgListAdapter);
		mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
		
		mAid = getString(R.string.default_aid);
		mAidView = (AutoCompleteTextView)findViewById(R.id.aid);

		mAidView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
		    @Override
		    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
		        if (event != null && event.getAction() != KeyEvent.ACTION_DOWN) {
		            return false;
		        } else
		        if (actionId == EditorInfo.IME_ACTION_DONE ||
		            event == null || event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
		            String text = mAidView.getText().toString();
		            if (text.length() < 5 || text.length() % 2 != 0)
		            {
		                // toast
		                Toast.makeText(ReaderActivity.this, "Invalid AID", Toast.LENGTH_SHORT).show();
		                return false;
		            }
		            mAid = text;
		            mAidView.clearFocus();
	                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
	                imm.hideSoftInputFromWindow(mAidView.getWindowToken(), 0);
		            return true;
		        }
		        return false;
		    }		    
		});
	} 

	@Override
	public void onResume() {
		super.onResume();
		mNfcAdapter.enableReaderMode(this, this,
		        NfcAdapter.FLAG_READER_NFC_A | NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
				null);
	}

	@Override
	public void onPause() {
		super.onPause();
		mNfcAdapter.disableReaderMode(this);
	}

    @Override
    protected void onSaveInstanceState(Bundle outstate) {
        if (mMsgListAdapter != null) {
            mMsgListAdapter.onSaveInstanceState(outstate);
        }
    }	

	@Override
	public void onTagDiscovered(Tag tag) {
		IsoDep isoDep = IsoDep.get(tag);
		if (isoDep == null) {
		    byte[] msg = getString(R.string.wrong_tag_err).getBytes();
		    onError(msg);
		} else {
		    ReaderTransceiver transceiver = new ReaderTransceiver(isoDep, mAid, this);
		    Thread thread = new Thread(transceiver);
		    thread.start();
		}
	}

    @Override
	public void onMessageRcv(final byte[] message) {
        onMessageAndType(new String(message), 0);
	}
    
    @Override
    public void onMessageSend(final String message) {
        onMessageAndType(message, 1);
    }

	@Override
	public void onError(final byte[] message) {
	    onMessageAndType(new String(message), -1);
	}
	
	private void onMessageAndType(final String message, final int type) {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                mMsgListAdapter.addMessage(message, type);
            }
        });
	}
}
