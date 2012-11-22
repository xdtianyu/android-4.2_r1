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

import com.android.quicksearchbox.util.Consumer;
import com.android.quicksearchbox.util.Consumers;
import com.android.quicksearchbox.util.SQLiteAsyncQuery;
import com.android.quicksearchbox.util.SQLiteTransaction;
import com.android.quicksearchbox.util.Util;
import com.google.common.annotations.VisibleForTesting;

import org.json.JSONException;

import android.app.SearchManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * A shortcut repository implementation that uses a log of every click.
 *
 * To inspect DB:
 * # sqlite3 /data/data/com.android.quicksearchbox/databases/qsb-log.db
 *
 * TODO: Refactor this class.
 */
public class ShortcutRepositoryImplLog implements ShortcutRepository {

    private static final boolean DBG = false;
    private static final String TAG = "QSB.ShortcutRepositoryImplLog";

    private static final String DB_NAME = "qsb-log.db";
    private static final int DB_VERSION = 32;

    private static final String HAS_HISTORY_QUERY =
        "SELECT " + Shortcuts.intent_key.fullName + " FROM " + Shortcuts.TABLE_NAME;
    private String mEmptyQueryShortcutQuery ;
    private String mShortcutQuery;

    private static final String SHORTCUT_BY_ID_WHERE =
            Shortcuts.shortcut_id.name() + "=? AND " + Shortcuts.source.name() + "=?";

    private static final String SOURCE_RANKING_SQL = buildSourceRankingSql();

    private final Context mContext;
    private final Config mConfig;
    private final Corpora mCorpora;
    private final ShortcutRefresher mRefresher;
    private final Handler mUiThread;
    // Used to perform log write operations asynchronously
    private final Executor mLogExecutor;
    private final DbOpenHelper mOpenHelper;
    private final String mSearchSpinner;

    /**
     * Create an instance to the repo.
     */
    public static ShortcutRepository create(Context context, Config config,
            Corpora sources, ShortcutRefresher refresher, Handler uiThread,
            Executor logExecutor) {
        return new ShortcutRepositoryImplLog(context, config, sources, refresher,
                uiThread, logExecutor, DB_NAME);
    }

    /**
     * @param context Used to create / open db
     * @param name The name of the database to create.
     */
    @VisibleForTesting
    ShortcutRepositoryImplLog(Context context, Config config, Corpora corpora,
            ShortcutRefresher refresher, Handler uiThread, Executor logExecutor, String name) {
        mContext = context;
        mConfig = config;
        mCorpora = corpora;
        mRefresher = refresher;
        mUiThread = uiThread;
        mLogExecutor = logExecutor;
        mOpenHelper = new DbOpenHelper(context, name, DB_VERSION, config);
        buildShortcutQueries();

        mSearchSpinner = Util.getResourceUri(mContext, R.drawable.search_spinner).toString();
    }

    // clicklog first, since that's where restrict the result set
    private static final String TABLES = ClickLog.TABLE_NAME + " INNER JOIN " +
            Shortcuts.TABLE_NAME + " ON " + ClickLog.intent_key.fullName + " = " +
            Shortcuts.intent_key.fullName;

    private static final String AS = " AS ";

    private static final String[] SHORTCUT_QUERY_COLUMNS = {
            Shortcuts.intent_key.fullName,
            Shortcuts.source.fullName,
            Shortcuts.source_version_code.fullName,
            Shortcuts.format.fullName + AS + SearchManager.SUGGEST_COLUMN_FORMAT,
            Shortcuts.title + AS + SearchManager.SUGGEST_COLUMN_TEXT_1,
            Shortcuts.description + AS + SearchManager.SUGGEST_COLUMN_TEXT_2,
            Shortcuts.description_url + AS + SearchManager.SUGGEST_COLUMN_TEXT_2_URL,
            Shortcuts.icon1 + AS + SearchManager.SUGGEST_COLUMN_ICON_1,
            Shortcuts.icon2 + AS + SearchManager.SUGGEST_COLUMN_ICON_2,
            Shortcuts.intent_action + AS + SearchManager.SUGGEST_COLUMN_INTENT_ACTION,
            Shortcuts.intent_component.fullName,
            Shortcuts.intent_data + AS + SearchManager.SUGGEST_COLUMN_INTENT_DATA,
            Shortcuts.intent_query + AS + SearchManager.SUGGEST_COLUMN_QUERY,
            Shortcuts.intent_extradata + AS + SearchManager.SUGGEST_COLUMN_INTENT_EXTRA_DATA,
            Shortcuts.shortcut_id + AS + SearchManager.SUGGEST_COLUMN_SHORTCUT_ID,
            Shortcuts.spinner_while_refreshing + AS +
                    SearchManager.SUGGEST_COLUMN_SPINNER_WHILE_REFRESHING,
            Shortcuts.log_type + AS + CursorBackedSuggestionCursor.SUGGEST_COLUMN_LOG_TYPE,
            Shortcuts.custom_columns.fullName,
        };

    // Avoid GLOB by using >= AND <, with some manipulation (see nextString(String)).
    // to figure out the upper bound (e.g. >= "abc" AND < "abd"
    // This allows us to use parameter binding and still take advantage of the
    // index on the query column.
    private static final String PREFIX_RESTRICTION =
            ClickLog.query.fullName + " >= ?1 AND " + ClickLog.query.fullName + " < ?2";

    private static final String LAST_HIT_TIME_EXPR = "MAX(" + ClickLog.hit_time.fullName + ")";
    private static final String GROUP_BY = ClickLog.intent_key.fullName;
    private static final String PREFER_LATEST_PREFIX =
        "(" + LAST_HIT_TIME_EXPR + " = (SELECT " + LAST_HIT_TIME_EXPR + " FROM " +
        ClickLog.TABLE_NAME + " WHERE ";
    private static final String PREFER_LATEST_SUFFIX = "))";

    private void buildShortcutQueries() {
        // SQL expression for the time before which no clicks should be counted.
        String cutOffTime_expr = "(?3 - " + mConfig.getMaxStatAgeMillis() + ")";
        // Filter out clicks that are too old
        String ageRestriction = ClickLog.hit_time.fullName + " >= " + cutOffTime_expr;
        String having = null;
        // Order by sum of hit times (seconds since cutoff) for the clicks for each shortcut.
        // This has the effect of multiplying the average hit time with the click count
        String ordering_expr =
                "SUM((" + ClickLog.hit_time.fullName + " - " + cutOffTime_expr + ") / 1000)";

        String where = ageRestriction;
        String preferLatest = PREFER_LATEST_PREFIX + where + PREFER_LATEST_SUFFIX;
        String orderBy = preferLatest + " DESC, " + ordering_expr + " DESC";
        mEmptyQueryShortcutQuery = SQLiteQueryBuilder.buildQueryString(
                false, TABLES, SHORTCUT_QUERY_COLUMNS, where, GROUP_BY, having, orderBy, null);
        if (DBG) Log.d(TAG, "Empty shortcut query:\n" + mEmptyQueryShortcutQuery);

        where = PREFIX_RESTRICTION + " AND " + ageRestriction;
        preferLatest = PREFER_LATEST_PREFIX + where + PREFER_LATEST_SUFFIX;
        orderBy = preferLatest + " DESC, " + ordering_expr + " DESC";
        mShortcutQuery = SQLiteQueryBuilder.buildQueryString(
                false, TABLES, SHORTCUT_QUERY_COLUMNS, where, GROUP_BY, having, orderBy, null);
        if (DBG) Log.d(TAG, "Empty shortcut:\n" + mShortcutQuery);
    }

    /**
     * @return sql that ranks sources by total clicks, filtering out sources
     *         without enough clicks.
     */
    private static String buildSourceRankingSql() {
        final String orderingExpr = SourceStats.total_clicks.name();
        final String tables = SourceStats.TABLE_NAME;
        final String[] columns = SourceStats.COLUMNS;
        final String where = SourceStats.total_clicks + " >= $1";
        final String groupBy = null;
        final String having = null;
        final String orderBy = orderingExpr + " DESC";
        final String limit = null;
        return SQLiteQueryBuilder.buildQueryString(
                false, tables, columns, where, groupBy, having, orderBy, limit);
    }

    protected DbOpenHelper getOpenHelper() {
        return mOpenHelper;
    }

    private void runTransactionAsync(final SQLiteTransaction transaction) {
        mLogExecutor.execute(new Runnable() {
            public void run() {
                transaction.run(mOpenHelper.getWritableDatabase());
            }
        });
    }

    private <A> void runQueryAsync(final SQLiteAsyncQuery<A> query, final Consumer<A> consumer) {
        mLogExecutor.execute(new Runnable() {
            public void run() {
                query.run(mOpenHelper.getReadableDatabase(), consumer);
            }
        });
    }

// --------------------- Interface ShortcutRepository ---------------------

    public void hasHistory(Consumer<Boolean> consumer) {
        runQueryAsync(new SQLiteAsyncQuery<Boolean>() {
            @Override
            protected Boolean performQuery(SQLiteDatabase db) {
                return hasHistory(db);
            }
        }, consumer);
    }

    public void removeFromHistory(SuggestionCursor suggestions, int position) {
        suggestions.moveTo(position);
        final String intentKey = makeIntentKey(suggestions);
        runTransactionAsync(new SQLiteTransaction() {
            @Override
            public boolean performTransaction(SQLiteDatabase db) {
                db.delete(Shortcuts.TABLE_NAME, Shortcuts.intent_key.fullName + " = ?",
                        new String[]{ intentKey });
                return true;
            }
        });
    }

    public void clearHistory() {
        runTransactionAsync(new SQLiteTransaction() {
            @Override
            public boolean performTransaction(SQLiteDatabase db) {
                db.delete(ClickLog.TABLE_NAME, null, null);
                db.delete(Shortcuts.TABLE_NAME, null, null);
                db.delete(SourceStats.TABLE_NAME, null, null);
                return true;
            }
        });
    }

    @VisibleForTesting
    public void deleteRepository() {
        getOpenHelper().deleteDatabase();
    }

    public void close() {
        getOpenHelper().close();
    }

    public void reportClick(final SuggestionCursor suggestions, final int position) {
        final long now = System.currentTimeMillis();
        reportClickAtTime(suggestions, position, now);
    }

    public void getShortcutsForQuery(final String query, final Collection<Corpus> allowedCorpora,
            final boolean allowWebSearchShortcuts, final Consumer<ShortcutCursor> consumer) {
        final long now = System.currentTimeMillis();
        mLogExecutor.execute(new Runnable() {
            public void run() {
                ShortcutCursor shortcuts = getShortcutsForQuery(query, allowedCorpora,
                        allowWebSearchShortcuts, now);
                Consumers.consumeCloseable(consumer, shortcuts);
            }
        });
    }

    public void updateShortcut(Source source, String shortcutId, SuggestionCursor refreshed) {
        refreshShortcut(source, shortcutId, refreshed);
    }

    public void getCorpusScores(final Consumer<Map<String, Integer>> consumer) {
        runQueryAsync(new SQLiteAsyncQuery<Map<String, Integer>>() {
            @Override
            protected Map<String, Integer> performQuery(SQLiteDatabase db) {
                return getCorpusScores();
            }
        }, consumer);
    }

// -------------------------- end ShortcutRepository --------------------------

    private boolean hasHistory(SQLiteDatabase db) {
        Cursor cursor = db.rawQuery(HAS_HISTORY_QUERY, null);
        try {
            if (DBG) Log.d(TAG, "hasHistory(): cursor=" + cursor);
            return cursor != null && cursor.getCount() > 0;
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    private Map<String,Integer> getCorpusScores() {
        return getCorpusScores(mConfig.getMinClicksForSourceRanking());
    }

    private boolean shouldRefresh(Suggestion suggestion) {
        return mRefresher.shouldRefresh(suggestion.getSuggestionSource(),
                suggestion.getShortcutId());
    }

    @VisibleForTesting
    ShortcutCursor getShortcutsForQuery(String query, Collection<Corpus> allowedCorpora,
            boolean allowWebSearchShortcuts, long now) {
        if (DBG) Log.d(TAG, "getShortcutsForQuery(" + query + "," + allowedCorpora + ")");
        String sql = query.length() == 0 ? mEmptyQueryShortcutQuery : mShortcutQuery;
        String[] params = buildShortcutQueryParams(query, now);

        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(sql, params);
        if (cursor.getCount() == 0) {
            cursor.close();
            return null;
        }

        if (DBG) Log.d(TAG, "Allowed sources: ");
        HashMap<String,Source> allowedSources = new HashMap<String,Source>();
        for (Corpus corpus : allowedCorpora) {
            for (Source source : corpus.getSources()) {
                if (DBG) Log.d(TAG, "\t" + source.getName());
                allowedSources.put(source.getName(), source);
            }
        }

        return new ShortcutCursor(new SuggestionCursorImpl(allowedSources, query, cursor),
                allowWebSearchShortcuts, mUiThread, mRefresher, this);
    }

    @VisibleForTesting
    void refreshShortcut(Source source, final String shortcutId,
            SuggestionCursor refreshed) {
        if (source == null) throw new NullPointerException("source");
        if (shortcutId == null) throw new NullPointerException("shortcutId");

        final String[] whereArgs = { shortcutId, source.getName() };
        final ContentValues shortcut;
        if (refreshed == null || refreshed.getCount() == 0) {
            shortcut = null;
        } else {
            refreshed.moveTo(0);
            shortcut = makeShortcutRow(refreshed);
        }

        runTransactionAsync(new SQLiteTransaction() {
            @Override
            protected boolean performTransaction(SQLiteDatabase db) {
                if (shortcut == null) {
                    if (DBG) Log.d(TAG, "Deleting shortcut: " + shortcutId);
                    db.delete(Shortcuts.TABLE_NAME, SHORTCUT_BY_ID_WHERE, whereArgs);
                } else {
                    if (DBG) Log.d(TAG, "Updating shortcut: " + shortcut);
                    db.updateWithOnConflict(Shortcuts.TABLE_NAME, shortcut,
                            SHORTCUT_BY_ID_WHERE, whereArgs, SQLiteDatabase.CONFLICT_REPLACE);
                }
                return true;
            }
        });
    }

    private class SuggestionCursorImpl extends CursorBackedSuggestionCursor {

        private final HashMap<String, Source> mAllowedSources;
        private final int mExtrasColumn;

        public SuggestionCursorImpl(HashMap<String,Source> allowedSources,
                String userQuery, Cursor cursor) {
            super(userQuery, cursor);
            mAllowedSources = allowedSources;
            mExtrasColumn = cursor.getColumnIndex(Shortcuts.custom_columns.name());
        }

        @Override
        public Source getSuggestionSource() {
            int srcCol = mCursor.getColumnIndex(Shortcuts.source.name());
            String srcStr = mCursor.getString(srcCol);
            if (srcStr == null) {
                throw new NullPointerException("Missing source for shortcut.");
            }
            Source source = mAllowedSources.get(srcStr);
            if (source == null) {
                if (DBG) {
                    Log.d(TAG, "Source " + srcStr + " (position " + mCursor.getPosition() +
                            ") not allowed");
                }
                return null;
            }
            int versionCode = mCursor.getInt(Shortcuts.source_version_code.ordinal());
            if (!source.isVersionCodeCompatible(versionCode)) {
                if (DBG) {
                    Log.d(TAG, "Version " + versionCode + " not compatible with " +
                            source.getVersionCode() + " for source " + srcStr);
                }
                return null;
            }
            return source;
        }

        @Override
        public ComponentName getSuggestionIntentComponent() {
            int componentCol = mCursor.getColumnIndex(Shortcuts.intent_component.name());
            // We don't fall back to getSuggestionSource().getIntentComponent() because
            // we want to return the same value that getSuggestionIntentComponent() did for the
            // original suggestion.
            return stringToComponentName(mCursor.getString(componentCol));
        }

        @Override
        public String getSuggestionIcon2() {
            if (isSpinnerWhileRefreshing() && shouldRefresh(this)) {
                if (DBG) Log.d(TAG, "shortcut " + getShortcutId() + " refreshing");
                return mSearchSpinner;
            }
            if (DBG) Log.d(TAG, "shortcut " + getShortcutId() + " NOT refreshing");
            return super.getSuggestionIcon2();
        }

        public boolean isSuggestionShortcut() {
            return true;
        }

        public boolean isHistorySuggestion() {
            // This always returns false, even for suggestions that originally came
            // from server-side history, since we'd otherwise have to parse the Genie
            // extra data. This is ok, since this method is only used for the
            // "Remove from history" UI, which is also shown for all shortcuts.
            return false;
        }

        @Override
        public SuggestionExtras getExtras() {
            String json = mCursor.getString(mExtrasColumn);
            if (!TextUtils.isEmpty(json)) {
                try {
                    return new JsonBackedSuggestionExtras(json);
                } catch (JSONException e) {
                    Log.e(TAG, "Could not parse JSON extras from DB: " + json);
                }
            }
            return null;
        }

        public Collection<String> getExtraColumns() {
            /*
             * We always return null here because:
             * - to return an accurate value, we'd have to aggregate all the extra columns in all
             *   shortcuts in the shortcuts table, which would mean parsing ALL the JSON contained
             *   therein
             * - ListSuggestionCursor does this aggregation, and does it lazily
             * - All shortcuts are put into a ListSuggestionCursor during the promotion process, so
             *   relying on ListSuggestionCursor to do the aggregation means that we only parse the
             *   JSON for shortcuts that are actually displayed.
             */
            return null;
        }
    }

    /**
     * Builds a parameter list for the queries built by {@link #buildShortcutQueries}.
     */
    private static String[] buildShortcutQueryParams(String query, long now) {
        return new String[]{ query, nextString(query), String.valueOf(now) };
    }

    /**
     * Given a string x, this method returns the least string y such that x is not a prefix of y.
     * This is useful to implement prefix filtering by comparison, since the only strings z that
     * have x as a prefix are such that z is greater than or equal to x and z is less than y.
     *
     * @param str A non-empty string. The contract above is not honored for an empty input string,
     *        since all strings have the empty string as a prefix.
     */
    private static String nextString(String str) {
        int len = str.length();
        if (len == 0) {
            return str;
        }
        // The last code point in the string. Within the Basic Multilingual Plane,
        // this is the same as str.charAt(len-1)
        int codePoint = str.codePointBefore(len);
        // This should be safe from overflow, since the largest code point
        // representable in UTF-16 is U+10FFFF.
        int nextCodePoint = codePoint + 1;
        // The index of the start of the last code point.
        // Character.charCount(codePoint) is always 1 (in the BMP) or 2
        int lastIndex = len - Character.charCount(codePoint);
        return new StringBuilder(len)
                .append(str, 0, lastIndex)  // append everything but the last code point
                .appendCodePoint(nextCodePoint)  // instead of the last code point, use successor
                .toString();
    }

    /**
     * Returns the source ranking for sources with a minimum number of clicks.
     *
     * @param minClicks The minimum number of clicks a source must have.
     * @return The list of sources, ranked by total clicks.
     */
    Map<String,Integer> getCorpusScores(int minClicks) {
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        final Cursor cursor = db.rawQuery(
                SOURCE_RANKING_SQL, new String[] { String.valueOf(minClicks) });
        try {
            Map<String,Integer> corpora = new HashMap<String,Integer>(cursor.getCount());
            while (cursor.moveToNext()) {
                String name = cursor.getString(SourceStats.corpus.ordinal());
                int clicks = cursor.getInt(SourceStats.total_clicks.ordinal());
                corpora.put(name, clicks);
            }
            return corpora;
        } finally {
            cursor.close();
        }
    }

    private ContentValues makeShortcutRow(Suggestion suggestion) {
        String intentAction = suggestion.getSuggestionIntentAction();
        String intentComponent = componentNameToString(suggestion.getSuggestionIntentComponent());
        String intentData = suggestion.getSuggestionIntentDataString();
        String intentQuery = suggestion.getSuggestionQuery();
        String intentExtraData = suggestion.getSuggestionIntentExtraData();

        Source source = suggestion.getSuggestionSource();
        String sourceName = source.getName();

        String intentKey = makeIntentKey(suggestion);

        // Get URIs for all icons, to make sure that they are stable
        String icon1Uri = getIconUriString(source, suggestion.getSuggestionIcon1());
        String icon2Uri = getIconUriString(source, suggestion.getSuggestionIcon2());

        String extrasJson = null;
        SuggestionExtras extras = suggestion.getExtras();
        if (extras != null) {
            // flatten any custom columns to JSON. We need to keep any custom columns so that
            // shortcuts for custom suggestion views work properly.
            try {
                extrasJson = extras.toJsonString();
            } catch (JSONException e) {
                Log.e(TAG, "Could not flatten extras to JSON from " + suggestion, e);
            }
        }

        ContentValues cv = new ContentValues();
        cv.put(Shortcuts.intent_key.name(), intentKey);
        cv.put(Shortcuts.source.name(), sourceName);
        cv.put(Shortcuts.source_version_code.name(), source.getVersionCode());
        cv.put(Shortcuts.format.name(), suggestion.getSuggestionFormat());
        cv.put(Shortcuts.title.name(), suggestion.getSuggestionText1());
        cv.put(Shortcuts.description.name(), suggestion.getSuggestionText2());
        cv.put(Shortcuts.description_url.name(), suggestion.getSuggestionText2Url());
        cv.put(Shortcuts.icon1.name(), icon1Uri);
        cv.put(Shortcuts.icon2.name(), icon2Uri);
        cv.put(Shortcuts.intent_action.name(), intentAction);
        cv.put(Shortcuts.intent_component.name(), intentComponent);
        cv.put(Shortcuts.intent_data.name(), intentData);
        cv.put(Shortcuts.intent_query.name(), intentQuery);
        cv.put(Shortcuts.intent_extradata.name(), intentExtraData);
        cv.put(Shortcuts.shortcut_id.name(), suggestion.getShortcutId());
        if (suggestion.isSpinnerWhileRefreshing()) {
            cv.put(Shortcuts.spinner_while_refreshing.name(), "true");
        }
        cv.put(Shortcuts.log_type.name(), suggestion.getSuggestionLogType());
        cv.put(Shortcuts.custom_columns.name(), extrasJson);

        return cv;
    }

    /**
     * Makes a string of the form source#intentData#intentAction#intentQuery
     * for use as a unique identifier of a suggestion.
     * */
    private String makeIntentKey(Suggestion suggestion) {
        String intentAction = suggestion.getSuggestionIntentAction();
        String intentComponent = componentNameToString(suggestion.getSuggestionIntentComponent());
        String intentData = suggestion.getSuggestionIntentDataString();
        String intentQuery = suggestion.getSuggestionQuery();

        Source source = suggestion.getSuggestionSource();
        String sourceName = source.getName();
        StringBuilder key = new StringBuilder(sourceName);
        key.append("#");
        if (intentData != null) {
            key.append(intentData);
        }
        key.append("#");
        if (intentAction != null) {
            key.append(intentAction);
        }
        key.append("#");
        if (intentComponent != null) {
            key.append(intentComponent);
        }
        key.append("#");
        if (intentQuery != null) {
            key.append(intentQuery);
        }

        return key.toString();
    }

    private String componentNameToString(ComponentName component) {
        return component == null ? null : component.flattenToShortString();
    }

    private ComponentName stringToComponentName(String str) {
        return str == null ? null : ComponentName.unflattenFromString(str);
    }

    private String getIconUriString(Source source, String drawableId) {
        // Fast path for empty icons
        if (TextUtils.isEmpty(drawableId) || "0".equals(drawableId)) {
            return null;
        }
        // Fast path for icon URIs
        if (drawableId.startsWith(ContentResolver.SCHEME_ANDROID_RESOURCE)
                || drawableId.startsWith(ContentResolver.SCHEME_CONTENT)
                || drawableId.startsWith(ContentResolver.SCHEME_FILE)) {
            return drawableId;
        }
        Uri uri = source.getIconUri(drawableId);
        return uri == null ? null : uri.toString();
    }

    @VisibleForTesting
    void reportClickAtTime(SuggestionCursor suggestion,
            int position, long now) {
        suggestion.moveTo(position);
        if (DBG) {
            Log.d(TAG, "logClicked(" + suggestion + ")");
        }

        if (SearchManager.SUGGEST_NEVER_MAKE_SHORTCUT.equals(suggestion.getShortcutId())) {
            if (DBG) Log.d(TAG, "clicked suggestion requested not to be shortcuted");
            return;
        }

        Corpus corpus = mCorpora.getCorpusForSource(suggestion.getSuggestionSource());
        if (corpus == null) {
            Log.w(TAG, "no corpus for clicked suggestion");
            return;
        }

        // Once the user has clicked on a shortcut, don't bother refreshing
        // (especially if this is a new shortcut)
        mRefresher.markShortcutRefreshed(suggestion.getSuggestionSource(),
                suggestion.getShortcutId());

        // Add or update suggestion info
        // Since intent_key is the primary key, any existing
        // suggestion with the same source+data+action will be replaced
        final ContentValues shortcut = makeShortcutRow(suggestion);
        String intentKey = shortcut.getAsString(Shortcuts.intent_key.name());

        // Log click for shortcut
        final ContentValues click = new ContentValues();
        click.put(ClickLog.intent_key.name(), intentKey);
        click.put(ClickLog.query.name(), suggestion.getUserQuery());
        click.put(ClickLog.hit_time.name(), now);
        click.put(ClickLog.corpus.name(), corpus.getName());

        runTransactionAsync(new SQLiteTransaction() {
            @Override
            protected boolean performTransaction(SQLiteDatabase db) {
                if (DBG) Log.d(TAG, "Adding shortcut: " + shortcut);
                db.replaceOrThrow(Shortcuts.TABLE_NAME, null, shortcut);
                db.insertOrThrow(ClickLog.TABLE_NAME, null, click);
                return true;
            }
        });
    }

// -------------------------- TABLES --------------------------

    /**
     * shortcuts table
     */
    enum Shortcuts {
        intent_key,
        source,
        source_version_code,
        format,
        title,
        description,
        description_url,
        icon1,
        icon2,
        intent_action,
        intent_component,
        intent_data,
        intent_query,
        intent_extradata,
        shortcut_id,
        spinner_while_refreshing,
        log_type,
        custom_columns;

        static final String TABLE_NAME = "shortcuts";

        public final String fullName;

        Shortcuts() {
            fullName = TABLE_NAME + "." + name();
        }
    }

    /**
     * clicklog table. Has one record for each click.
     */
    enum ClickLog {
        _id,
        intent_key,
        query,
        hit_time,
        corpus;

        static final String[] COLUMNS = initColumns();

        static final String TABLE_NAME = "clicklog";

        private static String[] initColumns() {
            ClickLog[] vals = ClickLog.values();
            String[] columns = new String[vals.length];
            for (int i = 0; i < vals.length; i++) {
                columns[i] = vals[i].fullName;
            }
            return columns;
        }

        public final String fullName;

        ClickLog() {
            fullName = TABLE_NAME + "." + name();
        }
    }

    /**
     * This is an aggregate table of {@link ClickLog} that stays up to date with the total
     * clicks for each corpus. This makes computing the corpus ranking more
     * more efficient, at the expense of some extra work when the clicks are reported.
     */
    enum SourceStats {
        corpus,
        total_clicks;

        static final String TABLE_NAME = "sourcetotals";

        static final String[] COLUMNS = initColumns();

        private static String[] initColumns() {
            SourceStats[] vals = SourceStats.values();
            String[] columns = new String[vals.length];
            for (int i = 0; i < vals.length; i++) {
                columns[i] = vals[i].fullName;
            }
            return columns;
        }

        public final String fullName;

        SourceStats() {
            fullName = TABLE_NAME + "." + name();
        }
    }

// -------------------------- END TABLES --------------------------

    // contains creation and update logic
    private static class DbOpenHelper extends SQLiteOpenHelper {
        private final Config mConfig;
        private String mPath;
        private static final String SHORTCUT_ID_INDEX
                = Shortcuts.TABLE_NAME + "_" + Shortcuts.shortcut_id.name();
        private static final String CLICKLOG_QUERY_INDEX
                = ClickLog.TABLE_NAME + "_" + ClickLog.query.name();
        private static final String CLICKLOG_HIT_TIME_INDEX
                = ClickLog.TABLE_NAME + "_" + ClickLog.hit_time.name();
        private static final String CLICKLOG_INSERT_TRIGGER
                = ClickLog.TABLE_NAME + "_insert";
        private static final String SHORTCUTS_DELETE_TRIGGER
                = Shortcuts.TABLE_NAME + "_delete";
        private static final String SHORTCUTS_UPDATE_INTENT_KEY_TRIGGER
                = Shortcuts.TABLE_NAME + "_update_intent_key";

        public DbOpenHelper(Context context, String name, int version, Config config) {
            super(context, name, null, version);
            mConfig = config;
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // The shortcuts info is not all that important, so we just drop the tables
            // and re-create empty ones.
            Log.i(TAG, "Upgrading shortcuts DB from version " +
                    + oldVersion + " to " + newVersion + ". This deletes all shortcuts.");
            dropTables(db);
            onCreate(db);
        }

        private void dropTables(SQLiteDatabase db) {
            db.execSQL("DROP TRIGGER IF EXISTS " + CLICKLOG_INSERT_TRIGGER);
            db.execSQL("DROP TRIGGER IF EXISTS " + SHORTCUTS_DELETE_TRIGGER);
            db.execSQL("DROP TRIGGER IF EXISTS " + SHORTCUTS_UPDATE_INTENT_KEY_TRIGGER);
            db.execSQL("DROP INDEX IF EXISTS " + CLICKLOG_HIT_TIME_INDEX);
            db.execSQL("DROP INDEX IF EXISTS " + CLICKLOG_QUERY_INDEX);
            db.execSQL("DROP INDEX IF EXISTS " + SHORTCUT_ID_INDEX);
            db.execSQL("DROP TABLE IF EXISTS " + ClickLog.TABLE_NAME);
            db.execSQL("DROP TABLE IF EXISTS " + Shortcuts.TABLE_NAME);
            db.execSQL("DROP TABLE IF EXISTS " + SourceStats.TABLE_NAME);
        }

        /**
         * Deletes the database file.
         */
        public void deleteDatabase() {
            close();
            if (mPath == null) return;
            try {
                new File(mPath).delete();
                if (DBG) Log.d(TAG, "deleted " + mPath);
            } catch (Exception e) {
                Log.w(TAG, "couldn't delete " + mPath, e);
            }
        }

        @Override
        public void onOpen(SQLiteDatabase db) {
            super.onOpen(db);
            mPath = db.getPath();
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + Shortcuts.TABLE_NAME + " (" +
                    // COLLATE UNICODE is needed to make it possible to use nextString()
                    // to implement fast prefix filtering.
                    Shortcuts.intent_key.name() + " TEXT NOT NULL COLLATE UNICODE PRIMARY KEY, " +
                    Shortcuts.source.name() + " TEXT NOT NULL, " +
                    Shortcuts.source_version_code.name() + " INTEGER NOT NULL, " +
                    Shortcuts.format.name() + " TEXT, " +
                    Shortcuts.title.name() + " TEXT, " +
                    Shortcuts.description.name() + " TEXT, " +
                    Shortcuts.description_url.name() + " TEXT, " +
                    Shortcuts.icon1.name() + " TEXT, " +
                    Shortcuts.icon2.name() + " TEXT, " +
                    Shortcuts.intent_action.name() + " TEXT, " +
                    Shortcuts.intent_component.name() + " TEXT, " +
                    Shortcuts.intent_data.name() + " TEXT, " +
                    Shortcuts.intent_query.name() + " TEXT, " +
                    Shortcuts.intent_extradata.name() + " TEXT, " +
                    Shortcuts.shortcut_id.name() + " TEXT, " +
                    Shortcuts.spinner_while_refreshing.name() + " TEXT, " +
                    Shortcuts.log_type.name() + " TEXT, " +
                    Shortcuts.custom_columns.name() + " TEXT" +
                    ");");

            // index for fast lookup of shortcuts by shortcut_id
            db.execSQL("CREATE INDEX " + SHORTCUT_ID_INDEX
                    + " ON " + Shortcuts.TABLE_NAME
                    + "(" + Shortcuts.shortcut_id.name() + ", " + Shortcuts.source.name() + ")");

            db.execSQL("CREATE TABLE " + ClickLog.TABLE_NAME + " ( " +
                    ClickLog._id.name() + " INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                    // type must match Shortcuts.intent_key
                    ClickLog.intent_key.name() + " TEXT NOT NULL COLLATE UNICODE REFERENCES "
                        + Shortcuts.TABLE_NAME + "(" + Shortcuts.intent_key + "), " +
                    ClickLog.query.name() + " TEXT, " +
                    ClickLog.hit_time.name() + " INTEGER," +
                    ClickLog.corpus.name() + " TEXT" +
                    ");");

            // index for fast lookup of clicks by query
            db.execSQL("CREATE INDEX " + CLICKLOG_QUERY_INDEX
                    + " ON " + ClickLog.TABLE_NAME + "(" + ClickLog.query.name() + ")");

            // index for finding old clicks quickly
            db.execSQL("CREATE INDEX " + CLICKLOG_HIT_TIME_INDEX
                    + " ON " + ClickLog.TABLE_NAME + "(" + ClickLog.hit_time.name() + ")");

            // trigger for purging old clicks, i.e. those such that
            // hit_time < now - MAX_MAX_STAT_AGE_MILLIS, where now is the
            // hit_time of the inserted record, and for updating the SourceStats table
            db.execSQL("CREATE TRIGGER " + CLICKLOG_INSERT_TRIGGER + " AFTER INSERT ON "
                    + ClickLog.TABLE_NAME
                    + " BEGIN"
                    + " DELETE FROM " + ClickLog.TABLE_NAME + " WHERE "
                            + ClickLog.hit_time.name() + " <"
                            + " NEW." + ClickLog.hit_time.name()
                                    + " - " + mConfig.getMaxStatAgeMillis() + ";"
                    + " DELETE FROM " + SourceStats.TABLE_NAME + ";"
                    + " INSERT INTO " + SourceStats.TABLE_NAME  + " "
                            + "SELECT " + ClickLog.corpus + "," + "COUNT(*) FROM "
                            + ClickLog.TABLE_NAME + " GROUP BY " + ClickLog.corpus.name() + ";"
                    + " END");

            // trigger for deleting clicks about a shortcut once that shortcut has been
            // deleted
            db.execSQL("CREATE TRIGGER " + SHORTCUTS_DELETE_TRIGGER + " AFTER DELETE ON "
                    + Shortcuts.TABLE_NAME
                    + " BEGIN"
                    + " DELETE FROM " + ClickLog.TABLE_NAME + " WHERE "
                            + ClickLog.intent_key.name()
                            + " = OLD." + Shortcuts.intent_key.name() + ";"
                    + " END");

            // trigger for updating click log entries when a shortcut changes its intent_key
            db.execSQL("CREATE TRIGGER " + SHORTCUTS_UPDATE_INTENT_KEY_TRIGGER
                    + " AFTER UPDATE ON " + Shortcuts.TABLE_NAME
                    + " WHEN NEW." + Shortcuts.intent_key.name()
                            + " != OLD." + Shortcuts.intent_key.name()
                    + " BEGIN"
                    + " UPDATE " + ClickLog.TABLE_NAME + " SET "
                            + ClickLog.intent_key.name() + " = NEW." + Shortcuts.intent_key.name()
                            + " WHERE "
                            + ClickLog.intent_key.name() + " = OLD." + Shortcuts.intent_key.name()
                            + ";"
                    + " END");

            db.execSQL("CREATE TABLE " + SourceStats.TABLE_NAME + " ( " +
                    SourceStats.corpus.name() + " TEXT NOT NULL COLLATE UNICODE PRIMARY KEY, " +
                    SourceStats.total_clicks + " INTEGER);"
                    );
        }
    }
}
