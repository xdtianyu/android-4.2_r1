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

#ifndef PHOTOEDITOR_JNI_UTILS_H_
#define PHOTOEDITOR_JNI_UTILS_H_

#include <android/bitmap.h>
#include <android/log.h>
#include <jni.h>

#define  LOG_TAG    "libjni_photoeditor"
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

#define MIN3(x, y, z)  ((y) <= (z) ? \
                         ((x) <= (y) ? (x) : (y)) \
                     : \
                         ((x) <= (z) ? (x) : (z)))

#define MAX3(x, y, z)  ((y) >= (z) ? \
                         ((x) >= (y) ? (x) : (y)) \
                     : \
                         ((x) >= (z) ? (x) : (z)))

namespace android {
namespace apps {
namespace photoeditor {
namespace utils {

inline int clamp(int x, int min, int max) {
  return (x < min ? min : ((x > max) ? max : x));
}

union pixel32_t {
  uint32_t rgba32;
  uint8_t rgba8[4];  // 0: red 1:green 2:blue 3:alpha
};

typedef union pixel32_t pixel32_t;

int ExtractInfoFromBitmap(JNIEnv *env,
    AndroidBitmapInfo *src_info, AndroidBitmapInfo *dst_info,
    jobject src_bitmap, jobject dst_bitmap);

int LockBitmaps(JNIEnv* env, jobject src_bitmap, jobject dst_bitmap,
    AndroidBitmapInfo* src_info, AndroidBitmapInfo* dst_info,
    void** src_pixels, void** dst_pixels);

void UnlockBitmaps(JNIEnv* env, jobject src_bitmap, jobject dst_bitmap);

}  // namespace utils
}  // namespace photoeditor
}  // namespace apps
}  // namespace android


#endif  // PHOTOEDITOR_JNI_UTILS_H_
