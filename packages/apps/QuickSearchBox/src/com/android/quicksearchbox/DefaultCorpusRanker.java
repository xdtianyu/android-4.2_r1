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

import com.android.quicksearchbox.util.CachedLater;
import com.android.quicksearchbox.util.Consumer;

import android.database.DataSetObserver;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * A corpus ranker that uses corpus scores from the shortcut repository to rank
 * corpora.
 */
public class DefaultCorpusRanker implements CorpusRanker {

    private static final boolean DBG = false;
    private static final String TAG = "QSB.DefaultCorpusRanker";

    private final ShortcutRepository mShortcuts;

    private final Corpora mCorpora;

    // Cached list of ranked corpora.
    private final RankedCorporaCache mRankedCorpora;

    /**
     * Creates a new default corpus ranker.
     *
     * @param corpora Corpora to rank.
     * @param shortcuts Shortcut repository for getting corpus scores.
     */
    public DefaultCorpusRanker(Corpora corpora, ShortcutRepository shortcuts) {
        mCorpora = corpora;
        mCorpora.registerDataSetObserver(new CorporaObserver());
        mShortcuts = shortcuts;
        mRankedCorpora = new RankedCorporaCache();
    }

    public void getCorporaInAll(Consumer<List<Corpus>> consumer) {
        mRankedCorpora.getLater(consumer);
    }

    public void clear() {
        mRankedCorpora.clear();
    }

    private class CorporaObserver extends DataSetObserver {
        @Override
        public void onChanged() {
            clear();
        }
    }

    private class RankedCorporaCache extends CachedLater<List<Corpus>> {

        @Override
        protected void create() {
            mShortcuts.getCorpusScores(new Consumer<Map<String,Integer>>(){
                public boolean consume(Map<String, Integer> clickScores) {
                    Collection<Corpus> enabledCorpora = mCorpora.getCorporaInAll();
                    if (DBG) Log.d(TAG, "Ranking: " + enabledCorpora);
                    ArrayList<Corpus> ordered = new ArrayList<Corpus>(enabledCorpora);
                    Collections.sort(ordered, new CorpusComparator(clickScores));

                    if (DBG) Log.d(TAG, "Click scores: " + clickScores);
                    if (DBG) Log.d(TAG, "Ordered: " + ordered);

                    store(ordered);
                    return true;
                }
            });
        }

    }

    private static class CorpusComparator implements Comparator<Corpus> {
        private final Map<String,Integer> mClickScores;

        public CorpusComparator(Map<String,Integer> clickScores) {
            mClickScores = clickScores;
        }

        public int compare(Corpus corpus1, Corpus corpus2) {
            boolean corpus1IsDefault = corpus1.isCorpusDefaultEnabled();
            boolean corpus2IsDefault = corpus2.isCorpusDefaultEnabled();

            if (corpus1IsDefault != corpus2IsDefault) {
                // Default corpora always come before non-default
                return corpus1IsDefault ? -1 : 1;
            }

            // Then by descending score
            int scoreDiff = getCorpusScore(corpus2) - getCorpusScore(corpus1);
            if (scoreDiff != 0) {
                return scoreDiff;
            }

            // Finally by name
            return corpus1.getLabel().toString().compareTo(corpus2.getLabel().toString());
        }

        /**
         * Scores a corpus. Higher score is better.
         */
        private int getCorpusScore(Corpus corpus) {
            // Web corpus always comes first
            if (corpus.isWebCorpus()) {
                return Integer.MAX_VALUE;
            }
            // Then use click score
            return getClickScore(corpus);
        }

        private int getClickScore(Corpus corpus) {
            if (mClickScores == null) return 0;
            Integer clickScore = mClickScores.get(corpus.getName());
            return clickScore == null ? 0 : clickScore;
        }
    }

}
