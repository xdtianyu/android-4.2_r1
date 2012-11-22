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

import java.util.HashSet;
import java.util.Set;

/**
 * Promotes shortcuts and suggestions from a single corpus.
 */
public class SingleCorpusPromoter implements Promoter {

    private final Corpus mCorpus;

    private final int mMaxShortcuts;

    private final Set<String> mAllowedSources;

    public SingleCorpusPromoter(Corpus corpus, int maxShortcuts) {
        mCorpus = corpus;
        mMaxShortcuts = maxShortcuts;
        mAllowedSources = new HashSet<String>();
        for (Source source : corpus.getSources()) {
            mAllowedSources.add(source.getName());
        }
    }

    public void pickPromoted(Suggestions suggestions, int maxPromoted,
            ListSuggestionCursor promoted) {
        // Add shortcuts
        SuggestionCursor shortcuts = suggestions.getShortcuts();
        promoteUntilFull(shortcuts, mMaxShortcuts, promoted);
        // Add suggestions
        CorpusResult corpusResult = suggestions.getCorpusResult(mCorpus);
        promoteUntilFull(corpusResult, maxPromoted, promoted);
    }

    private void promoteUntilFull(SuggestionCursor c, int maxSize, ListSuggestionCursor promoted) {
        if (c == null) return;
        int count = c.getCount();
        for (int i = 0; i < count && promoted.getCount() < maxSize; i++) {
            c.moveTo(i);
            if (accept(c)) {
                promoted.add(new SuggestionPosition(c, i));
            }
        }
    }

    protected boolean accept(Suggestion s) {
        return mAllowedSources.contains(s.getSuggestionSource().getName());
    }

}
