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

package com.android.exchange.provider;

import com.android.emailcommon.provider.Account;
import com.android.emailcommon.provider.EmailContent.MailboxColumns;
import com.android.emailcommon.provider.Mailbox;
import com.android.exchange.utility.ExchangeTestCase;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.test.suitebuilder.annotation.MediumTest;

/**
 * Tests of MailboxUtilities.
 *
 * You can run this entire test case with:
 *   runtest -c com.android.exchange.provider.MailboxUtilitiesTests exchange
 */
@MediumTest
public class MailboxUtilitiesTests extends ExchangeTestCase {

    // All tests must build their accounts in mAccount so it will be deleted from live data
    private Account mAccount;
    private ContentResolver mResolver;
    private ContentValues mNullParentKey;

    // Flag sets found in regular email folders that are parents or children
    private static final int PARENT_FLAGS =
            Mailbox.FLAG_ACCEPTS_MOVED_MAIL | Mailbox.FLAG_HOLDS_MAIL |
            Mailbox.FLAG_HAS_CHILDREN | Mailbox.FLAG_CHILDREN_VISIBLE;
    private static final int CHILD_FLAGS =
            Mailbox.FLAG_ACCEPTS_MOVED_MAIL | Mailbox.FLAG_HOLDS_MAIL;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        mAccount = null;
        mResolver = mProviderContext.getContentResolver();
        mNullParentKey = new ContentValues();
        mNullParentKey.putNull(Mailbox.PARENT_KEY);
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        // If we've created and saved an account, delete it
        if (mAccount != null && mAccount.mId > 0) {
            mResolver.delete(
                    ContentUris.withAppendedId(Account.CONTENT_URI, mAccount.mId), null, null);
        }
    }

    public void testSetupParentKeyAndFlag() {
        // Set up account and various mailboxes with/without parents
        mAccount = setupTestAccount("acct1", true);
        Mailbox box1 = EmailContentSetupUtils.setupMailbox("box1", mAccount.mId, true,
                mProviderContext, Mailbox.TYPE_DRAFTS);
        Mailbox box2 = EmailContentSetupUtils.setupMailbox("box2", mAccount.mId, true,
                mProviderContext, Mailbox.TYPE_OUTBOX, box1);
        Mailbox box3 = EmailContentSetupUtils.setupMailbox("box3", mAccount.mId, true,
                mProviderContext, Mailbox.TYPE_ATTACHMENT, box1);
        Mailbox box4 = EmailContentSetupUtils.setupMailbox("box4", mAccount.mId, true,
                mProviderContext, Mailbox.TYPE_NOT_SYNCABLE + 64, box3);
        Mailbox box5 = EmailContentSetupUtils.setupMailbox("box5", mAccount.mId, true,
                mProviderContext, Mailbox.TYPE_MAIL, box3);

        // To make this work, we need to manually set parentKey to null for all mailboxes
        // This simulates an older database needing update
        mResolver.update(Mailbox.CONTENT_URI, mNullParentKey, null, null);

        // Hand-create the account selector for our account
        String accountSelector = MailboxColumns.ACCOUNT_KEY + " IN (" + mAccount.mId + ")";

        // Fix up the database and restore the mailboxes
        MailboxUtilities.fixupUninitializedParentKeys(mProviderContext, accountSelector);
        box1 = Mailbox.restoreMailboxWithId(mProviderContext, box1.mId);
        box2 = Mailbox.restoreMailboxWithId(mProviderContext, box2.mId);
        box3 = Mailbox.restoreMailboxWithId(mProviderContext, box3.mId);
        box4 = Mailbox.restoreMailboxWithId(mProviderContext, box4.mId);
        box5 = Mailbox.restoreMailboxWithId(mProviderContext, box5.mId);

        // Test that flags and parent key are set properly
        assertEquals(Mailbox.FLAG_HOLDS_MAIL | Mailbox.FLAG_HAS_CHILDREN |
                Mailbox.FLAG_CHILDREN_VISIBLE, box1.mFlags);
        assertEquals(-1, box1.mParentKey);

        assertEquals(Mailbox.FLAG_HOLDS_MAIL, box2.mFlags);
        assertEquals(box1.mId, box2.mParentKey);

        assertEquals(Mailbox.FLAG_HAS_CHILDREN | Mailbox.FLAG_CHILDREN_VISIBLE, box3.mFlags);
        assertEquals(box1.mId, box3.mParentKey);

        assertEquals(0, box4.mFlags);
        assertEquals(box3.mId, box4.mParentKey);

        assertEquals(Mailbox.FLAG_HOLDS_MAIL | Mailbox.FLAG_ACCEPTS_MOVED_MAIL, box5.mFlags);
        assertEquals(box3.mId, box5.mParentKey);
    }

    private void simulateFolderSyncChangeHandling(String accountSelector, Mailbox...mailboxes) {
        // Run the parent key analyzer and reload the mailboxes
        MailboxUtilities.fixupUninitializedParentKeys(mProviderContext, accountSelector);
        for (Mailbox mailbox: mailboxes) {
            String serverId = mailbox.mServerId;
            MailboxUtilities.setFlagsAndChildrensParentKey(mProviderContext, accountSelector,
                    serverId);
        }
    }

    /**
     * Test three cases of adding a folder to an existing hierarchy.  Case 1:  Add to parent.
     */
    public void testParentKeyAddFolder1() {
        // Set up account and various mailboxes with/without parents
        mAccount = setupTestAccount("acct1", true);
        String accountSelector = MailboxColumns.ACCOUNT_KEY + " IN (" + mAccount.mId + ")";

        Mailbox box1 = EmailContentSetupUtils.setupMailbox(
                "box1", mAccount.mId, true, mProviderContext, Mailbox.TYPE_MAIL);
        Mailbox box2 = EmailContentSetupUtils.setupMailbox(
                "box2", mAccount.mId, true, mProviderContext, Mailbox.TYPE_MAIL, box1);

        // Manually set parentKey to null for all mailboxes, as if an initial sync or post-upgrade
        mResolver.update(Mailbox.CONTENT_URI, mNullParentKey, null, null);

        // Run the parent key analyzer to set up the initial hierarchy.
        MailboxUtilities.fixupUninitializedParentKeys(mProviderContext, accountSelector);
        box1 = Mailbox.restoreMailboxWithId(mProviderContext, box1.mId);
        box2 = Mailbox.restoreMailboxWithId(mProviderContext, box2.mId);

        // Confirm flags and parent key(s) as expected
        assertEquals(PARENT_FLAGS, box1.mFlags);
        assertEquals(-1, box1.mParentKey);

        assertEquals(CHILD_FLAGS, box2.mFlags);
        assertEquals(box1.mId, box2.mParentKey);

        // The specific test:  Create a 3rd mailbox and attach it to box1 (already a parent)

        Mailbox box3 = EmailContentSetupUtils.setupMailbox(
                "box3", mAccount.mId, false, mProviderContext, Mailbox.TYPE_MAIL, box1);
        box3.mParentKey = Mailbox.PARENT_KEY_UNINITIALIZED;
        box3.save(mProviderContext);
        simulateFolderSyncChangeHandling(accountSelector, box1 /*box3's parent*/);

        box1 = Mailbox.restoreMailboxWithId(mProviderContext, box1.mId);
        box2 = Mailbox.restoreMailboxWithId(mProviderContext, box2.mId);
        box3 = Mailbox.restoreMailboxWithId(mProviderContext, box3.mId);

        // Confirm flags and parent key(s) as expected
        assertEquals(PARENT_FLAGS, box1.mFlags);
        assertEquals(-1, box1.mParentKey);

        assertEquals(CHILD_FLAGS, box2.mFlags);
        assertEquals(box1.mId, box2.mParentKey);

        assertEquals(CHILD_FLAGS, box3.mFlags);
        assertEquals(box1.mId, box3.mParentKey);
    }

    /**
     * Test three cases of adding a folder to an existing hierarchy.  Case 2:  Add to child.
     */
    public void testParentKeyAddFolder2() {
        // Set up account and various mailboxes with/without parents
        mAccount = setupTestAccount("acct1", true);
        String accountSelector = MailboxColumns.ACCOUNT_KEY + " IN (" + mAccount.mId + ")";

        Mailbox box1 = EmailContentSetupUtils.setupMailbox(
                "box1", mAccount.mId, true, mProviderContext, Mailbox.TYPE_MAIL);
        Mailbox box2 = EmailContentSetupUtils.setupMailbox(
                "box2", mAccount.mId, true, mProviderContext, Mailbox.TYPE_MAIL, box1);

        // Manually set parentKey to null for all mailboxes, as if an initial sync or post-upgrade
        mResolver.update(Mailbox.CONTENT_URI, mNullParentKey, null, null);

        // Run the parent key analyzer to set up the initial hierarchy.
        MailboxUtilities.fixupUninitializedParentKeys(mProviderContext, accountSelector);

        // Skipping tests of initial hierarchy - see testParentKeyAddFolder1()

        // The specific test:  Create a 3rd mailbox and attach it to box2 (currently a child)

        Mailbox box3 = EmailContentSetupUtils.setupMailbox(
                "box3", mAccount.mId, false, mProviderContext, Mailbox.TYPE_MAIL, box2);
        box3.mParentKey = Mailbox.PARENT_KEY_UNINITIALIZED;
        box3.save(mProviderContext);
        simulateFolderSyncChangeHandling(accountSelector, box2 /*box3's parent*/);

        box1 = Mailbox.restoreMailboxWithId(mProviderContext, box1.mId);
        box2 = Mailbox.restoreMailboxWithId(mProviderContext, box2.mId);
        box3 = Mailbox.restoreMailboxWithId(mProviderContext, box3.mId);

        // Confirm flags and parent key(s) as expected
        assertEquals(PARENT_FLAGS, box1.mFlags);
        assertEquals(-1, box1.mParentKey);

        assertEquals(PARENT_FLAGS, box2.mFlags);    // should become a parent
        assertEquals(box1.mId, box2.mParentKey);

        assertEquals(CHILD_FLAGS, box3.mFlags);     // should be child of box2
        assertEquals(box2.mId, box3.mParentKey);
    }

    /**
     * Test three cases of adding a folder to an existing hierarchy.  Case 3:  Add to root.
     */
    public void testParentKeyAddFolder3() {
        // Set up account and various mailboxes with/without parents
        mAccount = setupTestAccount("acct1", true);
        String accountSelector = MailboxColumns.ACCOUNT_KEY + " IN (" + mAccount.mId + ")";

        Mailbox box1 = EmailContentSetupUtils.setupMailbox(
                "box1", mAccount.mId, true, mProviderContext, Mailbox.TYPE_MAIL);
        Mailbox box2 = EmailContentSetupUtils.setupMailbox(
                "box2", mAccount.mId, true, mProviderContext, Mailbox.TYPE_MAIL, box1);

        // Manually set parentKey to null for all mailboxes, as if an initial sync or post-upgrade
        mResolver.update(Mailbox.CONTENT_URI, mNullParentKey, null, null);

        // Run the parent key analyzer to set up the initial hierarchy.
        MailboxUtilities.fixupUninitializedParentKeys(mProviderContext, accountSelector);

        // Skipping tests of initial hierarchy - see testParentKeyAddFolder1()

        // The specific test:  Create a 3rd mailbox and give it no parent (it's top-level).

        Mailbox box3 = EmailContentSetupUtils.setupMailbox(
                "box3", mAccount.mId, false, mProviderContext, Mailbox.TYPE_MAIL);
        box3.mParentKey = Mailbox.PARENT_KEY_UNINITIALIZED;
        box3.save(mProviderContext);

        simulateFolderSyncChangeHandling(accountSelector);
        box1 = Mailbox.restoreMailboxWithId(mProviderContext, box1.mId);
        box2 = Mailbox.restoreMailboxWithId(mProviderContext, box2.mId);
        box3 = Mailbox.restoreMailboxWithId(mProviderContext, box3.mId);

        // Confirm flags and parent key(s) as expected
        assertEquals(PARENT_FLAGS, box1.mFlags);
        assertEquals(-1, box1.mParentKey);

        assertEquals(CHILD_FLAGS, box2.mFlags);
        assertEquals(box1.mId, box2.mParentKey);

        assertEquals(CHILD_FLAGS, box3.mFlags);
        assertEquals(-1, box3.mParentKey);
    }

    /**
     * Test three cases of removing a folder from the hierarchy.  Case 1:  Remove from parent.
     */
    public void testParentKeyRemoveFolder1() {
        // Set up account and mailboxes
        mAccount = setupTestAccount("acct1", true);
        String accountSelector = MailboxColumns.ACCOUNT_KEY + " IN (" + mAccount.mId + ")";

        // Initial configuration for this test:  box1 has two children.
        Mailbox box1 = EmailContentSetupUtils.setupMailbox(
                "box1", mAccount.mId, true, mProviderContext, Mailbox.TYPE_MAIL);
        Mailbox box2 = EmailContentSetupUtils.setupMailbox(
                "box2", mAccount.mId, true, mProviderContext, Mailbox.TYPE_MAIL, box1);
        Mailbox box3 = EmailContentSetupUtils.setupMailbox(
                "box3", mAccount.mId, true, mProviderContext, Mailbox.TYPE_MAIL, box1);

        // Manually set parentKey to null for all mailboxes, as if an initial sync or post-upgrade
        mResolver.update(Mailbox.CONTENT_URI, mNullParentKey, null, null);

        // Confirm initial configuration as expected
        MailboxUtilities.fixupUninitializedParentKeys(mProviderContext, accountSelector);
        box1 = Mailbox.restoreMailboxWithId(mProviderContext, box1.mId);
        box2 = Mailbox.restoreMailboxWithId(mProviderContext, box2.mId);
        box3 = Mailbox.restoreMailboxWithId(mProviderContext, box3.mId);

        // Confirm flags and parent key(s) as expected
        assertEquals(PARENT_FLAGS, box1.mFlags);
        assertEquals(-1, box1.mParentKey);

        assertEquals(CHILD_FLAGS, box2.mFlags);
        assertEquals(box1.mId, box2.mParentKey);

        assertEquals(CHILD_FLAGS, box3.mFlags);
        assertEquals(box1.mId, box3.mParentKey);

        // The specific test:  Delete box3 and check remaining configuration
        mResolver.delete(ContentUris.withAppendedId(Mailbox.CONTENT_URI, box3.mId), null, null);
        simulateFolderSyncChangeHandling(accountSelector, box1 /*box3's parent*/);

        box1 = Mailbox.restoreMailboxWithId(mProviderContext, box1.mId);
        box2 = Mailbox.restoreMailboxWithId(mProviderContext, box2.mId);
        box3 = Mailbox.restoreMailboxWithId(mProviderContext, box3.mId);

        // Confirm flags and parent key(s) as expected
        assertEquals(PARENT_FLAGS, box1.mFlags);        // Should still be a parent
        assertEquals(-1, box1.mParentKey);

        assertEquals(CHILD_FLAGS, box2.mFlags);
        assertEquals(box1.mId, box2.mParentKey);

        assertNull(box3);
    }

    /**
     * Test three cases of removing a folder from the hierarchy.  Case 2:  Remove from child.
     */
    public void testParentKeyRemoveFolder2() {
        // Set up account and mailboxes
        mAccount = setupTestAccount("acct1", true);
        String accountSelector = MailboxColumns.ACCOUNT_KEY + " IN (" + mAccount.mId + ")";

        // Initial configuration for this test:  box1 has box2 and box2 has box3.
        Mailbox box1 = EmailContentSetupUtils.setupMailbox(
                "box1", mAccount.mId, true, mProviderContext, Mailbox.TYPE_MAIL);
        Mailbox box2 = EmailContentSetupUtils.setupMailbox(
                "box2", mAccount.mId, true, mProviderContext, Mailbox.TYPE_MAIL, box1);
        Mailbox box3 = EmailContentSetupUtils.setupMailbox(
                "box3", mAccount.mId, true, mProviderContext, Mailbox.TYPE_MAIL, box2);

        // Manually set parentKey to null for all mailboxes, as if an initial sync or post-upgrade
        mResolver.update(Mailbox.CONTENT_URI, mNullParentKey, null, null);

        // Confirm initial configuration as expected
        MailboxUtilities.fixupUninitializedParentKeys(mProviderContext, accountSelector);
        box1 = Mailbox.restoreMailboxWithId(mProviderContext, box1.mId);
        box2 = Mailbox.restoreMailboxWithId(mProviderContext, box2.mId);
        box3 = Mailbox.restoreMailboxWithId(mProviderContext, box3.mId);

        // Confirm flags and parent key(s) as expected
        assertEquals(PARENT_FLAGS, box1.mFlags);
        assertEquals(-1, box1.mParentKey);

        assertEquals(PARENT_FLAGS, box2.mFlags);    // becomes a parent
        assertEquals(box1.mId, box2.mParentKey);

        assertEquals(CHILD_FLAGS, box3.mFlags);
        assertEquals(box2.mId, box3.mParentKey);

        // The specific test:  Delete box3 and check remaining configuration
        mResolver.delete(ContentUris.withAppendedId(Mailbox.CONTENT_URI, box3.mId), null, null);
        simulateFolderSyncChangeHandling(accountSelector, box2 /*box3's parent*/);

        box1 = Mailbox.restoreMailboxWithId(mProviderContext, box1.mId);
        box2 = Mailbox.restoreMailboxWithId(mProviderContext, box2.mId);
        box3 = Mailbox.restoreMailboxWithId(mProviderContext, box3.mId);

        // Confirm flags and parent key(s) as expected
        assertEquals(PARENT_FLAGS, box1.mFlags);        // Should still be a parent
        assertEquals(-1, box1.mParentKey);

        assertEquals(CHILD_FLAGS, box2.mFlags);         // Becomes a child
        assertEquals(box1.mId, box2.mParentKey);

        assertNull(box3);
    }

    /**
     * Test three cases of removing a folder from the hierarchy.  Case 3:  Remove from root.
     */
    public void testParentKeyRemoveFolder3() {
        // Set up account and mailboxes
        mAccount = setupTestAccount("acct1", true);
        String accountSelector = MailboxColumns.ACCOUNT_KEY + " IN (" + mAccount.mId + ")";

        // Initial configuration for this test:  box1 has box2, box3 is also at root.
        Mailbox box1 = EmailContentSetupUtils.setupMailbox(
                "box1", mAccount.mId, true, mProviderContext, Mailbox.TYPE_MAIL);
        Mailbox box2 = EmailContentSetupUtils.setupMailbox(
                "box2", mAccount.mId, true, mProviderContext, Mailbox.TYPE_MAIL, box1);
        Mailbox box3 = EmailContentSetupUtils.setupMailbox(
                "box3", mAccount.mId, true, mProviderContext, Mailbox.TYPE_MAIL);

        // Manually set parentKey to null for all mailboxes, as if an initial sync or post-upgrade
        mResolver.update(Mailbox.CONTENT_URI, mNullParentKey, null, null);

        // Confirm initial configuration as expected
        MailboxUtilities.fixupUninitializedParentKeys(mProviderContext, accountSelector);
        box1 = Mailbox.restoreMailboxWithId(mProviderContext, box1.mId);
        box2 = Mailbox.restoreMailboxWithId(mProviderContext, box2.mId);
        box3 = Mailbox.restoreMailboxWithId(mProviderContext, box3.mId);

        // Confirm flags and parent key(s) as expected
        assertEquals(PARENT_FLAGS, box1.mFlags);
        assertEquals(-1, box1.mParentKey);

        assertEquals(CHILD_FLAGS, box2.mFlags);
        assertEquals(box1.mId, box2.mParentKey);

        assertEquals(CHILD_FLAGS, box3.mFlags);
        assertEquals(-1, box3.mParentKey);

        // The specific test:  Delete box3 and check remaining configuration
        mResolver.delete(ContentUris.withAppendedId(Mailbox.CONTENT_URI, box3.mId), null, null);
        simulateFolderSyncChangeHandling(accountSelector);

        box1 = Mailbox.restoreMailboxWithId(mProviderContext, box1.mId);
        box2 = Mailbox.restoreMailboxWithId(mProviderContext, box2.mId);
        box3 = Mailbox.restoreMailboxWithId(mProviderContext, box3.mId);

        // Confirm flags and parent key(s) as expected
        assertEquals(PARENT_FLAGS, box1.mFlags);        // Should still be a parent
        assertEquals(-1, box1.mParentKey);

        assertEquals(CHILD_FLAGS, box2.mFlags);         // Should still be a child
        assertEquals(box1.mId, box2.mParentKey);

        assertNull(box3);
    }

    /**
     * Test changing a parent from none
     */
    public void testChangeFromNoParentToParent() {
        // Set up account and mailboxes
        mAccount = setupTestAccount("acct1", true);
        String accountSelector = MailboxColumns.ACCOUNT_KEY + " IN (" + mAccount.mId + ")";

        // Initial configuration for this test:  box1 has box2, box3 is also at root.
        Mailbox box1 = EmailContentSetupUtils.setupMailbox(
                "box1", mAccount.mId, true, mProviderContext, Mailbox.TYPE_MAIL);
        Mailbox box2 = EmailContentSetupUtils.setupMailbox(
                "box2", mAccount.mId, true, mProviderContext, Mailbox.TYPE_MAIL, box1);
        Mailbox box3 = EmailContentSetupUtils.setupMailbox(
                "box3", mAccount.mId, true, mProviderContext, Mailbox.TYPE_MAIL);

        // Manually set parentKey to null for all mailboxes, as if an initial sync or post-upgrade
        mResolver.update(Mailbox.CONTENT_URI, mNullParentKey, null, null);

        // Confirm initial configuration as expected
        MailboxUtilities.fixupUninitializedParentKeys(mProviderContext, accountSelector);
        box1 = Mailbox.restoreMailboxWithId(mProviderContext, box1.mId);
        box2 = Mailbox.restoreMailboxWithId(mProviderContext, box2.mId);
        box3 = Mailbox.restoreMailboxWithId(mProviderContext, box3.mId);

        // Confirm flags and parent key(s) as expected
        assertEquals(PARENT_FLAGS, box1.mFlags);
        assertEquals(-1, box1.mParentKey);

        assertEquals(CHILD_FLAGS, box2.mFlags);
        assertEquals(box1.mId, box2.mParentKey);

        assertEquals(CHILD_FLAGS, box3.mFlags);
        assertEquals(-1, box3.mParentKey);

        // The specific test:  Give box 3 a new parent (box 2) and check remaining configuration
        ContentValues values = new ContentValues();
        values.put(Mailbox.PARENT_SERVER_ID, box2.mServerId);
        mResolver.update(ContentUris.withAppendedId(Mailbox.CONTENT_URI, box3.mId), values,
                null, null);
        simulateFolderSyncChangeHandling(accountSelector, box2 /*box3's new parent*/);

        box1 = Mailbox.restoreMailboxWithId(mProviderContext, box1.mId);
        box2 = Mailbox.restoreMailboxWithId(mProviderContext, box2.mId);
        box3 = Mailbox.restoreMailboxWithId(mProviderContext, box3.mId);

        // Confirm flags and parent key(s) as expected
        assertEquals(PARENT_FLAGS, box1.mFlags);        // Should still be a parent
        assertEquals(-1, box1.mParentKey);

        assertEquals(PARENT_FLAGS, box2.mFlags);        // Should now be a parent
        assertEquals(box1.mId, box2.mParentKey);

        assertEquals(CHILD_FLAGS, box3.mFlags);         // Should still be a child (of box2)
        assertEquals(box2.mId, box3.mParentKey);
    }

    /**
     * Test changing to no parent from a parent
     */
    public void testChangeFromParentToNoParent() {
        // Set up account and mailboxes
        mAccount = setupTestAccount("acct1", true);
        String accountSelector = MailboxColumns.ACCOUNT_KEY + " IN (" + mAccount.mId + ")";

        // Initial configuration for this test:  box1 has box2, box3 is also at root.
        Mailbox box1 = EmailContentSetupUtils.setupMailbox(
                "box1", mAccount.mId, true, mProviderContext, Mailbox.TYPE_MAIL);
        Mailbox box2 = EmailContentSetupUtils.setupMailbox(
                "box2", mAccount.mId, true, mProviderContext, Mailbox.TYPE_MAIL, box1);

        // Manually set parentKey to null for all mailboxes, as if an initial sync or post-upgrade
        mResolver.update(Mailbox.CONTENT_URI, mNullParentKey, null, null);

        // Confirm initial configuration as expected
        MailboxUtilities.fixupUninitializedParentKeys(mProviderContext, accountSelector);
        box1 = Mailbox.restoreMailboxWithId(mProviderContext, box1.mId);
        box2 = Mailbox.restoreMailboxWithId(mProviderContext, box2.mId);

        // Confirm flags and parent key(s) as expected
        assertEquals(PARENT_FLAGS, box1.mFlags);
        assertEquals(-1, box1.mParentKey);

        assertEquals(CHILD_FLAGS, box2.mFlags);
        assertEquals(box1.mId, box2.mParentKey);

        // The specific test:  Remove the parent from box2 and check remaining configuration
        ContentValues values = new ContentValues();
        values.putNull(Mailbox.PARENT_SERVER_ID);
        mResolver.update(ContentUris.withAppendedId(Mailbox.CONTENT_URI, box2.mId), values,
                null, null);
        // Note: FolderSync handling of changed folder would cause parent key to be uninitialized
        // so we do so here
        mResolver.update(ContentUris.withAppendedId(Mailbox.CONTENT_URI, box2.mId), mNullParentKey,
                null, null);
        simulateFolderSyncChangeHandling(accountSelector, box1 /*box2's old parent*/);

        box1 = Mailbox.restoreMailboxWithId(mProviderContext, box1.mId);
        box2 = Mailbox.restoreMailboxWithId(mProviderContext, box2.mId);

        // Confirm flags and parent key(s) as expected
        assertEquals(CHILD_FLAGS, box1.mFlags);        // Should no longer be a parent
        assertEquals(-1, box1.mParentKey);

        assertEquals(CHILD_FLAGS, box2.mFlags);        // Should still be a child (no parent)
        assertEquals(-1, box2.mParentKey);
    }

    /**
     * Test a mailbox that has no server id (Hotmail Outbox is an example of this)
     */
    public void testNoServerId() {
        // Set up account and mailboxes
        mAccount = setupTestAccount("acct1", true);
        String accountSelector = MailboxColumns.ACCOUNT_KEY + " IN (" + mAccount.mId + ")";

        // Initial configuration for this test:  box1 has no serverId, box2 is a child of box1
        Mailbox box1 = EmailContentSetupUtils.setupMailbox(
                "box1", mAccount.mId, false, mProviderContext, Mailbox.TYPE_MAIL);
        box1.mServerId = null;
        box1.save(mProviderContext);
        Mailbox box2 = EmailContentSetupUtils.setupMailbox(
                "box2", mAccount.mId, true, mProviderContext, Mailbox.TYPE_OUTBOX, box1);

        // Manually set parentKey to null for all mailboxes, as if an initial sync or post-upgrade
        mResolver.update(Mailbox.CONTENT_URI, mNullParentKey, null, null);

        // Confirm initial configuration as expected
        MailboxUtilities.fixupUninitializedParentKeys(mProviderContext, accountSelector);
        box1 = Mailbox.restoreMailboxWithId(mProviderContext, box1.mId);
        box2 = Mailbox.restoreMailboxWithId(mProviderContext, box2.mId);

        // Box 1 should be a child, even though it is defined as the parent of box2, because it
        // has no serverId (presumably, this case can't happen, because a child stores the parent's
        // serverId, but it's nice to know it's handled properly). Box 1 should have no parent.
        assertEquals(Mailbox.NO_MAILBOX, box1.mParentKey);
        assertEquals(CHILD_FLAGS, box1.mFlags);
        // Box 2 should be a child with no parent (see above).  Since it's an outbox, the flags are
        // only "holds mail".
        assertEquals(Mailbox.NO_MAILBOX, box2.mParentKey);
        assertEquals(Mailbox.FLAG_HOLDS_MAIL, box2.mFlags);
    }

    /**
     * Test changing a parent from one mailbox to another
     */
    public void testChangeParent() {
        // Set up account and mailboxes
        mAccount = setupTestAccount("acct1", true);
        String accountSelector = MailboxColumns.ACCOUNT_KEY + " IN (" + mAccount.mId + ")";

        // Initial configuration for this test:  box1 has box2, box3 is also at root.
        Mailbox box1 = EmailContentSetupUtils.setupMailbox(
                "box1", mAccount.mId, true, mProviderContext, Mailbox.TYPE_MAIL);
        Mailbox box2 = EmailContentSetupUtils.setupMailbox(
                "box2", mAccount.mId, true, mProviderContext, Mailbox.TYPE_MAIL);
        Mailbox box3 = EmailContentSetupUtils.setupMailbox(
                "box3", mAccount.mId, true, mProviderContext, Mailbox.TYPE_MAIL, box1);

        // Manually set parentKey to null for all mailboxes, as if an initial sync or post-upgrade
        mResolver.update(Mailbox.CONTENT_URI, mNullParentKey, null, null);

        // Confirm initial configuration as expected
        MailboxUtilities.fixupUninitializedParentKeys(mProviderContext, accountSelector);
        box1 = Mailbox.restoreMailboxWithId(mProviderContext, box1.mId);
        box2 = Mailbox.restoreMailboxWithId(mProviderContext, box2.mId);
        box3 = Mailbox.restoreMailboxWithId(mProviderContext, box3.mId);

        // Confirm flags and parent key(s) as expected
        assertEquals(PARENT_FLAGS, box1.mFlags);
        assertEquals(-1, box1.mParentKey);

        assertEquals(CHILD_FLAGS, box2.mFlags);
        assertEquals(-1, box2.mParentKey);

        assertEquals(CHILD_FLAGS, box3.mFlags);
        assertEquals(box1.mId, box3.mParentKey);

        // The specific test:  Give box 3 a new parent (box 2) and check remaining configuration
        ContentValues values = new ContentValues();
        values.put(Mailbox.PARENT_SERVER_ID, box2.mServerId);
        mResolver.update(ContentUris.withAppendedId(Mailbox.CONTENT_URI, box3.mId), values,
                null, null);
        // Changes to old and new parent
        simulateFolderSyncChangeHandling(accountSelector, box2 /*box3's new parent*/,
                box1 /*box3's old parent*/);

        box1 = Mailbox.restoreMailboxWithId(mProviderContext, box1.mId);
        box2 = Mailbox.restoreMailboxWithId(mProviderContext, box2.mId);
        box3 = Mailbox.restoreMailboxWithId(mProviderContext, box3.mId);

        // Confirm flags and parent key(s) as expected
        assertEquals(CHILD_FLAGS, box1.mFlags);        // Should no longer be a parent
        assertEquals(-1, box1.mParentKey);

        assertEquals(PARENT_FLAGS, box2.mFlags);        // Should now be a parent
        assertEquals(-1, box2.mParentKey);

        assertEquals(CHILD_FLAGS, box3.mFlags);         // Should still be a child (of box2)
        assertEquals(box2.mId, box3.mParentKey);
    }


    /**
     * Tests the proper separation of two accounts using the methodology from the previous test.
     * This test will fail if MailboxUtilities fails to distinguish between mailboxes in different
     * accounts that happen to have the same serverId
     */
    public void testChangeParentTwoAccounts() {
        // Set up account and mailboxes
        mAccount = setupTestAccount("acct1", true);
        Account acct2 = setupTestAccount("acct2", true);

        String accountSelector1 = MailboxColumns.ACCOUNT_KEY + " IN (" + mAccount.mId + ")";
        String accountSelector2 = MailboxColumns.ACCOUNT_KEY + " IN (" + acct2.mId + ")";

        // Box3 is in Box1
        Mailbox box1 = EmailContentSetupUtils.setupMailbox(
                "box1", mAccount.mId, false, mProviderContext, Mailbox.TYPE_MAIL);
        box1.mServerId = "1:1";
        box1.save(mProviderContext);
        Mailbox box2 = EmailContentSetupUtils.setupMailbox(
                "box2", mAccount.mId, false, mProviderContext, Mailbox.TYPE_MAIL);
        box2.mServerId = "1:2";
        box2.save(mProviderContext);
        Mailbox box3 = EmailContentSetupUtils.setupMailbox(
                "box3", mAccount.mId, false, mProviderContext, Mailbox.TYPE_MAIL, box1);
        box3.mServerId = "1:3";
        box3.save(mProviderContext);

        // Box5 is in Box4; Box 6 is in Box5
        // Note that the three serverId's are identical to those in acct1; we want to make sure
        // that children get associated only with boxes in their own account
        Mailbox box4 = EmailContentSetupUtils.setupMailbox(
                "box4", acct2.mId, false, mProviderContext, Mailbox.TYPE_MAIL, null);
        box4.mServerId = "1:1";
        box4.save(mProviderContext);
        Mailbox box5 = EmailContentSetupUtils.setupMailbox(
                "box5", acct2.mId, false, mProviderContext, Mailbox.TYPE_MAIL, box4);
        box5.mServerId = "1:2";
        box5.save(mProviderContext);
        Mailbox box6 = EmailContentSetupUtils.setupMailbox(
                "box6", acct2.mId, false, mProviderContext, Mailbox.TYPE_MAIL, box5);
        box6.mServerId = "1:3";
        box6.save(mProviderContext);

        // Manually set parentKey to null for all mailboxes, as if an initial sync or post-upgrade
        mResolver.update(Mailbox.CONTENT_URI, mNullParentKey, null, null);

        // Confirm initial configuration as expected for mAccount
        MailboxUtilities.fixupUninitializedParentKeys(mProviderContext, accountSelector1);
        box1 = Mailbox.restoreMailboxWithId(mProviderContext, box1.mId);
        box2 = Mailbox.restoreMailboxWithId(mProviderContext, box2.mId);
        box3 = Mailbox.restoreMailboxWithId(mProviderContext, box3.mId);

        // Confirm flags and parent key(s) as expected
        assertEquals(PARENT_FLAGS, box1.mFlags);
        assertEquals(-1, box1.mParentKey);

        assertEquals(CHILD_FLAGS, box2.mFlags);
        assertEquals(-1, box2.mParentKey);

        assertEquals(CHILD_FLAGS, box3.mFlags);
        assertEquals(box1.mId, box3.mParentKey);

        // Confirm initial configuration as expected for acct2
        MailboxUtilities.fixupUninitializedParentKeys(mProviderContext, accountSelector2);
        box4 = Mailbox.restoreMailboxWithId(mProviderContext, box4.mId);
        box5 = Mailbox.restoreMailboxWithId(mProviderContext, box5.mId);
        box6 = Mailbox.restoreMailboxWithId(mProviderContext, box6.mId);

        // Confirm flags and parent key(s) as expected
        assertEquals(PARENT_FLAGS, box4.mFlags);
        assertEquals(-1, box4.mParentKey);

        assertEquals(PARENT_FLAGS, box5.mFlags);
        assertEquals(box4.mId, box5.mParentKey);

        assertEquals(CHILD_FLAGS, box6.mFlags);
        assertEquals(box5.mId, box6.mParentKey);

        // The specific test:  Change box1 to have a different serverId
        ContentValues values = new ContentValues();
        values.put(MailboxColumns.SERVER_ID, "1:4");
        mResolver.update(ContentUris.withAppendedId(Mailbox.CONTENT_URI, box1.mId), values,
                null, null);
        // Manually set parentKey to null for all mailboxes, as if an initial sync or post-upgrade
        mResolver.update(Mailbox.CONTENT_URI, mNullParentKey, null, null);
        // Fix up the parent keys
        MailboxUtilities.fixupUninitializedParentKeys(mProviderContext, accountSelector1);

        // Make sure that box1 reflects the change properly AND that other boxes remain correct
        // The reason for all of the seemingly-duplicated tests is to make sure that the fixup of
        // any account doesn't end up affecting the other account's mailboxes
        box1 = Mailbox.restoreMailboxWithId(mProviderContext, box1.mId);
        box2 = Mailbox.restoreMailboxWithId(mProviderContext, box2.mId);
        box3 = Mailbox.restoreMailboxWithId(mProviderContext, box3.mId);

        // Confirm flags and parent key(s) as expected
        assertEquals(CHILD_FLAGS, box1.mFlags);
        assertEquals(-1, box1.mParentKey);

        assertEquals(CHILD_FLAGS, box2.mFlags);
        assertEquals(-1, box2.mParentKey);

        assertEquals(CHILD_FLAGS, box3.mFlags);
        assertEquals(-1, box3.mParentKey);

        // Fix up the 2nd account now, and check that ALL boxes are correct
        MailboxUtilities.fixupUninitializedParentKeys(mProviderContext, accountSelector2);

        box1 = Mailbox.restoreMailboxWithId(mProviderContext, box1.mId);
        box2 = Mailbox.restoreMailboxWithId(mProviderContext, box2.mId);
        box3 = Mailbox.restoreMailboxWithId(mProviderContext, box3.mId);

        // Confirm flags and parent key(s) as expected
        assertEquals(CHILD_FLAGS, box1.mFlags);
        assertEquals(-1, box1.mParentKey);

        assertEquals(CHILD_FLAGS, box2.mFlags);
        assertEquals(-1, box2.mParentKey);

        assertEquals(CHILD_FLAGS, box3.mFlags);
        assertEquals(-1, box3.mParentKey);

        box4 = Mailbox.restoreMailboxWithId(mProviderContext, box4.mId);
        box5 = Mailbox.restoreMailboxWithId(mProviderContext, box5.mId);
        box6 = Mailbox.restoreMailboxWithId(mProviderContext, box6.mId);

        assertEquals(PARENT_FLAGS, box4.mFlags);
        assertEquals(-1, box4.mParentKey);

        assertEquals(PARENT_FLAGS, box5.mFlags);
        assertEquals(box4.mId, box5.mParentKey);

        assertEquals(CHILD_FLAGS, box6.mFlags);
        assertEquals(box5.mId, box6.mParentKey);
    }
}
