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

import com.google.common.annotations.VisibleForTesting;

import android.util.Log;

import java.util.Iterator;
import java.util.LinkedList;

/**
 * A promoter that gives preference to suggestions from higher ranking corpora.
 */
public class RankAwarePromoter extends AbstractPromoter {

    private static final boolean DBG = false;
    private static final String TAG = "QSB.RankAwarePromoter";

    public RankAwarePromoter(Config config, SuggestionFilter filter, Promoter next) {
        super(filter, next, config);
    }

    @Override
    public void doPickPromoted(Suggestions suggestions,
            int maxPromoted, ListSuggestionCursor promoted) {
        promoteSuggestions(suggestions.getCorpusResults(), maxPromoted, promoted);
    }

    @VisibleForTesting
    void promoteSuggestions(Iterable<CorpusResult> suggestions, int maxPromoted,
            ListSuggestionCursor promoted) {
        if (DBG) Log.d(TAG, "Available results: " + suggestions);

        // Split non-empty results into important suggestions and not-so-important
        // suggestions, each corpus's cursor positioned at the first suggestion.
        LinkedList<CorpusResult> highRankingSuggestions = new LinkedList<CorpusResult>();
        LinkedList<CorpusResult> lowRankingSuggestions = new LinkedList<CorpusResult>();
        partitionSuggestionsByRank(suggestions, highRankingSuggestions, lowRankingSuggestions);

        // Top results, evenly distributed between each high-ranking corpus.
        promoteTopSuggestions(highRankingSuggestions, promoted, maxPromoted);

        // Then try to fill promoted list with the remaining high-ranking suggestions,
        // and then use the low-ranking suggestions if the list isn't full yet.
        promoteEquallyFromEachCorpus(highRankingSuggestions, promoted, maxPromoted);
        promoteEquallyFromEachCorpus(lowRankingSuggestions, promoted, maxPromoted);

        if (DBG) Log.d(TAG, "Returning " + promoted.toString());
    }

    /**
     * Shares the top slots evenly among each of the high-ranking (default) corpora.
     *
     * The corpora will appear in the promoted list in the order they are listed
     * among the incoming suggestions (this method doesn't change their order).
     */
    private void promoteTopSuggestions(LinkedList<CorpusResult> highRankingSuggestions,
            ListSuggestionCursor promoted, int maxPromoted) {

        int slotsLeft = getSlotsLeft(promoted, maxPromoted);
        if (slotsLeft > 0 && !highRankingSuggestions.isEmpty()) {
            int slotsToFill = Math.min(getSlotsAboveKeyboard() - promoted.getCount(), slotsLeft);

            if (slotsToFill > 0) {
                int stripeSize = Math.max(1, slotsToFill / highRankingSuggestions.size());
                roundRobin(highRankingSuggestions, slotsToFill, stripeSize, promoted);
            }
        }
    }

    /**
     * Tries to promote the same number of elements from each corpus.
     *
     * The corpora will appear in the promoted list in the order they are listed
     * among the incoming suggestions (this method doesn't change their order).
     */
    private void promoteEquallyFromEachCorpus(LinkedList<CorpusResult> suggestions,
            ListSuggestionCursor promoted, int maxPromoted) {

        int slotsLeft = getSlotsLeft(promoted, maxPromoted);
        if (slotsLeft == 0) {
            // No more items to add.
            return;
        }

        if (suggestions.isEmpty()) {
            return;
        }

        int stripeSize = Math.max(1, slotsLeft / suggestions.size());
        roundRobin(suggestions, slotsLeft, stripeSize, promoted);

        // We may still have a few slots left
        slotsLeft = getSlotsLeft(promoted, maxPromoted);
        roundRobin(suggestions, slotsLeft, slotsLeft, promoted);
    }

    /**
     * Partitions the suggestions into "important" (high-ranking)
     * and "not-so-important" (low-ranking) suggestions, dependent on the
     * rank of the corpus the result is part of.
     *
     * @param suggestions
     * @param highRankingSuggestions These should be displayed first to the
     *     user.
     * @param lowRankingSuggestions These should be displayed if the
     *     high-ranking suggestions don't fill all the available space in the
     *     result view.
     */
    private void partitionSuggestionsByRank(Iterable<CorpusResult> suggestions,
            LinkedList<CorpusResult> highRankingSuggestions,
            LinkedList<CorpusResult> lowRankingSuggestions) {

        for (CorpusResult result : suggestions) {
            if (result.getCount() > 0) {
                result.moveTo(0);
                Corpus corpus = result.getCorpus();
                if (isCorpusHighlyRanked(corpus)) {
                    highRankingSuggestions.add(result);
                } else {
                    lowRankingSuggestions.add(result);
                }
            }
        }
    }

    private boolean isCorpusHighlyRanked(Corpus corpus) {
        // The default corpora shipped with QSB (apps, etc.) are
        // more important than ones that were registered later.
        return corpus == null || corpus.isCorpusDefaultEnabled();
    }

    private int getSlotsLeft(ListSuggestionCursor promoted, int maxPromoted) {
        // It's best to calculate this after each addition because duplicates
        // may get filtered out automatically in the list of promoted items.
        return Math.max(0, maxPromoted - promoted.getCount());
    }

    private int getSlotsAboveKeyboard() {
        return getConfig().getNumSuggestionsAboveKeyboard();
    }

    /**
     * Promotes "stripes" of suggestions from each corpus.
     *
     * @param results     the list of CorpusResults from which to promote.
     *                    Exhausted CorpusResults are removed from the list.
     * @param maxPromoted maximum number of suggestions to promote.
     * @param stripeSize  number of suggestions to take from each corpus.
     * @param promoted    the list to which promoted suggestions are added.
     * @return the number of suggestions actually promoted.
     */
    private int roundRobin(LinkedList<CorpusResult> results, int maxPromoted, int stripeSize,
            ListSuggestionCursor promoted) {
        int count = 0;
        if (maxPromoted > 0 && !results.isEmpty()) {
            for (Iterator<CorpusResult> iter = results.iterator();
                 count < maxPromoted && iter.hasNext();) {
                CorpusResult result = iter.next();
                count += promote(result, stripeSize, promoted);
                if (result.getPosition() == result.getCount()) {
                    iter.remove();
                }
            }
        }
        return count;
    }

    /**
     * Copies suggestions from a SuggestionCursor to the list of promoted suggestions.
     *
     * @param cursor from which to copy the suggestions
     * @param count maximum number of suggestions to copy
     * @param promoted the list to which to add the suggestions
     * @return the number of suggestions actually copied.
     */
    private int promote(SuggestionCursor cursor, int count, ListSuggestionCursor promoted) {
        if (count < 1 || cursor.getPosition() >= cursor.getCount()) {
            return 0;
        }
        int addedCount = 0;
        do {
            if (accept(cursor)) {
                if (promoted.add(new SuggestionPosition(cursor))) {
                    // Added successfully (wasn't already promoted).
                    addedCount++;
                }
            }
        } while (cursor.moveToNext() && addedCount < count);
        return addedCount;
    }

}
