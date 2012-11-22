/*
 * Copyright (C) 2008-2009 Marc Blank
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

package com.android.exchange.adapter;

import com.android.emailcommon.provider.Account;
import com.android.emailcommon.provider.Mailbox;
import com.android.exchange.CommandStatusException;
import com.android.exchange.Eas;
import com.android.exchange.EasSyncService;
import com.google.common.annotations.VisibleForTesting;

import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.OperationApplicationException;
import android.net.Uri;
import android.os.RemoteException;
import android.os.TransactionTooLargeException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

/**
 * Parent class of all sync adapters (EasMailbox, EasCalendar, and EasContacts)
 *
 */
public abstract class AbstractSyncAdapter {

    public static final int SECONDS = 1000;
    public static final int MINUTES = SECONDS*60;
    public static final int HOURS = MINUTES*60;
    public static final int DAYS = HOURS*24;
    public static final int WEEKS = DAYS*7;

    protected static final String PIM_WINDOW_SIZE = "4";

    private static final long SEPARATOR_ID = Long.MAX_VALUE;

    public Mailbox mMailbox;
    public EasSyncService mService;
    public Context mContext;
    public Account mAccount;
    public final ContentResolver mContentResolver;
    public final android.accounts.Account mAccountManagerAccount;

    // Create the data for local changes that need to be sent up to the server
    public abstract boolean sendLocalChanges(Serializer s) throws IOException;
    // Parse incoming data from the EAS server, creating, modifying, and deleting objects as
    // required through the EmailProvider
    public abstract boolean parse(InputStream is) throws IOException, CommandStatusException;
    // The name used to specify the collection type of the target (Email, Calendar, or Contacts)
    public abstract String getCollectionName();
    public abstract void cleanup();
    public abstract boolean isSyncable();
    // Add sync options (filter, body type - html vs plain, and truncation)
    public abstract void sendSyncOptions(Double protocolVersion, Serializer s, boolean initialSync)
            throws IOException;
    /**
     * Delete all records of this class in this account
     */
    public abstract void wipe();

    public boolean isLooping() {
        return false;
    }

    public AbstractSyncAdapter(EasSyncService service) {
        mService = service;
        mMailbox = service.mMailbox;
        mContext = service.mContext;
        mAccount = service.mAccount;
        mAccountManagerAccount = new android.accounts.Account(mAccount.mEmailAddress,
                Eas.EXCHANGE_ACCOUNT_MANAGER_TYPE);
        mContentResolver = mContext.getContentResolver();
    }

    public void userLog(String ...strings) {
        mService.userLog(strings);
    }

    public void incrementChangeCount() {
        mService.mChangeCount++;
    }

    /**
     * Set sync options common to PIM's (contacts and calendar)
     * @param protocolVersion the protocol version under which we're syncing
     * @param the filter to use (or null)
     * @param s the Serializer
     * @throws IOException
     */
    protected void setPimSyncOptions(Double protocolVersion, String filter, Serializer s)
            throws IOException {
        s.tag(Tags.SYNC_DELETES_AS_MOVES);
        s.tag(Tags.SYNC_GET_CHANGES);
        s.data(Tags.SYNC_WINDOW_SIZE, PIM_WINDOW_SIZE);
        s.start(Tags.SYNC_OPTIONS);
        // Set the filter (lookback), if provided
        if (filter != null) {
            s.data(Tags.SYNC_FILTER_TYPE, filter);
        }
        // Set the truncation amount and body type
        if (protocolVersion >= Eas.SUPPORTED_PROTOCOL_EX2007_DOUBLE) {
            s.start(Tags.BASE_BODY_PREFERENCE);
            // Plain text
            s.data(Tags.BASE_TYPE, Eas.BODY_PREFERENCE_TEXT);
            s.data(Tags.BASE_TRUNCATION_SIZE, Eas.EAS12_TRUNCATION_SIZE);
            s.end();
        } else {
            s.data(Tags.SYNC_TRUNCATION, Eas.EAS2_5_TRUNCATION_SIZE);
        }
        s.end();
    }

    /**
     * Returns the current SyncKey; override if the SyncKey is stored elsewhere (as for Contacts)
     * @return the current SyncKey for the Mailbox
     * @throws IOException
     */
    public String getSyncKey() throws IOException {
        if (mMailbox.mSyncKey == null) {
            userLog("Reset SyncKey to 0");
            mMailbox.mSyncKey = "0";
        }
        return mMailbox.mSyncKey;
    }

    public void setSyncKey(String syncKey, boolean inCommands) throws IOException {
        mMailbox.mSyncKey = syncKey;
    }

    /**
     * Operation is our binder-safe ContentProviderOperation (CPO) construct; an Operation can
     * be created from a CPO, a CPO Builder, or a CPO Builder with a "back reference" column name
     * and offset (that might be used in Builder.withValueBackReference).  The CPO is not actually
     * built until it is ready to be executed (with applyBatch); this allows us to recalculate
     * back reference offsets if we are required to re-send a large batch in smaller chunks.
     *
     * NOTE: A failed binder transaction is something of an emergency case, and shouldn't happen
     * with any frequency.  When it does, and we are forced to re-send the data to the content
     * provider in smaller chunks, we DO lose the sync-window atomicity, and thereby add another
     * small risk to the data.  Of course, this is far, far better than dropping the data on the
     * floor, as was done before the framework implemented TransactionTooLargeException
     */
    protected static class Operation {
        final ContentProviderOperation mOp;
        final ContentProviderOperation.Builder mBuilder;
        final String mColumnName;
        final int mOffset;
        // Is this Operation a separator? (a good place to break up a large transaction)
        boolean mSeparator = false;

        // For toString()
        final String[] TYPES = new String[] {"???", "Ins", "Upd", "Del", "Assert"};

        Operation(ContentProviderOperation.Builder builder, String columnName, int offset) {
            mOp = null;
            mBuilder = builder;
            mColumnName = columnName;
            mOffset = offset;
        }

        Operation(ContentProviderOperation.Builder builder) {
            mOp = null;
            mBuilder = builder;
            mColumnName = null;
            mOffset = 0;
        }

        Operation(ContentProviderOperation op) {
            mOp = op;
            mBuilder = null;
            mColumnName = null;
            mOffset = 0;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder("Op: ");
            ContentProviderOperation op = operationToContentProviderOperation(this, 0);
            int type = 0;
            //DO NOT SHIP WITH THE FOLLOWING LINE (the API is hidden!)
            //type = op.getType();
            sb.append(TYPES[type]);
            Uri uri = op.getUri();
            sb.append(' ');
            sb.append(uri.getPath());
            if (mColumnName != null) {
                sb.append(" Back value of " + mColumnName + ": " + mOffset);
            }
            return sb.toString();
        }
    }

    /**
     * We apply the batch of CPO's here.  We synchronize on the service to avoid thread-nasties,
     * and we just return quickly if the service has already been stopped.
     */
    private ContentProviderResult[] execute(String authority,
            ArrayList<ContentProviderOperation> ops)
            throws RemoteException, OperationApplicationException {
        synchronized (mService.getSynchronizer()) {
            if (!mService.isStopped()) {
                if (!ops.isEmpty()) {
                    ContentProviderResult[] result = mContentResolver.applyBatch(authority, ops);
                    mService.userLog("Results: " + result.length);
                    return result;
                }
            }
        }
        return new ContentProviderResult[0];
    }

    /**
     * Convert an Operation to a CPO; if the Operation has a back reference, apply it with the
     * passed-in offset
     */
    @VisibleForTesting
    static ContentProviderOperation operationToContentProviderOperation(Operation op, int offset) {
        if (op.mOp != null) {
            return op.mOp;
        } else if (op.mBuilder == null) {
            throw new IllegalArgumentException("Operation must have CPO.Builder");
        }
        ContentProviderOperation.Builder builder = op.mBuilder;
        if (op.mColumnName != null) {
            builder.withValueBackReference(op.mColumnName, op.mOffset - offset);
        }
        return builder.build();
    }

    /**
     * Create a list of CPOs from a list of Operations, and then apply them in a batch
     */
    private ContentProviderResult[] applyBatch(String authority, ArrayList<Operation> ops,
            int offset) throws RemoteException, OperationApplicationException {
        // Handle the empty case
        if (ops.isEmpty()) {
            return new ContentProviderResult[0];
        }
        ArrayList<ContentProviderOperation> cpos = new ArrayList<ContentProviderOperation>();
        for (Operation op: ops) {
            cpos.add(operationToContentProviderOperation(op, offset));
        }
        return execute(authority, cpos);
    }

    /**
     * Apply the list of CPO's in the provider and copy the "mini" result into our full result array
     */
    private void applyAndCopyResults(String authority, ArrayList<Operation> mini,
            ContentProviderResult[] result, int offset) throws RemoteException {
        // Empty lists are ok; we just ignore them
        if (mini.isEmpty()) return;
        try {
            ContentProviderResult[] miniResult = applyBatch(authority, mini, offset);
            // Copy the results from this mini-batch into our results array
            System.arraycopy(miniResult, 0, result, offset, miniResult.length);
        } catch (OperationApplicationException e) {
            // Not possible since we're building the ops ourselves
        }
    }

    /**
     * Called by a sync adapter to execute a list of Operations in the ContentProvider handling
     * the passed-in authority.  If the attempt to apply the batch fails due to a too-large
     * binder transaction, we split the Operations as directed by separators.  If any of the
     * "mini" batches fails due to a too-large transaction, we're screwed, but this would be
     * vanishingly rare.  Other, possibly transient, errors are handled by throwing a
     * RemoteException, which the caller will likely re-throw as an IOException so that the sync
     * can be attempted again.
     *
     * Callers MAY leave a dangling separator at the end of the list; note that the separators
     * themselves are only markers and are not sent to the provider.
     */
    protected ContentProviderResult[] safeExecute(String authority, ArrayList<Operation> ops)
            throws RemoteException {
        mService.userLog("Try to execute ", ops.size(), " CPO's for " + authority);
        ContentProviderResult[] result = null;
        try {
            // Try to execute the whole thing
            return applyBatch(authority, ops, 0);
        } catch (TransactionTooLargeException e) {
            // Nope; split into smaller chunks, demarcated by the separator operation
            mService.userLog("Transaction too large; spliting!");
            ArrayList<Operation> mini = new ArrayList<Operation>();
            // Build a result array with the total size we're sending
            result = new ContentProviderResult[ops.size()];
            int count = 0;
            int offset = 0;
            for (Operation op: ops) {
                if (op.mSeparator) {
                    try {
                        mService.userLog("Try mini-batch of ", mini.size(), " CPO's");
                        applyAndCopyResults(authority, mini, result, offset);
                        mini.clear();
                        // Save away the offset here; this will need to be subtracted out of the
                        // value originally set by the adapter
                        offset = count + 1; // Remember to add 1 for the separator!
                    } catch (TransactionTooLargeException e1) {
                        throw new RuntimeException("Can't send transaction; sync stopped.");
                    } catch (RemoteException e1) {
                        throw e1;
                    }
                } else {
                    mini.add(op);
                }
                count++;
            }
            // Check out what's left; if it's more than just a separator, apply the batch
            int miniSize = mini.size();
            if ((miniSize > 0) && !(miniSize == 1 && mini.get(0).mSeparator)) {
                applyAndCopyResults(authority, mini, result, offset);
            }
        } catch (RemoteException e) {
            throw e;
        } catch (OperationApplicationException e) {
            // Not possible since we're building the ops ourselves
        }
        return result;
    }

    /**
     * Called by a sync adapter to indicate a relatively safe place to split a batch of CPO's
     */
    protected void addSeparatorOperation(ArrayList<Operation> ops, Uri uri) {
        Operation op = new Operation(
                ContentProviderOperation.newDelete(ContentUris.withAppendedId(uri, SEPARATOR_ID)));
        op.mSeparator = true;
        ops.add(op);
    }
}

