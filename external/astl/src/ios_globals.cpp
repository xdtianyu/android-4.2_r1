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

// Make sure <iostream> is not included directly or indirectly. You
// can include <ostream> and/or <istream> just fine but <iostream>
// contains forward declarations for cout, cerr... that will conflict
// with the ones below.
#include <ostream>
#include <stdio_filebuf.h>

namespace std {

// Global instances of cout and cerr. Here we reserve the memory for
// the stdio filebuf. The first time ios_base::Init::Init() is called,
// placement new is used to initialize these areas with proper
// instances of the streams.
//
// - cout and cerr are mandated by the standard.
// - stdio_filebuf_cout and stdio_filebuf_cerr are our own stdio and
//   stderr streambuf implementation used to build the cout and cerr
//   ostreams. (see ios_base::Init::Init())

typedef char stdio_filebuf_mem[sizeof(android::stdio_filebuf)]
__attribute__ ((aligned(__alignof__(android::stdio_filebuf))));
stdio_filebuf_mem stdio_filebuf_cout;
stdio_filebuf_mem stdio_filebuf_cerr;

typedef char ostream_mem[sizeof(ostream)]
__attribute__ ((aligned(__alignof__(ostream))));
ostream_mem cout;
ostream_mem cerr;

}  // namespace std
