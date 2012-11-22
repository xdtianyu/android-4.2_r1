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
#include <ctime>

#include "utils.h"
#include "_jni.h"

using android::apps::photoeditor::utils::clamp;
using android::apps::photoeditor::utils::LockBitmaps;
using android::apps::photoeditor::utils::pixel32_t;
using android::apps::photoeditor::utils::UnlockBitmaps;

namespace {

const int k256Multiply255 = 65280;

void WhiteblackXF(float white, float black, int xform[]) {
  float scale = (black != white) ? 1.0f / (white - black) : 2000.0f;
  float offset = k256Multiply255 * black;
  for (int i = 0; i < 256; i++) {
    float vgamma = (256.0f * i - offset) * scale;
    xform[i] = clamp(floor(vgamma), 0, k256Multiply255);
  }
  xform[256] = xform[255];
}

extern "C" JNIEXPORT void JNICALL Java_com_android_photoeditor_filters_ImageUtils_nativeWhiteBlack(
    JNIEnv *env, jobject obj, jobject src_bitmap, jobject dst_bitmap, jfloat white, jfloat black) {
   pWhiteBlackType f = (pWhiteBlackType)JNIFunc[JNI_WhiteBlack].func_ptr;
   return f(env, obj, src_bitmap, dst_bitmap, white, black);
}

extern "C" void WhiteBlack(
    JNIEnv *env, jobject obj, jobject src_bitmap, jobject dst_bitmap, jfloat white, jfloat black) {
  AndroidBitmapInfo src_info;
  AndroidBitmapInfo dst_info;
  void* src_pixels;
  void* dst_pixels;

  int ret = LockBitmaps(
      env, src_bitmap, dst_bitmap, &src_info, &dst_info, &src_pixels, &dst_pixels);
  if (ret < 0) {
    LOGE("LockBitmaps in WhiteBlack failed, error=%d", ret);
    return;
  }

  srandom(time(NULL));

  int xform[257];
  WhiteblackXF(white, black, xform);

  for (uint32_t scan_line = 0; scan_line < src_info.height; scan_line++) {
    pixel32_t* src = reinterpret_cast<pixel32_t*>(src_pixels);
    uint32_t* dst = reinterpret_cast<uint32_t*>(dst_pixels);
    pixel32_t* src_line_end = src + src_info.width;
    while (src < src_line_end) {
      int32_t red = src->rgba8[0];
      int32_t green = src->rgba8[1];
      int32_t blue = src->rgba8[2];
      int32_t alpha = src->rgba8[3];

      int32_t xform_red = xform[red];
      int32_t xform_green = xform[green];
      int32_t xform_blue = xform[blue];

      red = xform[red + 1] - xform_red;
      green = xform[green + 1] - xform_green;
      blue = xform[blue + 1] - xform_blue;

      int32_t dither = random() % 256;
      int32_t diff_red = red * dither >> 8;
      int32_t diff_green = green * dither >> 8;
      int32_t diff_blue = blue * dither >> 8;

      red = (xform_red + diff_red - (red >> 1)) >> 8;
      green = (xform_green + diff_green - (green >> 1)) >> 8;
      blue = (xform_blue + diff_blue - (blue >> 1)) >> 8;

      red = clamp(red, 0, 255);
      green = clamp(green, 0, 255);
      blue = clamp(blue, 0, 255);

      *dst = (alpha << 24) | (blue << 16) | (green << 8) | red;
      src++;
      dst++;
    }
    src_pixels = reinterpret_cast<char*>(src_pixels) + src_info.stride;
    dst_pixels = reinterpret_cast<char*>(dst_pixels) + dst_info.stride;
  }

  UnlockBitmaps(env, src_bitmap, dst_bitmap);
}

}  // namespace
