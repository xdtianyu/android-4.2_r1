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

package com.android.quicksearchbox.ui;

import com.android.quicksearchbox.Corpora;
import com.android.quicksearchbox.Corpus;
import com.android.quicksearchbox.R;

import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Adapter for showing a list of sources in the source selection activity.
 */
public class CorporaAdapter extends BaseAdapter {

    private static final String TAG = "CorporaAdapter";
    private static final boolean DBG = false;

    private final Context mContext;

    private final Corpora mCorpora;

    private final int mCorpusViewRes;

    private final DataSetObserver mCorporaObserver = new CorporaObserver();

    private List<Corpus> mSortedCorpora;

    private String mCurrentCorpusName;

    public CorporaAdapter(Context context, Corpora corpora, int corpusViewRes) {
        mContext = context;
        mCorpora = corpora;
        mCorpusViewRes = corpusViewRes;
        mCorpora.registerDataSetObserver(mCorporaObserver);
        updateCorpora();
    }

    public void setCurrentCorpus(Corpus corpus) {
        mCurrentCorpusName = corpus == null ? null : corpus.getName();
        notifyDataSetChanged();
    }

    private void updateCorpora() {
        List<Corpus> enabledCorpora = mCorpora.getEnabledCorpora();
        ArrayList<Corpus> sorted = new ArrayList<Corpus>(enabledCorpora.size());
        for (Corpus corpus : enabledCorpora) {
            if (!corpus.isCorpusHidden()) {
                sorted.add(corpus);
            }
        }
        Collections.sort(sorted, new CorpusComparator());
        mSortedCorpora = sorted;
        notifyDataSetChanged();
    }

    private static class CorpusComparator implements Comparator<Corpus> {
        public int compare(Corpus corpus1, Corpus corpus2) {
            // Comparing a corpus against itself
            if (corpus1 == corpus2) return 0;
            // Web always comes first
            if (corpus1.isWebCorpus()) return -1;
            if (corpus2.isWebCorpus()) return 1;
            // Alphabetically by name
            return corpus1.getLabel().toString().compareTo(corpus2.getLabel().toString());
        }
    }

    public void close() {
        mCorpora.unregisterDataSetObserver(mCorporaObserver);
    }

    public int getCount() {
        return 1 + (mSortedCorpora == null ? 0 : mSortedCorpora.size());
    }

    public Corpus getItem(int position) {
        if (position == 0) {
            return null;
        } else {
            return mSortedCorpora.get(position - 1);
        }
    }

    public long getItemId(int position) {
        return position;
    }

    /**
     * Gets the position of the given corpus.
     */
    public int getCorpusPosition(Corpus corpus) {
        if (corpus == null) {
            return 0;
        }
        int count = getCount();
        for (int i = 0; i < count; i++) {
            if (corpus.equals(getItem(i))) {
                return i;
            }
        }
        Log.w(TAG, "Corpus not in adapter: " + corpus);
        return 0;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        CorpusView view = (CorpusView) convertView;
        if (view == null) {
            view = createView(parent);
        }
        Corpus corpus = getItem(position);
        if (DBG) Log.d(TAG, "Binding " + position + ", corpus=" + corpus);
        bindView(view, corpus);
        return view;
    }

    protected void bindView(CorpusView view, Corpus corpus) {
        Drawable icon = getCorpusIcon(corpus);
        CharSequence label = getCorpusLabel(corpus);
        boolean isCurrent = isCurrentCorpus(corpus);
        if (DBG) Log.d(TAG, "bind:name=" + corpus + ",label=" + label + ",current=" + isCurrent);
        view.setIcon(icon);
        view.setLabel(label);
        view.setChecked(isCurrent);
    }

    protected Drawable getCorpusIcon(Corpus corpus) {
        if (corpus == null) {
            return mContext.getResources().getDrawable(R.mipmap.search_app_icon);
        } else {
            return corpus.getCorpusIcon();
        }
    }

    protected CharSequence getCorpusLabel(Corpus corpus) {
        if (corpus == null) {
            return mContext.getText(R.string.corpus_label_global);
        } else {
            return corpus.getLabel();
        }
    }

    protected boolean isCurrentCorpus(Corpus corpus) {
        if (corpus == null) {
            return mCurrentCorpusName == null;
        } else {
            return corpus.getName().equals(mCurrentCorpusName);
        }
    }

    protected CorpusView createView(ViewGroup parent) {
        return (CorpusView) LayoutInflater.from(mContext).inflate(mCorpusViewRes, parent, false);
    }

    protected LayoutInflater getInflater() {
        return (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    private class CorporaObserver extends DataSetObserver {
        @Override
        public void onChanged() {
            updateCorpora();
        }

        @Override
        public void onInvalidated() {
            updateCorpora();
        }
    }

}
