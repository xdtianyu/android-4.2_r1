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
import static com.android.quicksearchbox.SuggestionCursorUtil.assertSameSuggestionsNoOrder;
import static com.android.quicksearchbox.SuggestionCursorUtil.concat;

import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.MediumTest;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests for {@link ResultPromoter}.
 */
@MediumTest
public class ResultPromoterTest extends AndroidTestCase {

    private static final int MAX_PROMOTED_SUGGESTIONS = 10;

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
    }

    public void testOnlyResultShortcutsPromoted() {
        SuggestionCursor promoted = promoteShortcuts(mShortcuts);
        SuggestionCursor expected = cursor(mS11, mS12, mS21, mS22);
        assertSameSuggestions(expected, promoted);
    }

    public void testOnlyResultSuggestionsPromoted() {
        SuggestionCursor promoted = promoteSuggestions(mCorpusResults);
        SuggestionCursor expected = concat(mCorpusResult1, mCorpusResult2);
        assertSameSuggestionsNoOrder(expected, promoted);
    }

    private ShortcutPromoter createResultPromoter() {
        ResultFilter results = new ResultFilter();
        return new ShortcutPromoter(config(),
                new RankAwarePromoter(config(), results, null), results);
    }

    private ListSuggestionCursor promoteShortcuts(SuggestionCursor shortcuts) {
        ShortcutPromoter promoter = createResultPromoter();
        ListSuggestionCursor promoted = new ListSuggestionCursor(mQuery);
        promoter.promoteShortcuts(shortcuts, MAX_PROMOTED_SUGGESTIONS, promoted);
        return promoted;
    }

    private ListSuggestionCursor promoteSuggestions(List<CorpusResult> suggestions) {
        ResultFilter results = new ResultFilter();
        RankAwarePromoter promoter = new RankAwarePromoter(config(), results, null);
        ListSuggestionCursor promoted = new ListSuggestionCursor(mQuery);
        promoter.promoteSuggestions(suggestions, MAX_PROMOTED_SUGGESTIONS, promoted);
        return promoted;
    }

    private Config config() {
        return new Config(getContext());
    }

    private SuggestionCursor cursor(Suggestion... suggestions) {
        return new ListSuggestionCursor(mQuery, suggestions);
    }

}
