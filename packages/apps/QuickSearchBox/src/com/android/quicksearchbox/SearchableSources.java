/*
 * Copyright (C) 2009 The Android Open Source Project
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

import com.android.quicksearchbox.util.NamedTaskExecutor;

import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Handler;
import android.util.Log;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/**
 * Maintains a list of search sources.
 */
public class SearchableSources implements Sources {

    // set to true to enable the more verbose debug logging for this file
    private static final boolean DBG = false;
    private static final String TAG = "QSB.SearchableSources";

    private final Context mContext;
    private final SearchManager mSearchManager;
    private final Handler mUiThread;
    private final Config mConfig;

    // All suggestion sources, by name.
    private HashMap<String, Source> mSources;

    // The web search source to use.
    private Source mWebSearchSource;

    private final NamedTaskExecutor mIconLoaderExecutor;

    /**
     *
     * @param context Used for looking up source information etc.
     */
    public SearchableSources(Context context, Handler uiThread,
            NamedTaskExecutor iconLoader, Config config) {
        mContext = context;
        mSearchManager = (SearchManager) context.getSystemService(Context.SEARCH_SERVICE);
        mUiThread = uiThread;
        mIconLoaderExecutor = iconLoader;
        mConfig = config;
    }

    protected Context getContext() {
        return mContext;
    }

    protected Handler getUiThreadHandler() {
        return mUiThread;
    }

    protected SearchManager getSearchManager() {
        return mSearchManager;
    }

    protected NamedTaskExecutor getIconLoaderExecutor() {
        return mIconLoaderExecutor;
    }

    protected Config getConfig() {
        return mConfig;
    }

    public Collection<Source> getSources() {
        return mSources.values();
    }

    public Source getSource(String name) {
        return mSources.get(name);
    }

    public Source getWebSearchSource() {
        return mWebSearchSource;
    }

    /**
     * Updates the list of suggestion sources.
     */
    public void update() {
        if (DBG) Log.d(TAG, "update()");
        mSources = new HashMap<String,Source>();

        addSearchableSources();

        addInternalSources();

        mWebSearchSource = createWebSearchSource();
        if (mWebSearchSource != null) {
            addSource(mWebSearchSource);
        }
    }

    protected void addInternalSources() {
    }

    private void addSearchableSources() {
        List<SearchableInfo> searchables = mSearchManager.getSearchablesInGlobalSearch();
        if (searchables == null) {
            Log.e(TAG, "getSearchablesInGlobalSearch() returned null");
            return;
        }
        for (SearchableInfo searchable : searchables) {
            SearchableSource source = createSearchableSource(searchable);
            if (source != null) {
                if (DBG) Log.d(TAG, "Created source " + source);
                addSource(source);
            }
        }
    }

    protected void addSource(Source source) {
        mSources.put(source.getName(), source);
    }

    protected Source createWebSearchSource() {
        return QsbApplication.get(getContext()).getGoogleSource();
    }

    protected SearchableSource createSearchableSource(SearchableInfo searchable) {
        if (searchable == null) return null;
        try {
            return new SearchableSource(mContext, searchable, getUiThreadHandler(),
                    getIconLoaderExecutor());
        } catch (NameNotFoundException ex) {
            Log.e(TAG, "Source not found: " + ex);
            return null;
        }
    }

    public Source createSourceFor(ComponentName component) {
        SearchableInfo info = mSearchManager.getSearchableInfo(component);
        SearchableSource source = createSearchableSource(info);
        if (DBG) Log.d(TAG, "SearchableSource for " + component + ": " + source);
        return source;
    }
}
