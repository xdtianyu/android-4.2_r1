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
import com.android.emailcommon.provider.EmailContent.Message;
import com.android.emailcommon.provider.Mailbox;

import android.content.Context;

/**
 * Simplified EmailContent class setup (condensed from ProviderTestUtils in com.android.email)
 */
public class EmailContentSetupUtils {

    /**
     * No constructor - statics only
     */
    private EmailContentSetupUtils() {
    }

    /**
     * Create an account for test purposes
     */
    public static Account setupAccount(String name, boolean saveIt, Context context) {
        Account account = new Account();

        account.mDisplayName = name;
        account.mEmailAddress = name + "@android.com";
        account.mProtocolVersion = "2.5" + name;
        if (saveIt) {
            account.save(context);
        }
        return account;
    }

    /**
     * Create a mailbox for test purposes
     */
    public static Mailbox setupMailbox(String name, long accountId, boolean saveIt,
            Context context) {
        return setupMailbox(name, accountId, saveIt, context, Mailbox.TYPE_MAIL, null);
    }

    public static Mailbox setupMailbox(String name, long accountId, boolean saveIt,
            Context context, int type) {
        return setupMailbox(name, accountId, saveIt, context, type, null);
    }

    public static Mailbox setupMailbox(String name, long accountId, boolean saveIt,
            Context context, int type, Mailbox parentBox) {
        Mailbox box = new Mailbox();

        box.mDisplayName = name;
        box.mAccountKey = accountId;
        box.mSyncKey = "sync-key-" + name;
        box.mSyncLookback = 2;
        box.mSyncInterval = Account.CHECK_INTERVAL_NEVER;
        box.mType = type;
        box.mServerId = "serverid-" + name;
        box.mParentServerId = parentBox != null ? parentBox.mServerId : "parent-serverid-" + name;

        if (saveIt) {
            box.save(context);
        }
        return box;
    }

    /**
     * Create a message for test purposes
     */
    public static Message setupMessage(String name, long accountId, long mailboxId,
            boolean addBody, boolean saveIt, Context context) {
        // Default starred, read,  (backword compatibility)
        return setupMessage(name, accountId, mailboxId, addBody, saveIt, context, true, true);
    }

    /**
     * Create a message for test purposes
     */
    public static Message setupMessage(String name, long accountId, long mailboxId,
            boolean addBody, boolean saveIt, Context context, boolean starred, boolean read) {
        Message message = new Message();

        message.mDisplayName = name;
        message.mMailboxKey = mailboxId;
        message.mAccountKey = accountId;
        message.mFlagRead = read;
        message.mFlagLoaded = Message.FLAG_LOADED_UNLOADED;
        message.mFlagFavorite = starred;
        message.mServerId = "serverid " + name;

        if (addBody) {
            message.mText = "body text " + name;
            message.mHtml = "body html " + name;
        }

        if (saveIt) {
            message.save(context);
        }
        return message;
    }
}
