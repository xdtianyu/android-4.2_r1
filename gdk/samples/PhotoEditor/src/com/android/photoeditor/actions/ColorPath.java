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

package com.android.photoeditor.actions;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;

/**
 * Colored path that could be painted on canvas.
 */
public class ColorPath {

    private final int color;
    private final Path path;

    public ColorPath(int color, Path path) {
        this.color = Color.argb(192, Color.red(color), Color.green(color), Color.blue(color));
        this.path = path;
    }

    public Path path() {
        return path;
    }

    public void draw(Canvas canvas, Paint paint) {
        paint.setColor(color);
        canvas.drawPath(path, paint);
    }

    /**
     * Creates a paint to draw color paths.
     */
    public static Paint createPaint() {
        Paint paint = new Paint(Paint.DITHER_FLAG | Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(12);
        return paint;
    }
}
