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
#include <cstring>

#include "utils.h"
#include "_jni.h"

using android::apps::photoeditor::utils::clamp;
using android::apps::photoeditor::utils::LockBitmaps;
using android::apps::photoeditor::utils::pixel32_t;
using android::apps::photoeditor::utils::UnlockBitmaps;

namespace {

  // Array of approximated CDF of normal distribution.
  // 1024 Entries in total. The array is approximated by sigmoid function and
  // the exact command in "octave" is:
  // x = [2/1029:1/1029:1025/1029];
  // y = (-1/11.5*log(1./x-0.9999)+0.5)*766*0.9+766*0.05;
  const int kCDFEntries = 1024;
  const uint32_t normal_cdf[] = {
    9, 33, 50, 64, 75, 84, 92, 99, 106, 112, 117, 122, 126, 130, 134, 138, 142,
    145, 148, 150, 154, 157, 159, 162, 164, 166, 169, 170, 173, 175, 177, 179,
    180, 182, 184, 186, 188, 189, 190, 192, 194, 195, 197, 198, 199, 200, 202,
    203, 205, 206, 207, 208, 209, 210, 212, 213, 214, 215, 216, 217, 218, 219,
    220, 221, 222, 223, 224, 225, 226, 227, 228, 229, 229, 230, 231, 232, 233,
    234, 235, 236, 236, 237, 238, 239, 239, 240, 240, 242, 242, 243, 244, 245,
    245, 246, 247, 247, 248, 249, 249, 250, 250, 251, 252, 253, 253, 254, 255,
    255, 256, 256, 257, 258, 258, 259, 259, 259, 260, 261, 262, 262, 263, 263,
    264, 264, 265, 265, 266, 267, 267, 268, 268, 269, 269, 269, 270, 270, 271,
    272, 272, 273, 273, 274, 274, 275, 275, 276, 276, 277, 277, 277, 278, 278,
    279, 279, 279, 280, 280, 281, 282, 282, 282, 283, 283, 284, 284, 285, 285,
    285, 286, 286, 287, 287, 288, 288, 288, 289, 289, 289, 290, 290, 290, 291,
    292, 292, 292, 293, 293, 294, 294, 294, 295, 295, 296, 296, 296, 297, 297,
    297, 298, 298, 298, 299, 299, 299, 299, 300, 300, 301, 301, 302, 302, 302,
    303, 303, 304, 304, 304, 305, 305, 305, 306, 306, 306, 307, 307, 307, 308,
    308, 308, 309, 309, 309, 309, 310, 310, 310, 310, 311, 312, 312, 312, 313,
    313, 313, 314, 314, 314, 315, 315, 315, 315, 316, 316, 316, 317, 317, 317,
    318, 318, 318, 319, 319, 319, 319, 319, 320, 320, 320, 321, 321, 322, 322,
    322, 323, 323, 323, 323, 324, 324, 324, 325, 325, 325, 325, 326, 326, 326,
    327, 327, 327, 327, 328, 328, 328, 329, 329, 329, 329, 329, 330, 330, 330,
    330, 331, 331, 332, 332, 332, 333, 333, 333, 333, 334, 334, 334, 334, 335,
    335, 335, 336, 336, 336, 336, 337, 337, 337, 337, 338, 338, 338, 339, 339,
    339, 339, 339, 339, 340, 340, 340, 340, 341, 341, 342, 342, 342, 342, 343,
    343, 343, 344, 344, 344, 344, 345, 345, 345, 345, 346, 346, 346, 346, 347,
    347, 347, 347, 348, 348, 348, 348, 349, 349, 349, 349, 349, 349, 350, 350,
    350, 350, 351, 351, 352, 352, 352, 352, 353, 353, 353, 353, 354, 354, 354,
    354, 355, 355, 355, 355, 356, 356, 356, 356, 357, 357, 357, 357, 358, 358,
    358, 358, 359, 359, 359, 359, 359, 359, 359, 360, 360, 360, 360, 361, 361,
    362, 362, 362, 362, 363, 363, 363, 363, 364, 364, 364, 364, 365, 365, 365,
    365, 366, 366, 366, 366, 366, 367, 367, 367, 367, 368, 368, 368, 368, 369,
    369, 369, 369, 369, 369, 370, 370, 370, 370, 370, 371, 371, 372, 372, 372,
    372, 373, 373, 373, 373, 374, 374, 374, 374, 374, 375, 375, 375, 375, 376,
    376, 376, 376, 377, 377, 377, 377, 378, 378, 378, 378, 378, 379, 379, 379,
    379, 379, 379, 380, 380, 380, 380, 381, 381, 381, 382, 382, 382, 382, 383,
    383, 383, 383, 384, 384, 384, 384, 385, 385, 385, 385, 385, 386, 386, 386,
    386, 387, 387, 387, 387, 388, 388, 388, 388, 388, 389, 389, 389, 389, 389,
    389, 390, 390, 390, 390, 391, 391, 392, 392, 392, 392, 392, 393, 393, 393,
    393, 394, 394, 394, 394, 395, 395, 395, 395, 396, 396, 396, 396, 396, 397,
    397, 397, 397, 398, 398, 398, 398, 399, 399, 399, 399, 399, 399, 400, 400,
    400, 400, 400, 401, 401, 402, 402, 402, 402, 403, 403, 403, 403, 404, 404,
    404, 404, 405, 405, 405, 405, 406, 406, 406, 406, 406, 407, 407, 407, 407,
    408, 408, 408, 408, 409, 409, 409, 409, 409, 409, 410, 410, 410, 410, 411,
    411, 412, 412, 412, 412, 413, 413, 413, 413, 414, 414, 414, 414, 415, 415,
    415, 415, 416, 416, 416, 416, 417, 417, 417, 417, 418, 418, 418, 418, 419,
    419, 419, 419, 419, 419, 420, 420, 420, 420, 421, 421, 422, 422, 422, 422,
    423, 423, 423, 423, 424, 424, 424, 425, 425, 425, 425, 426, 426, 426, 426,
    427, 427, 427, 427, 428, 428, 428, 429, 429, 429, 429, 429, 429, 430, 430,
    430, 430, 431, 431, 432, 432, 432, 433, 433, 433, 433, 434, 434, 434, 435,
    435, 435, 435, 436, 436, 436, 436, 437, 437, 437, 438, 438, 438, 438, 439,
    439, 439, 439, 439, 440, 440, 440, 441, 441, 442, 442, 442, 443, 443, 443,
    443, 444, 444, 444, 445, 445, 445, 446, 446, 446, 446, 447, 447, 447, 448,
    448, 448, 449, 449, 449, 449, 449, 450, 450, 450, 451, 451, 452, 452, 452,
    453, 453, 453, 454, 454, 454, 455, 455, 455, 456, 456, 456, 457, 457, 457,
    458, 458, 458, 459, 459, 459, 459, 460, 460, 460, 461, 461, 462, 462, 462,
    463, 463, 463, 464, 464, 465, 465, 465, 466, 466, 466, 467, 467, 467, 468,
    468, 469, 469, 469, 469, 470, 470, 470, 471, 472, 472, 472, 473, 473, 474,
    474, 474, 475, 475, 476, 476, 476, 477, 477, 478, 478, 478, 479, 479, 479,
    480, 480, 480, 481, 482, 482, 483, 483, 484, 484, 484, 485, 485, 486, 486,
    487, 487, 488, 488, 488, 489, 489, 489, 490, 490, 491, 492, 492, 493, 493,
    494, 494, 495, 495, 496, 496, 497, 497, 498, 498, 499, 499, 499, 500, 501,
    502, 502, 503, 503, 504, 504, 505, 505, 506, 507, 507, 508, 508, 509, 509,
    510, 510, 511, 512, 513, 513, 514, 515, 515, 516, 517, 517, 518, 519, 519,
    519, 520, 521, 522, 523, 524, 524, 525, 526, 526, 527, 528, 529, 529, 530,
    531, 532, 533, 534, 535, 535, 536, 537, 538, 539, 539, 540, 542, 543, 544,
    545, 546, 547, 548, 549, 549, 550, 552, 553, 554, 555, 556, 558, 559, 559,
    561, 562, 564, 565, 566, 568, 569, 570, 572, 574, 575, 577, 578, 579, 582,
    583, 585, 587, 589, 590, 593, 595, 597, 599, 602, 604, 607, 609, 612, 615,
    618, 620, 624, 628, 631, 635, 639, 644, 649, 654, 659, 666, 673, 680, 690,
    700, 714};

extern "C" JNIEXPORT void JNICALL Java_com_android_photoeditor_filters_ImageUtils_nativeHEQ(
    JNIEnv *env, jobject obj, jobject src_bitmap, jobject dst_bitmap, jfloat scale) {
   pHEQType f = (pHEQType)JNIFunc[JNI_HEQ].func_ptr;
   return f(env, obj, src_bitmap, dst_bitmap, scale);
}
   
extern "C" void HEQ(
    JNIEnv *env, jobject obj, jobject src_bitmap, jobject dst_bitmap, jfloat scale) {
  AndroidBitmapInfo src_info;
  AndroidBitmapInfo dst_info;
  void* src_pixels;
  void* dst_pixels;

  int ret = LockBitmaps(
      env, src_bitmap, dst_bitmap, &src_info, &dst_info, &src_pixels, &dst_pixels);
  if (ret < 0) {
    LOGE("LockBitmaps in HEQ failed, error=%d", ret);
    return;
  }

  const int kEnergyLevels = 766;  // 255 * 3 + 1

  // The energy has the range of [0, kEnergyLevels]
  int accumulated_histogram[kEnergyLevels];
  memset(accumulated_histogram, 0, sizeof(accumulated_histogram));

  // Store all the energy in the dst_pixels
  pixel32_t* dst = reinterpret_cast<pixel32_t*>(dst_pixels);
  pixel32_t const* src = reinterpret_cast<pixel32_t const*>(src_pixels);
  pixel32_t const* src_end = reinterpret_cast<pixel32_t const*>(
      reinterpret_cast<char const*>(src) + src_info.stride * src_info.height);
  while (src < src_end) {
    dst->rgba32 = src->rgba8[0] + src->rgba8[1] + src->rgba8[2];
    ++src;
    ++dst;
  }

  // Build up the accumulated histogram table by ignoring borders (1/20 = 5% width).
  float border_thickness_ratio = 0.05;
  int y_border_thickness = dst_info.height * border_thickness_ratio;
  pixel32_t* dst_line = reinterpret_cast<pixel32_t*>(
      reinterpret_cast<char*>(dst_pixels) + dst_info.stride * y_border_thickness);
  pixel32_t* dst_line_end = reinterpret_cast<pixel32_t*>(
      reinterpret_cast<char*>(dst_pixels) + dst_info.stride *
      (dst_info.height - y_border_thickness));
  int x_border_thickness = dst_info.width * border_thickness_ratio;
  int x_border_end = dst_info.width - x_border_thickness;
  while (dst_line < dst_line_end) {
    pixel32_t* dp = dst_line + x_border_thickness;
    pixel32_t* dp_end = dst_line + x_border_end;
    while (dp < dp_end) {
      ++accumulated_histogram[dp->rgba32];
      ++dp;
    }
    dst_line = reinterpret_cast<pixel32_t*>(
        reinterpret_cast<char*>(dst_line) + dst_info.stride);
  }

  for (int i = 1; i < kEnergyLevels; i++) {
    accumulated_histogram[i] += accumulated_histogram[i - 1];
  }

  uint32_t const* src_line =
      reinterpret_cast<uint32_t const*>(src_pixels);
  dst_line = reinterpret_cast<pixel32_t*>(dst_pixels);
  dst_line_end = reinterpret_cast<pixel32_t*>(reinterpret_cast<char*>(dst_line) +
      dst_info.height * dst_info.stride);
  // The whole process is done by apply the HEQ result with a mask.
  // The mask is a curve segmented by the energy_middle which could be tuned
  // based on each bitmap. For the lower part, the mask tries to make the change
  // significant for greater energy. For the smaller part, the mask does the
  // contrary. The two curve should be continuous on the energy_middle so the
  // final result is more natural. In this implementation, what I defined is two
  // curve based on the energy 'e', for the smaller part, e^2 is used. For the
  // greater part, e^1.5 is used. That is, for pixel with energy 'e', the mask
  // is obtained by:
  // if e > energy_middle
  //     (e - energy_middle)^1.5 / (765 - energy_middle)^1.5
  // else
  //     (e - energy_middle)^2 / (energy_middle)^2
  const int kShiftBits = 10;
  const int kShiftValue = (1 << kShiftBits);
  const int scale_shifted = scale * kShiftValue;
  const int normalization_scale_shifted = (1.0 - scale) * kShiftValue;
  const int energy_middle = 382;  // 765 / 2 = 382.5
  const int normalization_low = 7481;  // (765 - 382.5)^1.5
  const int normalization_high = 146307;  // 382.5^2
  int total_pixels = accumulated_histogram[kEnergyLevels - 1];
  while (dst_line < dst_line_end) {
    pixel32_t const* sp = reinterpret_cast<pixel32_t const*>(src_line);
    pixel32_t* dp = dst_line;
    pixel32_t* dp_end = dp + dst_info.width;
    while (dp < dp_end) {
      if (!dp->rgba32) {  // the energy is 0, no changes will be made.
        dp->rgba32 = sp->rgba32;
      } else {
        uint32_t energy = dp->rgba32;
        int mask_normalization;
        int mask_value = energy - energy_middle;

        if (mask_value > 0) {
          mask_value = mask_value * sqrt(mask_value);
          mask_normalization = normalization_low;
        } else {
          mask_value *= mask_value;
          mask_normalization = normalization_high;
        }
        mask_value = ((mask_value * scale_shifted) +
            (mask_normalization * (normalization_scale_shifted))) >> kShiftBits;
        // The final picture is masked by the original energy.
        // Assumption: Lower energy can result in low-confidence information and
        // higher energy indicates good confidence.
        // Therefore, pixels with low and high confidence should not be changed
        // greatly.
        uint32_t dst_energy = normal_cdf[
            kCDFEntries * accumulated_histogram[energy] / total_pixels];
        dst_energy = (energy * mask_value + dst_energy * (mask_normalization - mask_value)) /
            mask_normalization;

        // Ensure there is no RGB value will be greater than 255.
        uint32_t max_energy = energy * 255 / MAX3(sp->rgba8[0], sp->rgba8[1], sp->rgba8[2]);
        if (dst_energy > max_energy) {
          dst_energy = max_energy;
        }

        dst_energy = (dst_energy << kShiftBits) / energy;
        uint32_t dst_red = (sp->rgba8[0] * dst_energy) >> kShiftBits;
        uint32_t dst_green = (sp->rgba8[1] * dst_energy) >> kShiftBits;
        uint32_t dst_blue = (sp->rgba8[2] * dst_energy) >> kShiftBits;
        dp->rgba32 = (sp->rgba8[3] << 24) | (dst_blue << 16) | (dst_green << 8) | dst_red;
      }
      dp++;
      sp++;
    }
    src_line = reinterpret_cast<uint32_t const*>(
        reinterpret_cast<char const*>(src_line) + src_info.stride);
    dst_line = reinterpret_cast<pixel32_t*>(
        reinterpret_cast<char*>(dst_line) + dst_info.stride);
  }

  UnlockBitmaps(env, src_bitmap, dst_bitmap);
}

}  // namespace
