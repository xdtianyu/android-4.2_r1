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

import android.graphics.Bitmap;
import android.graphics.PointF;


/**
 * Image utilities that calls to JNI methods for image processing.
 */
public class ImageUtils {

    static {
        System.loadLibrary("jni_photoeditor");
    }
  
    public static native boolean gdk();
    public static native boolean init(byte[] pgm, int pgmLength);
   
    public static native void nativeBacklight(Bitmap src, Bitmap dst, float backlight);
    public static native void nativeBlur(Bitmap src, Bitmap dst, float scale);
    public static native void nativeColorTemp(Bitmap src, Bitmap dst, float scale);
    public static native void nativeCopy(Bitmap src, Bitmap dst);
    public static native void nativeCrossProcess(Bitmap src, Bitmap dst);
    public static native void nativeDuotone(Bitmap src, Bitmap dst, int firstColor,
            int secondColor);
    public static native void nativeFisheye(Bitmap src, Bitmap dst, float focusX,
            float focusY, float scale);
    public static native void nativeFlipBoth(Bitmap src, Bitmap dst);
    public static native void nativeFlipHorizontal(Bitmap src, Bitmap dst);
    public static native void nativeFlipVertical(Bitmap src, Bitmap dst);
    public static native void nativeGrain(Bitmap src, Bitmap dst, float scale);
    public static native void nativeGrayscale(Bitmap src, Bitmap dst, float scale);
    public static native void nativeHEQ(Bitmap src, Bitmap dst, float scale);
    public static native void nativeNegative(Bitmap src, Bitmap dst);
    public static native void nativeQuantize(Bitmap src, Bitmap dst);
    public static native void nativeRedEye(Bitmap src, Bitmap dst, PointF[] redeyes,
            float radius, float intensity);
    public static native void nativeSaturation(Bitmap src, Bitmap dst, float scale);
    public static native void nativeSepia(Bitmap src, Bitmap dst);
    public static native void nativeSharpen(Bitmap src, Bitmap dst, float scale);
    public static native void nativeTint(Bitmap src, Bitmap dst, int tint);
    public static native void nativeVignetting(Bitmap src, Bitmap dst, float range);
    public static native void nativeWarmify(Bitmap src, Bitmap dst);
    public static native void nativeWhiteBlack(Bitmap src, Bitmap dst, float white, float black);
}
