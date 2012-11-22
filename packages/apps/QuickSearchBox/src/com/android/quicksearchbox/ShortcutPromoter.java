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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.HashMultiset;

/**
 * A promoter limits the maximum number of shortcuts per source
 * (from non-web sources) and blends results
 * from multiple sources.
 */
public class ShortcutPromoter extends AbstractPromoter {

    public ShortcutPromoter(Config config, Promoter next, SuggestionFilter filter) {
        super(filter, next, config);
    }

    @Override
    public void doPickPromoted(Suggestions suggestions, int maxPromoted,
            ListSuggestionCursor promoted) {
        promoteShortcuts(suggestions.getShortcuts(), maxPromoted, promoted);
    }

    @VisibleForTesting
    void promoteShortcuts(SuggestionCursor shortcuts, int maxPromoted,
            ListSuggestionCursor promoted) {
        int shortcutCount = shortcuts == null ? 0 : shortcuts.getCount();
        if (shortcutCount == 0) return;
        HashMultiset<Source> sourceShortcutCounts = HashMultiset.create(shortcutCount);
        for (int i = 0; i < shortcutCount && promoted.getCount() < maxPromoted; i++) {
            shortcuts.moveTo(i);
            Source source = shortcuts.getSuggestionSource();
            if (source != null && accept(shortcuts)) {
                int prevCount = sourceShortcutCounts.add(source, 1);
                int maxShortcuts = source.getMaxShortcuts(getConfig());
                if (prevCount < maxShortcuts) {
                    promoted.add(new SuggestionPosition(shortcuts));
                }
            }
        }
    }

}
