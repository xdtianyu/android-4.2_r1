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

import com.android.quicksearchbox.util.NamedTask;
import com.android.quicksearchbox.util.NamedTaskExecutor;

import android.util.Log;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Refreshes shortcuts from their source.
 */
class SourceShortcutRefresher implements ShortcutRefresher {
    private static final String TAG = "QSB.SourceShortcutRefresher";
    private static final boolean DBG = false;

    private final NamedTaskExecutor mExecutor;

    private final Set<String> mRefreshed = Collections.synchronizedSet(new HashSet<String>());
    private final Set<String> mRefreshing = Collections.synchronizedSet(new HashSet<String>());

    /**
     * Create a ShortcutRefresher that will refresh shortcuts using the given executor.
     *
     * @param executor Used to execute the tasks.
     */
    public SourceShortcutRefresher(NamedTaskExecutor executor) {
        mExecutor = executor;
    }

    public void refresh(Suggestion shortcut, Listener listener) {
        Source source = shortcut.getSuggestionSource();
        if (source == null) {
            throw new NullPointerException("source");
        }
        String shortcutId = shortcut.getShortcutId();
        if (shouldRefresh(source, shortcutId) && !isRefreshing(source, shortcutId)) {
            if (DBG) {
                Log.d(TAG, "Refreshing shortcut  " + shortcutId + " '" +
                        shortcut.getSuggestionText1() + "'");
            }
            markShortcutRefreshing(source, shortcutId);
            String extraData = shortcut.getSuggestionIntentExtraData();
            ShortcutRefreshTask refreshTask = new ShortcutRefreshTask(
                    source, shortcutId, extraData, listener);
            mExecutor.execute(refreshTask);
        }
    }

    /**
     * Returns true if the given shortcut requires refreshing.
     */
    public boolean shouldRefresh(Source source, String shortcutId) {
        return source != null && shortcutId != null
                && !mRefreshed.contains(makeKey(source, shortcutId));
    }

    public boolean isRefreshing(Source source, String shortcutId) {
        return source != null && shortcutId != null
                && mRefreshing.contains(makeKey(source, shortcutId));
    }

    private void markShortcutRefreshing(Source source, String shortcutId) {
        mRefreshing.add(makeKey(source, shortcutId));
    }

    /**
     * Indicate that the shortcut no longer requires refreshing.
     */
    public void markShortcutRefreshed(Source source, String shortcutId) {
        String key = makeKey(source, shortcutId);
        mRefreshed.add(key);
        mRefreshing.remove(key);
    }

    /**
     * Reset internal state.  This results in all shortcuts requiring refreshing.
     */
    public void reset() {
        mRefreshed.clear();
    }

    private static String makeKey(Source source, String shortcutId) {
        return source.getName() + "#" + shortcutId;
    }

    /**
     * Refreshes a shortcut with a source and reports the result to a
     * {@link ShortcutRefresher.Listener}.
     */
    private class ShortcutRefreshTask implements NamedTask {
        private final Source mSource;
        private final String mShortcutId;
        private final String mExtraData;
        private final Listener mListener;

        /**
         * @param source The source that should validate the shortcut.
         * @param shortcutId The shortcut to be refreshed.
         * @param listener Who to report back to when the result is in.
         */
        ShortcutRefreshTask(Source source, String shortcutId, String extraData,
                Listener listener) {
            mSource = source;
            mShortcutId = shortcutId;
            mExtraData = extraData;
            mListener = listener;
        }

        public String getName() {
            return mSource.getName();
        }

        public void run() {
            // TODO: Add latency tracking and logging.
            SuggestionCursor refreshed = mSource.refreshShortcut(mShortcutId, mExtraData);
            // Close cursor if empty and pass null as the refreshed cursor
            if (refreshed != null && refreshed.getCount() == 0) {
                refreshed.close();
                refreshed = null;
            }
            markShortcutRefreshed(mSource, mShortcutId);
            mListener.onShortcutRefreshed(mSource, mShortcutId, refreshed);
        }

    }
}
