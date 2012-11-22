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

const int k256Multiply255 = 65280;

/*
 * @param scale ranges from -0.5 to 0.5.
 */
extern "C" JNIEXPORT void JNICALL Java_com_android_photoeditor_filters_ImageUtils_nativeColorTemp(
    JNIEnv *env, jobject obj, jobject src_bitmap, jobject dst_bitmap, jfloat scale) {
   pColorTempType f = (pColorTempType)JNIFunc[JNI_ColorTemp].func_ptr;
   return f(env, obj, src_bitmap, dst_bitmap, scale);
}
   
extern "C" void ColorTemp(
    JNIEnv *env, jobject obj, jobject src_bitmap, jobject dst_bitmap, jfloat scale) {
  AndroidBitmapInfo src_info;
  AndroidBitmapInfo dst_info;
  void* src_pixels;
  void* dst_pixels;

  int ret = LockBitmaps(
      env, src_bitmap, dst_bitmap, &src_info, &dst_info, &src_pixels, &dst_pixels);
  if (ret < 0) {
    LOGE("LockBitmaps in ColorTemp failed, error=%d", ret);
    return;
  }

  int curve[256];
  for (int i = 0; i < 256; i++) {
    curve[i] = i * (256 - i);
  }

  int ics = static_cast<int>(scale * 256);
  int icsr = ics;
  int icsg = ics > 0 ? ics : 0;
  int icsb = ics;

  for (uint32_t scan_line = 0; scan_line < dst_info.height; scan_line++) {
    uint32_t* dst = reinterpret_cast<uint32_t*>(dst_pixels);
    pixel32_t* src = reinterpret_cast<pixel32_t*>(src_pixels);
    pixel32_t* src_line_end = src + src_info.width;

    while (src < src_line_end) {
      int32_t src_red = src->rgba8[0];
      int32_t src_green = src->rgba8[1];
      int32_t src_blue = src->rgba8[2];
      int32_t src_alpha = src->rgba8[3];

      int32_t dst_red = src_red + ((curve[src_red] * icsr) >> 15);
      int32_t dst_green = src_green + ((curve[src_green] * icsg) >> 17);
      int32_t dst_blue = src_blue - ((curve[src_blue] * icsb) >> 15);

      int rgb_max = MAX3(dst_red, dst_green, dst_blue);
      if (rgb_max > 255) {
        int invmax = k256Multiply255 / rgb_max;
        dst_red = dst_red * invmax >> 8;
        dst_green = dst_green * invmax >> 8;
        dst_blue = dst_blue * invmax >> 8;
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
