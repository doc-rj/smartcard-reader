/*
 * Copyright 2015 Ryan Jones
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
import java.util.List;

public class BatchReaderXcvr implements Runnable, ReaderXcvr.UiCallbacks {

    private final List<SmartcardApp> mApps;
    private final IsoDep mIsoDep;
    private ReaderXcvr.UiCallbacks mUiCallbacks;
    private Context mContext;

    private int mSize;
    private int mIndex;

    public BatchReaderXcvr(IsoDep isoDep, List<SmartcardApp> apps,
                           ReaderXcvr.UiCallbacks uiCallbacks) {
        mIsoDep = isoDep;
        mApps = apps;
        mUiCallbacks = uiCallbacks;
        mContext = (Context) uiCallbacks;
        mSize = apps.size();
        mIndex = 0;
    }

    private void selectNextApp() {
        // using onOkay() generically as app name, header for each app select
        onOkay(mApps.get(mIndex).getName());
        new Thread(new OtherReaderXcvr(mIsoDep, mApps.get(mIndex++).getAid(),
                this, mContext)).start();
    }

    @Override
    public void run() {
        selectNextApp();
    }

    // callback for each select completion
    @Override
    public void onFinish(boolean err) {
        if (err) {
            onError(mContext.getString(R.string.batch_interrupted));
        } else if (mIndex < mSize) {
            mUiCallbacks.onSeparator();
            selectNextApp();
        } else {
            onOkay(mContext.getString(R.string.batch_complete));
        }
    }

    // display console messages
    @Override
    public void onMessageSend(String raw, String name) {
        mUiCallbacks.onMessageSend(raw, name);
    }

    @Override
    public void onMessageRcv(String raw, String name, String parsed) {
        mUiCallbacks.onMessageRcv(raw, name, parsed);
    }

    @Override
    public void onOkay(String message) {
        mUiCallbacks.onOkay(message);
    }

    @Override
    public void onError(String message) {
        mUiCallbacks.onError(message);
    }

    @Override
    public void onSeparator() {
        mUiCallbacks.onSeparator();
    }

    // clear console messages
    @Override
    public void clearMessages(){
        mUiCallbacks.clearMessages();
    }

    // ui listeners
    @Override
    public void setUserSelectListener(ReaderXcvr.UiListener callback) {
        mUiCallbacks.setUserSelectListener(callback);
    }
}
