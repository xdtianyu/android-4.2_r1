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

import com.android.quicksearchbox.QsbApplication;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;

/**
 * System search settings fragment.
 */
public abstract class SettingsFragmentBase extends PreferenceFragment {

    // Name of the preferences file used to store search preference
    public static final String PREFERENCES_NAME = "SearchSettings";

    private PreferenceController mController;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mController = createController();

        getPreferenceManager().setSharedPreferencesName(getPreferencesName());

        addPreferencesFromResource(getPreferencesResourceId());

        handlePreferenceGroup(getPreferenceScreen());

        mController.onCreateComplete();
    }

    protected PreferenceController getController() {
        return mController;
    }

    protected PreferenceControllerFactory createController() {
        QsbApplication app = QsbApplication.get(getActivity());
        return app.createPreferenceControllerFactory(getActivity());
    }

    protected String getPreferencesName() {
        return PREFERENCES_NAME;
    }

    protected abstract int getPreferencesResourceId();

    protected void handlePreferenceGroup(PreferenceGroup group) {
        for (int i = 0; i < group.getPreferenceCount(); ++i) {
            Preference p = group.getPreference(i);
            if (p instanceof PreferenceCategory) {
                handlePreferenceGroup((PreferenceCategory) p);
            } else {
                mController.handlePreference(group.getPreference(i));
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mController.onResume();
    }

    @Override
    public void onStop() {
        mController.onStop();
        super.onStop();
    }

    @Override
    public void onDestroy() {
        mController.onDestroy();
        super.onDestroy();
    }
}
