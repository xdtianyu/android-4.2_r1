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
import com.android.quicksearchbox.Corpus;
import com.android.quicksearchbox.R;
import com.android.quicksearchbox.SearchSettings;
import com.android.quicksearchbox.SearchSettingsImpl;

import android.content.Context;
import android.content.res.Resources;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceGroup;
import android.util.Log;

/**
 * Logic backing the searchable items activity or fragment.
 */
public class SearchableItemsController implements PreferenceController, OnPreferenceChangeListener {

    private static final boolean DBG = false;
    private static final String TAG = "QSB.SearchableItemsSettings";

    public static final String SEARCH_CORPORA_PREF = "search_corpora";

    private final SearchSettings mSearchSettings;
    private final Corpora mCorpora;
    private final Context mContext;

    // References to the top-level preference objects
    private PreferenceGroup mCorporaPreferences;

    public SearchableItemsController(SearchSettings searchSettings, Corpora corpora,
            Context context) {
        mSearchSettings = searchSettings;
        mCorpora = corpora;
        mContext = context;
    }

    public void handlePreference(Preference corporaPreferences) {
        mCorporaPreferences = (PreferenceGroup) corporaPreferences;
        populateSourcePreference();
    }

    public String getCorporaPreferenceKey() {
        return SEARCH_CORPORA_PREF;
    }

    private SearchSettings getSettings() {
        return mSearchSettings;
    }

    private Corpora getCorpora() {
        return mCorpora;
    }

    private Context getContext() {
        return mContext;
    }

    private Resources getResources() {
        return getContext().getResources();
    }


    /**
     * Fills the suggestion source list.
     */
    private void populateSourcePreference() {
        boolean includeNonAllCorpora =
                getResources().getBoolean(R.bool.show_non_all_corpora_in_settings);
        mCorporaPreferences.setOrderingAsAdded(false);
        for (Corpus corpus : getCorpora().getAllCorpora()) {
            if (includeNonAllCorpora || corpus.includeInAll()) {
                Preference pref = createCorpusPreference(corpus);
                if (pref != null) {
                    if (DBG) Log.d(TAG, "Adding corpus: " + corpus);
                    mCorporaPreferences.addPreference(pref);
                }
            }
        }
    }

    /**
     * Adds a suggestion source to the list of suggestion source checkbox preferences.
     */
    private Preference createCorpusPreference(Corpus corpus) {
        SearchableItemPreference sourcePref = new SearchableItemPreference(getContext());
        sourcePref.setKey(SearchSettingsImpl.getCorpusEnabledPreference(corpus));
        // Put web corpus first. The rest are alphabetical.
        if (corpus.isWebCorpus()) {
            sourcePref.setOrder(0);
        }
        sourcePref.setDefaultValue(corpus.isCorpusDefaultEnabled());
        sourcePref.setOnPreferenceChangeListener(this);
        CharSequence label = corpus.getLabel();
        sourcePref.setTitle(label);
        CharSequence description = corpus.getSettingsDescription();
        sourcePref.setSummaryOn(description);
        sourcePref.setSummaryOff(description);
        sourcePref.setIcon(corpus.getCorpusIcon());
        return sourcePref;
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        getSettings().broadcastSettingsChanged();
        return true;
    }

    public void onCreateComplete() {
    }

    public void onStop() {
    }

    public void onDestroy() {
    }

    public void onResume() {
    }

}
