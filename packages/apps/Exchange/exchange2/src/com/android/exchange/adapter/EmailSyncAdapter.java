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

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.os.TransactionTooLargeException;
import android.provider.CalendarContract.Events;
import android.text.Html;
import android.text.SpannedString;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.android.emailcommon.internet.MimeMessage;
import com.android.emailcommon.internet.MimeUtility;
import com.android.emailcommon.mail.Address;
import com.android.emailcommon.mail.MeetingInfo;
import com.android.emailcommon.mail.MessagingException;
import com.android.emailcommon.mail.PackedString;
import com.android.emailcommon.mail.Part;
import com.android.emailcommon.provider.Account;
import com.android.emailcommon.provider.EmailContent;
import com.android.emailcommon.provider.EmailContent.AccountColumns;
import com.android.emailcommon.provider.EmailContent.Attachment;
import com.android.emailcommon.provider.EmailContent.Body;
import com.android.emailcommon.provider.EmailContent.MailboxColumns;
import com.android.emailcommon.provider.EmailContent.Message;
import com.android.emailcommon.provider.EmailContent.MessageColumns;
import com.android.emailcommon.provider.EmailContent.SyncColumns;
import com.android.emailcommon.provider.Mailbox;
import com.android.emailcommon.provider.Policy;
import com.android.emailcommon.provider.ProviderUnavailableException;
import com.android.emailcommon.service.SyncWindow;
import com.android.emailcommon.utility.AttachmentUtilities;
import com.android.emailcommon.utility.ConversionUtilities;
import com.android.emailcommon.utility.TextUtilities;
import com.android.emailcommon.utility.Utility;
import com.android.exchange.CommandStatusException;
import com.android.exchange.Eas;
import com.android.exchange.EasResponse;
import com.android.exchange.EasSyncService;
import com.android.exchange.MessageMoveRequest;
import com.android.exchange.R;
import com.android.exchange.utility.CalendarUtilities;
import com.google.common.annotations.VisibleForTesting;

import org.apache.http.HttpStatus;
import org.apache.http.entity.ByteArrayEntity;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

/**
 * Sync adapter for EAS email
 *
 */
public class EmailSyncAdapter extends AbstractSyncAdapter {

    private static final String TAG = "EmailSyncAdapter";

    private static final int UPDATES_READ_COLUMN = 0;
    private static final int UPDATES_MAILBOX_KEY_COLUMN = 1;
    private static final int UPDATES_SERVER_ID_COLUMN = 2;
    private static final int UPDATES_FLAG_COLUMN = 3;
    private static final String[] UPDATES_PROJECTION =
        {MessageColumns.FLAG_READ, MessageColumns.MAILBOX_KEY, SyncColumns.SERVER_ID,
            MessageColumns.FLAG_FAVORITE};

    private static final int MESSAGE_ID_SUBJECT_ID_COLUMN = 0;
    private static final int MESSAGE_ID_SUBJECT_SUBJECT_COLUMN = 1;
    private static final String[] MESSAGE_ID_SUBJECT_PROJECTION =
        new String[] { Message.RECORD_ID, MessageColumns.SUBJECT };

    private static final String WHERE_BODY_SOURCE_MESSAGE_KEY = Body.SOURCE_MESSAGE_KEY + "=?";
    private static final String WHERE_MAILBOX_KEY_AND_MOVED =
        MessageColumns.MAILBOX_KEY + "=? AND (" + MessageColumns.FLAGS + "&" +
        EasSyncService.MESSAGE_FLAG_MOVED_MESSAGE + ")!=0";
    private static final String[] FETCH_REQUEST_PROJECTION =
        new String[] {EmailContent.RECORD_ID, SyncColumns.SERVER_ID};
    private static final int FETCH_REQUEST_RECORD_ID = 0;
    private static final int FETCH_REQUEST_SERVER_ID = 1;

    private static final String EMAIL_WINDOW_SIZE = "5";

    private static final int MAX_NUM_FETCH_SIZE_REDUCTIONS = 5;

    @VisibleForTesting
    static final int LAST_VERB_REPLY = 1;
    @VisibleForTesting
    static final int LAST_VERB_REPLY_ALL = 2;
    @VisibleForTesting
    static final int LAST_VERB_FORWARD = 3;

    private final String[] mBindArguments = new String[2];
    private final String[] mBindArgument = new String[1];

    @VisibleForTesting
    ArrayList<Long> mDeletedIdList = new ArrayList<Long>();
    @VisibleForTesting
    ArrayList<Long> mUpdatedIdList = new ArrayList<Long>();
    private final ArrayList<FetchRequest> mFetchRequestList = new ArrayList<FetchRequest>();
    private boolean mFetchNeeded = false;

    // Holds the parser's value for isLooping()
    private boolean mIsLooping = false;

    // The policy (if any) for this adapter's Account
    private final Policy mPolicy;

    public EmailSyncAdapter(EasSyncService service) {
        super(service);
        // If we've got an account with a policy, cache it now
        if (mAccount.mPolicyKey != 0) {
            mPolicy = Policy.restorePolicyWithId(mContext, mAccount.mPolicyKey);
        } else {
            mPolicy = null;
        }
    }

    @Override
    public void wipe() {
        mContentResolver.delete(Message.CONTENT_URI,
                Message.MAILBOX_KEY + "=" + mMailbox.mId, null);
        mContentResolver.delete(Message.DELETED_CONTENT_URI,
                Message.MAILBOX_KEY + "=" + mMailbox.mId, null);
        mContentResolver.delete(Message.UPDATED_CONTENT_URI,
                Message.MAILBOX_KEY + "=" + mMailbox.mId, null);
        mService.clearRequests();
        mFetchRequestList.clear();
        // Delete attachments...
        AttachmentUtilities.deleteAllMailboxAttachmentFiles(mContext, mAccount.mId, mMailbox.mId);
    }

    private String getEmailFilter() {
        int syncLookback = mMailbox.mSyncLookback;
        if (syncLookback == SyncWindow.SYNC_WINDOW_UNKNOWN
                || mMailbox.mType == Mailbox.TYPE_INBOX) {
            syncLookback = mAccount.mSyncLookback;
        }
        switch (syncLookback) {
            case SyncWindow.SYNC_WINDOW_AUTO:
                return Eas.FILTER_AUTO;
            case SyncWindow.SYNC_WINDOW_1_DAY:
                return Eas.FILTER_1_DAY;
            case SyncWindow.SYNC_WINDOW_3_DAYS:
                return Eas.FILTER_3_DAYS;
            case SyncWindow.SYNC_WINDOW_1_WEEK:
                return Eas.FILTER_1_WEEK;
            case SyncWindow.SYNC_WINDOW_2_WEEKS:
                return Eas.FILTER_2_WEEKS;
            case SyncWindow.SYNC_WINDOW_1_MONTH:
                return Eas.FILTER_1_MONTH;
            case SyncWindow.SYNC_WINDOW_ALL:
                return Eas.FILTER_ALL;
            default:
                return Eas.FILTER_1_WEEK;
        }
    }

    /**
     * Holder for fetch request information (record id and server id)
     */
    private static class FetchRequest {
        @SuppressWarnings("unused")
        final long messageId;
        final String serverId;

        FetchRequest(long _messageId, String _serverId) {
            messageId = _messageId;
            serverId = _serverId;
        }
    }

    @Override
    public void sendSyncOptions(Double protocolVersion, Serializer s, boolean initialSync)
            throws IOException  {
        if (initialSync) return;
        mFetchRequestList.clear();
        // Find partially loaded messages; this should typically be a rare occurrence
        Cursor c = mContext.getContentResolver().query(Message.CONTENT_URI,
                FETCH_REQUEST_PROJECTION,
                MessageColumns.FLAG_LOADED + "=" + Message.FLAG_LOADED_PARTIAL + " AND " +
                MessageColumns.MAILBOX_KEY + "=?",
                new String[] {Long.toString(mMailbox.mId)}, null);
        try {
            // Put all of these messages into a list; we'll need both id and server id
            while (c.moveToNext()) {
                mFetchRequestList.add(new FetchRequest(c.getLong(FETCH_REQUEST_RECORD_ID),
                        c.getString(FETCH_REQUEST_SERVER_ID)));
            }
        } finally {
            c.close();
        }

        // The "empty" case is typical; we send a request for changes, and also specify a sync
        // window, body preference type (HTML for EAS 12.0 and later; MIME for EAS 2.5), and
        // truncation
        // If there are fetch requests, we only want the fetches (i.e. no changes from the server)
        // so we turn MIME support off.  Note that we are always using EAS 2.5 if there are fetch
        // requests
        if (mFetchRequestList.isEmpty()) {
            // Permanently delete if in trash mailbox
            // In Exchange 2003, deletes-as-moves tag = true; no tag = false
            // In Exchange 2007 and up, deletes-as-moves tag is "0" (false) or "1" (true)
            boolean isTrashMailbox = mMailbox.mType == Mailbox.TYPE_TRASH;
            if (protocolVersion < Eas.SUPPORTED_PROTOCOL_EX2007_DOUBLE) {
                if (!isTrashMailbox) {
                    s.tag(Tags.SYNC_DELETES_AS_MOVES);
                }
            } else {
                s.data(Tags.SYNC_DELETES_AS_MOVES, isTrashMailbox ? "0" : "1");
            }
            s.tag(Tags.SYNC_GET_CHANGES);
            s.data(Tags.SYNC_WINDOW_SIZE, EMAIL_WINDOW_SIZE);
            s.start(Tags.SYNC_OPTIONS);
            // Set the lookback appropriately (EAS calls this a "filter")
            String filter = getEmailFilter();
            // We shouldn't get FILTER_AUTO here, but if we do, make it something legal...
            if (filter.equals(Eas.FILTER_AUTO)) {
                filter = Eas.FILTER_3_DAYS;
            }
            s.data(Tags.SYNC_FILTER_TYPE, filter);
            // Set the truncation amount for all classes
            if (protocolVersion >= Eas.SUPPORTED_PROTOCOL_EX2007_DOUBLE) {
                s.start(Tags.BASE_BODY_PREFERENCE);
                // HTML for email
                s.data(Tags.BASE_TYPE, Eas.BODY_PREFERENCE_HTML);
                s.data(Tags.BASE_TRUNCATION_SIZE, Eas.EAS12_TRUNCATION_SIZE);
                s.end();
            } else {
                // Use MIME data for EAS 2.5
                s.data(Tags.SYNC_MIME_SUPPORT, Eas.MIME_BODY_PREFERENCE_MIME);
                s.data(Tags.SYNC_MIME_TRUNCATION, Eas.EAS2_5_TRUNCATION_SIZE);
            }
            s.end();
        } else {
            s.start(Tags.SYNC_OPTIONS);
            // Ask for plain text, rather than MIME data.  This guarantees that we'll get a usable
            // text body
            s.data(Tags.SYNC_MIME_SUPPORT, Eas.MIME_BODY_PREFERENCE_TEXT);
            s.data(Tags.SYNC_TRUNCATION, Eas.EAS2_5_TRUNCATION_SIZE);
            s.end();
        }
    }

    @Override
    public boolean parse(InputStream is) throws IOException, CommandStatusException {
        EasEmailSyncParser p = new EasEmailSyncParser(is, this);
        mFetchNeeded = false;
        boolean res = p.parse();
        // Hold on to the parser's value for isLooping() to pass back to the service
        mIsLooping = p.isLooping();
        // If we've need a body fetch, or we've just finished one, return true in order to continue
        if (mFetchNeeded || !mFetchRequestList.isEmpty()) {
            return true;
        }

        // Don't check for "auto" on the initial sync
        if (!("0".equals(mMailbox.mSyncKey))) {
            // We've completed the first successful sync
            if (getEmailFilter().equals(Eas.FILTER_AUTO)) {
                getAutomaticLookback();
             }
        }

        return res;
    }

    private void getAutomaticLookback() throws IOException {
        // If we're using an auto lookback, check how many items in the past week
        // TODO Make the literal ints below constants once we twiddle them a bit
        int items = getEstimate(Eas.FILTER_1_WEEK);
        int lookback;
        if (items > 1050) {
            // Over 150/day, just use one day (smallest)
            lookback = SyncWindow.SYNC_WINDOW_1_DAY;
        } else if (items > 350 || (items == -1)) {
            // 50-150/day, use 3 days (150 to 450 messages synced)
            lookback = SyncWindow.SYNC_WINDOW_3_DAYS;
        } else if (items > 150) {
            // 20-50/day, use 1 week (140 to 350 messages synced)
            lookback = SyncWindow.SYNC_WINDOW_1_WEEK;
        } else if (items > 75) {
            // 10-25/day, use 1 week (140 to 350 messages synced)
            lookback = SyncWindow.SYNC_WINDOW_2_WEEKS;
        } else if (items < 5) {
            // If there are only a couple, see if it makes sense to get everything
            items = getEstimate(Eas.FILTER_ALL);
            if (items >= 0 && items < 100) {
                lookback = SyncWindow.SYNC_WINDOW_ALL;
            } else {
                lookback = SyncWindow.SYNC_WINDOW_1_MONTH;
            }
        } else {
            lookback = SyncWindow.SYNC_WINDOW_1_MONTH;
        }

        // Limit lookback to policy limit
        if (mAccount.mPolicyKey > 0) {
            Policy policy = Policy.restorePolicyWithId(mContext, mAccount.mPolicyKey);
            if (policy != null) {
                int maxLookback = policy.mMaxEmailLookback;
                if (maxLookback != 0 && (lookback > policy.mMaxEmailLookback)) {
                    lookback = policy.mMaxEmailLookback;
                }
            }
        }

        // Store the new lookback and persist it
        // TODO Code similar to this is used elsewhere (e.g. MailboxSettings); try to clean this up
        ContentValues cv = new ContentValues();
        Uri uri;
        if (mMailbox.mType == Mailbox.TYPE_INBOX) {
            mAccount.mSyncLookback = lookback;
            cv.put(AccountColumns.SYNC_LOOKBACK, lookback);
            uri = ContentUris.withAppendedId(Account.CONTENT_URI, mAccount.mId);
        } else {
            mMailbox.mSyncLookback = lookback;
            cv.put(MailboxColumns.SYNC_LOOKBACK, lookback);
            uri = ContentUris.withAppendedId(Mailbox.CONTENT_URI, mMailbox.mId);
        }
        mContentResolver.update(uri, cv, null, null);

        CharSequence[] windowEntries = mContext.getResources().getTextArray(
                R.array.account_settings_mail_window_entries);
        Log.d(TAG, "Auto lookback: " + windowEntries[lookback]);
    }

    private static class GetItemEstimateParser extends Parser {
        @SuppressWarnings("hiding")
        private static final String TAG = "GetItemEstimateParser";
        private int mEstimate = -1;

        public GetItemEstimateParser(InputStream in) throws IOException {
            super(in);
        }

        @Override
        public boolean parse() throws IOException {
            // Loop here through the remaining xml
            while (nextTag(START_DOCUMENT) != END_DOCUMENT) {
                if (tag == Tags.GIE_GET_ITEM_ESTIMATE) {
                    parseGetItemEstimate();
                } else {
                    skipTag();
                }
            }
            return true;
        }

        public void parseGetItemEstimate() throws IOException {
            while (nextTag(Tags.GIE_GET_ITEM_ESTIMATE) != END) {
                if (tag == Tags.GIE_RESPONSE) {
                    parseResponse();
                } else {
                    skipTag();
                }
            }
        }

        public void parseResponse() throws IOException {
            while (nextTag(Tags.GIE_RESPONSE) != END) {
                if (tag == Tags.GIE_STATUS) {
                    Log.d(TAG, "GIE status: " + getValue());
                } else if (tag == Tags.GIE_COLLECTION) {
                    parseCollection();
                } else {
                    skipTag();
                }
            }
        }

        public void parseCollection() throws IOException {
            while (nextTag(Tags.GIE_COLLECTION) != END) {
                if (tag == Tags.GIE_CLASS) {
                    Log.d(TAG, "GIE class: " + getValue());
                } else if (tag == Tags.GIE_COLLECTION_ID) {
                    Log.d(TAG, "GIE collectionId: " + getValue());
                } else if (tag == Tags.GIE_ESTIMATE) {
                    mEstimate = getValueInt();
                    Log.d(TAG, "GIE estimate: " + mEstimate);
                } else {
                    skipTag();
                }
            }
        }
    }

    /**
     * Return the estimated number of items to be synced in the current mailbox, based on the
     * passed in filter argument
     * @param filter an EAS "window" filter
     * @return the estimated number of items to be synced, or -1 if unknown
     * @throws IOException
     */
    private int getEstimate(String filter) throws IOException {
        Serializer s = new Serializer();
        boolean ex10 = mService.mProtocolVersionDouble >= Eas.SUPPORTED_PROTOCOL_EX2010_DOUBLE;
        boolean ex03 = mService.mProtocolVersionDouble < Eas.SUPPORTED_PROTOCOL_EX2007_DOUBLE;
        boolean ex07 = !ex10 && !ex03;

        String className = getCollectionName();
        String syncKey = getSyncKey();
        userLog("gie, sending ", className, " syncKey: ", syncKey);

        s.start(Tags.GIE_GET_ITEM_ESTIMATE).start(Tags.GIE_COLLECTIONS);
        s.start(Tags.GIE_COLLECTION);
        if (ex07) {
            // Exchange 2007 likes collection id first
            s.data(Tags.GIE_COLLECTION_ID, mMailbox.mServerId);
            s.data(Tags.SYNC_FILTER_TYPE, filter);
            s.data(Tags.SYNC_SYNC_KEY, syncKey);
        } else if (ex03) {
            // Exchange 2003 needs the "class" element
            s.data(Tags.GIE_CLASS, className);
            s.data(Tags.SYNC_SYNC_KEY, syncKey);
            s.data(Tags.GIE_COLLECTION_ID, mMailbox.mServerId);
            s.data(Tags.SYNC_FILTER_TYPE, filter);
        } else {
            // Exchange 2010 requires the filter inside an OPTIONS container and sync key first
            s.data(Tags.SYNC_SYNC_KEY, syncKey);
            s.data(Tags.GIE_COLLECTION_ID, mMailbox.mServerId);
            s.start(Tags.SYNC_OPTIONS).data(Tags.SYNC_FILTER_TYPE, filter).end();
        }
        s.end().end().end().done(); // GIE_COLLECTION, GIE_COLLECTIONS, GIE_GET_ITEM_ESTIMATE

        EasResponse resp = mService.sendHttpClientPost("GetItemEstimate",
                new ByteArrayEntity(s.toByteArray()), EasSyncService.COMMAND_TIMEOUT);
        try {
            int code = resp.getStatus();
            if (code == HttpStatus.SC_OK) {
                if (!resp.isEmpty()) {
                    InputStream is = resp.getInputStream();
                    GetItemEstimateParser gieParser = new GetItemEstimateParser(is);
                    gieParser.parse();
                    // Return the estimated number of items
                    return gieParser.mEstimate;
                }
            }
        } finally {
            resp.close();
        }
        // If we can't get an estimate, indicate this...
        return -1;
    }

    /**
     * Return the value of isLooping() as returned from the parser
     */
    @Override
    public boolean isLooping() {
        return mIsLooping;
    }

    @Override
    public boolean isSyncable() {
        return true;
    }

    public class EasEmailSyncParser extends AbstractSyncParser {

        private static final String WHERE_SERVER_ID_AND_MAILBOX_KEY =
            SyncColumns.SERVER_ID + "=? and " + MessageColumns.MAILBOX_KEY + "=?";

        private final String mMailboxIdAsString;

        private final ArrayList<Message> newEmails = new ArrayList<Message>();
        private final ArrayList<Message> fetchedEmails = new ArrayList<Message>();
        private final ArrayList<Long> deletedEmails = new ArrayList<Long>();
        private final ArrayList<ServerChange> changedEmails = new ArrayList<ServerChange>();

        public EasEmailSyncParser(InputStream in, EmailSyncAdapter adapter) throws IOException {
            super(in, adapter);
            mMailboxIdAsString = Long.toString(mMailbox.mId);
        }

        public EasEmailSyncParser(Parser parser, EmailSyncAdapter adapter) throws IOException {
            super(parser, adapter);
            mMailboxIdAsString = Long.toString(mMailbox.mId);
        }

        public void addData (Message msg, int endingTag) throws IOException {
            ArrayList<Attachment> atts = new ArrayList<Attachment>();
            boolean truncated = false;

            while (nextTag(endingTag) != END) {
                switch (tag) {
                    case Tags.EMAIL_ATTACHMENTS:
                    case Tags.BASE_ATTACHMENTS: // BASE_ATTACHMENTS is used in EAS 12.0 and up
                        attachmentsParser(atts, msg);
                        break;
                    case Tags.EMAIL_TO:
                        msg.mTo = Address.pack(Address.parse(getValue()));
                        break;
                    case Tags.EMAIL_FROM:
                        Address[] froms = Address.parse(getValue());
                        if (froms != null && froms.length > 0) {
                            msg.mDisplayName = froms[0].toFriendly();
                        }
                        msg.mFrom = Address.pack(froms);
                        break;
                    case Tags.EMAIL_CC:
                        msg.mCc = Address.pack(Address.parse(getValue()));
                        break;
                    case Tags.EMAIL_REPLY_TO:
                        msg.mReplyTo = Address.pack(Address.parse(getValue()));
                        break;
                    case Tags.EMAIL_DATE_RECEIVED:
                        msg.mTimeStamp = Utility.parseEmailDateTimeToMillis(getValue());
                        break;
                    case Tags.EMAIL_SUBJECT:
                        msg.mSubject = getValue();
                        break;
                    case Tags.EMAIL_READ:
                        msg.mFlagRead = getValueInt() == 1;
                        break;
                    case Tags.BASE_BODY:
                        bodyParser(msg);
                        break;
                    case Tags.EMAIL_FLAG:
                        msg.mFlagFavorite = flagParser();
                        break;
                    case Tags.EMAIL_MIME_TRUNCATED:
                        truncated = getValueInt() == 1;
                        break;
                    case Tags.EMAIL_MIME_DATA:
                        // We get MIME data for EAS 2.5.  First we parse it, then we take the
                        // html and/or plain text data and store it in the message
                        if (truncated) {
                            // If the MIME data is truncated, don't bother parsing it, because
                            // it will take time and throw an exception anyway when EOF is reached
                            // In this case, we will load the body separately by tagging the message
                            // "partially loaded".
                            // Get the data (and ignore it)
                            getValue();
                            userLog("Partially loaded: ", msg.mServerId);
                            msg.mFlagLoaded = Message.FLAG_LOADED_PARTIAL;
                            mFetchNeeded = true;
                        } else {
                            mimeBodyParser(msg, getValue());
                        }
                        break;
                    case Tags.EMAIL_BODY:
                        String text = getValue();
                        msg.mText = text;
                        break;
                    case Tags.EMAIL_MESSAGE_CLASS:
                        String messageClass = getValue();
                        if (messageClass.equals("IPM.Schedule.Meeting.Request")) {
                            msg.mFlags |= Message.FLAG_INCOMING_MEETING_INVITE;
                        } else if (messageClass.equals("IPM.Schedule.Meeting.Canceled")) {
                            msg.mFlags |= Message.FLAG_INCOMING_MEETING_CANCEL;
                        }
                        break;
                    case Tags.EMAIL_MEETING_REQUEST:
                        meetingRequestParser(msg);
                        break;
                    case Tags.EMAIL_THREAD_TOPIC:
                        msg.mThreadTopic = getValue();
                        break;
                    case Tags.RIGHTS_LICENSE:
                        skipParser(tag);
                        break;
                    case Tags.EMAIL2_CONVERSATION_ID:
                        msg.mServerConversationId =
                                Base64.encodeToString(getValueBytes(), Base64.URL_SAFE);
                        break;
                    case Tags.EMAIL2_CONVERSATION_INDEX:
                        // Ignore this byte array since we're not constructing a tree.
                        getValueBytes();
                        break;
                    case Tags.EMAIL2_LAST_VERB_EXECUTED:
                        int val = getValueInt();
                        if (val == LAST_VERB_REPLY || val == LAST_VERB_REPLY_ALL) {
                            // We aren't required to distinguish between reply and reply all here
                            msg.mFlags |= Message.FLAG_REPLIED_TO;
                        } else if (val == LAST_VERB_FORWARD) {
                            msg.mFlags |= Message.FLAG_FORWARDED;
                        }
                        break;
                    default:
                        skipTag();
                }
            }

            if (atts.size() > 0) {
                msg.mAttachments = atts;
            }

            if ((msg.mFlags & Message.FLAG_INCOMING_MEETING_MASK) != 0) {
                String text = TextUtilities.makeSnippetFromHtmlText(
                        msg.mText != null ? msg.mText : msg.mHtml);
                if (TextUtils.isEmpty(text)) {
                    // Create text for this invitation
                    String meetingInfo = msg.mMeetingInfo;
                    if (!TextUtils.isEmpty(meetingInfo)) {
                        PackedString ps = new PackedString(meetingInfo);
                        ContentValues values = new ContentValues();
                        putFromMeeting(ps, MeetingInfo.MEETING_LOCATION, values,
                                Events.EVENT_LOCATION);
                        String dtstart = ps.get(MeetingInfo.MEETING_DTSTART);
                        if (!TextUtils.isEmpty(dtstart)) {
                            long startTime = Utility.parseEmailDateTimeToMillis(dtstart);
                            values.put(Events.DTSTART, startTime);
                        }
                        putFromMeeting(ps, MeetingInfo.MEETING_ALL_DAY, values,
                                Events.ALL_DAY);
                        msg.mText = CalendarUtilities.buildMessageTextFromEntityValues(
                                mContext, values, null);
                        msg.mHtml = Html.toHtml(new SpannedString(msg.mText));
                    }
                }
            }
        }

        private void putFromMeeting(PackedString ps, String field, ContentValues values,
                String column) {
            String val = ps.get(field);
            if (!TextUtils.isEmpty(val)) {
                values.put(column, val);
            }
        }

        /**
         * Set up the meetingInfo field in the message with various pieces of information gleaned
         * from MeetingRequest tags.  This information will be used later to generate an appropriate
         * reply email if the user chooses to respond
         * @param msg the Message being built
         * @throws IOException
         */
        private void meetingRequestParser(Message msg) throws IOException {
            PackedString.Builder packedString = new PackedString.Builder();
            while (nextTag(Tags.EMAIL_MEETING_REQUEST) != END) {
                switch (tag) {
                    case Tags.EMAIL_DTSTAMP:
                        packedString.put(MeetingInfo.MEETING_DTSTAMP, getValue());
                        break;
                    case Tags.EMAIL_START_TIME:
                        packedString.put(MeetingInfo.MEETING_DTSTART, getValue());
                        break;
                    case Tags.EMAIL_END_TIME:
                        packedString.put(MeetingInfo.MEETING_DTEND, getValue());
                        break;
                    case Tags.EMAIL_ORGANIZER:
                        packedString.put(MeetingInfo.MEETING_ORGANIZER_EMAIL, getValue());
                        break;
                    case Tags.EMAIL_LOCATION:
                        packedString.put(MeetingInfo.MEETING_LOCATION, getValue());
                        break;
                    case Tags.EMAIL_GLOBAL_OBJID:
                        packedString.put(MeetingInfo.MEETING_UID,
                                CalendarUtilities.getUidFromGlobalObjId(getValue()));
                        break;
                    case Tags.EMAIL_CATEGORIES:
                        skipParser(tag);
                        break;
                    case Tags.EMAIL_RECURRENCES:
                        recurrencesParser();
                        break;
                    case Tags.EMAIL_RESPONSE_REQUESTED:
                        packedString.put(MeetingInfo.MEETING_RESPONSE_REQUESTED, getValue());
                        break;
                    case Tags.EMAIL_ALL_DAY_EVENT:
                        if (getValueInt() == 1) {
                            packedString.put(MeetingInfo.MEETING_ALL_DAY, "1");
                        }
                        break;
                    default:
                        skipTag();
                }
            }
            if (msg.mSubject != null) {
                packedString.put(MeetingInfo.MEETING_TITLE, msg.mSubject);
            }
            msg.mMeetingInfo = packedString.toString();
        }

        private void recurrencesParser() throws IOException {
            while (nextTag(Tags.EMAIL_RECURRENCES) != END) {
                switch (tag) {
                    case Tags.EMAIL_RECURRENCE:
                        skipParser(tag);
                        break;
                    default:
                        skipTag();
                }
            }
        }

        /**
         * Parse a message from the server stream.
         * @return the parsed Message
         * @throws IOException
         */
        private Message addParser() throws IOException, CommandStatusException {
            Message msg = new Message();
            msg.mAccountKey = mAccount.mId;
            msg.mMailboxKey = mMailbox.mId;
            msg.mFlagLoaded = Message.FLAG_LOADED_COMPLETE;
            // Default to 1 (success) in case we don't get this tag
            int status = 1;

            while (nextTag(Tags.SYNC_ADD) != END) {
                switch (tag) {
                    case Tags.SYNC_SERVER_ID:
                        msg.mServerId = getValue();
                        break;
                    case Tags.SYNC_STATUS:
                        status = getValueInt();
                        break;
                    case Tags.SYNC_APPLICATION_DATA:
                        addData(msg, tag);
                        break;
                    default:
                        skipTag();
                }
            }
            // For sync, status 1 = success
            if (status != 1) {
                throw new CommandStatusException(status, msg.mServerId);
            }
            return msg;
        }

        // For now, we only care about the "active" state
        private Boolean flagParser() throws IOException {
            Boolean state = false;
            while (nextTag(Tags.EMAIL_FLAG) != END) {
                switch (tag) {
                    case Tags.EMAIL_FLAG_STATUS:
                        state = getValueInt() == 2;
                        break;
                    default:
                        skipTag();
                }
            }
            return state;
        }

        private void bodyParser(Message msg) throws IOException {
            String bodyType = Eas.BODY_PREFERENCE_TEXT;
            String body = "";
            while (nextTag(Tags.EMAIL_BODY) != END) {
                switch (tag) {
                    case Tags.BASE_TYPE:
                        bodyType = getValue();
                        break;
                    case Tags.BASE_DATA:
                        body = getValue();
                        break;
                    default:
                        skipTag();
                }
            }
            // We always ask for TEXT or HTML; there's no third option
            if (bodyType.equals(Eas.BODY_PREFERENCE_HTML)) {
                msg.mHtml = body;
            } else {
                msg.mText = body;
            }
        }

        /**
         * Parses untruncated MIME data, saving away the text parts
         * @param msg the message we're building
         * @param mimeData the MIME data we've received from the server
         * @throws IOException
         */
        private void mimeBodyParser(Message msg, String mimeData) throws IOException {
            try {
                ByteArrayInputStream in = new ByteArrayInputStream(mimeData.getBytes());
                // The constructor parses the message
                MimeMessage mimeMessage = new MimeMessage(in);
                // Now process body parts & attachments
                ArrayList<Part> viewables = new ArrayList<Part>();
                // We'll ignore the attachments, as we'll get them directly from EAS
                ArrayList<Part> attachments = new ArrayList<Part>();
                MimeUtility.collectParts(mimeMessage, viewables, attachments);
                Body tempBody = new Body();
                // updateBodyFields fills in the content fields of the Body
                ConversionUtilities.updateBodyFields(tempBody, msg, viewables);
                // But we need them in the message itself for handling during commit()
                msg.mHtml = tempBody.mHtmlContent;
                msg.mText = tempBody.mTextContent;
            } catch (MessagingException e) {
                // This would most likely indicate a broken stream
                throw new IOException(e);
            }
        }

        private void attachmentsParser(ArrayList<Attachment> atts, Message msg) throws IOException {
            while (nextTag(Tags.EMAIL_ATTACHMENTS) != END) {
                switch (tag) {
                    case Tags.EMAIL_ATTACHMENT:
                    case Tags.BASE_ATTACHMENT:  // BASE_ATTACHMENT is used in EAS 12.0 and up
                        attachmentParser(atts, msg);
                        break;
                    default:
                        skipTag();
                }
            }
        }

        private void attachmentParser(ArrayList<Attachment> atts, Message msg) throws IOException {
            String fileName = null;
            String length = null;
            String location = null;
            boolean isInline = false;
            String contentId = null;

            while (nextTag(Tags.EMAIL_ATTACHMENT) != END) {
                switch (tag) {
                    // We handle both EAS 2.5 and 12.0+ attachments here
                    case Tags.EMAIL_DISPLAY_NAME:
                    case Tags.BASE_DISPLAY_NAME:
                        fileName = getValue();
                        break;
                    case Tags.EMAIL_ATT_NAME:
                    case Tags.BASE_FILE_REFERENCE:
                        location = getValue();
                        break;
                    case Tags.EMAIL_ATT_SIZE:
                    case Tags.BASE_ESTIMATED_DATA_SIZE:
                        length = getValue();
                        break;
                    case Tags.BASE_IS_INLINE:
                        isInline = getValueInt() == 1;
                        break;
                    case Tags.BASE_CONTENT_ID:
                        contentId = getValue();
                        break;
                    default:
                        skipTag();
                }
            }

            if ((fileName != null) && (length != null) && (location != null)) {
                Attachment att = new Attachment();
                att.mEncoding = "base64";
                att.mSize = Long.parseLong(length);
                att.mFileName = fileName;
                att.mLocation = location;
                att.mMimeType = getMimeTypeFromFileName(fileName);
                att.mAccountKey = mService.mAccount.mId;
                // Save away the contentId, if we've got one (for inline images); note that the
                // EAS docs appear to be wrong about the tags used; inline images come with
                // contentId rather than contentLocation, when sent from Ex03, Ex07, and Ex10
                if (isInline && !TextUtils.isEmpty(contentId)) {
                    att.mContentId = contentId;
                }
                // Check if this attachment can't be downloaded due to an account policy
                if (mPolicy != null) {
                    if (mPolicy.mDontAllowAttachments ||
                            (mPolicy.mMaxAttachmentSize > 0 &&
                                    (att.mSize > mPolicy.mMaxAttachmentSize))) {
                        att.mFlags = Attachment.FLAG_POLICY_DISALLOWS_DOWNLOAD;
                    }
                }
                atts.add(att);
                msg.mFlagAttachment = true;
            }
        }

        /**
         * Returns an appropriate mimetype for the given file name's extension. If a mimetype
         * cannot be determined, {@code application/<<x>>} [where @{code <<x>> is the extension,
         * if it exists or {@code application/octet-stream}].
         * At the moment, this is somewhat lame, since many file types aren't recognized
         * @param fileName the file name to ponder
         */
        // Note: The MimeTypeMap method currently uses a very limited set of mime types
        // A bug has been filed against this issue.
        public String getMimeTypeFromFileName(String fileName) {
            String mimeType;
            int lastDot = fileName.lastIndexOf('.');
            String extension = null;
            if ((lastDot > 0) && (lastDot < fileName.length() - 1)) {
                extension = fileName.substring(lastDot + 1).toLowerCase();
            }
            if (extension == null) {
                // A reasonable default for now.
                mimeType = "application/octet-stream";
            } else {
                mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
                if (mimeType == null) {
                    mimeType = "application/" + extension;
                }
            }
            return mimeType;
        }

        private Cursor getServerIdCursor(String serverId, String[] projection) {
            mBindArguments[0] = serverId;
            mBindArguments[1] = mMailboxIdAsString;
            Cursor c = mContentResolver.query(Message.CONTENT_URI, projection,
                    WHERE_SERVER_ID_AND_MAILBOX_KEY, mBindArguments, null);
            if (c == null) throw new ProviderUnavailableException();
            if (c.getCount() > 1) {
                userLog("Multiple messages with the same serverId/mailbox: " + serverId);
            }
            return c;
        }

        @VisibleForTesting
        void deleteParser(ArrayList<Long> deletes, int entryTag) throws IOException {
            while (nextTag(entryTag) != END) {
                switch (tag) {
                    case Tags.SYNC_SERVER_ID:
                        String serverId = getValue();
                        // Find the message in this mailbox with the given serverId
                        Cursor c = getServerIdCursor(serverId, MESSAGE_ID_SUBJECT_PROJECTION);
                        try {
                            if (c.moveToFirst()) {
                                deletes.add(c.getLong(MESSAGE_ID_SUBJECT_ID_COLUMN));
                                if (Eas.USER_LOG) {
                                    userLog("Deleting ", serverId + ", "
                                            + c.getString(MESSAGE_ID_SUBJECT_SUBJECT_COLUMN));
                                }
                            }
                        } finally {
                            c.close();
                        }
                        break;
                    default:
                        skipTag();
                }
            }
        }

        @VisibleForTesting
        class ServerChange {
            final long id;
            final Boolean read;
            final Boolean flag;
            final Integer flags;

            ServerChange(long _id, Boolean _read, Boolean _flag, Integer _flags) {
                id = _id;
                read = _read;
                flag = _flag;
                flags = _flags;
            }
        }

        @VisibleForTesting
        void changeParser(ArrayList<ServerChange> changes) throws IOException {
            String serverId = null;
            Boolean oldRead = false;
            Boolean oldFlag = false;
            int flags = 0;
            long id = 0;
            while (nextTag(Tags.SYNC_CHANGE) != END) {
                switch (tag) {
                    case Tags.SYNC_SERVER_ID:
                        serverId = getValue();
                        Cursor c = getServerIdCursor(serverId, Message.LIST_PROJECTION);
                        try {
                            if (c.moveToFirst()) {
                                userLog("Changing ", serverId);
                                oldRead = c.getInt(Message.LIST_READ_COLUMN) == Message.READ;
                                oldFlag = c.getInt(Message.LIST_FAVORITE_COLUMN) == 1;
                                flags = c.getInt(Message.LIST_FLAGS_COLUMN);
                                id = c.getLong(Message.LIST_ID_COLUMN);
                            }
                        } finally {
                            c.close();
                        }
                        break;
                    case Tags.SYNC_APPLICATION_DATA:
                        changeApplicationDataParser(changes, oldRead, oldFlag, flags, id);
                        break;
                    default:
                        skipTag();
                }
            }
        }

        private void changeApplicationDataParser(ArrayList<ServerChange> changes, Boolean oldRead,
                Boolean oldFlag, int oldFlags, long id) throws IOException {
            Boolean read = null;
            Boolean flag = null;
            Integer flags = null;
            while (nextTag(Tags.SYNC_APPLICATION_DATA) != END) {
                switch (tag) {
                    case Tags.EMAIL_READ:
                        read = getValueInt() == 1;
                        break;
                    case Tags.EMAIL_FLAG:
                        flag = flagParser();
                        break;
                    case Tags.EMAIL2_LAST_VERB_EXECUTED:
                        int val = getValueInt();
                        // Clear out the old replied/forward flags and add in the new flag
                        flags = oldFlags & ~(Message.FLAG_REPLIED_TO | Message.FLAG_FORWARDED);
                        if (val == LAST_VERB_REPLY || val == LAST_VERB_REPLY_ALL) {
                            // We aren't required to distinguish between reply and reply all here
                            flags |= Message.FLAG_REPLIED_TO;
                        } else if (val == LAST_VERB_FORWARD) {
                            flags |= Message.FLAG_FORWARDED;
                        }
                        break;
                    default:
                        skipTag();
                }
            }
            // See if there are flag changes re: read, flag (favorite) or replied/forwarded
            if (((read != null) && !oldRead.equals(read)) ||
                    ((flag != null) && !oldFlag.equals(flag)) || (flags != null)) {
                changes.add(new ServerChange(id, read, flag, flags));
            }
        }

        /* (non-Javadoc)
         * @see com.android.exchange.adapter.EasContentParser#commandsParser()
         */
        @Override
        public void commandsParser() throws IOException, CommandStatusException {
            while (nextTag(Tags.SYNC_COMMANDS) != END) {
                if (tag == Tags.SYNC_ADD) {
                    newEmails.add(addParser());
                    incrementChangeCount();
                } else if (tag == Tags.SYNC_DELETE || tag == Tags.SYNC_SOFT_DELETE) {
                    deleteParser(deletedEmails, tag);
                    incrementChangeCount();
                } else if (tag == Tags.SYNC_CHANGE) {
                    changeParser(changedEmails);
                    incrementChangeCount();
                } else
                    skipTag();
            }
        }

        /**
         * Removed any messages with status 7 (mismatch) from the updatedIdList
         * @param endTag the tag we end with
         * @throws IOException
         */
        public void failedUpdateParser(int endTag) throws IOException {
            // We get serverId and status in the responses
            String serverId = null;
            while (nextTag(endTag) != END) {
                if (tag == Tags.SYNC_STATUS) {
                    int status = getValueInt();
                    if (status == 7 && serverId != null) {
                        Cursor c = getServerIdCursor(serverId, Message.ID_COLUMN_PROJECTION);
                        try {
                            if (c.moveToFirst()) {
                                Long id = c.getLong(Message.ID_PROJECTION_COLUMN);
                                mService.userLog("Update of " + serverId + " failed; will retry");
                                mUpdatedIdList.remove(id);
                                mService.mUpsyncFailed = true;
                            }
                        } finally {
                            c.close();
                        }
                    }
                } else if (tag == Tags.SYNC_SERVER_ID) {
                    serverId = getValue();
                } else {
                    skipTag();
                }
            }
        }

        @Override
        public void responsesParser() throws IOException {
            while (nextTag(Tags.SYNC_RESPONSES) != END) {
                if (tag == Tags.SYNC_ADD || tag == Tags.SYNC_CHANGE || tag == Tags.SYNC_DELETE) {
                    failedUpdateParser(tag);
                } else if (tag == Tags.SYNC_FETCH) {
                    try {
                        fetchedEmails.add(addParser());
                    } catch (CommandStatusException sse) {
                        if (sse.mStatus == 8) {
                            // 8 = object not found; delete the message from EmailProvider
                            // No other status should be seen in a fetch response, except, perhaps,
                            // for some temporary server failure
                            mBindArguments[0] = sse.mItemId;
                            mBindArguments[1] = mMailboxIdAsString;
                            mContentResolver.delete(Message.CONTENT_URI,
                                    WHERE_SERVER_ID_AND_MAILBOX_KEY, mBindArguments);
                        }
                    }
                }
            }
        }

        @Override
        public void commit() {
            commitImpl(0);
        }

        public void commitImpl(final int tryCount) {
            // Use a batch operation to handle the changes
            final ArrayList<ContentProviderOperation> ops =
                    new ArrayList<ContentProviderOperation>();

            // Maximum size of message text per fetch
            int numFetched = fetchedEmails.size();
            int maxPerFetch = 0;
            if (numFetched > 0 && tryCount > 0) {
                // Educated guess that 450000 chars (900k) is ok; 600k is a killer
                // Remember that when fetching, we're not getting any other data
                // We'll keep trying, reducing the maximum each time
                // Realistically, this will rarely exceed 1, and probably never 2
                maxPerFetch = 450000 / numFetched / tryCount;
            }
            for (Message msg: fetchedEmails) {
                // Find the original message's id (by serverId and mailbox)
                Cursor c = getServerIdCursor(msg.mServerId, EmailContent.ID_PROJECTION);
                String id = null;
                try {
                    if (c.moveToFirst()) {
                        id = c.getString(EmailContent.ID_PROJECTION_COLUMN);
                        while (c.moveToNext()) {
                            // This shouldn't happen, but clean up if it does
                            Long dupId =
                                    Long.parseLong(c.getString(EmailContent.ID_PROJECTION_COLUMN));
                            userLog("Delete duplicate with id: " + dupId);
                            deletedEmails.add(dupId);
                        }
                    }
                } finally {
                    c.close();
                }

                // If we find one, we do two things atomically: 1) set the body text for the
                // message, and 2) mark the message loaded (i.e. completely loaded)
                if (id != null) {
                    mBindArgument[0] = id;
                    if (msg.mText != null && (maxPerFetch > 0) &&
                            (msg.mText.length() > maxPerFetch)) {
                        userLog("Truncating TEXT to " + maxPerFetch);
                        msg.mText = msg.mText.substring(0, maxPerFetch) + "...";
                    }
                    if (msg.mHtml != null && (maxPerFetch > 0) &&
                            (msg.mHtml.length() > maxPerFetch)) {
                        userLog("Truncating HTML to " + maxPerFetch);
                        msg.mHtml = msg.mHtml.substring(0, maxPerFetch) + "...";
                    }
                    ops.add(ContentProviderOperation.newUpdate(Body.CONTENT_URI)
                            .withSelection(Body.MESSAGE_KEY + "=?", mBindArgument)
                            .withValue(Body.TEXT_CONTENT, msg.mText)
                            .build());
                    ops.add(ContentProviderOperation.newUpdate(Body.CONTENT_URI)
                            .withSelection(Body.MESSAGE_KEY + "=?", mBindArgument)
                            .withValue(Body.HTML_CONTENT, msg.mHtml)
                            .build());
                    ops.add(ContentProviderOperation.newUpdate(Message.CONTENT_URI)
                            .withSelection(EmailContent.RECORD_ID + "=?", mBindArgument)
                            .withValue(Message.FLAG_LOADED, Message.FLAG_LOADED_COMPLETE)
                            .build());
                }
            }

            for (Message msg: newEmails) {
                msg.addSaveOps(ops);
            }

            for (Long id : deletedEmails) {
                ops.add(ContentProviderOperation.newDelete(
                        ContentUris.withAppendedId(Message.CONTENT_URI, id)).build());
                AttachmentUtilities.deleteAllAttachmentFiles(mContext, mAccount.mId, id);
            }

            if (!changedEmails.isEmpty()) {
                // Server wins in a conflict...
                for (ServerChange change : changedEmails) {
                    ContentValues cv = new ContentValues();
                    if (change.read != null) {
                        cv.put(MessageColumns.FLAG_READ, change.read);
                    }
                    if (change.flag != null) {
                        cv.put(MessageColumns.FLAG_FAVORITE, change.flag);
                    }
                    if (change.flags != null) {
                        cv.put(MessageColumns.FLAGS, change.flags);
                    }
                    ops.add(ContentProviderOperation.newUpdate(
                            ContentUris.withAppendedId(Message.CONTENT_URI, change.id))
                            .withValues(cv)
                            .build());
                }
            }

            // We only want to update the sync key here
            ContentValues mailboxValues = new ContentValues();
            mailboxValues.put(Mailbox.SYNC_KEY, mMailbox.mSyncKey);
            ops.add(ContentProviderOperation.newUpdate(
                    ContentUris.withAppendedId(Mailbox.CONTENT_URI, mMailbox.mId))
                    .withValues(mailboxValues).build());

            // No commits if we're stopped
            synchronized (mService.getSynchronizer()) {
                if (mService.isStopped()) return;
                try {
                    mContentResolver.applyBatch(EmailContent.AUTHORITY, ops);
                    userLog(mMailbox.mDisplayName, " SyncKey saved as: ", mMailbox.mSyncKey);
                } catch (TransactionTooLargeException e) {
                    Log.w(TAG, "Transaction failed on fetched message; retrying...");

                    if (tryCount <= MAX_NUM_FETCH_SIZE_REDUCTIONS || ops.size() == 1) {
                        // We haven't tried reducing the message body size enough yet. Try this
                        // commit again.
                        commitImpl(tryCount + 1);
                    } else {
                        // We have tried too many time to with just reducing the fetch size.
                        // Try applying the batch operations in smaller chunks
                        applyBatchOperations(ops);
                    }
                } catch (RemoteException e) {
                    // There is nothing to be done here; fail by returning null
                } catch (OperationApplicationException e) {
                    // There is nothing to be done here; fail by returning null
                }
            }
        }
    }

    private void applyBatchOperations(List<ContentProviderOperation> operations) {
        // Assume that since this method is being called, we want to break this batch up in chunks
        // First assume that we want to take the list and do it in two chunks
        int numberOfBatches = 2;

        // Make a copy of the operation list
        final List<ContentProviderOperation> remainingOperations = new ArrayList(operations);

        // determin the batch size
        int batchSize = remainingOperations.size() / numberOfBatches;
        try {
            while (remainingOperations.size() > 0) {
                // Ensure that batch size is smaller than the list size
                if (batchSize > remainingOperations.size()) {
                    batchSize = remainingOperations.size();
                }

                final List<ContentProviderOperation> workingBatch;
                // If the batch size and the list size is the same, just use the whole list
                if (batchSize == remainingOperations.size()) {
                    workingBatch = remainingOperations;
                } else {
                    // Get the sublist of of the remaining batch that contains only the batch size
                    workingBatch = remainingOperations.subList(0, batchSize - 1);
                }

                try {
                    // This is a waste, but ContentResolver#applyBatch requies an ArrayList, but
                    // List#subList retuns only a List
                    final ArrayList<ContentProviderOperation> batch = new ArrayList(workingBatch);
                    mContentResolver.applyBatch(EmailContent.AUTHORITY, batch);

                    // We successfully applied that batch.  Remove it from the remaining work
                    workingBatch.clear();
                } catch (TransactionTooLargeException e) {
                    if (batchSize == 1) {
                        Log.w(TAG, "Transaction failed applying batch. smallest possible batch.");
                        throw e;
                    }
                    Log.w(TAG, "Transaction failed applying batch. trying smaller size...");
                    numberOfBatches++;
                    batchSize = remainingOperations.size() / numberOfBatches;
                }
            }
        } catch (RemoteException e) {
            // There is nothing to be done here; fail by returning null
        } catch (OperationApplicationException e) {
            // There is nothing to be done here; fail by returning null
        }
    }

    @Override
    public String getCollectionName() {
        return "Email";
    }

    private void addCleanupOps(ArrayList<ContentProviderOperation> ops) {
        // If we've sent local deletions, clear out the deleted table
        for (Long id: mDeletedIdList) {
            ops.add(ContentProviderOperation.newDelete(
                    ContentUris.withAppendedId(Message.DELETED_CONTENT_URI, id)).build());
        }
        // And same with the updates
        for (Long id: mUpdatedIdList) {
            ops.add(ContentProviderOperation.newDelete(
                    ContentUris.withAppendedId(Message.UPDATED_CONTENT_URI, id)).build());
        }
    }

    @Override
    public void cleanup() {
        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
        // Delete any moved messages (since we've just synced the mailbox, and no longer need the
        // placeholder message); this prevents duplicates from appearing in the mailbox.
        mBindArgument[0] = Long.toString(mMailbox.mId);
        ops.add(ContentProviderOperation.newDelete(Message.CONTENT_URI)
                .withSelection(WHERE_MAILBOX_KEY_AND_MOVED, mBindArgument).build());
        // If we've done deletions/updates, clean up the deleted/updated tables
        if (!mDeletedIdList.isEmpty() || !mUpdatedIdList.isEmpty()) {
            addCleanupOps(ops);
        }
        try {
            mContext.getContentResolver()
                .applyBatch(EmailContent.AUTHORITY, ops);
        } catch (RemoteException e) {
            // There is nothing to be done here; fail by returning null
        } catch (OperationApplicationException e) {
            // There is nothing to be done here; fail by returning null
        }
    }

    private String formatTwo(int num) {
        if (num < 10) {
            return "0" + (char)('0' + num);
        } else
            return Integer.toString(num);
    }

    /**
     * Create date/time in RFC8601 format.  Oddly enough, for calendar date/time, Microsoft uses
     * a different format that excludes the punctuation (this is why I'm not putting this in a
     * parent class)
     */
    public String formatDateTime(Calendar calendar) {
        StringBuilder sb = new StringBuilder();
        //YYYY-MM-DDTHH:MM:SS.MSSZ
        sb.append(calendar.get(Calendar.YEAR));
        sb.append('-');
        sb.append(formatTwo(calendar.get(Calendar.MONTH) + 1));
        sb.append('-');
        sb.append(formatTwo(calendar.get(Calendar.DAY_OF_MONTH)));
        sb.append('T');
        sb.append(formatTwo(calendar.get(Calendar.HOUR_OF_DAY)));
        sb.append(':');
        sb.append(formatTwo(calendar.get(Calendar.MINUTE)));
        sb.append(':');
        sb.append(formatTwo(calendar.get(Calendar.SECOND)));
        sb.append(".000Z");
        return sb.toString();
    }

    /**
     * Note that messages in the deleted database preserve the message's unique id; therefore, we
     * can utilize this id to find references to the message.  The only reference situation at this
     * point is in the Body table; it is when sending messages via SmartForward and SmartReply
     */
    private boolean messageReferenced(ContentResolver cr, long id) {
        mBindArgument[0] = Long.toString(id);
        // See if this id is referenced in a body
        Cursor c = cr.query(Body.CONTENT_URI, Body.ID_PROJECTION, WHERE_BODY_SOURCE_MESSAGE_KEY,
                mBindArgument, null);
        try {
            return c.moveToFirst();
        } finally {
            c.close();
        }
    }

    /*private*/ /**
     * Serialize commands to delete items from the server; as we find items to delete, add their
     * id's to the deletedId's array
     *
     * @param s the Serializer we're using to create post data
     * @param deletedIds ids whose deletions are being sent to the server
     * @param first whether or not this is the first command being sent
     * @return true if SYNC_COMMANDS hasn't been sent (false otherwise)
     * @throws IOException
     */
    @VisibleForTesting
    boolean sendDeletedItems(Serializer s, ArrayList<Long> deletedIds, boolean first)
            throws IOException {
        ContentResolver cr = mContext.getContentResolver();

        // Find any of our deleted items
        Cursor c = cr.query(Message.DELETED_CONTENT_URI, Message.LIST_PROJECTION,
                MessageColumns.MAILBOX_KEY + '=' + mMailbox.mId, null, null);
        // We keep track of the list of deleted item id's so that we can remove them from the
        // deleted table after the server receives our command
        deletedIds.clear();
        try {
            while (c.moveToNext()) {
                String serverId = c.getString(Message.LIST_SERVER_ID_COLUMN);
                // Keep going if there's no serverId
                if (serverId == null) {
                    continue;
                // Also check if this message is referenced elsewhere
                } else if (messageReferenced(cr, c.getLong(Message.CONTENT_ID_COLUMN))) {
                    userLog("Postponing deletion of referenced message: ", serverId);
                    continue;
                } else if (first) {
                    s.start(Tags.SYNC_COMMANDS);
                    first = false;
                }
                // Send the command to delete this message
                s.start(Tags.SYNC_DELETE).data(Tags.SYNC_SERVER_ID, serverId).end();
                deletedIds.add(c.getLong(Message.LIST_ID_COLUMN));
            }
        } finally {
            c.close();
        }

       return first;
    }

    @Override
    public boolean sendLocalChanges(Serializer s) throws IOException {
        ContentResolver cr = mContext.getContentResolver();

        if (getSyncKey().equals("0")) {
            return false;
        }

        // Never upsync from these folders
        if (mMailbox.mType == Mailbox.TYPE_DRAFTS || mMailbox.mType == Mailbox.TYPE_OUTBOX) {
            return false;
        }

        // This code is split out for unit testing purposes
        boolean firstCommand = sendDeletedItems(s, mDeletedIdList, true);

        if (!mFetchRequestList.isEmpty()) {
            // Add FETCH commands for messages that need a body (i.e. we didn't find it during
            // our earlier sync; this happens only in EAS 2.5 where the body couldn't be found
            // after parsing the message's MIME data)
            if (firstCommand) {
                s.start(Tags.SYNC_COMMANDS);
                firstCommand = false;
            }
            for (FetchRequest req: mFetchRequestList) {
                s.start(Tags.SYNC_FETCH).data(Tags.SYNC_SERVER_ID, req.serverId).end();
            }
        }

        // Find our trash mailbox, since deletions will have been moved there...
        long trashMailboxId =
            Mailbox.findMailboxOfType(mContext, mMailbox.mAccountKey, Mailbox.TYPE_TRASH);

        // Do the same now for updated items
        Cursor c = cr.query(Message.UPDATED_CONTENT_URI, Message.LIST_PROJECTION,
                MessageColumns.MAILBOX_KEY + '=' + mMailbox.mId, null, null);

        // We keep track of the list of updated item id's as we did above with deleted items
        mUpdatedIdList.clear();
        try {
            ContentValues cv = new ContentValues();
            while (c.moveToNext()) {
                long id = c.getLong(Message.LIST_ID_COLUMN);
                // Say we've handled this update
                mUpdatedIdList.add(id);
                // We have the id of the changed item.  But first, we have to find out its current
                // state, since the updated table saves the opriginal state
                Cursor currentCursor = cr.query(ContentUris.withAppendedId(Message.CONTENT_URI, id),
                        UPDATES_PROJECTION, null, null, null);
                try {
                    // If this item no longer exists (shouldn't be possible), just move along
                    if (!currentCursor.moveToFirst()) {
                        continue;
                    }
                    // Keep going if there's no serverId
                    String serverId = currentCursor.getString(UPDATES_SERVER_ID_COLUMN);
                    if (serverId == null) {
                        continue;
                    }

                    boolean flagChange = false;
                    boolean readChange = false;

                    long mailbox = currentCursor.getLong(UPDATES_MAILBOX_KEY_COLUMN);
                    // If the message is now in the trash folder, it has been deleted by the user
                    if (mailbox == trashMailboxId) {
                         if (firstCommand) {
                            s.start(Tags.SYNC_COMMANDS);
                            firstCommand = false;
                        }
                        // Send the command to delete this message
                        s.start(Tags.SYNC_DELETE).data(Tags.SYNC_SERVER_ID, serverId).end();
                        // Mark the message as moved (so the copy will be deleted if/when the server
                        // version is synced)
                        int flags = c.getInt(Message.LIST_FLAGS_COLUMN);
                        cv.put(MessageColumns.FLAGS,
                                flags | EasSyncService.MESSAGE_FLAG_MOVED_MESSAGE);
                        cr.update(ContentUris.withAppendedId(Message.CONTENT_URI, id), cv,
                                null, null);
                        continue;
                    } else if (mailbox != c.getLong(Message.LIST_MAILBOX_KEY_COLUMN)) {
                        // The message has moved to another mailbox; add a request for this
                        // Note: The Sync command doesn't handle moving messages, so we need
                        // to handle this as a "request" (similar to meeting response and
                        // attachment load)
                        mService.addRequest(new MessageMoveRequest(id, mailbox));
                        // Regardless of other changes that might be made, we don't want to indicate
                        // that this message has been updated until the move request has been
                        // handled (without this, a crash between the flag upsync and the move
                        // would cause the move to be lost)
                        mUpdatedIdList.remove(id);
                    }

                    // We can only send flag changes to the server in 12.0 or later
                    int flag = 0;
                    if (mService.mProtocolVersionDouble >= Eas.SUPPORTED_PROTOCOL_EX2007_DOUBLE) {
                        flag = currentCursor.getInt(UPDATES_FLAG_COLUMN);
                        if (flag != c.getInt(Message.LIST_FAVORITE_COLUMN)) {
                            flagChange = true;
                        }
                    }

                    int read = currentCursor.getInt(UPDATES_READ_COLUMN);
                    if (read != c.getInt(Message.LIST_READ_COLUMN)) {
                        readChange = true;
                    }

                    if (!flagChange && !readChange) {
                        // In this case, we've got nothing to send to the server
                        continue;
                    }

                    if (firstCommand) {
                        s.start(Tags.SYNC_COMMANDS);
                        firstCommand = false;
                    }
                    // Send the change to "read" and "favorite" (flagged)
                    s.start(Tags.SYNC_CHANGE)
                        .data(Tags.SYNC_SERVER_ID, c.getString(Message.LIST_SERVER_ID_COLUMN))
                        .start(Tags.SYNC_APPLICATION_DATA);
                    if (readChange) {
                        s.data(Tags.EMAIL_READ, Integer.toString(read));
                    }
                    // "Flag" is a relatively complex concept in EAS 12.0 and above.  It is not only
                    // the boolean "favorite" that we think of in Gmail, but it also represents a
                    // follow up action, which can include a subject, start and due dates, and even
                    // recurrences.  We don't support any of this as yet, but EAS 12.0 and higher
                    // require that a flag contain a status, a type, and four date fields, two each
                    // for start date and end (due) date.
                    if (flagChange) {
                        if (flag != 0) {
                            // Status 2 = set flag
                            s.start(Tags.EMAIL_FLAG).data(Tags.EMAIL_FLAG_STATUS, "2");
                            // "FollowUp" is the standard type
                            s.data(Tags.EMAIL_FLAG_TYPE, "FollowUp");
                            long now = System.currentTimeMillis();
                            Calendar calendar =
                                GregorianCalendar.getInstance(TimeZone.getTimeZone("GMT"));
                            calendar.setTimeInMillis(now);
                            // Flags are required to have a start date and end date (duplicated)
                            // First, we'll set the current date/time in GMT as the start time
                            String utc = formatDateTime(calendar);
                            s.data(Tags.TASK_START_DATE, utc).data(Tags.TASK_UTC_START_DATE, utc);
                            // And then we'll use one week from today for completion date
                            calendar.setTimeInMillis(now + 1*WEEKS);
                            utc = formatDateTime(calendar);
                            s.data(Tags.TASK_DUE_DATE, utc).data(Tags.TASK_UTC_DUE_DATE, utc);
                            s.end();
                        } else {
                            s.tag(Tags.EMAIL_FLAG);
                        }
                    }
                    s.end().end(); // SYNC_APPLICATION_DATA, SYNC_CHANGE
                } finally {
                    currentCursor.close();
                }
            }
        } finally {
            c.close();
        }

        if (!firstCommand) {
            s.end(); // SYNC_COMMANDS
        }
        return false;
    }
}
