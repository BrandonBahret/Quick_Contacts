package com.example.brandon.quickcontacts;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.telephony.PhoneNumberFormattingTextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.EditText;

import java.util.ArrayList;

public class NewContactDialog extends DialogFragment {

    private ArrayList<OnFinishedListener> onFinishedListeners = new ArrayList<>();

    public void setOnFinishedListener(OnFinishedListener listener){
        onFinishedListeners.add(listener);
    }

    public interface OnFinishedListener{
        void onFinishedWithResult(String name, String phoneNumber);
    }

    View contentView;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

        contentView = View.inflate(getContext(), R.layout.contact_edit, null);

        EditText phoneEdit = (EditText)contentView.findViewById(R.id.phoneNumberEditText);
        phoneEdit.addTextChangedListener(new PhoneNumberFormattingTextWatcher());

        EditText nameEdit = (EditText)contentView.findViewById(R.id.nameEditText);
        nameEdit.addTextChangedListener(new PasswordTransformationMethod());

        builder.setView(contentView);

        builder.setTitle(R.string.new_contact_dialog_title)
                .setPositiveButton(R.string.add_contact, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        String name = ((EditText)contentView.findViewById(R.id.nameEditText))
                                .getText().toString();

                        if(name.length() > 0) {

                            String phoneNumber = ((EditText) contentView.findViewById(R.id.phoneNumberEditText))
                                    .getText().toString();

                            for(OnFinishedListener listener:onFinishedListeners){
                                listener.onFinishedWithResult(name, phoneNumber);
                            }

                        }
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                    }
                });


        return builder.create();
    }
}

