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

#include "../include/sstream"
#ifndef ANDROID_ASTL_SSTREAM__
#error "Wrong header included!!"
#endif
#include "common.h"

#include <ios_base.h>
#include <string>

namespace android {
using std::stringbuf;
using std::stringstream;
using std::string;

bool testConstructor() {
    {
        string str("Get out of here and get me some money too.");
        stringbuf buf1(str);
        stringbuf buf2(str, std::ios_base::in);
        stringbuf buf3(str, std::ios_base::out);

        EXPECT_TRUE(buf1.str() == str);
        EXPECT_TRUE(buf2.str() == str);
        EXPECT_TRUE(buf3.str() == str);
    }
    return true;
}

bool testInAvail() {
    {
        string str("Get out of here and get me some money too.");
        stringbuf buf1(str);
        stringbuf buf2(str, std::ios_base::in);
        stringbuf buf3(str, std::ios_base::out);
        stringbuf buf4;

        std::streamsize len1 = buf1.in_avail();
        std::streamsize len2 = buf2.in_avail();
        std::streamsize len3 = buf3.in_avail();
        std::streamsize len4 = buf4.in_avail();

        EXPECT_TRUE(len1 > 0);
        EXPECT_TRUE(len2 > 0);
        EXPECT_TRUE(len3 == -1); // out only
        EXPECT_TRUE(len4 == 0); // out only
    }
    return true;
}

bool testNulChar() {
    string str("String with \0 in the middle", 27);
    stringbuf buf(str);

    EXPECT_TRUE(buf.in_avail() == 27);
    EXPECT_TRUE(buf.str().size() == 27);
    return true;
}

bool testPut() {
    stringbuf buf;

    buf.sputc('A');
    buf.sputc('B');
    buf.sputc('C');
    buf.sputc('D');
    EXPECT_TRUE(buf.str() == "ABCD");

    buf.sputn(" alphabet", 9);
    EXPECT_TRUE(buf.str() == "ABCD alphabet");
    return true;
}

bool testStringStream() {
    stringstream ss;

    ss << "This is: " << 10 << std::endl;
    EXPECT_TRUE(ss.str() == "This is: 10\n");
    return true;
}
}  // namespace android

int main(int argc, char **argv){
    FAIL_UNLESS(testConstructor);
    FAIL_UNLESS(testInAvail);
    FAIL_UNLESS(testNulChar);
    FAIL_UNLESS(testPut);
    FAIL_UNLESS(testStringStream);
    return kPassed;
}
