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

import java.util.ArrayList;

import org.docrj.smartcard.reader.ReaderXcvr.OnMessage;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.nfc.NfcAdapter;
import android.nfc.NfcAdapter.ReaderCallback;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import org.docrj.smartcard.reader.R;

public class ReaderActivity extends Activity implements OnMessage, ReaderCallback {
    protected static final String TAG = "smartcard-reader";

    // update all five items below when adding/removing default apps!
    private final static int NUM_RO_APPS = 10;
    private final static int DEFAULT_APP_POS = 0; // Demo
    private final static String APP_NAMES =
                   "Demo" +
        		   "|Amex|Amex 7-Byte|Amex 8-Byte" +
        		   "|MC|MC 8-Byte" +
        		   "|Visa|Visa 8-Byte" +
                   "|Ryan" +
        		   "|Discover Zip";
    private final static String APP_AIDS =
                   "F0646F632D726A" +
        		   "|A00000002501|A0000000250109|A000000025010988" +
        		   "|A0000000041010|A000000004101088" +
        		   "|A0000000031010|A000000003101088" +
                   "|7465737420414944" +
        		   "|A0000003241010";
    private final static String APP_TYPES =
                   "1|0|0|0|0" +
                   "|0|0|0|0|0";

    private final static int DIALOG_NEW_APP = 1;
    private final static int DIALOG_EDIT_APP = 2;
    private final static int DIALOG_EDIT_ALL_APPS = 3;
    private final static int DIALOG_ENABLE_NFC = 4;

    private Handler mHandler;
    private Editor mEditor;
    private MenuItem mEditMenuItem;
	private NfcAdapter mNfcAdapter;
	private ListView mMsgListView;
	private ListView mEditAllListView;
	private AppAdapter mAppAdapter;
	private AppAdapter mEditAllAdapter;
	private MessageAdapter mMsgAdapter;
	private int mMsgPos;
	boolean mSkipNextClear;

    private int mSelectedAppPos = DEFAULT_APP_POS;
	private ArrayList<SmartcardApp> mApps;
	private String mDemoAid;
	private boolean mSelectOnCreate;
    private View mTitleView;
    CharSequence mTitle;
    private Spinner mAidSpinner;
	private ActionBar mActionBar;
	private AlertDialog mNewDialog;
	private AlertDialog mEditDialog;
	private AlertDialog mEditAllDialog;
	private AlertDialog mEnableNfcDialog;
	private int mEditPos;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	    getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        mActionBar = getActionBar();
        mTitleView = getLayoutInflater().inflate(R.layout.app_title, null);
        mActionBar.setCustomView(mTitleView);
        mActionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_SHOW_HOME);
        mActionBar.show();

		setContentView(R.layout.activity_reader);
		mMsgListView = (ListView)findViewById(R.id.listView);
		mMsgAdapter = new MessageAdapter(getLayoutInflater(), savedInstanceState);
		mMsgListView.setAdapter(mMsgAdapter);
		if (savedInstanceState != null) {
		    mMsgPos = savedInstanceState.getInt("msg_pos");
		}

		mHandler = new Handler();
		mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
		
		ApduParser.init(this);

		mDemoAid = getString(R.string.demo_aid);

		SharedPreferences ss = getSharedPreferences("prefs", Context.MODE_PRIVATE);
		mEditor = ss.edit();
		String appNames = ss.getString("app_names", null);

		// if shared prefs is empty, synchronously write defaults
		if (appNames == null) {
            writePrefs();
	        appNames = ss.getString("app_names", null);
		}

		String appAids = ss.getString("app_aids", null);
		String[] names = appNames.split("\\|");
		String[] aids = appAids.split("\\|");

		String appTypes = ss.getString("app_types", null);
		String[] typeStrs = appTypes.split("\\|");

		int[] types = new int[typeStrs.length];
		for (int i = 0; i < typeStrs.length; i++) {
		    types[i] = Integer.valueOf(typeStrs[i]);
		}

		mApps = new ArrayList<SmartcardApp>();
		for (int i = 0; i < names.length; i++) {
		    mApps.add(new SmartcardApp(names[i], aids[i], types[i]));   
		}

        mAidSpinner = (Spinner)findViewById(R.id.aid);
	    mAppAdapter = new AppAdapter(this, mApps, savedInstanceState, false);
	    mAidSpinner.setAdapter(mAppAdapter);
	    mAidSpinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
	        @Override
	        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                if (!mSelectOnCreate) {
	                clearMessages();
                }
                mSelectOnCreate = false;
	            mSelectedAppPos = pos;	            
	            Log.d(TAG, "App: " + mApps.get(pos).getName() +
	                  ", AID: " + mApps.get(pos).getAid());
            }

	        @Override
	        public void onNothingSelected(AdapterView<?> parent) {
	        }
	    });

        mSelectOnCreate = true;
        // mSelectedAppPos saved in onPause(), restored in onResume()
	}

	private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
	    @SuppressWarnings("deprecation")
        @Override
	    public void onReceive(Context context, Intent intent) {
	        String action = intent.getAction();
	        if (action == null) return;
	        if (action.equals(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED)) {
	            int state = intent.getIntExtra(NfcAdapter.EXTRA_ADAPTER_STATE, NfcAdapter.STATE_ON);
	            if (state == NfcAdapter.STATE_ON || state == NfcAdapter.STATE_TURNING_ON) {
                    Log.d(TAG, "state: " + state + " , dialog: " + mEnableNfcDialog);
	                if (mEnableNfcDialog != null) {
	                    mEnableNfcDialog.dismiss();
	                }
	                if (state == NfcAdapter.STATE_ON) {
	                    mNfcAdapter.enableReaderMode(ReaderActivity.this, ReaderActivity.this,
	                        NfcAdapter.FLAG_READER_NFC_A | NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
	                        null);
	                }
	            } else {
	                if (mEnableNfcDialog == null || !mEnableNfcDialog.isShowing()) {
	                    showDialog(DIALOG_ENABLE_NFC);
	                }
	            }
	        }
	    }
	};

	@SuppressWarnings("deprecation")
    @Override
	public void onResume() {
		super.onResume();
        // restore selected pos from prefs
        SharedPreferences ss = getSharedPreferences("prefs", Context.MODE_PRIVATE);
        mSelectedAppPos = ss.getInt("selected_aid_pos", mSelectedAppPos);
        mAidSpinner.setSelection(mSelectedAppPos);

		// this delay is a bit hacky; would be better to extend ListView
		// and override onLayout()
		mHandler.postDelayed(new Runnable() {
		    public void run() {
		        mMsgListView.smoothScrollToPosition(mMsgPos);
		    }
		}, 50L);

        // register broadcast receiver
        IntentFilter filter = new IntentFilter(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED);
        registerReceiver(mBroadcastReceiver, filter);

        // prompt to enable NFC if disabled
		if (!mNfcAdapter.isEnabled()) {
		    showDialog(DIALOG_ENABLE_NFC);
		}

        // listen for type A tags/smartcards, skipping ndef check
		mNfcAdapter.enableReaderMode(this, this,
		        NfcAdapter.FLAG_READER_NFC_A | NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
				null);
	}

	@Override
	public void onPause() {
		super.onPause();
	    // save selected pos to prefs
        writePrefs();
        // unregister broadcast receiver
        unregisterReceiver(mBroadcastReceiver);
        // disable reader mode
		mNfcAdapter.disableReaderMode(this);
	}

	@Override
	public void onStop() {
	    super.onStop();
	    if (mNewDialog != null) {
	        mNewDialog.dismiss();
	    }
	    if (mEditDialog != null) {
	        mEditDialog.dismiss();
	    }
	    if (mEditAllDialog != null) {
	        mEditAllDialog.dismiss();
	    }
        if (mEnableNfcDialog != null) {
            mEnableNfcDialog.dismiss();
        }
	}

    @Override
    protected void onSaveInstanceState(Bundle outstate) {
        Log.d(TAG, "saving instance state!");
        // message i/o list
        outstate.putInt("msg_pos", mMsgListView.getLastVisiblePosition());
        if (mMsgAdapter != null) {
            mMsgAdapter.onSaveInstanceState(outstate);
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        AlertDialog.Builder builder = new AlertDialog.Builder(ReaderActivity.this, R.style.dialog);
        final LayoutInflater li = getLayoutInflater();
        Dialog dialog = null;
        switch(id) {
        case DIALOG_NEW_APP: {
            final View view = li.inflate(R.layout.new_app, null);
            builder.setView(view)
            .setCancelable(false)
            .setIcon(R.drawable.credit_card_add_dark)
            .setTitle(R.string.new_app_title)
            .setPositiveButton(R.string.dialog_ok, null)
            .setNegativeButton(R.string.dialog_cancel,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });

            mNewDialog = builder.create();
            dialog = mNewDialog;        
            dialog.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface di) {
                    Button b = mNewDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                    b.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            EditText appName = (EditText)view.findViewById(R.id.app_name);
                            EditText appAid = (EditText)view.findViewById(R.id.app_aid);

                            // validate name and aid
                            String name = appName.getText().toString();
                            String aid = appAid.getText().toString();
                            if (name.isEmpty()) {
                                showToast(getString(R.string.empty_name));
                                return;
                            }
                            if (aid.length() < 10 || aid.length() > 32 || aid.length() % 2 != 0) {
                                showToast(getString(R.string.invalid_aid));
                                return;
                            }
                            // ensure name is unique (aid can be dup)
                            for (SmartcardApp app : mApps) {
                                if (app.getName().equals(name)) {
                                    showToast(getString(R.string.name_exists, name));
                                    return;
                                }
                            }
                            // app type radio group
                            RadioGroup appTypeGrp = (RadioGroup)view.findViewById(R.id.radio_grp_type);
                            int selectedId = appTypeGrp.getCheckedRadioButtonId();
                            RadioButton radioBtn = (RadioButton)view.findViewById(selectedId);
                            int type = radioBtn.getText().toString().equals(getString(R.string.radio_payment)) ?
                                SmartcardApp.TYPE_PAYMENT : SmartcardApp.TYPE_OTHER;

                            // current app checkbox
                            CheckBox cbCurrent = (CheckBox)view.findViewById(R.id.make_current);
                            if (cbCurrent.isChecked()) {
                                mSelectedAppPos = mApps.size();
                            }

                            // update apps list
                            SmartcardApp newApp = new SmartcardApp(appName.getText().toString(),
                                appAid.getText().toString(), type);
                            Log.d(TAG, "newApp: " + newApp);
                            synchronized(mApps) {
                                mApps.add(newApp);
                                if (mApps.size() == NUM_RO_APPS + 1) {
                                   // enable edit menu item
                                   mEditMenuItem.setEnabled(true); 
                                }
                            }

                            mAidSpinner.setAdapter(mAppAdapter);
                            mAidSpinner.setSelection(mSelectedAppPos);
                            mAppAdapter.notifyDataSetChanged();

                            // write apps to shared prefs
                            new writePrefsTask().execute();
                            mNewDialog.dismiss();
                        }
                    });
                }
            });
            break;
        } // case
        case DIALOG_EDIT_APP: {
            final View view = li.inflate(R.layout.new_app, null);
            builder.setView(view)
            .setCancelable(false)
            .setIcon(R.drawable.credit_card_edit_dark)
            .setTitle(R.string.new_app_title)
            .setPositiveButton(R.string.dialog_ok, null)
            .setNegativeButton(R.string.dialog_cancel,
                new DialogInterface.OnClickListener() {
                    @SuppressWarnings("deprecation")
                    public void onClick(DialogInterface dialog, int id) {
                        dismissKeyboard(mEditDialog.getCurrentFocus());
                        showDialog(DIALOG_EDIT_ALL_APPS);
                        dialog.cancel();
                    }
                });

            mEditDialog = builder.create();
            dialog = mEditDialog;

            dialog.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface di) {
                    Button b = mEditDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                    b.setOnClickListener(new View.OnClickListener() {
                        @SuppressWarnings("deprecation")
                        public void onClick(View v) {
                            EditText appName = (EditText)view.findViewById(R.id.app_name);
                            EditText appAid = (EditText)view.findViewById(R.id.app_aid);

                            // validate name and aid
                            String name = appName.getText().toString();
                            String aid = appAid.getText().toString();
                            if (name.isEmpty()) {
                                showToast(getString(R.string.empty_name));
                                return;
                            }
                            if (aid.length() < 10 || aid.length() > 32 || aid.length() % 2 != 0) {
                                showToast(getString(R.string.invalid_aid));
                                return;
                            }
                            // ensure name is unique
                            for (int i = 0; i < mApps.size(); i++) {
                                // skip the app being edited
                                if (i == mEditPos) continue;
                                SmartcardApp app = mApps.get(i);
                                if (app.getName().equals(name)) {
                                    showToast(getString(R.string.name_exists, name));
                                    return;
                                }
                            }
                            // app type radio group
                            RadioGroup appTypeGrp = (RadioGroup)view.findViewById(R.id.radio_grp_type);
                            int selectedId = appTypeGrp.getCheckedRadioButtonId();
                            RadioButton radioBtn = (RadioButton)view.findViewById(selectedId);
                            int type = radioBtn.getText().toString().equals(getString(R.string.radio_payment)) ?
                                SmartcardApp.TYPE_PAYMENT : SmartcardApp.TYPE_OTHER;

                            // current app checkbox
                            CheckBox cbCurrent = (CheckBox)view.findViewById(R.id.make_current);
                            if (cbCurrent.isChecked()) {
                                mSelectedAppPos = mEditPos;
                            }

                            // update apps list
                            SmartcardApp app;
                            synchronized(mApps) {
                                app = mApps.get(mEditPos);
                                app.setName(name);
                                app.setAid(aid);
                                app.setType(type);
                            }
                            Log.d(TAG, "app: " + app);

                            mAidSpinner.setSelection(mSelectedAppPos);
                            mAppAdapter.notifyDataSetChanged();

                            SmartcardApp subApp = mEditAllAdapter.getItem(mEditPos - NUM_RO_APPS);
                            subApp.copy(app);
                            mEditAllAdapter.notifyDataSetChanged();
                            mEditAllListView.setAdapter(mEditAllAdapter);

                            // write shared prefs in another thread
                            new writePrefsTask().execute();
                            dismissKeyboard(mEditDialog.getCurrentFocus());
                            showDialog(DIALOG_EDIT_ALL_APPS);
                            mEditDialog.dismiss();
                        }
                    });
                }
            });
            break;
        } // case
        case DIALOG_EDIT_ALL_APPS: {
            final View view = li.inflate(R.layout.edit_apps, null);
            final ListView listView = (ListView)view.findViewById(R.id.listView);
            mEditAllListView = listView;

            ArrayList<SmartcardApp> sl =
                new ArrayList<SmartcardApp>(mApps.subList(NUM_RO_APPS, mApps.size()));
            mEditAllAdapter = new AppAdapter(this, sl, null, true);
            listView.setAdapter(mEditAllAdapter);
            listView.setOnItemClickListener(new ListView.OnItemClickListener() {
                @SuppressWarnings("deprecation")
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
                    mEditPos = NUM_RO_APPS + pos;
                    showDialog(DIALOG_EDIT_APP);
                    mEditAllDialog.dismiss();
                }
            });
            listView.setOnItemLongClickListener(new ListView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View view, int pos, long id) {
                    // TODO: confirmation dialog or discard icon?
                    mApps.remove(NUM_RO_APPS + pos);
                    if (mSelectedAppPos == NUM_RO_APPS + pos) {
                        mSelectedAppPos = 0;
                    } else
                    if (mSelectedAppPos > NUM_RO_APPS + pos) {
                        mSelectedAppPos--;
                    }

                    mAidSpinner.setAdapter(mAppAdapter);
                    mAidSpinner.setSelection(mSelectedAppPos);
                    mAppAdapter.notifyDataSetChanged();

                    SmartcardApp app = mEditAllAdapter.getItem(pos);
                    mEditAllAdapter.remove(app);
                    mEditAllAdapter.notifyDataSetChanged();
                    listView.setAdapter(mEditAllAdapter);

                    new writePrefsTask().execute();
                    if (mApps.size() == NUM_RO_APPS) {
                        parent.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                        mEditAllDialog.dismiss();
                        mEditMenuItem.setEnabled(false);
                    }
                    return true;
                }
            });         

            builder.setView(view)
            .setCancelable(false)
            .setIcon(R.drawable.credit_card_edit_dark)
            .setTitle(R.string.edit_apps_title)
            .setPositiveButton(R.string.dialog_done, null);

            mEditAllDialog = builder.create();
            dialog = mEditAllDialog;
            break;
        } // case
        case DIALOG_ENABLE_NFC: {
            final View view = li.inflate(R.layout.enable_nfc, null);
            builder.setView(view)
            .setCancelable(false)
            .setIcon(R.drawable.ic_enable_nfc)
            .setTitle(R.string.nfc_disabled)
            .setPositiveButton(R.string.dialog_ok,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // take user to wireless settings
                        startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
                    }
                })
            .setNegativeButton(R.string.dialog_quit,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        finish(); 
                    }
                });

            mEnableNfcDialog = builder.create();
            dialog = mEnableNfcDialog;
            break;
        } // case
        } // switch
        return dialog;
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        switch(id) {
            case DIALOG_NEW_APP: {
                EditText name = (EditText)dialog.findViewById(R.id.app_name);
                EditText aid = (EditText)dialog.findViewById(R.id.app_aid);
                RadioGroup type = (RadioGroup)dialog.findViewById(R.id.radio_grp_type);
                CheckBox current = (CheckBox)dialog.findViewById(R.id.make_current);

                name.setText("");
                name.requestFocus();
                aid.setText("");
                type.check(R.id.radio_payment);
                current.setChecked(false);
                break;
            }
            case DIALOG_EDIT_APP: {
                EditText name = (EditText)dialog.findViewById(R.id.app_name);
                EditText aid = (EditText)dialog.findViewById(R.id.app_aid);
                RadioGroup type = (RadioGroup)dialog.findViewById(R.id.radio_grp_type);
                CheckBox current = (CheckBox)dialog.findViewById(R.id.make_current);

                SmartcardApp app = mApps.get(mEditPos);
                name.setText(app.getName());
                name.requestFocus();
                aid.setText(app.getAid());
                type.check((app.getType() == SmartcardApp.TYPE_OTHER) ?
                    R.id.radio_other : R.id.radio_payment);
                current.setChecked(mEditPos == mSelectedAppPos);
                current.setEnabled(mEditPos != mSelectedAppPos);
                break;
            }
            case DIALOG_EDIT_ALL_APPS: {
                ListView listView = (ListView)dialog.findViewById(R.id.listView);
                ArrayList<SmartcardApp> sl =
                    new ArrayList<SmartcardApp>(mApps.subList(NUM_RO_APPS, mApps.size()));
                mEditAllAdapter = new AppAdapter(this, sl, null, true);
                listView.setAdapter(mEditAllAdapter);
                break;
            }
            case DIALOG_ENABLE_NFC: {
                break;
            }
        }
    }

    private void dismissKeyboard(View focus) {
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(focus.getWindowToken(), 0);    
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        mEditMenuItem = menu.findItem(R.id.menu_edit_all_apps);
        if (mApps.size() > NUM_RO_APPS) {
            mEditMenuItem.setEnabled(true);
        }
        return true;
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_new_app:
                showDialog(DIALOG_NEW_APP);
                return true;

            case R.id.menu_edit_all_apps:
                showDialog(DIALOG_EDIT_ALL_APPS);
                return true;
                
            case R.id.menu_clear_msgs:
                clearMessages();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showToast(String text) {
        Toast toast = Toast.makeText(ReaderActivity.this, text, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER_VERTICAL, 0, -100);
        toast.show();
    }

	@Override
	public void onTagDiscovered(Tag tag) {
	    // first clear messages
	    if (mSkipNextClear) {
	        mSkipNextClear = false;
	    } else {
            clearMessages();
	    }
        // get IsoDep handle and run xcvr thread
		IsoDep isoDep = IsoDep.get(tag);
		if (isoDep == null) {
		    onError(getString(R.string.wrong_tag_err), true);
		} else {
		    ReaderXcvr xcvr;
		    String aid = mApps.get(mSelectedAppPos).getAid();
		    if (mDemoAid.equals(aid)) {
		        xcvr = new DemoReaderXcvr(isoDep, aid, this);
		    } else
		    if (mApps.get(mSelectedAppPos).getType() == 0) {
		        xcvr = new PaymentReaderXcvr(isoDep, aid, this);
		    } else {
		        xcvr = new OtherReaderXcvr(isoDep, aid, this);
		    }
		    new Thread(xcvr).start();
		}
	}

    @Override
    public void onMessageSend(final String message) {
        onMessageAndType(message, MessageAdapter.MSG_SEND);
    }	

    @Override
	public void onMessageRcv(final String message) {
        onMessageAndType(message, MessageAdapter.MSG_RCV);
	}

    @Override
    public void onOkay(final String message) {
        onMessageAndType(message, MessageAdapter.MSG_OKAY);
    }
    
	@Override
	public void onError(final String message, boolean skipNextClear) {
	    onMessageAndType(message, MessageAdapter.MSG_ERROR);
        mSkipNextClear = skipNextClear;
	}

	private void onMessageAndType(final String message, final int type) {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                mMsgAdapter.addMessage(message, type);
            }
        });
	}
	
	private void clearMessages() {
        if (mMsgAdapter != null) {
            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    mMsgAdapter.clearMessages();
                }
            });
        }	    
	}

	private void writePrefs() {
        StringBuffer names = new StringBuffer(APP_NAMES);
        StringBuffer aids = new StringBuffer(APP_AIDS);
        StringBuffer types = new StringBuffer(APP_TYPES);

        if (mApps != null) {
            synchronized(mApps) {
                for (int i = NUM_RO_APPS; i < mApps.size(); i++) {
                    SmartcardApp app = mApps.get(i);
                    names.append("|" + app.getName());
                    aids.append("|" + app.getAid());
                    types.append("|" + app.getType());
                }
            }
        }

        mEditor.putString("app_names", names.toString());
        mEditor.putString("app_aids", aids.toString());
        mEditor.putString("app_types", types.toString());
        mEditor.putInt("selected_aid_pos", mSelectedAppPos);
        mEditor.commit();	    
	}
	
    private class writePrefsTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... v) {
            writePrefs();
            return null;
        }
    }
}
