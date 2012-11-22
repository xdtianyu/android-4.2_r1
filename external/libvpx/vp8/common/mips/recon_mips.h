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

#ifndef RECON_MIPS_H
#define RECON_MIPS_H

extern prototype_copy_block(vp8_copy_mem8x8_mips);
extern prototype_copy_block(vp8_copy_mem8x4_mips);
extern prototype_copy_block(vp8_copy_mem16x16_mips);

#ifndef MUST_BE_ALIGNED
#define MUST_BE_ALIGNED
#endif

#undef  vp8_recon_copy8x8
#define vp8_recon_copy8x8 vp8_copy_mem8x8_mips

#undef  vp8_recon_copy8x4
#define vp8_recon_copy8x4 vp8_copy_mem8x4_mips

#undef  vp8_recon_copy16x16
#define vp8_recon_copy16x16 vp8_copy_mem16x16_mips

#endif
#endif