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

extern "C" JNIEXPORT void JNICALL Java_com_android_photoeditor_filters_ImageUtils_nativeDuotone(
    JNIEnv *env, jobject obj, jobject src_bitmap, jobject dst_bitmap, jint color1, jint color2) {
   pDuotoneType f = (pDuotoneType)JNIFunc[JNI_Duotone].func_ptr;
   return f(env, obj, src_bitmap, dst_bitmap, color1, color2);
}
   
extern "C" void Duotone(
    JNIEnv *env, jobject obj, jobject src_bitmap, jobject dst_bitmap, jint color1, jint color2) {
  AndroidBitmapInfo src_info;
  AndroidBitmapInfo dst_info;
  void* src_pixels;
  void* dst_pixels;

  int ret = LockBitmaps(
      env, src_bitmap, dst_bitmap, &src_info, &dst_info, &src_pixels, &dst_pixels);
  if (ret < 0) {
    LOGE("LockBitmaps in Duotone failed, error=%d", ret);
    return;
  }

  uint8_t red1 = static_cast<uint8_t>(color1 >> 16) & 0xff;
  uint8_t green1 = static_cast<uint8_t>(color1 >> 8) & 0xff;
  uint8_t blue1 = static_cast<uint8_t>(color1) & 0xff;

  uint8_t red2 = (uint8_t)(color2 >> 16) & 0xff;
  uint8_t green2 = (uint8_t)(color2 >> 8) & 0xff;
  uint8_t blue2 = (uint8_t)color2 & 0xff;

  int r_delta = red2 - red1;
  int g_delta = green2 - green1;
  int b_delta = blue2 - blue1;

  const uint32_t kEnergyLevels = 255 * 3 + 1;
  uint8_t r_table[kEnergyLevels];
  uint8_t g_table[kEnergyLevels];
  uint8_t b_table[kEnergyLevels];
  for (uint32_t energy = 0; energy < kEnergyLevels; energy++) {
    r_table[energy] = (uint8_t)(red1 + energy * r_delta / kEnergyLevels);
    g_table[energy] = (uint8_t)(green1 + energy * g_delta / kEnergyLevels);
    b_table[energy] = (uint8_t)(blue1 + energy * b_delta / kEnergyLevels);
  }

  for (uint32_t scan_line = 0; scan_line < dst_info.height; scan_line++) {
    uint32_t* dst = reinterpret_cast<uint32_t*>(dst_pixels);
    pixel32_t* src = reinterpret_cast<pixel32_t*>(src_pixels);
    pixel32_t* src_line_end = src + src_info.width;

    while (src < src_line_end) {
      uint32_t energy = src->rgba8[0] + src->rgba8[1] + src->rgba8[2];

      int dst_red = r_table[energy];
      int dst_green = g_table[energy];
      int dst_blue = b_table[energy];

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
