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
import com.android.photoeditor.R;
import com.android.photoeditor.filters.RedEyeFilter;

/**
 * An action handling red-eye removal.
 */
public class RedEyeAction extends FilterAction {

    public RedEyeAction(FilterStack filterStack, ViewGroup tools) {
        super(filterStack, tools, R.string.redeye_tooltip);
    }

    @Override
    public void onBegin() {
        final RedEyeFilter filter = new RedEyeFilter();

        touchView.setSingleTapListener(new TouchView.SingleTapListener() {

            @Override
            public void onSingleTap(float x, float y) {
                filter.addRedEyePosition(photoView.mapPhotoPoint(x, y));
                notifyFilterChanged(filter, true);
            }
        });
        touchView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onEnd() {
    }
}
