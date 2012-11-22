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
import android.database.DataSetObservable;
import android.database.DataSetObserver;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * Maintains the list of all corpora.
 */
public class SearchableCorpora implements Corpora {

    // set to true to enable the more verbose debug logging for this file
    private static final boolean DBG = false;
    private static final String TAG = "QSB.DefaultCorpora";

    private final DataSetObservable mDataSetObservable = new DataSetObservable();

    private final Context mContext;
    private final SearchSettings mSettings;
    private final CorpusFactory mCorpusFactory;

    private Sources mSources;
    // Maps corpus names to corpora
    private HashMap<String,Corpus> mCorporaByName;
    // Maps sources to the corpus that contains them
    private HashMap<Source,Corpus> mCorporaBySource;
    // Enabled corpora
    private List<Corpus> mEnabledCorpora;
    // Web corpus
    private Corpus mWebCorpus;

    /**
     *
     * @param context Used for looking up source information etc.
     */
    public SearchableCorpora(Context context, SearchSettings settings, Sources sources,
            CorpusFactory corpusFactory) {
        mContext = context;
        mSettings = settings;
        mCorpusFactory = corpusFactory;
        mSources = sources;
    }

    protected Context getContext() {
        return mContext;
    }

    public Collection<Corpus> getAllCorpora() {
        return Collections.unmodifiableCollection(mCorporaByName.values());
    }

    public List<Corpus> getEnabledCorpora() {
        return mEnabledCorpora;
    }

    public List<Corpus> getCorporaInAll() {
        ArrayList<Corpus> corpora = new ArrayList<Corpus>(mEnabledCorpora.size());
        for (Corpus corpus : mEnabledCorpora) {
            if (corpus.includeInAll()) {
                corpora.add(corpus);
            }
        }
        return corpora;
    }

    public Corpus getCorpus(String name) {
        return mCorporaByName.get(name);
    }

    public Corpus getWebCorpus() {
        return mWebCorpus;
    }

    public Corpus getCorpusForSource(Source source) {
        return mCorporaBySource.get(source);
    }

    public Source getSource(String name) {
        if (TextUtils.isEmpty(name)) {
            Log.w(TAG, "Empty source name");
            return null;
        }
        return mSources.getSource(name);
    }

    public void update() {
        mSources.update();

        Collection<Corpus> corpora = mCorpusFactory.createCorpora(mSources);

        mCorporaByName = new HashMap<String,Corpus>(corpora.size());
        mCorporaBySource = new HashMap<Source,Corpus>(corpora.size());
        mEnabledCorpora = new ArrayList<Corpus>(corpora.size());
        mWebCorpus = null;

        for (Corpus corpus : corpora) {
            mCorporaByName.put(corpus.getName(), corpus);
            for (Source source : corpus.getSources()) {
                mCorporaBySource.put(source, corpus);
            }
            if (mSettings.isCorpusEnabled(corpus)) {
                mEnabledCorpora.add(corpus);
            }
            if (corpus.isWebCorpus()) {
                if (mWebCorpus != null) {
                    Log.w(TAG, "Multiple web corpora: " + mWebCorpus + ", " + corpus);
                }
                mWebCorpus = corpus;
            }
        }

        if (DBG) Log.d(TAG, "Updated corpora: " + mCorporaBySource.values());

        mEnabledCorpora = Collections.unmodifiableList(mEnabledCorpora);

        notifyDataSetChanged();
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
