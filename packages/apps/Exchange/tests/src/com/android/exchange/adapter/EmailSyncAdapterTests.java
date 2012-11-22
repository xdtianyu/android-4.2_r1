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

import android.content.ContentUris;
import android.content.ContentValues;
import android.test.suitebuilder.annotation.SmallTest;

import com.android.emailcommon.provider.Account;
import com.android.emailcommon.provider.EmailContent;
import com.android.emailcommon.provider.EmailContent.Body;
import com.android.emailcommon.provider.EmailContent.Message;
import com.android.emailcommon.provider.EmailContent.MessageColumns;
import com.android.emailcommon.provider.EmailContent.SyncColumns;
import com.android.emailcommon.provider.Mailbox;
import com.android.exchange.EasSyncService;
import com.android.exchange.adapter.EmailSyncAdapter.EasEmailSyncParser;
import com.android.exchange.adapter.EmailSyncAdapter.EasEmailSyncParser.ServerChange;
import com.android.exchange.provider.EmailContentSetupUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.TimeZone;
@SmallTest
public class EmailSyncAdapterTests extends SyncAdapterTestCase<EmailSyncAdapter> {

    private static final String WHERE_ACCOUNT_KEY = Message.ACCOUNT_KEY + "=?";
    private static final String[] ACCOUNT_ARGUMENT = new String[1];

    // A server id that is guaranteed to be test-related
    private static final String TEST_SERVER_ID = "__1:22";

    public EmailSyncAdapterTests() {
        super();
    }

    /**
     * Check functionality for getting mime type from a file name (using its extension)
     * The default for all unknown files is application/octet-stream
     */
    public void testGetMimeTypeFromFileName() throws IOException {
        EasSyncService service = getTestService();
        EmailSyncAdapter adapter = new EmailSyncAdapter(service);
        EasEmailSyncParser p = adapter.new EasEmailSyncParser(getTestInputStream(), adapter);
        // Test a few known types
        String mimeType = p.getMimeTypeFromFileName("foo.jpg");
        assertEquals("image/jpeg", mimeType);
        // Make sure this is case insensitive
        mimeType = p.getMimeTypeFromFileName("foo.JPG");
        assertEquals("image/jpeg", mimeType);
        mimeType = p.getMimeTypeFromFileName("this_is_a_weird_filename.gif");
        assertEquals("image/gif", mimeType);
        // Test an illegal file name ending with the extension prefix
        mimeType = p.getMimeTypeFromFileName("foo.");
        assertEquals("application/octet-stream", mimeType);
        // Test a really awful name
        mimeType = p.getMimeTypeFromFileName(".....");
        assertEquals("application/octet-stream", mimeType);
        // Test a bare file name (no extension)
        mimeType = p.getMimeTypeFromFileName("foo");
        assertEquals("application/octet-stream", mimeType);
        // And no name at all (null isn't a valid input)
        mimeType = p.getMimeTypeFromFileName("");
        assertEquals("application/octet-stream", mimeType);
    }

    public void testFormatDateTime() throws IOException {
        EmailSyncAdapter adapter = getTestSyncAdapter(EmailSyncAdapter.class);
        GregorianCalendar calendar = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        // Calendar is odd, months are zero based, so the first 11 below is December...
        calendar.set(2008, 11, 11, 18, 19, 20);
        String date = adapter.formatDateTime(calendar);
        assertEquals("2008-12-11T18:19:20.000Z", date);
        calendar.clear();
        calendar.set(2012, 0, 2, 23, 0, 1);
        date = adapter.formatDateTime(calendar);
        assertEquals("2012-01-02T23:00:01.000Z", date);
    }

    public void testSendDeletedItems() throws IOException {
        setupAccountMailboxAndMessages(0);
        // Setup our adapter and parser
        setupSyncParserAndAdapter(mAccount, mMailbox);

        Serializer s = new Serializer();
        ArrayList<Long> ids = new ArrayList<Long>();
        ArrayList<Long> deletedIds = new ArrayList<Long>();

        // Create account and two mailboxes
        mSyncAdapter.mAccount = mAccount;
        Mailbox box1 = EmailContentSetupUtils.setupMailbox("box1", mAccount.mId, true,
                mProviderContext);
        mSyncAdapter.mMailbox = box1;

        // Create 3 messages
        Message msg1 = EmailContentSetupUtils.setupMessage("message1", mAccount.mId, box1.mId,
                true, true, mProviderContext);
        ids.add(msg1.mId);
        Message msg2 = EmailContentSetupUtils.setupMessage("message2", mAccount.mId, box1.mId,
                true, true, mProviderContext);
        ids.add(msg2.mId);
        Message msg3 = EmailContentSetupUtils.setupMessage("message3", mAccount.mId, box1.mId,
                true, true, mProviderContext);
        ids.add(msg3.mId);
        assertEquals(3, EmailContent.count(mProviderContext, Message.CONTENT_URI, WHERE_ACCOUNT_KEY,
                getAccountArgument(mAccount.mId)));

        // Delete them
        for (long id: ids) {
            mResolver.delete(ContentUris.withAppendedId(Message.SYNCED_CONTENT_URI, id),
                    null, null);
        }

        // Confirm that the messages are in the proper table
        assertEquals(0, EmailContent.count(mProviderContext, Message.CONTENT_URI, WHERE_ACCOUNT_KEY,
                getAccountArgument(mAccount.mId)));
        assertEquals(3, EmailContent.count(mProviderContext, Message.DELETED_CONTENT_URI,
                WHERE_ACCOUNT_KEY, getAccountArgument(mAccount.mId)));

        // Call code to send deletions; the id's of the ones actually deleted will be in the
        // deletedIds list
        mSyncAdapter.sendDeletedItems(s, deletedIds, true);
        assertEquals(3, deletedIds.size());

        // Clear this out for the next test
        deletedIds.clear();

        // Create a new message
        Message msg4 = EmailContentSetupUtils.setupMessage("message4", mAccount.mId, box1.mId,
                true, true, mProviderContext);
        assertEquals(1, EmailContent.count(mProviderContext, Message.CONTENT_URI, WHERE_ACCOUNT_KEY,
                getAccountArgument(mAccount.mId)));
        // Find the body for this message
        Body body = Body.restoreBodyWithMessageId(mProviderContext, msg4.mId);
        // Set its source message to msg2's id
        ContentValues values = new ContentValues();
        values.put(Body.SOURCE_MESSAGE_KEY, msg2.mId);
        body.update(mProviderContext, values);

        // Now send deletions again; this time only two should get deleted; msg2 should NOT be
        // deleted as it's referenced by msg4
        mSyncAdapter.sendDeletedItems(s, deletedIds, true);
        assertEquals(2, deletedIds.size());
        assertFalse(deletedIds.contains(msg2.mId));
    }

    private String[] getAccountArgument(long id) {
        ACCOUNT_ARGUMENT[0] = Long.toString(id);
        return ACCOUNT_ARGUMENT;
    }

    void setupSyncParserAndAdapter(Account account, Mailbox mailbox) throws IOException {
        EasSyncService service = getTestService(account, mailbox);
        mSyncAdapter = new EmailSyncAdapter(service);
        mSyncParser = mSyncAdapter.new EasEmailSyncParser(getTestInputStream(), mSyncAdapter);
    }

    ArrayList<Long> setupAccountMailboxAndMessages(int numMessages) {
        ArrayList<Long> ids = new ArrayList<Long>();

        // Create account and two mailboxes
        mAccount = EmailContentSetupUtils.setupAccount("account", true, mProviderContext);
        mMailbox = EmailContentSetupUtils.setupMailbox("box1", mAccount.mId, true,
                mProviderContext);

        for (int i = 0; i < numMessages; i++) {
            Message msg = EmailContentSetupUtils.setupMessage("message" + i, mAccount.mId,
                    mMailbox.mId, true, true, mProviderContext);
            ids.add(msg.mId);
        }

        assertEquals(numMessages, EmailContent.count(mProviderContext, Message.CONTENT_URI,
                WHERE_ACCOUNT_KEY, getAccountArgument(mAccount.mId)));
        return ids;
    }

    public void testDeleteParser() throws IOException {
        // Setup some messages
        ArrayList<Long> messageIds = setupAccountMailboxAndMessages(3);
        ContentValues cv = new ContentValues();
        cv.put(SyncColumns.SERVER_ID, TEST_SERVER_ID);
        long deleteMessageId = messageIds.get(1);
        mResolver.update(ContentUris.withAppendedId(Message.CONTENT_URI, deleteMessageId), cv,
                null, null);

        // Setup our adapter and parser
        setupSyncParserAndAdapter(mAccount, mMailbox);

        // Set up an input stream with a delete command
        Serializer s = new Serializer(false);
        s.start(Tags.SYNC_DELETE).data(Tags.SYNC_SERVER_ID, TEST_SERVER_ID).end().done();
        byte[] bytes = s.toByteArray();
        mSyncParser.resetInput(new ByteArrayInputStream(bytes));
        mSyncParser.nextTag(0);

        // Run the delete parser
        ArrayList<Long> deleteList = new ArrayList<Long>();
        mSyncParser.deleteParser(deleteList, Tags.SYNC_DELETE);
        // It should have found the message
        assertEquals(1, deleteList.size());
        long id = deleteList.get(0);
        // And the id's should match
        assertEquals(deleteMessageId, id);
    }

    public void testChangeParser() throws IOException {
        // Setup some messages
        ArrayList<Long> messageIds = setupAccountMailboxAndMessages(3);
        ContentValues cv = new ContentValues();
        int randomFlags = Message.FLAG_INCOMING_MEETING_CANCEL | Message.FLAG_TYPE_FORWARD;
        cv.put(SyncColumns.SERVER_ID, TEST_SERVER_ID);
        cv.put(MessageColumns.FLAGS, randomFlags);
        long changeMessageId = messageIds.get(1);
        mResolver.update(ContentUris.withAppendedId(Message.CONTENT_URI, changeMessageId), cv,
                null, null);

        // Setup our adapter and parser
        setupSyncParserAndAdapter(mAccount, mMailbox);

        // Set up an input stream with a change command (marking TEST_SERVER_ID unread)
        // Note that the test message creation code sets read to "true"
        Serializer s = new Serializer(false);
        s.start(Tags.SYNC_CHANGE).data(Tags.SYNC_SERVER_ID, TEST_SERVER_ID);
        s.start(Tags.SYNC_APPLICATION_DATA);
        s.data(Tags.EMAIL_READ, "0");
        s.data(Tags.EMAIL2_LAST_VERB_EXECUTED,
                Integer.toString(EmailSyncAdapter.LAST_VERB_FORWARD));
        s.end().end().done();
        byte[] bytes = s.toByteArray();
        mSyncParser.resetInput(new ByteArrayInputStream(bytes));
        mSyncParser.nextTag(0);

        // Run the delete parser
        ArrayList<ServerChange> changeList = new ArrayList<ServerChange>();
        mSyncParser.changeParser(changeList);
        // It should have found the message
        assertEquals(1, changeList.size());
        // And the id's should match
        ServerChange change = changeList.get(0);
        assertEquals(changeMessageId, change.id);
        assertNotNull(change.read);
        assertFalse(change.read);
        // Make sure we see the forwarded flag AND that the original flags are preserved
        assertEquals((Integer)(randomFlags | Message.FLAG_FORWARDED), change.flags);
    }

    public void testCleanup() throws IOException {
        // Setup some messages
        ArrayList<Long> messageIds = setupAccountMailboxAndMessages(3);
        // Setup our adapter and parser
        setupSyncParserAndAdapter(mAccount, mMailbox);

        // Delete two of the messages, change one
        long id = messageIds.get(0);
        mResolver.delete(ContentUris.withAppendedId(Message.SYNCED_CONTENT_URI, id),
                null, null);
        mSyncAdapter.mDeletedIdList.add(id);
        id = messageIds.get(1);
        mResolver.delete(ContentUris.withAppendedId(Message.SYNCED_CONTENT_URI,
                id), null, null);
        mSyncAdapter.mDeletedIdList.add(id);
        id = messageIds.get(2);
        ContentValues cv = new ContentValues();
        cv.put(Message.FLAG_READ, 0);
        mResolver.update(ContentUris.withAppendedId(Message.SYNCED_CONTENT_URI,
                id), cv, null, null);
        mSyncAdapter.mUpdatedIdList.add(id);

        // The changed message should still exist
        assertEquals(1, EmailContent.count(mProviderContext, Message.CONTENT_URI, WHERE_ACCOUNT_KEY,
                getAccountArgument(mAccount.mId)));

        // As well, the two deletions and one update
        assertEquals(2, EmailContent.count(mProviderContext, Message.DELETED_CONTENT_URI,
                WHERE_ACCOUNT_KEY, getAccountArgument(mAccount.mId)));
        assertEquals(1, EmailContent.count(mProviderContext, Message.UPDATED_CONTENT_URI,
                WHERE_ACCOUNT_KEY, getAccountArgument(mAccount.mId)));

        // Cleanup (i.e. after sync); should remove items from delete/update tables
        mSyncAdapter.cleanup();

        // The three should be gone
        assertEquals(0, EmailContent.count(mProviderContext, Message.DELETED_CONTENT_URI,
                WHERE_ACCOUNT_KEY, getAccountArgument(mAccount.mId)));
        assertEquals(0, EmailContent.count(mProviderContext, Message.UPDATED_CONTENT_URI,
                WHERE_ACCOUNT_KEY, getAccountArgument(mAccount.mId)));
    }
}
