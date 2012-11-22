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

#include "../include/memory"
#ifndef ANDROID_ASTL_MEMORY__
#error "Wrong header included!!"
#endif
#include "common.h"
#include <iterator>

namespace android {

bool testUnitializedCopyPODRandomIterators() {
    const char *const kSrc = "a string";
    const char *begin = kSrc;
    const int kLen = strlen(kSrc);
    const char *end = begin + kLen;
    char dest[kLen];
    char *const kDest = dest;
    char *res;

    res = std::uninitialized_copy(begin, end, dest);
    EXPECT_TRUE(res == kDest + kLen);

    for (int i = 0; i < kLen; ++i) {
        EXPECT_TRUE(kDest[i] == kSrc[i]);
    }
    return true;
}

bool testUnitializedCopyClassRandomIterators() {
    const size_t kLen = 10;
    const CtorDtorCounter kSrc[10];
    const CtorDtorCounter *begin = kSrc;
    const CtorDtorCounter *end = begin + kLen;
    CtorDtorCounter *dest = new CtorDtorCounter[kLen];
    CtorDtorCounter *const kDest = dest;
    CtorDtorCounter *res;

    CtorDtorCounter::reset();
    res = std::uninitialized_copy(begin, end, dest);
    EXPECT_TRUE(res == kDest + kLen);
    EXPECT_TRUE(kLen == CtorDtorCounter::mCopyCtorCount);
    EXPECT_TRUE(0 == CtorDtorCounter::mCtorCount);

    delete [] dest;
    return true;
}

}  // namespace android

int main(int argc, char **argv)
{
    FAIL_UNLESS(testUnitializedCopyPODRandomIterators);
    FAIL_UNLESS(testUnitializedCopyClassRandomIterators);
    return kPassed;
}
