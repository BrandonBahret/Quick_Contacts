package com.example.brandon.quickcontacts;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;

import java.util.ArrayList;

public class ConfirmationDialog extends AlertDialog.Builder {
    private ArrayList<OnFinished> onFinishedListeners = new ArrayList<OnFinished>();

    public void setOnFinished(OnFinished listener){
        onFinishedListeners.add(listener);
    }

    public interface OnFinished{
        void onFinished(boolean result);
    }

    private Context context;
    private AlertDialog dialog;

    private String title, message, positiveButtonText, negativeButtonText;

    public ConfirmationDialog(Context context, String title, String message,
                              String positiveButtonText, String negativeButtonText){
        super(context);
        this.context = context;

        this.title = title;
        this.message = message;
        this.positiveButtonText = positiveButtonText;
        this.negativeButtonText = negativeButtonText;
    }

    @Override
    public AlertDialog create() {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(message)
                .setTitle(title)
                .setPositiveButton(positiveButtonText, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        for(OnFinished listeners: onFinishedListeners){
                            listeners.onFinished(true);
                        }
                    }
                })
                .setNegativeButton(negativeButtonText, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        for(OnFinished listeners: onFinishedListeners){
                            listeners.onFinished(false);
                        }
                    }
                });

        dialog = builder.create();

        return dialog;
    }
}
