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

import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests for RankAwarePromoter
 */
@SmallTest
public class RankAwarePromoterTest extends AndroidTestCase {
    public static final int NUM_SUGGESTIONS_ABOVE_KEYBOARD = 4;
    public static final int MAX_PROMOTED_CORPORA = 3;
    public static final int MAX_PROMOTED_SUGGESTIONS = 8;
    public static final String TEST_QUERY = "query";

    private final List<Corpus> mCorpora = createMockCorpora(5, MAX_PROMOTED_CORPORA);
    private final Corpus mShortcuts = createMockShortcutsCorpus();
    private RankAwarePromoter mPromoter;

    @Override
    public void setUp() {
        mPromoter = new RankAwarePromoter(new Config(mContext){
            @Override
            public int getNumSuggestionsAboveKeyboard() {
                return NUM_SUGGESTIONS_ABOVE_KEYBOARD;
            }
        }, null, null);
    }

    public void testPromotesExpectedSuggestions() {
        List<CorpusResult> suggestions = getSuggestions(TEST_QUERY);
        ListSuggestionCursorNoDuplicates promoted = 
            new ListSuggestionCursorNoDuplicates(TEST_QUERY);
        mPromoter.promoteSuggestions(suggestions, MAX_PROMOTED_SUGGESTIONS, promoted);

        int[] expectedSource = {0, 1, 2, 0, 1, 2, 3, 4};
        int[] expectedSuggestion = {1, 1, 1, 2, 2, 2, 1, 1};

        assertRightSuggestionsWerePromoted(promoted, expectedSource, expectedSuggestion);
    }

    public void testWhenPromotingAlreadyPromotedResults() {
        List<CorpusResult> suggestions = getSuggestions(TEST_QUERY);
        ListSuggestionCursorNoDuplicates promoted =
                new ListSuggestionCursorNoDuplicates(TEST_QUERY);

        // Simulate scenario where another promoter has already put an item
        // (e.g. a shortcut) into the promoted suggestions list, and the current
        // promoter tries to promote the same item.
        // The promoter must notice that this duplicate (which automatically
        // gets filtered out by the suggestion cursor) doesn't decrease the
        // number of slots available on the screen.
        CorpusResult corpora2 = suggestions.get(2);
        corpora2.moveTo(0);
        Suggestion shortcut = new SuggestionPosition(corpora2);
        promoted.add(shortcut);

        mPromoter.promoteSuggestions(suggestions, MAX_PROMOTED_SUGGESTIONS, promoted);

        int[] expectedSource = {
                // The shortcut at the top of the list.
                2,
                // The promoted results. There's just one result from corpus
                // 2 because only 2 test suggestions per corpus are generated
                // by the test data generator, and we use up all our suggestions
                // for corpus 2 earlier than the rest.
                0, 1, 2, 0, 1, 3, 4};
        int[] expectedSuggestion = {
                // The shortcut at the top of the list.
                1,
                // The promoted results.
                1, 1, 2, 2, 2, 1, 1};

        assertRightSuggestionsWerePromoted(promoted, expectedSource, expectedSuggestion);
    }

    private void assertRightSuggestionsWerePromoted(ListSuggestionCursorNoDuplicates promoted,
            int[] expectedSource, int[] expectedSuggestion) {
        assertEquals(MAX_PROMOTED_SUGGESTIONS, promoted.getCount());

        for (int i = 0; i < promoted.getCount(); i++) {
            promoted.moveTo(i);
            assertEquals("Source in position " + i,
                    "MockSource Source" + expectedSource[i],
                    promoted.getSuggestionSource().getLabel());
            assertEquals("Suggestion in position " + i,
                    TEST_QUERY + "_" + expectedSuggestion[i],
                    promoted.getSuggestionText1());
        }
    }

    public void testPromotesRightNumberOfSuggestions() {
        List<CorpusResult> suggestions = getSuggestions(TEST_QUERY);
        ListSuggestionCursor promoted = new ListSuggestionCursor(TEST_QUERY);
        SuggestionCursor shortcuts = mShortcuts.
                getSuggestions(TEST_QUERY, MAX_PROMOTED_SUGGESTIONS / 2, true);
        for (int i = 0; i < shortcuts.getCount(); ++i) {
            promoted.add(new SuggestionPosition(shortcuts, 1));
        }
        mPromoter.promoteSuggestions(suggestions, MAX_PROMOTED_SUGGESTIONS, promoted);
        assertEquals(MAX_PROMOTED_SUGGESTIONS, promoted.getCount());
    }

    private List<CorpusResult> getSuggestions(String query) {
        ArrayList<CorpusResult> results = new ArrayList<CorpusResult>();
        for (Corpus corpus : mCorpora) {
            results.add(corpus.getSuggestions(query, 10, false));
        }
        return results;
    }

    private static List<Corpus> createMockCorpora(int count, int defaultCount) {
        ArrayList<Corpus> corpora = new ArrayList<Corpus>();
        for (int i = 0; i < count; i++) {
            Source mockSource = new MockSource("Source" + i);
            Corpus mockCorpus = new MockCorpus(mockSource, i < defaultCount);
            corpora.add(mockCorpus);
        }
        return corpora;
    }

    private static Corpus createMockShortcutsCorpus() {
        Source mockSource = new MockSource("Shortcuts");
        Corpus mockCorpus = new MockCorpus(mockSource, true);
        return mockCorpus;
    }

}
