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

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;

import java.util.Collection;
import java.util.Collections;

/**
 * A corpus that uses a single source.
 */
public class SingleSourceCorpus extends AbstractCorpus {

    private final Source mSource;

    public SingleSourceCorpus(Context context, Config config, Source source) {
        super(context, config);
        mSource = source;
    }

    public Drawable getCorpusIcon() {
        return mSource.getSourceIcon();
    }

    public Uri getCorpusIconUri() {
        return mSource.getSourceIconUri();
    }

    public CharSequence getLabel() {
        return mSource.getLabel();
    }

    public CharSequence getHint() {
        return mSource.getHint();
    }

    public CharSequence getSettingsDescription() {
        return mSource.getSettingsDescription();
    }

    public CorpusResult getSuggestions(String query, int queryLimit, boolean onlyCorpus) {
        LatencyTracker latencyTracker = new LatencyTracker();
        SourceResult sourceResult = mSource.getSuggestions(query, queryLimit, true);
        int latency = latencyTracker.getLatency();
        return new SingleSourceCorpusResult(this, query, sourceResult, latency);
    }

    public String getName() {
        return mSource.getName();
    }

    public boolean queryAfterZeroResults() {
        return mSource.queryAfterZeroResults();
    }

    public int getQueryThreshold() {
        return mSource.getQueryThreshold();
    }

    public boolean voiceSearchEnabled() {
        return mSource.voiceSearchEnabled();
    }

    public Intent createSearchIntent(String query, Bundle appData) {
        return mSource.createSearchIntent(query, appData);
    }

    public Intent createVoiceSearchIntent(Bundle appData) {
        return mSource.createVoiceSearchIntent(appData);
    }

    public boolean includeInAll() {
        return mSource.includeInAll();
    }

    public boolean isWebCorpus() {
        return false;
    }

    public Collection<Source> getSources() {
        return Collections.singletonList(mSource);
    }

}
