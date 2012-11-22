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
 * A sequence of suggestions from a single corpus.
 */
public interface CorpusResult extends SuggestionCursor {

    /**
     * Gets the corpus that produced these suggestions.
     */
    Corpus getCorpus();

    /**
     * The user query that returned these suggestions.
     */
    String getUserQuery();

    /**
     * Gets the latency of the suggestion query that produced this result.
     *
     * @return The latency in milliseconds.
     */
    int getLatency();
}
