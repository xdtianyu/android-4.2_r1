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
import com.android.photoeditor.filters.TintFilter;

/**
 * An action handling tint effect.
 */
public class TintAction extends FilterAction {

    private static final int DEFAULT_COLOR_INDEX = 13;

    public TintAction(FilterStack filterStack, ViewGroup tools) {
        super(filterStack, tools);
    }

    @Override
    public void onBegin() {
        final TintFilter filter = new TintFilter();

        colorWheel.setOnColorChangeListener(new ColorWheel.OnColorChangeListener() {

            @Override
            public void onColorChanged(int color, boolean fromUser){
                if (fromUser) {
                    filter.setTint(color);
                    notifyFilterChanged(filter, true);
                }
            }
        });
        // Tint photo with the default color.
        colorWheel.setColorIndex(DEFAULT_COLOR_INDEX);
        colorWheel.setVisibility(View.VISIBLE);

        filter.setTint(colorWheel.getColor());
        notifyFilterChanged(filter, true);
    }

    @Override
    public void onEnd() {
    }
}
