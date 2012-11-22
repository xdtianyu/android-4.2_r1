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

import com.android.quicksearchbox.Corpora;
import com.android.quicksearchbox.Corpus;
import com.android.quicksearchbox.CorpusResult;
import com.android.quicksearchbox.Logger;
import com.android.quicksearchbox.Promoter;
import com.android.quicksearchbox.QsbApplication;
import com.android.quicksearchbox.R;
import com.android.quicksearchbox.SearchActivity;
import com.android.quicksearchbox.SuggestionCursor;
import com.android.quicksearchbox.Suggestions;
import com.android.quicksearchbox.VoiceSearch;

import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import java.util.ArrayList;
import java.util.Arrays;

public abstract class SearchActivityView extends RelativeLayout {
    protected static final boolean DBG = false;
    protected static final String TAG = "QSB.SearchActivityView";

    // The string used for privateImeOptions to identify to the IME that it should not show
    // a microphone button since one already exists in the search dialog.
    // TODO: This should move to android-common or something.
    private static final String IME_OPTION_NO_MICROPHONE = "nm";

    private Corpus mCorpus;

    protected QueryTextView mQueryTextView;
    // True if the query was empty on the previous call to updateQuery()
    protected boolean mQueryWasEmpty = true;
    protected Drawable mQueryTextEmptyBg;
    protected Drawable mQueryTextNotEmptyBg;

    protected SuggestionsListView<ListAdapter> mSuggestionsView;
    protected SuggestionsAdapter<ListAdapter> mSuggestionsAdapter;

    protected ImageButton mSearchCloseButton;
    protected ImageButton mSearchGoButton;
    protected ImageButton mVoiceSearchButton;

    protected ButtonsKeyListener mButtonsKeyListener;

    private boolean mUpdateSuggestions;

    private QueryListener mQueryListener;
    private SearchClickListener mSearchClickListener;
    protected View.OnClickListener mExitClickListener;

    public SearchActivityView(Context context) {
        super(context);
    }

    public SearchActivityView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SearchActivityView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onFinishInflate() {
        mQueryTextView = (QueryTextView) findViewById(R.id.search_src_text);

        mSuggestionsView = (SuggestionsView) findViewById(R.id.suggestions);
        mSuggestionsView.setOnScrollListener(new InputMethodCloser());
        mSuggestionsView.setOnKeyListener(new SuggestionsViewKeyListener());
        mSuggestionsView.setOnFocusChangeListener(new SuggestListFocusListener());

        mSuggestionsAdapter = createSuggestionsAdapter();
        // TODO: why do we need focus listeners both on the SuggestionsView and the individual
        // suggestions?
        mSuggestionsAdapter.setOnFocusChangeListener(new SuggestListFocusListener());

        mSearchCloseButton = (ImageButton) findViewById(R.id.search_close_btn);
        mSearchGoButton = (ImageButton) findViewById(R.id.search_go_btn);
        mVoiceSearchButton = (ImageButton) findViewById(R.id.search_voice_btn);
        mVoiceSearchButton.setImageDrawable(getVoiceSearchIcon());

        mQueryTextView.addTextChangedListener(new SearchTextWatcher());
        mQueryTextView.setOnEditorActionListener(new QueryTextEditorActionListener());
        mQueryTextView.setOnFocusChangeListener(new QueryTextViewFocusListener());
        mQueryTextEmptyBg = mQueryTextView.getBackground();

        mSearchGoButton.setOnClickListener(new SearchGoButtonClickListener());

        mButtonsKeyListener = new ButtonsKeyListener();
        mSearchGoButton.setOnKeyListener(mButtonsKeyListener);
        mVoiceSearchButton.setOnKeyListener(mButtonsKeyListener);
        if (mSearchCloseButton != null) {
            mSearchCloseButton.setOnKeyListener(mButtonsKeyListener);
            mSearchCloseButton.setOnClickListener(new CloseClickListener());
        }

        mUpdateSuggestions = true;
    }

    public abstract void onResume();

    public abstract void onStop();

    public void onPause() {
        // Override if necessary
    }

    public void start() {
        mSuggestionsAdapter.getListAdapter().registerDataSetObserver(new SuggestionsObserver());
        mSuggestionsView.setSuggestionsAdapter(mSuggestionsAdapter);
    }

    public void destroy() {
        mSuggestionsView.setSuggestionsAdapter(null);  // closes mSuggestionsAdapter
    }

    // TODO: Get rid of this. To make it more easily testable,
    // the SearchActivityView should not depend on QsbApplication.
    protected QsbApplication getQsbApplication() {
        return QsbApplication.get(getContext());
    }

    protected Drawable getVoiceSearchIcon() {
        return getResources().getDrawable(R.drawable.ic_btn_speak_now);
    }

    protected VoiceSearch getVoiceSearch() {
        return getQsbApplication().getVoiceSearch();
    }

    protected SuggestionsAdapter<ListAdapter> createSuggestionsAdapter() {
        return new DelayingSuggestionsAdapter<ListAdapter>(new SuggestionsListAdapter(
                getQsbApplication().getSuggestionViewFactory()));
    }

    protected Corpora getCorpora() {
        return getQsbApplication().getCorpora();
    }

    public Corpus getCorpus() {
        return mCorpus;
    }

    protected abstract Promoter createSuggestionsPromoter();

    protected Corpus getCorpus(String sourceName) {
        if (sourceName == null) return null;
        Corpus corpus = getCorpora().getCorpus(sourceName);
        if (corpus == null) {
            Log.w(TAG, "Unknown corpus " + sourceName);
            return null;
        }
        return corpus;
    }

    public void onCorpusSelected(String corpusName) {
        setCorpus(corpusName);
        focusQueryTextView();
        showInputMethodForQuery();
    }

    public void setCorpus(String corpusName) {
        if (DBG) Log.d(TAG, "setCorpus(" + corpusName + ")");
        Corpus corpus = getCorpus(corpusName);
        setCorpus(corpus);
        updateUi();
    }

    protected void setCorpus(Corpus corpus) {
        mCorpus = corpus;
        mSuggestionsAdapter.setPromoter(createSuggestionsPromoter());
        Suggestions suggestions = getSuggestions();
        if (corpus == null || suggestions == null || !suggestions.expectsCorpus(corpus)) {
            getActivity().updateSuggestions();
        }
    }

    public String getCorpusName() {
        Corpus corpus = getCorpus();
        return corpus == null ? null : corpus.getName();
    }

    public abstract Corpus getSearchCorpus();

    public Corpus getWebCorpus() {
        Corpus webCorpus = getCorpora().getWebCorpus();
        if (webCorpus == null) {
            Log.e(TAG, "No web corpus");
        }
        return webCorpus;
    }

    public void setMaxPromotedSuggestions(int maxPromoted) {
        mSuggestionsView.setLimitSuggestionsToViewHeight(false);
        mSuggestionsAdapter.setMaxPromoted(maxPromoted);
    }

    public void limitSuggestionsToViewHeight() {
        mSuggestionsView.setLimitSuggestionsToViewHeight(true);
    }

    public void setMaxPromotedResults(int maxPromoted) {
    }

    public void limitResultsToViewHeight() {
    }

    public void setQueryListener(QueryListener listener) {
        mQueryListener = listener;
    }

    public void setSearchClickListener(SearchClickListener listener) {
        mSearchClickListener = listener;
    }

    public abstract void showCorpusSelectionDialog();

    public void setVoiceSearchButtonClickListener(View.OnClickListener listener) {
        if (mVoiceSearchButton != null) {
            mVoiceSearchButton.setOnClickListener(listener);
        }
    }

    public void setSuggestionClickListener(final SuggestionClickListener listener) {
        mSuggestionsAdapter.setSuggestionClickListener(listener);
        mQueryTextView.setCommitCompletionListener(new QueryTextView.CommitCompletionListener() {
            @Override
            public void onCommitCompletion(int position) {
                mSuggestionsAdapter.onSuggestionClicked(position);
            }
        });
    }

    public void setExitClickListener(final View.OnClickListener listener) {
        mExitClickListener = listener;
    }

    public Suggestions getSuggestions() {
        return mSuggestionsAdapter.getSuggestions();
    }

    public SuggestionCursor getCurrentPromotedSuggestions() {
        return mSuggestionsAdapter.getCurrentPromotedSuggestions();
    }

    public void setSuggestions(Suggestions suggestions) {
        suggestions.acquire();
        mSuggestionsAdapter.setSuggestions(suggestions);
    }

    public void clearSuggestions() {
        mSuggestionsAdapter.setSuggestions(null);
    }

    public String getQuery() {
        CharSequence q = mQueryTextView.getText();
        return q == null ? "" : q.toString();
    }

    public boolean isQueryEmpty() {
        return TextUtils.isEmpty(getQuery());
    }

    /**
     * Sets the text in the query box. Does not update the suggestions.
     */
    public void setQuery(String query, boolean selectAll) {
        mUpdateSuggestions = false;
        mQueryTextView.setText(query);
        mQueryTextView.setTextSelection(selectAll);
        mUpdateSuggestions = true;
    }

    protected SearchActivity getActivity() {
        Context context = getContext();
        if (context instanceof SearchActivity) {
            return (SearchActivity) context;
        } else {
            return null;
        }
    }

    public void hideSuggestions() {
        mSuggestionsView.setVisibility(GONE);
    }

    public void showSuggestions() {
        mSuggestionsView.setVisibility(VISIBLE);
    }

    public void focusQueryTextView() {
        mQueryTextView.requestFocus();
    }

    protected void updateUi() {
        updateUi(isQueryEmpty());
    }

    protected void updateUi(boolean queryEmpty) {
        updateQueryTextView(queryEmpty);
        updateSearchGoButton(queryEmpty);
        updateVoiceSearchButton(queryEmpty);
    }

    protected void updateQueryTextView(boolean queryEmpty) {
        if (queryEmpty) {
            if (isSearchCorpusWeb()) {
                mQueryTextView.setBackgroundDrawable(mQueryTextEmptyBg);
                mQueryTextView.setHint(null);
            } else {
                if (mQueryTextNotEmptyBg == null) {
                    mQueryTextNotEmptyBg =
                            getResources().getDrawable(R.drawable.textfield_search_empty);
                }
                mQueryTextView.setBackgroundDrawable(mQueryTextNotEmptyBg);
                Corpus corpus = getCorpus();
                mQueryTextView.setHint(corpus == null ? "" : corpus.getHint());
            }
        } else {
            mQueryTextView.setBackgroundResource(R.drawable.textfield_search);
        }
    }

    private void updateSearchGoButton(boolean queryEmpty) {
        if (queryEmpty) {
            mSearchGoButton.setVisibility(View.GONE);
        } else {
            mSearchGoButton.setVisibility(View.VISIBLE);
        }
    }

    protected void updateVoiceSearchButton(boolean queryEmpty) {
        if (shouldShowVoiceSearch(queryEmpty)
                && getVoiceSearch().shouldShowVoiceSearch(getCorpus())) {
            mVoiceSearchButton.setVisibility(View.VISIBLE);
            mQueryTextView.setPrivateImeOptions(IME_OPTION_NO_MICROPHONE);
        } else {
            mVoiceSearchButton.setVisibility(View.GONE);
            mQueryTextView.setPrivateImeOptions(null);
        }
    }

    protected boolean shouldShowVoiceSearch(boolean queryEmpty) {
        return queryEmpty;
    }

    /**
     * Hides the input method.
     */
    protected void hideInputMethod() {
        InputMethodManager imm = (InputMethodManager)
                getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(getWindowToken(), 0);
        }
    }

    public abstract void considerHidingInputMethod();

    public void showInputMethodForQuery() {
        mQueryTextView.showInputMethod();
    }

    /**
     * Dismiss the activity if BACK is pressed when the search box is empty.
     */
    @Override
    public boolean dispatchKeyEventPreIme(KeyEvent event) {
        SearchActivity activity = getActivity();
        if (activity != null && event.getKeyCode() == KeyEvent.KEYCODE_BACK
                && isQueryEmpty()) {
            KeyEvent.DispatcherState state = getKeyDispatcherState();
            if (state != null) {
                if (event.getAction() == KeyEvent.ACTION_DOWN
                        && event.getRepeatCount() == 0) {
                    state.startTracking(event, this);
                    return true;
                } else if (event.getAction() == KeyEvent.ACTION_UP
                        && !event.isCanceled() && state.isTracking(event)) {
                    hideInputMethod();
                    activity.onBackPressed();
                    return true;
                }
            }
        }
        return super.dispatchKeyEventPreIme(event);
    }

    /**
     * If the input method is in fullscreen mode, and the selector corpus
     * is All or Web, use the web search suggestions as completions.
     */
    protected void updateInputMethodSuggestions() {
        InputMethodManager imm = (InputMethodManager)
                getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm == null || !imm.isFullscreenMode()) return;
        Suggestions suggestions = mSuggestionsAdapter.getSuggestions();
        if (suggestions == null) return;
        CompletionInfo[] completions = webSuggestionsToCompletions(suggestions);
        if (DBG) Log.d(TAG, "displayCompletions(" + Arrays.toString(completions) + ")");
        imm.displayCompletions(mQueryTextView, completions);
    }

    private CompletionInfo[] webSuggestionsToCompletions(Suggestions suggestions) {
        // TODO: This should also include include web search shortcuts
        CorpusResult cursor = suggestions.getWebResult();
        if (cursor == null) return null;
        int count = cursor.getCount();
        ArrayList<CompletionInfo> completions = new ArrayList<CompletionInfo>(count);
        boolean usingWebCorpus = isSearchCorpusWeb();
        for (int i = 0; i < count; i++) {
            cursor.moveTo(i);
            if (!usingWebCorpus || cursor.isWebSearchSuggestion()) {
                String text1 = cursor.getSuggestionText1();
                completions.add(new CompletionInfo(i, i, text1));
            }
        }
        return completions.toArray(new CompletionInfo[completions.size()]);
    }

    protected void onSuggestionsChanged() {
        updateInputMethodSuggestions();
    }

    /**
     * Checks if the corpus used for typed searches is the web corpus.
     */
    protected boolean isSearchCorpusWeb() {
        Corpus corpus = getSearchCorpus();
        return corpus != null && corpus.isWebCorpus();
    }

    protected boolean onSuggestionKeyDown(SuggestionsAdapter<?> adapter,
            long suggestionId, int keyCode, KeyEvent event) {
        // Treat enter or search as a click
        if (       keyCode == KeyEvent.KEYCODE_ENTER
                || keyCode == KeyEvent.KEYCODE_SEARCH
                || keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
            if (adapter != null) {
                adapter.onSuggestionClicked(suggestionId);
                return true;
            } else {
                return false;
            }
        }

        return false;
    }

    protected boolean onSearchClicked(int method) {
        if (mSearchClickListener != null) {
            return mSearchClickListener.onSearchClicked(method);
        }
        return false;
    }

    /**
     * Filters the suggestions list when the search text changes.
     */
    private class SearchTextWatcher implements TextWatcher {
        public void afterTextChanged(Editable s) {
            boolean empty = s.length() == 0;
            if (empty != mQueryWasEmpty) {
                mQueryWasEmpty = empty;
                updateUi(empty);
            }
            if (mUpdateSuggestions) {
                if (mQueryListener != null) {
                    mQueryListener.onQueryChanged();
                }
            }
        }

        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }
    }

    /**
     * Handles key events on the suggestions list view.
     */
    protected class SuggestionsViewKeyListener implements View.OnKeyListener {
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            if (event.getAction() == KeyEvent.ACTION_DOWN
                    && v instanceof SuggestionsListView<?>) {
                SuggestionsListView<?> listView = (SuggestionsListView<?>) v;
                if (onSuggestionKeyDown(listView.getSuggestionsAdapter(), 
                        listView.getSelectedItemId(), keyCode, event)) {
                    return true;
                }
            }
            return forwardKeyToQueryTextView(keyCode, event);
        }
    }

    private class InputMethodCloser implements SuggestionsView.OnScrollListener {

        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
                int totalItemCount) {
        }

        public void onScrollStateChanged(AbsListView view, int scrollState) {
            considerHidingInputMethod();
        }
    }

    /**
     * Listens for clicks on the source selector.
     */
    private class SearchGoButtonClickListener implements View.OnClickListener {
        public void onClick(View view) {
            onSearchClicked(Logger.SEARCH_METHOD_BUTTON);
        }
    }

    /**
     * This class handles enter key presses in the query text view.
     */
    private class QueryTextEditorActionListener implements OnEditorActionListener {
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            boolean consumed = false;
            if (event != null) {
                if (event.getAction() == KeyEvent.ACTION_UP) {
                    consumed = onSearchClicked(Logger.SEARCH_METHOD_KEYBOARD);
                } else if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    // we have to consume the down event so that we receive the up event too
                    consumed = true;
                }
            }
            if (DBG) Log.d(TAG, "onEditorAction consumed=" + consumed);
            return consumed;
        }
    }

    /**
     * Handles key events on the search and voice search buttons,
     * by refocusing to EditText.
     */
    private class ButtonsKeyListener implements View.OnKeyListener {
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            return forwardKeyToQueryTextView(keyCode, event);
        }
    }

    private boolean forwardKeyToQueryTextView(int keyCode, KeyEvent event) {
        if (!event.isSystem() && shouldForwardToQueryTextView(keyCode)) {
            if (DBG) Log.d(TAG, "Forwarding key to query box: " + event);
            if (mQueryTextView.requestFocus()) {
                return mQueryTextView.dispatchKeyEvent(event);
            }
        }
        return false;
    }

    private boolean shouldForwardToQueryTextView(int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_SEARCH:
                return false;
            default:
                return true;
        }
    }

    /**
     * Hides the input method when the suggestions get focus.
     */
    private class SuggestListFocusListener implements OnFocusChangeListener {
        public void onFocusChange(View v, boolean focused) {
            if (DBG) Log.d(TAG, "Suggestions focus change, now: " + focused);
            if (focused) {
                considerHidingInputMethod();
            }
        }
    }

    private class QueryTextViewFocusListener implements OnFocusChangeListener {
        public void onFocusChange(View v, boolean focused) {
            if (DBG) Log.d(TAG, "Query focus change, now: " + focused);
            if (focused) {
                // The query box got focus, show the input method
                showInputMethodForQuery();
            }
        }
    }

    protected class SuggestionsObserver extends DataSetObserver {
        @Override
        public void onChanged() {
            onSuggestionsChanged();
        }
    }

    public interface QueryListener {
        void onQueryChanged();
    }

    public interface SearchClickListener {
        boolean onSearchClicked(int method);
    }

    private class CloseClickListener implements OnClickListener {
        public void onClick(View v) {
            if (!isQueryEmpty()) {
                mQueryTextView.setText("");
            } else {
                mExitClickListener.onClick(v);
            }
        }
    }
}
