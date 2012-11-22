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
import com.android.photoeditor.filters.StraightenFilter;

/**
 * An action handling straighten effect.
 */
public class StraightenAction extends FilterAction {

    private static final float DEFAULT_ANGLE = 0.0f;
    private static final float DEFAULT_ROTATE_SPAN = 60.0f;

    private StraightenFilter filter;

    public StraightenAction(FilterStack filterStack, ViewGroup tools) {
        super(filterStack, tools, R.string.straighten_tooltip);
    }

    @Override
    public void onBegin() {
        filter = new StraightenFilter();

        final Matrix matrix = new Matrix();
        final RectF photoBounds = photoView.getPhotoBounds();
        final float displayScale = RectUtils.getDisplayScale(photoBounds, photoView);

        RectF displayBounds = photoView.getPhotoDisplayBounds();
        photoView.clipPhoto(displayBounds);

        // Directly transform photo-view instead of waiting for top-filter output callback.
        rotateView.setOnAngleChangeListener(new RotateView.OnRotateChangeListener() {

            @Override
            public void onAngleChanged(float angle, boolean fromUser){
                if (fromUser) {
                    filter.setAngle(angle);
                    notifyFilterChanged(filter, false);
                    RectUtils.getStraightenMatrix(photoBounds, angle, matrix);
                    matrix.postScale(displayScale, displayScale);
                    photoView.transformDisplay(matrix);
                }
            }

            @Override
            public void onStartTrackingTouch() {
                // no-op
            }

            @Override
            public void onStopTrackingTouch() {
                // no-op
            }
        });
        rotateView.setGridBounds(displayBounds);
        rotateView.setRotatedAngle(DEFAULT_ANGLE);
        rotateView.setRotateSpan(DEFAULT_ROTATE_SPAN);
        rotateView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onEnd() {
        notifyFilterChanged(filter, true);
    }
}
