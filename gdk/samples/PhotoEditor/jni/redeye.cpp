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

extern "C" JNIEXPORT void JNICALL Java_com_android_photoeditor_filters_ImageUtils_nativeRedEye(
    JNIEnv *env, jobject obj, jobject src_bitmap, jobject dst_bitmap,
    jobjectArray redeye_positions, jfloat radius, jfloat intensity) {
   pRedEyeType f = (pRedEyeType)JNIFunc[JNI_RedEye].func_ptr;
   return f(env, obj, src_bitmap, dst_bitmap, redeye_positions, radius, intensity);
}
   
extern "C" void RedEye(
    JNIEnv *env, jobject obj, jobject src_bitmap, jobject dst_bitmap,
    jobjectArray redeye_positions, jfloat radius, jfloat intensity) {
  AndroidBitmapInfo src_info;
  AndroidBitmapInfo dst_info;
  void* src_pixels;
  void* dst_pixels;

  int ret = LockBitmaps(
      env, src_bitmap, dst_bitmap, &src_info, &dst_info, &src_pixels, &dst_pixels);
  if (ret < 0) {
    LOGE("LockBitmaps in RedEye failed, error=%d", ret);
    return;
  }

  memcpy(dst_pixels, src_pixels, src_info.stride * src_info.height);

  // get the class reference.
  jclass cls = env->FindClass("android/graphics/PointF");
  jfieldID xid = env->GetFieldID(cls, "x", "F");
  jfieldID yid = env->GetFieldID(cls, "y", "F");
  int eye_number = env->GetArrayLength(redeye_positions);
  for (int eye = 0; eye < eye_number; eye++) {
    jobject point_f = env->GetObjectArrayElement(redeye_positions, eye);
    float focus_x = env->GetFloatField(point_f, xid);
    float focus_y = env->GetFloatField(point_f, yid);
    if (focus_x < 0.0 || focus_x > 1.0 || focus_y < 0.0 || focus_y > 1.0) {
      // did not tap on the picture
      continue;
    }

    float x = focus_x * src_info.width;
    float y = focus_y * src_info.height;
    uint32_t bound_y = y + radius;
    if (bound_y > dst_info.height) {
      bound_y = dst_info.height;
    }
    uint32_t start_x = (x > radius) ? x - radius: 0;
    uint32_t bound_x = x + radius;
    if (bound_x > dst_info.width) {
      bound_x = dst_info.width;
    }
    uint32_t dst_y = (y > radius) ? y - radius : 0;
    for (; dst_y < bound_y; dst_y++) {
      uint32_t *dst = reinterpret_cast<uint32_t*>(
          reinterpret_cast<char*>(dst_pixels) + dst_info.stride * dst_y) + start_x;
      pixel32_t *src = reinterpret_cast<pixel32_t*>(
          reinterpret_cast<char*>(src_pixels) + src_info.stride * dst_y) + start_x;
      for (uint32_t dst_x = start_x; dst_x < bound_x; dst_x++) {
        if (hypotf(dst_x - x, dst_y - y) <= radius) {
          int32_t red = src->rgba8[0];
          int32_t green = src->rgba8[1];
          int32_t blue = src->rgba8[2];
          int32_t alpha = src->rgba8[3];

          float redIntensity = static_cast<float>(red) / (green + blue);
          if (redIntensity > intensity) {
            red = (green + blue) / 2.0f;
            *dst = alpha << 24 | (blue << 16) | (green << 8) | red;
          }
        }
        dst++;
        src++;
      }  // dst_x
    }  // dst_y
  }

  UnlockBitmaps(env, src_bitmap, dst_bitmap);
}

}  // namespace
