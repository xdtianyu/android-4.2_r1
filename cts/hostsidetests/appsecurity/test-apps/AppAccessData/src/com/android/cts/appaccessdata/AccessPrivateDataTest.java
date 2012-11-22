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

package com.android.cts.appaccessdata;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import android.test.AndroidTestCase;

/**
 * Test that another app's private data cannot be accessed.
 *
 * Assumes that {@link APP_WITH_DATA_PKG} has already created the private data.
 */
public class AccessPrivateDataTest extends AndroidTestCase {

    /**
     * The Android package name of the application that owns the private data
     */
    private static final String APP_WITH_DATA_PKG = "com.android.cts.appwithdata";

    /**
     * Name of private file to access. This must match the name of the file created by
     * {@link APP_WITH_DATA_PKG}.
     */
    private static final String PRIVATE_FILE_NAME = "private_file.txt";

    /**
     * Tests that another app's private file cannot be accessed
     * @throws IOException
     */
    public void testAccessPrivateData() throws IOException {
        try {
            // construct the absolute file path to the app's private file
            String privateFilePath = String.format("/data/data/%s/%s", APP_WITH_DATA_PKG,
                    PRIVATE_FILE_NAME);
            FileInputStream inputStream = new FileInputStream(privateFilePath);
            inputStream.read();
            inputStream.close();
            fail("Was able to access another app's private data");
        } catch (FileNotFoundException e) {
            // expected
        } catch (SecurityException e) {
            // also valid
        }
    }
}
