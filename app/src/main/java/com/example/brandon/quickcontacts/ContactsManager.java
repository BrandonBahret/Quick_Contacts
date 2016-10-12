package com.example.brandon.quickcontacts;

import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.support.annotation.Nullable;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

class ContactsManager {

    private ArrayList<OnFinishedListener> onFinishedListeners = new ArrayList<>();

    void setOnFinishedListener(OnFinishedListener listener){
        onFinishedListeners.add(listener);
    }

    interface OnFinishedListener{
        void onFinishedWithResult(int type, Bundle params);
    }

    private Context context;
    final int INSERTED = 0, UPDATED = 1;

    ContactsManager(Context context){
        this.context = context;
    }

    class Contact{
        String name, phoneNumber;
        int index;

        Contact(String name, String phoneNumber, int index){
            this.name = name;
            this.phoneNumber = phoneNumber;
            this.index = index;
        }
    }

    ArrayList<Contact> getAddressBook(){

        ArrayList<Contact> result = new ArrayList<>();

        Cursor cursor = context.getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null
        );

        if(cursor != null) {
            while (cursor.moveToNext()) {
                int nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                int phoneIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);

                String name = cursor.getString(nameIndex);
                String phone = cursor.getString(phoneIndex);

                Contact contact = new Contact(name, phone, -1);

                result.add(contact);
            }

            cursor.close();
        }

        Collections.sort(result, new Comparator<Contact>() {
            @Override
            public int compare(Contact contact1, Contact contact2) {
                return contact1.name.toLowerCase().
                        compareTo(contact2.name.toLowerCase());
            }
        });

        int index = 0;
        for(Contact contact:result) {
            contact.index = index++;
        }

        return result;
    }

    @Nullable
    private Contact readContactFromContactID(String contactId) {

        Cursor cursor = context.getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null,
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                new String[]{contactId}, null);

        if (cursor != null) {
            if (cursor.moveToNext()) {
                int idx = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
                String name = cursor.getString(idx);

                idx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                String phoneNumber = cursor.getString(idx);

                int index = getIndexFromContactName(name);

                cursor.close();
                return new Contact(name, phoneNumber, index);
            }
        }

        return null;
    }

    @Nullable
    private Contact readContactFromResults(ContentProviderResult[] results){
        Uri contactUri = results[1].uri;

        Cursor cursor = context.getContentResolver().query(contactUri, null, null, null, null);

        if(cursor != null) {
            if (cursor.moveToFirst()) {
                int idx = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
                String name = cursor.getString(idx);

                idx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                String phoneNumber = cursor.getString(idx);

                int index = getIndexFromContactName(name);

                return new Contact(name, phoneNumber, index);
            }
            cursor.close();
        }

        return null;
    }

    private String getContactIdFromName(String queryName){
        String contactId = "";

        Cursor cursor = context.getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null
        );

        if(cursor != null) {
            while (cursor.moveToNext()) {
                int nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                String name = cursor.getString(nameIndex);

                if(queryName.equalsIgnoreCase(name)){
                    int id = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID);
                    contactId = cursor.getString(id);
                    break;
                }
            }

            cursor.close();
        }

        return contactId;
    }

    int getIndexFromContactName(String name){

        ArrayList<Contact> contacts = getAddressBook();

        for(Contact contact: contacts){
            if(contact.name.equalsIgnoreCase(name)){
                return contact.index;
            }
        }

        return -1;
    }

    boolean removeContact(String name) {
        ContentResolver cr = context.getContentResolver();
        Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI,
                null, null, null, null);

        if(cur != null){
            while(cur.moveToNext()){
                try{
                    if (cur.getString(cur.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)).equalsIgnoreCase(name)) {
                        String lookupKey = cur.getString(cur.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY));
                        Uri uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI, lookupKey);
                        cr.delete(uri, null, null);
                    }
                }
                catch(Exception e) {
                    e.printStackTrace();
                }
            }
            cur.close();
        }

        return false;
    }

    void addContact(String name, String phoneNumber){
        // Code snippet from Stack Overflow:
        // http://stackoverflow.com/questions/4459138/insert-contact-in-android-with-contactscontract

        // If there isn't a contact with the given name
        if(getIndexFromContactName(name) == -1) {

            ArrayList<ContentProviderOperation> ops = new ArrayList<>();
            int rawContactInsertIndex = ops.size();

            ops.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null).build());

            //Phone Number
            ops.add(ContentProviderOperation
                    .newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID,
                            rawContactInsertIndex)
                    .withValue(ContactsContract.Data.MIMETYPE,
                            ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phoneNumber)
                    .withValue(ContactsContract.Data.MIMETYPE,
                            ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, "1").build());

            //Display name/Contact name
            ops.add(ContentProviderOperation
                    .newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                    .withValue(ContactsContract.Data.MIMETYPE,
                            ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name)
                    .build());
            try {
                ContentProviderResult[] results =
                        context.getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);

                // Now entering code not from stack overflow
                Contact newContact = readContactFromResults(results);

                if (newContact != null) {
                    Bundle params = new Bundle();
                    params.putString("name", newContact.name);
                    params.putString("phone_number", newContact.phoneNumber);
                    params.putInt("index", newContact.index);

                    for (OnFinishedListener listener : onFinishedListeners) {
                        listener.onFinishedWithResult(INSERTED, params);
                    }
                }

            }  catch (OperationApplicationException | RemoteException e) {
                e.printStackTrace();
            }
        }
        else{
            Toast.makeText(context, context.getResources().getString(R.string.add_contact_name_fail),
                    Toast.LENGTH_SHORT).show();
        }
    }

    void updateContact(String oldName, String newName, String newPhoneNumber){

        // If there isn't already a contact named "newName"
        if(getIndexFromContactName(newName) == -1 || (oldName.equals(newName))) {
            Bundle params = new Bundle();
            params.putString("old_name", oldName);
            params.putString("new_name", newName);
            params.putString("new_phone_number", newPhoneNumber);

            String contactId = getContactIdFromName(oldName);
            int id = Integer.parseInt(contactId);

            ArrayList<ContentProviderOperation> ops = new ArrayList<>();

            // Name
            ContentProviderOperation.Builder builder = ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI);
            builder.withSelection(ContactsContract.Data.CONTACT_ID + "=? AND " +
                    ContactsContract.Data.MIMETYPE + "=?", new String[]{String.valueOf(id),
                    ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE});

            // Split the full name into it's components, this is necessary to update the contact name.
            NameSplitter nameSplitter = new NameSplitter("Dr, Mr, Mrs, Ms, Miss",
                    "", "Jr, Sr, Phd, M.D., MD, D.D.S.", "");
            NameSplitter.Name name = new NameSplitter.Name();
            nameSplitter.split(name, newName);

            builder.withValue(ContactsContract.CommonDataKinds.StructuredName.PREFIX, name.getPrefix());
            builder.withValue(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, name.getGivenNames()); // First Name
            builder.withValue(ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME, name.getMiddleName());
            builder.withValue(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, name.getFamilyName()); // Last Name
            builder.withValue(ContactsContract.CommonDataKinds.StructuredName.SUFFIX, name.getSuffix());
            ops.add(builder.build());

            // Number
            builder = ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI);
            builder.withSelection(ContactsContract.Data.CONTACT_ID + "=? AND " +
                            ContactsContract.Data.MIMETYPE + "=? AND " +
                            ContactsContract.CommonDataKinds.Organization.TYPE + "=?",
                    new String[]{String.valueOf(id), ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE,
                            String.valueOf(ContactsContract.CommonDataKinds.Phone.TYPE_HOME)});

            builder.withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, newPhoneNumber);
            ops.add(builder.build());


            try {
                context.getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);

                Contact newContact = readContactFromContactID(contactId);
                if (newContact != null) {
                    newName = newContact.name;
                    params.putString("new_name", newName);
                    params.putString("new_phone_number", newContact.phoneNumber);
                }

                params.putInt("index", getIndexFromContactName(newName));
                for (OnFinishedListener listener : onFinishedListeners) {
                    listener.onFinishedWithResult(UPDATED, params);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        else{
            Toast.makeText(context, context.getResources().getString(R.string.edit_contact_name_fail),
                    Toast.LENGTH_SHORT).show();
        }

    }
}
