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

#ifndef LOOPFILTER_MIPS_H
#define LOOPFILTER_MIPS_H

extern prototype_loopfilter_block(vp8_loop_filter_mbv_mips);
extern prototype_loopfilter_block(vp8_loop_filter_bv_mips);
extern prototype_loopfilter_block(vp8_loop_filter_mbh_mips);
extern prototype_loopfilter_block(vp8_loop_filter_bh_mips);

#define prototype_loopfilter_mips(sym) \
    void sym(unsigned char *src, int pitch, const unsigned int flimit,\
             const unsigned int limit, const unsigned int thresh, int count)

#undef  vp8_lf_normal_mb_v
#define vp8_lf_normal_mb_v vp8_loop_filter_mbv_mips

#undef  vp8_lf_normal_b_v
#define vp8_lf_normal_b_v vp8_loop_filter_bv_mips

#undef  vp8_lf_normal_mb_h
#define vp8_lf_normal_mb_h vp8_loop_filter_mbh_mips

#undef  vp8_lf_normal_b_h
#define vp8_lf_normal_b_h vp8_loop_filter_bh_mips

#endif
#endif
