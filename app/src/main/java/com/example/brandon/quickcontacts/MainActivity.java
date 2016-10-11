package com.example.brandon.quickcontacts;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Map;

public class MainActivity extends AppCompatActivity {

    ContactsManager contactsManager;

    LinearLayout contactLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        contactsManager = new ContactsManager(MainActivity.this);
        contactsManager.setOnFinishedListener(new ContactsManager.OnFinishedListener() {
            @Override
            public void onFinishedWithResult(int type, Bundle params) {

                if(type == contactsManager.INSERTED){
                    String name = params.getString("name");
                    String phoneNumber = params.getString("phone_number");

                    int index = contactsManager.getIndexFromContactName(name);

                    // TODO :: Bug where the index is out of bounds, due to reformatting of the name on insert
                    contactLayout.addView(contactsManager.getView(name, phoneNumber, 20, 40), index);
                }

                else if(type == contactsManager.UPDATED){
                    String oldName = params.getString("old_name");
                    String newName = params.getString("new_name");
                    String newPhoneNumber = params.getString("new_phone_number");

                    for(int i = 0; i < contactLayout.getChildCount(); i++){
                        View view = contactLayout.getChildAt(i);
                        String name = ((TextView)view.findViewById(R.id.name)).getText().toString();
                        if(name.equalsIgnoreCase(oldName)){
                            contactLayout.removeView(view);
                            contactLayout.addView(contactsManager.getView(newName, newPhoneNumber, 20, 40), i);
                        }
                    }

                }
            }
        });

        // TODO :: add search method
        // TODO :: add alphabetical ordering to contacts display method
        // TODO :: add Alphabetical sectioning
        // TODO :: add Alphabetical side indexing

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view){
                NewContactDialog dialog = new NewContactDialog();

                dialog.setOnFinishedListener(new NewContactDialog.OnFinishedListener() {
                    @Override
                    public void onFinishedWithResult(String name, String phoneNumber) {
                        contactsManager.addContact(name, phoneNumber);
                    }
                });

                dialog.show(getSupportFragmentManager(), "new contact");
            }
        });

        displayContacts();
    }

    @Override
    protected void onStart() {
        super.onStart();

        contactLayout.removeAllViews();
        displayContacts();
    }

    public void displayContacts(){
        Map<String, String> contacts = contactsManager.getAddressBook();

        contactLayout = (LinearLayout)findViewById(R.id.contacts_container);

        for (Map.Entry<String, String> pair : contacts.entrySet()) {
            String name = pair.getKey();
            String phoneNumber = pair.getValue();

            contactLayout.addView(contactsManager.getView(name, phoneNumber, 20, 40));
        }
    }

    public void editContact(View view) {
        // To get the contact view that was clicked we must get the parent of the parent of the button.
        final View contactView   = (View)view.getParent().getParent();
        final String name        = ((TextView)contactView.findViewById(R.id.name)).getText().toString();
        final String phoneNumber = ((TextView)contactView.findViewById(R.id.phonenumber)).getText().toString();

        Bundle args = new Bundle();
        args.putString("name", name);
        args.putString("phone_number", phoneNumber);

        EditContactDialog dialog = new EditContactDialog();
        dialog.setArguments(args);

        dialog.setOnFinishedListener(new EditContactDialog.OnFinishedListener() {
            @Override
            public void onFinishedWithResult(int result, Bundle params) {
                if(result == EditContactDialog.DELETE){

                    ConfirmationDialog confirmationDialog = new ConfirmationDialog(MainActivity.this,
                            "Are You Sure?", "About to delete contact " + name, "confirm", "Cancel");

                    confirmationDialog.setOnFinished(new ConfirmationDialog.OnFinished() {
                        @Override
                        public void onFinished(boolean result) {
                            if(result){
                                contactLayout.removeView(contactView);
                                contactsManager.removeContact(name);
                            }
                        }
                    });

                    confirmationDialog.show();
                }

                if(result == EditContactDialog.UPDATE){
                    String oldName        = params.getString("old_name");
                    String newName        = params.getString("new_name");
                    String newPhoneNumber = params.getString("new_phone_number");

                    contactsManager.updateContact(oldName, newName, newPhoneNumber);
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
                    Intent intent = new Intent(Intent.ACTION_CALL);
                    intent.setData(Uri.parse("tel:" + phoneNumber));
                    if (intent.resolveActivity(getPackageManager()) != null) {
                        startActivity(intent);
                    }

                    Toast.makeText(getApplicationContext(), "Calling " + name, Toast.LENGTH_SHORT).show();
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
                            "Launching messenger for " + name, Toast.LENGTH_SHORT).show();

                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.fromParts("sms", phoneNumber, null)));
                }
            }
        });

        dialog.show();

    }
}
