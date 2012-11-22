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

extern "C" JNIEXPORT void JNICALL Java_com_android_photoeditor_filters_ImageUtils_nativeTint(
    JNIEnv *env, jobject obj, jobject src_bitmap, jobject dst_bitmap, jint tint) {
   pTintType f = (pTintType)JNIFunc[JNI_Tint].func_ptr;
   return f(env, obj, src_bitmap, dst_bitmap, tint);
}

extern "C" void Tint(
    JNIEnv *env, jobject obj, jobject src_bitmap, jobject dst_bitmap, jint tint) {
  AndroidBitmapInfo src_info;
  AndroidBitmapInfo dst_info;
  void* src_pixels;
  void* dst_pixels;

  int ret = LockBitmaps(
      env, src_bitmap, dst_bitmap, &src_info, &dst_info, &src_pixels, &dst_pixels);
  if (ret < 0) {
    LOGE("LockBitmaps in Tint failed, error=%d", ret);
    return;
  }

  const int kShiftBits = 10;
  const int kRedRatio = static_cast<int>(0.8f * (1 << kShiftBits) * 0.21f);
  const int kGreenRatio = static_cast<int>(0.8f * (1 << kShiftBits) * 0.71f);
  const int kBlueRatio = static_cast<int>(0.8f * (1 << kShiftBits) * 0.07f);
  const int kTintRatio = static_cast<int>(0.2f * (1 << kShiftBits));

  uint32_t tint_red = kTintRatio * ((tint >> 16) & 0xff);
  uint32_t tint_green = kTintRatio * ((tint >> 8) & 0xff);
  uint32_t tint_blue = kTintRatio * (tint & 0xff);

  for (uint32_t scan_line = 0; scan_line < dst_info.height; scan_line++) {
    uint32_t* dst = reinterpret_cast<uint32_t*>(dst_pixels);
    pixel32_t* src = reinterpret_cast<pixel32_t*>(src_pixels);
    pixel32_t* src_line_end = src + src_info.width;
    while (src < src_line_end) {
      int32_t avg = kRedRatio * src->rgba8[0] + kGreenRatio * src->rgba8[1] +
          kBlueRatio * src->rgba8[2];

      int32_t dst_red = (avg + tint_red) >> kShiftBits;
      int32_t dst_green = (avg + tint_green) >> kShiftBits;
      int32_t dst_blue = (avg + tint_blue) >> kShiftBits;

      if (dst_red > 255) {
        dst_red = 255;
      }
      if (dst_green > 255) {
        dst_green = 255;
      }
      if (dst_blue > 255) {
        dst_blue = 255;
      }

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
