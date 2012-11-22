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

package com.android.photoeditor.filters;

import android.graphics.Matrix;

import com.android.photoeditor.Photo;

/**
 * Rotate filter applied to the image.
 */
public class RotateFilter extends Filter {

    private final Matrix matrix = new Matrix();

    public void setAngle(float degrees) {
        matrix.setRotate(degrees);
        validate();
    }

    @Override
    public void process(Photo src, Photo dst) {
        ImageUtils.nativeCopy(src.bitmap(), dst.bitmap());
        dst.transform(matrix);
    }
}
