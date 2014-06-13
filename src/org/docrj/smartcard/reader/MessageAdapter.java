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
import android.widget.Button;
import android.widget.ImageView;

public class MessageAdapter extends BaseAdapter {

    public interface OnDialog {
        void onDialogParsedMsg(String name, String text);
    }

    public static final int MSG_SEND = 1;
    public static final int MSG_RCV = 2;
    public static final int MSG_OKAY = 0;
    public static final int MSG_ERROR = -1;

    private class Message {
        private String text;
        private int type;
        private String name;
        private String parsed;

        Message(String text, int type, String name, String parsed) {
            this.text = text;
            this.type = type;
            this.name = name;
            this.parsed = parsed;
        }
    };

    private LayoutInflater mLayoutInflater;
    private List<Message> mMessages = new ArrayList<Message>(100);
    private Context mContext;
    private OnDialog mOnDialog;

    public MessageAdapter(LayoutInflater layoutInflater, Bundle instate, OnDialog onDialog) {
        mLayoutInflater = layoutInflater;
        mContext = layoutInflater.getContext();
        mOnDialog = onDialog;
        if (instate != null) {
            // restore state
            ArrayList<String> text = instate.getStringArrayList("msg_text");
            ArrayList<Integer> type = instate.getIntegerArrayList("msg_type");
            ArrayList<String> name = instate.getStringArrayList("msg_name");
            ArrayList<String> parsed = instate.getStringArrayList("msg_parsed");
            for (int i = 0; i < text.size(); i++) {
                mMessages.add(new Message(text.get(i), type.get(i),
                    name.get(i), parsed.get(i)));
            }
        }
    }

    public void onSaveInstanceState(Bundle outstate) {
        ArrayList<String> text = new ArrayList<String>(mMessages.size());
        ArrayList<Integer> type = new ArrayList<Integer>(mMessages.size());
        ArrayList<String> name = new ArrayList<String>(mMessages.size());
        ArrayList<String> parsed = new ArrayList<String>(mMessages.size());
        for (Message msg : mMessages) {
            text.add(msg.text);
            type.add(msg.type);
            name.add(msg.name);
            parsed.add(msg.parsed);
        }
        outstate.putStringArrayList("msg_text", text);
        outstate.putIntegerArrayList("msg_type", type);
        outstate.putStringArrayList("msg_name", name);
        outstate.putStringArrayList("msg_parsed", parsed);
    }

    public void clearMessages() {
        mMessages.clear();
        notifyDataSetChanged();
    }

    public void addMessage(String text, int type, String name, String parsed) {
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
        Message msg = new Message(prefix + text, type, (name == null) ? "" : name,
            (parsed == null) ? "" : parsed);
        mMessages.add(msg);
        notifyDataSetChanged();
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
            convertView = mLayoutInflater.inflate(R.layout.list_item_2, parent,
                    false);
        }
        Button btn = (Button) convertView
                .findViewById(R.id.list_item_btn);
        ImageView img = (ImageView) convertView
                .findViewById(R.id.list_item_img);
        Message msg = (Message)getItem(position);
        btn.setText(msg.text);
        // handling based on presence of parsed message contents
        if (msg.parsed.isEmpty()) {
            btn.setEnabled(false);
            btn.setBackgroundResource(0);
            img.setVisibility(View.GONE);
        } else {
            btn.setEnabled(true);
            btn.setBackgroundResource(R.drawable.button_bg_states);
            img.setVisibility(View.VISIBLE);
            btn.setOnClickListener(new MessageClickListener(position));
        }
        // default style
        int color = android.R.color.black;
        // specific message type styles
        switch (msg.type) {
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
        btn.setTextColor(mContext.getResources().getColor(color));
        return convertView;
    }    

    private class MessageClickListener implements View.OnClickListener {
        int position;

        MessageClickListener(int position) {
            this.position = position;
        }
        
        @Override
        public void onClick(View v) {
            Message msg = (Message)getItem(position);
            String suffix = "";
            switch (msg.type) {
            case MSG_SEND:
                suffix = mContext.getString(R.string.cmd_suffix);
                break;
            case MSG_RCV:
                suffix = mContext.getString(R.string.rsp_suffix);
                break;
            }
            String name = msg.name + " " + suffix;
            mOnDialog.onDialogParsedMsg(name, msg.parsed);
        }
    };
}
