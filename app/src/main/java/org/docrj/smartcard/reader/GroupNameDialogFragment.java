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

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnShowListener;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import com.afollestad.materialdialogs.AlertDialogWrapper;

/**
 * superclass for creating and renaming groups.
 */
public abstract class GroupNameDialogFragment extends DialogFragment {
    protected abstract int getTitleResourceId();
    protected abstract void initializeGroupLabelEditText(EditText editText);
    protected abstract void onCompleted(String groupLabel);

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
        final View view = layoutInflater.inflate(R.layout.dialog_new_group, null);
        final EditText editText = (EditText) view.findViewById(R.id.group_name);
        initializeGroupLabelEditText(editText);
        editText.requestFocus();

        //AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final AlertDialogWrapper.Builder builder = new AlertDialogWrapper.Builder(getActivity());
        builder.setView(view)
                .setCancelable(true)
                .setTitle(getTitleResourceId())
                .setPositiveButton(R.string.dialog_ok,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int whichButton) {
                            onCompleted(editText.getText().toString().trim());
                        }
                    }
                )
                .setNegativeButton(R.string.dialog_cancel, null);
        final AlertDialog dialog = (AlertDialog) builder.create();

        dialog.setOnShowListener(new OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                updateOkButtonState(dialog, editText);
            }
        });

        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                updateOkButtonState(dialog, editText);
            }
        });

        dialog.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        return dialog;
    }

    void updateOkButtonState(AlertDialog dialog, EditText editText) {
        final Button okButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        okButton.setEnabled(!TextUtils.isEmpty(editText.getText().toString().trim()));
    }
}
