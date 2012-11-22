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
import android.view.View;
import android.view.ViewGroup;

import com.android.photoeditor.FilterStack;
import com.android.photoeditor.R;
import com.android.photoeditor.filters.DoodleFilter;

/**
 * An action handling doodle effect.
 */
public class DoodleAction extends FilterAction {

    private static final int DEFAULT_COLOR_INDEX = 4;

    private DoodleFilter filter;

    public DoodleAction(FilterStack filterStack, ViewGroup tools) {
        super(filterStack, tools, R.string.doodle_tooltip);
    }

    @Override
    public void onBegin() {
        filter = new DoodleFilter();

        colorWheel.setOnColorChangeListener(new ColorWheel.OnColorChangeListener() {

            @Override
            public void onColorChanged(int color, boolean fromUser){
                if (fromUser) {
                    doodleView.startPath(color);
                    filter.addPath(color);
                }
            }
        });
        colorWheel.setColorIndex(DEFAULT_COLOR_INDEX);
        colorWheel.setVisibility(View.VISIBLE);

        // Directly draw on doodle-view instead of waiting for top-filter output callback.
        doodleView.setOnDoodleChangeListener(new DoodleView.OnDoodleChangeListener() {

            private final Path transformPath = new Path();

            @Override
            public void onLastPathChanged(Path path) {
                photoView.mapPhotoPath(path, transformPath);
                filter.updateLastPath(transformPath);
                notifyFilterChanged(filter, false);
            }
        });
        doodleView.clear();
        doodleView.clipBounds(photoView.getPhotoDisplayBounds());
        doodleView.setVisibility(View.VISIBLE);

        int color = colorWheel.getColor();
        doodleView.startPath(color);

        filter.addPath(color);
    }

    @Override
    public void onEnd() {
        notifyFilterChanged(filter, true);
    }
}
