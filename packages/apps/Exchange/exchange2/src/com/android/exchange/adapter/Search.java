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

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.os.RemoteException;
import android.util.Log;

import com.android.emailcommon.Logging;
import com.android.emailcommon.provider.Account;
import com.android.emailcommon.provider.EmailContent;
import com.android.emailcommon.provider.EmailContent.Message;
import com.android.emailcommon.provider.Mailbox;
import com.android.emailcommon.service.EmailServiceStatus;
import com.android.emailcommon.service.SearchParams;
import com.android.emailcommon.utility.TextUtilities;
import com.android.exchange.Eas;
import com.android.exchange.EasResponse;
import com.android.exchange.EasSyncService;
import com.android.exchange.ExchangeService;
import com.android.exchange.adapter.EmailSyncAdapter.EasEmailSyncParser;
import com.android.mail.providers.UIProvider;

import org.apache.http.HttpStatus;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

/**
 * Implementation of server-side search for EAS using the EmailService API
 */
public class Search {
    // The shortest search query we'll accept
    // TODO Check with UX whether this is correct
    private static final int MIN_QUERY_LENGTH = 3;
    // The largest number of results we'll ask for per server request
    private static final int MAX_SEARCH_RESULTS = 100;

    public static int searchMessages(Context context, long accountId, SearchParams searchParams,
            long destMailboxId) {
        // Sanity check for arguments
        int offset = searchParams.mOffset;
        int limit = searchParams.mLimit;
        String filter = searchParams.mFilter;
        if (limit < 0 || limit > MAX_SEARCH_RESULTS || offset < 0) return 0;
        // TODO Should this be checked in UI?  Are there guidelines for minimums?
        if (filter == null || filter.length() < MIN_QUERY_LENGTH) return 0;

        int res = 0;
        Account account = Account.restoreAccountWithId(context, accountId);
        if (account == null) return res;
        EasSyncService svc = EasSyncService.setupServiceForAccount(context, account);
        if (svc == null) return res;
        Mailbox searchMailbox = Mailbox.restoreMailboxWithId(context, destMailboxId);
        // Sanity check; account might have been deleted?
        if (searchMailbox == null) return res;
        ContentValues statusValues = new ContentValues();
        try {
            // Set the status of this mailbox to indicate query
            statusValues.put(Mailbox.UI_SYNC_STATUS, UIProvider.SyncStatus.LIVE_QUERY);
            searchMailbox.update(context, statusValues);

            svc.mMailbox = searchMailbox;
            svc.mAccount = account;
            Serializer s = new Serializer();
            s.start(Tags.SEARCH_SEARCH).start(Tags.SEARCH_STORE);
            s.data(Tags.SEARCH_NAME, "Mailbox");
            s.start(Tags.SEARCH_QUERY).start(Tags.SEARCH_AND);
            s.data(Tags.SYNC_CLASS, "Email");

            // If this isn't an inbox search, then include the collection id
            Mailbox inbox = Mailbox.restoreMailboxOfType(context, accountId, Mailbox.TYPE_INBOX);
            if (inbox == null) return 0;
            if (searchParams.mMailboxId != inbox.mId) {
                s.data(Tags.SYNC_COLLECTION_ID, inbox.mServerId);
            }

            s.data(Tags.SEARCH_FREE_TEXT, filter);
            s.end().end();              // SEARCH_AND, SEARCH_QUERY
            s.start(Tags.SEARCH_OPTIONS);
            if (offset == 0) {
                s.tag(Tags.SEARCH_REBUILD_RESULTS);
            }
            if (searchParams.mIncludeChildren) {
                s.tag(Tags.SEARCH_DEEP_TRAVERSAL);
            }
            // Range is sent in the form first-last (e.g. 0-9)
            s.data(Tags.SEARCH_RANGE, offset + "-" + (offset + limit - 1));
            s.start(Tags.BASE_BODY_PREFERENCE);
            s.data(Tags.BASE_TYPE, Eas.BODY_PREFERENCE_HTML);
            s.data(Tags.BASE_TRUNCATION_SIZE, "20000");
            s.end();                    // BASE_BODY_PREFERENCE
            s.end().end().end().done(); // SEARCH_OPTIONS, SEARCH_STORE, SEARCH_SEARCH
            EasResponse resp = svc.sendHttpClientPost("Search", s.toByteArray());
            try {
                int code = resp.getStatus();
                if (code == HttpStatus.SC_OK) {
                    InputStream is = resp.getInputStream();
                    try {
                        SearchParser sp = new SearchParser(is, svc, filter);
                        sp.parse();
                        res = sp.getTotalResults();
                    } finally {
                        is.close();
                    }
                } else {
                    svc.userLog("Search returned " + code);
                }
            } finally {
                resp.close();
            }
        } catch (IOException e) {
            svc.userLog("Search exception " + e);
        } finally {
            try {
                // TODO: Handle error states
                // Set the status of this mailbox to indicate query over
                statusValues.put(Mailbox.UI_SYNC_STATUS, UIProvider.SyncStatus.NO_SYNC);
                searchMailbox.update(context, statusValues);
                ExchangeService.callback().syncMailboxStatus(destMailboxId,
                        EmailServiceStatus.SUCCESS, 100);
            } catch (RemoteException e) {
            }
        }
        // Return the total count
        return res;
    }

    /**
     * Parse the result of a Search command
     */
    static class SearchParser extends Parser {
        private final EasSyncService mService;
        private final String mQuery;
        private int mTotalResults;

        private SearchParser(InputStream in, EasSyncService service, String query)
                throws IOException {
            super(in);
            mService = service;
            mQuery = query;
        }

        protected int getTotalResults() {
            return mTotalResults;
        }

        @Override
        public boolean parse() throws IOException {
            boolean res = false;
            if (nextTag(START_DOCUMENT) != Tags.SEARCH_SEARCH) {
                throw new IOException();
            }
            while (nextTag(START_DOCUMENT) != END_DOCUMENT) {
                if (tag == Tags.SEARCH_STATUS) {
                    String status = getValue();
                    if (Eas.USER_LOG) {
                        Log.d(Logging.LOG_TAG, "Search status: " + status);
                    }
                } else if (tag == Tags.SEARCH_RESPONSE) {
                    parseResponse();
                } else {
                    skipTag();
                }
            }
            return res;
        }

        private boolean parseResponse() throws IOException {
            boolean res = false;
            while (nextTag(Tags.SEARCH_RESPONSE) != END) {
                if (tag == Tags.SEARCH_STORE) {
                    parseStore();
                } else {
                    skipTag();
                }
            }
            return res;
        }

        private boolean parseStore() throws IOException {
            EmailSyncAdapter adapter = new EmailSyncAdapter(mService);
            EasEmailSyncParser parser = adapter.new EasEmailSyncParser(this, adapter);
            ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
            boolean res = false;

            while (nextTag(Tags.SEARCH_STORE) != END) {
                if (tag == Tags.SEARCH_STATUS) {
                    String status = getValue();
                } else if (tag == Tags.SEARCH_TOTAL) {
                    mTotalResults = getValueInt();
                } else if (tag == Tags.SEARCH_RESULT) {
                    parseResult(parser, ops);
                } else {
                    skipTag();
                }
            }

            try {
                adapter.mContentResolver.applyBatch(EmailContent.AUTHORITY, ops);
                if (Eas.USER_LOG) {
                    mService.userLog("Saved " + ops.size() + " search results");
                }
            } catch (RemoteException e) {
                Log.d(Logging.LOG_TAG, "RemoteException while saving search results.");
            } catch (OperationApplicationException e) {
            }

            return res;
        }

        private boolean parseResult(EasEmailSyncParser parser,
                ArrayList<ContentProviderOperation> ops) throws IOException {
            // Get an email sync parser for our incoming message data
            boolean res = false;
            Message msg = new Message();
            while (nextTag(Tags.SEARCH_RESULT) != END) {
                if (tag == Tags.SYNC_CLASS) {
                    getValue();
                } else if (tag == Tags.SYNC_COLLECTION_ID) {
                    getValue();
                } else if (tag == Tags.SEARCH_LONG_ID) {
                    msg.mProtocolSearchInfo = getValue();
                } else if (tag == Tags.SEARCH_PROPERTIES) {
                    msg.mAccountKey = mService.mAccount.mId;
                    msg.mMailboxKey = mService.mMailbox.mId;
                    msg.mFlagLoaded = Message.FLAG_LOADED_COMPLETE;
                    parser.pushTag(tag);
                    parser.addData(msg, tag);
                    if (msg.mHtml != null) {
                        msg.mHtml = TextUtilities.highlightTermsInHtml(msg.mHtml, mQuery);
                    }
                    msg.addSaveOps(ops);
                } else {
                    skipTag();
                }
            }
            return res;
        }
    }
}
