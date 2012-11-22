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


#include "../include/memory"
#ifndef ANDROID_ASTL_MEMORY__
#error "Wrong header included!!"
#endif
#include "common.h"
#include <algorithm>
#include <cstdlib>

namespace android {
using std::uninitialized_copy;
using std::uninitialized_fill;

bool testCopyPod()
{
    {
        int src[0];
        const int size = ARRAYSIZE(src);
        int dest[10] = {0, };
        EXPECT_TRUE(uninitialized_copy(src, src + size, dest) == dest);
    }
    {
        int src[] = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 };
        const int size = ARRAYSIZE(src);
        int dest[size] = {0, };

        EXPECT_TRUE(uninitialized_copy(src, src + size, dest) == dest + size);

        EXPECT_TRUE(std::equal(src, src + size, dest));
    }
    return true;
}


bool testCopyPodOverflow()
{
    int src, dest;

    // Should not crash
    EXPECT_TRUE(
        uninitialized_copy(&src, &src + kMaxSizeT / sizeof(src) + 1, &dest) ==
        &dest);
    return true;
}

bool testCopyClass()
{
    const size_t kSize = 100;
    CtorDtorCounter::reset();

    CtorDtorCounter src[kSize];
    CtorDtorCounter *dest = static_cast<CtorDtorCounter*>(
        malloc(kSize * sizeof(CtorDtorCounter)));

    EXPECT_TRUE(CtorDtorCounter::mCtorCount == kSize);
    EXPECT_TRUE(CtorDtorCounter::mCopyCtorCount == 0);
    EXPECT_TRUE(CtorDtorCounter::mDtorCount == 0);

    CtorDtorCounter::reset();

    EXPECT_TRUE(uninitialized_copy(src, src + kSize, dest) == dest + kSize);

    EXPECT_TRUE(CtorDtorCounter::mCtorCount == 0);
    EXPECT_TRUE(CtorDtorCounter::mCopyCtorCount == kSize);
    EXPECT_TRUE(CtorDtorCounter::mDtorCount == 0);
    free(dest);
    return true;
}

struct A {};
bool testCopyArray()
{
    {
        A src[0];
        A one;
        A *dest = &one;
        // Empty, dest should not have moved.
        EXPECT_TRUE(uninitialized_copy(src, src, dest) == dest);
    }
    {
        const A src[] = {A()};
        A one;
        A *dest = &one;

        EXPECT_TRUE(uninitialized_copy(src, src + 1, dest) == dest + 1);
    }
    {
        A src[] = {A()};
        A one;
        A *dest = &one;

        EXPECT_TRUE(uninitialized_copy(src, src + 1, dest) == dest + 1);
    }
    {
        const A src[] = {A()};
        A dest[1];

        EXPECT_TRUE(uninitialized_copy(src, src + 1, dest) == dest + 1);
    }
    return true;
}

bool testFillChar()
{
    const char src[] = {'a', 'a', 'a', 'a', 'a', 'a', 'a', 'a', 'a', 'a'};
    const int size = ARRAYSIZE(src);
    char dest[size];

    uninitialized_fill(dest, dest + size, 'a');

    EXPECT_TRUE(std::equal(dest, dest + size, src));
    return true;
}

bool testFillPod()
{
    const int src[] = {10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10};
    const int size = ARRAYSIZE(src);
    int dest[size];

    uninitialized_fill(dest, dest + size, 10);

    EXPECT_TRUE(std::equal(dest, dest + size, src));
    return true;
}

bool testFillClass()
{
    const size_t kSize = 100;
    CtorDtorCounter::reset();

    CtorDtorCounter src;
    CtorDtorCounter *dest = static_cast<CtorDtorCounter*>(
        malloc(kSize * sizeof(CtorDtorCounter)));

    EXPECT_TRUE(CtorDtorCounter::mCtorCount == 1);
    EXPECT_TRUE(CtorDtorCounter::mCopyCtorCount == 0);
    EXPECT_TRUE(CtorDtorCounter::mDtorCount == 0);

    CtorDtorCounter::reset();

    uninitialized_fill(dest, dest + kSize, src);

    EXPECT_TRUE(CtorDtorCounter::mCtorCount == 0);
    EXPECT_TRUE(CtorDtorCounter::mCopyCtorCount == kSize);
    EXPECT_TRUE(CtorDtorCounter::mDtorCount == 0);
    free(dest);
    return true;
}


}  // namespace android

int main(int argc, char **argv)
{
    // copy
    FAIL_UNLESS(testCopyPod);
    FAIL_UNLESS(testCopyPodOverflow);
    FAIL_UNLESS(testCopyClass);
    FAIL_UNLESS(testCopyArray);
    // fill
    FAIL_UNLESS(testFillChar);
    FAIL_UNLESS(testFillPod);
    FAIL_UNLESS(testFillClass);

    return kPassed;
}
