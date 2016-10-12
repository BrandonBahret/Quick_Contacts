package com.example.brandon.quickcontacts;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
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

                    removeAlphabeticalSectioning();

                    contactLayout.addView(getView(name, phoneNumber,
                            (int)getResources().getDimension(R.dimen.contactsTopMargin),
                            (int)getResources().getDimension(R.dimen.contactsBottomMargin)),
                            index);

                    updateAlphabeticalSectioning();
                }

                else if(type == contactsManager.UPDATED) {
                    String oldName = params.getString("old_name");
                    String newName = params.getString("new_name");
                    String newPhoneNumber = params.getString("new_phone_number");

                    removeAlphabeticalSectioning();

                    for (int i = 0; i < contactLayout.getChildCount(); i++) {
                        View view = contactLayout.getChildAt(i);
                        String tag = (String) view.getTag();

                        if (tag != null) {
                            if (tag.equals("contact_view")) {
                                String name = ((TextView) view.findViewById(R.id.name)).getText().toString();
                                if (name.equalsIgnoreCase(oldName)) {
                                    int index = contactsManager.getIndexFromContactName(newName);
                                    contactLayout.removeView(view);
                                    contactLayout.addView(getView(newName, newPhoneNumber,
                                            (int) getResources().getDimension(R.dimen.contactsTopMargin),
                                            (int) getResources().getDimension(R.dimen.contactsBottomMargin))
                                            , index);
                                    break;
                                }
                            }
                        }
                    }

                    updateAlphabeticalSectioning();
                }

            }
        });

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

        removeAlphabeticalSectioning();

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

        updateAlphabeticalSectioning();
    }

    View getView(String name, String phoneNumber, int topMargin, int bottomMargin){
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(0, topMargin, 0, bottomMargin);

        View v = View.inflate(MainActivity.this, R.layout.contact_layout, null);

        ((TextView)v.findViewById(R.id.name)).setText(name);
        ((TextView)v.findViewById(R.id.phonenumber)).setText(phoneNumber);

        v.setLayoutParams(layoutParams);

        return v;
    }

    @Nullable
    String getNameFromView(View view){
        if(view != null){
            String tag = (String)view.getTag();
            if(tag.equals("contact_view")){
                return ((TextView)view.findViewById(R.id.name)).getText().toString();
            }
        }

        return null;
    }

    public void displayContacts(){
        ArrayList<ContactsManager.Contact> contacts = contactsManager.getAddressBook();

        contactLayout = (LinearLayout)findViewById(R.id.contacts_container);

        removeAlphabeticalSectioning();

        for(ContactsManager.Contact contact : contacts){

            View newView = getView( contact.name, contact.phoneNumber,
                    (int)getResources().getDimension(R.dimen.contactsTopMargin), (int)getResources().getDimension(R.dimen.contactsBottomMargin));

            contactLayout.addView(newView);
        }

        updateAlphabeticalSectioning();
    }

    public void removeAlphabeticalSectioning(){
        for (int i = 0; i < contactLayout.getChildCount(); i++) {
            View view = contactLayout.getChildAt(i);
            String tag = (String) view.getTag();

            if (tag.equals("sectioning")) {
                contactLayout.removeViewAt(i);
            }
        }
    }

    public void updateAlphabeticalSectioning(){

        int contactsLength = contactLayout.getChildCount();

        Character section = '.';

        int count = 0;
        for(int i = 0; i < contactsLength; i++){
            View contactView = contactLayout.getChildAt(i);

            if(contactView.getVisibility() == View.VISIBLE) {

                String name = getNameFromView(contactView);

                if (name != null) {
                    Character leadingChar = name.toLowerCase().charAt(0);
                    if (!Character.isLetter(leadingChar)) {
                        leadingChar = '#';
                    }
                    if (!leadingChar.equals(section)) {
                        section = leadingChar;
                        View view = View.inflate(MainActivity.this, R.layout.alphabetical_sectioning, null);
                        TextView sectionHeader = (TextView) view.findViewById(R.id.alphabetical_section);
                        sectionHeader.setText(section.toString());
                        contactLayout.addView(view, i);

                        contactsLength = contactLayout.getChildCount();
                    }
                    else{
                        count++;
                    }
                }
            }
        }

        View numberOfContacts = View.inflate(MainActivity.this, R.layout.number_of_contacts, null);
        TextView text = (TextView)numberOfContacts.findViewById(R.id.text_section);
        String numberOfContactsString = String.format(Locale.US, getString(R.string.number_of_contacts_format), count);
        text.setText(numberOfContactsString);

        contactLayout.addView(numberOfContacts);
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
                                contactsManager.removeContact(name);

                                removeAlphabeticalSectioning();
                                contactLayout.removeView(contactView);
                                updateAlphabeticalSectioning();
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
