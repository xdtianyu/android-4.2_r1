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

#ifndef ANDROID_ASTL_IOS_POS_TYPES__
#define ANDROID_ASTL_IOS_POS_TYPES__

#include <cstddef>  // ptrdiff_t
namespace std {

// Type use by fpos.
typedef long long streamoff;

// Signed type for I/O operation counts and buffer sizes.
typedef ptrdiff_t  streamsize;

// In the regular STL fpos is a template which gets specialized for
// char and wchar and defines 2 types streampos and wstreampos. Since
// we don't support wchar, we just provide the final char compliant
// version.  The methods to get/set the mbstate in fpos are not
// provided.

class fpos {
  public:
    fpos() : mOffs(0) { }
    fpos(streamoff offs) : mOffs(offs) { }

    // Convert to streamoff.
    operator streamoff() const { return mOffs; }

    // Not supported:
    // void state(mbstate_t);
    // mbstate_t state() const;

    fpos& operator+=(streamoff offs);
    fpos& operator-=(streamoff offs) {
        return operator+=(-offs);
    }

    fpos operator+(streamoff offs) const;
    fpos operator-(streamoff offs) const;

  private:
    streamoff mOffs;
};

inline bool
operator==(const fpos& lhs, const fpos& rhs)
{ return streamoff(lhs) == streamoff(rhs); }

inline bool
operator!=(const fpos& lhs, const fpos& rhs)
{ return streamoff(lhs) != streamoff(rhs); }

typedef fpos streampos;

}  // namespace std

#endif
