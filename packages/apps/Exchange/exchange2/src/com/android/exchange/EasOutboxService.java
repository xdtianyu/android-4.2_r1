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

package com.android.exchange;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.TrafficStats;
import android.net.Uri;
import android.os.RemoteException;
import android.text.TextUtils;

import com.android.emailcommon.TrafficFlags;
import com.android.emailcommon.internet.Rfc822Output;
import com.android.emailcommon.mail.MessagingException;
import com.android.emailcommon.provider.Account;
import com.android.emailcommon.provider.EmailContent.Body;
import com.android.emailcommon.provider.EmailContent.BodyColumns;
import com.android.emailcommon.provider.EmailContent.MailboxColumns;
import com.android.emailcommon.provider.EmailContent.Message;
import com.android.emailcommon.provider.EmailContent.MessageColumns;
import com.android.emailcommon.provider.EmailContent.SyncColumns;
import com.android.emailcommon.provider.Mailbox;
import com.android.emailcommon.service.EmailServiceStatus;
import com.android.emailcommon.utility.Utility;
import com.android.exchange.CommandStatusException.CommandStatus;
import com.android.exchange.adapter.Parser;
import com.android.exchange.adapter.Parser.EmptyStreamException;
import com.android.exchange.adapter.Serializer;
import com.android.exchange.adapter.Tags;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.entity.InputStreamEntity;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class EasOutboxService extends EasSyncService {

    public static final int SEND_FAILED = 1;
    public static final String MAILBOX_KEY_AND_NOT_SEND_FAILED =
        MessageColumns.MAILBOX_KEY + "=? and (" + SyncColumns.SERVER_ID + " is null or " +
        SyncColumns.SERVER_ID + "!=" + SEND_FAILED + ')';
    public static final String[] BODY_SOURCE_PROJECTION =
        new String[] {BodyColumns.SOURCE_MESSAGE_KEY};
    public static final String WHERE_MESSAGE_KEY = Body.MESSAGE_KEY + "=?";

    // This is a normal email (i.e. not one of the other types)
    public static final int MODE_NORMAL = 0;
    // This is a smart reply email
    public static final int MODE_SMART_REPLY = 1;
    // This is a smart forward email
    public static final int MODE_SMART_FORWARD = 2;

    // This needs to be long enough to send the longest reasonable message, without being so long
    // as to effectively "hang" sending of mail.  The standard 30 second timeout isn't long enough
    // for pictures and the like.  For now, we'll use 15 minutes, in the knowledge that any socket
    // failure would probably generate an Exception before timing out anyway
    public static final int SEND_MAIL_TIMEOUT = 15*MINUTES;

    protected EasOutboxService(Context _context, Mailbox _mailbox) {
        super(_context, _mailbox);
    }

    /**
     * Our own HttpEntity subclass that is able to insert opaque data (in this case the MIME
     * representation of the message body as stored in a temporary file) into the serializer stream
     */
    private static class SendMailEntity extends InputStreamEntity {
        private final Context mContext;
        private final FileInputStream mFileStream;
        private final long mFileLength;
        private final int mSendTag;
        private final Message mMessage;

        private static final int[] MODE_TAGS =  new int[] {Tags.COMPOSE_SEND_MAIL,
            Tags.COMPOSE_SMART_REPLY, Tags.COMPOSE_SMART_FORWARD};

        public SendMailEntity(Context context, FileInputStream instream, long length, int tag,
                Message message) {
            super(instream, length);
            mContext = context;
            mFileStream = instream;
            mFileLength = length;
            mSendTag = tag;
            mMessage = message;
        }

        /**
         * We always return -1 because we don't know the actual length of the POST data (this
         * causes HttpClient to send the data in "chunked" mode)
         */
        @Override
        public long getContentLength() {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                // Calculate the overhead for the WBXML data
                writeTo(baos, false);
                // Return the actual size that will be sent
                return baos.size() + mFileLength;
            } catch (IOException e) {
                // Just return -1 (unknown)
            } finally {
                try {
                    baos.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
            return -1;
        }

        @Override
        public void writeTo(OutputStream outstream) throws IOException {
            writeTo(outstream, true);
        }

        /**
         * Write the message to the output stream
         * @param outstream the output stream to write
         * @param withData whether or not the actual data is to be written; true when sending
         *   mail; false when calculating size only
         * @throws IOException
         */
        public void writeTo(OutputStream outstream, boolean withData) throws IOException {
            // Not sure if this is possible; the check is taken from the superclass
            if (outstream == null) {
                throw new IllegalArgumentException("Output stream may not be null");
            }

            // We'll serialize directly into the output stream
            Serializer s = new Serializer(outstream);
            // Send the appropriate initial tag
            s.start(mSendTag);
            // The Message-Id for this message (note that we cannot use the messageId stored in
            // the message, as EAS 14 limits the length to 40 chars and we use 70+)
            s.data(Tags.COMPOSE_CLIENT_ID, "SendMail-" + System.nanoTime());
            // We always save sent mail
            s.tag(Tags.COMPOSE_SAVE_IN_SENT_ITEMS);

            // If we're using smart reply/forward, we need info about the original message
            if (mSendTag != Tags.COMPOSE_SEND_MAIL) {
                OriginalMessageInfo info = getOriginalMessageInfo(mContext, mMessage.mId);
                if (info != null) {
                    s.start(Tags.COMPOSE_SOURCE);
                    // For search results, use the long id (stored in mProtocolSearchInfo); else,
                    // use folder id/item id combo
                    if (mMessage.mProtocolSearchInfo != null) {
                        s.data(Tags.COMPOSE_LONG_ID, mMessage.mProtocolSearchInfo);
                    } else {
                        s.data(Tags.COMPOSE_ITEM_ID, info.mItemId);
                        s.data(Tags.COMPOSE_FOLDER_ID, info.mCollectionId);
                    }
                    s.end();  // Tags.COMPOSE_SOURCE
                }
            }

            // Start the MIME tag; this is followed by "opaque" data (byte array)
            s.start(Tags.COMPOSE_MIME);
            // Send opaque data from the file stream
            if (withData) {
                s.opaque(mFileStream, (int)mFileLength);
            } else {
                s.opaqueWithoutData((int)mFileLength);
            }
            // And we're done
            s.end().end().done();
        }
    }

    private static class SendMailParser extends Parser {
        private final int mStartTag;
        private int mStatus;

        public SendMailParser(InputStream in, int startTag) throws IOException {
            super(in);
            mStartTag = startTag;
        }

        public int getStatus() {
            return mStatus;
        }

        /**
         * The only useful info in the SendMail response is the status; we capture and save it
         */
        @Override
        public boolean parse() throws IOException {
            if (nextTag(START_DOCUMENT) != mStartTag) {
                throw new IOException();
            }
            while (nextTag(START_DOCUMENT) != END_DOCUMENT) {
                if (tag == Tags.COMPOSE_STATUS) {
                    mStatus = getValueInt();
                } else {
                    skipTag();
                }
            }
            return true;
        }
    }

    /**
     * For OriginalMessageInfo, we use the terminology of EAS for the serverId and mailboxId of the
     * original message
     */
    protected static class OriginalMessageInfo {
        final String mItemId;
        final String mCollectionId;
        final String mLongId;

        OriginalMessageInfo(String itemId, String collectionId, String longId) {
            mItemId = itemId;
            mCollectionId = collectionId;
            mLongId = longId;
        }
    }

    private void sendCallback(long msgId, String subject, int status) {
        try {
            ExchangeService.callback().sendMessageStatus(msgId, subject, status, 0);
        } catch (RemoteException e) {
            // It's all good
        }
    }

    /*package*/ String generateSmartSendCmd(boolean reply, OriginalMessageInfo info) {
        StringBuilder sb = new StringBuilder();
        sb.append(reply ? "SmartReply" : "SmartForward");
        if (!TextUtils.isEmpty(info.mLongId)) {
            sb.append("&LongId=");
            sb.append(Uri.encode(info.mLongId, ":"));
        } else {
            sb.append("&ItemId=");
            sb.append(Uri.encode(info.mItemId, ":"));
            sb.append("&CollectionId=");
            sb.append(Uri.encode(info.mCollectionId, ":"));
        }
        return sb.toString();
    }

    /**
     * Get information about the original message that is referenced by the message to be sent; this
     * information will exist for replies and forwards
     *
     * @param context the caller's context
     * @param msgId the id of the message we're sending
     * @return a data structure with the serverId and mailboxId of the original message, or null if
     * either or both of those pieces of information can't be found
     */
    private static OriginalMessageInfo getOriginalMessageInfo(Context context, long msgId) {
        // Note: itemId and collectionId are the terms used by EAS to refer to the serverId and
        // mailboxId of a Message
        String itemId = null;
        String collectionId = null;
        String longId = null;

        // First, we need to get the id of the reply/forward message
        String[] cols = Utility.getRowColumns(context, Body.CONTENT_URI,
                BODY_SOURCE_PROJECTION, WHERE_MESSAGE_KEY,
                new String[] {Long.toString(msgId)});
        if (cols != null) {
            long refId = Long.parseLong(cols[0]);
            // Then, we need the serverId and mailboxKey of the message
            cols = Utility.getRowColumns(context, Message.CONTENT_URI, refId,
                    SyncColumns.SERVER_ID, MessageColumns.MAILBOX_KEY,
                    MessageColumns.PROTOCOL_SEARCH_INFO);
            if (cols != null) {
                itemId = cols[0];
                long boxId = Long.parseLong(cols[1]);
                // Then, we need the serverId of the mailbox
                cols = Utility.getRowColumns(context, Mailbox.CONTENT_URI, boxId,
                        MailboxColumns.SERVER_ID);
                if (cols != null) {
                    collectionId = cols[0];
                }
            }
        }
        // We need either a longId or both itemId (serverId) and collectionId (mailboxId) to process
        // a smart reply or a smart forward
        if (longId != null || (itemId != null && collectionId != null)){
            return new OriginalMessageInfo(itemId, collectionId, longId);
        }
        return null;
    }

    private void sendFailed(long msgId, int result) {
        ContentValues cv = new ContentValues();
        cv.put(SyncColumns.SERVER_ID, SEND_FAILED);
        Message.update(mContext, Message.CONTENT_URI, msgId, cv);
        sendCallback(msgId, null, result);
    }

    /**
     * Send a single message via EAS
     * Note that we mark messages SEND_FAILED when there is a permanent failure, rather than an
     * IOException, which is handled by ExchangeService with retries, backoffs, etc.
     *
     * @param cacheDir the cache directory for this context
     * @param msgId the _id of the message to send
     * @throws IOException
     */
    int sendMessage(File cacheDir, long msgId) throws IOException, MessagingException {
        // We always return SUCCESS unless the sending error is account-specific (security or
        // authentication) rather than message-specific; returning anything else will terminate
        // the Outbox sync! Message-specific errors are marked in the messages themselves.
        int result = EmailServiceStatus.SUCCESS;
        // Say we're starting to send this message
        sendCallback(msgId, null, EmailServiceStatus.IN_PROGRESS);
        // Create a temporary file (this will hold the outgoing message in RFC822 (MIME) format)
        File tmpFile = File.createTempFile("eas_", "tmp", cacheDir);
        try {
            // Get the message and fail quickly if not found
            Message msg = Message.restoreMessageWithId(mContext, msgId);
            if (msg == null) return EmailServiceStatus.MESSAGE_NOT_FOUND;

            // See what kind of outgoing messge this is
            int flags = msg.mFlags;
            boolean reply = (flags & Message.FLAG_TYPE_REPLY) != 0;
            boolean forward = (flags & Message.FLAG_TYPE_FORWARD) != 0;
            boolean includeQuotedText = (flags & Message.FLAG_NOT_INCLUDE_QUOTED_TEXT) == 0;

            // The reference message and mailbox are called item and collection in EAS
            OriginalMessageInfo referenceInfo = null;
            // Respect the sense of the include quoted text flag
            if (includeQuotedText && (reply || forward)) {
                referenceInfo = getOriginalMessageInfo(mContext, msgId);
            }
            // Generally, we use SmartReply/SmartForward if we've got a good reference
            boolean smartSend = referenceInfo != null;
            // But we won't use SmartForward if the account isn't set up for it (currently, we only
            // use SmartForward for EAS 12.0 or later to avoid creating eml files that are
            // potentially difficult for the recipient to handle)
            if (forward && ((mAccount.mFlags & Account.FLAGS_SUPPORTS_SMART_FORWARD) == 0)) {
                smartSend = false;
            }

            // Write the message to the temporary file
            FileOutputStream fileOutputStream = new FileOutputStream(tmpFile);
            Rfc822Output.writeTo(mContext, msgId, fileOutputStream, smartSend, true);
            fileOutputStream.close();

            // Sending via EAS14 is a whole 'nother kettle of fish
            boolean isEas14 = (Double.parseDouble(mAccount.mProtocolVersion) >=
                Eas.SUPPORTED_PROTOCOL_EX2010_DOUBLE);

            while (true) {
                // Get an input stream to our temporary file and create an entity with it
                FileInputStream fileStream = new FileInputStream(tmpFile);
                long fileLength = tmpFile.length();

                // The type of entity depends on whether we're using EAS 14
                HttpEntity inputEntity;
                // For EAS 14, we need to save the wbxml tag we're using
                int modeTag = 0;
                if (isEas14) {
                    int mode =
                        !smartSend ? MODE_NORMAL : reply ? MODE_SMART_REPLY : MODE_SMART_FORWARD;
                    modeTag = SendMailEntity.MODE_TAGS[mode];
                    inputEntity =
                        new SendMailEntity(mContext, fileStream, fileLength, modeTag, msg);
                } else {
                    inputEntity = new InputStreamEntity(fileStream, fileLength);
                }
                // Create the appropriate command and POST it to the server
                String cmd = "SendMail";
                if (smartSend) {
                    // In EAS 14, we don't send itemId and collectionId in the command
                    if (isEas14) {
                        cmd = reply ? "SmartReply" : "SmartForward";
                    } else {
                        cmd = generateSmartSendCmd(reply, referenceInfo);
                    }
                }

                // If we're not EAS 14, add our save-in-sent setting here
                if (!isEas14) {
                    cmd += "&SaveInSent=T";
                }
                userLog("Send cmd: " + cmd);

                // Finally, post SendMail to the server
                EasResponse resp = sendHttpClientPost(cmd, inputEntity, SEND_MAIL_TIMEOUT);
                try {
                    fileStream.close();
                    int code = resp.getStatus();
                    if (code == HttpStatus.SC_OK) {
                        // HTTP OK before EAS 14 is a thumbs up; in EAS 14, we've got to parse
                        // the reply
                        if (isEas14) {
                            try {
                                // Try to parse the result
                                SendMailParser p =
                                    new SendMailParser(resp.getInputStream(), modeTag);
                                // If we get here, the SendMail failed; go figure
                                p.parse();
                                // The parser holds the status
                                int status = p.getStatus();
                                userLog("SendMail error, status: " + status);
                                if (CommandStatus.isNeedsProvisioning(status)) {
                                    result = EmailServiceStatus.SECURITY_FAILURE;
                                } else if (status == CommandStatus.ITEM_NOT_FOUND && smartSend) {
                                    // This is the retry case for EAS 14; we'll send without "smart"
                                    // commands next time
                                    resp.close();
                                    smartSend = false;
                                    continue;
                                }
                                sendFailed(msgId, result);
                                return result;
                            } catch (EmptyStreamException e) {
                                // This is actually fine; an empty stream means SendMail succeeded
                            }
                        }

                        // If we're here, the SendMail command succeeded
                        userLog("Deleting message...");
                        // Delete the message from the Outbox and send callback
                        mContentResolver.delete(
                                ContentUris.withAppendedId(Message.CONTENT_URI, msgId), null, null);
                        sendCallback(-1, msg.mSubject, EmailServiceStatus.SUCCESS);
                        break;
                    } else if (code == EasSyncService.INTERNAL_SERVER_ERROR_CODE && smartSend) {
                        // This is the retry case for EAS 12.1 and below; we'll send without "smart"
                        // commands next time
                        resp.close();
                        smartSend = false;
                    } else {
                        userLog("Message sending failed, code: " + code);
                        if (EasResponse.isAuthError(code)) {
                            result = EmailServiceStatus.LOGIN_FAILED;
                        } else if (EasResponse.isProvisionError(code)) {
                            result = EmailServiceStatus.SECURITY_FAILURE;
                        }
                        sendFailed(msgId, result);
                        break;
                    }
                } finally {
                    resp.close();
                }
            }
        } catch (IOException e) {
            // We catch this just to send the callback
            sendCallback(msgId, null, EmailServiceStatus.CONNECTION_ERROR);
            throw e;
        } finally {
            // Clean up the temporary file
            if (tmpFile.exists()) {
                tmpFile.delete();
            }
        }
        return result;
    }

    @Override
    public void run() {
        setupService();
        // Use SMTP flags for sending mail
        TrafficStats.setThreadStatsTag(TrafficFlags.getSmtpFlags(mContext, mAccount));
        File cacheDir = mContext.getCacheDir();
        try {
            mDeviceId = ExchangeService.getDeviceId(mContext);
            // Get a cursor to Outbox messages
            Cursor c = mContext.getContentResolver().query(Message.CONTENT_URI,
                    Message.ID_COLUMN_PROJECTION, MAILBOX_KEY_AND_NOT_SEND_FAILED,
                    new String[] {Long.toString(mMailbox.mId)}, null);
            try {
                // Loop through the messages, sending each one
                while (c.moveToNext()) {
                    long msgId = c.getLong(Message.ID_COLUMNS_ID_COLUMN);
                    if (msgId != 0) {
                        if (Utility.hasUnloadedAttachments(mContext, msgId)) {
                            // We'll just have to wait on this...
                            continue;
                        }
                        int result = sendMessage(cacheDir, msgId);
                        // If there's an error, it should stop the service; we will distinguish
                        // at least between login failures and everything else
                        if (result == EmailServiceStatus.LOGIN_FAILED) {
                            mExitStatus = EXIT_LOGIN_FAILURE;
                            return;
                        } else if (result == EmailServiceStatus.SECURITY_FAILURE) {
                            mExitStatus = EXIT_SECURITY_FAILURE;
                            return;
                        } else if (result == EmailServiceStatus.REMOTE_EXCEPTION) {
                            mExitStatus = EXIT_EXCEPTION;
                            return;
                        }
                    }
                }
            } finally {
                c.close();
            }
            mExitStatus = EXIT_DONE;
        } catch (IOException e) {
            mExitStatus = EXIT_IO_ERROR;
        } catch (Exception e) {
            userLog("Exception caught in EasOutboxService", e);
            mExitStatus = EXIT_EXCEPTION;
        } finally {
            userLog(mMailbox.mDisplayName, ": sync finished");
            userLog("Outbox exited with status ", mExitStatus);
            ExchangeService.done(this);
        }
    }

    /**
     * Convenience method for adding a Message to an account's outbox
     * @param context the context of the caller
     * @param accountId the accountId for the sending account
     * @param msg the message to send
     */
    public static void sendMessage(Context context, long accountId, Message msg) {
        Mailbox mailbox = Mailbox.restoreMailboxOfType(context, accountId, Mailbox.TYPE_OUTBOX);
        if (mailbox != null) {
            msg.mMailboxKey = mailbox.mId;
            msg.mAccountKey = accountId;
            msg.save(context);
        }
    }
}