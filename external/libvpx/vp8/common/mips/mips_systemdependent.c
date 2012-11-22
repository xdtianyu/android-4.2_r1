/*
 *  Copyright (c) 2010 The WebM project authors. All Rights Reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */


#include "vpx_ports/config.h"
#include "g_common.h"
#include "subpixel.h"
#include "loopfilter.h"
#include "recon.h"
#include "idct.h"
#include "pragmas.h"
#include "onyxc_int.h"

void vp8_arch_mips_common_init(VP8_COMMON *ctx)
{

#if CONFIG_RUNTIME_CPU_DETECT

    VP8_COMMON_RTCD *rtcd = &ctx->rtcd;
#ifdef MIPS_DSP_REV
#if (MIPS_DSP_REV>=1)
    rtcd->subpix.sixtap16x16     = vp8_sixtap_predict16x16_mips;
    rtcd->subpix.sixtap8x8       = vp8_sixtap_predict8x8_mips;
    rtcd->subpix.sixtap8x4       = vp8_sixtap_predict8x4_mips;
    rtcd->subpix.sixtap4x4       = vp8_sixtap_predict_mips;

    rtcd->recon.copy16x16        = vp8_copy_mem16x16_mips;
    rtcd->recon.copy8x8          = vp8_copy_mem8x8_mips;
    rtcd->recon.copy8x4          = vp8_copy_mem8x4_mips;

    rtcd->idct.idct1_scalar_add  = vp8_dc_only_idct_add_mips;
    rtcd->idct.iwalsh1           = vp8_short_inv_walsh4x4_1_mips;
    rtcd->idct.idct16            = vp8_short_idct4x4llm_mips;
    rtcd->idct.iwalsh16          = vp8_short_inv_walsh4x4_mips;

#if (MIPS_DSP_REV>=2)
    rtcd->loopfilter.normal_mb_v = vp8_loop_filter_mbv_mips;
    rtcd->loopfilter.normal_b_v  = vp8_loop_filter_bv_mips;
    rtcd->loopfilter.normal_mb_h = vp8_loop_filter_mbh_mips;
    rtcd->loopfilter.normal_b_h  = vp8_loop_filter_bh_mips;

#endif
#endif

#endif
#endif
}
