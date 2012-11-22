/*
 * Copyright (C) 2011 The Android Open Source Project
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
package com.android.chimpchat.adb;

import com.android.ddmlib.RawImage;
import com.android.chimpchat.adb.image.ImageUtils;
import com.android.chimpchat.core.ChimpImageBase;

import java.awt.image.BufferedImage;

/**
 * ADB implementation of the ChimpImage class.
 */
public class AdbChimpImage extends ChimpImageBase {
    private final RawImage image;

    /**
     * Create a new AdbMonkeyImage.
     *
     * @param image the image from adb.
     */
    AdbChimpImage(RawImage image) {
        this.image = image;
    }

    @Override
    public BufferedImage createBufferedImage() {
        return ImageUtils.convertImage(image);
    }

    public RawImage getRawImage() {
        return image;
    }
}
