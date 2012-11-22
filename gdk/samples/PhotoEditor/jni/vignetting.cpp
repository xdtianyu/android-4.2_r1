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

#include <cmath>

#include "utils.h"
#include "_jni.h"

using android::apps::photoeditor::utils::LockBitmaps;
using android::apps::photoeditor::utils::pixel32_t;
using android::apps::photoeditor::utils::UnlockBitmaps;

namespace {

/**
 * @param range the position of the significant falloff of light (50% luminosity).
 */
extern "C" JNIEXPORT void JNICALL Java_com_android_photoeditor_filters_ImageUtils_nativeVignetting(
    JNIEnv *env, jobject obj, jobject src_bitmap, jobject dst_bitmap, jfloat range) {
   pVignettingType f = (pVignettingType)JNIFunc[JNI_Vignetting].func_ptr;
   return f(env, obj, src_bitmap, dst_bitmap, range);
}

extern "C" void Vignetting(
    JNIEnv *env, jobject obj, jobject src_bitmap, jobject dst_bitmap, jfloat range) {
  AndroidBitmapInfo src_info;
  AndroidBitmapInfo dst_info;
  void* src_pixels;
  void* dst_pixels;

  const float slope = 20.0f;   // how fast the light changes, the bigger the faster.
  range *= slope;
  const int kShiftBits = 10;
  const uint32_t shade = 0.85 * (1 << kShiftBits);   // the intensity of the shade
  const uint32_t shade_offset = (1 << kShiftBits) - shade;

  int ret = LockBitmaps(
      env, src_bitmap, dst_bitmap, &src_info, &dst_info, &src_pixels, &dst_pixels);
  if (ret < 0) {
    LOGE("LockBitmaps in Vignetting failed, error=%d", ret);
    return;
  }

  const uint32_t center_x = src_info.width / 2;
  const uint32_t center_y = src_info.height / 2;
  const uint32_t hypot = sqrt(center_x * center_x + center_y * center_y) / slope;

  for (uint32_t dst_y = 0; dst_y < dst_info.height; dst_y++) {
    uint32_t *dst = reinterpret_cast<uint32_t*>(
        reinterpret_cast<char*>(dst_pixels) + dst_info.stride * dst_y);
    pixel32_t *src = reinterpret_cast<pixel32_t*>(
        reinterpret_cast<char*>(src_pixels) + src_info.stride * dst_y);
    int dy = dst_y - center_y;
    for (uint32_t dst_x = 0; dst_x < dst_info.width; dst_x++) {
      int dx = dst_x - center_x;
      int dist_square = dx * dx + dy * dy;
      uint32_t luminousity = shade / (1 + exp(sqrt(dist_square) / hypot - range)) +
          shade_offset;
      uint32_t red = (luminousity * src->rgba8[0]) >> kShiftBits;
      uint32_t green = (luminousity * src->rgba8[1]) >> kShiftBits;
      uint32_t blue = (luminousity * src->rgba8[2]) >> kShiftBits;
      uint32_t alpha = src->rgba8[3];
      *dst = (alpha << 24) | (blue << 16) | (green << 8) | red;
      dst++;
      src++;
    }  // dst_x
  }  // dst_y

  UnlockBitmaps(env, src_bitmap, dst_bitmap);
}

}  // namespace
