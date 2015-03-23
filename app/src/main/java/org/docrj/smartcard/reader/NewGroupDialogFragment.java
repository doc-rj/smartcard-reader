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

import android.annotation.SuppressLint;
import android.app.FragmentManager;
import android.widget.EditText;


public class NewGroupDialogFragment extends GroupNameDialogFragment {

    public static final String FRAGMENT_TAG = "newGroupDialog";

    private final OnNewGroupListener mListener;

    public interface OnNewGroupListener {
        public void onNewGroup(String name);
    }

    public static void show(
            FragmentManager fragmentManager, OnNewGroupListener listener) {
        NewGroupDialogFragment dialog = new NewGroupDialogFragment(listener);
        dialog.show(fragmentManager, FRAGMENT_TAG);
    }

    public NewGroupDialogFragment() {
        super();
        mListener = null;
    }

    @SuppressLint("ValidFragment")
    private NewGroupDialogFragment(OnNewGroupListener listener) {
        super();
        mListener = listener;
    }

    @Override
    protected void initializeGroupLabelEditText(EditText editText) {
    }

    @Override
    protected int getTitleResourceId() {
        return R.string.title_dialog_new_group;
    }

    @Override
    protected void onCompleted(String name) {
        if (mListener != null) {
            mListener.onNewGroup(name);
        }
    }
}
