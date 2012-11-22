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

#include "../include/iomanip"
#ifndef ANDROID_ASTL_IOMANIP__
#error "Wrong header included!!"
#endif
#include "common.h"

namespace android {
using std::ios_base;

class os: public std::ostream {
  public:
};

bool testSetPrecision() {
    os s;
    EXPECT_TRUE(s.precision() == 6);
    s << std::setprecision(20);
    EXPECT_TRUE(s.precision() == 20);
    return true;
}

bool testSetBase() {
    typedef std::ios_base::fmtflags fmtflags;
    os s;

    EXPECT_TRUE(s.flags() == (ios_base::dec | ios_base::skipws));
    s << std::setbase(8);
    EXPECT_TRUE(s.flags() == (ios_base::oct | ios_base::skipws));
    s << std::setbase(10);
    EXPECT_TRUE(s.flags() == (ios_base::dec | ios_base::skipws));
    s << std::setbase(16);
    EXPECT_TRUE(s.flags() == (ios_base::hex | ios_base::skipws));
    s << std::setbase(35);
    EXPECT_TRUE(s.flags() == ( ios_base::skipws));
    return true;
}

}  // namespace android

int main(int argc, char **argv){
    FAIL_UNLESS(testSetPrecision);
    FAIL_UNLESS(testSetBase);
    return kPassed;
}
