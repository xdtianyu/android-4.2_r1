/*
 * Copyright (C) 2010 The Android Open Source Project
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

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.test.AndroidTestCase;

import com.android.emailcommon.provider.Account;
import com.android.emailcommon.provider.Mailbox;
import com.android.exchange.EasSyncService;
import com.android.exchange.adapter.AbstractSyncAdapter;
import com.android.exchange.adapter.EmailSyncAdapter;
import com.android.exchange.adapter.EmailSyncAdapter.EasEmailSyncParser;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class SyncAdapterTestCase<T extends AbstractSyncAdapter> extends AndroidTestCase {
    public Context mContext;
    public Context mProviderContext;
    public ContentResolver mResolver;
    public Mailbox mMailbox;
    public Account mAccount;
    public EmailSyncAdapter mSyncAdapter;
    public EasEmailSyncParser mSyncParser;

    public SyncAdapterTestCase() {
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        mContext = getContext();
        // This could be used with a MockContext if we switch over
        mProviderContext = mContext;
        mResolver = mContext.getContentResolver();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        // If we've created and saved an account, delete it
        if (mAccount != null && mAccount.mId > 0) {
            mResolver.delete(ContentUris.withAppendedId(Account.CONTENT_URI, mAccount.mId), null,
                    null);
        }
    }

    /**
     * Create and return a short, simple InputStream that has at least four bytes, which is all
     * that's required to initialize an EasParser (the parent class of EasEmailSyncParser)
     * @return the InputStream
     */
    public InputStream getTestInputStream() {
        return new ByteArrayInputStream(new byte[] {0, 0, 0, 0, 0});
    }

    EasSyncService getTestService() {
        mAccount = new Account();
        mAccount.mEmailAddress = "__test__@android.com";
        mAccount.mId = -1;
        Mailbox mailbox = new Mailbox();
        mailbox.mId = -1;
        return getTestService(mAccount, mailbox);
    }

    EasSyncService getTestService(Account account, Mailbox mailbox) {
        EasSyncService service = new EasSyncService();
        service.mContext = mContext;
        service.mMailbox = mailbox;
        service.mAccount = account;
        service.mContentResolver = mContext.getContentResolver();
        return service;
    }

    protected T getTestSyncAdapter(Class<T> klass) {
        EasSyncService service = getTestService();
        Constructor<T> c;
        try {
            c = klass.getDeclaredConstructor(new Class[] {EasSyncService.class});
            return c.newInstance(service);
        } catch (SecurityException e) {
        } catch (NoSuchMethodException e) {
        } catch (IllegalArgumentException e) {
        } catch (InstantiationException e) {
        } catch (IllegalAccessException e) {
        } catch (InvocationTargetException e) {
        }
        return null;
    }

}
