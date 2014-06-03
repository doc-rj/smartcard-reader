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
import java.util.List;

import org.docrj.smartcard.reader.R;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class MessageAdapter extends BaseAdapter {

    public static final int MSG_SEND = 1;
    public static final int MSG_RCV = 2;
    public static final int MSG_OKAY = 0;
    public static final int MSG_ERROR = -1;

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

    public MessageAdapter(LayoutInflater layoutInflater, Bundle instate) {
        this.mLayoutInflater = layoutInflater;
        this.mContext = layoutInflater.getContext();
        if (instate != null) {
            // restore state
            ArrayList<String> strings = instate
                    .getStringArrayList("msg_strings");
            ArrayList<Integer> ints = instate.getIntegerArrayList("msg_ints");
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
        outstate.putStringArrayList("msg_strings", strings);
        outstate.putIntegerArrayList("msg_ints", ints);
    }

    public void clearMessages() {
        mMessages.clear();
        notifyDataSetChanged();
    }

    public void addMessage(String message, int type) {
        String prefix = "";
        switch (type) {
        case MSG_SEND:
            prefix = mContext.getString(R.string.out_msg_prefix);
            break;
        case MSG_RCV:
            prefix = mContext.getString(R.string.in_msg_prefix);
            break;
        case MSG_OKAY:
            prefix = mContext.getString(R.string.okay_msg_prefix);
            break;
        case MSG_ERROR:
            prefix = mContext.getString(R.string.err_msg_prefix);
            break;
        }
        mMessages.add(new Message(prefix + message, type));
        notifyDataSetChanged();
    }

    private CharSequence getItemText(int position) {
        return (CharSequence) mMessages.get(position).text;
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
            convertView = mLayoutInflater.inflate(R.layout.list_item_1, parent,
                    false);
        }
        TextView view = (TextView) convertView
                .findViewById(R.id.list_item_text);
        view.setText(getItemText(position));
        int type = getItemType(position);
        int color = android.R.color.black;
        switch (type) {
        case MSG_SEND:
            color = R.color.msg_send;
            break;
        case MSG_RCV:
            color = R.color.msg_rcv;
            break;
        case MSG_OKAY:
            color = R.color.msg_okay;
            break;
        case MSG_ERROR:
            color = R.color.msg_err;
            break;
        }
        view.setTextColor(mContext.getResources().getColor(color));
        return convertView;
    }
}
