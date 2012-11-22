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

package com.android.providers.applications;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Applications;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

public class ApplicationsAdapter extends ResourceCursorAdapter {

    private static final boolean DBG = false;
    private static final String TAG = "ApplicationsAdapter";

    public ApplicationsAdapter(Context context, Cursor c) {
        super(context, R.layout.application_list_item, c);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ImageView icon1 = (ImageView) view.findViewById(R.id.icon1);
        TextView text1 = (TextView) view.findViewById(R.id.text1);
        Uri iconUri = getColumnUri(cursor, Applications.ApplicationColumns.ICON);
        String name = getColumnString(cursor, Applications.ApplicationColumns.NAME);
        if (DBG) Log.d(TAG, "name=" + name + ",icon=" + iconUri);
        icon1.setImageURI(iconUri);
        text1.setText(name);
    }

    public static Uri getColumnUri(Cursor cursor, String columnName) {
        String uriString = getColumnString(cursor, columnName);
        if (TextUtils.isEmpty(uriString)) return null;
        return Uri.parse(uriString);
    }

    public static String getColumnString(Cursor cursor, String columnName) {
        int col = cursor.getColumnIndex(columnName);
        return getStringOrNull(cursor, col);
    }

    private static String getStringOrNull(Cursor cursor, int col) {
        if (col < 0) return null;
        try {
            return cursor.getString(col);
        } catch (RuntimeException e) {
            Log.e(TAG, "Failed to get column " + col + " from cursor", e);
            return null;
        }
    }
}
