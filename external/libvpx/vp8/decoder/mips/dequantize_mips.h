/*
 *  Copyright (c) 2010 The WebM project authors. All Rights Reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */


#ifndef DEQUANTIZE_MIPS_H
#define DEQUANTIZE_MIPS_H

extern prototype_dequant_idct_add(vp8_dequant_idct_add_mips);
extern prototype_dequant_dc_idct_add(vp8_dequant_dc_idct_add_mips);
extern prototype_dequant_dc_idct_add_y_block(vp8_dequant_dc_idct_add_y_block_mips);
extern prototype_dequant_idct_add_y_block(vp8_dequant_idct_add_y_block_mips);
extern prototype_dequant_idct_add_uv_block(vp8_dequant_idct_add_uv_block_mips);

#if !CONFIG_RUNTIME_CPU_DETECT
#undef vp8_dequant_idct_add
#define vp8_dequant_idct_add vp8_dequant_idct_add_mips

#undef vp8_dequant_dc_idct_add
#define vp8_dequant_dc_idct_add vp8_dequant_dc_idct_add_mips

#undef vp8_dequant_dc_idct_add_y_block
#define vp8_dequant_dc_idct_add_y_block vp8_dequant_dc_idct_add_y_block_mips

#undef vp8_dequant_idct_add_y_block
#define vp8_dequant_idct_add_y_block vp8_dequant_idct_add_y_block_mips

#undef vp8_dequant_idct_add_uv_block
#define vp8_dequant_idct_add_uv_block vp8_dequant_idct_add_uv_block_mips

#endif
#endif