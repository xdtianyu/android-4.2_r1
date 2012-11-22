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


import com.android.quicksearchbox.util.Util;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.webkit.URLUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * The web search source.
 */
public class WebCorpus extends MultiSourceCorpus {
    private static final String TAG = "QSB.WebCorpus";
    private static final boolean DBG = false;

    private static final String WEB_CORPUS_NAME = "web";

    private final SearchSettings mSettings;

    private Source mWebSearchSource;

    private final Source mBrowserSource;

    public WebCorpus(Context context, Config config, SearchSettings settings, Executor executor,
            Source webSearchSource, Source browserSource) {
        super(context, config, executor, webSearchSource, browserSource);
        if (DBG) {
            Log.d(TAG, "init webSource=" + webSearchSource + "; browser source = " + browserSource);
        }
        mSettings = settings;
        mWebSearchSource = webSearchSource;
        mBrowserSource = browserSource;
    }

    protected SearchSettings getSettings() {
        return mSettings;
    }

    public void setWebSource(Source web) {
        if (DBG) Log.d(TAG, "setWebSource(" + web + ")");
        mWebSearchSource = web;
    }

    public CharSequence getLabel() {
        return getContext().getText(R.string.corpus_label_web);
    }

    public CharSequence getHint() {
        // The web corpus uses a drawable hint instead
        return null;
    }

    private boolean isUrl(String query) {
       return Patterns.WEB_URL.matcher(query).matches();
    }

    public Intent createSearchIntent(String query, Bundle appData) {
        if (isUrl(query)) {
            return createBrowseIntent(query);
        } else if (mWebSearchSource != null){
            return mWebSearchSource.createSearchIntent(query, appData);
        } else {
            return null;
        }
    }

    private Intent createBrowseIntent(String query) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.addCategory(Intent.CATEGORY_BROWSABLE);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        String url = URLUtil.guessUrl(query);
        intent.setData(Uri.parse(url));
        return intent;
    }

    public Intent createVoiceSearchIntent(Bundle appData) {
        // TODO in 2-pane mode, mWebSearchSource may be NULL
        // this functionality should be moved elsewhere.
        if (mWebSearchSource != null){
            return mWebSearchSource.createVoiceSearchIntent(appData);
        } else {
            return null;
        }
    }

    private int getCorpusIconResource() {
        return R.drawable.corpus_icon_web;
    }

    public Drawable getCorpusIcon() {
        return getContext().getResources().getDrawable(getCorpusIconResource());
    }

    public Uri getCorpusIconUri() {
        return Util.getResourceUri(getContext(), getCorpusIconResource());
    }

    public String getName() {
        return WEB_CORPUS_NAME;
    }

    @Override
    public int getQueryThreshold() {
        return 0;
    }

    @Override
    public boolean queryAfterZeroResults() {
        return true;
    }

    @Override
    public boolean voiceSearchEnabled() {
        return true;
    }

    public boolean isWebCorpus() {
        return true;
    }

    public CharSequence getSettingsDescription() {
        return getContext().getText(R.string.corpus_description_web);
    }

    @Override
    protected List<Source> getSourcesToQuery(String query, boolean onlyCorpus) {
        ArrayList<Source> sourcesToQuery = new ArrayList<Source>(2);
        if (mWebSearchSource != null) sourcesToQuery.add(mWebSearchSource);
        if (mBrowserSource != null && query.length() > 0) {
            sourcesToQuery.add(mBrowserSource);
        }
        if (DBG) Log.d(TAG, "getSourcesToQuery sourcesToQuery=" + sourcesToQuery);
        return sourcesToQuery;
    }

    @Override
    protected Result createResult(String query, ArrayList<SourceResult> results, int latency) {
        return new WebResult(query, results, latency);
    }

    protected class WebResult extends Result {

        public WebResult(String query, ArrayList<SourceResult> results, int latency) {
            super(query, results, latency);
        }

        @Override
        public void fill() {
            SourceResult webSearchResult = null;
            SourceResult browserResult = null;
            for (SourceResult result : getResults()) {
                if (result.getSource().equals(mWebSearchSource)) {
                    webSearchResult = result;
                } else {
                    browserResult = result;
                }
            }
            if (browserResult != null && browserResult.getCount() > 0) {
                add(new SuggestionPosition(browserResult, 0));
            }
            if (webSearchResult != null) {
                int count = webSearchResult.getCount();
                for (int i = 0; i < count; i++) {
                    add(new SuggestionPosition(webSearchResult, i));
                }
            }
        }

    }
}
