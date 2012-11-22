/* -*- c++ -*- */
/*
 * Copyright (C) 2010 The Android Open Source Project
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

#include "../include/limits"
#ifndef ANDROID_ASTL_LIMITS__
#error "Wrong header included!!"
#endif
#include "common.h"

namespace android {

bool testSpecialized()
{
    EXPECT_TRUE(std::numeric_limits<float>::is_specialized);
    EXPECT_TRUE(std::numeric_limits<double>::is_specialized);
    EXPECT_TRUE(std::numeric_limits<long>::is_specialized);
    EXPECT_TRUE(std::numeric_limits<long long>::is_specialized);
    return true;
}

bool testMin()
{
    EXPECT_TRUE(std::numeric_limits<float>::min() == __FLT_MIN__);
    EXPECT_TRUE(std::numeric_limits<double>::min() == __DBL_MIN__);
    EXPECT_TRUE(std::numeric_limits<long>::min() == LONG_MIN);
    return true;
}

bool testMax()
{
    EXPECT_TRUE(std::numeric_limits<float>::max() == __FLT_MAX__);
    EXPECT_TRUE(std::numeric_limits<double>::max() == __DBL_MAX__);
    EXPECT_TRUE(std::numeric_limits<long>::max() == LONG_MAX);
    EXPECT_TRUE(std::numeric_limits<long long>::max() == LLONG_MAX);
    return true;
}

bool testSigned()
{
    EXPECT_TRUE(std::numeric_limits<float>::is_signed);
    EXPECT_TRUE(std::numeric_limits<double>::is_signed);
    EXPECT_TRUE(std::numeric_limits<long>::is_signed);
    EXPECT_TRUE(std::numeric_limits<long long>::is_signed);
    return true;
}

bool testIsInteger()
{
    EXPECT_FALSE(std::numeric_limits<float>::is_integer);
    EXPECT_FALSE(std::numeric_limits<double>::is_integer);
    EXPECT_TRUE(std::numeric_limits<long>::is_integer);
    EXPECT_TRUE(std::numeric_limits<long long>::is_integer);
    return true;
}

bool testDigits()
{
    EXPECT_TRUE(std::numeric_limits<long>::digits == 32);
    EXPECT_TRUE(std::numeric_limits<long>::digits10 == 10);
    EXPECT_TRUE(std::numeric_limits<long long>::digits == 64);
    EXPECT_TRUE(std::numeric_limits<long long>::digits10 == 19);
    return true;
}

}  // namespace android

int main(int argc, char **argv)
{
    FAIL_UNLESS(testSpecialized);
    FAIL_UNLESS(testMin);
    FAIL_UNLESS(testMax);
    FAIL_UNLESS(testSigned);
    FAIL_UNLESS(testIsInteger);
    FAIL_UNLESS(testDigits);
    return kPassed;
}
