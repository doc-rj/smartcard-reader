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


public class RenameGroupDialogFragment extends GroupNameDialogFragment {

    public static final String FRAGMENT_TAG = "renameGroupDialog";

    private String mOldName;
    private final OnRenameGroupListener mListener;

    public interface OnRenameGroupListener {
        public void onRenameGroup(String name);
    }

    public static void show(
            FragmentManager fragmentManager, String oldName, OnRenameGroupListener listener) {
        RenameGroupDialogFragment dialog = new RenameGroupDialogFragment(oldName, listener);
        dialog.show(fragmentManager, FRAGMENT_TAG);
    }

    public RenameGroupDialogFragment() {
        super();
        mListener = null;
    }

    @SuppressLint("ValidFragment")
    private RenameGroupDialogFragment(String oldName, OnRenameGroupListener listener) {
        super();
        mOldName = oldName;
        mListener = listener;
    }

    @Override
    protected void initializeGroupLabelEditText(EditText editText) {
        editText.setText(mOldName);
    }

    @Override
    protected int getTitleResourceId() {
        return R.string.title_dialog_rename_group;
    }

    @Override
    protected void onCompleted(String name) {
        if (mListener != null) {
            mListener.onRenameGroup(name);
        }
    }
}
