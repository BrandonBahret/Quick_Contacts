package com.example.brandon.quickcontacts;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view){
                DialogFragment dialog = new NewContactDialog();
                dialog.show(getSupportFragmentManager(), "new contact");
            }
        });

        LinearLayout contactLayout = (LinearLayout)findViewById(R.id.contacts_container);

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(0, 0, 0, 45);

        for(int i=0;i<100;i++){
            View v = View.inflate(this, R.layout.contact_layout, null);
            v.setLayoutParams(layoutParams);
            contactLayout.addView(v);
        }

    }

    public void editContact(View view) {
        View v = (View)view.getParent().getParent();
        String name = ((TextView)v.findViewById(R.id.name)).getText().toString();
        String phoneNumber = ((TextView)v.findViewById(R.id.phonenumber)).getText().toString();

        Bundle args = new Bundle();
        args.putString("name", name);
        args.putString("phone number", phoneNumber);

        EditContactDialog dialog = new EditContactDialog();
        dialog.setArguments(args);

        dialog.setOnFinishedListener(new EditContactDialog.OnFinishedListener() {
            @Override
            public void onFinishedWithResult(int result) {
                if(result == EditContactDialog.DELETE){
                    ConfirmationDialog confirmationDialog = new ConfirmationDialog(MainActivity.this,
                            "Are You Sure?", "About to delete contact", "Okay", "Cancel");

                    confirmationDialog.setOnFinished(new ConfirmationDialog.OnFinished() {
                        @Override
                        public void onFinished(boolean result) {
                            if(result){
                                Toast.makeText(MainActivity.this, "Delete contact", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

                    confirmationDialog.show();
                }
            }
        });

        dialog.show(getSupportFragmentManager(), "edit contact");
    }

    public void callContact(View view) {
        View v = (View)view.getParent().getParent();
        final String name = ((TextView)v.findViewById(R.id.name)).getText().toString();
        final String phoneNumber = ((TextView)v.findViewById(R.id.phonenumber)).getText().toString();

        ConfirmationDialog dialog = new ConfirmationDialog(this, "Are You Sure?",
                "About to call " + name, "Make Call", "Cancel");

        dialog.setOnFinished(new ConfirmationDialog.OnFinished() {
            @Override
            public void onFinished(boolean result) {
                if(result){
                    Toast.makeText(getApplicationContext(), "call contact " + phoneNumber, Toast.LENGTH_SHORT).show();
                }
            }
        });

        dialog.show();
    }

    public void messageContact(View view) {
        View v = (View)view.getParent().getParent();
        final String name = ((TextView)v.findViewById(R.id.name)).getText().toString();
        final String phoneNumber = ((TextView)v.findViewById(R.id.phonenumber)).getText().toString();

        ConfirmationDialog dialog = new ConfirmationDialog(this, "Are You Sure?",
                "Start conversation with " + name, "Open Messenger", "Cancel");

        dialog.setOnFinished(new ConfirmationDialog.OnFinished() {
            @Override
            public void onFinished(boolean result) {
                if(result){
                    Toast.makeText(getApplicationContext(),
                            "message contact " + phoneNumber, Toast.LENGTH_SHORT).show();
                }
            }
        });

        dialog.show();

    }
}
