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

#include <cstdlib>

#include "utils.h"

using android::apps::photoeditor::utils::clamp;
using android::apps::photoeditor::utils::LockBitmaps;
using android::apps::photoeditor::utils::pixel32_t;
using android::apps::photoeditor::utils::UnlockBitmaps;

namespace android {
namespace apps {
namespace photoeditor {
namespace convolution {

/*
 * Used for convolution of following kernel:
 *   n n n
 *   n c n
 *   n n n
 * n: neighbor, c: center
 */
void SpecialConvolution(AndroidBitmapInfo *src_info, AndroidBitmapInfo *dst_info,
    void *src_pixels, void *dst_pixels, float neighbor) {

  if (neighbor == 0) {
    memcpy(dst_pixels, src_pixels, src_info->stride * src_info->height);
    return;
  }

  /* Copy the first row to dst_pixels */
  memcpy(dst_pixels, src_pixels, src_info->stride);
  src_pixels = reinterpret_cast<char*>(src_pixels) + src_info->stride;
  dst_pixels = reinterpret_cast<char*>(dst_pixels) + dst_info->stride;

  const int32_t kShiftBits = 10;
  int32_t fixed_neighbor = static_cast<int32_t>((1 << kShiftBits) * neighbor);
  int32_t fixed_center = static_cast<int32_t>((1 << kShiftBits) * (1 - neighbor * 8));

  for (uint32_t scan_line = 1; scan_line < dst_info->height - 1; scan_line++) {
    pixel32_t* src = reinterpret_cast<pixel32_t*>(src_pixels);
    uint32_t* src_prev = reinterpret_cast<uint32_t*>
        (reinterpret_cast<char*>(src_pixels) - src_info->stride);
    uint32_t* src_next = reinterpret_cast<uint32_t*>
        (reinterpret_cast<char*>(src_pixels) + src_info->stride);

    uint32_t* dst = reinterpret_cast<uint32_t*>(dst_pixels);
    *dst++ = src->rgba32;
    src++;
    src_prev++;
    src_next++;

    uint32_t* dst_line_end = dst + dst_info->width - 1;
    while (dst < dst_line_end) {
      int32_t src_red = src->rgba8[0];
      int32_t src_green = src->rgba8[1];
      int32_t src_blue = src->rgba8[2];
      int32_t src_alpha = src->rgba8[3];

      uint8_t* n1 = reinterpret_cast<uint8_t*>(src_prev - 1);
      uint8_t* n2 = reinterpret_cast<uint8_t*>(src - 1);
      uint8_t* n3 = reinterpret_cast<uint8_t*>(src_next - 1);
      uint8_t* n4 = reinterpret_cast<uint8_t*>(src_prev);
      uint8_t* n5 = reinterpret_cast<uint8_t*>(src_next);
      uint8_t* n6 = reinterpret_cast<uint8_t*>(src_prev + 1);
      uint8_t* n7 = reinterpret_cast<uint8_t*>(src + 1);
      uint8_t* n8 = reinterpret_cast<uint8_t*>(src_next + 1);

      int32_t red = n1[0] + n2[0] + n3[0] + n4[0] + n5[0] + n6[0] + n7[0] + n8[0];
      int32_t green = n1[1] + n2[1] + n3[1] + n4[1] + n5[1] + n6[1] + n7[1] + n8[1];
      int32_t blue = n1[2] + n2[2] + n3[2] + n4[2] + n5[2] + n6[2] + n7[2] + n8[2];

      red = (fixed_neighbor * red + fixed_center * src_red) >> kShiftBits;
      green = (fixed_neighbor * green + fixed_center * src_green) >> kShiftBits;
      blue = (fixed_neighbor * blue + fixed_center * src_blue) >> kShiftBits;

      red = clamp(red, 0, 255);
      green = clamp(green, 0, 255);
      blue = clamp(blue, 0, 255);

      *dst = (src_alpha << 24) | (blue << 16) | (green << 8) | red;
      dst++;
      src++;
      src_prev++;
      src_next++;
    }
    *dst = src->rgba32;
    src_pixels = reinterpret_cast<char*>(src_pixels) + src_info->stride;
    dst_pixels = reinterpret_cast<char*>(dst_pixels) + dst_info->stride;
  }

  /* Copy the last row to dst_pixels */
  memcpy(dst_pixels, src_pixels, src_info->stride);
}

}  // namespace convolution
}  // namespace photoeditor
}  // namespace apps
}  // namespace android
