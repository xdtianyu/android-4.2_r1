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
 * Mock implementation of {@link ShortcutRepository}.
 *
 */
public class MockShortcutRepository implements ShortcutRepository {

    private Map<String, Integer> mCorpusScores;

    public void setCorpusScores(Map<String, Integer> corpusScores) {
        mCorpusScores = corpusScores;
    }

    public void clearHistory() {
    }

    public void removeFromHistory(SuggestionCursor suggestions, int position) {
    }

    public void close() {
    }

    public void getShortcutsForQuery(String query, Collection<Corpus> corporaToQuery,
            boolean allowWebSearchShortcuts, Consumer<ShortcutCursor> consumer) {
        ShortcutCursor cursor = getShortcutsForQuery(query, corporaToQuery);
        consumer.consume(cursor);
    }

    /**
     * Synchronous version for use in tests that just need a ShortcutCursor.
     */
    public ShortcutCursor getShortcutsForQuery(String query, Collection<Corpus> corporaToQuery) {
        // TODO: should look at corporaToQuery
        ShortcutCursor cursor = new ShortcutCursor(query, null, null, null);
        cursor.add(MockSource.SOURCE_1.createSuggestion(query + "_1_shortcut"));
        cursor.add(MockSource.SOURCE_2.createSuggestion(query + "_2_shortcut"));
        return cursor;
    }

    public void updateShortcut(Source source, String shortcutId, SuggestionCursor refreshed) {
    }

    public void getCorpusScores(Consumer<Map<String, Integer>> consumer) {
        consumer.consume(mCorpusScores);
    }

    public void hasHistory(Consumer<Boolean> consumer) {
        consumer.consume(false);
    }

    public void reportClick(SuggestionCursor suggestions, int position) {
    }

}
