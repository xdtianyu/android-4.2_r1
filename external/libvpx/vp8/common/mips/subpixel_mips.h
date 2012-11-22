/*
 *  Copyright (c) 2010 The WebM project authors. All Rights Reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */


#if !CONFIG_RUNTIME_CPU_DETECT

#ifndef SUBPIXEL_MIPS_H
#define SUBPIXEL_MIPS_H

extern prototype_subpixel_predict(vp8_sixtap_predict16x16_mips);
extern prototype_subpixel_predict(vp8_sixtap_predict8x8_mips);
extern prototype_subpixel_predict(vp8_sixtap_predict8x4_mips);
extern prototype_subpixel_predict(vp8_sixtap_predict_mips);
extern void dsputil_static_init(void);

#undef  vp8_subpix_sixtap16x16
#define vp8_subpix_sixtap16x16 vp8_sixtap_predict16x16_mips

#undef  vp8_subpix_sixtap8x8
#define vp8_subpix_sixtap8x8 vp8_sixtap_predict8x8_mips

#undef  vp8_subpix_sixtap8x4
#define vp8_subpix_sixtap8x4 vp8_sixtap_predict8x4_mips

#undef  vp8_subpix_sixtap4x4
#define vp8_subpix_sixtap4x4 vp8_sixtap_predict_mips

#endif
#endif