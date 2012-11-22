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

#include "convolution.h"
#include "utils.h"
#include "_jni.h"

using android::apps::photoeditor::convolution::SpecialConvolution;
using android::apps::photoeditor::utils::LockBitmaps;
using android::apps::photoeditor::utils::UnlockBitmaps;

namespace {

extern "C" JNIEXPORT void JNICALL Java_com_android_photoeditor_filters_ImageUtils_nativeBlur(
    JNIEnv *env, jobject obj, jobject src_bitmap, jobject dst_bitmap, jfloat scale) {
   pBlurType f = (pBlurType)JNIFunc[JNI_Blur].func_ptr;
   return f(env, obj, src_bitmap, dst_bitmap, scale);
}

extern "C" void Blur(
    JNIEnv *env, jobject obj, jobject src_bitmap, jobject dst_bitmap, jfloat scale) {
  AndroidBitmapInfo src_info;
  AndroidBitmapInfo dst_info;
  void* src_pixels;
  void* dst_pixels;

  int ret = LockBitmaps(
      env, src_bitmap, dst_bitmap, &src_info, &dst_info, &src_pixels, &dst_pixels);
  if (ret < 0) {
    LOGE("LockBitmaps in Sharpen failed, error=%d", ret);
    return;
  }

  /* Divide blur scale by a factor of 5 which comes from trials. Due to a small (3x3)
   * kernel is used for convolution, small blur factor causes double edge. */
  SpecialConvolution(&src_info, &dst_info, src_pixels, dst_pixels, scale / 5);

  UnlockBitmaps(env, src_bitmap, dst_bitmap);
}

}  // namespace
