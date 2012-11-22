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

import android.view.View;

/**
 * Handler that adjusts layouts of toolbar's child views whose positions need being calculated
 * according to the current screen dimensions.
 */
class ToolbarLayoutHandler {

    private final View tools;

    /**
     * Constructor should only be invoked after toolbar has done inflation and added all its child
     * views; then its child views could be found by findViewById calls.
     */
    public ToolbarLayoutHandler(View toolbar) {
        this.tools = toolbar;
    }

    /**
     * Layouts child tool views' positions that need being updated when toolbar is being layout.
     */
    public void extraLayout(int left, int top, int right, int bottom) {
        // Wheels need being centered vertically.
        int height = bottom - top;

        View scaleWheel = tools.findViewById(R.id.scale_wheel);
        scaleWheel.offsetTopAndBottom((height - scaleWheel.getHeight()) / 2);

        View colorWheel = tools.findViewById(R.id.color_wheel);
        colorWheel.offsetTopAndBottom((height - colorWheel.getHeight()) / 2);
    }
}
