package com.example.brandon.quickcontacts;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.util.ArrayList;

public class EditContactDialog extends DialogFragment {

    private ArrayList<OnFinishedListener> onFinishedListeners = new ArrayList<OnFinishedListener>();

    public void setOnFinishedListener(OnFinishedListener listener){
        onFinishedListeners.add(listener);
    }

    public interface OnFinishedListener{
        void onFinishedWithResult(int result);
    }

    public static final int DELETE = 2;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Edit Contact")
                .setPositiveButton(R.string.update_contact, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Toast.makeText(getContext(), "Add Contact", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNeutralButton(R.string.delete_contact, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        for(OnFinishedListener listener: onFinishedListeners){
                            listener.onFinishedWithResult(DELETE);
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
        View view = View.inflate(getContext(), R.layout.contact_edit, null);

        Bundle args = getArguments();
        String name = args.getString("name");
        String phoneNumber = args.getString("phone number");

        ((EditText)view.findViewById(R.id.nameEditText))
                .setText(name);

        ((EditText)view.findViewById(R.id.phoneNumberEditText))
                .setText(phoneNumber);

        return view;
    }
}

