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

import android.graphics.Matrix;
import android.graphics.RectF;
import android.view.View;
import android.view.ViewGroup;

import com.android.photoeditor.FilterStack;
import com.android.photoeditor.R;
import com.android.photoeditor.RectUtils;
import com.android.photoeditor.filters.RotateFilter;

/**
 * An action handling rotate effect.
 */
public class RotateAction extends FilterAction {

    private static final float DEFAULT_ANGLE = 0.0f;
    private static final float DEFAULT_ROTATE_SPAN = 360.0f;

    private RotateFilter filter;
    private float rotateDegrees;

    public RotateAction(FilterStack filterStack, ViewGroup tools) {
        super(filterStack, tools, R.string.rotate_tooltip);
    }

    @Override
    public void onBegin() {
        filter = new RotateFilter();
        rotateDegrees = 0;

        final Matrix matrix = new Matrix();
        final RectF rotateBounds = new RectF();
        final RectF photoBounds = photoView.getPhotoBounds();

        // Directly transform photo-view instead of waiting for top-filter output callback.
        rotateView.setOnAngleChangeListener(new RotateView.OnRotateChangeListener() {

            @Override
            public void onAngleChanged(float degrees, boolean fromUser){
                if (fromUser) {
                    rotateDegrees = degrees;
                    filter.setAngle(degrees);
                    notifyFilterChanged(filter, false);
                    transformPhotoView(degrees);
                }
            }

            @Override
            public void onStartTrackingTouch() {
                // no-op
            }

            @Override
            public void onStopTrackingTouch() {
                if (roundFilterRotationDegrees()) {
                    notifyFilterChanged(filter, false);
                    transformPhotoView(rotateDegrees);
                    rotateView.setRotatedAngle(rotateDegrees);
                }
            }

            private void transformPhotoView(float degrees) {
                matrix.reset();
                rotateBounds.set(photoBounds);
                RectUtils.postRotateMatrix(degrees, rotateBounds, matrix);
                float scale = RectUtils.getDisplayScale(rotateBounds, photoView);
                matrix.postScale(scale, scale);
                photoView.transformDisplay(matrix);
            }
        });
        rotateView.setGridBounds(null);
        rotateView.setRotatedAngle(DEFAULT_ANGLE);
        rotateView.setRotateSpan(DEFAULT_ROTATE_SPAN);
        rotateView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onEnd() {
        // Round the current rotation degrees in case rotation tracking has not stopped yet.
        roundFilterRotationDegrees();
        notifyFilterChanged(filter, true);
    }

    /**
     * Rounds filter rotation degrees to multiples of 90 degrees.
     *
     * @return true if the rotation degrees has been changed.
     */
    private boolean roundFilterRotationDegrees() {
        if (rotateDegrees % 90 != 0) {
            rotateDegrees = Math.round(rotateDegrees / 90) * 90;
            filter.setAngle(rotateDegrees);
            return true;
        }
        return false;
    }
}
