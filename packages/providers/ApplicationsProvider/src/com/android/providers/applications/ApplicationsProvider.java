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

package com.android.providers.applications;

import com.android.internal.content.PackageMonitor;
import com.android.internal.os.PkgUsageStats;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.UriMatcher;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.provider.Applications;
import android.text.TextUtils;
import android.util.Log;

import java.lang.Runnable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.annotations.VisibleForTesting;

/**
 * Fetches the list of applications installed on the phone to provide search suggestions.
 * If the functionality of this provider changes, the documentation at
 * {@link android.provider.Applications} should be updated.
 *
 * TODO: this provider should be moved to the Launcher, which contains similar logic to keep an up
 * to date list of installed applications.  Alternatively, Launcher could be updated to use this
 * provider.
 */
public class ApplicationsProvider extends ContentProvider {

    private static final boolean DBG = false;

    private static final String TAG = "ApplicationsProvider";

    private static final int SEARCH_SUGGEST = 0;
    private static final int SHORTCUT_REFRESH = 1;
    private static final int SEARCH = 2;

    private static final UriMatcher sURIMatcher = buildUriMatcher();

    private static final int THREAD_PRIORITY = android.os.Process.THREAD_PRIORITY_BACKGROUND;

    // Messages for mHandler
    private static final int MSG_UPDATE_ALL = 0;
    private static final int MSG_UPDATE_PACKAGE = 1;

    public static final String _ID = "_id";
    public static final String NAME = "name";
    public static final String DESCRIPTION = "description";
    public static final String PACKAGE = "package";
    public static final String CLASS = "class";
    public static final String ICON = "icon";
    public static final String LAUNCH_COUNT = "launch_count";
    public static final String LAST_RESUME_TIME = "last_resume_time";

    // A query parameter to refresh application statistics. Used by QSB.
    public static final String REFRESH_STATS = "refresh";

    private static final String APPLICATIONS_TABLE = "applications";

    private static final String APPLICATIONS_LOOKUP_JOIN =
            "applicationsLookup JOIN " + APPLICATIONS_TABLE + " ON"
            + " applicationsLookup.source = " + APPLICATIONS_TABLE + "." + _ID;

    private static final HashMap<String, String> sSearchSuggestionsProjectionMap =
            buildSuggestionsProjectionMap(false);
    private static final HashMap<String, String> sGlobalSearchSuggestionsProjectionMap =
            buildSuggestionsProjectionMap(true);
    private static final HashMap<String, String> sSearchProjectionMap =
            buildSearchProjectionMap();

    /**
     * An in-memory database storing the details of applications installed on
     * the device. Populated when the ApplicationsProvider is launched.
     */
    private SQLiteDatabase mDb;

    // Handler that runs DB updates.
    private Handler mHandler;

    /**
     * We delay application updates by this many millis to avoid doing more than one update to the
     * applications list within this window.
     */
    private static final long UPDATE_DELAY_MILLIS = 1000L;

    private static UriMatcher buildUriMatcher() {
        UriMatcher matcher =  new UriMatcher(UriMatcher.NO_MATCH);
        matcher.addURI(Applications.AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY,
                SEARCH_SUGGEST);
        matcher.addURI(Applications.AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY + "/*",
                SEARCH_SUGGEST);
        matcher.addURI(Applications.AUTHORITY, SearchManager.SUGGEST_URI_PATH_SHORTCUT,
                SHORTCUT_REFRESH);
        matcher.addURI(Applications.AUTHORITY, SearchManager.SUGGEST_URI_PATH_SHORTCUT + "/*",
                SHORTCUT_REFRESH);
        matcher.addURI(Applications.AUTHORITY, Applications.SEARCH_PATH,
                SEARCH);
        matcher.addURI(Applications.AUTHORITY, Applications.SEARCH_PATH + "/*",
                SEARCH);
        return matcher;
    }

    /**
     * Updates applications list when packages are added/removed.
     *
     * TODO: Maybe this should listen for changes to individual apps instead.
     */
    private class MyPackageMonitor extends PackageMonitor {
        @Override
        public void onSomePackagesChanged() {
            postUpdateAll();
        }

        @Override
        public void onPackageModified(String packageName) {
            postUpdatePackage(packageName);
        }
    }

    // Broadcast receiver for updating applications list when the locale changes.
    private BroadcastReceiver mLocaleChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_LOCALE_CHANGED.equals(action)) {
                if (DBG) Log.d(TAG, "locale changed");
                postUpdateAll();
            }
        }
    };

    @Override
    public boolean onCreate() {
        createDatabase();
        // Start thread that runs app updates
        HandlerThread thread = new HandlerThread("ApplicationsProviderUpdater", THREAD_PRIORITY);
        thread.start();
        mHandler = createHandler(thread.getLooper());
        // Kick off first apps update
        postUpdateAll();
        // Listen for package changes
        new MyPackageMonitor().register(getContext(), null, true);
        // Listen for locale changes
        IntentFilter localeFilter = new IntentFilter(Intent.ACTION_LOCALE_CHANGED);
        getContext().registerReceiver(mLocaleChangeReceiver, localeFilter);
        return true;
    }

    @VisibleForTesting
    Handler createHandler(Looper looper) {
        return new UpdateHandler(looper);
    }

    @VisibleForTesting
    class UpdateHandler extends Handler {

        public UpdateHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_UPDATE_ALL:
                    updateApplicationsList(null);
                    break;
                case MSG_UPDATE_PACKAGE:
                    updateApplicationsList((String) msg.obj);
                    break;
                default:
                    Log.e(TAG, "Unknown message: " + msg.what);
                    break;
            }
        }
    }

    /**
     * Posts an update to run on the DB update thread.
     */
    private void postUpdateAll() {
        // Clear pending updates
        mHandler.removeMessages(MSG_UPDATE_ALL);
        // Post a new update
        Message msg = Message.obtain();
        msg.what = MSG_UPDATE_ALL;
        mHandler.sendMessageDelayed(msg, UPDATE_DELAY_MILLIS);
    }

    private void postUpdatePackage(String packageName) {
        Message msg = Message.obtain();
        msg.what = MSG_UPDATE_PACKAGE;
        msg.obj = packageName;
        mHandler.sendMessageDelayed(msg, UPDATE_DELAY_MILLIS);
    }

    // ----------
    // END ASYC UPDATE CODE
    // ----------


    /**
     * Creates an in-memory database for storing application info.
     */
    private void createDatabase() {
        mDb = SQLiteDatabase.create(null);
        mDb.execSQL("CREATE TABLE IF NOT EXISTS " + APPLICATIONS_TABLE + " ("+
                _ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                NAME + " TEXT COLLATE LOCALIZED," +
                DESCRIPTION + " description TEXT," +
                PACKAGE + " TEXT," +
                CLASS + " TEXT," +
                ICON + " TEXT," +
                LAUNCH_COUNT + " INTEGER DEFAULT 0," +
                LAST_RESUME_TIME + " INTEGER DEFAULT 0" +
                ");");
        // Needed for efficient update and remove
        mDb.execSQL("CREATE INDEX applicationsComponentIndex ON " + APPLICATIONS_TABLE + " ("
                + PACKAGE + "," + CLASS + ");");
        // Maps token from the app name to records in the applications table
        mDb.execSQL("CREATE TABLE applicationsLookup (" +
                "token TEXT," +
                "source INTEGER REFERENCES " + APPLICATIONS_TABLE + "(" + _ID + ")," +
                "token_index INTEGER" +
                ");");
        mDb.execSQL("CREATE INDEX applicationsLookupIndex ON applicationsLookup (" +
                "token," +
                "source" +
                ");");
        // Triggers to keep the applicationsLookup table up to date
        mDb.execSQL("CREATE TRIGGER applicationsLookup_update UPDATE OF " + NAME + " ON " +
                APPLICATIONS_TABLE + " " +
                "BEGIN " +
                "DELETE FROM applicationsLookup WHERE source = new." + _ID + ";" +
                "SELECT _TOKENIZE('applicationsLookup', new." + _ID + ", new." + NAME + ", ' ', 1);" +
                "END");
        mDb.execSQL("CREATE TRIGGER applicationsLookup_insert AFTER INSERT ON " +
                APPLICATIONS_TABLE + " " +
                "BEGIN " +
                "SELECT _TOKENIZE('applicationsLookup', new." + _ID + ", new." + NAME + ", ' ', 1);" +
                "END");
        mDb.execSQL("CREATE TRIGGER applicationsLookup_delete DELETE ON " +
                APPLICATIONS_TABLE + " " +
                "BEGIN " +
                "DELETE FROM applicationsLookup WHERE source = old." + _ID + ";" +
                "END");
    }

    /**
     * This will always return {@link SearchManager#SUGGEST_MIME_TYPE} as this
     * provider is purely to provide suggestions.
     */
    @Override
    public String getType(Uri uri) {
        switch (sURIMatcher.match(uri)) {
            case SEARCH_SUGGEST:
                return SearchManager.SUGGEST_MIME_TYPE;
            case SHORTCUT_REFRESH:
                return SearchManager.SHORTCUT_MIME_TYPE;
            case SEARCH:
                return Applications.APPLICATION_DIR_TYPE;
            default:
                throw new IllegalArgumentException("URL " + uri + " doesn't support querying.");
        }
    }

    /**
     * Queries for a given search term and returns a cursor containing
     * suggestions ordered by best match.
     */
    @Override
    public Cursor query(Uri uri, String[] projectionIn, String selection,
            String[] selectionArgs, String sortOrder) {
        if (DBG) Log.d(TAG, "query(" + uri + ")");

        if (!TextUtils.isEmpty(selection)) {
            throw new IllegalArgumentException("selection not allowed for " + uri);
        }
        if (selectionArgs != null && selectionArgs.length != 0) {
            throw new IllegalArgumentException("selectionArgs not allowed for " + uri);
        }
        if (!TextUtils.isEmpty(sortOrder)) {
            throw new IllegalArgumentException("sortOrder not allowed for " + uri);
        }

        switch (sURIMatcher.match(uri)) {
            case SEARCH_SUGGEST: {
                String query = null;
                if (uri.getPathSegments().size() > 1) {
                    query = uri.getLastPathSegment().toLowerCase();
                }
                if (uri.getQueryParameter(REFRESH_STATS) != null) {
                    updateUsageStats();
                }
                return getSuggestions(query, projectionIn);
            }
            case SHORTCUT_REFRESH: {
                String shortcutId = null;
                if (uri.getPathSegments().size() > 1) {
                    shortcutId = uri.getLastPathSegment();
                }
                return refreshShortcut(shortcutId, projectionIn);
            }
            case SEARCH: {
                String query = null;
                if (uri.getPathSegments().size() > 1) {
                    query = uri.getLastPathSegment().toLowerCase();
                }
                return getSearchResults(query, projectionIn);
            }
            default:
                throw new IllegalArgumentException("URL " + uri + " doesn't support querying.");
        }
    }

    private Cursor getSuggestions(String query, String[] projectionIn) {
        Map<String, String> projectionMap = sSearchSuggestionsProjectionMap;
        // No zero-query suggestions or launch times except for global search,
        // to avoid leaking info about apps that have been used.
        if (hasGlobalSearchPermission()) {
            projectionMap = sGlobalSearchSuggestionsProjectionMap;
        } else if (TextUtils.isEmpty(query)) {
            return null;
        }
        return searchApplications(query, projectionIn, projectionMap);
    }

    /**
     * Refreshes the shortcut of an application.
     *
     * @param shortcutId Flattened component name of an activity.
     */
    private Cursor refreshShortcut(String shortcutId, String[] projectionIn) {
        ComponentName component = ComponentName.unflattenFromString(shortcutId);
        if (component == null) {
            Log.w(TAG, "Bad shortcut id: " + shortcutId);
            return null;
        }
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(APPLICATIONS_TABLE);
        qb.setProjectionMap(sSearchSuggestionsProjectionMap);
        qb.appendWhere("package = ? AND class = ?");
        String[] selectionArgs = { component.getPackageName(), component.getClassName() };
        Cursor cursor = qb.query(mDb, projectionIn, null, selectionArgs, null, null, null);
        if (DBG) Log.d(TAG, "Returning " + cursor.getCount() + " results for shortcut refresh.");
        return cursor;
    }

    private Cursor getSearchResults(String query, String[] projectionIn) {
        return searchApplications(query, projectionIn, sSearchProjectionMap);
    }

    private Cursor searchApplications(String query, String[] projectionIn,
            Map<String, String> columnMap) {
        final boolean zeroQuery = TextUtils.isEmpty(query);
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(APPLICATIONS_LOOKUP_JOIN);
        qb.setProjectionMap(columnMap);
        String orderBy = null;
        if (!zeroQuery) {
            qb.appendWhere(buildTokenFilter(query));
        } else {
            if (hasGlobalSearchPermission()) {
                qb.appendWhere(LAST_RESUME_TIME + " > 0");
            }
        }
        if (!hasGlobalSearchPermission()) {
            orderBy = getOrderBy(zeroQuery);
        }
        // don't return duplicates when there are two matching tokens for an app
        String groupBy = APPLICATIONS_TABLE + "." + _ID;
        Cursor cursor = qb.query(mDb, projectionIn, null, null, groupBy, null, orderBy);
        if (DBG) Log.d(TAG, "Returning " + cursor.getCount() + " results for " + query);
        return cursor;
    }

    private String getOrderBy(boolean zeroQuery) {
        // order first by whether it a full prefix match, then by launch
        // count (if allowed, frequently used apps rank higher), then name
        // MIN(token_index) != 0 is true for non-full prefix matches,
        // and since false (0) < true(1), this expression makes sure
        // that full prefix matches come first.
        StringBuilder orderBy = new StringBuilder();
        if (!zeroQuery) {
            orderBy.append("MIN(token_index) != 0, ");
        }

        if (hasGlobalSearchPermission()) {
            orderBy.append(LAST_RESUME_TIME + " DESC, ");
        }

        orderBy.append(NAME);

        return orderBy.toString();
    }

    @SuppressWarnings("deprecation")
    private String buildTokenFilter(String filterParam) {
        StringBuilder filter = new StringBuilder("token GLOB ");
        // NOTE: Query parameters won't work here since the SQL compiler
        // needs to parse the actual string to know that it can use the
        // index to do a prefix scan.
        DatabaseUtils.appendEscapedSQLString(filter,
                DatabaseUtils.getHexCollationKey(filterParam) + "*");
        return filter.toString();
    }

    private static HashMap<String, String> buildSuggestionsProjectionMap(boolean forGlobalSearch) {
        HashMap<String, String> map = new HashMap<String, String>();
        addProjection(map, Applications.ApplicationColumns._ID, _ID);
        addProjection(map, SearchManager.SUGGEST_COLUMN_TEXT_1, NAME);
        addProjection(map, SearchManager.SUGGEST_COLUMN_TEXT_2, DESCRIPTION);
        addProjection(map, SearchManager.SUGGEST_COLUMN_INTENT_DATA,
                "'content://" + Applications.AUTHORITY + "/applications/'"
                + " || " + PACKAGE + " || '/' || " + CLASS);
        addProjection(map, SearchManager.SUGGEST_COLUMN_ICON_1, ICON);
        addProjection(map, SearchManager.SUGGEST_COLUMN_ICON_2, "NULL");
        addProjection(map, SearchManager.SUGGEST_COLUMN_SHORTCUT_ID,
                PACKAGE + " || '/' || " + CLASS);
        if (forGlobalSearch) {
            addProjection(map, SearchManager.SUGGEST_COLUMN_LAST_ACCESS_HINT,
                    LAST_RESUME_TIME);
        }
        return map;
    }

    private static HashMap<String, String> buildSearchProjectionMap() {
        HashMap<String, String> map = new HashMap<String, String>();
        addProjection(map, Applications.ApplicationColumns._ID, _ID);
        addProjection(map, Applications.ApplicationColumns.NAME, NAME);
        addProjection(map, Applications.ApplicationColumns.ICON, ICON);
        addProjection(map, Applications.ApplicationColumns.URI,
                "'content://" + Applications.AUTHORITY + "/applications/'"
                + " || " + PACKAGE + " || '/' || " + CLASS);
        return map;
    }

    private static void addProjection(HashMap<String, String> map, String name, String value) {
        if (!value.equals(name)) {
            value = value + " AS " + name;
        }
        map.put(name, value);
    }

    /**
     * Updates the cached list of installed applications.
     *
     * @param packageName Name of package whose activities to update.
     *        If {@code null}, all packages are updated.
     */
    private synchronized void updateApplicationsList(String packageName) {
        if (DBG) Log.d(TAG, "Updating database (packageName = " + packageName + ")...");

        DatabaseUtils.InsertHelper inserter =
                new DatabaseUtils.InsertHelper(mDb, APPLICATIONS_TABLE);
        int nameCol = inserter.getColumnIndex(NAME);
        int descriptionCol = inserter.getColumnIndex(DESCRIPTION);
        int packageCol = inserter.getColumnIndex(PACKAGE);
        int classCol = inserter.getColumnIndex(CLASS);
        int iconCol = inserter.getColumnIndex(ICON);
        int launchCountCol = inserter.getColumnIndex(LAUNCH_COUNT);
        int lastResumeTimeCol = inserter.getColumnIndex(LAST_RESUME_TIME);

        Map<String, PkgUsageStats> usageStats = fetchUsageStats();

        mDb.beginTransaction();
        try {
            removeApplications(packageName);
            String description = getContext().getString(R.string.application_desc);
            // Iterate and find all the activities which have the LAUNCHER category set.
            Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            if (packageName != null) {
                // Limit to activities in the package, if given
                mainIntent.setPackage(packageName);
            }
            final PackageManager manager = getPackageManager();
            List<ResolveInfo> activities = manager.queryIntentActivities(mainIntent, 0);
            int activityCount = activities == null ? 0 : activities.size();
            for (int i = 0; i < activityCount; i++) {
                ResolveInfo info = activities.get(i);
                String title = info.loadLabel(manager).toString();
                String activityClassName = info.activityInfo.name;
                if (TextUtils.isEmpty(title)) {
                    title = activityClassName;
                }

                String activityPackageName = info.activityInfo.applicationInfo.packageName;
                if (DBG) Log.d(TAG, "activity " + activityPackageName + "/" + activityClassName);
                PkgUsageStats stats = usageStats.get(activityPackageName);
                int launchCount = 0;
                long lastResumeTime = 0;
                if (stats != null) {
                    launchCount = stats.launchCount;
                    if (stats.componentResumeTimes.containsKey(activityClassName)) {
                        lastResumeTime = stats.componentResumeTimes.get(activityClassName);
                    }
                }

                String icon = getActivityIconUri(info.activityInfo);
                inserter.prepareForInsert();
                inserter.bind(nameCol, title);
                inserter.bind(descriptionCol, description);
                inserter.bind(packageCol, activityPackageName);
                inserter.bind(classCol, activityClassName);
                inserter.bind(iconCol, icon);
                inserter.bind(launchCountCol, launchCount);
                inserter.bind(lastResumeTimeCol, lastResumeTime);
                inserter.execute();
            }
            mDb.setTransactionSuccessful();
        } finally {
            mDb.endTransaction();
            inserter.close();
        }

        if (DBG) Log.d(TAG, "Finished updating database.");
    }

    @VisibleForTesting
    protected synchronized void updateUsageStats() {
        if (DBG) Log.d(TAG, "Update application usage stats.");
        Map<String, PkgUsageStats> usageStats = fetchUsageStats();

        mDb.beginTransaction();
        try {
            for (Map.Entry<String, PkgUsageStats> statsEntry : usageStats.entrySet()) {
                ContentValues updatedLaunchCount = new ContentValues();
                String packageName = statsEntry.getKey();
                PkgUsageStats stats = statsEntry.getValue();
                updatedLaunchCount.put(LAUNCH_COUNT, stats.launchCount);

                mDb.update(APPLICATIONS_TABLE, updatedLaunchCount,
                        PACKAGE + " = ?", new String[] { packageName });

                for (Map.Entry<String, Long> crtEntry: stats.componentResumeTimes.entrySet()) {
                    ContentValues updatedLastResumeTime = new ContentValues();
                    String componentName = crtEntry.getKey();
                    updatedLastResumeTime.put(LAST_RESUME_TIME, crtEntry.getValue());

                    mDb.update(APPLICATIONS_TABLE, updatedLastResumeTime,
                            PACKAGE + " = ? AND " + CLASS + " = ?",
                            new String[] { packageName, componentName });
                }
            }
            mDb.setTransactionSuccessful();
        } finally {
            mDb.endTransaction();
        }

        if (DBG) Log.d(TAG, "Finished updating application usage stats in database.");
    }

    private String getActivityIconUri(ActivityInfo activityInfo) {
        int icon = activityInfo.getIconResource();
        if (icon == 0) return null;
        Uri uri = getResourceUri(activityInfo.applicationInfo, icon);
        return uri == null ? null : uri.toString();
    }

    private void removeApplications(String packageName) {
        if (packageName == null) {
            mDb.delete(APPLICATIONS_TABLE, null, null);
        } else {
            mDb.delete(APPLICATIONS_TABLE, PACKAGE + " = ?", new String[] { packageName });
        }

    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
            String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

    private Uri getResourceUri(ApplicationInfo appInfo, int res) {
        try {
            Resources resources = getPackageManager().getResourcesForApplication(appInfo);
            return getResourceUri(resources, appInfo.packageName, res);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        } catch (Resources.NotFoundException e) {
            return null;
        }
    }

    private static Uri getResourceUri(Resources resources, String appPkg, int res)
            throws Resources.NotFoundException {
        String resPkg = resources.getResourcePackageName(res);
        String type = resources.getResourceTypeName(res);
        String name = resources.getResourceEntryName(res);
        return makeResourceUri(appPkg, resPkg, type, name);
    }

    private static Uri makeResourceUri(String appPkg, String resPkg, String type, String name)
            throws Resources.NotFoundException {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.scheme(ContentResolver.SCHEME_ANDROID_RESOURCE);
        uriBuilder.encodedAuthority(appPkg);
        uriBuilder.appendEncodedPath(type);
        if (!appPkg.equals(resPkg)) {
            uriBuilder.appendEncodedPath(resPkg + ":" + name);
        } else {
            uriBuilder.appendEncodedPath(name);
        }
        return uriBuilder.build();
    }

    @VisibleForTesting
    protected Map<String, PkgUsageStats> fetchUsageStats() {
        try {
            ActivityManager activityManager = (ActivityManager)
                    getContext().getSystemService(Context.ACTIVITY_SERVICE);

            if (activityManager != null) {
                Map<String, PkgUsageStats> stats = new HashMap<String, PkgUsageStats>();
                PkgUsageStats[] pkgUsageStats = activityManager.getAllPackageUsageStats();
                if (pkgUsageStats != null) {
                    for (PkgUsageStats pus : pkgUsageStats) {
                        stats.put(pus.packageName, pus);
                    }
                }
                return stats;
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not fetch usage stats", e);
        }
        return new HashMap<String, PkgUsageStats>();
    }

    @VisibleForTesting
    protected PackageManager getPackageManager() {
        return getContext().getPackageManager();
    }

    @VisibleForTesting
    protected boolean hasGlobalSearchPermission() {
        // Only the global-search system is allowed to see the usage stats of
        // applications. Without this restriction the ApplicationsProvider
        // could leak information about the user's behavior to applications.
        return (PackageManager.PERMISSION_GRANTED ==
                getContext().checkCallingPermission(android.Manifest.permission.GLOBAL_SEARCH));
    }

}
