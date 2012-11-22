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
#include "loopfilter.h"
#include "loopfilter_mips.h"
#include "onyxc_int.h"

extern prototype_loopfilter_mips(vp8_loop_filter_horizontal_edge_mips);
extern prototype_loopfilter_mips(vp8_loop_filter_uvhorizontal_edge_mips);
extern prototype_loopfilter_mips(vp8_loop_filter_vertical_edge_mips);
extern prototype_loopfilter_mips(vp8_loop_filter_uvvertical_edge_mips);
extern prototype_loopfilter_mips(vp8_mbloop_filter_horizontal_edge_mips);
extern prototype_loopfilter_mips(vp8_mbloop_filter_uvhorizontal_edge_mips);
extern prototype_loopfilter_mips(vp8_mbloop_filter_vertical_edge_mips);
extern prototype_loopfilter_mips(vp8_mbloop_filter_uvvertical_edge_mips);

/* Horizontal MB filtering */
void vp8_loop_filter_mbh_mips(unsigned char *y_ptr, unsigned char *u_ptr, unsigned char *v_ptr,
                              int y_stride, int uv_stride, loop_filter_info *lfi, int simpler_lpf)
{
    unsigned int thresh_vec, flimit_vec, limit_vec;
    unsigned char thresh, flimit, limit, flimit_temp;

    (void) simpler_lpf;

    /* use direct value instead pointers */
    limit = *(lfi->lim);
    flimit_temp = *(lfi->mbflim);
    thresh = *(lfi->thr);
    flimit = flimit_temp + flimit_temp + limit;

    /* create quad-byte */
    __asm__ __volatile__ (
        "replv.qb       %[thresh_vec], %[thresh]    \n\t"
        "replv.qb       %[flimit_vec], %[flimit]    \n\t"
        "replv.qb       %[limit_vec],  %[limit]     \n\t"
        : [thresh_vec] "=&r" (thresh_vec), [flimit_vec] "=&r" (flimit_vec), [limit_vec] "=r" (limit_vec)
        : [thresh] "r" (thresh), [flimit] "r" (flimit), [limit] "r" (limit)
    );

    vp8_mbloop_filter_horizontal_edge_mips(y_ptr, y_stride, flimit_vec, limit_vec, thresh_vec, 16);

    if (u_ptr) {
        vp8_mbloop_filter_uvhorizontal_edge_mips(u_ptr, uv_stride, flimit_vec, limit_vec, thresh_vec, 0);
    }

    if (v_ptr) {
        vp8_mbloop_filter_uvhorizontal_edge_mips(v_ptr, uv_stride, flimit_vec, limit_vec, thresh_vec, 0);
    }
}


/* Vertical MB Filtering */
void vp8_loop_filter_mbv_mips(unsigned char *y_ptr, unsigned char *u_ptr, unsigned char *v_ptr,
                              int y_stride, int uv_stride, loop_filter_info *lfi, int simpler_lpf)
{
    unsigned int thresh_vec, flimit_vec, limit_vec;
    unsigned char thresh, flimit, limit, flimit_temp;

    (void) simpler_lpf;

    /* use direct value instead pointers */
    limit = *(lfi->lim);
    flimit_temp = *(lfi->mbflim);
    thresh = *(lfi->thr);
    flimit = flimit_temp + flimit_temp + limit;

    /* create quad-byte */
    __asm__ __volatile__ (
        "replv.qb       %[thresh_vec], %[thresh]    \n\t"
        "replv.qb       %[flimit_vec], %[flimit]    \n\t"
        "replv.qb       %[limit_vec],  %[limit]     \n\t"
        : [thresh_vec] "=&r" (thresh_vec), [flimit_vec] "=&r" (flimit_vec), [limit_vec] "=r" (limit_vec)
        : [thresh] "r" (thresh), [flimit] "r" (flimit), [limit] "r" (limit)
    );

    vp8_mbloop_filter_vertical_edge_mips(y_ptr, y_stride, flimit_vec, limit_vec, thresh_vec, 16);

    if (u_ptr)
        vp8_mbloop_filter_uvvertical_edge_mips(u_ptr, uv_stride, flimit_vec, limit_vec, thresh_vec, 0);

    if (v_ptr)
        vp8_mbloop_filter_uvvertical_edge_mips(v_ptr, uv_stride, flimit_vec, limit_vec, thresh_vec, 0);
}


/* Horizontal B Filtering */
void vp8_loop_filter_bh_mips(unsigned char *y_ptr, unsigned char *u_ptr, unsigned char *v_ptr,
                             int y_stride, int uv_stride, loop_filter_info *lfi, int simpler_lpf)
{
    unsigned int thresh_vec, flimit_vec, limit_vec;
    unsigned char thresh, flimit, limit, flimit_temp;

    (void) simpler_lpf;

    /* use direct value instead pointers */
    limit = *(lfi->lim);
    flimit_temp = *(lfi->flim);
    thresh = *(lfi->thr);
    flimit = flimit_temp + flimit_temp + limit;

    /* create quad-byte */
    __asm__ __volatile__ (
        "replv.qb       %[thresh_vec], %[thresh]    \n\t"
        "replv.qb       %[flimit_vec], %[flimit]    \n\t"
        "replv.qb       %[limit_vec],  %[limit]     \n\t"
        : [thresh_vec] "=&r" (thresh_vec), [flimit_vec] "=&r" (flimit_vec), [limit_vec] "=r" (limit_vec)
        : [thresh] "r" (thresh), [flimit] "r" (flimit), [limit] "r" (limit)
    );

    vp8_loop_filter_horizontal_edge_mips(y_ptr + 4 * y_stride, y_stride, flimit_vec, limit_vec, thresh_vec, 16);
    vp8_loop_filter_horizontal_edge_mips(y_ptr + 8 * y_stride, y_stride, flimit_vec, limit_vec, thresh_vec, 16);
    vp8_loop_filter_horizontal_edge_mips(y_ptr + 12 * y_stride, y_stride, flimit_vec, limit_vec, thresh_vec, 16);

    if (u_ptr)
        vp8_loop_filter_uvhorizontal_edge_mips(u_ptr + 4 * uv_stride, uv_stride, flimit_vec, limit_vec, thresh_vec, 0);

    if (v_ptr)
        vp8_loop_filter_uvhorizontal_edge_mips(v_ptr + 4 * uv_stride, uv_stride, flimit_vec, limit_vec, thresh_vec, 0);
}


/* Vertical B Filtering */
void vp8_loop_filter_bv_mips(unsigned char *y_ptr, unsigned char *u_ptr, unsigned char *v_ptr,
                             int y_stride, int uv_stride, loop_filter_info *lfi, int simpler_lpf)
{
    unsigned int thresh_vec, flimit_vec, limit_vec;
    unsigned char thresh, flimit, limit, flimit_temp;

    (void) simpler_lpf;

    /* use direct value instead pointers */
    limit = *(lfi->lim);
    flimit_temp = *(lfi->flim);
    thresh = *(lfi->thr);
    flimit = flimit_temp + flimit_temp + limit;

    /* create quad-byte */
    __asm__ __volatile__ (
        "replv.qb       %[thresh_vec], %[thresh]    \n\t"
        "replv.qb       %[flimit_vec], %[flimit]    \n\t"
        "replv.qb       %[limit_vec],  %[limit]     \n\t"
        : [thresh_vec] "=&r" (thresh_vec), [flimit_vec] "=&r" (flimit_vec), [limit_vec] "=r" (limit_vec)
        : [thresh] "r" (thresh), [flimit] "r" (flimit), [limit] "r" (limit)
    );

    vp8_loop_filter_vertical_edge_mips(y_ptr + 4, y_stride, flimit_vec, limit_vec, thresh_vec, 16);
    vp8_loop_filter_vertical_edge_mips(y_ptr + 8, y_stride, flimit_vec, limit_vec, thresh_vec, 16);
    vp8_loop_filter_vertical_edge_mips(y_ptr + 12, y_stride, flimit_vec, limit_vec, thresh_vec, 16);

    if (u_ptr)
        vp8_loop_filter_uvvertical_edge_mips(u_ptr + 4, uv_stride, flimit_vec, limit_vec, thresh_vec, 0);

    if (v_ptr)
        vp8_loop_filter_uvvertical_edge_mips(v_ptr + 4, uv_stride, flimit_vec, limit_vec, thresh_vec, 0);
}