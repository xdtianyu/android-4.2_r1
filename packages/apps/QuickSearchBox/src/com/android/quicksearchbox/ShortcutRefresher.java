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
 * Fires off tasks to validate shortcuts, and reports the results back to a
 * {@link Listener}.
 */
public interface ShortcutRefresher {

    public interface Listener {
        /**
         * Called by the ShortcutRefresher when a shortcut has been refreshed.
         *
         * @param source source of this shortcut.
         * @param shortcutId the id of the shortcut.
         * @param refreshed the updated shortcut, or {@code null} if the shortcut
         *        is no longer valid and should be deleted.
         */
        void onShortcutRefreshed(Source source, String shortcutId,
                SuggestionCursor refreshed);
    }

    /**
     * Starts a task to refresh a single shortcut.
     *
     * @param shortcut The shortcut to be refreshed.
     * @param listener Who to report back to.
     */
    void refresh(Suggestion shortcut, Listener listener);

    /**
     * Returns true if the given shortcut requires refreshing.
     */
    boolean shouldRefresh(Source source, String shortcutId);

    /**
     * Indicates that the shortcut no longer requires refreshing.
     */
    public void markShortcutRefreshed(Source source, String shortcutId);

    /**
     * Resets internal state. This results in all shortcuts requiring refreshing.
     */
    public void reset();

}
