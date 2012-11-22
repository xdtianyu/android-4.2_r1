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

#ifndef IDCT_MIPS_H
#define IDCT_MIPS_H

extern prototype_idct(vp8_short_idct4x4llm_mips);
extern prototype_second_order(vp8_short_inv_walsh4x4_mips);
extern prototype_idct_scalar_add(vp8_dc_only_idct_add_mips);
extern prototype_second_order(vp8_short_inv_walsh4x4_1_mips);

#undef  vp8_idct_idct16
#define vp8_idct_idct16 vp8_short_idct4x4llm_mips

#undef  vp8_idct_iwalsh16
#define vp8_idct_iwalsh16 vp8_short_inv_walsh4x4_mips

#undef  vp8_idct_idct1_scalar_add
#define vp8_idct_idct1_scalar_add vp8_dc_only_idct_add_mips

#undef  vp8_idct_iwalsh1
#define vp8_idct_iwalsh1 vp8_short_inv_walsh4x4_1_mips

#endif
#endif