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
import android.graphics.Paint;
import android.graphics.Path;

import com.android.photoeditor.actions.ColorPath;
import com.android.photoeditor.Photo;

import java.util.Vector;

/**
 * Doodle filter applied to the image.
 */
public class DoodleFilter extends Filter {

    private final Vector<ColorPath> doodles = new Vector<ColorPath>();
    private final Paint paint = ColorPath.createPaint();
    private final Canvas canvas = new Canvas();

    public void addPath(int color) {
        // Remove last empty path before adding a new one.
        if (!doodles.isEmpty() && doodles.lastElement().path().isEmpty()) {
            doodles.remove(doodles.size() - 1);
        }
        doodles.add(new ColorPath(color, new Path()));
    }

    public synchronized void updateLastPath(Path path) {
        if (!doodles.isEmpty()) {
            doodles.lastElement().path().set(path);
            validate();
        }
    }

    @Override
    public void process(Photo src, Photo dst) {
        ImageUtils.nativeCopy(src.bitmap(), dst.bitmap());

        if (!doodles.isEmpty()) {
            canvas.setBitmap(dst.bitmap());
            for (ColorPath doodle : doodles) {
                doodle.draw(canvas, paint);
            }
        }
    }
}
