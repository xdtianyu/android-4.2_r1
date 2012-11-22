/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.cts.appwithdata;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.test.AndroidTestCase;

import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Test that will create private app data.
 *
 * This is not really a test per-say. Its just used as a hook so the test controller can trigger
 * the creation of private app data.
 */
public class CreatePrivateDataTest extends AndroidTestCase {

    /**
     * Name of private file to create.
     */
    private static final String PRIVATE_FILE_NAME = "private_file.txt";

    private static final String PREFERENCES_FILE_NAME = "preferences";
    private static final String PREFERENCE_KEY = "preference_key";
    private static final String PREFERENCE_VALUE = "preference_value";

    static final String DB_TABLE_NAME = "test_table";
    static final String DB_COLUMN = "test_column";
    static final String DB_VALUE = "test_value";

    /**
     * Creates a file private to this app
     * @throws IOException if any error occurred when creating the file
     */
    public void testCreatePrivateData() throws IOException {
        FileOutputStream outputStream = getContext().openFileOutput(PRIVATE_FILE_NAME,
                Context.MODE_PRIVATE);
        outputStream.write("file contents".getBytes());
        outputStream.close();
        assertTrue(getContext().getFileStreamPath(PRIVATE_FILE_NAME).exists());

        writeToPreferences();
        writeToDatabase();
    }

    private void writeToPreferences() {
        SharedPreferences prefs = mContext.getSharedPreferences(PREFERENCES_FILE_NAME, 0);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PREFERENCE_KEY, PREFERENCE_VALUE);
        editor.commit();
        assertEquals(PREFERENCE_VALUE, prefs.getString(PREFERENCE_KEY, null));
    }

    private void writeToDatabase() {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = new TestDatabaseOpenHelper(mContext).getWritableDatabase();
            ContentValues values = new ContentValues(1);
            values.put(DB_COLUMN, DB_VALUE);
            assertTrue(db.insert(DB_TABLE_NAME, null, values) != -1);

            cursor = db.query(DB_TABLE_NAME, new String[] {DB_COLUMN},
                    null, null, null, null, null);
            assertEquals(1, cursor.getCount());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            if (db != null) {
                db.close();
            }
        }
    }

    /**
     * Check to ensure the private file created in testCreatePrivateData does not exist.
     * Used to check that uninstall of an app deletes the app's data.
     */
    public void testEnsurePrivateDataNotExist() throws IOException {
        assertFalse(getContext().getFileStreamPath(PRIVATE_FILE_NAME).exists());

        assertPreferencesDataDoesNotExist();
        assertDatabaseDataDoesNotExist();
    }

    private void assertPreferencesDataDoesNotExist() {
        SharedPreferences prefs = mContext.getSharedPreferences(PREFERENCES_FILE_NAME, 0);
        assertNull(prefs.getString(PREFERENCE_KEY, null));
    }

    private void assertDatabaseDataDoesNotExist() {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = new TestDatabaseOpenHelper(mContext).getWritableDatabase();
            cursor = db.query(DB_TABLE_NAME, new String[] {DB_COLUMN},
                    null, null, null, null, null);
            assertEquals(0, cursor.getCount());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            if (db != null) {
                db.close();
            }
        }
    }

    static class TestDatabaseOpenHelper extends SQLiteOpenHelper {

        static final String _ID = "_id";

        public TestDatabaseOpenHelper(Context context) {
            super(context, "test.db", null, 1337);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + DB_TABLE_NAME + " ("
                    + _ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + DB_COLUMN + " TEXT);");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + DB_TABLE_NAME);
            onCreate(db);
        }
    }
}
