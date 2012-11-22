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

package com.android.exchange;

import com.android.emailcommon.provider.EmailContent.Attachment;

import android.test.AndroidTestCase;

/**
 * You can run this entire test case with:
 *   runtest -c com.android.exchange.RequestTests exchange
 */
public class RequestTests extends AndroidTestCase {

    public void testPartRequestEquals() {
        Attachment att1 = new Attachment();
        att1.mId = 1;
        Attachment att2 = new Attachment();
        att2.mId = 2;
        // For part requests, the attachment id's must be ==
        PartRequest req1 = new PartRequest(att1, "dest1", "content1");
        PartRequest req2 = new PartRequest(att2, "dest2", "content2");
        assertFalse(req1.equals(req2));
        Attachment att3 = new Attachment();
        att3.mId = 1;
        PartRequest req3 = new PartRequest(att3, "dest3", "content3");
        assertTrue(req1.equals(req3));
        MessageMoveRequest req4 = new MessageMoveRequest(10L, 12L);
        assertFalse(req1.equals(req4));
    }

    public void testRequestEquals() {
        // Only the messageId needs to be ==
        MessageMoveRequest req1 = new MessageMoveRequest(1L, 10L);
        MessageMoveRequest req2 = new MessageMoveRequest(1L, 11L);
        assertTrue(req1.equals(req2));
        MessageMoveRequest req3 = new MessageMoveRequest(2L, 11L);
        assertFalse(req3.equals(req2));
        MeetingResponseRequest req4 = new MeetingResponseRequest(1L, 3);
        assertFalse(req4.equals(req1));
        MeetingResponseRequest req5 = new MeetingResponseRequest(1L, 4);
        assertTrue(req5.equals(req4));
    }
}