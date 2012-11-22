/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.cts.writeexternalstorageapp;

import android.os.Environment;
import android.test.AndroidTestCase;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;

/**
 * Test if {@link Environment#getExternalStorageDirectory()} is writable.
 */
public class WriteExternalStorageTest extends AndroidTestCase {

    private static final File TEST_FILE = new File(
            Environment.getExternalStorageDirectory(), "meow");

    /**
     * Set of file paths that should all refer to the same location to verify
     * support for legacy paths.
     */
    private static final File[] IDENTICAL_FILES = {
            new File("/sdcard/caek"),
            new File(System.getenv("EXTERNAL_STORAGE"), "caek"),
            new File(Environment.getExternalStorageDirectory(), "caek"),
    };

    @Override
    protected void tearDown() throws Exception {
        try {
            TEST_FILE.delete();
            for (File file : IDENTICAL_FILES) {
                file.delete();
            }
        } finally {
            super.tearDown();
        }
    }

    public void testReadExternalStorage() throws Exception {
        assertExternalStorageMounted();
        Environment.getExternalStorageDirectory().list();
    }

    public void testWriteExternalStorage() throws Exception {
        assertExternalStorageMounted();

        // Write a value and make sure we can read it back
        writeInt(TEST_FILE, 32);
        assertEquals(readInt(TEST_FILE), 32);
    }

    /**
     * Verify that legacy filesystem paths continue working, and that they all
     * point to same location.
     */
    public void testLegacyPaths() throws Exception {
        final Random r = new Random();
        for (File target : IDENTICAL_FILES) {
            // Ensure we're starting with clean slate
            for (File file : IDENTICAL_FILES) {
                file.delete();
            }

            // Write value to our current target
            final int value = r.nextInt();
            writeInt(target, value);

            // Ensure that identical files all contain the value
            for (File file : IDENTICAL_FILES) {
                assertEquals(readInt(file), value);
            }
        }
    }

    private static void assertExternalStorageMounted() {
        assertEquals(Environment.MEDIA_MOUNTED, Environment.getExternalStorageState());
    }

    private static void writeInt(File file, int value) throws IOException {
        final DataOutputStream os = new DataOutputStream(new FileOutputStream(file));
        try {
            os.writeInt(value);
        } finally {
            os.close();
        }
    }

    private static int readInt(File file) throws IOException {
        final DataInputStream is = new DataInputStream(new FileInputStream(file));
        try {
            return is.readInt();
        } finally {
            is.close();
        }
    }
}
