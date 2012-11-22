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


/**
 * A CorpusResult backed by a single SourceResult.
 */
public class SingleSourceCorpusResult extends SuggestionCursorWrapper implements CorpusResult {

    private final Corpus mCorpus;

    private final int mLatency;

    public SingleSourceCorpusResult(Corpus corpus, String userQuery, SuggestionCursor cursor,
            int latency) {
        super(userQuery, cursor);
        mCorpus = corpus;
        mLatency = latency;
    }

    public Corpus getCorpus() {
        return mCorpus;
    }

    public int getLatency() {
        return mLatency;
    }

    @Override
    public String toString() {
        return getCorpus() + "[" + getUserQuery() + "]";
    }

}
