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

import android.database.DataSetObservable;
import android.database.DataSetObserver;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;


/**
 * Mock implementation of {@link Corpora}.
 */
public class MockCorpora implements Corpora {

    private static final String TAG = "QSB.MockCorpora";

    private final DataSetObservable mDataSetObservable = new DataSetObservable();

    private HashMap<String,Corpus> mCorporaByName = new HashMap<String,Corpus>();
    private HashSet<Corpus> mDefaultCorpora = new HashSet<Corpus>();

    private Corpus mWebCorpus;

    public void addCorpus(Corpus corpus) {
        Corpus oldCorpus = mCorporaByName.put(corpus.getName(), corpus);
        if (oldCorpus != null) {
            Log.d(TAG, "Replaced " + oldCorpus + " with " + corpus);
        }
        notifyDataSetChanged();
    }

    public void setWebCorpus(Corpus webCorpus) {
        mWebCorpus = webCorpus;
    }

    public void addDefaultCorpus(Corpus corpus) {
        mDefaultCorpora.add(corpus);
    }

    public List<Corpus> getAllCorpora() {
        return Collections.unmodifiableList(new ArrayList<Corpus>(mCorporaByName.values()));
    }

    public Corpus getCorpus(String name) {
        return mCorporaByName.get(name);
    }

    public Corpus getWebCorpus() {
        return mWebCorpus;
    }

    public Corpus getCorpusForSource(Source source) {
        for (Corpus corpus : mCorporaByName.values()) {
            for (Source corpusSource : corpus.getSources()) {
                if (corpusSource.equals(source)) {
                    return corpus;
                }
            }
        }
        return null;
    }

    public List<Corpus> getEnabledCorpora() {
        return getAllCorpora();
    }

    public List<Corpus> getCorporaInAll() {
        return getAllCorpora();
    }

    public Source getSource(String name) {
        for (Corpus corpus : mCorporaByName.values()) {
            for (Source source : corpus.getSources()) {
                if (source.getName().equals(name)) {
                    return source;
                }
            }
        }
        return null;
    }

    public boolean isCorpusDefaultEnabled(Corpus corpus) {
        return mDefaultCorpora.contains(corpus);
    }

    public boolean isCorpusEnabled(Corpus corpus) {
        return true;
    }

    public void update() {
    }

    public void registerDataSetObserver(DataSetObserver observer) {
        mDataSetObservable.registerObserver(observer);
    }

    public void unregisterDataSetObserver(DataSetObserver observer) {
        mDataSetObservable.unregisterObserver(observer);
    }

    protected void notifyDataSetChanged() {
        mDataSetObservable.notifyChanged();
    }

}
