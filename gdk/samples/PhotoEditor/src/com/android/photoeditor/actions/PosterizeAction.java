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

import android.view.ViewGroup;

import com.android.photoeditor.FilterStack;
import com.android.photoeditor.filters.PosterizeFilter;

/**
 * An action handling the "Posterize" effect.
 */
public class PosterizeAction extends FilterAction {

    public PosterizeAction(FilterStack filterStack, ViewGroup tools) {
        super(filterStack, tools);
    }

    @Override
    public void onBegin() {
        notifyFilterChanged(new PosterizeFilter(), true);
        end();
    }

    @Override
    public void onEnd() {
    }
}
