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

#include "../include/ios_pos_types.h"
#ifndef ANDROID_ASTL_IOS_POS_TYPES__
#error "Wrong header included!!"
#endif
#include "common.h"

#include <limits>

namespace android {
using std::fpos;
using std::streamoff;

bool testConstructor() {
    {
        fpos p;

        EXPECT_TRUE(streamoff(p) == 0);
    }
    {
        fpos p(1000);

        EXPECT_TRUE(streamoff(p) == 1000);
    }
    return true;
}

bool testComparator() {
    {
        fpos p1(100);
        fpos p2(100);

        EXPECT_TRUE(p1 == p2);
        EXPECT_FALSE(p1 != p2);
    }
    {
        fpos p1(100);
        fpos p2(200);

        EXPECT_TRUE(p1 != p2);
        EXPECT_FALSE(p1 == p2);
    }
    return true;
}

bool testIncrDecr() {
    fpos p(100);

    p += 0;
    EXPECT_TRUE(streamoff(p) == 100);

    p -= 0;
    EXPECT_TRUE(streamoff(p) == 100);

    p += 100;
    EXPECT_TRUE(streamoff(p) == 200);

    // overflow -> nop
    p += std::numeric_limits<long long>::max();
    EXPECT_TRUE(streamoff(p) == 200);

    p -= 1000;
    EXPECT_TRUE(streamoff(p) == -800);

    // overflow -> nop
    p -= std::numeric_limits<long long>::min();
    EXPECT_TRUE(streamoff(p) == -800);

    return true;
}


}  // namespace android

int main(int argc, char **argv){
    FAIL_UNLESS(testConstructor);
    FAIL_UNLESS(testComparator);
    FAIL_UNLESS(testIncrDecr);
    return kPassed;
}
