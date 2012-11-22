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
import static com.android.quicksearchbox.SuggestionCursorUtil.concat;
import static com.android.quicksearchbox.SuggestionCursorUtil.iterable;
import static com.android.quicksearchbox.SuggestionCursorUtil.slice;

import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.MediumTest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Tests for {@link WebPromoter}.
 */
@MediumTest
public class WebPromoterTest extends AndroidTestCase {

    private String mQuery;

    private Suggestion mS11;
    private Suggestion mS12;
    private Suggestion mS21;
    private Suggestion mS22;
    private Suggestion mWeb1;
    private Suggestion mWeb2;

    private CorpusResult mCorpusResult1;
    private CorpusResult mCorpusResult2;
    private CorpusResult mCorpusResultWeb;

    private SuggestionCursor mShortcuts;
    private List<CorpusResult> mCorpusResults;
    private Suggestions mSuggestions;

    @Override
    protected void setUp() throws Exception {
        mQuery = "foo";
        mS11 = MockSource.SOURCE_1.createSuggestion(mQuery + "_1_1");
        mS12 = MockSource.SOURCE_1.createSuggestion(mQuery + "_1_2");
        mS21 = MockSource.SOURCE_2.createSuggestion(mQuery + "_1_1");
        mS22 = MockSource.SOURCE_2.createSuggestion(mQuery + "_1_2");
        mWeb1 = MockSource.WEB_SOURCE.createSuggestion(mQuery + "_web_1");
        mWeb2 = MockSource.WEB_SOURCE.createSuggestion(mQuery + "_web_2");
        mShortcuts = cursor(mS11, mS12, mS21, mS22, mWeb1, mWeb2);
        mCorpusResult1 = MockCorpus.CORPUS_1.getSuggestions(mQuery, 10, false);
        mCorpusResult2 = MockCorpus.CORPUS_2.getSuggestions(mQuery, 10, false);
        mCorpusResultWeb = MockCorpus.WEB_CORPUS.getSuggestions(mQuery, 10, false);
        mCorpusResults = new ArrayList<CorpusResult>();
        mCorpusResults.add(mCorpusResult1);
        mCorpusResults.add(mCorpusResult2);
        mCorpusResults.add(mCorpusResultWeb);
        mSuggestions = new Suggestions(mQuery,
                Arrays.asList(MockCorpus.CORPUS_1, MockCorpus.CORPUS_2, MockCorpus.WEB_CORPUS));
        mSuggestions.setShortcuts(new ShortcutCursor(mShortcuts));
        mSuggestions.addCorpusResults(mCorpusResults);
    }

    public void testZeroShortcuts() {
        SuggestionCursor promoted = promote(mSuggestions, 0, 1);
        SuggestionCursor expected = slice(mCorpusResultWeb, 0, 1);
        // Test sanity check: shouldn't expect any non-web suggestions
        assertOnlyWebSuggestions(expected);
        assertSameSuggestions(expected, promoted);
    }

    public void testZeroSuggestions() {
        SuggestionCursor promoted = promote(mSuggestions, 1, 1);
        SuggestionCursor expected = cursor(mWeb1);
        // Test sanity check: shouldn't expect any non-web suggestions
        assertOnlyWebSuggestions(expected);
        assertSameSuggestions(expected, promoted);
    }

    public void testOnlyWebPromoted() {
        SuggestionCursor promoted = promote(mSuggestions, 1, 2);
        SuggestionCursor expected = concat(cursor(mWeb1), slice(mCorpusResultWeb, 0, 1));
        // Test sanity check: shouldn't expect any non-web suggestions
        assertOnlyWebSuggestions(expected);
        assertSameSuggestions(expected, promoted);
    }

    private ListSuggestionCursor promote(Suggestions suggestions, int maxShortcuts,
            int maxSuggestions) {
        WebPromoter promoter = new WebPromoter(maxShortcuts);
        ListSuggestionCursor promoted = new ListSuggestionCursor(mQuery);
        promoter.pickPromoted(suggestions, maxSuggestions, promoted);
        return promoted;
    }

    private SuggestionCursor cursor(Suggestion... suggestions) {
        return new ListSuggestionCursor(mQuery, suggestions);
    }

    private void assertOnlyWebSuggestions(SuggestionCursor expected) {
        for (Suggestion s : iterable(expected)) {
            assertTrue("Not a web suggestion", s.isWebSearchSuggestion());
        }
    }

}
