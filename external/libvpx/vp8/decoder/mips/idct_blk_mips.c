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
#include "idct.h"
#include "dequantize.h"


void vp8_dequant_dc_idct_add_y_block_mips
            (short *q, short *dq, unsigned char *pre,
             unsigned char *dst, int stride, char *eobs, short *dc)
{
    int i, j;
    int higher = 4*stride - 12;

    /* unroll the loop */
    for (i = 0; i < 4; i++)
    {
        if (*eobs++ > 1)
            vp8_dequant_dc_idct_add_mips(q, dq, pre, dst, 16, stride, dc[0]);
        else
            vp8_dc_only_idct_add_mips(dc[0], pre, dst, 16, stride);

        q   += 16;
        pre += 4;
        dst += 4;

        if (*eobs++ > 1)
            vp8_dequant_dc_idct_add_mips(q, dq, pre, dst, 16, stride, dc[1]);
        else
            vp8_dc_only_idct_add_mips(dc[1], pre, dst, 16, stride);

        q   += 16;
        pre += 4;
        dst += 4;

        if (*eobs++ > 1)
            vp8_dequant_dc_idct_add_mips(q, dq, pre, dst, 16, stride, dc[2]);
        else
            vp8_dc_only_idct_add_mips(dc[2], pre, dst, 16, stride);

        q   += 16;
        pre += 4;
        dst += 4;

        if (*eobs++ > 1)
            vp8_dequant_dc_idct_add_mips(q, dq, pre, dst, 16, stride, dc[3]);
        else
            vp8_dc_only_idct_add_mips(dc[3], pre, dst, 16, stride);

        q   += 16;
        dc += 4;
        pre += 52;
        dst += higher;
    }
}


void vp8_dequant_idct_add_y_block_mips
            (short *q, short *dq, unsigned char *pre,
             unsigned char *dst, int stride, char *eobs)
{
    int i, j;
    int higher = 4*stride -12;

    /* unroll the loop */
    for (i = 4; i--; )
    {
        if (*eobs++ > 1)
            vp8_dequant_idct_add_mips(q, dq, pre, dst, 16, stride);
        else
        {
            vp8_dc_only_idct_add_mips(q[0]*dq[0], pre, dst, 16, stride);
            ((int *)q)[0] = 0;
        }

        q   += 16;
        pre += 4;
        dst += 4;

        if (*eobs++ > 1)
            vp8_dequant_idct_add_mips(q, dq, pre, dst, 16, stride);
        else
        {
            vp8_dc_only_idct_add_mips(q[0]*dq[0], pre, dst, 16, stride);
            ((int *)q)[0] = 0;
        }

        q   += 16;
        pre += 4;
        dst += 4;

        if (*eobs++ > 1)
            vp8_dequant_idct_add_mips(q, dq, pre, dst, 16, stride);
        else
        {
            vp8_dc_only_idct_add_mips(q[0]*dq[0], pre, dst, 16, stride);
            ((int *)q)[0] = 0;
        }

        q   += 16;
        pre += 4;
        dst += 4;

        if (*eobs++ > 1)
            vp8_dequant_idct_add_mips(q, dq, pre, dst, 16, stride);
        else
        {
            vp8_dc_only_idct_add_mips(q[0]*dq[0], pre, dst, 16, stride);
            ((int *)q)[0] = 0;
        }

        q   += 16;
        pre += 52;
        dst += higher;
    }
}


void vp8_dequant_idct_add_uv_block_mips
            (short *q, short *dq, unsigned char *pre,
             unsigned char *dstu, unsigned char *dstv, int stride, char *eobs)
{
    int i, j;
    int higher = 4*stride -4;

    /* unroll the loops */
    if (*eobs++ > 1)
        vp8_dequant_idct_add_mips(q, dq, pre, dstu, 8, stride);
    else
    {
        vp8_dc_only_idct_add_mips(q[0]*dq[0], pre, dstu, 8, stride);
        ((int *)q)[0] = 0;
    }

    q    += 16;
    pre  += 4;
    dstu += 4;

    if (*eobs++ > 1)
        vp8_dequant_idct_add_mips(q, dq, pre, dstu, 8, stride);
    else
    {
        vp8_dc_only_idct_add_mips(q[0]*dq[0], pre, dstu, 8, stride);
        ((int *)q)[0] = 0;
    }

    q    += 16;
    pre  += 28;
    dstu += higher;

    if (*eobs++ > 1)
        vp8_dequant_idct_add_mips(q, dq, pre, dstu, 8, stride);
    else
    {
        vp8_dc_only_idct_add_mips(q[0]*dq[0], pre, dstu, 8, stride);
        ((int *)q)[0] = 0;
    }

    q    += 16;
    pre  += 4;
    dstu += 4;

    if (*eobs++ > 1)
        vp8_dequant_idct_add_mips(q, dq, pre, dstu, 8, stride);
    else
    {
        vp8_dc_only_idct_add_mips(q[0]*dq[0], pre, dstu, 8, stride);
        ((int *)q)[0] = 0;
    }

    q    += 16;
    pre  += 28;

    if (*eobs++ > 1)
        vp8_dequant_idct_add_mips(q, dq, pre, dstv, 8, stride);
    else
    {
        vp8_dc_only_idct_add_mips(q[0]*dq[0], pre, dstv, 8, stride);
        ((int *)q)[0] = 0;
    }

    q    += 16;
    pre  += 4;
    dstv += 4;

    if (*eobs++ > 1)
        vp8_dequant_idct_add_mips(q, dq, pre, dstv, 8, stride);
    else
    {
        vp8_dc_only_idct_add_mips(q[0]*dq[0], pre, dstv, 8, stride);
        ((int *)q)[0] = 0;
    }

    q    += 16;
    pre  += 28;
    dstv += higher;

    if (*eobs++ > 1)
        vp8_dequant_idct_add_mips(q, dq, pre, dstv, 8, stride);
    else
    {
        vp8_dc_only_idct_add_mips(q[0]*dq[0], pre, dstv, 8, stride);
        ((int *)q)[0] = 0;
    }

    q    += 16;
    pre  += 4;
    dstv += 4;

    if (*eobs++ > 1)
        vp8_dequant_idct_add_mips(q, dq, pre, dstv, 8, stride);
    else
    {
        vp8_dc_only_idct_add_mips(q[0]*dq[0], pre, dstv, 8, stride);
        ((int *)q)[0] = 0;
    }
}