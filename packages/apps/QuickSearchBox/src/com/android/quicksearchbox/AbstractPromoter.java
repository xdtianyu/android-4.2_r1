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
 * Abstract {@link Promoter} that uses a {@link SuggestionFilter} to choose shortcuts.
 */
public abstract class AbstractPromoter implements Promoter {

    private final SuggestionFilter mFilter;
    private final Promoter mNext;
    private final Config mConfig;

    protected AbstractPromoter(SuggestionFilter filter, Promoter next, Config config) {
        mFilter = filter;
        mNext = next;
        mConfig = config;
    }

    public void pickPromoted(
            Suggestions suggestions, int maxPromoted, ListSuggestionCursor promoted) {
        doPickPromoted(suggestions, maxPromoted, promoted);
        if (mNext != null) {
            mNext.pickPromoted(suggestions, maxPromoted, promoted);
        }
    }

    protected abstract void doPickPromoted(
            Suggestions suggestions, int maxPromoted, ListSuggestionCursor promoted);

    protected Config getConfig() {
        return mConfig;
    }

    protected boolean accept(Suggestion s) {
        if (mFilter != null) {
            return mFilter.accept(s);
        } else {
            return true;
        }
    }

}
