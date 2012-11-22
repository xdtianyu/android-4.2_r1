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

import android.view.View;
import android.view.ViewGroup;

import com.android.photoeditor.FilterStack;
import com.android.photoeditor.filters.GrainFilter;

/**
 * An action handling the film-grain effect.
 */
public class GrainAction extends FilterAction {

    private static final float DEFAULT_SCALE = 0.5f;

    public GrainAction(FilterStack filterStack, ViewGroup tools) {
        super(filterStack, tools);
    }

    @Override
    public void onBegin() {
        final GrainFilter filter = new GrainFilter();

        scaleWheel.setOnScaleChangeListener(new ScaleWheel.OnScaleChangeListener() {

            @Override
            public void onProgressChanged(float progress, boolean fromUser) {
                if (fromUser) {
                    filter.setScale(progress);
                    notifyFilterChanged(filter, true);
                }
            }
        });
        scaleWheel.setProgress(DEFAULT_SCALE);
        scaleWheel.setVisibility(View.VISIBLE);

        filter.setScale(DEFAULT_SCALE);
        notifyFilterChanged(filter, true);
    }

    @Override
    public void onEnd() {
    }
}
