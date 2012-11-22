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
using android::apps::photoeditor::utils::UnlockBitmaps;

namespace {

uint32_t BilinearPixelInterpolation(
    float x, float y, uint8_t *p00, uint8_t *p01, uint8_t *p10, uint8_t *p11) {

  float coef4 = x * y;
  float coef2 = x - coef4; // x * (1 - y)
  float coef3 = y - coef4; // (1 - x) * y
  float coef1 = 1 - x - coef3; // (1 - x) * (1 - y)

  int dst_red = p00[0] * coef1 + p10[0] * coef2 + p01[0] * coef3 + p11[0] * coef4;
  int dst_green = p00[1] * coef1 + p10[1] * coef2 + p01[1] * coef3 + p11[1] * coef4;
  int dst_blue = p00[2] * coef1 + p10[2] * coef2 + p01[2] * coef3 + p11[2] * coef4;
  if (dst_red > 255) {
    dst_red = 255;
  }
  if (dst_green > 255) {
    dst_green = 255;
  }
  if (dst_blue > 255) {
    dst_blue = 255;
  }

  // alpha is not calculated, directly from any point should do.
  return p00[3] << 24 | (dst_blue << 16) | (dst_green << 8) | dst_red;
}

void FisheyeMapPixels(
    float px, float py, uint32_t x, uint32_t y, AndroidBitmapInfo *src_info,
    AndroidBitmapInfo *dst_info, void *src_pixels, void *dst_pixels) {
  if (x >= src_info->width || y >= src_info->height) {
    return;
  }

  uint32_t px_floor = floor(px);
  uint32_t py_floor = floor(py);

  uint32_t *p00 = (uint32_t*)((char*)src_pixels + src_info->stride * py_floor) + px_floor;
  uint32_t *p01, *p10, *p11;
  if (py_floor + 1 < src_info->height) {
    p01 = (uint32_t*)((char*)p00 + src_info->stride);
  } else {
    p01 = p00;
  }
  if (py_floor + 1 < src_info->width) {
    p10 = p00 + 1;
    p11 = p01 + 1;
  } else {
    p10 = p00;
    p11 = p01;
  }

  uint32_t *dst = (uint32_t*)((char*)dst_pixels + dst_info->stride * y) + x;
  *dst = BilinearPixelInterpolation(px - px_floor, py - py_floor,
                                    (uint8_t*)p00, (uint8_t*)p01, (uint8_t*)p10, (uint8_t*)p11);
}

/*
 * @param scale ranges from 0.0 to 1.0.
 * @param focus_x is the X-coord of the center of the projection ranging from 0 to 1.
 * @param focus_y is the Y-coord of the center of the projection ranging from 0 to 1.
 */
extern "C" JNIEXPORT void JNICALL Java_com_android_photoeditor_filters_ImageUtils_nativeFisheye(
    JNIEnv *env, jobject obj, jobject src_bitmap, jobject dst_bitmap,
    jfloat focus_x, jfloat focus_y, jfloat scale) {
   pFisheyeType f = (pFisheyeType)JNIFunc[JNI_Fisheye].func_ptr;
   return f(env, obj, src_bitmap, dst_bitmap, focus_x, focus_y, scale);
}

extern "C" void Fisheye(
    JNIEnv *env, jobject obj, jobject src_bitmap, jobject dst_bitmap,
    jfloat focus_x, jfloat focus_y, jfloat scale) {
  AndroidBitmapInfo src_info;
  AndroidBitmapInfo dst_info;
  void* src_pixels;
  void* dst_pixels;

  int ret = LockBitmaps(
      env, src_bitmap, dst_bitmap, &src_info, &dst_info, &src_pixels, &dst_pixels);
  if (ret < 0) {
    LOGE("LockBitmaps in FishEye failed, error=%d", ret);
    return;
  }

  if (scale == 0) {
    memcpy(dst_pixels, src_pixels, src_info.stride * src_info.height);
    return;
  }

  float far_point = hypotf(src_info.height * focus_y, src_info.width * focus_x);
  const float r = far_point * 1.15; // radius
  const float r2 = r * r;
  const uint32_t center_x = round(src_info.width * focus_x);
  const uint32_t center_y = round(src_info.height * focus_y);
  float alpha = 0.75 + scale * 2.0;
  float linear_scale = far_point /
      (M_PI_2 - atan(alpha * sqrtf(r2 - far_point * far_point) / far_point));

  for (uint32_t scan_line = 0; scan_line <= center_y; scan_line++) {

    int y = scan_line - center_y;
    int y2 = y * y;

    for (uint32_t dst_x = 0; dst_x <= center_x; dst_x++) {
      int x = dst_x - center_x;
      float xy2 = (x * x + y2);
      float s_xy2 = sqrtf(xy2);
      float r_scale = linear_scale * (M_PI_2 - atan(alpha * sqrtf(r2 - xy2) / s_xy2)) / s_xy2;
      float scaled_x = x * r_scale;
      float scaled_y = y * r_scale;
      int nx = center_x - x;
      int ny = center_y - y;
      float fpx = center_x + scaled_x;
      float fnx = center_x - scaled_x;
      float fpy = center_y + scaled_y;
      float fny = center_y - scaled_y;

      FisheyeMapPixels(fpx, fpy, dst_x, scan_line, &src_info, &dst_info, src_pixels, dst_pixels);
      FisheyeMapPixels(fpx, fny, dst_x, ny, &src_info, &dst_info, src_pixels, dst_pixels);
      FisheyeMapPixels(fnx, fpy, nx, scan_line, &src_info, &dst_info, src_pixels, dst_pixels);
      FisheyeMapPixels(fnx, fny, nx, ny, &src_info, &dst_info, src_pixels, dst_pixels);
    }
  }
  // Deal with the condition when x = 0 and y = 0 (dst_x = center_x and
  // scan_line = center_y.
  uint32_t *dst = (uint32_t*)((char*)dst_pixels + dst_info.stride * center_y) + center_x;
  *dst = *((uint32_t*)((char*)src_pixels + src_info.stride * center_y) + center_x);

  UnlockBitmaps(env, src_bitmap, dst_bitmap);
}

}  // namespace
