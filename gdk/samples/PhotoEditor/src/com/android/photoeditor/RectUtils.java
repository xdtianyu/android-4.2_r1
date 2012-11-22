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

import android.graphics.Matrix;
import android.graphics.RectF;
import android.view.View;

/**
 * Utils for rectangles/bounds related calculations.
 */
public class RectUtils {

    private static final float MATH_PI = (float) Math.PI;
    private static final float DEGREES_TO_RADIAN = MATH_PI / 180.0f;

    /**
     * Gets straighten matrix for the given bounds and degrees.
     */
    public static void getStraightenMatrix(RectF bounds, float degrees, Matrix matrix) {
        matrix.reset();
        if ((degrees != 0) && !bounds.isEmpty()) {
            float w = bounds.width() / 2;
            float h = bounds.height() / 2;
            float adjustAngle;
            if ((degrees < 0 && w > h) || (degrees > 0 && w <= h)) {
                // The top left point is the boundary.
                adjustAngle = (float) Math.atan(h / -w) + MATH_PI + degrees * DEGREES_TO_RADIAN;
            } else {
                // The top right point is the boundary.
                adjustAngle = (float) Math.atan(h / w) - MATH_PI + degrees * DEGREES_TO_RADIAN;
            }
            float radius = (float) Math.hypot(w, h);
            float scaleX = (float) Math.abs(radius * Math.cos(adjustAngle)) / w;
            float scaleY = (float) Math.abs(radius * Math.sin(adjustAngle)) / h;
            float scale = Math.max(scaleX, scaleY);

            postRotateMatrix(degrees, new RectF(bounds), matrix);
            matrix.postScale(scale, scale);
        }
    }

    /**
     * Post rotates the matrix and bounds for the given bounds and degrees.
     */
    public static void postRotateMatrix(float degrees, RectF bounds, Matrix matrix) {
        matrix.postRotate(degrees);
        matrix.mapRect(bounds);
        matrix.postTranslate(-bounds.left, -bounds.top);
    }

    /**
     * Post translates the matrix to center the given bounds inside the view.
     */
    public static void postCenterMatrix(RectF contentBounds, View view, Matrix matrix) {
        matrix.postTranslate((view.getWidth() - contentBounds.width()) / 2,
                (view.getHeight() - contentBounds.height()) / 2);
    }

    /**
     * Gets the proper scale value that scales down the content and keeps its aspect ratio to
     * display inside the view.
     */
    public static float getDisplayScale(RectF contentBounds, View view) {
        if (contentBounds.isEmpty()) {
            return 1;
        }

        float scale = Math.min(view.getWidth() / contentBounds.width(),
                view.getHeight() / contentBounds.height());
        // Avoid scaling up the content.
        return Math.min(scale, 1);
    }
}
