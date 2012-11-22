/*
 * Copyright (C) 2012 The Android Open Source Project
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
package com.android.dreams.phototable;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.service.dreams.DreamService;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import java.util.Set;

/**
 * Example interactive screen saver: flick photos onto a table.
 */
public class PhotoTableDream extends DreamService {
    public static final String TAG = "PhotoTableDream";
    private PhotoTable mTable;

    @Override
    public void onDreamingStarted() {
        super.onDreamingStarted();
        setInteractive(true);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        LayoutInflater inflater =
                (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        AlbumSettings settings = AlbumSettings.getAlbumSettings(
                getSharedPreferences(PhotoTableDreamSettings.PREFS_NAME, 0));
        if (settings.isConfigured()) {
            ViewGroup view = (ViewGroup) inflater.inflate(R.layout.table, null);
            PhotoTable table = (PhotoTable) view.findViewById(R.id.table);
            table.setDream(this);
            setContentView(view);
        } else {
            Resources resources = getResources();
            ViewGroup view = (ViewGroup) inflater.inflate(R.layout.bummer, null);
            BummerView bummer = (BummerView) view.findViewById(R.id.bummer);
            bummer.setAnimationParams(true,
                                      resources.getInteger(R.integer.table_drop_period),
                                      resources.getInteger(R.integer.fast_drop));
            setContentView(view);
        }
        setFullscreen(true);
    }
}
