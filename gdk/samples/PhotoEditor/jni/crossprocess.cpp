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

extern "C" JNIEXPORT void JNICALL Java_com_android_photoeditor_filters_ImageUtils_nativeCrossProcess(
    JNIEnv *env, jobject obj, jobject src_bitmap, jobject dst_bitmap) {
   pCrossProcessType f = (pCrossProcessType)JNIFunc[JNI_CrossProcess].func_ptr;
   return f(env, obj, src_bitmap, dst_bitmap);
}

extern "C" void CrossProcess(
    JNIEnv *env, jobject obj, jobject src_bitmap, jobject dst_bitmap) {
  AndroidBitmapInfo src_info;
  AndroidBitmapInfo dst_info;
  void* src_pixels;
  void* dst_pixels;

  int ret = LockBitmaps(
      env, src_bitmap, dst_bitmap, &src_info, &dst_info, &src_pixels, &dst_pixels);
  if (ret < 0) {
    LOGE("LockBitmaps in Crossprocess failed, error=%d", ret);
    return;
  }

  const int k128SquareDouble = 32768;  // 2 * 128^2
  const int k128CubicDouble = 4194304;  // 2 * 128^3
  for (uint32_t scan_line = 0; scan_line < dst_info.height; scan_line++) {
    uint32_t* dst = reinterpret_cast<uint32_t*>(dst_pixels);
    pixel32_t* src = reinterpret_cast<pixel32_t*>(src_pixels);
    pixel32_t* src_line_end = src + src_info.width;

    while (src < src_line_end) {
      // Enhance red contrast
      uint32_t r = src->rgba8[0];
      uint32_t dst_red;
      if (r <= 128) {
        dst_red = 255 * r * r * r / k128CubicDouble;
      } else {
        uint32_t diff_r = 255 - r;
        dst_red = 255 - 255 * diff_r * diff_r * diff_r / k128CubicDouble;
      }
      // Enhance green contrast
      int g = src->rgba8[1];
      uint32_t dst_green;
      if (g <= 128) {
        dst_green = 255 * g * g / k128SquareDouble;
      } else {
        int diff_g = 255 - g;
        dst_green = 255 - 255 * diff_g * diff_g / k128SquareDouble;
      }
      // Narrow the blue channel, from 64 to 192.
      int dst_blue = src->rgba8[2] * 128 / 255 + 64;

      *dst = (src->rgba8[3] << 24) | (dst_blue << 16) | (dst_green << 8) | dst_red;
      dst++;
      src++;
    }
    dst_pixels = reinterpret_cast<char*>(dst_pixels) + dst_info.stride;
    src_pixels = reinterpret_cast<char*>(src_pixels) + src_info.stride;
  }

  UnlockBitmaps(env, src_bitmap, dst_bitmap);
}

}  // namespace
