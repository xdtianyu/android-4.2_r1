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

/**
 * Promotes non-web-search shortcuts and suggestions from a single corpus.
 */
public class SingleCorpusResultsPromoter extends SingleCorpusPromoter {

    public SingleCorpusResultsPromoter(Corpus corpus, int maxShortcuts) {
        super(corpus, maxShortcuts);
    }

    protected boolean accept(Suggestion s) {
        return !s.isWebSearchSuggestion() && super.accept(s);
    }

}
