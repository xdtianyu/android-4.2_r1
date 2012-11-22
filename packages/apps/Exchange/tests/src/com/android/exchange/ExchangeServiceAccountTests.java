/*
 * Copyright (C) 2009 Marc Blank
 * Licensed to The Android Open Source Project.
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

import com.android.emailcommon.provider.Account;
import com.android.emailcommon.provider.Mailbox;
import com.android.exchange.ExchangeService.SyncError;
import com.android.exchange.provider.EmailContentSetupUtils;
import com.android.exchange.utility.ExchangeTestCase;

import java.util.concurrent.ConcurrentHashMap;

/**
 * You can run this entire test case with:
 *   runtest -c com.android.exchange.ExchangeServiceAccountTests exchange
 */
@MediumTest
public class ExchangeServiceAccountTests extends ExchangeTestCase {

    public ExchangeServiceAccountTests() {
        super();
    }

    public void testReleaseSyncHolds() {
        ExchangeService exchangeService = new ExchangeService();
        SyncError securityErrorAccount1 =
            exchangeService.new SyncError(AbstractSyncService.EXIT_SECURITY_FAILURE, false);
        SyncError ioError =
            exchangeService.new SyncError(AbstractSyncService.EXIT_IO_ERROR, false);
        SyncError securityErrorAccount2 =
            exchangeService.new SyncError(AbstractSyncService.EXIT_SECURITY_FAILURE, false);
        // Create account and two mailboxes
        Account acct1 = setupTestAccount("acct1", true);
        Mailbox box1 = EmailContentSetupUtils.setupMailbox("box1", acct1.mId, true,
                mProviderContext);
        Mailbox box2 = EmailContentSetupUtils.setupMailbox("box2", acct1.mId, true,
                mProviderContext);
        Account acct2 = setupTestAccount("acct2", true);
        Mailbox box3 = EmailContentSetupUtils.setupMailbox("box3", acct2.mId, true,
                mProviderContext);
        Mailbox box4 = EmailContentSetupUtils.setupMailbox("box4", acct2.mId, true,
                mProviderContext);

        ConcurrentHashMap<Long, SyncError> errorMap = exchangeService.mSyncErrorMap;
        // Add errors into the map
        errorMap.put(box1.mId, securityErrorAccount1);
        errorMap.put(box2.mId, ioError);
        errorMap.put(box3.mId, securityErrorAccount2);
        errorMap.put(box4.mId, securityErrorAccount2);
        // We should have 4
        assertEquals(4, errorMap.keySet().size());
        // Release the holds on acct2 (there are two of them)
        assertTrue(exchangeService.releaseSyncHolds(mProviderContext,
                AbstractSyncService.EXIT_SECURITY_FAILURE, acct2));
        // There should be two left
        assertEquals(2, errorMap.keySet().size());
        // And these are the two...
        assertNotNull(errorMap.get(box2.mId));
        assertNotNull(errorMap.get(box1.mId));

        // Put the two back
        errorMap.put(box3.mId, securityErrorAccount2);
        errorMap.put(box4.mId, securityErrorAccount2);
        // We should have 4 again
        assertEquals(4, errorMap.keySet().size());
        // Release all of the security holds
        assertTrue(exchangeService.releaseSyncHolds(mProviderContext,
                AbstractSyncService.EXIT_SECURITY_FAILURE, null));
        // There should be one left
        assertEquals(1, errorMap.keySet().size());
        // And this is the one
        assertNotNull(errorMap.get(box2.mId));

        // Release the i/o holds on account 2 (there aren't any)
        assertFalse(exchangeService.releaseSyncHolds(mProviderContext,
                AbstractSyncService.EXIT_IO_ERROR, acct2));
        // There should still be one left
        assertEquals(1, errorMap.keySet().size());

        // Release the i/o holds on account 1 (there's one)
        assertTrue(exchangeService.releaseSyncHolds(mProviderContext,
                AbstractSyncService.EXIT_IO_ERROR, acct1));
        // There should still be one left
        assertEquals(0, errorMap.keySet().size());
    }

    public void testIsSyncable() {
        Account acct1 = setupTestAccount("acct1", true);
        Mailbox box1 = EmailContentSetupUtils.setupMailbox("box1", acct1.mId, true,
                mProviderContext, Mailbox.TYPE_DRAFTS);
        Mailbox box2 = EmailContentSetupUtils.setupMailbox("box2", acct1.mId, true,
                mProviderContext, Mailbox.TYPE_OUTBOX);
        Mailbox box3 = EmailContentSetupUtils.setupMailbox("box2", acct1.mId, true,
                mProviderContext, Mailbox.TYPE_ATTACHMENT);
        Mailbox box4 = EmailContentSetupUtils.setupMailbox("box2", acct1.mId, true,
                mProviderContext, Mailbox.TYPE_NOT_SYNCABLE + 64);
        Mailbox box5 = EmailContentSetupUtils.setupMailbox("box2", acct1.mId, true,
                mProviderContext, Mailbox.TYPE_MAIL);
        assertFalse(ExchangeService.isSyncable(box1));
        assertFalse(ExchangeService.isSyncable(box2));
        assertFalse(ExchangeService.isSyncable(box3));
        assertFalse(ExchangeService.isSyncable(box4));
        assertTrue(ExchangeService.isSyncable(box5));
    }
}
