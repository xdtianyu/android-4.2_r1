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
import android.database.Cursor;
import android.provider.MediaStore;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;

/**
 * Loads images from the local store.
 */
public class LocalSource extends PhotoSource {
    private static final String TAG = "PhotoTable.LocalSource";

    private final String mUnknownAlbumName;
    private final String mLocalSourceName;
    private Set<String> mFoundAlbumIds;
    private int mNextPosition;

    public LocalSource(Context context, SharedPreferences settings) {
        super(context, settings);
        mLocalSourceName = mResources.getString(R.string.local_source_name, "Photos on Device");
        mUnknownAlbumName = mResources.getString(R.string.unknown_album_name, "Unknown");
        mSourceName = TAG;
        mNextPosition = -1;
        fillQueue();
    }

    private Set<String> getFoundAlbums() {
        if (mFoundAlbumIds == null) {
            findAlbums();
        }
        return mFoundAlbumIds;
    }

    @Override
    public Collection<AlbumData> findAlbums() {
        log(TAG, "finding albums");
        HashMap<String, AlbumData> foundAlbums = new HashMap<String, AlbumData>();

        String[] projection = {MediaStore.Images.Media.DATA, MediaStore.Images.Media.BUCKET_ID,
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME, MediaStore.Images.Media.DATE_TAKEN};
        // This is a horrible hack that closes the where clause and injects a grouping clause.
        Cursor cursor = mResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();

            int dataIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
            int bucketIndex = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_ID);
            int nameIndex = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME);
            int updatedIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN);

            if (bucketIndex < 0) {
                log(TAG, "can't find the ID column!");
            } else {
                while (!cursor.isAfterLast()) {
                    String id = TAG + ":" + cursor.getString(bucketIndex);
                    AlbumData data = foundAlbums.get(id);
                    if (foundAlbums.get(id) == null) {
                        data = new AlbumData();
                        data.id = id;
                        data.account = mLocalSourceName;

                        if (dataIndex >= 0) {
                            data.thumbnailUrl = cursor.getString(dataIndex);
                        }

                        if (nameIndex >= 0) {
                            data.title = cursor.getString(nameIndex);
                        } else {
                            data.title = mUnknownAlbumName;
                        }

                        log(TAG, data.title + " found");
                        foundAlbums.put(id, data);
                    }
                    if (updatedIndex >= 0) {
                        long updated = cursor.getLong(updatedIndex);
                        data.updated = (data.updated == 0 ?
                                        updated :
                                        Math.min(data.updated, updated));
                    }
                    cursor.moveToNext();
                }
            }
            cursor.close();

        }
        log(TAG, "found " + foundAlbums.size() + " items.");
        mFoundAlbumIds = foundAlbums.keySet();
        return foundAlbums.values();
    }

    @Override
    protected Collection<ImageData> findImages(int howMany) {
        log(TAG, "finding images");
        LinkedList<ImageData> foundImages = new LinkedList<ImageData>();

        String[] projection = {MediaStore.Images.Media.DATA, MediaStore.Images.Media.ORIENTATION,
                MediaStore.Images.Media.BUCKET_ID, MediaStore.Images.Media.BUCKET_DISPLAY_NAME};
        String selection = "";
        for (String id : getFoundAlbums()) {
            if (mSettings.isAlbumEnabled(id)) {
                String[] parts = id.split(":");
                if (parts.length > 1) {
                    if (selection.length() > 0) {
                        selection += " OR ";
                    }
                    selection += MediaStore.Images.Media.BUCKET_ID + " = '" + parts[1] + "'";
                }
            }
        }
        if (selection.isEmpty()) {
            return foundImages;
        }

        Cursor cursor = mResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection, selection, null, null);
        if (cursor != null) {
            if (cursor.getCount() > howMany && mNextPosition == -1) {
                mNextPosition = mRNG.nextInt() % (cursor.getCount() - howMany);
            }
            if (mNextPosition == -1) {
                mNextPosition = 0;
            }
            cursor.moveToPosition(mNextPosition);

            int dataIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
            int orientationIndex = cursor.getColumnIndex(MediaStore.Images.Media.ORIENTATION);
            int bucketIndex = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_ID);
            int nameIndex = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME);

            if (dataIndex < 0) {
                log(TAG, "can't find the DATA column!");
            } else {
                while (foundImages.size() < howMany && !cursor.isAfterLast()) {
                    ImageData data = new ImageData();
                    data.url = cursor.getString(dataIndex);
                    data.orientation = cursor.getInt(orientationIndex);
                    foundImages.offer(data);
                    if (cursor.moveToNext()) {
                        mNextPosition++;
                    }
                }
                if (cursor.isAfterLast()) {
                    mNextPosition = 0;
                }
            }

            cursor.close();
        }
        log(TAG, "found " + foundImages.size() + " items.");
        return foundImages;
    }

    @Override
    protected InputStream getStream(ImageData data, int longSide) {
        FileInputStream fis = null;
        try {
            log(TAG, "opening:" + data.url);
            fis = new FileInputStream(data.url);
        } catch (Exception ex) {
            log(TAG, ex.toString());
            fis = null;
        }

        return (InputStream) fis;
    }
}
