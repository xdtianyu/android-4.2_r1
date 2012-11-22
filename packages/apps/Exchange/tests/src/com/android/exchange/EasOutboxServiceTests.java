/*
 * Copyright (C) 2010 The Android Open Source Project.
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

import android.test.suitebuilder.annotation.MediumTest;

import com.android.emailcommon.provider.Mailbox;
import com.android.exchange.EasOutboxService.OriginalMessageInfo;
import com.android.exchange.utility.ExchangeTestCase;

/**
 * You can run this entire test case with:
 *   runtest -c com.android.exchange.EasOutboxServiceTests exchange
 */
@MediumTest
public class EasOutboxServiceTests extends ExchangeTestCase {

    public void testGenerateSmartSendCmd() {
        EasOutboxService svc = new EasOutboxService(mProviderContext, new Mailbox());
        // Test encoding of collection id; colon should be preserved
        OriginalMessageInfo info = new OriginalMessageInfo("1339085683659694034", "Mail:^f", null);
        String cmd = svc.generateSmartSendCmd(true, info);
        assertEquals("SmartReply&ItemId=1339085683659694034&CollectionId=Mail:%5Ef", cmd);
        // Test encoding of item id
        info = new OriginalMessageInfo("14:&3", "6", null);
        cmd = svc.generateSmartSendCmd(false, info);
        assertEquals("SmartForward&ItemId=14:%263&CollectionId=6", cmd);
        // Test use of long id
        info = new OriginalMessageInfo("1339085683659694034", "Mail:^f", "3232323AAA");
        cmd = svc.generateSmartSendCmd(false, info);
        assertEquals("SmartForward&LongId=3232323AAA", cmd);
    }
}
