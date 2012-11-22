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

import com.android.photoeditor.Photo;

/**
 * Flip filter applied to the image.
 */
public class FlipFilter extends Filter {

    private boolean flipHorizontal;
    private boolean flipVertical;

    public void setFlip(boolean flipHorizontal, boolean flipVertical) {
        this.flipHorizontal = flipHorizontal;
        this.flipVertical = flipVertical;
        validate();
    }

    @Override
    public void process(Photo src, Photo dst) {
        if (flipHorizontal && flipVertical) {
            ImageUtils.nativeFlipBoth(src.bitmap(), dst.bitmap());
        } else if (flipHorizontal) {
            ImageUtils.nativeFlipHorizontal(src.bitmap(), dst.bitmap());
        } else if (flipVertical){
            ImageUtils.nativeFlipVertical(src.bitmap(), dst.bitmap());
        } else {
            ImageUtils.nativeCopy(src.bitmap(), dst.bitmap());
        }
    }
}
