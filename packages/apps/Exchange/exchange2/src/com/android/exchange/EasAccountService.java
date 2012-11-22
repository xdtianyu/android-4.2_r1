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

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.TrafficStats;
import android.os.RemoteException;
import android.os.SystemClock;
import android.provider.CalendarContract;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;

import com.android.emailcommon.TrafficFlags;
import com.android.emailcommon.mail.MessagingException;
import com.android.emailcommon.provider.Account;
import com.android.emailcommon.provider.EmailContent;
import com.android.emailcommon.provider.EmailContent.AccountColumns;
import com.android.emailcommon.provider.EmailContent.HostAuthColumns;
import com.android.emailcommon.provider.EmailContent.MailboxColumns;
import com.android.emailcommon.provider.HostAuth;
import com.android.emailcommon.provider.Mailbox;
import com.android.emailcommon.provider.Policy;
import com.android.emailcommon.provider.ProviderUnavailableException;
import com.android.emailcommon.service.EmailServiceStatus;
import com.android.emailcommon.service.PolicyServiceProxy;
import com.android.exchange.CommandStatusException.CommandStatus;
import com.android.exchange.adapter.AccountSyncAdapter;
import com.android.exchange.adapter.FolderSyncParser;
import com.android.exchange.adapter.Parser.EasParserException;
import com.android.exchange.adapter.PingParser;
import com.android.exchange.adapter.Serializer;
import com.android.exchange.adapter.Tags;
import com.android.exchange.provider.MailboxUtilities;
import com.google.common.annotations.VisibleForTesting;

import org.apache.http.Header;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ByteArrayEntity;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * AccountMailbox handles sync for the EAS "account mailbox"; this includes sync of the mailbox list
 * as well as management of mailbox push (using the EAS "Ping" command
 */
public class EasAccountService extends EasSyncService {
    private static final String WHERE_ACCOUNT_AND_SYNC_INTERVAL_PING =
        MailboxColumns.ACCOUNT_KEY + "=? and " + MailboxColumns.SYNC_INTERVAL +
        '=' + Mailbox.CHECK_INTERVAL_PING;
    private static final String AND_FREQUENCY_PING_PUSH_AND_NOT_ACCOUNT_MAILBOX = " AND " +
        MailboxColumns.SYNC_INTERVAL + " IN (" + Mailbox.CHECK_INTERVAL_PING +
        ',' + Mailbox.CHECK_INTERVAL_PUSH + ") AND " + MailboxColumns.TYPE + "!=\"" +
        Mailbox.TYPE_EAS_ACCOUNT_MAILBOX + '\"';
    private static final String WHERE_PUSH_HOLD_NOT_ACCOUNT_MAILBOX =
        MailboxColumns.ACCOUNT_KEY + "=? and " + MailboxColumns.SYNC_INTERVAL +
        '=' + Mailbox.CHECK_INTERVAL_PUSH_HOLD;
    private static final String WHERE_ACCOUNT_KEY_AND_SERVER_ID =
        MailboxColumns.ACCOUNT_KEY + "=? and " + MailboxColumns.SERVER_ID + "=?";

    /**
     * We start with an 8 minute timeout, and increase/decrease by 3 minutes at a time.  There's
     * no point having a timeout shorter than 5 minutes, I think; at that point, we can just let
     * the ping exception out.  The maximum I use is 17 minutes, which is really an empirical
     * choice; too long and we risk silent connection loss and loss of push for that period.  Too
     * short and we lose efficiency/battery life.
     *
     * If we ever have to drop the ping timeout, we'll never increase it again.  There's no point
     * going into hysteresis; the NAT timeout isn't going to change without a change in connection,
     * which will cause the sync service to be restarted at the starting heartbeat and going through
     * the process again.
     */
    static private final int PING_MINUTES = 60; // in seconds
    static private final int PING_FUDGE_LOW = 10;
    static private final int PING_STARTING_HEARTBEAT = (8*PING_MINUTES)-PING_FUDGE_LOW;
    static private final int PING_HEARTBEAT_INCREMENT = 3*PING_MINUTES;

    static private final int PROTOCOL_PING_STATUS_COMPLETED = 1;
    static private final int PROTOCOL_PING_STATUS_BAD_PARAMETERS = 3;
    static private final int PROTOCOL_PING_STATUS_RETRY = 8;

    // Fallbacks (in minutes) for ping loop failures
    static private final int MAX_PING_FAILURES = 1;
    static private final int PING_FALLBACK_INBOX = 5;
    static private final int PING_FALLBACK_PIM = 25;

    // The amount of time the account mailbox will sleep if there are no pingable mailboxes
    // This could happen if the sync time is set to "never"; we always want to check in from time
    // to time, however, for folder list/policy changes
    static private final int ACCOUNT_MAILBOX_SLEEP_TIME = 20*MINUTES;
    static private final String ACCOUNT_MAILBOX_SLEEP_TEXT =
        "Account mailbox sleeping for " + (ACCOUNT_MAILBOX_SLEEP_TIME / MINUTES) + "m";

    // Our heartbeat when we are waiting for ping boxes to be ready
    /*package*/ int mPingForceHeartbeat = 2*PING_MINUTES;
    // The minimum heartbeat we will send
    /*package*/ int mPingMinHeartbeat = (5*PING_MINUTES)-PING_FUDGE_LOW;
    // The maximum heartbeat we will send
    /*package*/ int mPingMaxHeartbeat = (17*PING_MINUTES)-PING_FUDGE_LOW;
    // The ping time (in seconds)
    /*package*/ int mPingHeartbeat = PING_STARTING_HEARTBEAT;
    // The longest successful ping heartbeat
    private int mPingHighWaterMark = 0;
    // Whether we've ever lowered the heartbeat
    /*package*/ boolean mPingHeartbeatDropped = false;

    private final String[] mBindArguments = new String[2];
    private ArrayList<String> mPingChangeList;

    protected EasAccountService(Context _context, Mailbox _mailbox) {
        super(_context, _mailbox);
    }

    @VisibleForTesting
    protected EasAccountService() {
    }

    @Override
    public void run() {
        mExitStatus = EXIT_DONE;
        try {
            // Make sure account and mailbox are still valid
            if (!setupService()) return;
            // If we've been stopped, we're done
            if (mStop) return;

            try {
                mDeviceId = ExchangeService.getDeviceId(mContext);
                int trafficFlags = TrafficFlags.getSyncFlags(mContext, mAccount);
                TrafficStats.setThreadStatsTag(trafficFlags | TrafficFlags.DATA_EMAIL);
                if ((mMailbox == null) || (mAccount == null)) {
                    return;
                } else {
                    sync();
                }
            } catch (EasAuthenticationException e) {
                userLog("Caught authentication error");
                mExitStatus = EXIT_LOGIN_FAILURE;
            } catch (IOException e) {
                String message = e.getMessage();
                userLog("Caught IOException: ", (message == null) ? "No message" : message);
                mExitStatus = EXIT_IO_ERROR;
            } catch (Exception e) {
                userLog("Uncaught exception in AccountMailboxService", e);
            } finally {
                ExchangeService.done(this);
                if (!mStop) {
                    userLog("Sync finished");
                    switch (mExitStatus) {
                        case EXIT_SECURITY_FAILURE:
                            // Ask for a new folder list. This should wake up the account mailbox; a
                            // security error in account mailbox should start provisioning
                            ExchangeService.reloadFolderList(mContext, mAccount.mId, true);
                            break;
                        case EXIT_EXCEPTION:
                            errorLog("Sync ended due to an exception.");
                            break;
                    }
                } else {
                    userLog("Stopped sync finished.");
                }

                // Make sure ExchangeService knows about this
                ExchangeService.kick("sync finished");
            }
        } catch (ProviderUnavailableException e) {
            Log.e(TAG, "EmailProvider unavailable; sync ended prematurely");
        }
    }

    /**
     * If possible, update the account to the new server address; report result
     * @param resp the EasResponse from the current POST
     * @return whether or not the redirect is handled and the POST should be retried
     */
    private boolean canHandleAccountMailboxRedirect(EasResponse resp) {
        userLog("AccountMailbox redirect error");
        HostAuth ha =
                HostAuth.restoreHostAuthWithId(mContext, mAccount.mHostAuthKeyRecv);
        if (ha != null && getValidateRedirect(resp, ha)) {
            // Update the account's HostAuth with new values
            ContentValues haValues = new ContentValues();
            haValues.put(HostAuthColumns.ADDRESS, ha.mAddress);
            ha.update(mContext, haValues);
            return true;
        }
        return false;
    }

    /**
     * Performs FolderSync
     *
     * @throws IOException
     * @throws EasParserException
     */
    public void sync() throws IOException, EasParserException {
         // Check that the account's mailboxes are consistent
        MailboxUtilities.checkMailboxConsistency(mContext, mAccount.mId);
        // Initialize exit status to success
        try {
            try {
                ExchangeService.callback()
                    .syncMailboxListStatus(mAccount.mId, EmailServiceStatus.IN_PROGRESS, 0);
            } catch (RemoteException e1) {
                // Don't care if this fails
            }

            if (mAccount.mSyncKey == null) {
                mAccount.mSyncKey = "0";
                userLog("Account syncKey INIT to 0");
                ContentValues cv = new ContentValues();
                cv.put(AccountColumns.SYNC_KEY, mAccount.mSyncKey);
                mAccount.update(mContext, cv);
            }

            boolean firstSync = mAccount.mSyncKey.equals("0");
            if (firstSync) {
                userLog("Initial FolderSync");
            }

            // When we first start up, change all mailboxes to push.
            ContentValues cv = new ContentValues();
            cv.put(Mailbox.SYNC_INTERVAL, Mailbox.CHECK_INTERVAL_PUSH);
            if (mContentResolver.update(Mailbox.CONTENT_URI, cv,
                    WHERE_ACCOUNT_AND_SYNC_INTERVAL_PING,
                    new String[] {Long.toString(mAccount.mId)}) > 0) {
                ExchangeService.kick("change ping boxes to push");
            }

            // Determine our protocol version, if we haven't already and save it in the Account
            // Also re-check protocol version at least once a day (in case of upgrade)
            if (mAccount.mProtocolVersion == null || firstSync ||
                   ((System.currentTimeMillis() - mMailbox.mSyncTime) > DAYS)) {
                userLog("Determine EAS protocol version");
                EasResponse resp = sendHttpClientOptions();
                try {
                    int code = resp.getStatus();
                    userLog("OPTIONS response: ", code);
                    if (code == HttpStatus.SC_OK) {
                        Header header = resp.getHeader("MS-ASProtocolCommands");
                        userLog(header.getValue());
                        header = resp.getHeader("ms-asprotocolversions");
                        try {
                            setupProtocolVersion(this, header);
                        } catch (MessagingException e) {
                            // Since we've already validated, this can't really happen
                            // But if it does, we'll rethrow this...
                            throw new IOException(e);
                        }
                        // Save the protocol version
                        cv.clear();
                        // Save the protocol version in the account; if we're using 12.0 or greater,
                        // set the flag for support of SmartForward
                        cv.put(Account.PROTOCOL_VERSION, mProtocolVersion);
                        if (mProtocolVersionDouble >= 12.0) {
                            cv.put(Account.FLAGS,
                                    mAccount.mFlags |
                                    Account.FLAGS_SUPPORTS_SMART_FORWARD |
                                    Account.FLAGS_SUPPORTS_SEARCH |
                                    Account.FLAGS_SUPPORTS_GLOBAL_SEARCH);
                        }
                        mAccount.update(mContext, cv);
                        cv.clear();
                        // Save the sync time of the account mailbox to current time
                        cv.put(Mailbox.SYNC_TIME, System.currentTimeMillis());
                        mMailbox.update(mContext, cv);
                     } else if (code == EAS_REDIRECT_CODE && canHandleAccountMailboxRedirect(resp)) {
                        // Cause this to re-run
                        throw new IOException("Will retry after a brief hold...");
                     } else {
                        errorLog("OPTIONS command failed; throwing IOException");
                        throw new IOException();
                    }
                } finally {
                    resp.close();
                }
            }

            // Change all pushable boxes to push when we start the account mailbox
            if (mAccount.mSyncInterval == Account.CHECK_INTERVAL_PUSH) {
                cv.clear();
                cv.put(Mailbox.SYNC_INTERVAL, Mailbox.CHECK_INTERVAL_PUSH);
                if (mContentResolver.update(Mailbox.CONTENT_URI, cv,
                        ExchangeService.WHERE_IN_ACCOUNT_AND_PUSHABLE,
                        new String[] {Long.toString(mAccount.mId)}) > 0) {
                    userLog("Push account; set pushable boxes to push...");
                }
            }

            while (!isStopped()) {
                // If we're not allowed to sync (e.g. roaming policy), leave now
                if (!ExchangeService.canAutoSync(mAccount)) return;
                userLog("Sending Account syncKey: ", mAccount.mSyncKey);
                Serializer s = new Serializer();
                s.start(Tags.FOLDER_FOLDER_SYNC).start(Tags.FOLDER_SYNC_KEY)
                    .text(mAccount.mSyncKey).end().end().done();
                EasResponse resp = sendHttpClientPost("FolderSync", s.toByteArray());
                try {
                    if (isStopped()) break;
                    int code = resp.getStatus();
                    if (code == HttpStatus.SC_OK) {
                        if (!resp.isEmpty()) {
                            InputStream is = resp.getInputStream();
                            // Returns true if we need to sync again
                            if (new FolderSyncParser(is,
                                    new AccountSyncAdapter(this)).parse()) {
                                continue;
                            }
                        }
                    } else if (EasResponse.isProvisionError(code)) {
                        throw new CommandStatusException(CommandStatus.NEEDS_PROVISIONING);
                    } else if (EasResponse.isAuthError(code)) {
                        mExitStatus = EasSyncService.EXIT_LOGIN_FAILURE;
                        return;
                    } else if (code == EAS_REDIRECT_CODE && canHandleAccountMailboxRedirect(resp)) {
                        // This will cause a retry of the FolderSync
                        continue;
                    } else {
                        userLog("FolderSync response error: ", code);
                    }
                } finally {
                    resp.close();
                }

                // Change all push/hold boxes to push
                cv.clear();
                cv.put(Mailbox.SYNC_INTERVAL, Account.CHECK_INTERVAL_PUSH);
                if (mContentResolver.update(Mailbox.CONTENT_URI, cv,
                        WHERE_PUSH_HOLD_NOT_ACCOUNT_MAILBOX,
                        new String[] {Long.toString(mAccount.mId)}) > 0) {
                    userLog("Set push/hold boxes to push...");
                }

                try {
                    ExchangeService.callback()
                        .syncMailboxListStatus(mAccount.mId,
                                exitStatusToServiceStatus(mExitStatus),
                                0);
                } catch (RemoteException e1) {
                    // Don't care if this fails
                }

                // Before each run of the pingLoop, if this Account has a PolicySet, make sure it's
                // active; otherwise, clear out the key/flag.  This should cause a provisioning
                // error on the next POST, and start the security sequence over again
                String key = mAccount.mSecuritySyncKey;
                if (!TextUtils.isEmpty(key)) {
                    Policy policy = Policy.restorePolicyWithId(mContext, mAccount.mPolicyKey);
                    if ((policy != null) && !PolicyServiceProxy.isActive(mContext, policy)) {
                        resetSecurityPolicies();
                    }
                }

                // Wait for push notifications.
                String threadName = Thread.currentThread().getName();
                try {
                    runPingLoop();
                } catch (StaleFolderListException e) {
                    // We break out if we get told about a stale folder list
                    userLog("Ping interrupted; folder list requires sync...");
                } catch (IllegalHeartbeatException e) {
                    // If we're sending an illegal heartbeat, reset either the min or the max to
                    // that heartbeat
                    resetHeartbeats(e.mLegalHeartbeat);
                } finally {
                    Thread.currentThread().setName(threadName);
                }
            }
        } catch (CommandStatusException e) {
            // If the sync error is a provisioning failure (perhaps policies changed),
            // let's try the provisioning procedure
            // Provisioning must only be attempted for the account mailbox - trying to
            // provision any other mailbox may result in race conditions and the
            // creation of multiple policy keys.
            int status = e.mStatus;
            if (CommandStatus.isNeedsProvisioning(status)) {
                if (!tryProvision(this)) {
                    // Set the appropriate failure status
                    mExitStatus = EasSyncService.EXIT_SECURITY_FAILURE;
                    return;
                }
            } else if (CommandStatus.isDeniedAccess(status)) {
                mExitStatus = EasSyncService.EXIT_ACCESS_DENIED;
                try {
                    ExchangeService.callback().syncMailboxListStatus(mAccount.mId,
                            EmailServiceStatus.ACCESS_DENIED, 0);
                } catch (RemoteException e1) {
                    // Don't care if this fails
                }
                return;
            } else {
                userLog("Unexpected status: " + CommandStatus.toString(status));
                mExitStatus = EasSyncService.EXIT_EXCEPTION;
            }
        } catch (IOException e) {
            // We catch this here to send the folder sync status callback
            // A folder sync failed callback will get sent from run()
            try {
                if (!isStopped()) {
                    // NOTE: The correct status is CONNECTION_ERROR, but the UI displays this, and
                    // it's not really appropriate for EAS as this is not unexpected for a ping and
                    // connection errors are retried in any case
                    ExchangeService.callback()
                        .syncMailboxListStatus(mAccount.mId, EmailServiceStatus.SUCCESS, 0);
                }
            } catch (RemoteException e1) {
                // Don't care if this fails
            }
            throw e;
        }
    }

    /**
     * Reset either our minimum or maximum ping heartbeat to a heartbeat known to be legal
     * @param legalHeartbeat a known legal heartbeat (from the EAS server)
     */
    /*package*/ void resetHeartbeats(int legalHeartbeat) {
        userLog("Resetting min/max heartbeat, legal = " + legalHeartbeat);
        // We are here because the current heartbeat (mPingHeartbeat) is invalid.  Depending on
        // whether the argument is above or below the current heartbeat, we can infer the need to
        // change either the minimum or maximum heartbeat
        if (legalHeartbeat > mPingHeartbeat) {
            // The legal heartbeat is higher than the ping heartbeat; therefore, our minimum was
            // too low.  We respond by raising either or both of the minimum heartbeat or the
            // force heartbeat to the argument value
            if (mPingMinHeartbeat < legalHeartbeat) {
                mPingMinHeartbeat = legalHeartbeat;
            }
            if (mPingForceHeartbeat < legalHeartbeat) {
                mPingForceHeartbeat = legalHeartbeat;
            }
            // If our minimum is now greater than the max, bring them together
            if (mPingMinHeartbeat > mPingMaxHeartbeat) {
                mPingMaxHeartbeat = legalHeartbeat;
            }
        } else if (legalHeartbeat < mPingHeartbeat) {
            // The legal heartbeat is lower than the ping heartbeat; therefore, our maximum was
            // too high.  We respond by lowering the maximum to the argument value
            mPingMaxHeartbeat = legalHeartbeat;
            // If our maximum is now less than the minimum, bring them together
            if (mPingMaxHeartbeat < mPingMinHeartbeat) {
                mPingMinHeartbeat = legalHeartbeat;
            }
        }
        // Set current heartbeat to the legal heartbeat
        mPingHeartbeat = legalHeartbeat;
        // Allow the heartbeat logic to run
        mPingHeartbeatDropped = false;
    }

    private void pushFallback(long mailboxId) {
        Mailbox mailbox = Mailbox.restoreMailboxWithId(mContext, mailboxId);
        if (mailbox == null) {
            return;
        }
        ContentValues cv = new ContentValues();
        int mins = PING_FALLBACK_PIM;
        if (mailbox.mType == Mailbox.TYPE_INBOX) {
            mins = PING_FALLBACK_INBOX;
        }
        cv.put(Mailbox.SYNC_INTERVAL, mins);
        mContentResolver.update(ContentUris.withAppendedId(Mailbox.CONTENT_URI, mailboxId),
                cv, null, null);
        errorLog("*** PING ERROR LOOP: Set " + mailbox.mDisplayName + " to " + mins
                + " min sync");
        ExchangeService.kick("push fallback");
    }

    /**
     * Simplistic attempt to determine a NAT timeout, based on experience with various carriers
     * and networks.  The string "reset by peer" is very common in these situations, so we look for
     * that specifically.  We may add additional tests here as more is learned.
     * @param message
     * @return whether this message is likely associated with a NAT failure
     */
    private boolean isLikelyNatFailure(String message) {
        if (message == null) return false;
        if (message.contains("reset by peer")) {
            return true;
        }
        return false;
    }

    private void sleep(long ms, boolean runAsleep) {
        if (runAsleep) {
            ExchangeService.runAsleep(mMailboxId, ms+(5*SECONDS));
        }
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            // Doesn't matter whether we stop early; it's the thought that counts
        } finally {
            if (runAsleep) {
                ExchangeService.runAwake(mMailboxId);
            }
        }
    }

    @Override
    protected EasResponse sendPing(byte[] bytes, int heartbeat) throws IOException {
       Thread.currentThread().setName(mAccount.mDisplayName + ": Ping");
       if (Eas.USER_LOG) {
           userLog("Send ping, timeout: " + heartbeat + "s, high: " + mPingHighWaterMark + 's');
       }
       return sendHttpClientPost(EasSyncService.PING_COMMAND, new ByteArrayEntity(bytes),
               (heartbeat+5)*SECONDS);
    }

    private void runPingLoop() throws IOException, StaleFolderListException,
            IllegalHeartbeatException, CommandStatusException {
        int pingHeartbeat = mPingHeartbeat;
        userLog("runPingLoop");
        // Do push for all sync services here
        long endTime = System.currentTimeMillis() + (30*MINUTES);
        HashMap<String, Integer> pingErrorMap = new HashMap<String, Integer>();
        ArrayList<String> readyMailboxes = new ArrayList<String>();
        ArrayList<String> notReadyMailboxes = new ArrayList<String>();
        int pingWaitCount = 0;
        long inboxId = -1;
        android.accounts.Account amAccount = new android.accounts.Account(mAccount.mEmailAddress,
                Eas.EXCHANGE_ACCOUNT_MANAGER_TYPE);
        while ((System.currentTimeMillis() < endTime) && !isStopped()) {
            // Count of pushable mailboxes
            int pushCount = 0;
            // Count of mailboxes that can be pushed right now
            int canPushCount = 0;
            // Count of uninitialized boxes
            int uninitCount = 0;

            Serializer s = new Serializer();
            Cursor c = mContentResolver.query(Mailbox.CONTENT_URI, Mailbox.CONTENT_PROJECTION,
                    MailboxColumns.ACCOUNT_KEY + '=' + mAccount.mId +
                    AND_FREQUENCY_PING_PUSH_AND_NOT_ACCOUNT_MAILBOX, null, null);
            if (c == null) throw new ProviderUnavailableException();
            notReadyMailboxes.clear();
            readyMailboxes.clear();
            // Look for an inbox, and remember its id
            if (inboxId == -1) {
                inboxId = Mailbox.findMailboxOfType(mContext, mAccount.mId, Mailbox.TYPE_INBOX);
            }
            try {
                // Loop through our pushed boxes seeing what is available to push
                while (c.moveToNext()) {
                    pushCount++;
                    // Two requirements for push:
                    // 1) ExchangeService tells us the mailbox is syncable (not running/not stopped)
                    // 2) The syncKey isn't "0" (i.e. it's synced at least once)
                    long mailboxId = c.getLong(Mailbox.CONTENT_ID_COLUMN);
                    String mailboxName = c.getString(Mailbox.CONTENT_DISPLAY_NAME_COLUMN);

                    // See what type of box we've got and get authority
                    int mailboxType = c.getInt(Mailbox.CONTENT_TYPE_COLUMN);
                    String authority = EmailContent.AUTHORITY;
                    switch(mailboxType) {
                        case Mailbox.TYPE_CALENDAR:
                            authority = CalendarContract.AUTHORITY;
                            break;
                        case Mailbox.TYPE_CONTACTS:
                            authority = ContactsContract.AUTHORITY;
                            break;
                    }

                    // See whether we can ping for this mailbox
                    int pingStatus;
                    if (!ContentResolver.getSyncAutomatically(amAccount, authority)) {
                        pingStatus = ExchangeService.PING_STATUS_DISABLED;
                    } else {
                        pingStatus = ExchangeService.pingStatus(mailboxId);

                    }

                    if (pingStatus == ExchangeService.PING_STATUS_OK) {
                        String syncKey = c.getString(Mailbox.CONTENT_SYNC_KEY_COLUMN);
                        if ((syncKey == null) || syncKey.equals("0")) {
                            // We can't push until the initial sync is done
                            pushCount--;
                            uninitCount++;
                            continue;
                        }

                        if (canPushCount++ == 0) {
                            // Initialize the Ping command
                            s.start(Tags.PING_PING)
                                .data(Tags.PING_HEARTBEAT_INTERVAL,
                                        Integer.toString(pingHeartbeat))
                                .start(Tags.PING_FOLDERS);
                        }

                        String folderClass = getTargetCollectionClassFromCursor(c);
                        s.start(Tags.PING_FOLDER)
                            .data(Tags.PING_ID, c.getString(Mailbox.CONTENT_SERVER_ID_COLUMN))
                            .data(Tags.PING_CLASS, folderClass)
                            .end();
                        readyMailboxes.add(mailboxName);
                    } else if ((pingStatus == ExchangeService.PING_STATUS_RUNNING) ||
                            (pingStatus == ExchangeService.PING_STATUS_WAITING)) {
                        notReadyMailboxes.add(mailboxName);
                    } else if (pingStatus == ExchangeService.PING_STATUS_UNABLE) {
                        pushCount--;
                        userLog(mailboxName, " in error state; ignore");
                        continue;
                    } else if (pingStatus == ExchangeService.PING_STATUS_DISABLED) {
                        pushCount--;
                        userLog(mailboxName, " disabled by user; ignore");
                        continue;
                    }
                }
            } finally {
                c.close();
            }

            if (Eas.USER_LOG) {
                if (!notReadyMailboxes.isEmpty()) {
                    userLog("Ping not ready for: " + notReadyMailboxes);
                }
                if (!readyMailboxes.isEmpty()) {
                    userLog("Ping ready for: " + readyMailboxes);
                }
            }

            // If we've waited 10 seconds or more, just ping with whatever boxes are ready
            // But use a shorter than normal heartbeat
            boolean forcePing = !notReadyMailboxes.isEmpty() && (pingWaitCount > 5);

            if ((canPushCount > 0) && ((canPushCount == pushCount) || forcePing)) {
                // If all pingable boxes are ready for push, send Ping to the server
                s.end().end().done();
                pingWaitCount = 0;
                mPostAborted = false;
                mPostReset = false;

                // If we've been stopped, this is a good time to return
                if (isStopped()) return;

                long pingTime = SystemClock.elapsedRealtime();
                try {
                    // Send the ping, wrapped by appropriate timeout/alarm
                    if (forcePing) {
                        userLog("Forcing ping after waiting for all boxes to be ready");
                    }
                    EasResponse resp =
                        sendPing(s.toByteArray(), forcePing ? mPingForceHeartbeat : pingHeartbeat);

                    try {
                        int code = resp.getStatus();
                        userLog("Ping response: ", code);

                        // If we're not allowed to sync (e.g. roaming policy), terminate gracefully
                        // now; otherwise we might start a sync based on the response
                        if (!ExchangeService.canAutoSync(mAccount)) {
                            stop();
                        }

                        // Return immediately if we've been asked to stop during the ping
                        if (isStopped()) {
                            userLog("Stopping pingLoop");
                            return;
                        }

                        if (code == HttpStatus.SC_OK) {
                            // Make sure to clear out any pending sync errors
                            ExchangeService.removeFromSyncErrorMap(mMailboxId);
                            if (!resp.isEmpty()) {
                                InputStream is = resp.getInputStream();
                                int pingResult = parsePingResult(is, mContentResolver,
                                        pingErrorMap);
                                // If our ping completed (status = 1), and wasn't forced and we're
                                // not at the maximum, try increasing timeout by two minutes
                                if (pingResult == PROTOCOL_PING_STATUS_COMPLETED && !forcePing) {
                                    if (pingHeartbeat > mPingHighWaterMark) {
                                        mPingHighWaterMark = pingHeartbeat;
                                        userLog("Setting high water mark at: ", mPingHighWaterMark);
                                    }
                                    if ((pingHeartbeat < mPingMaxHeartbeat) &&
                                            !mPingHeartbeatDropped) {
                                        pingHeartbeat += PING_HEARTBEAT_INCREMENT;
                                        if (pingHeartbeat > mPingMaxHeartbeat) {
                                            pingHeartbeat = mPingMaxHeartbeat;
                                        }
                                        userLog("Increase ping heartbeat to ", pingHeartbeat, "s");
                                    }
                                } else if (pingResult == PROTOCOL_PING_STATUS_BAD_PARAMETERS ||
                                        pingResult == PROTOCOL_PING_STATUS_RETRY) {
                                    // These errors appear to be server-related (I've seen a bad
                                    // parameters result with known good parameters...)
                                    userLog("Server error during Ping: " + pingResult);
                                    // Act as if we have an IOException (backoff, etc.)
                                    throw new IOException();
                                }
                            } else {
                                userLog("Ping returned empty result; throwing IOException");
                                throw new IOException();
                            }
                        } else if (EasResponse.isAuthError(code)) {
                            mExitStatus = EasSyncService.EXIT_LOGIN_FAILURE;
                            userLog("Authorization error during Ping: ", code);
                            throw new IOException();
                        }
                    } finally {
                        resp.close();
                    }
                } catch (IOException e) {
                    String message = e.getMessage();
                    // If we get the exception that is indicative of a NAT timeout and if we
                    // haven't yet "fixed" the timeout, back off by two minutes and "fix" it
                    boolean hasMessage = message != null;
                    userLog("IOException runPingLoop: " + (hasMessage ? message : "[no message]"));
                    if (mPostReset) {
                        // Nothing to do in this case; this is ExchangeService telling us to try
                        // another ping.
                    } else if (mPostAborted || isLikelyNatFailure(message)) {
                        long pingLength = SystemClock.elapsedRealtime() - pingTime;
                        if ((pingHeartbeat > mPingMinHeartbeat) &&
                                (pingHeartbeat > mPingHighWaterMark)) {
                            pingHeartbeat -= PING_HEARTBEAT_INCREMENT;
                            mPingHeartbeatDropped = true;
                            if (pingHeartbeat < mPingMinHeartbeat) {
                                pingHeartbeat = mPingMinHeartbeat;
                            }
                            userLog("Decreased ping heartbeat to ", pingHeartbeat, "s");
                        } else if (mPostAborted) {
                            // There's no point in throwing here; this can happen in two cases
                            // 1) An alarm, which indicates minutes without activity; no sense
                            //    backing off
                            // 2) ExchangeService abort, due to sync of mailbox.  Again, we want to
                            //    keep on trying to ping
                            userLog("Ping aborted; retry");
                        } else if (pingLength < 2000) {
                            userLog("Abort or NAT type return < 2 seconds; throwing IOException");
                            throw e;
                        } else {
                            userLog("NAT type IOException");
                        }
                    } else if (hasMessage && message.contains("roken pipe")) {
                        // The "broken pipe" error (uppercase or lowercase "b") seems to be an
                        // internal error, so let's not throw an exception (which leads to delays)
                        // but rather simply run through the loop again
                    } else {
                        throw e;
                    }
                }
            } else if (forcePing) {
                // In this case, there aren't any boxes that are pingable, but there are boxes
                // waiting (for IOExceptions)
                userLog("pingLoop waiting 60s for any pingable boxes");
                sleep(60*SECONDS, true);
            } else if (pushCount > 0) {
                // If we want to Ping, but can't just yet, wait a little bit
                // TODO Change sleep to wait and use notify from ExchangeService when a sync ends
                sleep(2*SECONDS, false);
                pingWaitCount++;
                //userLog("pingLoop waited 2s for: ", (pushCount - canPushCount), " box(es)");
            } else if (uninitCount > 0) {
                // In this case, we're doing an initial sync of at least one mailbox.  Since this
                // is typically a one-time case, I'm ok with trying again every 10 seconds until
                // we're in one of the other possible states.
                userLog("pingLoop waiting for initial sync of ", uninitCount, " box(es)");
                sleep(10*SECONDS, true);
            } else if (inboxId == -1) {
                // In this case, we're still syncing mailboxes, so sleep for only a short time
                sleep(45*SECONDS, true);
            } else {
                // We've got nothing to do, so we'll check again in 20 minutes at which time
                // we'll update the folder list, check for policy changes and/or remote wipe, etc.
                // Let the device sleep in the meantime...
                userLog(ACCOUNT_MAILBOX_SLEEP_TEXT);
                sleep(ACCOUNT_MAILBOX_SLEEP_TIME, true);
            }
        }

        // Save away the current heartbeat
        mPingHeartbeat = pingHeartbeat;
    }

    private int parsePingResult(InputStream is, ContentResolver cr,
            HashMap<String, Integer> errorMap)
            throws IOException, StaleFolderListException, IllegalHeartbeatException,
                CommandStatusException {
        PingParser pp = new PingParser(is, this);
        if (pp.parse()) {
            // True indicates some mailboxes need syncing...
            // syncList has the serverId's of the mailboxes...
            mBindArguments[0] = Long.toString(mAccount.mId);
            mPingChangeList = pp.getSyncList();
            for (String serverId: mPingChangeList) {
                mBindArguments[1] = serverId;
                Cursor c = cr.query(Mailbox.CONTENT_URI, Mailbox.CONTENT_PROJECTION,
                        WHERE_ACCOUNT_KEY_AND_SERVER_ID, mBindArguments, null);
                if (c == null) throw new ProviderUnavailableException();
                try {
                    if (c.moveToFirst()) {

                        /**
                         * Check the boxes reporting changes to see if there really were any...
                         * We do this because bugs in various Exchange servers can put us into a
                         * looping behavior by continually reporting changes in a mailbox, even when
                         * there aren't any.
                         *
                         * This behavior is seemingly random, and therefore we must code defensively
                         * by backing off of push behavior when it is detected.
                         *
                         * One known cause, on certain Exchange 2003 servers, is acknowledged by
                         * Microsoft, and the server hotfix for this case can be found at
                         * http://support.microsoft.com/kb/923282
                         */

                        // Check the status of the last sync
                        String status = c.getString(Mailbox.CONTENT_SYNC_STATUS_COLUMN);
                        int type = ExchangeService.getStatusType(status);
                        // This check should always be true...
                        if (type == ExchangeService.SYNC_PING) {
                            int changeCount = ExchangeService.getStatusChangeCount(status);
                            if (changeCount > 0) {
                                errorMap.remove(serverId);
                            } else if (changeCount == 0) {
                                // This means that a ping reported changes in error; we keep a count
                                // of consecutive errors of this kind
                                String name = c.getString(Mailbox.CONTENT_DISPLAY_NAME_COLUMN);
                                Integer failures = errorMap.get(serverId);
                                if (failures == null) {
                                    userLog("Last ping reported changes in error for: ", name);
                                    errorMap.put(serverId, 1);
                                } else if (failures > MAX_PING_FAILURES) {
                                    // We'll back off of push for this box
                                    pushFallback(c.getLong(Mailbox.CONTENT_ID_COLUMN));
                                    continue;
                                } else {
                                    userLog("Last ping reported changes in error for: ", name);
                                    errorMap.put(serverId, failures + 1);
                                }
                            }
                        }

                        // If there were no problems with previous sync, we'll start another one
                        ExchangeService.startManualSync(c.getLong(Mailbox.CONTENT_ID_COLUMN),
                                ExchangeService.SYNC_PING, null);
                    }
                } finally {
                    c.close();
                }
            }
        }
        return pp.getSyncStatus();
    }

    /**
     * Translate exit status code to service status code (used in callbacks)
     * @param exitStatus the service's exit status
     * @return the corresponding service status
     */
    private int exitStatusToServiceStatus(int exitStatus) {
        switch(exitStatus) {
            case EasSyncService.EXIT_SECURITY_FAILURE:
                return EmailServiceStatus.SECURITY_FAILURE;
            case EasSyncService.EXIT_LOGIN_FAILURE:
                return EmailServiceStatus.LOGIN_FAILED;
            default:
                return EmailServiceStatus.SUCCESS;
        }
    }
}
