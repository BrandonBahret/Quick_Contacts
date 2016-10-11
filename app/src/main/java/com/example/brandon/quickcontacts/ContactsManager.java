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
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ContactsManager {

    private ArrayList<OnFinishedListener> onFinishedListeners = new ArrayList<OnFinishedListener>();

    public void setOnFinishedListener(OnFinishedListener listener){
        onFinishedListeners.add(listener);
    }

    public interface OnFinishedListener{
        void onFinishedWithResult(int type, Bundle params);
    }

    private Context context;
    public final int INSERTED = 0, UPDATED = 1;

    ContactsManager(Context context){
        this.context = context;
    }

    public Map<String, String> getAddressBook(){
        Map<String, String> result = new HashMap<String, String>();

        Cursor cursor = context.getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null
        );

        if(cursor != null) {
            while (cursor.moveToNext()) {
                int phoneIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                int nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);

                String phone = cursor.getString(phoneIndex);
                String name = cursor.getString(nameIndex);
                result.put(name, phone);
            }

            cursor.close();
        }

        return result;
    }

    String getContactIdFromName(String queryName){
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

        Map<String, String> contacts = getAddressBook();

        int index = 0;
        for (Map.Entry<String, String> pair : contacts.entrySet()) {
            String currentName = pair.getKey();
            if(currentName.equals(name)){
                break;
            }
            index++;
        }

        return index;
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
                    System.out.println(e.getStackTrace());
                }
            }
            cur.close();
        }

        return false;
    }

    void addContact(String name, String phoneNumber){
        // Code snippet from Stack Overflow:
        // http://stackoverflow.com/questions/4459138/insert-contact-in-android-with-contactscontract

        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
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
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (OperationApplicationException e) {
            e.printStackTrace();
        }

        // Now entering code not from stack overflow

        Bundle params = new Bundle();
        params.putString("name", name);
        params.putString("phone_number", phoneNumber);

        for(OnFinishedListener listener : onFinishedListeners){
            listener.onFinishedWithResult(INSERTED, params);
        }
    }

    void updateContact(String oldName, String newName, String newPhoneNumber){
        // TODO :: Change this method to put a name into the Bundle that matches the one stored in contacts
        // When the names don't match other functions malfunction causing app crashing bugs

        Bundle params = new Bundle();
        params.putString("old_name", oldName);
        params.putString("new_name", newName);
        params.putString("new_phone_number", newPhoneNumber);

        String contactId = getContactIdFromName(oldName);
        int id = Integer.parseInt(contactId);

        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();

        // Name
        ContentProviderOperation.Builder builder = ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI);
        builder.withSelection(ContactsContract.Data.CONTACT_ID + "=? AND " +
                ContactsContract.Data.MIMETYPE + "=?", new String[]{String.valueOf(id),
                ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE});

//         Split the full name into it's components, this is necessary to update the contact name.
        NameSplitter nameSplitter = new NameSplitter("Dr, Mr, Mrs, Ms, Miss",
                "", "Jr, Sr, Phd, M.D., MD, D.D.S.", "", Locale.US);
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
            for(OnFinishedListener listener : onFinishedListeners){
                listener.onFinishedWithResult(UPDATED, params);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    View getView(String name, String phoneNumber, int topMargin, int bottomMargin){
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(0, topMargin, 0, bottomMargin);

        View v = View.inflate(context, R.layout.contact_layout, null);

        ((TextView)v.findViewById(R.id.name)).setText(name);
        ((TextView)v.findViewById(R.id.phonenumber)).setText(phoneNumber);

        v.setLayoutParams(layoutParams);

        return v;
    }
}
