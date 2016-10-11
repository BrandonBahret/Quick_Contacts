package com.example.brandon.quickcontacts;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Locale;

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
                    int index = params.getInt("index");

                    contactLayout.addView(contactsManager.getView(name, phoneNumber, 20, 40), index);
                }

                else if(type == contactsManager.UPDATED){
                    String oldName = params.getString("old_name");
                    String newName = params.getString("new_name");
                    String newPhoneNumber = params.getString("new_phone_number");
                    int index = params.getInt("index");

                    for(int i = 0; i < contactLayout.getChildCount(); i++){
                        View view = contactLayout.getChildAt(i);
                        String name = ((TextView)view.findViewById(R.id.name)).getText().toString();
                        if(name.equalsIgnoreCase(oldName)){
                            contactLayout.removeView(view);

                            contactLayout.addView(contactsManager.getView(newName, newPhoneNumber, 20, 40), index);
                        }
                    }

                }
            }
        });

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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_bar, menu);

        SearchView searchView = (SearchView)menu.findItem(R.id.search).getActionView();
        searchView.setOnQueryTextListener(
                new SearchView.OnQueryTextListener() {
                    @Override
                    public boolean onQueryTextSubmit(String query) {
                        searchContacts(query);
                        return false;
                    }

                    @Override
                    public boolean onQueryTextChange(String query) {
                        searchContacts(query);
                        return false;
                    }
                }
        );

        return true;
    }

    public void searchContacts(String query){
        query = query.toLowerCase();

        for(int i = 0; i < contactLayout.getChildCount(); i++){
            View view = contactLayout.getChildAt(i);
            String name = ((TextView)view.findViewById(R.id.name)).getText().toString();
            if(name.toLowerCase().contains(query)){
                view.setVisibility(View.VISIBLE);
            }
            else{
                view.setVisibility(View.GONE);
            }
        }

    }

    public void displayContacts(){
        ArrayList<ContactsManager.Contact> contacts = contactsManager.getAddressBook();

        contactLayout = (LinearLayout)findViewById(R.id.contacts_container);

        for(ContactsManager.Contact contact : contacts){
            View newView = contactsManager.getView( contact.name, contact.phoneNumber, 20, 40 );
            contactLayout.addView(newView);
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


                    String message = String.format(Locale.US, getString(R.string.about_to_delete_format), name);
                    ConfirmationDialog confirmationDialog = new ConfirmationDialog(MainActivity.this,
                            getString(R.string.are_you_sure), message, getString(R.string.confirm_delete), getString(android.R.string.cancel));

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

        String message = String.format(Locale.US, getString(R.string.about_to_call_format), name);

        ConfirmationDialog dialog = new ConfirmationDialog(this, getString(R.string.are_you_sure),
                message, getString(R.string.make_call), getString(android.R.string.cancel));

        dialog.setOnFinished(new ConfirmationDialog.OnFinished() {
            @Override
            public void onFinished(boolean result) {
                if(result){
                    Intent intent = new Intent(Intent.ACTION_CALL);
                    intent.setData(Uri.parse("tel:" + phoneNumber));
                    if (intent.resolveActivity(getPackageManager()) != null) {
                        startActivity(intent);
                    }
                }
            }
        });

        dialog.show();
    }

    public void messageContact(View view) {
        View v = (View)view.getParent().getParent();
        final String name = ((TextView)v.findViewById(R.id.name)).getText().toString();
        final String phoneNumber = ((TextView)v.findViewById(R.id.phonenumber)).getText().toString();

        String message = String.format(Locale.US, getString(R.string.about_to_message_format), name);
        ConfirmationDialog dialog = new ConfirmationDialog(this, getString(R.string.are_you_sure),
                message, getString(R.string.open_messenger), getString(android.R.string.cancel));

        dialog.setOnFinished(new ConfirmationDialog.OnFinished() {
            @Override
            public void onFinished(boolean result) {
                if(result){
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.fromParts("sms", phoneNumber, null)));
                }
            }
        });

        dialog.show();

    }
}
