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
import android.test.MoreAsserts;
import android.test.suitebuilder.annotation.MediumTest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tests for {@link DefaultCorpusRankerTest}.
 */
@MediumTest
public class DefaultCorpusRankerTest extends AndroidTestCase {

    protected MockCorpora mCorpora;
    protected MockShortcutRepository mRepo;
    protected DefaultCorpusRanker mRanker;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        mCorpora = new MockCorpora();
        mCorpora.addCorpus(MockCorpus.CORPUS_1);
        mCorpora.addCorpus(MockCorpus.CORPUS_2);
        mCorpora.addCorpus(MockCorpus.WEB_CORPUS);
        mRepo = new MockShortcutRepository();
        mRanker = new DefaultCorpusRanker(mCorpora, mRepo);
    }

    public void testRankNoScores() {
        mRanker.clear();
        MoreAsserts.assertContentsInOrder(getRankedCorpora(),
                MockCorpus.WEB_CORPUS, MockCorpus.CORPUS_1, MockCorpus.CORPUS_2);
    }

    public void testRankWebLowScore() {
        setCorpusScores(1, 2, -100);
        MoreAsserts.assertContentsInOrder(getRankedCorpora(),
                MockCorpus.WEB_CORPUS, MockCorpus.CORPUS_2, MockCorpus.CORPUS_1);
    }

    public void testRankSomeScores() {
        setCorpusScores(null, 1, null);
        MoreAsserts.assertContentsInOrder(getRankedCorpora(),
                MockCorpus.WEB_CORPUS, MockCorpus.CORPUS_2, MockCorpus.CORPUS_1);
    }

    private void setCorpusScores(Integer corpus1, Integer corpus2, Integer webCorpus) {
        Map<String,Integer> scores = new HashMap<String,Integer>();
        if (corpus1 != null) scores.put(MockCorpus.CORPUS_1.getName(), corpus1);
        if (corpus2 != null) scores.put(MockCorpus.CORPUS_2.getName(), corpus2);
        if (webCorpus != null) scores.put(MockCorpus.WEB_CORPUS.getName(), webCorpus);
        mRepo.setCorpusScores(scores);
        mRanker.clear();
    }

    private List<Corpus> getRankedCorpora() {
        ConsumerTrap<List<Corpus>> consumer = new ConsumerTrap<List<Corpus>>();
        mRanker.getCorporaInAll(consumer);
        return consumer.getValue();
    }

}
