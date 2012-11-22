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

extern "C" JNIEXPORT void JNICALL Java_com_android_photoeditor_filters_ImageUtils_nativeBacklight(
    JNIEnv *env, jobject obj, jobject src_bitmap, jobject dst_bitmap, jfloat backlight) {
   pBacklightType f = (pBacklightType)JNIFunc[JNI_Backlight].func_ptr;
   return f(env, obj, src_bitmap, dst_bitmap, backlight);
}

extern "C" void Backlight(JNIEnv *env, jobject obj, jobject src_bitmap, jobject dst_bitmap, jfloat backlight) {
  AndroidBitmapInfo src_info;
  AndroidBitmapInfo dst_info;
  void* src_pixels;
  void* dst_pixels;

  int ret = LockBitmaps(
      env, src_bitmap, dst_bitmap, &src_info, &dst_info, &src_pixels, &dst_pixels);
  if (ret < 0) {
    LOGE("LockBitmaps in Backlight failed, error=%d", ret);
    return;
  }

  const float fade_gamma = 0.3f;
  const float amt = 1.0f - backlight;
  const float mult = 1.0f / (amt * 0.7f + 0.3f);
  const float faded = fade_gamma + (1.0f - fade_gamma) * mult;
  const float igamma = 1.0f / faded;

  int lookup[256];
  for (int i = 0; i < 256; i++) {
    float value = static_cast<float>(pow(mult * i / 255.0f, igamma));
    if (value > 256.0f) {
      value = 256.0f;
    }
    lookup[i] = floor(255.0f * value) - i;
  }

  for (uint32_t scan_line = 0; scan_line < dst_info.height; scan_line++) {
    uint32_t* dst = reinterpret_cast<uint32_t*>(dst_pixels);
    pixel32_t* src = reinterpret_cast<pixel32_t*>(src_pixels);
    pixel32_t* src_line_end = src + src_info.width;
    while (src < src_line_end) {
      int32_t src_red = src->rgba8[0];
      int32_t src_green = src->rgba8[1];
      int32_t src_blue = src->rgba8[2];
      int32_t src_alpha = src->rgba8[3];

      int32_t lightmask = (src_red + src_green * 2 + src_blue) >> 2;
      int32_t backmask = 256 * (255 - lightmask);

      int32_t diff_red = lookup[src_red] * backmask;
      int32_t diff_green = lookup[src_green] * backmask;
      int32_t diff_blue = lookup[src_blue] * backmask;

      int32_t dst_red = src_red + (diff_red >> 16);
      int32_t dst_green = src_green + (diff_green >> 16);
      int32_t dst_blue = src_blue + (diff_blue >> 16);

      if (dst_red > 255) {
        dst_red = 255;
      }
      if (dst_green > 255) {
        dst_green = 255;
      }
      if (dst_blue > 255) {
        dst_blue = 255;
      }

      *dst = (src_alpha << 24) | (dst_blue << 16) | (dst_green << 8) | dst_red;
      dst++;
      src++;
    }
    dst_pixels = reinterpret_cast<char*>(dst_pixels) + dst_info.stride;
    src_pixels = reinterpret_cast<char*>(src_pixels) + src_info.stride;
  }

  UnlockBitmaps(env, src_bitmap, dst_bitmap);
}

}  // namespace
