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

#include "../include/streambuf"
#ifndef ANDROID_ASTL_STREAMBUF__
#error "Wrong header included!!"
#endif
#include "common.h"

#include <char_traits.h>

namespace android {
using std::char_traits;

class streambuf: public std::streambuf {
  public:
    streambuf() {
        setp(mBuffer, mBuffer + sizeof(mBuffer));
        traits_type::assign(mBuffer, sizeof(mBuffer), 'X');
    }

    char mBuffer[5];
};

bool testSputc() {
    streambuf buf;

    EXPECT_TRUE(buf.sputc('A') == 65);
    EXPECT_TRUE(buf.sputc('B') == 66);
    EXPECT_TRUE(buf.sputc('C') == 67);
    EXPECT_TRUE(buf.sputc('D') == 68);
    EXPECT_TRUE(buf.sputc('E') == 69);
    // TODO: The sputc implementation has been changed to use
    // sputn. This is non standard so disabling the tests below for
    // now.
    //    EXPECT_TRUE(buf.sputc('F') == char_traits<char>::eof());
    //    EXPECT_TRUE(buf.sputc('G') == char_traits<char>::eof());
    return true;
}

bool testSputn() {
    streambuf buf;

    EXPECT_TRUE(buf.sputn("ABCDE", 5) == 5);
    EXPECT_TRUE(buf.sputn("F", 1) == 0);
    return true;
}

}  // namespace android

int main(int argc, char **argv){
    FAIL_UNLESS(testSputc);
    FAIL_UNLESS(testSputn);
    return kPassed;
}
