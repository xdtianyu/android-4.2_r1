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
import java.util.Arrays;
import java.util.List;

/**
 * Tests for SingleCorpusPromoter.
 */
@SmallTest
public class SingleCorpusPromoterTest extends AndroidTestCase {
    public static final String TEST_QUERY = "query";

    private List<Corpus> mCorpora = Arrays.asList(MockCorpus.CORPUS_1, MockCorpus.CORPUS_2);
    private SingleCorpusPromoter mPromoter;

    @Override
    public void setUp() {
        mPromoter = new SingleCorpusPromoter(MockCorpus.CORPUS_1, 10);
    }

    public void testPromotesOnlyGivenCorpus() {
        Suggestions suggestions = makeSuggestions(TEST_QUERY);

        ListSuggestionCursor promoted = new ListSuggestionCursor(TEST_QUERY);
        mPromoter.pickPromoted(suggestions, 4, promoted);

        assertEquals(2, promoted.getCount());

        Source[] expectedSource = { MockSource.SOURCE_1, MockSource.SOURCE_1 };

        for (int i = 0; i < promoted.getCount(); i++) {
            promoted.moveTo(i);
            assertEquals("Source in position " + i,
                    expectedSource[i], promoted.getSuggestionSource());
        }
    }

    private Suggestions makeSuggestions(String query) {
        Suggestions suggestions = new Suggestions(query, mCorpora);
        ArrayList<CorpusResult> results = new ArrayList<CorpusResult>();
        for (Corpus corpus : mCorpora) {
            results.add(corpus.getSuggestions(query, 10, false));
        }
        suggestions.addCorpusResults(results);
        return suggestions;
    }

}
