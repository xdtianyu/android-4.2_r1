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

#include "utils.h"

#include <android/bitmap.h>

namespace android {
namespace apps {
namespace photoeditor {
namespace utils {

int ExtractInfoFromBitmap(JNIEnv *env,
    AndroidBitmapInfo *src_info, AndroidBitmapInfo *dst_info,
    jobject src_bitmap, jobject dst_bitmap) {
  int ret;
  /*
   * The info->format of both bitmap should be ANDROID_BITMAP_FORMAT_RGBA_8888.
   * Both bitmaps shall also be in the same dimension.
   */
  if ((ret = AndroidBitmap_getInfo(env, src_bitmap, src_info)) < 0) {
    LOGE("AndroidBitmap_getInfo(src_bitmap) failed, error=%d", ret);
    return ret;
  }
  if ((ret = AndroidBitmap_getInfo(env, dst_bitmap, dst_info)) < 0) {
    LOGE("AndroidBitmap_getInfo(dst_bitmap) failed, error=%d", ret);
    return ret;
  }
  return 0;
}

int LockBitmaps(JNIEnv* env, jobject src_bitmap, jobject dst_bitmap,
    AndroidBitmapInfo* src_info, AndroidBitmapInfo* dst_info,
    void** src_pixels, void** dst_pixels) {
  int ret;
  if ((ret = ExtractInfoFromBitmap(env, src_info, dst_info, src_bitmap, dst_bitmap)) < 0) {
    return ret;
  }
  if ((ret = AndroidBitmap_lockPixels(env, src_bitmap, src_pixels)) < 0) {
    LOGE("AndroidBitmap_lockPixels(src_bitmap) failed, error=%d", ret);
    return ret;
  }
  if ((ret = AndroidBitmap_lockPixels(env, dst_bitmap, dst_pixels)) < 0) {
    LOGE("AndroidBitmap_lockPixels(dst_bitmap) failed, error=%d", ret);
    AndroidBitmap_unlockPixels(env, src_bitmap);
    return ret;
  }
  return 0;
}

void UnlockBitmaps(JNIEnv* env, jobject src_bitmap, jobject dst_bitmap) {
  AndroidBitmap_unlockPixels(env, src_bitmap);
  AndroidBitmap_unlockPixels(env, dst_bitmap);
}

}  // namespace utils
}  // namespace photoeditor
}  // namespace apps
}  // namespace android

