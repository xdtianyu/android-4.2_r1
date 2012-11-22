/*
 * Copyright (C) 2009 The Android Open Source Project
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
package com.android.quicksearchbox;

import com.android.quicksearchbox.util.MockExecutor;
import com.android.quicksearchbox.util.Util;

import org.json.JSONArray;

import android.app.SearchManager;
import android.content.Intent;
import android.test.AndroidTestCase;
import android.test.MoreAsserts;
import android.test.suitebuilder.annotation.MediumTest;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Abstract base class for tests of  {@link ShortcutRepository}
 * implementations.  Most importantly, verifies the
 * stuff we are doing with sqlite works how we expect it to.
 *
 * Attempts to test logic independent of the (sql) details of the implementation, so these should
 * be useful even in the face of a schema change.
 */
@MediumTest
public class ShortcutRepositoryTest extends AndroidTestCase {

    private static final String TAG = "ShortcutRepositoryTest";

    static final long NOW = 1239841162000L; // millis since epoch. some time in 2009

    static final Source APP_SOURCE = new MockSource("com.example.app/.App");

    static final Source APP_SOURCE_V2 = new MockSource("com.example.app/.App", 2);

    static final Source CONTACTS_SOURCE = new MockSource("com.android.contacts/.Contacts");

    static final Source BOOKMARKS_SOURCE = new MockSource("com.android.browser/.Bookmarks");

    static final Source HISTORY_SOURCE = new MockSource("com.android.browser/.History");

    static final Source MUSIC_SOURCE = new MockSource("com.android.music/.Music");

    static final Source MARKET_SOURCE = new MockSource("com.android.vending/.Market");

    static final Corpus APP_CORPUS = new MockCorpus(APP_SOURCE);

    static final Corpus CONTACTS_CORPUS = new MockCorpus(CONTACTS_SOURCE);

    static final Corpus WEB_CORPUS = new MockCorpus(MockSource.WEB_SOURCE);

    static final int MAX_SHORTCUTS = 8;

    protected Config mConfig;
    protected MockCorpora mCorpora;
    protected MockExecutor mLogExecutor;
    protected ShortcutRefresher mRefresher;

    protected List<Corpus> mAllowedCorpora;

    protected ShortcutRepositoryImplLog mRepo;

    protected ListSuggestionCursor mAppSuggestions;
    protected ListSuggestionCursor mContactSuggestions;

    protected SuggestionData mApp1;
    protected SuggestionData mApp2;
    protected SuggestionData mApp3;

    protected SuggestionData mContact1;
    protected SuggestionData mContact2;

    protected SuggestionData mWeb1;

    protected ShortcutRepositoryImplLog createShortcutRepository() {
        return new ShortcutRepositoryImplLog(getContext(), mConfig, mCorpora,
                mRefresher, new MockHandler(), mLogExecutor,
                "test-shortcuts-log.db");
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        mConfig = new Config(getContext());
        mCorpora = new MockCorpora();
        mCorpora.addCorpus(APP_CORPUS);
        mCorpora.addCorpus(CONTACTS_CORPUS);
        mCorpora.addCorpus(WEB_CORPUS);
        mRefresher = new MockShortcutRefresher();
        mLogExecutor = new MockExecutor();
        mRepo = createShortcutRepository();

        mAllowedCorpora = new ArrayList<Corpus>(mCorpora.getAllCorpora());

        mApp1 = makeApp("app1");
        mApp2 = makeApp("app2");
        mApp3 = makeApp("app3");
        mAppSuggestions = new ListSuggestionCursor("foo", mApp1, mApp2, mApp3);

        mContact1 = new SuggestionData(CONTACTS_SOURCE)
                .setText1("Joe Blow")
                .setIntentAction("view")
                .setIntentData("contacts/joeblow")
                .setShortcutId("j-blow");
        mContact2 = new SuggestionData(CONTACTS_SOURCE)
                .setText1("Mike Johnston")
                .setIntentAction("view")
                .setIntentData("contacts/mikeJ")
                .setShortcutId("mo-jo");

        mWeb1 = new SuggestionData(MockSource.WEB_SOURCE)
                .setText1("foo")
                .setIntentAction(Intent.ACTION_WEB_SEARCH)
                .setSuggestionQuery("foo");

        mContactSuggestions = new ListSuggestionCursor("foo", mContact1, mContact2);
    }

    private SuggestionData makeApp(String name) {
        return new SuggestionData(APP_SOURCE)
                .setText1(name)
                .setIntentAction("view")
                .setIntentData("apps/" + name)
                .setShortcutId("shorcut_" + name);
    }

    private SuggestionData makeContact(String name) {
        return new SuggestionData(CONTACTS_SOURCE)
                .setText1(name)
                .setIntentAction("view")
                .setIntentData("contacts/" + name)
                .setShortcutId("shorcut_" + name);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        mRepo.deleteRepository();
    }

    public void testHasHistory() {
        assertHasHistory(false);
        reportClickAtTime(mAppSuggestions, 0, NOW);
        assertHasHistory(true);
        mRepo.clearHistory();
        mLogExecutor.runNext();
        assertHasHistory(false);
    }

    public void testRemoveFromHistory() {
        SuggestionData john = new SuggestionData(CONTACTS_SOURCE)
                .setText1("john doe")
                .setIntentAction("view")
                .setIntentData("john_doe");
        SuggestionData jane = new SuggestionData(CONTACTS_SOURCE)
                .setText1("jane doe")
                .setIntentAction("view")
                .setIntentData("jane_doe");
        reportClick("j", john);
        reportClick("j", john);
        reportClick("j", jane);
        assertShortcuts("j", john, jane);
        removeFromHistory(new ListSuggestionCursor("j", jane, john), 1);
        assertShortcuts("j", jane);
    }

    public void testRemoveFromHistoryNonExisting() {
        SuggestionData john = new SuggestionData(CONTACTS_SOURCE)
                .setText1("john doe")
                .setIntentAction("view")
                .setIntentData("john_doe");
        SuggestionData jane = new SuggestionData(CONTACTS_SOURCE)
                .setText1("jane doe")
                .setIntentAction("view")
                .setIntentData("jane_doe");
        reportClick("j", john);
        assertShortcuts("j", john);
        removeFromHistory(new ListSuggestionCursor("j", jane), 0);
        assertShortcuts("j", john);
    }

    public void testNoMatch() {
        SuggestionData clicked = new SuggestionData(CONTACTS_SOURCE)
                .setText1("bob smith")
                .setIntentAction("action")
                .setIntentData("data");

        reportClick("bob smith", clicked);
        assertNoShortcuts("joe");
    }

    public void testFullPackingUnpacking() {
        SuggestionData clicked = new SuggestionData(CONTACTS_SOURCE)
                .setFormat("<i>%s</i>")
                .setText1("title")
                .setText2("description")
                .setText2Url("description_url")
                .setIcon1("android.resource://system/drawable/foo")
                .setIcon2("content://test/bar")
                .setIntentAction("action")
                .setIntentData("data")
                .setSuggestionQuery("query")
                .setIntentExtraData("extradata")
                .setShortcutId("idofshortcut")
                .setSuggestionLogType("logtype");
        reportClick("q", clicked);

        assertShortcuts("q", clicked);
        assertShortcuts("", clicked);
    }

    public void testSpinnerWhileRefreshing() {
        SuggestionData clicked = new SuggestionData(CONTACTS_SOURCE)
                .setText1("title")
                .setText2("description")
                .setIcon2("icon2")
                .setSuggestionQuery("query")
                .setIntentExtraData("extradata")
                .setShortcutId("idofshortcut")
                .setSpinnerWhileRefreshing(true);

        reportClick("q", clicked);

        String spinnerUri = Util.getResourceUri(mContext, R.drawable.search_spinner).toString();
        SuggestionData expected = new SuggestionData(CONTACTS_SOURCE)
                .setText1("title")
                .setText2("description")
                .setIcon2(spinnerUri)
                .setSuggestionQuery("query")
                .setIntentExtraData("extradata")
                .setShortcutId("idofshortcut")
                .setSpinnerWhileRefreshing(true);

        assertShortcuts("q", expected);
    }

    public void testPrefixesMatch() {
        assertNoShortcuts("bob");

        SuggestionData clicked = new SuggestionData(CONTACTS_SOURCE)
                .setText1("bob smith the third")
                .setIntentAction("action")
                .setIntentData("intentdata");

        reportClick("bob smith", clicked);

        assertShortcuts("bob smith", clicked);
        assertShortcuts("bob s", clicked);
        assertShortcuts("b", clicked);
    }

    public void testMatchesOneAndNotOthers() {
        SuggestionData bob = new SuggestionData(CONTACTS_SOURCE)
                .setText1("bob smith the third")
                .setIntentAction("action")
                .setIntentData("intentdata/bob");

        reportClick("bob", bob);

        SuggestionData george = new SuggestionData(CONTACTS_SOURCE)
                .setText1("george jones")
                .setIntentAction("action")
                .setIntentData("intentdata/george");
        reportClick("geor", george);

        assertShortcuts("b for bob", "b", bob);
        assertShortcuts("g for george", "g", george);
    }

    public void testDifferentPrefixesMatchSameEntity() {
        SuggestionData clicked = new SuggestionData(CONTACTS_SOURCE)
                .setText1("bob smith the third")
                .setIntentAction("action")
                .setIntentData("intentdata");

        reportClick("bob", clicked);
        reportClick("smith", clicked);
        assertShortcuts("b", clicked);
        assertShortcuts("s", clicked);
    }

    public void testMoreClicksWins() {
        reportClick("app", mApp1);
        reportClick("app", mApp2);
        reportClick("app", mApp1);

        assertShortcuts("expected app1 to beat app2 since it has more hits",
                "app", mApp1, mApp2);

        reportClick("app", mApp2);
        reportClick("app", mApp2);

        assertShortcuts("query 'app': expecting app2 to beat app1 since it has more hits",
                "app", mApp2, mApp1);
        assertShortcuts("query 'a': expecting app2 to beat app1 since it has more hits",
                "a", mApp2, mApp1);
    }

    public void testMostRecentClickWins() {
        // App 1 has 3 clicks
        reportClick("app", mApp1, NOW - 5);
        reportClick("app", mApp1, NOW - 5);
        reportClick("app", mApp1, NOW - 5);
        // App 2 has 2 clicks
        reportClick("app", mApp2, NOW - 2);
        reportClick("app", mApp2, NOW - 2);
        // App 3 only has 1, but it's most recent
        reportClick("app", mApp3, NOW - 1);

        assertShortcuts("expected app3 to beat app1 and app2 because it's clicked last",
                "app", mApp3, mApp1, mApp2);

        reportClick("app", mApp2, NOW);

        assertShortcuts("query 'app': expecting app2 to beat app1 since it's clicked last",
                "app", mApp2, mApp1, mApp3);
        assertShortcuts("query 'a': expecting app2 to beat app1 since it's clicked last",
                "a", mApp2, mApp1, mApp3);
        assertShortcuts("query '': expecting app2 to beat app1 since it's clicked last",
                "", mApp2, mApp1, mApp3);
    }

    public void testMostRecentClickWinsOnEmptyQuery() {
        reportClick("app", mApp1, NOW - 3);
        reportClick("app", mApp1, NOW - 2);
        reportClick("app", mApp2, NOW - 1);

        assertShortcuts("expected app2 to beat app1 since it's clicked last", "",
                mApp2, mApp1);
    }

    public void testMostRecentClickWinsEvenWithMoreThanLimitShortcuts() {
        for (int i = 0; i < MAX_SHORTCUTS; i++) {
            SuggestionData app = makeApp("TestApp" + i);
            // Each of these shortcuts has two clicks
            reportClick("app", app, NOW - 2);
            reportClick("app", app, NOW - 1);
        }

        // mApp1 has only one click, but is more recent
        reportClick("app", mApp1, NOW);

        assertShortcutAtPosition(
            "expecting app1 to beat all others since it's clicked last",
            "app", 0, mApp1);
    }

    /**
     * similar to {@link #testMoreClicksWins()} but clicks are reported with prefixes of the
     * original query.  we want to make sure a match on query 'a' updates the stats for the
     * entry it matched against, 'app'.
     */
    public void testPrefixMatchUpdatesSameEntry() {
        reportClick("app", mApp1, NOW);
        reportClick("app", mApp2, NOW);
        reportClick("app", mApp1, NOW);

        assertShortcuts("expected app1 to beat app2 since it has more hits",
                "app", mApp1, mApp2);
    }

    private static final long DAY_MILLIS = 86400000L; // just ask the google
    private static final long HOUR_MILLIS = 3600000L;

    public void testMoreRecentlyClickedWins() {
        reportClick("app", mApp1, NOW - DAY_MILLIS*2);
        reportClick("app", mApp2, NOW);
        reportClick("app", mApp3, NOW - DAY_MILLIS*4);

        assertShortcuts("expecting more recently clicked app to rank higher",
                "app", mApp2, mApp1, mApp3);
    }

    public void testMoreRecentlyClickedWinsSeconds() {
        reportClick("app", mApp1, NOW - 10000);
        reportClick("app", mApp2, NOW - 5000);
        reportClick("app", mApp3, NOW);

        assertShortcuts("expecting more recently clicked app to rank higher",
                "app", mApp3, mApp2, mApp1);
    }

    public void testRecencyOverridesClicks() {

        // 5 clicks, most recent half way through age limit
        long halfWindow = mConfig.getMaxStatAgeMillis() / 2;
        reportClick("app", mApp1, NOW - halfWindow);
        reportClick("app", mApp1, NOW - halfWindow);
        reportClick("app", mApp1, NOW - halfWindow);
        reportClick("app", mApp1, NOW - halfWindow);
        reportClick("app", mApp1, NOW - halfWindow);

        // 3 clicks, the most recent very recent
        reportClick("app", mApp2, NOW - HOUR_MILLIS);
        reportClick("app", mApp2, NOW - HOUR_MILLIS);
        reportClick("app", mApp2, NOW - HOUR_MILLIS);

        assertShortcuts("expecting 3 recent clicks to beat 5 clicks long ago",
                "app", mApp2, mApp1);
    }

    public void testEntryOlderThanAgeLimitFiltered() {
        reportClick("app", mApp1);

        long pastWindow = mConfig.getMaxStatAgeMillis() + 1000;
        reportClick("app", mApp2, NOW - pastWindow);

        assertShortcuts("expecting app2 not clicked on recently enough to be filtered",
                "app", mApp1);
    }

    public void testZeroQueryResults_MoreClicksWins() {
        reportClick("app", mApp1);
        reportClick("app", mApp1);
        reportClick("foo", mApp2);

        assertShortcuts("", mApp1, mApp2);

        reportClick("foo", mApp2);
        reportClick("foo", mApp2);

        assertShortcuts("", mApp2, mApp1);
    }

    public void testZeroQueryResults_DifferentQueryhitsCreditSameShortcut() {
        reportClick("app", mApp1);
        reportClick("foo", mApp2);
        reportClick("bar", mApp2);

        assertShortcuts("hits for 'foo' and 'bar' on app2 should have combined to rank it " +
                "ahead of app1, which only has one hit.",
                "", mApp2, mApp1);

        reportClick("z", mApp1);
        reportClick("2", mApp1);

        assertShortcuts("", mApp1, mApp2);
    }

    public void testZeroQueryResults_zeroQueryHitCounts() {
        reportClick("app", mApp1);
        reportClick("", mApp2);
        reportClick("", mApp2);

        assertShortcuts("hits for '' on app2 should have combined to rank it " +
                "ahead of app1, which only has one hit.",
                "", mApp2, mApp1);

        reportClick("", mApp1);
        reportClick("", mApp1);

        assertShortcuts("zero query hits for app1 should have made it higher than app2.",
                "", mApp1, mApp2);

        assertShortcuts("query for 'a' should only match app1.",
                "a", mApp1);
    }

    public void testRefreshShortcut() {
        final SuggestionData app1 = new SuggestionData(APP_SOURCE)
                .setFormat("format")
                .setText1("app1")
                .setText2("cool app")
                .setShortcutId("app1_id");

        reportClick("app", app1);

        final SuggestionData updated = new SuggestionData(APP_SOURCE)
                .setFormat("format (updated)")
                .setText1("app1 (updated)")
                .setText2("cool app")
                .setShortcutId("app1_id");

        refreshShortcut(APP_SOURCE, "app1_id", updated);

        assertShortcuts("expected updated properties in match",
                "app", updated);
    }

    public void testRefreshShortcutChangedIntent() {

        final SuggestionData app1 = new SuggestionData(APP_SOURCE)
                .setIntentData("data")
                .setFormat("format")
                .setText1("app1")
                .setText2("cool app")
                .setShortcutId("app1_id");

        reportClick("app", app1);

        final SuggestionData updated = new SuggestionData(APP_SOURCE)
                .setIntentData("data-updated")
                .setFormat("format (updated)")
                .setText1("app1 (updated)")
                .setText2("cool app")
                .setShortcutId("app1_id");

        refreshShortcut(APP_SOURCE, "app1_id", updated);

        assertShortcuts("expected updated properties in match",
                "app", updated);
    }

    public void testInvalidateShortcut() {
        final SuggestionData app1 = new SuggestionData(APP_SOURCE)
                .setText1("app1")
                .setText2("cool app")
                .setShortcutId("app1_id");

        reportClick("app", app1);

        invalidateShortcut(APP_SOURCE, "app1_id");

        assertNoShortcuts("should be no matches since shortcut is invalid.", "app");
    }

    public void testInvalidateShortcut_sameIdDifferentSources() {
        final String sameid = "same_id";
        final SuggestionData app = new SuggestionData(APP_SOURCE)
                .setText1("app1")
                .setText2("cool app")
                .setShortcutId(sameid);
        reportClick("app", app);
        assertShortcuts("app should be there", "", app);

        final SuggestionData contact = new SuggestionData(CONTACTS_SOURCE)
                .setText1("joe blow")
                .setText2("a good pal")
                .setShortcutId(sameid);
        reportClick("joe", contact);
        reportClick("joe", contact);
        assertShortcuts("app and contact should be there.", "", contact, app);

        refreshShortcut(APP_SOURCE, sameid, null);
        assertNoShortcuts("app should not be there.", "app");
        assertShortcuts("contact with same shortcut id should still be there.",
                "joe", contact);
        assertShortcuts("contact with same shortcut id should still be there.",
                "", contact);
    }

    public void testNeverMakeShortcut() {
        final SuggestionData contact = new SuggestionData(CONTACTS_SOURCE)
                .setText1("unshortcuttable contact")
                .setText2("you didn't want to call them again anyway")
                .setShortcutId(SearchManager.SUGGEST_NEVER_MAKE_SHORTCUT);
        reportClick("unshortcuttable", contact);
        assertNoShortcuts("never-shortcutted suggestion should not be there.", "unshortcuttable");
    }

    public void testCountResetAfterShortcutDeleted() {
        reportClick("app", mApp1);
        reportClick("app", mApp1);
        reportClick("app", mApp1);
        reportClick("app", mApp1);

        reportClick("app", mApp2);
        reportClick("app", mApp2);

        // app1 wins 4 - 2
        assertShortcuts("app", mApp1, mApp2);

        // reset to 1
        invalidateShortcut(APP_SOURCE, mApp1.getShortcutId());
        reportClick("app", mApp1);

        // app2 wins 2 - 1
        assertShortcuts("expecting app1's click count to reset after being invalidated.",
                "app", mApp2, mApp1);
    }

    public void testShortcutsAllowedCorpora() {
        reportClick("a", mApp1);
        reportClick("a", mContact1);

        assertShortcuts("only allowed shortcuts should be returned",
                "a", Arrays.asList(APP_CORPUS), mApp1);
    }

    //
    // SOURCE RANKING TESTS BELOW
    //

    public void testSourceRanking_moreClicksWins() {
        assertCorpusRanking("expected no ranking");

        int minClicks = mConfig.getMinClicksForSourceRanking();

        // click on an app
        for (int i = 0; i < minClicks + 1; i++) {
            reportClick("a", mApp1);
        }
        // fewer clicks on a contact
        for (int i = 0; i < minClicks; i++) {
            reportClick("a", mContact1);
        }

        assertCorpusRanking("expecting apps to rank ahead of contacts (more clicks)",
                APP_CORPUS, CONTACTS_CORPUS);

        // more clicks on a contact
        reportClick("a", mContact1);
        reportClick("a", mContact1);

        assertCorpusRanking("expecting contacts to rank ahead of apps (more clicks)",
                CONTACTS_CORPUS, APP_CORPUS);
    }

    public void testOldSourceStatsDontCount() {
        // apps were popular back in the day
        final long toOld = mConfig.getMaxStatAgeMillis() + 1;
        int minClicks = mConfig.getMinClicksForSourceRanking();
        for (int i = 0; i < minClicks; i++) {
            reportClick("app", mApp1, NOW - toOld);
        }

        // and contacts is 1/2
        for (int i = 0; i < minClicks; i++) {
            reportClick("bob", mContact1, NOW);
        }

        assertCorpusRanking("old clicks for apps shouldn't count.",
                CONTACTS_CORPUS);
    }


    public void testSourceRanking_filterSourcesWithInsufficientData() {
        int minClicks = mConfig.getMinClicksForSourceRanking();
        // not enough
        for (int i = 0; i < minClicks - 1; i++) {
            reportClick("app", mApp1);
        }
        // just enough
        for (int i = 0; i < minClicks; i++) {
            reportClick("bob", mContact1);
        }

        assertCorpusRanking(
                "ordering should only include sources with at least " + minClicks + " clicks.",
                CONTACTS_CORPUS);
    }

    // App upgrade tests

    public void testAppUpgradeClearsShortcuts() {
        reportClick("a", mApp1);
        reportClick("add", mApp1);
        reportClick("a", mContact1);

        assertShortcuts("all shortcuts should be returned",
                "a", mAllowedCorpora, mApp1, mContact1);

        // Upgrade an existing corpus
        MockCorpus upgradedCorpus = new MockCorpus(APP_SOURCE_V2);
        mCorpora.addCorpus(upgradedCorpus);

        List<Corpus> newAllowedCorpora = new ArrayList<Corpus>(mCorpora.getAllCorpora());
        assertShortcuts("app shortcuts should be removed when the source was upgraded",
                "a", newAllowedCorpora, mContact1);
    }

    public void testAppUpgradePromotesLowerRanked() {

        ListSuggestionCursor expected = new ListSuggestionCursor("a");
        for (int i = 0; i < MAX_SHORTCUTS + 1; i++) {
            reportClick("app", mApp1, NOW);
        }
        expected.add(mApp1);

        // Enough contact clicks to make one more shortcut than getMaxShortcutsReturned()
        for (int i = 0; i < MAX_SHORTCUTS; i++) {
            SuggestionData contact = makeContact("andy" + i);
            int numClicks = MAX_SHORTCUTS - i;  // use click count to get shortcuts in order
            for (int j = 0; j < numClicks; j++) {
                reportClick("and", contact, NOW);
            }
            expected.add(contact);
        }

        // Expect the app, and then all contacts
        assertShortcuts("app and all contacts should be returned",
                "a", mAllowedCorpora, expected);

        // Upgrade app corpus
        MockCorpus upgradedCorpus = new MockCorpus(APP_SOURCE_V2);
        mCorpora.addCorpus(upgradedCorpus);

        // Expect all contacts
        List<Corpus> newAllowedCorpora = new ArrayList<Corpus>(mCorpora.getAllCorpora());
        assertShortcuts("app shortcuts should be removed when the source was upgraded "
                + "and a contact should take its place",
                "a", newAllowedCorpora, SuggestionCursorUtil.slice(expected, 1));
    }

    public void testIrrelevantAppUpgrade() {
        reportClick("a", mApp1);
        reportClick("add", mApp1);
        reportClick("a", mContact1);

        assertShortcuts("all shortcuts should be returned",
                "a", mAllowedCorpora, mApp1, mContact1);

        // Fire a corpus set update that affect no shortcuts corpus
        MockCorpus newCorpus = new MockCorpus(new MockSource("newsource"));
        mCorpora.addCorpus(newCorpus);

        assertShortcuts("all shortcuts should be returned",
                "a", mAllowedCorpora, mApp1, mContact1);
    }

    public void testAllowWebSearchShortcuts() {
        reportClick("a", mApp1);
        reportClick("a", mApp1);
        reportClick("a", mWeb1);
        assertShortcuts("web shortcuts should be included", "a",
                mAllowedCorpora, true, mApp1, mWeb1);
        assertShortcuts("web shortcuts should not be included", "a",
                mAllowedCorpora, false, mApp1);
    }

    public void testExtraDataNull() {
        assertExtra("Null extra", "extra_null", null);
    }

    public void testExtraDataString() {
        assertExtra("String extra", "extra_string", "stringy-stringy-string");
    }

    public void testExtraDataInteger() {
        assertExtra("Integer extra", "extra_int", new Integer(42));
    }

    public void testExtraDataFloat() {
        assertExtra("Float extra", "extra_float", new Float(Math.PI));
    }

    public void testExtraDataStringWithDodgyChars() {
        assertExtra("String extra with newlines", "extra_string", "line\nline\nline\n");
        JSONArray a = new JSONArray();
        a.put(true);
        a.put(42);
        a.put("hello");
        a.put("hello \"again\"");
        assertExtra("String extra with JSON", "extra_string", a.toString());
        assertExtra("String extra with control chars", "extra_string", "\0\b\t\f\r");
    }

    // Utilities

    protected ListSuggestionCursor makeCursor(String query, SuggestionData... suggestions) {
        ListSuggestionCursor cursor = new ListSuggestionCursor(query);
        for (SuggestionData suggestion : suggestions) {
            cursor.add(suggestion);
        }
        return cursor;
    }

    protected void reportClick(String query, SuggestionData suggestion) {
        reportClick(new ListSuggestionCursor(query, suggestion), 0);
    }

    protected void reportClick(String query, SuggestionData suggestion, long now) {
        reportClickAtTime(new ListSuggestionCursor(query, suggestion), 0, now);
    }

    protected void reportClick(SuggestionCursor suggestions, int position) {
        reportClickAtTime(suggestions, position, NOW);
    }

    protected void reportClickAtTime(SuggestionCursor suggestions, int position, long now) {
        mRepo.reportClickAtTime(suggestions, position, now);
        mLogExecutor.runNext();
    }

    protected void removeFromHistory(SuggestionCursor suggestions, int position) {
        mRepo.removeFromHistory(suggestions, position);
        mLogExecutor.runNext();
    }

    protected void invalidateShortcut(Source source, String shortcutId) {
        refreshShortcut(source, shortcutId, null);
    }

    protected void refreshShortcut(Source source, String shortcutId, SuggestionData suggestion) {
        SuggestionCursor refreshed =
                suggestion == null ? null : new ListSuggestionCursor(null, suggestion);
        mRepo.refreshShortcut(source, shortcutId, refreshed);
        mLogExecutor.runNext();
    }

    protected void sourceImpressions(Source source, int clicks, int impressions) {
        if (clicks > impressions) throw new IllegalArgumentException("ya moran!");

        for (int i = 0; i < impressions; i++, clicks--) {
            sourceImpression(source, clicks > 0);
        }
    }

    /**
     * Simulate an impression, and optionally a click, on a source.
     *
     * @param source The name of the source.
     * @param click Whether to register a click in addition to the impression.
     */
    protected void sourceImpression(Source source, boolean click) {
        sourceImpression(source, click, NOW);
    }

    protected SuggestionData sourceSuggestion(Source source) {
        return new SuggestionData(source)
            .setIntentAction("view")
            .setIntentData("data/id")
            .setShortcutId("shortcutid");
    }

    /**
     * Simulate an impression, and optionally a click, on a source.
     *
     * @param source The name of the source.
     * @param click Whether to register a click in addition to the impression.
     */
    protected void sourceImpression(Source source, boolean click, long now) {
        SuggestionData suggestionClicked = !click ?
                null : sourceSuggestion(source);

        reportClick("a", suggestionClicked);
    }

    void assertNoShortcuts(String query) {
        assertNoShortcuts("", query);
    }

    void assertNoShortcuts(String message, String query) {
        SuggestionCursor cursor = getShortcuts(query, mAllowedCorpora);
        try {
            assertNull(message + ", got shortcuts", cursor);
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    void assertShortcuts(String query, SuggestionData... expected) {
        assertShortcuts("", query, expected);
    }

    void assertShortcutAtPosition(String message, String query,
            int position, SuggestionData expected) {
        SuggestionCursor cursor = getShortcuts(query, mAllowedCorpora);
        try {
            SuggestionCursor expectedCursor = new ListSuggestionCursor(query, expected);
            SuggestionCursorUtil.assertSameSuggestion(message, position, expectedCursor, cursor);
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    void assertShortcutCount(String message, String query, int expectedCount) {
        SuggestionCursor cursor = getShortcuts(query, mAllowedCorpora);
        try {
            assertEquals(message, expectedCount, cursor.getCount());
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    void assertShortcuts(String message, String query, Collection<Corpus> allowedCorpora,
            boolean allowWebSearchShortcuts, SuggestionCursor expected) {
        SuggestionCursor cursor = mRepo.getShortcutsForQuery(query, allowedCorpora, allowWebSearchShortcuts, NOW);
        try {
            SuggestionCursorUtil.assertSameSuggestions(message, expected, cursor);
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    void assertShortcuts(String message, String query, Collection<Corpus> allowedCorpora,
            SuggestionCursor expected) {
        assertShortcuts(message, query, allowedCorpora, true, expected);
    }

    SuggestionCursor getShortcuts(String query, Collection<Corpus> allowedCorpora) {
        return mRepo.getShortcutsForQuery(query, allowedCorpora, true, NOW);
    }

    void assertShortcuts(String message, String query, Collection<Corpus> allowedCorpora,
            boolean allowWebSearchShortcuts, SuggestionData... expected) {
        assertShortcuts(message, query, allowedCorpora, allowWebSearchShortcuts,
                new ListSuggestionCursor(query, expected));
    }

    void assertShortcuts(String message, String query, Collection<Corpus> allowedCorpora,
            SuggestionData... expected) {
        assertShortcuts(message, query, allowedCorpora, new ListSuggestionCursor(query, expected));
    }

    void assertShortcuts(String message, String query, SuggestionData... expected) {
        assertShortcuts(message, query, mAllowedCorpora, expected);
    }

    private void assertHasHistory(boolean expected) {
        ConsumerTrap<Boolean> trap = new ConsumerTrap<Boolean>();
        mRepo.hasHistory(trap);
        mLogExecutor.runNext();
        assertEquals("hasHistory() returned bad value", expected, (boolean) trap.getValue());
    }

    void assertCorpusRanking(String message, Corpus... expected) {
        String[] expectedNames = new String[expected.length];
        for (int i = 0; i < expected.length; i++) {
            expectedNames[i] = expected[i].getName();
        }
        Map<String,Integer> scores = getCorpusScores();
        List<String> observed = sortByValues(scores);
        // Highest scores should come first
        Collections.reverse(observed);
        Log.d(TAG, "scores=" + scores);
        MoreAsserts.assertContentsInOrder(message, observed, (Object[]) expectedNames);
    }

    private Map<String,Integer> getCorpusScores() {
        ConsumerTrap<Map<String,Integer>> trap = new ConsumerTrap<Map<String,Integer>>();
        mRepo.getCorpusScores(trap);
        mLogExecutor.runNext();
        return trap.getValue();
    }

    static <A extends Comparable<A>, B extends Comparable<B>> List<A> sortByValues(Map<A,B> map) {
        Comparator<Map.Entry<A,B>> comp = new Comparator<Map.Entry<A,B>>() {
            public int compare(Entry<A, B> object1, Entry<A, B> object2) {
                int diff = object1.getValue().compareTo(object2.getValue());
                if (diff != 0) {
                    return diff;
                } else {
                    return object1.getKey().compareTo(object2.getKey());
                }
            }
        };
        ArrayList<Map.Entry<A,B>> sorted = new ArrayList<Map.Entry<A,B>>(map.size());
        sorted.addAll(map.entrySet());
        Collections.sort(sorted, comp);
        ArrayList<A> out = new ArrayList<A>(sorted.size());
        for (Map.Entry<A,B> e : sorted) {
            out.add(e.getKey());
        }
        return out;
    }

    static void assertContentsInOrder(Iterable<?> actual, Object... expected) {
        MoreAsserts.assertContentsInOrder(null, actual, expected);
    }

    void assertExtra(String message, String extraColumn, Object extraValue) {
        SuggestionData s = sourceSuggestion(APP_SOURCE);
        s.setExtras(new MockSuggestionExtras().put(extraColumn, extraValue));
        reportClick("a", s);
        assertShortcutExtra(message, "a", extraColumn, extraValue);
    }

    void assertShortcutExtra(String message, String query, String extraColumn, Object extraValue) {
        SuggestionCursor cursor = getShortcuts(query, mAllowedCorpora);
        try {
            SuggestionCursorUtil.assertSuggestionExtras(message, cursor, extraColumn, extraValue);
        } finally {
            if (cursor != null) cursor.close();
        }
    }

}
