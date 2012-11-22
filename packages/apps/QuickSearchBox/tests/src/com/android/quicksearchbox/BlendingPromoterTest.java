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
package com.android.quicksearchbox;

import static com.android.quicksearchbox.SuggestionCursorUtil.assertSameSuggestions;
import static com.android.quicksearchbox.SuggestionCursorUtil.slice;

import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.MediumTest;

/**
 * Tests for {@link ShortcutPromoter}.
 */
@MediumTest
public class BlendingPromoterTest extends AndroidTestCase {

    private String mQuery;

    private Suggestion mS11;
    private Suggestion mS12;
    private Suggestion mS21;
    private Suggestion mS22;
    private Suggestion mWeb1;
    private Suggestion mWeb2;

    @Override
    protected void setUp() throws Exception {
        mQuery = "foo";
        mS11 = MockSource.SOURCE_1.createSuggestion(mQuery + "_1_1");
        mS12 = MockSource.SOURCE_1.createSuggestion(mQuery + "_1_2");
        mS21 = MockSource.SOURCE_2.createSuggestion(mQuery + "_1_1");
        mS22 = MockSource.SOURCE_2.createSuggestion(mQuery + "_1_2");
        mWeb1 = MockSource.WEB_SOURCE.createSuggestion(mQuery + "_web_1");
        mWeb2 = MockSource.WEB_SOURCE.createSuggestion(mQuery + "_web_2");
    }

    public void testMaxPromoted() {
        maxPromotedTest(0);
        maxPromotedTest(1);
        maxPromotedTest(2);
        maxPromotedTest(5);
    }

    public void testLimitZeroShortcutsPerSource() {
        SuggestionCursor promoted = limit(cursor(mS11, mS12, mS21, mS22), 0, 0);
        SuggestionCursor expected = cursor();
        assertSameSuggestions(expected, promoted);
    }

    public void testLimitOneShortcutPerSource() {
        SuggestionCursor promoted = limit(cursor(mS11, mS12, mS21, mS22), 1, 1);
        SuggestionCursor expected = cursor(mS11, mS21);
        assertSameSuggestions(expected, promoted);
    }

    public void testLimitTwoShortcutsPerSource() {
        SuggestionCursor promoted = limit(cursor(mS11, mS12, mS21, mS22), 2, 2);
        SuggestionCursor expected = cursor(mS11, mS12, mS21, mS22);
        assertSameSuggestions(expected, promoted);
    }

    public void testLimitThreeShortcutsPerSource() {
        SuggestionCursor promoted = limit(cursor(mS11, mS12, mS21, mS22), 3, 3);
        SuggestionCursor expected = cursor(mS11, mS12, mS21, mS22);
        assertSameSuggestions(expected, promoted);
    }

    public void testLimitOneSourceZeroPromoted() {
        SuggestionCursor promoted = limit(cursor(mS11, mS12), 0, 0);
        SuggestionCursor expected = cursor();
        assertSameSuggestions(expected, promoted);
    }

    public void testLimitOneSourceOnePromoted() {
        SuggestionCursor promoted = limit(cursor(mS11, mS12), 1, 1);
        SuggestionCursor expected = cursor(mS11);
        assertSameSuggestions(expected, promoted);
    }

    public void testLimitOneSourceTwoPromoted() {
        SuggestionCursor promoted = limit(cursor(mS11, mS12), 2, 2);
        SuggestionCursor expected = cursor(mS11, mS12);
        assertSameSuggestions(expected, promoted);
    }

    public void testLimitNoShortcuts() {
        SuggestionCursor promoted = limit(cursor(), 2, 2);
        SuggestionCursor expected = cursor();
        assertSameSuggestions(expected, promoted);
    }

    public void testLimitZeroWebShortcuts() {
        SuggestionCursor promoted = limit(cursor(mS11, mS12, mWeb1, mWeb2), 0, 2);
        SuggestionCursor expected = cursor(mS11, mS12);
        assertSameSuggestions(expected, promoted);
    }

    public void testLimitTwoWebShortcuts() {
        SuggestionCursor promoted = limit(cursor(mS11, mS12, mWeb1, mWeb2), 2, 2);
        SuggestionCursor expected = cursor(mS11, mS12, mWeb1, mWeb2);
        assertSameSuggestions(expected, promoted);
    }

    private void maxPromotedTest(int maxPromoted) {
        SuggestionCursor shortcuts = cursor(mS11, mS12, mS21, mS22, mWeb1, mWeb2);
        ListSuggestionCursorNoDuplicates promoted = promote(config(), shortcuts, maxPromoted);
        int expectedCount = Math.min(maxPromoted, shortcuts.getCount());
        assertEquals(expectedCount, promoted.getCount());
        int count = Math.min(maxPromoted, shortcuts.getCount());
        assertSameSuggestions(slice(promoted, 0, count), slice(shortcuts, 0, count));
    }

    private SuggestionCursor limit(SuggestionCursor shortcuts, int maxShortcutsPerWebSource,
            int maxShortcutsPerNonWebSource) {
        Config config = config(maxShortcutsPerWebSource, maxShortcutsPerNonWebSource);
        int maxPromoted = 10;
        return promote(config, shortcuts, maxPromoted);
    }

    private ListSuggestionCursorNoDuplicates promote(Config config, SuggestionCursor shortcuts,
            int maxPromoted) {
        ShortcutPromoter promoter = new ShortcutPromoter(config,
                new RankAwarePromoter(config, null, null), null);
        ListSuggestionCursorNoDuplicates promoted = new ListSuggestionCursorNoDuplicates(mQuery);
        promoter.promoteShortcuts(shortcuts, maxPromoted, promoted);
        return promoted;
    }

    private Config config() {
        return new Config(getContext());
    }

    private Config config(final int maxShortcutsPerWebSource,
            final int maxShortcutsPerNonWebSource) {
        return new Config(getContext()) {
            @Override
            public int getMaxShortcutsPerWebSource() {
                return maxShortcutsPerWebSource;
            }
            @Override
            public int getMaxShortcutsPerNonWebSource() {
                return maxShortcutsPerNonWebSource;
            }
        };
    }

    private SuggestionCursor cursor(Suggestion... suggestions) {
        return new ListSuggestionCursor(mQuery, suggestions);
    }

}
