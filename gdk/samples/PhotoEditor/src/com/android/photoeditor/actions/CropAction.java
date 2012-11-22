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

import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;
import android.view.ViewGroup;

import com.android.photoeditor.FilterStack;
import com.android.photoeditor.R;
import com.android.photoeditor.filters.CropFilter;

/**
 * An action handling crop effect.
 */
public class CropAction extends FilterAction {

    private static final float DEFAULT_CROP = 0.2f;
    private CropFilter filter;

    public CropAction(FilterStack filterStack, ViewGroup tools) {
        super(filterStack, tools, R.string.crop_tooltip);
    }

    private RectF mapPhotoBounds(RectF bounds, RectF photoBounds) {
        return new RectF((bounds.left - photoBounds.left) / photoBounds.width(),
                (bounds.top - photoBounds.top) / photoBounds.height(),
                (bounds.right - photoBounds.left) / photoBounds.width(),
                (bounds.bottom - photoBounds.top) / photoBounds.height());
    }

    @Override
    public void onBegin() {
        filter = new CropFilter();

        final RectF photoBounds = photoView.getPhotoDisplayBounds();
        cropView.setPhotoBounds(new RectF(photoBounds));
        cropView.setOnCropChangeListener(new CropView.OnCropChangeListener() {

            @Override
            public void onCropChanged(RectF bounds, boolean fromUser) {
                if (fromUser) {
                    filter.setCropBounds(mapPhotoBounds(bounds, photoBounds));
                    notifyFilterChanged(filter, false);
                }
            }
        });
        RectF cropBounds = new RectF(photoBounds);
        cropBounds.inset(photoBounds.width() * DEFAULT_CROP, photoBounds.height() * DEFAULT_CROP);
        cropView.setCropBounds(cropBounds);
        if (!cropView.fullPhotoCropped()) {
            filter.setCropBounds(mapPhotoBounds(cropBounds, photoBounds));
            notifyFilterChanged(filter, false);
        }
        cropView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onEnd() {
        notifyFilterChanged(filter, true);
    }
}
