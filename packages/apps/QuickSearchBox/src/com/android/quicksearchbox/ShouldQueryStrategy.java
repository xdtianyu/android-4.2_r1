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

import android.text.TextUtils;
import android.util.Log;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Decides whether a given source should be queried for a given query, taking
 * into account the source's query threshold and query after zero results flag.
 *
 * This class is thread safe.
 */
class ShouldQueryStrategy {

    private static final boolean DBG = false;
    private static final String TAG = "QSB.ShouldQueryStrategy";

    // The last query we've seen
    private String mLastQuery = "";

    private final Config mConfig;

    // The current implementation keeps a record of those corpora that have
    // returned zero results for some prefix of the current query. mEmptyCorpora
    // maps from corpus to the length of the query which returned
    // zero results.  When a query is shortened (e.g., by deleting characters)
    // or changed entirely, mEmptyCorpora is pruned (in updateQuery)
    private final HashMap<Corpus, Integer> mEmptyCorpora
            = new HashMap<Corpus, Integer>();

    public ShouldQueryStrategy(Config config) {
        mConfig = config;
    }

    /**
     * Returns whether we should query the given source for the given query.
     */
    public boolean shouldQueryCorpus(Corpus corpus, String query) {
        updateQuery(query);
        if (query.length() == 0
                && !corpus.isWebCorpus() // always query web, to warm up connection
                && !mConfig.showSuggestionsForZeroQuery()) {
                return false;
        }
        if (query.length() >= corpus.getQueryThreshold()) {
            if (!corpus.queryAfterZeroResults() && mEmptyCorpora.containsKey(corpus)) {
                if (DBG) Log.i(TAG, "Not querying " + corpus + ", returned 0 after "
                        + mEmptyCorpora.get(corpus));
                return false;
            }
            return true;
        }
        if (DBG) Log.d(TAG, "Query too short for corpus " + corpus);
        return false;
    }

    /**
     * Called to notify ShouldQueryStrategy when a source reports no results for a query.
     */
    public void onZeroResults(Corpus corpus, String query) {
        // Make sure this result is actually for a prefix of the current query.
        if (mLastQuery.startsWith(query) && !corpus.queryAfterZeroResults()
                && !TextUtils.isEmpty(query)) {
            if (DBG) Log.d(TAG, corpus + " returned 0 results for '" + query + "'");
            mEmptyCorpora.put(corpus, query.length());
        }
    }

    private void updateQuery(String query) {
        if (query.startsWith(mLastQuery)) {
            // This is a refinement of the last query, no changes to mEmptyCorpora needed
        } else if (mLastQuery.startsWith(query)) {
            // This is a widening of the last query: clear out any sources
            // that reported zero results after this query.
            Iterator<Map.Entry<Corpus, Integer>> iter = mEmptyCorpora.entrySet().iterator();
            while (iter.hasNext()) {
                if (iter.next().getValue() > query.length()) {
                    iter.remove();
                }
            }
        } else {
            // This is a completely different query, clear everything.
            mEmptyCorpora.clear();
        }
        mLastQuery = query;
    }
}
