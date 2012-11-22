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

import com.android.quicksearchbox.CorpusResult;
import com.android.quicksearchbox.ListSuggestionCursor;
import com.android.quicksearchbox.R;
import com.android.quicksearchbox.Suggestion;
import com.android.quicksearchbox.SuggestionCursor;
import com.android.quicksearchbox.SuggestionPosition;
import com.android.quicksearchbox.SuggestionUtils;
import com.android.quicksearchbox.Suggestions;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListAdapter;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Adapter for suggestions list where suggestions are clustered by corpus.
 */
public class ClusteredSuggestionsAdapter extends SuggestionsAdapterBase<ExpandableListAdapter> {

    private static final String TAG = "QSB.ClusteredSuggestionsAdapter";

    private final static int GROUP_SHIFT = 32;
    private final static long CHILD_MASK = 0xffffffff;

    private final Adapter mAdapter;
    private final Context mContext;
    private final LayoutInflater mInflater;

    public ClusteredSuggestionsAdapter(SuggestionViewFactory viewFactory, Context context) {
        super(viewFactory);
        mAdapter = new Adapter();
        mContext = context;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public boolean isEmpty() {
        return mAdapter.getGroupCount() == 0;
    }

    @Override
    public boolean willPublishNonPromotedSuggestions() {
        return true;
    }

    @Override
    public SuggestionPosition getSuggestion(long suggestionId) {
        return mAdapter.getChildById(suggestionId);
    }

    @Override
    public ExpandableListAdapter getListAdapter() {
        return mAdapter;
    }

    @Override
    protected void notifyDataSetChanged() {
        mAdapter.buildCorpusGroups();
        mAdapter.notifyDataSetChanged();
    }

    @Override
    protected void notifyDataSetInvalidated() {
        mAdapter.buildCorpusGroups();
        mAdapter.notifyDataSetInvalidated();
    }

    private class Adapter extends BaseExpandableListAdapter {

        private ArrayList<SuggestionCursor> mCorpusGroups;

        public void buildCorpusGroups() {
            Suggestions suggestions = getSuggestions();
            SuggestionCursor promoted = getCurrentPromotedSuggestions();
            HashSet<String> promotedSuggestions = new HashSet<String>();
            if (promoted != null && promoted.getCount() > 0) {
                promoted.moveTo(0);
                do {
                    promotedSuggestions.add(SuggestionUtils.getSuggestionKey(promoted));
                } while (promoted.moveToNext());
            }
            if (suggestions == null) {
                mCorpusGroups = null;
            } else {
                if (mCorpusGroups == null) {
                    mCorpusGroups = new ArrayList<SuggestionCursor>();
                } else {
                    mCorpusGroups.clear();
                }
                for (CorpusResult result : suggestions.getCorpusResults()) {
                    ListSuggestionCursor corpusSuggestions = new ListSuggestionCursor(
                            result.getUserQuery());
                    for (int i = 0; i < result.getCount(); ++i) {
                        result.moveTo(i);
                        if (!result.isWebSearchSuggestion()) {
                            if (!promotedSuggestions.contains(
                                    SuggestionUtils.getSuggestionKey(result))) {
                                corpusSuggestions.add(new SuggestionPosition(result, i));
                            }
                        }
                    }
                    if (corpusSuggestions.getCount() > 0) {
                        mCorpusGroups.add(corpusSuggestions);
                    }
                }
            }
        }

        @Override
        public long getCombinedChildId(long groupId, long childId) {
            // add one to the child ID to ensure that the group elements do not have the same ID
            // as the first child within the group.
            return (groupId << GROUP_SHIFT) | ((childId + 1) & CHILD_MASK);
        }

        @Override
        public long getCombinedGroupId(long groupId) {
            return groupId << GROUP_SHIFT;
        }

        public int getChildPosition(long childId) {
            return (int) (childId & CHILD_MASK) - 1;
        }

        public int getGroupPosition(long childId) {
            return (int) ((childId >> GROUP_SHIFT) & CHILD_MASK);
        }

        @Override
        public Suggestion getChild(int groupPosition, int childPosition) {
            SuggestionCursor c = getGroup(groupPosition);
            if (c != null) {
                c.moveTo(childPosition);
                return new SuggestionPosition(c, childPosition);
            }
            return null;
        }

        public SuggestionPosition getChildById(long childId) {
            SuggestionCursor groupCursor = getGroup(getGroupPosition(childId));
            if (groupCursor != null) {
                return new SuggestionPosition(groupCursor, getChildPosition(childId));
            } else {
                Log.w(TAG, "Invalid childId " + Long.toHexString(childId) + " (invalid group)");
                return null;
            }
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            return childPosition;
        }

        @Override
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
                View convertView, ViewGroup parent) {
            SuggestionCursor cursor = getGroup(groupPosition);
            if (cursor == null) return null;
            return getView(cursor, childPosition, getCombinedChildId(groupPosition, childPosition),
                    convertView, parent);
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            SuggestionCursor group = getGroup(groupPosition);
            return group == null ? 0 : group.getCount();
        }

        @Override
        public SuggestionCursor getGroup(int groupPosition) {
            if (groupPosition < promotedGroupCount()) {
                return getCurrentPromotedSuggestions();
            } else {
                int pos = groupPosition - promotedGroupCount();
                if ((pos < 0 ) || (pos >= mCorpusGroups.size())) return null;
                return mCorpusGroups.get(pos);
            }
        }

        private int promotedCount() {
            SuggestionCursor promoted = getCurrentPromotedSuggestions();
            return (promoted == null ? 0 : promoted.getCount());
        }

        private int promotedGroupCount() {
            return (promotedCount() == 0) ? 0 : 1;
        }

        private int corpusGroupCount() {
            return mCorpusGroups == null ? 0 : mCorpusGroups.size();
        }

        @Override
        public int getGroupCount() {
            return promotedGroupCount() + corpusGroupCount();
        }

        @Override
        public long getGroupId(int groupPosition) {
            return groupPosition;
        }

        @Override
        public View getGroupView(
                int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.suggestion_group, parent, false);
            }
            if (groupPosition == 0) {
                // don't show the group separator for the first group, to avoid seeing an empty
                // gap at the top of the list.
                convertView.getLayoutParams().height = 0;
            } else {
                convertView.getLayoutParams().height = mContext.getResources().
                        getDimensionPixelSize(R.dimen.suggestion_group_spacing);
            }
            // since we've fiddled with the layout params:
            convertView.requestLayout();
            return convertView;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }

        @Override
        public int getChildType(int groupPosition, int childPosition) {
            return getSuggestionViewType(getGroup(groupPosition), childPosition);
        }

        @Override
        public int getChildTypeCount() {
            return getSuggestionViewTypeCount();
        }
    }

}
