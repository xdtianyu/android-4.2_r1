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

const int k256Multiply255 = 65280;

void BenSaturate(AndroidBitmapInfo *src_info, AndroidBitmapInfo *dst_info,
    void *src_pixels, void *dst_pixels, int scale) {
  for (uint32_t scan_line = 0; scan_line < dst_info->height; scan_line++) {
    uint32_t* dst = reinterpret_cast<uint32_t*>(dst_pixels);
    pixel32_t* src = reinterpret_cast<pixel32_t*>(src_pixels);
    pixel32_t* src_end = src + src_info->width;

    while (src < src_end) {
      int kv = (src->rgba8[0] + src->rgba8[0] + src->rgba8[1] * 5 + src->rgba8[2] + 4) >> 3;

      int dst_red = kv + (((src->rgba8[0] - kv) * scale) >> 8);
      int dst_green = kv + (((src->rgba8[1] - kv) * scale) >> 8);
      int dst_blue = kv + (((src->rgba8[2] - kv) * scale) >> 8);

      *dst = (src->rgba8[3] << 24) | (dst_blue << 16) | (dst_green << 8) | dst_red;

      dst++;
      src++;
    }
    dst_pixels = reinterpret_cast<char*>(dst_pixels) + dst_info->stride;
    src_pixels = reinterpret_cast<char*>(src_pixels) + src_info->stride;
  }
}

void HerfSaturate(AndroidBitmapInfo *src_info, AndroidBitmapInfo *dst_info,
      void *src_pixels, void *dst_pixels, float scale) {
  const float kMapScale = 8.0;
  const int kMapSize = 2048;
  int rmap[kMapSize];
  int gmap[kMapSize];
  int bmap[kMapSize];

  const float kR = 0.3;
  const float kG = 0.7;
  const float kB = 0.9;
  const float amtR = (kR * scale) + 1;
  const float amtG = (kG * scale) + 1;
  const float amtB = (kB * scale) + 1;

  for (int i = 0; i < kMapSize; i++) {
      float inv = kMapScale * static_cast<float>(i) / kMapSize;
      rmap[i] = static_cast<int>(pow(inv, amtR) * 256);
      gmap[i] = static_cast<int>(pow(inv, amtG) * 256);
      bmap[i] = static_cast<int>(pow(inv, amtB) * 256);
  }

  int kMapFactor256 = static_cast<int>(kMapSize / kMapScale) * 256;
  for (uint32_t scan_line = 0; scan_line < dst_info->height; scan_line++) {
    uint32_t* dst = reinterpret_cast<uint32_t*>(dst_pixels);
    pixel32_t* src = reinterpret_cast<pixel32_t*>(src_pixels);
    pixel32_t* src_end = src + src_info->width;

    while (src < src_end) {
      int de = (src->rgba8[0] + src->rgba8[0] + src->rgba8[1] * 5 + src->rgba8[2]) >> 3;
      if (!de) {
          *dst = src->rgba8[3] << 24;
      } else {
        int invde = (kMapFactor256 - 1) / de;
        int r = invde * src->rgba8[0] >> 8;
        int g = invde * src->rgba8[1] >> 8;
        int b = invde * src->rgba8[2] >> 8;

        int dst_red = de * rmap[r] >> 8;
        int dst_green = de * gmap[g] >> 8;
        int dst_blue = de * bmap[b] >> 8;

        int rgb_max = MAX3(dst_red, dst_green, dst_blue);
        if (rgb_max > 255) {
          int invmax = k256Multiply255 / rgb_max;
          dst_red = dst_red * invmax >> 8;
          dst_green = dst_green * invmax >> 8;
          dst_blue = dst_blue * invmax >> 8;
        }

        *dst = (src->rgba8[3] << 24) | (dst_blue << 16) | (dst_green << 8) | dst_red;
      }
      dst++;
      src++;
    }
    dst_pixels = reinterpret_cast<char*>(dst_pixels) + dst_info->stride;
    src_pixels = reinterpret_cast<char*>(src_pixels) + src_info->stride;
  }
}

/*
 * @param scale ranges from -1 to 1.
 */
extern "C" JNIEXPORT void JNICALL Java_com_android_photoeditor_filters_ImageUtils_nativeSaturation(
    JNIEnv *env, jobject obj, jobject src_bitmap, jobject dst_bitmap, jfloat scale) {
   pSaturationType f = (pSaturationType)JNIFunc[JNI_Saturation].func_ptr;
   return f(env, obj, src_bitmap, dst_bitmap, scale);
}
   
extern "C" void Saturation(
    JNIEnv *env, jobject obj, jobject src_bitmap, jobject dst_bitmap, jfloat scale) {
  AndroidBitmapInfo src_info;
  AndroidBitmapInfo dst_info;
  void* src_pixels;
  void* dst_pixels;

  int ret = LockBitmaps(
      env, src_bitmap, dst_bitmap, &src_info, &dst_info, &src_pixels, &dst_pixels);
  if (ret < 0) {
    LOGE("LockBitmaps in Saturation failed, error=%d", ret);
    return;
  }

  if (scale >= 0) {
    HerfSaturate(&src_info, &dst_info, src_pixels, dst_pixels, scale * 3);
  } else {
    BenSaturate(&src_info, &dst_info, src_pixels, dst_pixels,
        static_cast<int>((1 + scale) * 256));
  }

  UnlockBitmaps(env, src_bitmap, dst_bitmap);
}

}  // namespace
