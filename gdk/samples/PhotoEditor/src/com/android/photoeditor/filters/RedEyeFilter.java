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

import android.graphics.PointF;

import com.android.photoeditor.Photo;

import java.util.Vector;

/**
 * Red-eye removal filter applied to the image.
 */
public class RedEyeFilter extends Filter {

    private static final float RADIUS_RATIO = 0.06f;
    private static final float MIN_RADIUS = 10.0f;
    private static final float DEFAULT_RED_INTENSITY = 1.30f; // an empirical value

    private final Vector<PointF> redeyePositions = new Vector<PointF>();

    public void addRedEyePosition(PointF point) {
        redeyePositions.add(point);
        validate();
    }

    @Override
    public void process(Photo src, Photo dst) {
        float radius = Math.max(MIN_RADIUS, RADIUS_RATIO * Math.min(src.width(), src.height()));

        PointF[] a = new PointF[redeyePositions.size()];
        ImageUtils.nativeRedEye(src.bitmap(), dst.bitmap(),
                redeyePositions.toArray(a), radius, DEFAULT_RED_INTENSITY);
    }
}
