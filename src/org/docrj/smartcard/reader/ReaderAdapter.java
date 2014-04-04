package org.docrj.smartcard.reader;

import java.util.ArrayList;
import java.util.List;

import org.docrj.smartcard.reader.R;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class ReaderAdapter extends BaseAdapter {
    
    private class Message {
        private String text;
        private int type;
    
        Message(String text, int type) {
            this.text = text;
            this.type = type;
        }
    };

	private LayoutInflater mLayoutInflater;
	private List<Message> mMessages = new ArrayList<Message>(100);
	private Context mContext;

	public ReaderAdapter(LayoutInflater layoutInflater, Bundle instate) {
		this.mLayoutInflater = layoutInflater;
		this.mContext = layoutInflater.getContext();
        if (instate != null) {
            ArrayList<String> strings = instate.getStringArrayList("strings");
            ArrayList<Integer> ints = instate.getIntegerArrayList("ints");
            for (int i = 0; i < strings.size(); i++) {
                mMessages.add(new Message(strings.get(i), ints.get(i)));
            }
        }		
	}

    public void onSaveInstanceState(Bundle outstate) {
        ArrayList<String> strings = new ArrayList<String>(mMessages.size());
        ArrayList<Integer> ints = new ArrayList<Integer>(mMessages.size()); 
        for (Message msg : mMessages) {
            strings.add(msg.text);
            ints.add(msg.type);
        }
        outstate.putStringArrayList("strings", strings);
        outstate.putIntegerArrayList("ints", ints);
    }	

	public void addMessage(String message, int type) {
		String prefix = (type == 0) ? "<--" : (type == 1) ? "-->" : "!!";
		mMessages.add(new Message(prefix + message, type));
		notifyDataSetChanged();
	}
	
	private CharSequence getItemText(int position) {
	    return (CharSequence)mMessages.get(position).text;
	}
	
	private int getItemType(int position) {
	    return mMessages.get(position).type;
	}

	@Override
	public int getCount() {
		return mMessages == null ? 0 : mMessages.size();
	}

	@Override
	public Object getItem(int position) {
		return mMessages.get(position);
	}

	@Override
	public long getItemId(int position) {
		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (convertView == null) {
			convertView = mLayoutInflater.inflate(R.layout.list_item_1, parent, false);
		}
		TextView view = (TextView)convertView.findViewById(R.id.list_item_text);
		view.setText(getItemText(position));
		int type = getItemType(position);
		int color = (type == 0) ? android.R.color.black :
		    (type == 1) ? android.R.color.holo_blue_dark : android.R.color.holo_red_light;
		view.setTextColor(mContext.getResources().getColor(color));
		return convertView;
	}
}
