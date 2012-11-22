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

import com.android.quicksearchbox.util.BatchingNamedTaskExecutor;
import com.android.quicksearchbox.util.Consumer;
import com.android.quicksearchbox.util.NamedTaskExecutor;
import com.android.quicksearchbox.util.NoOpConsumer;

import android.os.Handler;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Suggestions provider implementation.
 *
 * The provider will only handle a single query at a time. If a new query comes
 * in, the old one is cancelled.
 */
public class SuggestionsProviderImpl implements SuggestionsProvider {

    private static final boolean DBG = false;
    private static final String TAG = "QSB.SuggestionsProviderImpl";

    private final Config mConfig;

    private final NamedTaskExecutor mQueryExecutor;

    private final Handler mPublishThread;

    private final ShouldQueryStrategy mShouldQueryStrategy;

    private final Logger mLogger;

    private BatchingNamedTaskExecutor mBatchingExecutor;

    public SuggestionsProviderImpl(Config config,
            NamedTaskExecutor queryExecutor,
            Handler publishThread,
            Logger logger) {
        mConfig = config;
        mQueryExecutor = queryExecutor;
        mPublishThread = publishThread;
        mLogger = logger;
        mShouldQueryStrategy = new ShouldQueryStrategy(mConfig);
    }

    public void close() {
        cancelPendingTasks();
    }

    /**
     * Cancels all pending query tasks.
     */
    private void cancelPendingTasks() {
        if (mBatchingExecutor != null) {
            mBatchingExecutor.cancelPendingTasks();
            mBatchingExecutor = null;
        }
    }

    /**
     * Gets the sources that should be queried for the given query.
     */
    private List<Corpus> filterCorpora(String query, List<Corpus> corpora) {
        // If there is only one corpus, always query it
        if (corpora.size() <= 1) return corpora;
        ArrayList<Corpus> corporaToQuery = new ArrayList<Corpus>(corpora.size());
        for (Corpus corpus : corpora) {
            if (shouldQueryCorpus(corpus, query)) {
                if (DBG) Log.d(TAG, "should query corpus " + corpus);
                corporaToQuery.add(corpus);
            } else {
                if (DBG) Log.d(TAG, "should NOT query corpus " + corpus);
            }
        }
        if (DBG) Log.d(TAG, "getCorporaToQuery corporaToQuery=" + corporaToQuery);
        return corporaToQuery;
    }

    protected boolean shouldQueryCorpus(Corpus corpus, String query) {
        return mShouldQueryStrategy.shouldQueryCorpus(corpus, query);
    }

    private void updateShouldQueryStrategy(CorpusResult cursor) {
        if (cursor.getCount() == 0) {
            mShouldQueryStrategy.onZeroResults(cursor.getCorpus(),
                    cursor.getUserQuery());
        }
    }

    public Suggestions getSuggestions(String query, List<Corpus> corporaToQuery) {
        if (DBG) Log.d(TAG, "getSuggestions(" + query + ")");
        corporaToQuery = filterCorpora(query, corporaToQuery);
        final Suggestions suggestions = new Suggestions(query, corporaToQuery);
        Log.i(TAG, "chars:" + query.length() + ",corpora:" + corporaToQuery);

        // Fast path for the zero sources case
        if (corporaToQuery.size() == 0) {
            return suggestions;
        }

        int initialBatchSize = countDefaultCorpora(corporaToQuery);
        if (initialBatchSize == 0) {
            initialBatchSize = mConfig.getNumPromotedSources();
        }

        mBatchingExecutor = new BatchingNamedTaskExecutor(mQueryExecutor);

        long publishResultDelayMillis = mConfig.getPublishResultDelayMillis();

        Consumer<CorpusResult> receiver;
        if (shouldDisplayResults(query)) {
            receiver = new SuggestionCursorReceiver(
                    mBatchingExecutor, suggestions, initialBatchSize,
                    publishResultDelayMillis);
        } else {
            receiver = new NoOpConsumer<CorpusResult>();
            suggestions.done();
        }

        int maxResultsPerSource = mConfig.getMaxResultsPerSource();
        QueryTask.startQueries(query, maxResultsPerSource, corporaToQuery, mBatchingExecutor,
                mPublishThread, receiver, corporaToQuery.size() == 1);
        mBatchingExecutor.executeNextBatch(initialBatchSize);

        return suggestions;
    }

    private int countDefaultCorpora(List<Corpus> corpora) {
        int count = 0;
        for (Corpus corpus : corpora) {
            if (corpus.isCorpusDefaultEnabled()) {
                count++;
            }
        }
        return count;
    }

    private boolean shouldDisplayResults(String query) {
        if (query.length() == 0 && !mConfig.showSuggestionsForZeroQuery()) {
            // Note that even though we don't display such results, it's
            // useful to run the query itself because it warms up the network
            // connection.
            return false;
        }
        return true;
    }


    private class SuggestionCursorReceiver implements Consumer<CorpusResult> {
        private final BatchingNamedTaskExecutor mExecutor;
        private final Suggestions mSuggestions;
        private final long mResultPublishDelayMillis;
        private final ArrayList<CorpusResult> mPendingResults;
        private final Runnable mResultPublishTask = new Runnable () {
            public void run() {
                if (DBG) Log.d(TAG, "Publishing delayed results");
                publishPendingResults();
            }
        };

        private int mCountAtWhichToExecuteNextBatch;

        public SuggestionCursorReceiver(BatchingNamedTaskExecutor executor,
                Suggestions suggestions, int initialBatchSize,
                long publishResultDelayMillis) {
            mExecutor = executor;
            mSuggestions = suggestions;
            mCountAtWhichToExecuteNextBatch = initialBatchSize;
            mResultPublishDelayMillis = publishResultDelayMillis;
            mPendingResults = new ArrayList<CorpusResult>();
        }

        public boolean consume(CorpusResult cursor) {
            if (DBG) {
                Log.d(TAG, "SuggestionCursorReceiver.consume(" + cursor + ") corpus=" +
                        cursor.getCorpus() + " count = " + cursor.getCount());
            }
            updateShouldQueryStrategy(cursor);
            mPendingResults.add(cursor);
            if (mResultPublishDelayMillis > 0
                    && !mSuggestions.isClosed()
                    && mSuggestions.getResultCount() + mPendingResults.size()
                            < mCountAtWhichToExecuteNextBatch) {
                // This is not the last result of the batch, delay publishing
                if (DBG) Log.d(TAG, "Delaying result by " + mResultPublishDelayMillis + " ms");
                mPublishThread.removeCallbacks(mResultPublishTask);
                mPublishThread.postDelayed(mResultPublishTask, mResultPublishDelayMillis);
            } else {
                // This is the last result, publish immediately
                if (DBG) Log.d(TAG, "Publishing result immediately");
                mPublishThread.removeCallbacks(mResultPublishTask);
                publishPendingResults();
            }
            if (!mSuggestions.isClosed()) {
                executeNextBatchIfNeeded();
            }
            if (cursor != null && mLogger != null) {
                mLogger.logLatency(cursor);
            }
            return true;
        }

        private void publishPendingResults() {
            mSuggestions.addCorpusResults(mPendingResults);
            mPendingResults.clear();
        }

        private void executeNextBatchIfNeeded() {
            if (mSuggestions.getResultCount() == mCountAtWhichToExecuteNextBatch) {
                // We've just finished one batch, ask for more
                int nextBatchSize = mConfig.getNumPromotedSources();
                mCountAtWhichToExecuteNextBatch += nextBatchSize;
                mExecutor.executeNextBatch(nextBatchSize);
            }
        }
    }
}
