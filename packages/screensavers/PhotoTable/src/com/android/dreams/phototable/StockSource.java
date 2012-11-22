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
import android.util.Log;

import java.io.InputStream;
import java.util.Collection;
import java.util.ArrayList;

/**
 * Picks a random image from the local store.
 */
public class 

StockSource extends PhotoSource {
    public static final String ALBUM_ID = "com.android.dreams.phototable.StockSource";
    private static final String TAG = "PhotoTable.StockSource";
    private static final int[] PHOTOS = { R.drawable.blank_photo };

    private final ArrayList<ImageData> mImageList;
    private final ArrayList<AlbumData> mAlbumList;

    private final String mStockPhotoName;
    private int mNextPosition;

    public StockSource(Context context, SharedPreferences settings) {
        super(context, settings, null);
        mSourceName = TAG;
        mStockPhotoName = mResources.getString(R.string.stock_photo_album_name, "Default Photos");
        mImageList = new ArrayList<ImageData>(PHOTOS.length);
        mAlbumList = new ArrayList<AlbumData>(1);
        fillQueue();
    }

    @Override
    public Collection<AlbumData> findAlbums() {
        if (mAlbumList.isEmpty()) {
            AlbumData data = new AlbumData();
            data.id = ALBUM_ID;
            data.account = mStockPhotoName;
            data.title = mStockPhotoName;
            mAlbumList.add(data);
        }
        log(TAG, "returning a list of albums: " + mAlbumList.size());
        return mAlbumList;
    }

    @Override
    protected Collection<ImageData> findImages(int howMany) {
        if (mImageList.isEmpty()) {
            for (int i = 0; i < PHOTOS.length; i++) {
                ImageData data = new ImageData();
                data.id = Integer.toString(PHOTOS[i]);
                mImageList.add(data);
            }
        }
        return mImageList;
    }

    @Override
    protected InputStream getStream(ImageData data, int longSide) {
        InputStream is = null;
        try {
            log(TAG, "opening:" + data.id);
            is = mResources.openRawResource(Integer.valueOf(data.id));
        } catch (Exception ex) {
            log(TAG, ex.toString());
            is = null;
        }

        return is;
    }
}

