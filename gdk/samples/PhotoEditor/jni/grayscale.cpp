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

#include <android/bitmap.h>
#include <jni.h>

#include "utils.h"
#include "_jni.h"

using android::apps::photoeditor::utils::LockBitmaps;
using android::apps::photoeditor::utils::pixel32_t;
using android::apps::photoeditor::utils::UnlockBitmaps;

namespace {

extern "C" JNIEXPORT void JNICALL Java_com_android_photoeditor_filters_ImageUtils_nativeGrayscale(
    JNIEnv *env, jobject obj, jobject src_bitmap, jobject dst_bitmap, jfloat scale) {
   pGrayscaleType f = (pGrayscaleType)JNIFunc[JNI_Grayscale].func_ptr;
   return f(env, obj, src_bitmap, dst_bitmap, scale);
}

extern "C" void Grayscale(
    JNIEnv *env, jobject obj, jobject src_bitmap, jobject dst_bitmap, jfloat scale) {
  AndroidBitmapInfo src_info;
  AndroidBitmapInfo dst_info;
  void* src_pixels;
  void* dst_pixels;

  int ret = LockBitmaps(
      env, src_bitmap, dst_bitmap, &src_info, &dst_info, &src_pixels, &dst_pixels);
  if (ret < 0) {
    LOGE("LockBitmaps in GrayScale failed, error=%d", ret);
    return;
  }

  const int kShiftBits = 20;
  const int32_t kRedRatio = static_cast<int32_t>((1 << kShiftBits) * 0.21f);
  const int32_t kGreenRatio = static_cast<int32_t>((1 << kShiftBits) * 0.71f);
  const int32_t kBlueRatio = static_cast<int32_t>((1 << kShiftBits) * 0.07f);

  for (uint32_t scan_line = 0; scan_line < dst_info.height; scan_line++) {
    uint32_t* dst = reinterpret_cast<uint32_t*>(dst_pixels);
    pixel32_t* src = reinterpret_cast<pixel32_t*>(src_pixels);
    pixel32_t* src_line_end = src + src_info.width;
    while (src < src_line_end) {
      int32_t src_red = src->rgba8[0];
      int32_t src_green = src->rgba8[1];
      int32_t src_blue = src->rgba8[2];
      int32_t src_alpha = src->rgba8[3];

      int32_t dst_color = (kRedRatio * src_red + kGreenRatio * src_green +
          kBlueRatio * src_blue) >> kShiftBits;
      if (dst_color > 255) {
        dst_color = 255;
      }
      *dst = (src_alpha << 24) | (dst_color << 16) | (dst_color << 8) | dst_color;
      dst++;
      src++;
    }
    dst_pixels = reinterpret_cast<char*>(dst_pixels) + dst_info.stride;
    src_pixels = reinterpret_cast<char*>(src_pixels) + src_info.stride;
  }

  UnlockBitmaps(env, src_bitmap, dst_bitmap);
}

}  // namespace
