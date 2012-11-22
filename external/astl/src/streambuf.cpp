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

#include <streambuf>
#include <algorithm>  // for min
namespace std {

// Implementation of the streambuf, common stuff for all the stream buffers.
streambuf::streambuf()
    : mPutBeg(0), mPutCurr(0), mPutEnd(0) { }

streambuf::streambuf(const streambuf& sb)
    : mPutBeg(sb.mPutBeg), mPutCurr(sb.mPutCurr), mPutEnd(sb.mPutEnd) { }

streambuf::~streambuf() {}

streamsize streambuf::xsputn(const char_type* str, streamsize num) {
    streamsize written = 0;

    while (written < num) {
        const streamsize avail = this->epptr() - this->pptr();
        if (avail) {
            const streamsize remaining = num - written;
            const streamsize len = std::min(avail, remaining);
            traits_type::copy(this->pptr(), str, len);
            written += len;
            str += len;
            this->pbump(len);
        }

        if (written < num) {
            // Indicate to the impl that we overflown. Either the impl
            // can increase its buffer and consume one character (in
            // which case eof() is not returned) or it can't and we
            // stop here.
            int_type c = this->overflow(traits_type::to_int_type(*str));
            if (!traits_type::eq_int_type(c, traits_type::eof())) {
                ++written;
                ++str;
            } else {
                break;
            }
        }
    }
    return written;
}

}  // namespace std
