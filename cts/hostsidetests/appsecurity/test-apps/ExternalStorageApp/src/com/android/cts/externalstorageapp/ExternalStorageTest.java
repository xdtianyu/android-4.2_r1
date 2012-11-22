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

package com.android.cts.externalstorageapp;

import android.os.Environment;
import android.test.AndroidTestCase;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Test if {@link Environment#getExternalStorageDirectory()} is readable.
 */
public class ExternalStorageTest extends AndroidTestCase {

    private static final String TEST_FILE = "meow";

    private void assertExternalStorageMounted() {
        assertEquals(Environment.MEDIA_MOUNTED, Environment.getExternalStorageState());
    }

    private void readExternalStorage() throws IOException {
        final File file = new File(Environment.getExternalStorageDirectory(), TEST_FILE);
        final InputStream is = new FileInputStream(file);
        try {
            is.read();
        } finally {
            is.close();
        }
    }

    public void testReadExternalStorage() throws Exception {
        assertExternalStorageMounted();
        try {
            readExternalStorage();
        } catch (IOException e) {
            fail("unable to read external file");
        }
    }

    public void testFailReadExternalStorage() throws Exception {
        assertExternalStorageMounted();
        try {
            readExternalStorage();
            fail("able read external file");
        } catch (IOException e) {
            // expected
            e.printStackTrace();
        }
    }
}
