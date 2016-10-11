package com.example.brandon.quickcontacts;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.telephony.PhoneNumberFormattingTextWatcher;
import android.view.View;
import android.widget.EditText;

import java.util.ArrayList;

public class EditContactDialog extends DialogFragment {

    private ArrayList<OnFinishedListener> onFinishedListeners = new ArrayList<OnFinishedListener>();

    public void setOnFinishedListener(OnFinishedListener listener){
        onFinishedListeners.add(listener);
    }

    public interface OnFinishedListener{
        void onFinishedWithResult(int result, @Nullable Bundle params);
    }

    public static final int DELETE = 2, UPDATE = 3;
    public View contentView;

    private String oldName, oldPhoneNumber;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Edit Contact")
                .setPositiveButton(R.string.update_contact, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Bundle params = new Bundle();

                        Dialog theDialog = Dialog.class.cast(dialog);

                        String newName = ((EditText)theDialog.findViewById(R.id.nameEditText))
                                .getText().toString();

                        if(newName.length() > 0) {

                            String newPhoneNumber = ((EditText) theDialog.findViewById(R.id.phoneNumberEditText))
                                    .getText().toString();

                            params.putString("old_name", oldName);
                            params.putString("new_name", newName);
                            params.putString("new_phone_number", newPhoneNumber);

                            for (OnFinishedListener listener : onFinishedListeners) {
                                listener.onFinishedWithResult(UPDATE, params);
                            }

                        }
                    }
                })
                .setNeutralButton(R.string.delete_contact, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        for(OnFinishedListener listener: onFinishedListeners){
                            listener.onFinishedWithResult(DELETE, null);
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                    }
                });

        builder.setView(getView());

        return builder.create();
    }

    @Nullable
    @Override
    public View getView() {
        contentView = View.inflate(getContext(), R.layout.contact_edit, null);

        Bundle args = getArguments();
        oldName = args.getString("name");
        oldPhoneNumber = args.getString("phone_number");

        EditText nameEdit = (EditText)contentView.findViewById(R.id.nameEditText);
        nameEdit.setText(oldName);

        final EditText phoneEdit = (EditText)contentView.findViewById(R.id.phoneNumberEditText);
        phoneEdit.addTextChangedListener(new PhoneNumberFormattingTextWatcher());
        phoneEdit.setText(oldPhoneNumber);

        return contentView;
    }
}

