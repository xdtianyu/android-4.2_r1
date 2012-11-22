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

#ifndef PHOTOEDITOR_JNI_CONVOLUTION_H_
#define PHOTOEDITOR_JNI_CONVOLUTION_H_

#include <android/bitmap.h>

namespace android {
namespace apps {
namespace photoeditor {
namespace convolution {

void SpecialConvolution(AndroidBitmapInfo *src_info, AndroidBitmapInfo *dst_info,
    void *src_pixels, void *dst_pixels, float neighbor);

}  // namespace convolution
}  // namespace photoeditor
}  // namespace apps
}  // namespace android

#endif  // PHOTOEDITOR_JNI_CONVOLUTION_H_
