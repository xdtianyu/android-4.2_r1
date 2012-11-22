/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.exchange.adapter;

import android.content.ContentValues;
import android.test.AndroidTestCase;
import android.test.MoreAsserts;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/** You can run this entire test case with:
 *   runtest -c com.android.exchange.adapter.SerializerTests exchange
 */
public class SerializerTests extends AndroidTestCase {

    private static final byte[] BYTE_ARRAY = new byte[] {1, 2, 3, 4, 5};
    private static final int BYTE_ARRAY_LENGTH = 5;
    private static final String ID = "ID";
    private static final String KEY = "Key";

    // Basic test for use of start, end, tag, data, opaque, and done
    public void testSerializer() throws IOException {
        ContentValues values = new ContentValues();
        // Create a test stream
        Serializer s = new Serializer();
        s.start(Tags.COMPOSE_SEND_MAIL);

        // Test writeStringValue without and with data
        s.writeStringValue(values, KEY, Tags.COMPOSE_ACCOUNT_ID);
        values.put(KEY, ID);
        s.writeStringValue(values, KEY, Tags.COMPOSE_ACCOUNT_ID);

        s.data(Tags.COMPOSE_CLIENT_ID, ID);
        s.tag(Tags.COMPOSE_SAVE_IN_SENT_ITEMS);
        s.start(Tags.COMPOSE_MIME);
        s.opaque(new ByteArrayInputStream(BYTE_ARRAY), BYTE_ARRAY_LENGTH);
        s.end();  // COMPOSE_MIME
        s.end();  // COMPOSE_SEND_MAIL
        s.done(); // DOCUMENT
        // Get the bytes for the stream
        byte[] bytes = s.toByteArray();
        // These are the expected bytes (self-explanatory)
        byte[] expectedBytes = new byte[] {
                3,      // Version 1.3
                1,      // unknown or missing public identifier
                106,    // UTF-8
                0,      // String array length
                Wbxml.SWITCH_PAGE,
                Tags.COMPOSE,
                Tags.COMPOSE_SEND_MAIL - Tags.COMPOSE_PAGE + Wbxml.WITH_CONTENT,
                Tags.COMPOSE_ACCOUNT_ID - Tags.COMPOSE_PAGE,
                Tags.COMPOSE_ACCOUNT_ID - Tags.COMPOSE_PAGE + Wbxml.WITH_CONTENT,
                Wbxml.STR_I,    // 0-terminated string
                (byte)ID.charAt(0),
                (byte)ID.charAt(1),
                0,
                Wbxml.END,  // COMPOSE_ACCOUNT_ID
                Tags.COMPOSE_CLIENT_ID - Tags.COMPOSE_PAGE + Wbxml.WITH_CONTENT,
                Wbxml.STR_I,    // 0-terminated string
                (byte)ID.charAt(0),
                (byte)ID.charAt(1),
                0,
                Wbxml.END,  // COMPOSE_CLIENT_ID
                Tags.COMPOSE_SAVE_IN_SENT_ITEMS - Tags.COMPOSE_PAGE,
                Tags.COMPOSE_MIME - Tags.COMPOSE_PAGE + Wbxml.WITH_CONTENT,
                (byte)Wbxml.OPAQUE,
                BYTE_ARRAY_LENGTH,
                BYTE_ARRAY[0],
                BYTE_ARRAY[1],
                BYTE_ARRAY[2],
                BYTE_ARRAY[3],
                BYTE_ARRAY[4],
                Wbxml.END,  // COMPOSE_MIME
                Wbxml.END   // COMPOSE_SEND_MAIL
         };
        // Make sure we get what's expected
        MoreAsserts.assertEquals("Serializer mismatch", bytes, expectedBytes);
    }
}
