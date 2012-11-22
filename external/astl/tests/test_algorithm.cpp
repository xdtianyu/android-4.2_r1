/* -*- c++ -*- */
/*
 * Copyright (C) 2009 The Android Open Source Project
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 * AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */

#include "../include/algorithm"
#ifndef ANDROID_ASTL_ALGORITHM__
#error "Wrong header included!!"
#endif
#include "common.h"

namespace android {

bool testSwapInt()
{
    int a = 5;
    int b = 10;

    std::swap(a, b);
    EXPECT_TRUE(a == 10);
    EXPECT_TRUE(b == 5);
    return true;
}

bool testMin()
{
    int a = 5;
    int b = 10;

    int c = std::min(a, b);
    EXPECT_TRUE(c == 5);
    c = std::min(b, a);
    EXPECT_TRUE(c == 5);
    return true;
}

bool testMax()
{
    int a = 5;
    int b = 10;

    int c = std::max(a, b);
    EXPECT_TRUE(c == 10);
    c = std::max(b, a);
    EXPECT_TRUE(c == 10);
    return true;
}

bool testFill()
{
    int array[10];

    std::fill(array, array + 10, 0xaa);
    EXPECT_TRUE(array[0] == 0xaa);
    EXPECT_TRUE(array[1] == 0xaa);
    EXPECT_TRUE(array[2] == 0xaa);
    EXPECT_TRUE(array[3] == 0xaa);
    EXPECT_TRUE(array[4] == 0xaa);
    EXPECT_TRUE(array[5] == 0xaa);
    EXPECT_TRUE(array[6] == 0xaa);
    EXPECT_TRUE(array[7] == 0xaa);
    EXPECT_TRUE(array[8] == 0xaa);
    EXPECT_TRUE(array[9] == 0xaa);
    return true;
}

bool testFill_N()
{
    int array[10];

    int *ret = std::fill_n(array, 10, 0xaa);
    EXPECT_TRUE(array[0] == 0xaa);
    EXPECT_TRUE(array[1] == 0xaa);
    EXPECT_TRUE(array[2] == 0xaa);
    EXPECT_TRUE(array[3] == 0xaa);
    EXPECT_TRUE(array[4] == 0xaa);
    EXPECT_TRUE(array[5] == 0xaa);
    EXPECT_TRUE(array[6] == 0xaa);
    EXPECT_TRUE(array[7] == 0xaa);
    EXPECT_TRUE(array[8] == 0xaa);
    EXPECT_TRUE(array[9] == 0xaa);
    EXPECT_TRUE(ret == array + 10);

    char array2[1] = { '\0' };
    signed char sc = 1;

    std::fill_n(array2, 1, sc);
    EXPECT_TRUE(array2[0] == 1 );

    return true;
}

struct Left1 { };
struct Right1 { };
bool operator==(const Left1&, const Right1&) {return true;}

struct Left2 { };
struct Right2 { };
bool predicate(const Left2&, const Right2&) {return true;}

bool testEqual()
{
    Left1 left1;
    Right1 right1;
    Left2 left2;
    Right2 right2;

    EXPECT_TRUE(std::equal(&left1, &left1, &right1));
    EXPECT_TRUE(std::equal(&left2, &left2, &right2, predicate));
    return true;
}

bool testCopy()
{
    {
        int data[] = {1,2,3,4,5,6};
        std::copy(data + 2, data + 5, data);
        EXPECT_TRUE(data[0] == 3);
        EXPECT_TRUE(data[1] == 4);
        EXPECT_TRUE(data[2] == 5);
        EXPECT_TRUE(data[3] == 4);
        EXPECT_TRUE(data[4] == 5);
        EXPECT_TRUE(data[5] == 6);
    }
    return true;
}

}  // namespace android

int main(int argc, char **argv)
{
    FAIL_UNLESS(testSwapInt);
    FAIL_UNLESS(testMin);
    FAIL_UNLESS(testMax);
    FAIL_UNLESS(testFill);
    FAIL_UNLESS(testFill_N);
    FAIL_UNLESS(testEqual);
    FAIL_UNLESS(testCopy);
    return kPassed;
}
