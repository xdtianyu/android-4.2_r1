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

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;

import java.util.Collection;
import java.util.Collections;

/**
 * Mock implementation of {@link Corpus}.
 *
 */
public class MockCorpus implements Corpus {

    public static final Corpus CORPUS_1 = new MockCorpus(MockSource.SOURCE_1);

    public static final Corpus CORPUS_2 = new MockCorpus(MockSource.SOURCE_2);

    public static final Corpus WEB_CORPUS = new MockCorpus(MockSource.WEB_SOURCE);

    private final String mName;

    private final Source mSource;

    private final boolean mDefaultEnabled;

    public MockCorpus(Source source) {
        this(source, true);
    }

    public MockCorpus(Source source, boolean defaultEnabled) {
        mName = "corpus_" + source.getName();
        mSource = source;
        mDefaultEnabled = defaultEnabled;
    }

    public Intent createSearchIntent(String query, Bundle appData) {
        return null;
    }

    public Intent createVoiceSearchIntent(Bundle appData) {
        return null;
    }

    public Drawable getCorpusIcon() {
        return null;
    }

    public Uri getCorpusIconUri() {
        return null;
    }

    public CharSequence getLabel() {
        return mName;
    }

    public CharSequence getHint() {
        return null;
    }

    public String getName() {
        return mName;
    }

    public int getQueryThreshold() {
        return 0;
    }

    public Collection<Source> getSources() {
        return Collections.singletonList(mSource);
    }

    public CharSequence getSettingsDescription() {
        return null;
    }

    public CorpusResult getSuggestions(String query, int queryLimit, boolean onlyCorpus) {
        return new Result(query, mSource.getSuggestions(query, queryLimit, true));
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public boolean equals(Object o) {
        if (o != null && o.getClass().equals(this.getClass())) {
            MockCorpus s = (MockCorpus) o;
            return s.mName.equals(mName);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return mName.hashCode();
    }

    private class Result extends SuggestionCursorWrapper implements CorpusResult {
        public Result(String userQuery, SuggestionCursor cursor) {
            super(userQuery, cursor);
        }

        public Corpus getCorpus() {
            return MockCorpus.this;
        }

        public int getLatency() {
            return 0;
        }
    }

    public boolean isWebCorpus() {
        return mSource.getName().equals(MockSource.WEB_SOURCE.getName());
    }

    public boolean queryAfterZeroResults() {
        return false;
    }

    public boolean voiceSearchEnabled() {
        return false;
    }

    public boolean isCorpusDefaultEnabled() {
        return mDefaultEnabled;
    }

    public boolean includeInAll() {
        return true;
    }

    public boolean isCorpusEnabled() {
        return true;
    }

    public boolean isCorpusHidden() {
        return false;
    }

}
