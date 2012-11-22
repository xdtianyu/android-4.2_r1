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

import com.android.quicksearchbox.util.Consumer;

import java.util.Collection;
import java.util.Map;

/**
 * Holds information about shortcuts (results the user has clicked on before), and returns
 * appropriate shortcuts for a given query.
 */
public interface ShortcutRepository {

    /**
     * Checks whether there is any stored history.
     *
     * @param consumer Consumer that the result will be passed to.
     *        The value passed to the consumer will always be non-null.
     *        The consumer will be called on an unspecified thread, and will always
     *        get called eventually.
     */
    void hasHistory(Consumer<Boolean> consumer);

    /**
     * Removes a single suggestion from the stored history.
     */
    void removeFromHistory(SuggestionCursor suggestions, int position);

    /**
     * Clears all shortcut history.
     */
    void clearHistory();

    /**
     * Closes any database connections etc held by this object.
     */
    void close();

    /**
     * Reports a click on a suggestion.
     * Must be called on the UI thread.
     */
    void reportClick(SuggestionCursor suggestions, int position);

    /**
     * Gets shortcuts for a query.
     *
     * @param query The query. May be empty.
     * @param allowedCorpora The corpora to get shortcuts for.
     * @param allowWebSearchShortcuts Whether to include web search shortcuts.
     * @param consumer Consumer that the shortcuts cursor will be passed to.
     *        The shortcut cursor passed to the consumer may be null if there are no shortcuts.
     *        If non-null, and the consumer returns {@code true}, the consumer must ensure that
     *        the shortcut cursor will get closed eventually.
     *        The consumer will be called on an unspecified thread, and will always
     *        get called eventually.
     */
    void getShortcutsForQuery(String query, Collection<Corpus> allowedCorpora,
            boolean allowWebSearchShortcuts,
            Consumer<ShortcutCursor> consumer);

    /**
     * Updates a shortcut in the repository after it's been refreshed.
     *
     * @param source The source of the shortcut that's been refreshed
     * @param shortcutId The ID of the shortcut that's been refershed
     * @param refreshed The refreshed shortcut suggestion.
     */
    void updateShortcut(Source source, String shortcutId, SuggestionCursor refreshed);

    /**
     * Gets scores for all corpora in the click log.
     *
     * @param consumer Consumer that the result will be passed to.
     *        The result is a map of corpus name to score. A higher score means that the corpus
     *        is more important.
     *        The value passed to the consumer may be non-null.
     *        The consumer will be called on an unspecified thread, and will always
     *        get called eventually.
     */
    void getCorpusScores(Consumer<Map<String,Integer>> consumer);
}
