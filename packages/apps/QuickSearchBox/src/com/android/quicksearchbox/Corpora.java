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

import android.database.DataSetObserver;

import java.util.Collection;
import java.util.List;

/**
 * Maintains the set of available and enabled corpora.
 */
public interface Corpora {

    /**
     * Gets all corpora, including the web corpus.
     *
     * @return Callers must not modify the returned collection.
     */
    Collection<Corpus> getAllCorpora();

    /**
     * Gets all enabled corpora.
     *
     * @return Callers must not modify the returned list.
     */
    List<Corpus> getEnabledCorpora();

    /**
     * Gets all corpora that should be included in the blended All mode.
     *
     * @return Callers must not modify the returned list.
     */
    List<Corpus> getCorporaInAll();

    /**
     * Gets a corpus by name.
     *
     * @return A corpus, or null.
     */
    Corpus getCorpus(String name);

    /**
     * Gets the web search corpus.
     *
     * @return The web search corpus, or {@code null} if there is no web search corpus.
     */
    Corpus getWebCorpus();

    /**
     * Gets a source by name.
     *
     * @param name Source name.
     * @return A source, or {@code null} if no source with the given name exists.
     */
    Source getSource(String name);

    /**
     * Gets the corpus that contains the given source.
     */
    Corpus getCorpusForSource(Source source);

    /**
     * Updates the corpora.
     */
    void update();

    /**
     * Registers an observer that is called when corpus set changes.
     *
     * @param observer gets notified when the data set changes.
     */
    void registerDataSetObserver(DataSetObserver observer);

    /**
     * Unregisters an observer that has previously been registered with
     * {@link #registerDataSetObserver(DataSetObserver)}
     *
     * @param observer the observer to unregister.
     */
    void unregisterDataSetObserver(DataSetObserver observer);
}
