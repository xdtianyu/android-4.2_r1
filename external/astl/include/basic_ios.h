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

#ifndef ANDROID_ASTL_BASIC_IOS_H__
#define ANDROID_ASTL_BASIC_IOS_H__

#include <ios_base.h>

namespace std {

// basic_ios holds the streambuf instance used to perform th I/O
// operations.
// A concrete stream implementation does its work and calls rdbuf() to
// access the streambuf when it is ready to output the date (if the
// stream is an ostream).
// The standard says that basic_ios should deal with the state of the
// stream (good,eof,fail, etc), currently this is not implemented.

class streambuf;
class basic_ios: public ios_base {
  public:
    typedef int io_state;
    typedef int open_mode;
    typedef int seek_dir;
    typedef std::streampos streampos;
    typedef std::streamoff streamoff;

  protected:
    basic_ios();

  public:
    // No op, does NOT destroy mStreambuf.
    virtual ~basic_ios();

    /**
     * Change the unlying buffer.
     * @param sb The new buffer.
     * @return The previous stream buffer.
     */
    streambuf* rdbuf(streambuf *sb);
    /**
     * @return the underlying buffer associated with this stream.
     */
    streambuf* rdbuf() const { return mStreambuf; }

  protected:
    void init(streambuf* sb);
    streambuf* mStreambuf;
};

}  // namespace std

#endif
