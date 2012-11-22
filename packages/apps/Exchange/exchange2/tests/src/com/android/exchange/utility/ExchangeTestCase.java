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

package com.android.exchange.utility;

import com.android.emailcommon.provider.Account;
import com.android.exchange.provider.EmailContentSetupUtils;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.test.AndroidTestCase;

import java.util.ArrayList;

public abstract class ExchangeTestCase extends AndroidTestCase {
    private final ArrayList<Long> mCreatedAccountIds = new ArrayList<Long>();
    protected Context mProviderContext;
    
    @Override
    public void setUp() throws Exception {
        super.setUp();
        mContext = getContext();
        // Could use MockContext here if we switch over
        mProviderContext = mContext;
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        ContentResolver resolver = mProviderContext.getContentResolver();
        for (Long accountId: mCreatedAccountIds) {
            resolver.delete(ContentUris.withAppendedId(Account.CONTENT_URI, accountId), null,
                    null);
        }
    }

    /**
     * Add an account to our list of test accounts; we'll delete it automatically in tearDown()
     * @param account the account to be added to our list of test accounts
     */
    protected void addTestAccount(Account account) {
        if (account.mId > 0) {
            mCreatedAccountIds.add(account.mId);
        }
    }
    
    /**
     * Create a test account that will be automatically deleted when the test is finished
     * @param name the name of the account
     * @param saveIt whether or not to save the account in EmailProvider
     * @return the account created
     */
    protected Account setupTestAccount(String name, boolean saveIt) {
        Account account = EmailContentSetupUtils.setupAccount(name, saveIt, mProviderContext);
        addTestAccount(account);
        return account;
    }
}
