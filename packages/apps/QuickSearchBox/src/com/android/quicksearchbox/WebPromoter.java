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

import android.util.Log;


public class WebPromoter implements Promoter {

    private static final String TAG = "QSB.WebPromoter";
    private static final boolean DBG = false;

    private final int mMaxShortcuts;

    public WebPromoter(int maxShortcuts) {
        mMaxShortcuts = maxShortcuts;
    }

    public void pickPromoted(Suggestions suggestions, int maxPromoted,
            ListSuggestionCursor promoted) {
        // Add web shortcuts
        SuggestionCursor shortcuts = suggestions.getShortcuts();
        int shortcutCount = shortcuts == null ? 0 : shortcuts.getCount();
        if (DBG) Log.d(TAG, "Shortcut count: " + shortcutCount);
        int maxShortcutCount = Math.min(mMaxShortcuts, maxPromoted);
        for (int i = 0; i < shortcutCount && promoted.getCount() < maxShortcutCount; i++) {
            shortcuts.moveTo(i);
            if (shortcuts.isWebSearchSuggestion()) {
                if (DBG) Log.d(TAG, "Including shortcut " + i);
                promoted.add(new SuggestionPosition(shortcuts, i));
            } else {
                if (DBG) Log.d(TAG, "Skipping shortcut " + i);
            }
        }

        // Add web suggestion
        CorpusResult webResult = suggestions.getWebResult();
        int webCount = webResult == null ? 0 : webResult.getCount();
        if (DBG) Log.d(TAG, "Web suggestion count: " + webCount);
        for (int i = 0; i < webCount && promoted.getCount() < maxPromoted; i++) {
            webResult.moveTo(i);
            if (webResult.isWebSearchSuggestion()) {
                if (DBG) Log.d(TAG, "Including suggestion " + i);
                promoted.add(new SuggestionPosition(webResult, i));
            } else {
                if (DBG) Log.d(TAG, "Skipping suggestion " + i);
            }
        }
    }

}