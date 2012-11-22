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

#include <ios_base.h>
#include <cstdio>
#include <new>  // for placement new.
#include <iostream>  // For cout, cerr
#include <ostream>
#include <stdio_filebuf.h>
#include <streambuf>

// Defined in bionic/libstdc++/src/one_time_construction.cpp
extern "C" int __cxa_guard_acquire(int volatile * gv);
extern "C" void __cxa_guard_release(int volatile * gv);

namespace std {
int ios_base::Init::sGuard = 0;
bool ios_base::Init::sDone = false;

// Implementation of the ios_base, common stuff for all the streams.

ios_base::ios_base()
    : mFlags(skipws | dec), mPrecision(6), mWidth(0) {}

ios_base::~ios_base() {}

ios_base::fmtflags ios_base::flags(fmtflags flags) {
    fmtflags prev = mFlags;
    mFlags = flags;
    return prev;
}

ios_base::fmtflags ios_base::setf(fmtflags flags) {
    fmtflags prev = mFlags;
    mFlags = flags;
    return prev;
}

ios_base::fmtflags ios_base::setf(fmtflags flags, fmtflags mask) {
    fmtflags prev = mFlags;
    mFlags &= ~mask;
    mFlags |= (flags & mask);
    return prev;
}

void ios_base::unsetf(fmtflags mask) {
    mFlags &= ~mask;
}

streamsize ios_base::precision(streamsize precision) {
    const streamsize prev = mPrecision;
    if (precision >= 0) {  // Not sure what a negative precision would mean.
        mPrecision = precision;
    }
    return prev;
}

streamsize ios_base::width(streamsize width) {
    const streamsize prev = mWidth;
    if (width >= 0) {  // Not sure what a negative width would mean.
        mWidth = width;
    }
    return prev;
}

// TODO: This is a temporary class used to illustrate how the
// construction will happen.

class std_filebuf: public streambuf {
  public:
    std_filebuf() {}
    virtual ~std_filebuf() {}
};

// Storage is declared in src/ios_globals.cpp
extern android::stdio_filebuf stdio_filebuf_cout;
extern android::stdio_filebuf stdio_filebuf_cerr;

ios_base::Init::Init() {
    if (__cxa_guard_acquire(&sGuard) == 1) {
        if (!sDone) {
            // Create the global cout and cerr
            // structures. stdio_filebuf_cout/stdio_filebuf_cerr and
            // cout/cerr storage are in ios_globals.cpp.
            new (&stdio_filebuf_cout) android::stdio_filebuf(stdout);
            new (&stdio_filebuf_cerr) android::stdio_filebuf(stderr);
            new (&cout) ostream(&stdio_filebuf_cout);
            new (&cerr) ostream(&stdio_filebuf_cerr);
            sDone = true;
        }
        __cxa_guard_release(&sGuard);
    }
}

ios_base::Init::~Init() {
    cout.flush();
    cerr.flush();
}

}  // namespace std
