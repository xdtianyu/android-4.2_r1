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
using android::apps::photoeditor::utils::pixel32_t;
using android::apps::photoeditor::utils::UnlockBitmaps;

namespace {

const int map[256] = {
    0xff000000,0xff010101,0xff030201,0xff040302,0xff050303,0xff060403,0xff080504,0xff090605,
    0xff0a0705,0xff0c0806,0xff0d0907,0xff0e0907,0xff0f0a08,0xff110b09,0xff120c09,0xff130d0a,
    0xff140e0b,0xff160f0b,0xff17100c,0xff18100d,0xff1a110d,0xff1b120e,0xff1c130f,0xff1d140f,
    0xff1f1510,0xff201611,0xff211711,0xff221812,0xff241913,0xff251a13,0xff261b14,0xff271c15,
    0xff291d15,0xff2a1e16,0xff2b1f17,0xff2c2017,0xff2e2118,0xff2f2119,0xff30221a,0xff31231a,
    0xff32241b,0xff34251c,0xff35261c,0xff36271d,0xff37281e,0xff38291e,0xff3a2a1f,0xff3b2b20,
    0xff3c2c20,0xff3d2d21,0xff3e2e22,0xff402f23,0xff413023,0xff423124,0xff433225,0xff443325,
    0xff463426,0xff473527,0xff483628,0xff493728,0xff4a3829,0xff4b392a,0xff4c3a2b,0xff4e3b2b,
    0xff4f3c2c,0xff503d2d,0xff513e2d,0xff523f2e,0xff53402f,0xff544130,0xff554130,0xff564231,
    0xff574332,0xff594433,0xff5a4533,0xff5b4634,0xff5c4735,0xff5d4836,0xff5e4937,0xff5f4a37,
    0xff604b38,0xff614c39,0xff624c3a,0xff634d3a,0xff644e3b,0xff654f3c,0xff66503d,0xff67513e,
    0xff68523e,0xff69533f,0xff6a5340,0xff6b5441,0xff6c5542,0xff6d5642,0xff6e5743,0xff6f5844,
    0xff705845,0xff715946,0xff715a47,0xff725b47,0xff735c48,0xff745c49,0xff755d4a,0xff765e4b,
    0xff775f4c,0xff785f4c,0xff79604d,0xff7a614e,0xff7a624f,0xff7b6350,0xff7c6351,0xff7d6452,
    0xff7e6552,0xff7f6653,0xff806654,0xff806755,0xff816856,0xff826957,0xff836958,0xff846a59,
    0xff856b5a,0xff856c5a,0xff866c5b,0xff876d5c,0xff886e5d,0xff896f5e,0xff896f5f,0xff8a7060,
    0xff8b7161,0xff8c7262,0xff8d7263,0xff8d7364,0xff8e7465,0xff8f7565,0xff907566,0xff907667,
    0xff917768,0xff927869,0xff93796a,0xff94796b,0xff947a6c,0xff957b6d,0xff967c6e,0xff977c6f,
    0xff977d70,0xff987e71,0xff997f72,0xff9a8073,0xff9a8074,0xff9b8175,0xff9c8276,0xff9d8377,
    0xff9d8478,0xff9e8479,0xff9f857a,0xffa0867b,0xffa0877c,0xffa1887d,0xffa2897e,0xffa3897f,
    0xffa38a80,0xffa48b81,0xffa58c82,0xffa68d83,0xffa68e84,0xffa78f85,0xffa89086,0xffa89087,
    0xffa99188,0xffaa9289,0xffab938a,0xffab948b,0xffac958c,0xffad968e,0xffae978f,0xffae9890,
    0xffaf9991,0xffb09992,0xffb19a93,0xffb19b94,0xffb29c95,0xffb39d96,0xffb49e97,0xffb49f98,
    0xffb5a099,0xffb6a19a,0xffb7a29c,0xffb8a39d,0xffb8a49e,0xffb9a59f,0xffbaa6a0,0xffbba7a1,
    0xffbba8a2,0xffbca9a3,0xffbdaaa4,0xffbeaba5,0xffbfaba7,0xffbfaca8,0xffc0ada9,0xffc1aeaa,
    0xffc2afab,0xffc3b0ac,0xffc3b1ad,0xffc4b2ae,0xffc5b3af,0xffc6b4b1,0xffc7b5b2,0xffc7b6b3,
    0xffc8b7b4,0xffc9b8b5,0xffcab9b6,0xffcbbab7,0xffccbbb8,0xffcdbcba,0xffcdbdbb,0xffcebebc,
    0xffcfbfbd,0xffd0c0be,0xffd1c1bf,0xffd2c3c0,0xffd3c4c2,0xffd3c5c3,0xffd4c6c4,0xffd5c7c5,
    0xffd6c8c6,0xffd7c9c7,0xffd8cac8,0xffd9cbca,0xffdacccb,0xffdacdcc,0xffdbcecd,0xffdccfce,
    0xffddd0cf,0xffded1d1,0xffdfd2d2,0xffe0d3d3,0xffe1d4d4,0xffe2d5d5,0xffe3d6d6,0xffe3d7d7,
    0xffe4d8d9,0xffe5d9da,0xffe6dadb,0xffe7dbdc,0xffe8dcdd,0xffe9ddde,0xffeadfe0,0xffebe0e1,
    0xffece1e2,0xffede2e3,0xffede3e4,0xffeee4e5,0xffefe5e7,0xfff0e6e8,0xfff1e7e9,0xfff2e8ea
};

extern "C" JNIEXPORT void JNICALL Java_com_android_photoeditor_filters_ImageUtils_nativeWarmify(
    JNIEnv *env, jobject obj, jobject src_bitmap, jobject dst_bitmap) {
   pWarmifyType f = (pWarmifyType)JNIFunc[JNI_Warmify].func_ptr;
   return f(env, obj, src_bitmap, dst_bitmap);
}
   
extern "C" void Warmify(
    JNIEnv *env, jobject obj, jobject src_bitmap, jobject dst_bitmap) {
  AndroidBitmapInfo src_info;
  AndroidBitmapInfo dst_info;
  void* src_pixels;
  void* dst_pixels;

  int ret = LockBitmaps(
      env, src_bitmap, dst_bitmap, &src_info, &dst_info, &src_pixels, &dst_pixels);
  if (ret < 0) {
    LOGE("LockBitmaps in nativeWarmify failed, error=%d", ret);
    return;
  }

  for (uint32_t scan_line = 0; scan_line < dst_info.height; scan_line++) {
    uint32_t* dst = reinterpret_cast<uint32_t*>(dst_pixels);
    pixel32_t* src = reinterpret_cast<pixel32_t*>(src_pixels);
    pixel32_t* src_line_end = src + src_info.width;

    while (src < src_line_end) {
      int dst_red = (map[src->rgba8[0]] & 0xFF0000) >> 16;
      int dst_green = map[src->rgba8[1]] & 0xFF00;
      int dst_blue = (map[src->rgba8[2]] & 0xFF) << 16;

      *dst = (src->rgba8[3] << 24) | dst_blue | dst_green | dst_red;

      src++;
      dst++;
    }
    src_pixels = reinterpret_cast<char*>(src_pixels) + src_info.stride;
    dst_pixels = reinterpret_cast<char*>(dst_pixels) + dst_info.stride;
  }

  UnlockBitmaps(env, src_bitmap, dst_bitmap);
}

}  // namespace
