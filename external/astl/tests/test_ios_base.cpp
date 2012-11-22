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

#include "../include/ios_base.h"
#ifndef ANDROID_ASTL_IOS_BASE_H__
#error "Wrong header included!!"
#endif
#include "common.h"

namespace android {
class ios: public std::ios_base {
  public:
};

bool testDefaultPrecision() {
    ios s;
    EXPECT_TRUE(s.precision() == 6);
    return true;
}

bool testSetPrecision() {
    ios s;
    EXPECT_TRUE(s.precision(10) == 6);
    EXPECT_TRUE(s.precision() == 10);
    EXPECT_TRUE(s.precision(-1) == 10); // no-op
    EXPECT_TRUE(s.precision() == 10);
    return true;
}

bool testDefaultWidth() {
    ios s;
    EXPECT_TRUE(s.width() == 0);
    return true;
}

bool testSetWidth() {
    ios s;
    EXPECT_TRUE(s.width(10) == 0);
    EXPECT_TRUE(s.width() == 10);
    EXPECT_TRUE(s.width(-1) == 10); // no-op
    EXPECT_TRUE(s.width() == 10);
    return true;
}

bool testInit() {
    {
        std::ios_base::Init init;
        EXPECT_TRUE(init.done());
    }
    {
        std::ios_base::Init init1;
        EXPECT_TRUE(init1.done());
        std::ios_base::Init init2;
        EXPECT_TRUE(init2.done());
        std::ios_base::Init init3;
        EXPECT_TRUE(init3.done());
    }
    return true;
}

}  // namespace android

int main(int argc, char **argv){
    FAIL_UNLESS(testDefaultPrecision);
    FAIL_UNLESS(testSetPrecision);
    FAIL_UNLESS(testDefaultWidth);
    FAIL_UNLESS(testSetWidth);
    FAIL_UNLESS(testInit);
    return kPassed;
}
