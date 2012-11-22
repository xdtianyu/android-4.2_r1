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

package com.android.photoeditor.filters;

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;

import com.android.photoeditor.Photo;
import com.android.photoeditor.RectUtils;

/**
 * Straighten filter applied to the image.
 */
public class StraightenFilter extends Filter {

    private final RectF bounds = new RectF();
    private final Matrix matrix = new Matrix();
    private final Canvas canvas = new Canvas();
    private final Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);

    private float angle;

    public void setAngle(float degrees) {
        this.angle = degrees;
        validate();
    }

    @Override
    public void process(Photo src, Photo dst) {
        bounds.set(0, 0, src.width(), src.height());
        RectUtils.getStraightenMatrix(bounds, angle, matrix);
        matrix.mapRect(bounds);
        matrix.postTranslate((src.width() - bounds.width()) / 2,
                (src.height() - bounds.height()) / 2);
        canvas.setBitmap(dst.bitmap());
        canvas.drawBitmap(src.bitmap(), matrix, paint);
    }
}
