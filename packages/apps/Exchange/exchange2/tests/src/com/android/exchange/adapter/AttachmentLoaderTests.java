/* Copyright (C) 2011 The Android Open Source Project.
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

/**
 * You can run this entire test case with:
 *   runtest -c com.android.exchange.adapter.AttachmentLoaderTests exchange
 */
package com.android.exchange.adapter;

import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

@SmallTest
public class AttachmentLoaderTests extends AndroidTestCase {
    private static final String TEST_LOCATION =
        "Inbox/FW:%204G%20Netbook%20|%20Now%20Available%20for%20Order.EML/image012.jpg";

    public void testEncodeForExchange2003() {
        assertEquals("abc", AttachmentLoader.encodeForExchange2003("abc"));
        // We don't encode the four characters after abc
        assertEquals("abc_:/.", AttachmentLoader.encodeForExchange2003("abc_:/."));
        // We don't re-encode escaped characters
        assertEquals("%20%33", AttachmentLoader.encodeForExchange2003("%20%33"));
        // Test with the location that failed in use
        assertEquals(TEST_LOCATION.replace("|", "%7C"),
                AttachmentLoader.encodeForExchange2003(TEST_LOCATION));
    }
}
