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
#include <cstdlib>

#include "utils.h"
#include "_jni.h"

using android::apps::photoeditor::utils::LockBitmaps;
using android::apps::photoeditor::utils::pixel32_t;
using android::apps::photoeditor::utils::UnlockBitmaps;

namespace {

extern "C" JNIEXPORT void JNICALL Java_com_android_photoeditor_filters_ImageUtils_nativeQuantize(
    JNIEnv *env, jobject obj, jobject src_bitmap, jobject dst_bitmap) {
   pQuantizeType f = (pQuantizeType)JNIFunc[JNI_Quantize].func_ptr;
   return f(env, obj, src_bitmap, dst_bitmap);
}
   
extern "C" void Quantize(
    JNIEnv *env, jobject obj, jobject src_bitmap, jobject dst_bitmap) {
  AndroidBitmapInfo src_info;
  AndroidBitmapInfo dst_info;
  void* src_pixels;
  void* dst_pixels;

  int ret = LockBitmaps(
      env, src_bitmap, dst_bitmap, &src_info, &dst_info, &src_pixels, &dst_pixels);
  if (ret < 0) {
    LOGE("LockBitmaps in quantize failed, error=%d", ret);
    return;
  }

  uint8_t quantize_map[256];

  for (uint32_t i = 0; i < 256; i++) {
    quantize_map[i] = i / 128 * 128 + 64;
  }

  for (uint32_t scan_line = 0; scan_line < src_info.height; scan_line++) {
    pixel32_t* src = reinterpret_cast<pixel32_t*>(src_pixels);
    pixel32_t* dst = reinterpret_cast<pixel32_t*>(dst_pixels);

    pixel32_t* src_line_end = src + src_info.width;
    while (src < src_line_end) {
      dst->rgba8[0] = quantize_map[src->rgba8[0]];
      dst->rgba8[1] = quantize_map[src->rgba8[1]];
      dst->rgba8[2] = quantize_map[src->rgba8[2]];
      dst->rgba8[3] = src->rgba8[3];
      dst++;
      src++;
    }
    dst_pixels = reinterpret_cast<char*>(dst_pixels) + dst_info.stride;
    src_pixels = reinterpret_cast<char*>(src_pixels) + src_info.stride;
  }

  UnlockBitmaps(env, src_bitmap, dst_bitmap);
}

}  // namespace
