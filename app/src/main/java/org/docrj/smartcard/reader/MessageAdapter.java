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

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;

public class MessageAdapter extends BaseAdapter {
    private static final String TAG = "smartcard-reader";

    public interface UiCallbacks {
        void onViewParsedMsg(Bundle parsedMsg);
    }

    public static final int MSG_ERROR = -1;
    public static final int MSG_OKAY = 0;
    public static final int MSG_SEND = 1;
    public static final int MSG_RCV = 2;
    public static final int MSG_BREAK = 3;

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

        Message(int type) {
            this.type = type;
        }
    };

    private LayoutInflater mLayoutInflater;
    private Context mContext;
    private UiCallbacks mUiCallbacks;
    private List<Message> mMessages = new ArrayList<Message>(100);
    private StringBuilder mHtmlBuilder = new StringBuilder(2400);

    public MessageAdapter(LayoutInflater layoutInflater, Bundle instate, UiCallbacks uiCallbacks) {
        mLayoutInflater = layoutInflater;
        mContext = layoutInflater.getContext();
        mUiCallbacks = uiCallbacks;
        if (instate != null) {
            // restore state
            ArrayList<String> text = instate.getStringArrayList("msg_text");
            ArrayList<Integer> type = instate.getIntegerArrayList("msg_type");
            ArrayList<String> name = instate.getStringArrayList("msg_name");
            ArrayList<String> parsed = instate.getStringArrayList("msg_parsed");
            for (int i = 0; i < text.size(); i++) {
                Message msg = new Message(text.get(i), type.get(i),
                        name.get(i), parsed.get(i));
                mMessages.add(msg);
                updateShareMsgsHtml(msg);
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
        mHtmlBuilder.setLength(0);
        notifyDataSetChanged();
    }

    public void addMessage(String text, int type, String name, String parsed) {
        String textPrefix = "";
        String nameSuffix = "";
        switch (type) {
        case MSG_SEND:
            textPrefix = mContext.getString(R.string.out_msg_prefix);
            nameSuffix = mContext.getString(R.string.cmd_suffix);
            break;
        case MSG_RCV:
            textPrefix = mContext.getString(R.string.in_msg_prefix);
            nameSuffix = mContext.getString(R.string.rsp_suffix);
            break;
        case MSG_OKAY:
            textPrefix = mContext.getString(R.string.okay_msg_prefix);
            break;
        case MSG_ERROR:
            textPrefix = mContext.getString(R.string.err_msg_prefix);
            break;
        }
        String newText = textPrefix + text;
        String newName = (name == null) ? "" : name + " " + nameSuffix;

        Message msg = new Message(newText, type, newName,
            (parsed == null) ? "" : parsed);
        mMessages.add(msg);
        updateShareMsgsHtml(msg);
        notifyDataSetChanged();
    }

    public void addSeparator() {
        Message msg = new Message(MSG_BREAK);
        mMessages.add(msg);
        updateShareMsgsHtml(msg);
        notifyDataSetChanged();
    }

    private static String parsedToHtml(Message msg) {
        StringBuilder sb = new StringBuilder(1200);
        sb.append("<p style= 'font-family:courier new;'>");
        if (!msg.parsed.isEmpty()) {
            String name = (msg.type == MSG_SEND || msg.type == MSG_RCV) ?
                    "<b>" + msg.name + ":</b><br/><br/>" : "";
            String parsed = msg.parsed.replace("\n", "<br/>");
            sb.append(name + parsed);
        }
        sb.append("</p>");
        return sb.toString();
    }

    private void updateShareMsgsHtml(Message msg) {
        if (msg.type == MSG_BREAK) {
            mHtmlBuilder.append("<p style= 'font-family:courier new;'>=====</p>");
            return;
        }
        String name = (msg.type == MSG_SEND || msg.type == MSG_RCV) ?
                "<b>" + msg.name + " (raw):</b><br/><br/>" : "";
        String text = msg.text.replace("-->", "&#45;&#45;&gt;").replace("<--", "&lt;&#45;&#45;");
        mHtmlBuilder.append("<p style= 'font-family:courier new;'>" + name + text);
        if (!msg.parsed.isEmpty()) {
            name = (name.isEmpty()) ? "" : name.replace("(raw)", "(parsed)");
            String parsed = msg.parsed.replace("\n", "<br/>");
            mHtmlBuilder.append("<br/><br/>" + name + parsed);
        }
        mHtmlBuilder.append("</p>");
    }

    public String getShareMsgsHtml() {
        String html = mHtmlBuilder.toString();
        Log.d(TAG, "length: " + html.length());
        return html;
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
            convertView = mLayoutInflater.inflate(R.layout.parsed_msg, parent,
                    false);
        }
        Button btn = (Button) convertView
                .findViewById(R.id.list_item_btn);
        ImageView img = (ImageView) convertView
                .findViewById(R.id.list_item_img);
        View separator = convertView
                .findViewById(R.id.separator);

        Message msg = (Message)getItem(position);

        // handling for separator/break
        if (msg.type == MSG_BREAK) {
            btn.setVisibility(View.GONE);
            img.setVisibility(View.GONE);
            separator.setVisibility(View.VISIBLE);
            return convertView;
        }

        btn.setText(msg.text);
        btn.setVisibility(View.VISIBLE);
        // img visibility handled below
        separator.setVisibility(View.GONE);

        // handling based on presence of parsed message contents
        if (msg.parsed.isEmpty()) {
            btn.setEnabled(false);
            btn.setBackground(null);
            img.setVisibility(View.GONE);
        } else {
            btn.setEnabled(true);
            btn.setBackground(mContext.getResources().
                    getDrawable(R.drawable.parsed_msg_bg_states));
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
            Bundle b = new Bundle();
            b.putString("name", msg.name);
            b.putString("text", msg.parsed);
            b.putString("html", parsedToHtml(msg));
            mUiCallbacks.onViewParsedMsg(b);
        }
    };
}
