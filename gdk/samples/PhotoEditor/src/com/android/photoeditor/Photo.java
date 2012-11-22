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

package com.android.photoeditor;

import android.graphics.Bitmap;
import android.graphics.Matrix;

/**
 * Photo that is used for editing/display and should be synchronized for concurrent access.
 */
public class Photo {

    private Bitmap bitmap;

    /**
     * Factory method to ensure every Photo instance holds a non-null bitmap.
     */
    public static Photo create(Bitmap bitmap) {
        return (bitmap != null) ? new Photo(bitmap) : null;
    }

    private Photo(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    public Bitmap bitmap() {
        return bitmap;
    }

    public Photo copy(Bitmap.Config config) {
        Bitmap copy = bitmap.copy(config, true);
        return (copy != null) ? new Photo(copy) : null;
    }

    public boolean matchDimension(Photo photo) {
        return ((photo.width() == width()) && (photo.height() == height()));
    }

    public int width() {
        return bitmap.getWidth();
    }

    public int height() {
        return bitmap.getHeight();
    }

    public void transform(Matrix matrix) {
        // Copy immutable transformed photo; no-op if it fails to ensure bitmap isn't assigned null.
        Bitmap transformed = BitmapUtils.createBitmap(bitmap, matrix).copy(
                bitmap.getConfig(), true);
        if (transformed != null) {
            bitmap.recycle();
            bitmap = transformed;
        }
    }

    public void crop(int left, int top, int width, int height) {
        // Copy immutable cropped photo; no-op if it fails to ensure bitmap isn't assigned null.
        Bitmap cropped = Bitmap.createBitmap(bitmap, left, top, width, height).copy(
                bitmap.getConfig(), true);
        if (cropped != null) {
            bitmap.recycle();
            bitmap = cropped;
        }
    }

    /**
     * Recycles bitmaps; this instance should not be used after its clear() is called.
     */
    public void clear() {
        bitmap.recycle();
    }
}
