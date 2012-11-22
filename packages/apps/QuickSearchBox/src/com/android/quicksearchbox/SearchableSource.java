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

import com.android.quicksearchbox.util.NamedTaskExecutor;
import com.android.quicksearchbox.util.Util;

import android.app.PendingIntent;
import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.PathPermission;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.util.Log;

import java.util.Arrays;

/**
 * Represents a single suggestion source, e.g. Contacts.
 */
public class SearchableSource extends AbstractSource {

    private static final boolean DBG = false;
    private static final String TAG = "QSB.SearchableSource";

    // TODO: This should be exposed or moved to android-common, see http://b/issue?id=2440614
    // The extra key used in an intent to the speech recognizer for in-app voice search.
    private static final String EXTRA_CALLING_PACKAGE = "calling_package";

    private final SearchableInfo mSearchable;

    private final String mName;

    private final ActivityInfo mActivityInfo;

    private final int mVersionCode;

    // Cached label for the activity
    private CharSequence mLabel = null;

    // Cached icon for the activity
    private Drawable.ConstantState mSourceIcon = null;

    private Uri mSuggestUriBase;

    public SearchableSource(Context context, SearchableInfo searchable, Handler uiThread,
            NamedTaskExecutor iconLoader) throws NameNotFoundException {
        super(context, uiThread, iconLoader);
        ComponentName componentName = searchable.getSearchActivity();
        if (DBG) Log.d(TAG, "created Searchable for " + componentName);
        mSearchable = searchable;
        mName = componentName.flattenToShortString();
        PackageManager pm = context.getPackageManager();
        mActivityInfo = pm.getActivityInfo(componentName, 0);
        PackageInfo pkgInfo = pm.getPackageInfo(componentName.getPackageName(), 0);
        mVersionCode = pkgInfo.versionCode;
    }

    public SearchableInfo getSearchableInfo() {
        return mSearchable;
    }

    /**
     * Checks if the current process can read the suggestion provider in this source.
     */
    public boolean canRead() {
        String authority = mSearchable.getSuggestAuthority();
        if (authority == null) {
            // TODO: maybe we should have a way to distinguish between having suggestions
            // and being readable.
            return true;
        }

        Uri.Builder uriBuilder = new Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority(authority);
        // if content path provided, insert it now
        String contentPath = mSearchable.getSuggestPath();
        if (contentPath != null) {
            uriBuilder.appendEncodedPath(contentPath);
        }
        // append standard suggestion query path
        uriBuilder.appendEncodedPath(SearchManager.SUGGEST_URI_PATH_QUERY);
        Uri uri = uriBuilder.build();
        return canRead(uri);
    }

    /**
     * Checks if the current process can read the given content URI.
     *
     * TODO: Shouldn't this be a PackageManager / Context / ContentResolver method?
     */
    private boolean canRead(Uri uri) {
        ProviderInfo provider = getContext().getPackageManager().resolveContentProvider(
                uri.getAuthority(), 0);
        if (provider == null) {
            Log.w(TAG, getName() + " has bad suggestion authority " + uri.getAuthority());
            return false;
        }
        String readPermission = provider.readPermission;
        if (readPermission == null) {
            // No permission required to read anything in the content provider
            return true;
        }
        int pid = android.os.Process.myPid();
        int uid = android.os.Process.myUid();
        if (getContext().checkPermission(readPermission, pid, uid)
                == PackageManager.PERMISSION_GRANTED) {
            // We have permission to read everything in the content provider
            return true;
        }
        PathPermission[] pathPermissions = provider.pathPermissions;
        if (pathPermissions == null || pathPermissions.length == 0) {
            // We don't have the readPermission, and there are no pathPermissions
            if (DBG) Log.d(TAG, "Missing " + readPermission);
            return false;
        }
        String path = uri.getPath();
        for (PathPermission perm : pathPermissions) {
            String pathReadPermission = perm.getReadPermission();
            if (pathReadPermission != null
                    && perm.match(path)
                    && getContext().checkPermission(pathReadPermission, pid, uid)
                            == PackageManager.PERMISSION_GRANTED) {
                // We have the path permission
                return true;
            }
        }
        if (DBG) Log.d(TAG, "Missing " + readPermission + " and no path permission applies");
        return false;
    }

    public ComponentName getIntentComponent() {
        return mSearchable.getSearchActivity();
    }

    public int getVersionCode() {
        return mVersionCode;
    }

    public String getName() {
        return mName;
    }

    @Override
    protected String getIconPackage() {
        // Get icons from the package containing the suggestion provider, if any
        String iconPackage = mSearchable.getSuggestPackage();
        if (iconPackage != null) {
            return iconPackage;
        } else {
            // Fall back to the package containing the searchable activity
            return mSearchable.getSearchActivity().getPackageName();
        }
    }

    public CharSequence getLabel() {
        if (mLabel == null) {
            // Load label lazily
            mLabel = mActivityInfo.loadLabel(getContext().getPackageManager());
        }
        return mLabel;
    }

    public CharSequence getHint() {
        return getText(mSearchable.getHintId());
    }

    public int getQueryThreshold() {
        return mSearchable.getSuggestThreshold();
    }

    public CharSequence getSettingsDescription() {
        return getText(mSearchable.getSettingsDescriptionId());
    }

    public Drawable getSourceIcon() {
        if (mSourceIcon == null) {
            Drawable icon = loadSourceIcon();
            if (icon == null) {
                icon = getContext().getResources().getDrawable(R.drawable.corpus_icon_default);
            }
            // Can't share Drawable instances, save constant state instead.
            mSourceIcon = (icon != null) ? icon.getConstantState() : null;
            // Optimization, return the Drawable the first time
            return icon;
        }
        return (mSourceIcon != null) ? mSourceIcon.newDrawable() : null;
    }

    private Drawable loadSourceIcon() {
        int iconRes = getSourceIconResource();
        if (iconRes == 0) return null;
        PackageManager pm = getContext().getPackageManager();
        return pm.getDrawable(mActivityInfo.packageName, iconRes,
                mActivityInfo.applicationInfo);
    }

    public Uri getSourceIconUri() {
        int resourceId = getSourceIconResource();
        if (resourceId == 0) {
            return Util.getResourceUri(getContext(), R.drawable.corpus_icon_default);
        } else {
            return Util.getResourceUri(getContext(), mActivityInfo.applicationInfo, resourceId);
        }
    }

    private int getSourceIconResource() {
        return mActivityInfo.getIconResource();
    }

    public boolean voiceSearchEnabled() {
        return mSearchable.getVoiceSearchEnabled();
    }

    public Intent createVoiceSearchIntent(Bundle appData) {
        if (mSearchable.getVoiceSearchLaunchWebSearch()) {
            return createVoiceWebSearchIntent(appData);
        } else if (mSearchable.getVoiceSearchLaunchRecognizer()) {
            return createVoiceAppSearchIntent(appData);
        }
        return null;
    }

    /**
     * Create and return an Intent that can launch the voice search activity, perform a specific
     * voice transcription, and forward the results to the searchable activity.
     *
     * This code is copied from SearchDialog
     *
     * @return A completely-configured intent ready to send to the voice search activity
     */
    private Intent createVoiceAppSearchIntent(Bundle appData) {
        ComponentName searchActivity = mSearchable.getSearchActivity();

        // create the necessary intent to set up a search-and-forward operation
        // in the voice search system.   We have to keep the bundle separate,
        // because it becomes immutable once it enters the PendingIntent
        Intent queryIntent = new Intent(Intent.ACTION_SEARCH);
        queryIntent.setComponent(searchActivity);
        PendingIntent pending = PendingIntent.getActivity(
                getContext(), 0, queryIntent, PendingIntent.FLAG_ONE_SHOT);

        // Now set up the bundle that will be inserted into the pending intent
        // when it's time to do the search.  We always build it here (even if empty)
        // because the voice search activity will always need to insert "QUERY" into
        // it anyway.
        Bundle queryExtras = new Bundle();
        if (appData != null) {
            queryExtras.putBundle(SearchManager.APP_DATA, appData);
        }

        // Now build the intent to launch the voice search.  Add all necessary
        // extras to launch the voice recognizer, and then all the necessary extras
        // to forward the results to the searchable activity
        Intent voiceIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        voiceIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        // Add all of the configuration options supplied by the searchable's metadata
        String languageModel = getString(mSearchable.getVoiceLanguageModeId());
        if (languageModel == null) {
            languageModel = RecognizerIntent.LANGUAGE_MODEL_FREE_FORM;
        }
        String prompt = getString(mSearchable.getVoicePromptTextId());
        String language = getString(mSearchable.getVoiceLanguageId());
        int maxResults = mSearchable.getVoiceMaxResults();
        if (maxResults <= 0) {
            maxResults = 1;
        }

        voiceIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, languageModel);
        voiceIntent.putExtra(RecognizerIntent.EXTRA_PROMPT, prompt);
        voiceIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, language);
        voiceIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, maxResults);
        voiceIntent.putExtra(EXTRA_CALLING_PACKAGE,
                searchActivity == null ? null : searchActivity.toShortString());

        // Add the values that configure forwarding the results
        voiceIntent.putExtra(RecognizerIntent.EXTRA_RESULTS_PENDINGINTENT, pending);
        voiceIntent.putExtra(RecognizerIntent.EXTRA_RESULTS_PENDINGINTENT_BUNDLE, queryExtras);

        return voiceIntent;
    }

    public SourceResult getSuggestions(String query, int queryLimit, boolean onlySource) {
        try {
            Cursor cursor = getSuggestions(getContext(), mSearchable, query, queryLimit);
            if (DBG) Log.d(TAG, toString() + "[" + query + "] returned.");
            return new CursorBackedSourceResult(this, query, cursor);
        } catch (RuntimeException ex) {
            Log.e(TAG, toString() + "[" + query + "] failed", ex);
            return new CursorBackedSourceResult(this, query);
        }
    }

    public SuggestionCursor refreshShortcut(String shortcutId, String extraData) {
        Cursor cursor = null;
        try {
            cursor = getValidationCursor(getContext(), mSearchable, shortcutId, extraData);
            if (DBG) Log.d(TAG, toString() + "[" + shortcutId + "] returned.");
            if (cursor != null && cursor.getCount() > 0) {
                cursor.moveToFirst();
            }
            return new CursorBackedSourceResult(this, null, cursor);
        } catch (RuntimeException ex) {
            Log.e(TAG, toString() + "[" + shortcutId + "] failed", ex);
            if (cursor != null) {
                cursor.close();
            }
            // TODO: Should we delete the shortcut even if the failure is temporary?
            return null;
        }
    }

    public String getSuggestUri() {
        Uri uri = getSuggestUriBase(mSearchable);
        if (uri == null) return null;
        return uri.toString();
    }

    private synchronized Uri getSuggestUriBase(SearchableInfo searchable) {
        if (searchable == null) {
            return null;
        }
        if (mSuggestUriBase == null) {

            String authority = searchable.getSuggestAuthority();
            if (authority == null) {
                return null;
            }

            Uri.Builder uriBuilder = new Uri.Builder()
                    .scheme(ContentResolver.SCHEME_CONTENT)
                    .authority(authority);

            // if content path provided, insert it now
            final String contentPath = searchable.getSuggestPath();
            if (contentPath != null) {
                uriBuilder.appendEncodedPath(contentPath);
            }

            // append standard suggestion query path
            uriBuilder.appendPath(SearchManager.SUGGEST_URI_PATH_QUERY);
            mSuggestUriBase = uriBuilder.build();
        }
        return mSuggestUriBase;
    }

    /**
     * This is a copy of {@link SearchManager#getSuggestions(SearchableInfo, String)}.
     */
    private Cursor getSuggestions(Context context, SearchableInfo searchable, String query,
            int queryLimit) {

        Uri base = getSuggestUriBase(searchable);
        if (base == null) return null;
        Uri.Builder uriBuilder = base.buildUpon();

        // get the query selection, may be null
        String selection = searchable.getSuggestSelection();
        // inject query, either as selection args or inline
        String[] selArgs = null;
        if (selection != null) {    // use selection if provided
            selArgs = new String[] { query };
        } else {                    // no selection, use REST pattern
            uriBuilder.appendPath(query);
        }

        uriBuilder.appendQueryParameter("limit", String.valueOf(queryLimit));

        Uri uri = uriBuilder.build();

        // finally, make the query
        if (DBG) {
            Log.d(TAG, "query(" + uri + ",null," + selection + ","
                    + Arrays.toString(selArgs) + ",null)");
        }
        Cursor c = context.getContentResolver().query(uri, null, selection, selArgs, null);
        if (DBG) Log.d(TAG, "Got cursor from " + mName + ": " + c);
        return c;
    }

    private static Cursor getValidationCursor(Context context, SearchableInfo searchable,
            String shortcutId, String extraData) {
        String authority = searchable.getSuggestAuthority();
        if (authority == null) {
            return null;
        }

        Uri.Builder uriBuilder = new Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority(authority);

        // if content path provided, insert it now
        final String contentPath = searchable.getSuggestPath();
        if (contentPath != null) {
            uriBuilder.appendEncodedPath(contentPath);
        }

        // append the shortcut path and id
        uriBuilder.appendPath(SearchManager.SUGGEST_URI_PATH_SHORTCUT);
        uriBuilder.appendPath(shortcutId);

        Uri uri = uriBuilder
                .appendQueryParameter(SearchManager.SUGGEST_COLUMN_INTENT_EXTRA_DATA, extraData)
                .build();

        if (DBG) Log.d(TAG, "Requesting refresh " + uri);
        // finally, make the query
        return context.getContentResolver().query(uri, null, null, null, null);
    }

    public int getMaxShortcuts(Config config) {
        return config.getMaxShortcuts(getName());
    }

    public boolean includeInAll() {
        return true;
    }

    public boolean queryAfterZeroResults() {
        return mSearchable.queryAfterZeroResults();
    }

    public String getDefaultIntentAction() {
        String action = mSearchable.getSuggestIntentAction();
        if (action != null) return action;
        return Intent.ACTION_SEARCH;
    }

    public String getDefaultIntentData() {
        return mSearchable.getSuggestIntentData();
    }

    private CharSequence getText(int id) {
        if (id == 0) return null;
        return getContext().getPackageManager().getText(mActivityInfo.packageName, id,
                mActivityInfo.applicationInfo);
    }

    private String getString(int id) {
        CharSequence text = getText(id);
        return text == null ? null : text.toString();
    }
}
