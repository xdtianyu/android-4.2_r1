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

import com.android.quicksearchbox.util.MockDataSetObserver;

import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Tests for {@link Suggestions}.
 *
 */
@SmallTest
public class SuggestionsTest extends AndroidTestCase {

    private Suggestions mSuggestions;
    private MockDataSetObserver mObserver;
    private List<Corpus> mExpectedCorpora;

    @Override
    protected void setUp() throws Exception {
        mExpectedCorpora = Arrays.asList(MockCorpus.CORPUS_1, MockCorpus.WEB_CORPUS);
        mSuggestions = new Suggestions("foo", mExpectedCorpora);
        mObserver = new MockDataSetObserver();
        mSuggestions.registerDataSetObserver(mObserver);
    }

    @Override
    protected void tearDown() throws Exception {
        mSuggestions.release();
        mSuggestions = null;
    }

    public void testGetExpectedResultCount() {
        assertEquals(mExpectedCorpora.size(), mSuggestions.getExpectedResultCount());
    }

    public void testGetExpectedCorpora() {
        List<Corpus> expectedCorpora = mSuggestions.getExpectedCorpora();
        assertEquals(mExpectedCorpora.size(), expectedCorpora.size());
        for (int i=0; i<mExpectedCorpora.size(); ++i) {
            assertEquals(mExpectedCorpora.get(i), expectedCorpora.get(i));
        }
    }

    public void testExpectsCorpus() {
        for (int i=0; i<mExpectedCorpora.size(); ++i) {
            assertTrue(mSuggestions.expectsCorpus(mExpectedCorpora.get(i)));
        }
        assertFalse(mSuggestions.expectsCorpus(MockCorpus.CORPUS_2));
    }

    public void testGetUserQuery() {
        assertEquals("foo", mSuggestions.getQuery());
    }

    public void testGetIncludedCorpora() {
        Corpus corpus = MockCorpus.CORPUS_1;
        mSuggestions.addCorpusResults(
                Collections.singletonList(corpus.getSuggestions("foo", 50, true)));
        Set<Corpus> includedCorpora = mSuggestions.getIncludedCorpora();
        assertEquals(includedCorpora.size(), 1);
        assertTrue(includedCorpora.contains(corpus));
    }

    public void testObserverNotified() {
        Corpus corpus = MockCorpus.CORPUS_1;
        mObserver.assertNotChanged();
        mObserver.assertNotInvalidated();
        mSuggestions.addCorpusResults(
                Collections.singletonList(corpus.getSuggestions("foo", 50, true)));
        mObserver.assertChanged();
        mObserver.assertNotInvalidated();
    }

}
