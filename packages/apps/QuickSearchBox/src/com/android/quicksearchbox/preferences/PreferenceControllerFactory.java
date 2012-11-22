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

import com.android.quicksearchbox.Corpora;
import com.android.quicksearchbox.QsbApplication;
import com.android.quicksearchbox.SearchSettings;
import com.android.quicksearchbox.ShortcutRepository;

import android.content.Context;
import android.preference.Preference;

import java.util.ArrayList;

/**
 * Class to handle logic behind the preferences in settings.
 */
public class PreferenceControllerFactory implements PreferenceController {

    private final SearchSettings mSettings;
    private final Context mContext;
    private final ArrayList<PreferenceController> mControllers;

    public PreferenceControllerFactory(SearchSettings settings, Context context) {
        mSettings = settings;
        mContext = context;
        mControllers = new ArrayList<PreferenceController>();
    }

    protected Context getContext() {
        return mContext;
    }

    public void handlePreference(Preference p) {
        String key = p.getKey();
        if (key == null) return;
        if (SearchableItemsController.SEARCH_CORPORA_PREF.equals(key)) {
            Corpora corpora = QsbApplication.get(mContext).getCorpora();
            addController(new SearchableItemsController(mSettings, corpora, getContext()), p);
        } else if (ClearShortcutsController.CLEAR_SHORTCUTS_PREF.equals(key)) {
            ShortcutRepository shortcuts = QsbApplication.get(getContext()).getShortcutRepository();
            addController(new ClearShortcutsController(shortcuts), p);
        } else {
            throw new UnknownPreferenceException(p);
        }
    }

    public void onCreateComplete() {
        for (PreferenceController controller : mControllers) {
            controller.onCreateComplete();
        }
    }

    public void onResume() {
        for (PreferenceController controller : mControllers) {
            controller.onResume();
        }
    }

    public void onStop() {
        for (PreferenceController controller : mControllers) {
            controller.onStop();
        }
    }

    public void onDestroy() {
        for (PreferenceController controller : mControllers) {
            controller.onDestroy();
        }
    }

    protected void addController(PreferenceController controller, Preference forPreference) {
        mControllers.add(controller);
        controller.handlePreference(forPreference);
    }

    private static class UnknownPreferenceException extends RuntimeException {
        public UnknownPreferenceException(Preference p) {
            super("Preference key " + p.getKey() + "; class: " + p.getClass().toString());
        }
    }
}
