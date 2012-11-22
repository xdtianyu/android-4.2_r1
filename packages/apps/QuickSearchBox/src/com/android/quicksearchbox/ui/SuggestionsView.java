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

import com.android.quicksearchbox.R;
import com.android.quicksearchbox.SuggestionPosition;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ListAdapter;
import android.widget.ListView;

/**
 * Holds a list of suggestions.
 */
public class SuggestionsView extends ListView implements SuggestionsListView<ListAdapter> {

    private static final boolean DBG = false;
    private static final String TAG = "QSB.SuggestionsView";

    private boolean mLimitSuggestionsToViewHeight;
    private SuggestionsAdapter<ListAdapter> mSuggestionsAdapter;

    public SuggestionsView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setSuggestionsAdapter(SuggestionsAdapter<ListAdapter> adapter) {
        super.setAdapter(adapter == null ? null : adapter.getListAdapter());
        mSuggestionsAdapter = adapter;
        if (mLimitSuggestionsToViewHeight) {
            setMaxPromotedByHeight();
        }
    }

    public SuggestionsAdapter<ListAdapter> getSuggestionsAdapter() {
        return mSuggestionsAdapter;
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();
        setItemsCanFocus(true);
    }

    /**
     * Gets the position of the selected suggestion.
     *
     * @return A 0-based index, or {@code -1} if no suggestion is selected.
     */
    public int getSelectedPosition() {
        return getSelectedItemPosition();
    }

    /**
     * Gets the selected suggestion.
     *
     * @return {@code null} if no suggestion is selected.
     */
    public SuggestionPosition getSelectedSuggestion() {
        return (SuggestionPosition) getSelectedItem();
    }

    public void setLimitSuggestionsToViewHeight(boolean limit) {
        mLimitSuggestionsToViewHeight = limit;
        if (mLimitSuggestionsToViewHeight) {
            setMaxPromotedByHeight();
        }
    }

    @Override
    protected void onSizeChanged (int w, int h, int oldw, int oldh) {
        if (mLimitSuggestionsToViewHeight) {
            setMaxPromotedByHeight();
        }
    }

    private void setMaxPromotedByHeight() {
        if (mSuggestionsAdapter != null) {
            float maxHeight;
            if (getParent() instanceof FrameLayout) {
                // We put the SuggestionView inside a frame layout so that we know what its
                // maximum height is. Since this views height is set to 'wrap content' (in two-pane
                // mode at least), we can't use our own height for these calculations.
                maxHeight = ((View) getParent()).getHeight();
                if (DBG) Log.d(TAG, "Parent height=" + maxHeight);
            } else {
                maxHeight = getHeight();
                if (DBG) Log.d(TAG, "This height=" + maxHeight);
            }
            float suggestionHeight =
                getContext().getResources().getDimension(R.dimen.suggestion_view_height);
            if (suggestionHeight != 0) {
                int suggestions = Math.max(1, (int) Math.floor(maxHeight / suggestionHeight));
                if (DBG) {
                    Log.d(TAG, "view height=" + maxHeight + " suggestion height=" +
                            suggestionHeight + " -> maxSuggestions=" + suggestions);
                }
                mSuggestionsAdapter.setMaxPromoted(suggestions);
            }
        }
    }

}
