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

import com.android.quicksearchbox.util.MockNamedTaskExecutor;

import android.os.Handler;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.MediumTest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Tests for {@link SuggestionsProviderImpl}.
 */
@MediumTest
public class SuggestionsProviderImplTest extends AndroidTestCase {

    private List<Corpus> mCorpora;
    private MockNamedTaskExecutor mTaskExecutor;
    private SuggestionsProviderImpl mProvider;
    private MockShortcutRepository mShortcutRepo;

    @Override
    protected void setUp() throws Exception {
        Config config = new Config(getContext());
        mTaskExecutor = new MockNamedTaskExecutor();
        Handler publishThread = new MockHandler();
        mShortcutRepo = new MockShortcutRepository();
        mCorpora = new ArrayList<Corpus>();
        mCorpora.add(MockCorpus.CORPUS_1);
        mCorpora.add(MockCorpus.CORPUS_2);
        Logger logger = new MockLogger();
        mProvider = new SuggestionsProviderImpl(config,
                mTaskExecutor,
                publishThread,
                logger);
    }

    public void testSingleCorpus() {
        Suggestions suggestions = mProvider.getSuggestions("foo",
                Collections.singletonList(MockCorpus.CORPUS_1));
        suggestions.setShortcuts(mShortcutRepo.getShortcutsForQuery(
                "foo", mCorpora));
        try {
            assertEquals(1, suggestions.getExpectedResultCount());
            assertEquals(0, suggestions.getResultCount());
            assertEquals(0, promote(suggestions).getCount());
            assertTrue(mTaskExecutor.runNext());
            assertEquals(1, suggestions.getExpectedResultCount());
            assertEquals(1, suggestions.getResultCount());
            assertEquals(MockCorpus.CORPUS_1.getSuggestions("foo", 3, true).getCount(),
                    promote(suggestions).getCount());
            mTaskExecutor.assertDone();
        } finally {
            if (suggestions != null) suggestions.release();
        }
    }

    public void testMultipleCorpora() {
        Suggestions suggestions = mProvider.getSuggestions("foo",
                Arrays.asList(MockCorpus.CORPUS_1, MockCorpus.CORPUS_2));
        suggestions.setShortcuts(mShortcutRepo.getShortcutsForQuery(
                        "foo", mCorpora));
        try {
            int corpus1Count = MockCorpus.CORPUS_1.getSuggestions("foo", 3, true).getCount();
            int corpus2Count = MockCorpus.CORPUS_2.getSuggestions("foo", 3, true).getCount();
            assertEquals(mCorpora.size(), suggestions.getExpectedResultCount());
            assertEquals(0, suggestions.getResultCount());
            assertEquals(0, promote(suggestions).getCount());
            assertTrue(mTaskExecutor.runNext());
            assertEquals(1, suggestions.getResultCount());
            assertEquals("Incorrect promoted: " + promote(suggestions),
                    corpus1Count, promote(suggestions).getCount());
            assertTrue(mTaskExecutor.runNext());
            assertEquals(2, suggestions.getResultCount());
            assertEquals("Incorrect promoted: " + promote(suggestions),
                    corpus1Count + corpus2Count, promote(suggestions).getCount());
            mTaskExecutor.assertDone();
        } finally {
            if (suggestions != null) suggestions.release();
        }
    }

    private SuggestionCursor promote(Suggestions suggestions) {
        return suggestions.getPromoted(new ConcatPromoter(), 10);
    }

    private static class ConcatPromoter implements Promoter {
        public void pickPromoted(Suggestions suggestions, int maxPromoted,
                ListSuggestionCursor promoted) {
            // Add suggestions
            for (SuggestionCursor c : suggestions.getCorpusResults()) {
                for (int i = 0; i < c.getCount(); i++) {
                    if (promoted.getCount() >= maxPromoted) {
                        return;
                    }
                    promoted.add(new SuggestionPosition(c, i));
                }
            }
        }
    }

}
