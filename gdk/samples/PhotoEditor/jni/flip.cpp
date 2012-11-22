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
using android::apps::photoeditor::utils::UnlockBitmaps;

namespace {

extern "C" JNIEXPORT void JNICALL Java_com_android_photoeditor_filters_ImageUtils_nativeFlipHorizontal(
    JNIEnv *env, jobject obj, jobject src_bitmap, jobject dst_bitmap) {
   pFlipHorizontalType f = (pFlipHorizontalType)JNIFunc[JNI_FlipHorizontal].func_ptr;
   return f(env, obj, src_bitmap, dst_bitmap);
}

extern "C" void FlipHorizontal(
    JNIEnv *env, jobject obj, jobject src_bitmap, jobject dst_bitmap) {
   
  AndroidBitmapInfo src_info;
  AndroidBitmapInfo dst_info;
  void* src_pixels;
  void* dst_pixels;

  int ret = LockBitmaps(
      env, src_bitmap, dst_bitmap, &src_info, &dst_info, &src_pixels, &dst_pixels);
  if (ret < 0) {
    LOGE("LockBitmaps in FlipHorizontal failed, error=%d", ret);
    return;
  }

  for (uint32_t scan_line = 0; scan_line < src_info.height; scan_line++) {
    uint32_t* dst = reinterpret_cast<uint32_t*>(dst_pixels) + src_info.width - 1;
    uint32_t* src = reinterpret_cast<uint32_t*>(src_pixels);
    uint32_t* src_line_end = src + src_info.width;

    while (src < src_line_end) {
      *dst = *src;
      src++;
      dst--;
    }
    src_pixels = reinterpret_cast<char*>(src_pixels) + src_info.stride;
    dst_pixels = reinterpret_cast<char*>(dst_pixels) + dst_info.stride;
  }

  UnlockBitmaps(env, src_bitmap, dst_bitmap);
}

extern "C" JNIEXPORT void JNICALL Java_com_android_photoeditor_filters_ImageUtils_nativeFlipVertical(
    JNIEnv *env, jobject obj, jobject src_bitmap, jobject dst_bitmap) {
   pFlipVerticalType f = (pFlipVerticalType)JNIFunc[JNI_FlipVertical].func_ptr;
   return f(env, obj, src_bitmap, dst_bitmap);
}
   
extern "C" void FlipVertical(
    JNIEnv *env, jobject obj, jobject src_bitmap, jobject dst_bitmap) {
  AndroidBitmapInfo src_info;
  AndroidBitmapInfo dst_info;
  void* src_pixels;
  void* dst_pixels;

  int ret = LockBitmaps(
      env, src_bitmap, dst_bitmap, &src_info, &dst_info, &src_pixels, &dst_pixels);
  if (ret < 0) {
    LOGE("LockBitmaps in FlipVertical failed, error=%d", ret);
    return;
  }

  dst_pixels = reinterpret_cast<char*>(dst_pixels) + dst_info.stride * (dst_info.height - 1);
  for (uint32_t scan_line = 0; scan_line < src_info.height; scan_line++) {
    uint32_t* dst = reinterpret_cast<uint32_t*>(dst_pixels);
    uint32_t* src = reinterpret_cast<uint32_t*>(src_pixels);
    uint32_t* src_line_end = src + src_info.width;

    while (src < src_line_end) {
      *dst = *src;
      src++;
      dst++;
    }
    src_pixels = reinterpret_cast<char*>(src_pixels) + src_info.stride;
    dst_pixels = reinterpret_cast<char*>(dst_pixels) - dst_info.stride;
  }

  UnlockBitmaps(env, src_bitmap, dst_bitmap);
}

extern "C" JNIEXPORT void JNICALL Java_com_android_photoeditor_filters_ImageUtils_nativeFlipBoth(
    JNIEnv *env, jobject obj, jobject src_bitmap, jobject dst_bitmap) {
   pFlipBothType f = (pFlipBothType)JNIFunc[JNI_FlipBoth].func_ptr;
   return f(env, obj, src_bitmap, dst_bitmap);
}
   
extern "C" void FlipBoth(
    JNIEnv *env, jobject obj, jobject src_bitmap, jobject dst_bitmap) {
  AndroidBitmapInfo src_info;
  AndroidBitmapInfo dst_info;
  void* src_pixels;
  void* dst_pixels;

  int ret = LockBitmaps(
      env, src_bitmap, dst_bitmap, &src_info, &dst_info, &src_pixels, &dst_pixels);
  if (ret < 0) {
    LOGE("LockBitmaps in FlipBoth failed, error=%d", ret);
    return;
  }

  dst_pixels = reinterpret_cast<char*>(dst_pixels) + dst_info.stride * (dst_info.height - 1);
  for (uint32_t scan_line = 0; scan_line < src_info.height; scan_line++) {
    uint32_t* dst = reinterpret_cast<uint32_t*>(dst_pixels) + src_info.width - 1;
    uint32_t* src = reinterpret_cast<uint32_t*>(src_pixels);
    uint32_t* src_line_end = src + src_info.width;

    while (src < src_line_end) {
      *dst = *src;
      src++;
      dst--;
    }
    src_pixels = reinterpret_cast<char*>(src_pixels) + src_info.stride;
    dst_pixels = reinterpret_cast<char*>(dst_pixels) - dst_info.stride;
  }

  UnlockBitmaps(env, src_bitmap, dst_bitmap);
}

}  // namespace
