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

package com.android.quicksearchbox.ui;

import com.android.quicksearchbox.Corpus;
import com.android.quicksearchbox.Promoter;
import com.android.quicksearchbox.R;
import com.android.quicksearchbox.Suggestions;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.database.DataSetObserver;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListAdapter;
import android.widget.ImageView;
import android.widget.PopupMenu;

/**
 * Two-pane variant for the search activity view.
 */
public class SearchActivityViewTwoPane extends SearchActivityView {

    private static final int ENTRY_ANIMATION_START_DELAY = 150; // in millis
    private static final int ENTRY_ANIMATION_DURATION = 150; // in millis
    private static final float ANIMATION_STARTING_WIDTH_FACTOR = 0.5f;
    private static final String TOOLBAR_ICON_METADATA_NAME = "com.android.launcher.toolbar_icon";

    private ImageView mMenuButton;

    // View that shows the results other than the query completions
    private ClusteredSuggestionsView mResultsView;
    private SuggestionsAdapter<ExpandableListAdapter> mResultsAdapter;
    private View mResultsHeader;
    private View mSearchPlate;
    private boolean mJustCreated;

    public SearchActivityViewTwoPane(Context context) {
        super(context);
    }

    public SearchActivityViewTwoPane(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SearchActivityViewTwoPane(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mMenuButton = (ImageView) findViewById(R.id.menu_button);
        mMenuButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showPopupMenu();
            }
        });

        mResultsView = (ClusteredSuggestionsView) findViewById(R.id.shortcuts);
        mResultsAdapter = createClusteredSuggestionsAdapter();
        mResultsAdapter.getListAdapter().registerDataSetObserver(new DataSetObserver(){
            @Override
            public void onChanged() {
                mResultsView.expandAll();
            }
        });
        mResultsView.setOnKeyListener(new SuggestionsViewKeyListener());
        mResultsView.setFocusable(true);
        mResultsHeader = findViewById(R.id.shortcut_title);
        mSearchPlate = findViewById(R.id.left_pane);
        mJustCreated = true;

        View dismissBg = findViewById(R.id.dismiss_bg);
        dismissBg.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (isQueryEmpty() && mExitClickListener != null) {
                    mExitClickListener.onClick(v);
                }
            }
        });
    }

    private void showPopupMenu() {
        PopupMenu popup = new PopupMenu(getContext(), mMenuButton);
        Menu menu = popup.getMenu();
        getActivity().createMenuItems(menu, false);
        popup.show();
    }

    protected SuggestionsAdapter<ExpandableListAdapter> createClusteredSuggestionsAdapter() {
        return new DelayingSuggestionsAdapter<ExpandableListAdapter>(
                new ClusteredSuggestionsAdapter(
                        getQsbApplication().getSuggestionViewFactory(),
                        getContext()));
    }

    @Override
    public void onResume() {
        if (mJustCreated) {
            setupEntryAnimations();
            mJustCreated = false;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().overridePendingTransition(R.anim.fade_in_fast, R.anim.fade_out_fast);
    }

    private void setupEntryAnimations() {
        // TODO: Use the left/top of the source bounds to start the animation from
        final int endingWidth = getResources().getDimensionPixelSize(R.dimen.suggestions_width);
        final int startingWidth = (int) (endingWidth * ANIMATION_STARTING_WIDTH_FACTOR);

        ViewGroup.LayoutParams params = mSearchPlate.getLayoutParams();
        params.width = startingWidth;
        mSearchPlate.setLayoutParams(params);

        Animator animator = ObjectAnimator.ofInt(mSearchPlate, "alpha", 0, 255);
        animator.setDuration(ENTRY_ANIMATION_DURATION);
        ((ValueAnimator)animator).addUpdateListener(new AnimatorUpdateListener() {

            public void onAnimationUpdate(ValueAnimator animator) {
                ViewGroup.LayoutParams params = mSearchPlate.getLayoutParams();
                params.width = startingWidth
                        + (int) ((Integer) animator.getAnimatedValue() / 255f
                                * (endingWidth - startingWidth));
                mSearchPlate.setLayoutParams(params);
            }
        });
        animator.setStartDelay(ENTRY_ANIMATION_START_DELAY);
        animator.start();

    }

    @Override
    public void onStop() {
    }

    @Override
    public void start() {
        super.start();
        mResultsAdapter.getListAdapter().registerDataSetObserver(new ResultsObserver());
        mResultsView.setSuggestionsAdapter(mResultsAdapter);
    }

    @Override
    public void destroy() {
        mResultsView.setSuggestionsAdapter(null);

        super.destroy();
    }

    @Override
    protected Drawable getVoiceSearchIcon() {
        ComponentName voiceSearch = getVoiceSearch().getComponent();
        if (voiceSearch != null) {
            // this code copied from Launcher to get the same icon that's displayed on home screen
            try {
                PackageManager packageManager = getContext().getPackageManager();
                // Look for the toolbar icon specified in the activity meta-data
                Bundle metaData = packageManager.getActivityInfo(
                        voiceSearch, PackageManager.GET_META_DATA).metaData;
                if (metaData != null) {
                    int iconResId = metaData.getInt(TOOLBAR_ICON_METADATA_NAME);
                    if (iconResId != 0) {
                        Resources res = packageManager.getResourcesForActivity(voiceSearch);
                        if (DBG) Log.d(TAG, "Got toolbar icon from Voice Search");
                        return res.getDrawable(iconResId);
                    }
                }
            } catch (NameNotFoundException e) {
                // Do nothing
            }
        }
        if (DBG) Log.d(TAG, "Failed to get toolbar icon from Voice Search; using default.");
        return super.getVoiceSearchIcon();
    }

    @Override
    public void considerHidingInputMethod() {
        // Don't hide keyboard when interacting with suggestions list
    }

    @Override
    public void hideSuggestions() {
        // Never hiding suggestions view in two-pane UI
    }

    @Override
    public void showSuggestions() {
        // Never hiding suggestions view in two-pane UI
    }

    @Override
    public void showCorpusSelectionDialog() {
        // not used
    }

    @Override
    public void clearSuggestions() {
        super.clearSuggestions();
        mResultsAdapter.setSuggestions(null);
    }

    @Override
    public void setMaxPromotedResults(int maxPromoted) {
        mResultsView.setLimitSuggestionsToViewHeight(false);
        mResultsAdapter.setMaxPromoted(maxPromoted);
    }

    @Override
    public void limitResultsToViewHeight() {
        mResultsView.setLimitSuggestionsToViewHeight(true);
    }

    @Override
    public void setSuggestionClickListener(SuggestionClickListener listener) {
        super.setSuggestionClickListener(listener);
        mResultsAdapter.setSuggestionClickListener(listener);
    }

    @Override
    public void setSuggestions(Suggestions suggestions) {
        super.setSuggestions(suggestions);
        suggestions.acquire();
        mResultsAdapter.setSuggestions(suggestions);
    }

    @Override
    protected void setCorpus(Corpus corpus) {
        super.setCorpus(corpus);
        mResultsAdapter.setPromoter(createResultsPromoter());
    }

    @Override
    protected Promoter createSuggestionsPromoter() {
        return getQsbApplication().createWebPromoter();
    }

    protected Promoter createResultsPromoter() {
        Corpus corpus = getCorpus();
        if (corpus == null) {
            return getQsbApplication().createResultsPromoter();
        } else {
            return getQsbApplication().createSingleCorpusResultsPromoter(corpus);
        }
    }

    protected void onResultsChanged() {
        checkHideResultsHeader();
    }

    @Override
    protected void updateQueryTextView(boolean queryEmpty) {
        super.updateQueryTextView(queryEmpty);
        if (mSearchCloseButton == null) return;

        if (queryEmpty) {
            mSearchCloseButton.setImageResource(R.drawable.ic_clear_disabled);
        } else {
            mSearchCloseButton.setImageResource(R.drawable.ic_clear);
        }
    }

    private void checkHideResultsHeader() {
        if (mResultsHeader != null) {
            if (!mResultsAdapter.isEmpty()) {
                if (DBG) Log.d(TAG, "Results non-empty");
                mResultsHeader.setVisibility(VISIBLE);
            } else {
                if (DBG) Log.d(TAG, "Results empty");
                mResultsHeader.setVisibility(INVISIBLE);
            }
        }
    }

    @Override
    public Corpus getSearchCorpus() {
        return getWebCorpus();
    }

    protected class ResultsObserver extends DataSetObserver {
        @Override
        public void onChanged() {
            onResultsChanged();
        }
    }

}
