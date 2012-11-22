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

import android.test.AndroidTestCase;
import android.test.MoreAsserts;
import android.test.suitebuilder.annotation.MediumTest;

import java.util.Arrays;
import java.util.Collection;

/**
 * Tests for {@link SearchableCorpora}.
 *
 */
@MediumTest
public class SearchableCorporaTest extends AndroidTestCase {

    protected SearchSettings mSettings;

    protected SearchableCorpora mCorpora;

    private static final MockCorpus CORPUS_1 = new MockCorpus(MockSource.SOURCE_1);
    private static final MockCorpus CORPUS_DISABLED = new MockCorpus(MockSource.SOURCE_2);
    private static final MockCorpus CORPUS_NOT_IN_ALL = new MockCorpus(MockSource.SOURCE_3) {
        @Override
        public boolean includeInAll() {
            return false;
        }
    };
    private static final MockCorpus CORPUS_WEB = new MockCorpus(MockSource.WEB_SOURCE);

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        mSettings = new MockSearchSettings() {
            @Override
            public boolean isCorpusEnabled(Corpus corpus) {
                return !CORPUS_DISABLED.equals(corpus);
            }
        };
        MockSources sources = new MockSources();
        sources.addSource(MockSource.SOURCE_1);
        sources.addSource(MockSource.SOURCE_2);
        sources.addSource(MockSource.SOURCE_3);
        sources.addSource(MockSource.WEB_SOURCE);
        mCorpora = new SearchableCorpora(mContext, mSettings, sources, new MockCorpusFactory());
        mCorpora.update();
    }

    public void testGetAllCorpora() {
        MoreAsserts.assertContentsInAnyOrder(mCorpora.getAllCorpora(),
                CORPUS_1, CORPUS_DISABLED, CORPUS_NOT_IN_ALL, CORPUS_WEB);
    }

    public void testEnabledCorpora() {
        MoreAsserts.assertContentsInAnyOrder(mCorpora.getEnabledCorpora(),
                CORPUS_1, CORPUS_NOT_IN_ALL, CORPUS_WEB);
    }

    public void testCorporaIncludedInAll() {
        MoreAsserts.assertContentsInAnyOrder(mCorpora.getCorporaInAll(),
                CORPUS_1, CORPUS_WEB);
    }

    public void testGetWebCorpus() {
        assertEquals(CORPUS_WEB, mCorpora.getWebCorpus());
    }

    public void testGetCorpusForSource() {
        assertEquals(CORPUS_1, mCorpora.getCorpusForSource(MockSource.SOURCE_1));
        assertNull(mCorpora.getCorpusForSource(new MockSource("foo")));
    }

    public void testGetCorpus() {
        assertEquals(CORPUS_WEB, mCorpora.getCorpus(CORPUS_WEB.getName()));
    }

    /**
     * Mock implementation of {@link CorpusFactory}.
     */
    private static class MockCorpusFactory implements CorpusFactory {
        public Collection<Corpus> createCorpora(Sources sources) {
            return Arrays.<Corpus>asList(CORPUS_1, CORPUS_DISABLED, CORPUS_NOT_IN_ALL, CORPUS_WEB);
        }
    }

}
