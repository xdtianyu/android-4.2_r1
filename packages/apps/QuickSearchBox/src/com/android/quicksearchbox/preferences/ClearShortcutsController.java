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
package com.android.quicksearchbox.preferences;

import com.android.quicksearchbox.ShortcutRepository;
import com.android.quicksearchbox.util.Consumer;
import com.android.quicksearchbox.util.Consumers;

import android.os.Handler;
import android.preference.Preference;
import android.util.Log;

/**
 * Logic behind the 'clear shortcuts' preference.
 */
public class ClearShortcutsController implements PreferenceController {

    public static final String CLEAR_SHORTCUTS_PREF = "clear_shortcuts";

    private static final boolean DBG = false;
    private static final String TAG = "QSB.ClearShortcutsController";

    private final ShortcutRepository mShortcuts;
    private final Handler mHandler = new Handler();

    private OkCancelPreference mClearShortcutsPreference;


    public ClearShortcutsController(ShortcutRepository shortcuts) {
        mShortcuts = shortcuts;
    }

    @Override
    public void handlePreference(Preference p) {
        mClearShortcutsPreference = (OkCancelPreference) p;
        mClearShortcutsPreference.setListener(new OkCancelPreference.Listener() {
            @Override
            public void onDialogClosed(boolean okClicked) {
                if (okClicked) {
                    clearShortcuts();
                }
            }
        });
    }

    public void onCreateComplete() {
    }

    public void onStop() {
    }

    public void onDestroy() {
    }

    @Override
    public void onResume() {
        updateClearShortcutsPreference();
    }

    /**
     * Enables/disables the "Clear search shortcuts" preference depending
     * on whether there is any search history.
     */
    private void updateClearShortcutsPreference() {
        mShortcuts.hasHistory(Consumers.createAsyncConsumer(mHandler, new Consumer<Boolean>() {
            @Override
            public boolean consume(Boolean hasHistory) {
                if (DBG) Log.d(TAG, "hasHistory()=" + hasHistory);
                mClearShortcutsPreference.setEnabled(hasHistory);
                return true;
            }
        }));
    }

    private void clearShortcuts() {
        Log.i(TAG, "Clearing shortcuts...");
        mShortcuts.clearHistory();
        mClearShortcutsPreference.setEnabled(false);
    }
}
