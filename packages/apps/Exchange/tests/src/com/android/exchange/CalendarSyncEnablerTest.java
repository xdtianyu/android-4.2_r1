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

package com.android.exchange;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.CalendarContract;
import android.test.MoreAsserts;
import android.test.suitebuilder.annotation.MediumTest;
import android.text.TextUtils;
import android.util.Log;

import com.android.emailcommon.AccountManagerTypes;
import com.android.emailcommon.Logging;
import com.android.exchange.utility.ExchangeTestCase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
@MediumTest
public class CalendarSyncEnablerTest extends ExchangeTestCase {

    protected static final String TEST_ACCOUNT_PREFIX = "__test";
    protected static final String TEST_ACCOUNT_SUFFIX = "@android.com";

    private HashMap<Account, Boolean> origCalendarSyncStates = new HashMap<Account, Boolean>();

    // To make the rest of the code shorter thus more readable...
    private static final String EAT = AccountManagerTypes.TYPE_EXCHANGE;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // Delete any test accounts we might have created earlier
        deleteTemporaryAccountManagerAccounts();

        // Save the original calendar sync states.
        for (Account account : AccountManager.get(getContext()).getAccounts()) {
            origCalendarSyncStates.put(account,
                    ContentResolver.getSyncAutomatically(account, CalendarContract.AUTHORITY));
        }
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        // Delete any test accounts we might have created earlier
        deleteTemporaryAccountManagerAccounts();

        // Restore the original calendar sync states.
        // Note we restore only for Exchange accounts.
        // Other accounts should remain intact throughout the tests.  Plus we don't know if the
        // Calendar.AUTHORITY is supported by other types of accounts.
        for (Account account : getExchangeAccounts()) {
            Boolean state = origCalendarSyncStates.get(account);
            if (state == null) continue; // Shouldn't happen, but just in case.

            ContentResolver.setSyncAutomatically(account, CalendarContract.AUTHORITY, state);
        }
    }

    public void testEnableEasCalendarSync() {
        final Account[] baseAccounts = getExchangeAccounts();

        String a1 = getTestAccountEmailAddress("1");
        String a2 = getTestAccountEmailAddress("2");

        // 1. Test with 1 account

        CalendarSyncEnabler enabler = new CalendarSyncEnabler(getContext());

        // Add exchange accounts
        createAccountManagerAccount(a1);

        String emailAddresses = enabler.enableEasCalendarSyncInternalForTest();

        // Verify
        verifyCalendarSyncState();

        // There seems to be no good way to examine the contents of Notification, so let's verify
        // we at least (tried to) show the correct email addresses.
        checkNotificationEmailAddresses(emailAddresses, baseAccounts, a1);

        // Delete added account.
        deleteTemporaryAccountManagerAccounts();

        // 2. Test with 2 accounts
        enabler = new CalendarSyncEnabler(getContext());

        // Add exchange accounts
        createAccountManagerAccount(a1);
        createAccountManagerAccount(a2);

        emailAddresses = enabler.enableEasCalendarSyncInternalForTest();

        // Verify
        verifyCalendarSyncState();

        // Check
        checkNotificationEmailAddresses(emailAddresses, baseAccounts, a1, a2);
    }

    private static void checkNotificationEmailAddresses(String actual, Account[] baseAccounts,
            String... addedAddresses) {
        // Build and sort actual string array.
        final String[] actualArray = TextUtils.split(actual, " ");
        Arrays.sort(actualArray);

        // Build and sort expected string array.
        ArrayList<String> expected = new ArrayList<String>();
        for (Account account : baseAccounts) {
            expected.add(account.name);
        }
        for (String address : addedAddresses) {
            expected.add(address);
        }
        final String[] expectedArray = new String[expected.size()];
        expected.toArray(expectedArray);
        Arrays.sort(expectedArray);

        // Check!
        MoreAsserts.assertEquals(expectedArray, actualArray);
    }

    /**
     * For all {@link Account}, confirm that:
     * <ol>
     *   <li>Calendar sync is enabled if it's an Exchange account.<br>
     *       Unfortunately setSyncAutomatically() doesn't take effect immediately, so we skip this
     *       check for now.
             TODO Find a stable way to check this.
     *   <li>Otherwise, calendar sync state isn't changed.
     * </ol>
     */
    private void verifyCalendarSyncState() {
        // It's very unfortunate that setSyncAutomatically doesn't take effect immediately.
        for (Account account : AccountManager.get(getContext()).getAccounts()) {
            String message = "account=" + account.name + "(" + account.type + ")";
            boolean enabled = ContentResolver.getSyncAutomatically(account,
                    CalendarContract.AUTHORITY);
            int syncable = ContentResolver.getIsSyncable(account, CalendarContract.AUTHORITY);

            if (EAT.equals(account.type)) {
                // Should be enabled.
                // assertEquals(message, Boolean.TRUE, (Boolean) enabled);
                // assertEquals(message, 1, syncable);
            } else {
                // Shouldn't change.
                assertEquals(message, origCalendarSyncStates.get(account), (Boolean) enabled);
            }
        }
    }

    public void testEnableEasCalendarSyncWithNoExchangeAccounts() {
        // This test can only meaningfully run when there's no exchange accounts
        // set up on the device.  Otherwise there'll be no difference from
        // testEnableEasCalendarSync.
        if (AccountManager.get(getContext()).getAccountsByType(EAT).length > 0) {
            Log.w(Logging.LOG_TAG, "testEnableEasCalendarSyncWithNoExchangeAccounts skipped:"
                    + " It only runs when there's no Exchange account on the device.");
            return;
        }
        CalendarSyncEnabler enabler = new CalendarSyncEnabler(getContext());
        String emailAddresses = enabler.enableEasCalendarSyncInternalForTest();

        // Verify (nothing should change)
        verifyCalendarSyncState();

        // No exchange accounts found.
        assertEquals(0, emailAddresses.length());
    }

    public void testShowNotification() {
        CalendarSyncEnabler enabler = new CalendarSyncEnabler(getContext());

        // We can't really check the result, but at least we can make sure it won't crash....
        enabler.showNotificationForTest("a@b.com");

        // Remove the notification.  Comment it out when you want to know how it looks like.
        // TODO If NotificationController supports this notification, we can just mock it out
        // and remove this code.
        ((NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE))
                .cancel(CalendarSyncEnabler.NOTIFICATION_ID_EXCHANGE_CALENDAR_ADDED);
    }

    protected Account[] getExchangeAccounts() {
        return AccountManager.get(getContext()).getAccountsByType(
                AccountManagerTypes.TYPE_EXCHANGE);
    }

    protected Account makeAccountManagerAccount(String username) {
        return new Account(username, AccountManagerTypes.TYPE_EXCHANGE);
    }

    protected void createAccountManagerAccount(String username) {
        final Account account = makeAccountManagerAccount(username);
        AccountManager.get(getContext()).addAccountExplicitly(account, "password", null);
    }

    protected com.android.emailcommon.provider.Account
        setupProviderAndAccountManagerAccount(String username) {
        // Note that setupAccount creates the email address username@android.com, so that's what
        // we need to use for the account manager
        createAccountManagerAccount(username + TEST_ACCOUNT_SUFFIX);
        return setupTestAccount(username, true);
    }

    protected ArrayList<com.android.emailcommon.provider.Account> makeExchangeServiceAccountList() {
        ArrayList<com.android.emailcommon.provider.Account> accountList =
            new ArrayList<com.android.emailcommon.provider.Account>();
        Cursor c = mProviderContext.getContentResolver().query(
                com.android.emailcommon.provider.Account.CONTENT_URI,
                com.android.emailcommon.provider.Account.CONTENT_PROJECTION, null, null, null);
        try {
            while (c.moveToNext()) {
                com.android.emailcommon.provider.Account account =
                    new com.android.emailcommon.provider.Account();
                account.restore(c);
                accountList.add(account);
            }
        } finally {
            c.close();
        }
        return accountList;
    }

    protected void deleteAccountManagerAccount(Account account) {
        AccountManagerFuture<Boolean> future =
            AccountManager.get(getContext()).removeAccount(account, null, null);
        try {
            future.getResult();
        } catch (OperationCanceledException e) {
        } catch (AuthenticatorException e) {
        } catch (IOException e) {
        }
    }

    protected void deleteTemporaryAccountManagerAccounts() {
        for (Account accountManagerAccount: getExchangeAccounts()) {
            if (accountManagerAccount.name.startsWith(TEST_ACCOUNT_PREFIX) &&
                    accountManagerAccount.name.endsWith(TEST_ACCOUNT_SUFFIX)) {
                deleteAccountManagerAccount(accountManagerAccount);
            }
        }
    }

    protected String getTestAccountName(String name) {
        return TEST_ACCOUNT_PREFIX + name;
    }

    protected String getTestAccountEmailAddress(String name) {
        return TEST_ACCOUNT_PREFIX + name + TEST_ACCOUNT_SUFFIX;
    }


    /**
     * Helper to retrieve account manager accounts *and* remove any preexisting accounts
     * from the list, to "hide" them from the reconciler.
     */
    protected Account[] getAccountManagerAccounts(Account[] baseline) {
        Account[] rawList = getExchangeAccounts();
        if (baseline.length == 0) {
            return rawList;
        }
        HashSet<Account> set = new HashSet<Account>();
        for (Account addAccount : rawList) {
            set.add(addAccount);
        }
        for (Account removeAccount : baseline) {
            set.remove(removeAccount);
        }
        return set.toArray(new Account[0]);
    }
}
