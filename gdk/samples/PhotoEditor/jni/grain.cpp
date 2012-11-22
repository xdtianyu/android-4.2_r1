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

const uint32_t kShiftBits = 10;
const uint32_t kShiftValue = (1 << kShiftBits);

/*
 * Convolution matrix of distance 2 with fixed point of 'kShiftBits' bits
 * shifted. Thus the sum of this matrix should be 'kShiftValue'. Entries of
 * small values are not calculated to gain efficiency.
 * The order ot pixels represented in this matrix is:
 *  1  2  3
 *  4  0  5
 *  6  7  8
 *  and the matrix should be: {230, 56, 114, 56, 114, 114, 56, 114, 56}.
 *  However, since most of the valus are identical, we only use the first three
 *  entries and the entries corresponding to the pixels is:
 *  1  2  1
 *  2  0  2
 *  1  2  1
 */
const uint32_t convolution_matrix[3] = {230, 56, 114};

/*
 * Generate a blurred random noise bitmap.
 */
void GenerateBlurredNoise(void* dst_pixels, AndroidBitmapInfo* dst_info,
      float noise_scale) {
  uint32_t fixed_noise_scale = noise_scale * kShiftValue;

  // Clear dst bitmap to 0 for storing generated random  noise.
  memset(dst_pixels, 0, dst_info->stride * dst_info->height);

  // 0.5 is a empirical value and could be tuned.
  int random_threshold = RAND_MAX * 0.5;
  for (uint32_t y = 0; y < dst_info->height; y++) {
    uint32_t* dp_line = reinterpret_cast<uint32_t*>(
        reinterpret_cast<char*>(dst_pixels) + y * dst_info->stride);

    for (uint32_t x = 0; x < dst_info->width; x++) {
      if (rand() < random_threshold) {
        uint32_t* dp = dp_line + x;
        uint32_t* dp_prev = (y == 0) ? dp : (dp - dst_info->width);
        uint32_t* dp_next = (y == dst_info->height - 1) ? dp : (dp + dst_info->width);

        /*
         * 1  2  3
         * 4  0  5
         * 6  7  8
         */
        uint32_t* n[9];
        n[0] = dp;
        n[2] = dp_prev;
        n[7] = dp_next;
        if (x == 0) {
          n[1] = n[2];
          n[4] = n[0];
          n[6] = n[7];
        } else {
          n[1] = n[2] - 1;
          n[4] = n[0] - 1;
          n[6] = n[7] - 1;
        }
        if (x == dst_info->width - 1) {
          n[3] = n[2];
          n[5] = n[0];
          n[8] = n[7];
        } else {
          n[3] = n[2] + 1;
          n[5] = n[0] + 1;
          n[8] = n[7] + 1;
        }

        // noise randomness uniformly distributed between 0.5 to 1.5,
        // 0.5 is an empirical value.
        uint32_t random_noise_scale = fixed_noise_scale
            * (static_cast<double>(rand()) / RAND_MAX + 0.5);

        *n[0] = *n[0] + ((convolution_matrix[0] * random_noise_scale) >> kShiftBits);
        // The value in convolution_matrix is identical (56) for indexes 1, 3, 6, 8.
        uint32_t normal_scaled_noise = (convolution_matrix[1] * random_noise_scale) >> kShiftBits;
        *n[1] += normal_scaled_noise;
        *n[3] += normal_scaled_noise;
        *n[6] += normal_scaled_noise;
        *n[8] += normal_scaled_noise;
        // Likewise, the computation could be saved for indexes 2, 4, 5, 7;
        normal_scaled_noise = (convolution_matrix[2] * random_noise_scale) >> kShiftBits;
        *n[2] += normal_scaled_noise;
        *n[4] += normal_scaled_noise;
        *n[5] += normal_scaled_noise;
        *n[7] += normal_scaled_noise;
      }
    }
  }
}

extern "C" JNIEXPORT void JNICALL Java_com_android_photoeditor_filters_ImageUtils_nativeGrain(
    JNIEnv *env, jobject obj, jobject src_bitmap, jobject dst_bitmap, jfloat noise_scale) {
   pGrainType f = (pGrainType)JNIFunc[JNI_Grain].func_ptr;
   return f(env, obj, src_bitmap, dst_bitmap, noise_scale);
}
   
extern "C" void Grain(
    JNIEnv *env, jobject obj, jobject src_bitmap, jobject dst_bitmap, jfloat noise_scale) {
  AndroidBitmapInfo src_info;
  AndroidBitmapInfo dst_info;
  void* src_pixels;
  void* dst_pixels;

  int ret = LockBitmaps(
      env, src_bitmap, dst_bitmap, &src_info, &dst_info, &src_pixels, &dst_pixels);
  if (ret < 0) {
    LOGE("LockBitmaps in grain failed, error=%d", ret);
    return;
  }

  GenerateBlurredNoise(dst_pixels, &dst_info, noise_scale);

  for (uint32_t scan_line = 0; scan_line < src_info.height; scan_line++) {
    uint32_t* src = reinterpret_cast<uint32_t*>(
        reinterpret_cast<char*>(src_pixels) + src_info.stride * scan_line);
    uint32_t* dst = reinterpret_cast<uint32_t*>(
        reinterpret_cast<char*>(dst_pixels) + dst_info.stride * scan_line);

    uint32_t* src_line_end = src + src_info.width;
    while (src < src_line_end) {
      pixel32_t* sp = reinterpret_cast<pixel32_t*>(src);
      pixel32_t* dp = reinterpret_cast<pixel32_t*>(dst);

      // energy_mask is used to constrain the noise according to the energy
      // level. Film grain appear more in dark part.
      // The energy level (from 0 to 765) is square-rooted and should in the
      // range from 0 to 27.659 (sqrt(765)), so 28 is used for normalization.
      uint32_t energy_level = sp->rgba8[0] + sp->rgba8[1] + sp->rgba8[2];
      uint32_t energy_mask = 28 - static_cast<uint32_t>(sqrtf(energy_level));

      // The intensity of each channel of RGB is affected by the random
      // noise previously produced and stored in dp->pixel.
      // dp->pixel should be in the range of [1.3 * noise_scale * kShiftValue,
      // 0]. Therefore 'scale' should be in the range of
      // [kShiftValue, kShiftValue - 1.3 * noise_scale * kShiftValue]
      uint32_t scale = (kShiftValue - dp->rgba32 * energy_mask / 28);
      uint32_t red = (sp->rgba8[0] * scale) >> kShiftBits;
      uint32_t green = (sp->rgba8[1] * scale) >> kShiftBits;
      uint32_t blue = (sp->rgba8[2] * scale) >> kShiftBits;
      dp->rgba32 = (sp->rgba8[3] << 24) | (blue << 16) | (green << 8) | red;

      dst++;
      src++;
    }
  }

  UnlockBitmaps(env, src_bitmap, dst_bitmap);
}

}  // namespace
