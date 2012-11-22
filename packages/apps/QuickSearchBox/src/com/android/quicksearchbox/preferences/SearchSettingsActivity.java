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
import com.android.quicksearchbox.R;

import android.preference.PreferenceActivity;
import android.util.Log;
import android.view.Menu;

import java.util.List;

/**
 * Activity for setting global search preferences.
 */
public class SearchSettingsActivity extends PreferenceActivity {
    private static final String TAG = "QSB.SearchSettingsActivity";
    private static final boolean DBG = false;

    private static final String CLEAR_SHORTCUTS_FRAGMENT = DeviceSearchFragment.class.getName();

    private static final String ACTIVITY_HELP_CONTEXT = "settings";

    /**
     * Populate the activity with the top-level headers.
     */
    @Override
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.preferences_headers, target);
        onHeadersBuilt(target);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getQsbApplication().getHelp().addHelpMenuItem(menu, ACTIVITY_HELP_CONTEXT, true);
        return true;
    }

    protected QsbApplication getQsbApplication() {
        return QsbApplication.get(this);
    }

    /**
     * Get the name of the fragment that contains only a 'clear shortcuts' preference, and hence
     * can be removed if zero-query shortcuts are disabled. Returns null if no such fragment exists.
     */
    protected String getShortcutsOnlyFragment() {
        return CLEAR_SHORTCUTS_FRAGMENT;
    }

    protected void onHeadersBuilt(List<Header> target) {
        String shortcutsFragment = getShortcutsOnlyFragment();
        if (shortcutsFragment == null) return;
        if (DBG) Log.d(TAG, "onHeadersBuilt shortcutsFragment=" + shortcutsFragment);
        if (!QsbApplication.get(this).getConfig().showShortcutsForZeroQuery()) {
            // remove 'clear shortcuts'
            for (int i = 0; i < target.size(); ++i) {
                String fragment = target.get(i).fragment;
                if (DBG) Log.d(TAG, "fragment " + i + ": " + fragment);
                if (shortcutsFragment.equals(fragment)) {
                    target.remove(i);
                    break;
                }
            }
        }
    }

}
