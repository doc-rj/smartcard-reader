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

import java.util.List;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class AppAdapter extends ArrayAdapter<SmartcardApp> {

    private Activity mContext;
    List<SmartcardApp> mApps;
    boolean mBrief;

    public AppAdapter(Activity context, List<SmartcardApp> apps, boolean brief) {
        super(context, R.layout.spinner_apps_list, apps);
        mContext = context;
        mApps = apps;
        mBrief = brief;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View item = convertView;
        if (item == null) {
            LayoutInflater inflater = mContext.getLayoutInflater();
            item = inflater.inflate(R.layout.spinner_apps_list, parent, false);
        }
        SmartcardApp current = mApps.get(position);
        TextView textView = (TextView) item.findViewById(android.R.id.text1);
        textView.setText(mBrief ? current.toBriefString() : current.toString());
        Drawable img;
        if (current.getType() == SmartcardApp.TYPE_PAYMENT) {
            img = mContext.getResources().getDrawable(R.drawable.credit_card2_green);
        } else {
            img = mContext.getResources().getDrawable(R.drawable.credit_card2_blue);
        }
        textView.setCompoundDrawablesWithIntrinsicBounds(img, null, null, null);
        return item;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        if (row == null) {
            LayoutInflater inflater = mContext.getLayoutInflater();
            row = inflater.inflate(R.layout.spinner_dropdown_apps_list, parent, false);
        }
        SmartcardApp current = mApps.get(position);
        TextView textView = (TextView) row.findViewById(android.R.id.text1);
        textView.setText(mBrief ? current.toBriefString() : current.toString());
        Drawable img;
        if (current.getType() == SmartcardApp.TYPE_PAYMENT) {
            img = mContext.getResources().getDrawable(R.drawable.credit_card_green);
        } else {
            img = mContext.getResources().getDrawable(R.drawable.credit_card_blue);
        }
        textView.setCompoundDrawablesWithIntrinsicBounds(img, null, null, null);
        return row;
    }
}
