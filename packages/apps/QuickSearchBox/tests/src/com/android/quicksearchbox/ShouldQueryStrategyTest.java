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

/**
 * Tests for {@link ShouldQueryStrategy}.
 */
@SmallTest
public class ShouldQueryStrategyTest extends AndroidTestCase {

    private ShouldQueryStrategy mShouldQuery;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        mShouldQuery = new ShouldQueryStrategy(new Config(getContext()){
            @Override
            public boolean showSuggestionsForZeroQuery() {
                return true;
            }
            @Override
            public boolean showShortcutsForZeroQuery() {
                return true;
            }});
    }

    public static final Corpus CORPUS_1 = new MockCorpus(MockSource.SOURCE_1) {
        @Override
        public int getQueryThreshold() {
            return 3;
        }
    };

    public static final Corpus CORPUS_2 = new MockCorpus(MockSource.SOURCE_2) {
        @Override
        public boolean queryAfterZeroResults() {
            return true;
        }
    };

    public void testRespectsQueryThreshold() {
        assertFalse(mShouldQuery.shouldQueryCorpus(CORPUS_1, "aa"));
        assertTrue(mShouldQuery.shouldQueryCorpus(CORPUS_1, "aaa"));
        assertTrue(mShouldQuery.shouldQueryCorpus(CORPUS_2, ""));
    }

    public void testQueriesAfterNoResultsWhenQueryAfterZeroIsTrue() {
        assertTrue(mShouldQuery.shouldQueryCorpus(CORPUS_2, "query"));
        mShouldQuery.onZeroResults(CORPUS_2, "query");
        assertTrue(mShouldQuery.shouldQueryCorpus(CORPUS_2, "query"));
        assertTrue(mShouldQuery.shouldQueryCorpus(CORPUS_2, "query123"));
    }

    public void testDoesntQueryLongerAfterNoResults() {
        assertTrue(mShouldQuery.shouldQueryCorpus(CORPUS_1, "query"));
        mShouldQuery.onZeroResults(CORPUS_1, "query");
        // Now we don't query for queries starting with "query"
        assertFalse(mShouldQuery.shouldQueryCorpus(CORPUS_1, "queryx"));
        assertFalse(mShouldQuery.shouldQueryCorpus(CORPUS_1, "queryxy"));
    }

    public void testDoesntQuerySameAfterNoResults() {
        assertTrue(mShouldQuery.shouldQueryCorpus(CORPUS_1, "query"));
        mShouldQuery.onZeroResults(CORPUS_1, "query");
        // Now we don't query for "query"
        assertFalse(mShouldQuery.shouldQueryCorpus(CORPUS_1, "query"));
    }

    public void testQueriesDifferent() {
        assertTrue(mShouldQuery.shouldQueryCorpus(CORPUS_1, "query"));
        mShouldQuery.onZeroResults(CORPUS_1, "query");
        // Now we do query for "queen"
        assertTrue(mShouldQuery.shouldQueryCorpus(CORPUS_1, "queen"));
        mShouldQuery.onZeroResults(CORPUS_1, "queen");
        // And then not for "queens"
        assertFalse(mShouldQuery.shouldQueryCorpus(CORPUS_1, "queens"));
    }

    public void testShorterThenDifferent() {
        assertTrue(mShouldQuery.shouldQueryCorpus(CORPUS_1, "query"));
        mShouldQuery.onZeroResults(CORPUS_1, "query");
        // Should query for shorter
        assertTrue(mShouldQuery.shouldQueryCorpus(CORPUS_1, "que"));
        mShouldQuery.onZeroResults(CORPUS_1, "que");
        // But not for longer after that
        assertFalse(mShouldQuery.shouldQueryCorpus(CORPUS_1, "queen"));
    }

    public void testQueriesForShorterAfterNoResults() {
        assertTrue(mShouldQuery.shouldQueryCorpus(CORPUS_1, "query"));
        mShouldQuery.onZeroResults(CORPUS_1, "query");
        // We do query for something shorter
        assertTrue(mShouldQuery.shouldQueryCorpus(CORPUS_1, "quer"));
    }

    public void testOutOfOrder1() {
        assertTrue(mShouldQuery.shouldQueryCorpus(CORPUS_1, "quer"));
        // Result for something typed earlier comes in
        mShouldQuery.onZeroResults(CORPUS_1, "que");
        // Now we don't query for the original
        assertFalse(mShouldQuery.shouldQueryCorpus(CORPUS_1, "quer"));
    }

    public void testOutOfOrder2() {
        assertTrue(mShouldQuery.shouldQueryCorpus(CORPUS_1, "quer"));
        // Result for something typed earlier comes in
        mShouldQuery.onZeroResults(CORPUS_1, "que");
        // Now we don't query for an extension of the original
        assertFalse(mShouldQuery.shouldQueryCorpus(CORPUS_1, "query"));
    }
}
