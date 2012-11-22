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
#include "dequantize.h"
#include "idct.h"
#include "vpx_mem/vpx_mem.h"

DECLARE_ALIGNED(8, const unsigned char, cma[512]) = {
0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11,
12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32,
33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53,
54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74,
75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95,
96, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113,
114, 115, 116, 117, 118, 119, 120, 121, 122, 123, 124, 125, 126, 127, 128, 129, 130,
131, 132, 133, 134, 135, 136, 137, 138, 139, 140, 141, 142, 143, 144, 145, 146, 147,
148, 149, 150, 151, 152, 153, 154, 155, 156, 157, 158, 159, 160, 161, 162, 163, 164,
165, 166, 167, 168, 169, 170, 171, 172, 173, 174, 175, 176, 177, 178, 179, 180, 181,
182, 183, 184, 185, 186, 187, 188, 189, 190, 191, 192, 193, 194, 195, 196, 197, 198,
199, 200, 201, 202, 203, 204, 205, 206, 207, 208, 209, 210, 211, 212, 213, 214, 215,
216, 217, 218, 219, 220, 221, 222, 223, 224, 225, 226, 227, 228, 229, 230, 231, 232,
233, 234, 235, 236, 237, 238, 239, 240, 241, 242, 243, 244, 245, 246, 247, 248, 249,
250, 251, 252, 253, 254, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255
};

extern void vp8_short_idct4x4llm_mips(short *input, short *output, int pitch) ;
extern void vp8_short_idct4x4llm_1_mips(short *input, short *output, int pitch);


void vp8_dequant_idct_add_mips(short *input, short *dq, unsigned char *pred,
                            unsigned char *dest, int pitch, int stride)
{
    short output[16];
    short *diff_ptr = output;
    int i;
    short a1, a2, a3, a0;

    short input_temp[16];
    unsigned int in1, dq1, x1, in2, dq2;
    const unsigned char *cm = &(cma[128]);

    __asm__ __volatile__ (
        "lh            %[in1], 0(%[input])                \n\t"
        "lh            %[dq1], 0(%[dq])                   \n\t"
        "lh            %[in2], 2(%[input])                \n\t"
        "lh            %[dq2], 2(%[dq])                   \n\t"
        "append        %[in1], %[in2],          16        \n\t"
        "append        %[dq1], %[dq2],          16        \n\t"
        "mul.ph        %[x1],  %[dq1],          %[in1]    \n\t"
        "sh            %[x1],  2(%[input_temp])           \n\t"
        "srl           %[x1],  %[x1], 16                  \n\t"
        "sh            %[x1],  0(%[input_temp])           \n\t"

        : [x1] "=&r" (x1), [in1] "=&r" (in1), [dq1] "=&r" (dq1),
          [in2] "=&r" (in2), [dq2] "=&r" (dq2)
        : [dq] "r" (dq), [input] "r" (input),
          [input_temp] "r" (input_temp)
    );

    for (i = 2; i < 16; i++)
    {
        input_temp[i] = dq[i] * input[i];
    }

    /* the idct halves ( >> 1) the pitch */
    vp8_short_idct4x4llm_mips(input_temp, output, 4);

    vpx_memset(input, 0, 32);

    /* unroll the loop */
    for (i = 4; i--; )
    {
        a0 = diff_ptr[0] + pred[0];
        a1 = diff_ptr[1] + pred[1];
        a2 = diff_ptr[2] + pred[2];
        a3 = diff_ptr[3] + pred[3];

        dest[0] = cm[a0];
        dest[1] = cm[a1];
        dest[2] = cm[a2];
        dest[3] = cm[a3];

        dest += stride;
        diff_ptr += 4;
        pred += pitch;
    }
}


void vp8_dequant_dc_idct_add_mips(short *input, short *dq, unsigned char *pred,
                               unsigned char *dest, int pitch, int stride,
                               int Dc)
{
    int i;
    short output[16];
    short *diff_ptr = output;
    short input_temp[16];
    short a1, a2, a3, a0;

    unsigned int in1, dq1, x1, in2, dq2;
    const unsigned char *cm = &(cma[128]);

    input_temp[0] = (short)Dc;

    __asm__ __volatile__ (
        "lh            %[in1], 2(%[input])                \n\t"
        "lh            %[dq1], 2(%[dq])                   \n\t"
        "lh            %[in2], 4(%[input])                \n\t"
        "lh            %[dq2], 4(%[dq])                   \n\t"
        "append        %[in1], %[in2],          16        \n\t"
        "append        %[dq1], %[dq2],          16        \n\t"
        "mul.ph        %[x1],  %[dq1],          %[in1]    \n\t"
        "sh            %[x1],  4(%[input_temp])           \n\t"
        "srl           %[x1],  %[x1], 16                  \n\t"
        "sh            %[x1],  2(%[input_temp])           \n\t"

        : [x1] "=&r" (x1), [in1] "=&r" (in1), [dq1] "=&r" (dq1),
          [in2] "=&r" (in2), [dq2] "=&r" (dq2)
        : [dq] "r" (dq), [input] "r" (input),
          [input_temp] "r" (input_temp)
    );

    for (i = 3; i < 16; i++)
    {
        input_temp[i] = dq[i] * input[i];
    }

    vp8_short_idct4x4llm_mips(input_temp, output, 4);

    vpx_memset(input, 0, 32);

    for (i = 4; i--; )
    {
        a0 = diff_ptr[0] + pred[0];
        a1 = diff_ptr[1] + pred[1];
        a2 = diff_ptr[2] + pred[2];
        a3 = diff_ptr[3] + pred[3];

        dest[0] = cm[a0];
        dest[1] = cm[a1];
        dest[2] = cm[a2];
        dest[3] = cm[a3];

        dest += stride;
        diff_ptr += 4;
        pred += pitch;
    }
}