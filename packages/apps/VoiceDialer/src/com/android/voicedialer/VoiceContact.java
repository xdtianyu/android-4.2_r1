/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.voicedialer;

import android.app.Activity;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.CallLog;
import android.util.Log;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * This class represents a person who may be called via the VoiceDialer app.
 * The person has a name and a list of phones (home, mobile, work, other).
 */
public class VoiceContact {
    private static final String TAG = "VoiceContact";

    /**
     * Corresponding row doesn't exist.
     */
    public static final long ID_UNDEFINED = -1;

    public final String mName;
    public final long mContactId;
    public final long mPrimaryId;
    public final long mHomeId;
    public final long mMobileId;
    public final long mWorkId;
    public final long mOtherId;
    /**
     * Id for a phone number which doesn't belong to any other ids stored above.
     */
    public final long mFallbackId;

    /**
     * Constructor.
     *
     * @param name person's name.
     * @param contactId ID in person table.
     * @param primaryId primary ID in phone table.
     * @param homeId home ID in phone table.
     * @param mobileId mobile ID in phone table.
     * @param workId work ID in phone table.
     * @param otherId other ID in phone table.
     */
    private VoiceContact(String name, long contactId, long primaryId,
            long homeId, long mobileId, long workId, long otherId, long fallbackId) {
        mName = name;
        mContactId = contactId;
        mPrimaryId = primaryId;
        mHomeId = homeId;
        mMobileId = mobileId;
        mWorkId = workId;
        mOtherId = otherId;
        mFallbackId = fallbackId;
    }

    @Override
    public int hashCode() {
        final int LARGE_PRIME = 1610612741;
        int hash = 0;
        hash = LARGE_PRIME * (hash + (int)mContactId);
        hash = LARGE_PRIME * (hash + (int)mPrimaryId);
        hash = LARGE_PRIME * (hash + (int)mHomeId);
        hash = LARGE_PRIME * (hash + (int)mMobileId);
        hash = LARGE_PRIME * (hash + (int)mWorkId);
        hash = LARGE_PRIME * (hash + (int)mOtherId);
        hash = LARGE_PRIME * (hash + (int)mFallbackId);
        return mName.hashCode() + hash;
    }

    @Override
    public String toString() {
        return "mName=" + mName
                + " mPersonId=" + mContactId
                + " mPrimaryId=" + mPrimaryId
                + " mHomeId=" + mHomeId
                + " mMobileId=" + mMobileId
                + " mWorkId=" + mWorkId
                + " mOtherId=" + mOtherId
                + " mFallbackId=" + mFallbackId;
    }

    /**
     * @param activity The VoiceDialerActivity instance.
     * @return List of {@link VoiceContact} from
     * the contact list content provider.
     */
    public static List<VoiceContact> getVoiceContacts(Activity activity) {
        if (false) Log.d(TAG, "VoiceContact.getVoiceContacts");

        List<VoiceContact> contacts = new ArrayList<VoiceContact>();

        String[] phonesProjection = new String[] {
            Phone._ID,
            Phone.TYPE,
            Phone.IS_PRIMARY,
            // TODO: handle type != 0,1,2, and use LABEL
            Phone.LABEL,
            Phone.DISPLAY_NAME,
            Phone.CONTACT_ID,
        };

        // Table is sorted by number of times contacted and name. If we cannot fit all contacts
        // in the recognizer, we will at least have the commonly used ones.
        Cursor cursor = activity.getContentResolver().query(
                Phone.CONTENT_URI, phonesProjection,
                Phone.NUMBER + " NOT NULL", null,
                Phone.LAST_TIME_CONTACTED + " DESC, "
                        + Phone.DISPLAY_NAME + " ASC, "
                        + Phone._ID + " DESC");

        final int phoneIdColumn = cursor.getColumnIndexOrThrow(Phone._ID);
        final int typeColumn = cursor.getColumnIndexOrThrow(Phone.TYPE);
        final int isPrimaryColumn = cursor.getColumnIndexOrThrow(Phone.IS_PRIMARY);
        final int labelColumn = cursor.getColumnIndexOrThrow(Phone.LABEL);
        final int nameColumn = cursor.getColumnIndexOrThrow(Phone.DISPLAY_NAME);
        final int personIdColumn = cursor.getColumnIndexOrThrow(Phone.CONTACT_ID);

        // pieces of next VoiceContact
        String name = null;
        long personId = ID_UNDEFINED;
        long primaryId = ID_UNDEFINED;
        long homeId = ID_UNDEFINED;
        long mobileId = ID_UNDEFINED;
        long workId = ID_UNDEFINED;
        long otherId = ID_UNDEFINED;
        long fallbackId = ID_UNDEFINED;

        // loop over phone table
        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            long phoneIdAtCursor = cursor.getLong(phoneIdColumn);
            int typeAtCursor = cursor.getInt(typeColumn);
            long isPrimaryAtCursor = cursor.getLong(isPrimaryColumn);
            String labelAtCursor = cursor.getString(labelColumn);
            String nameAtCursor = cursor.getString(nameColumn);
            long personIdAtCursor = cursor.getLong(personIdColumn);

            /*
            if (false) {
                Log.d(TAG, "phoneId=" + phoneIdAtCursor
                        + " type=" + typeAtCursor
                        + " isPrimary=" + isPrimaryAtCursor
                        + " label=" + labelAtCursor
                        + " name=" + nameAtCursor
                        + " personId=" + personIdAtCursor
                        );
            }
            */

            // encountered a new name, so generate current VoiceContact
            if (name != null && !name.equals(nameAtCursor)) {
                contacts.add(new VoiceContact(name, personId, primaryId,
                        homeId, mobileId, workId, otherId, fallbackId));
                name = null;
            }

            // start accumulating pieces for a new VoiceContact
            if (name == null) {
                name = nameAtCursor;
                personId = personIdAtCursor;
                primaryId = ID_UNDEFINED;
                homeId = ID_UNDEFINED;
                mobileId = ID_UNDEFINED;
                workId = ID_UNDEFINED;
                otherId = ID_UNDEFINED;
                fallbackId = ID_UNDEFINED;
            }

            // if labeled, then patch to HOME/MOBILE/WORK/OTHER
            if (typeAtCursor == Phone.TYPE_CUSTOM &&
                    labelAtCursor != null) {
                String label = labelAtCursor.toLowerCase();
                if (label.contains("home") || label.contains("house")) {
                    typeAtCursor = Phone.TYPE_HOME;
                }
                else if (label.contains("mobile") || label.contains("cell")) {
                    typeAtCursor = Phone.TYPE_MOBILE;
                }
                else if (label.contains("work") || label.contains("office")) {
                    typeAtCursor = Phone.TYPE_WORK;
                }
                else if (label.contains("other")) {
                    typeAtCursor = Phone.TYPE_OTHER;
                }
            }

            boolean idAtCursorWasUsed = false;
            // save the home, mobile, or work phone id
            switch (typeAtCursor) {
                case Phone.TYPE_HOME:
                    homeId = phoneIdAtCursor;
                    if (isPrimaryAtCursor != 0) {
                        primaryId = phoneIdAtCursor;
                    }
                    idAtCursorWasUsed = true;
                    break;
                case Phone.TYPE_MOBILE:
                    mobileId = phoneIdAtCursor;
                    if (isPrimaryAtCursor != 0) {
                        primaryId = phoneIdAtCursor;
                    }
                    idAtCursorWasUsed = true;
                    break;
                case Phone.TYPE_WORK:
                    workId = phoneIdAtCursor;
                    if (isPrimaryAtCursor != 0) {
                        primaryId = phoneIdAtCursor;
                    }
                    idAtCursorWasUsed = true;
                    break;
                case Phone.TYPE_OTHER:
                    otherId = phoneIdAtCursor;
                    if (isPrimaryAtCursor != 0) {
                        primaryId = phoneIdAtCursor;
                    }
                    idAtCursorWasUsed = true;
                    break;
            }

            if (fallbackId == ID_UNDEFINED && !idAtCursorWasUsed) {
                fallbackId = phoneIdAtCursor;
            }
        }

        // generate the last VoiceContact
        if (name != null) {
            contacts.add(new VoiceContact(name, personId, primaryId,
                            homeId, mobileId, workId, otherId, fallbackId));
        }

        // clean up cursor
        cursor.close();

        if (false) Log.d(TAG, "VoiceContact.getVoiceContacts " + contacts.size());

        return contacts;
    }

    /**
     * @param contactsFile File containing a list of names,
     * one per line.
     * @return a List of {@link VoiceContact} in a File.
     */
    public static List<VoiceContact> getVoiceContactsFromFile(File contactsFile) {
        if (false) Log.d(TAG, "getVoiceContactsFromFile " + contactsFile);

        List<VoiceContact> contacts = new ArrayList<VoiceContact>();

        // read from a file
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(contactsFile), 8192);
            String name;
            for (int id = 1; (name = br.readLine()) != null; id++) {
                contacts.add(new VoiceContact(name, id, ID_UNDEFINED,
                        ID_UNDEFINED, ID_UNDEFINED, ID_UNDEFINED, ID_UNDEFINED, ID_UNDEFINED));
            }
        }
        catch (IOException e) {
            if (false) Log.d(TAG, "getVoiceContactsFromFile failed " + e);
        }
        finally {
            try {
                br.close();
            } catch (IOException e) {
                if (false) Log.d(TAG, "getVoiceContactsFromFile failed during close " + e);
            }
        }

        if (false) Log.d(TAG, "getVoiceContactsFromFile " + contacts.size());

        return contacts;
    }

    /**
     * @param activity The VoiceDialerActivity instance.
     * @return String of last number dialed.
     */
    public static String redialNumber(Activity activity) {
        Cursor cursor = activity.getContentResolver().query(
                CallLog.Calls.CONTENT_URI,
                new String[] { CallLog.Calls.NUMBER },
                CallLog.Calls.TYPE + "=" + CallLog.Calls.OUTGOING_TYPE,
                null,
                CallLog.Calls.DEFAULT_SORT_ORDER + " LIMIT 1");
        String number = null;
        if (cursor.getCount() >= 1) {
            cursor.moveToNext();
            int column = cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER);
            number = cursor.getString(column);
        }
        cursor.close();

        if (false) Log.d(TAG, "redialNumber " + number);

        return number;
    }

}
