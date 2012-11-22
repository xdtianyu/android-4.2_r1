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

#ifndef ANDROID_ASTL_IOS_BASE_H__
#define ANDROID_ASTL_IOS_BASE_H__

#include <ios_pos_types.h>

namespace android {
// Flags are used to put the stream is a certain state which affect
// how data is formatted.
enum IosBaseFlags {
    ios_baseflags_boolalpha   = 1 << 0,
    ios_baseflags_dec         = 1 << 1,
    ios_baseflags_fixed       = 1 << 2,
    ios_baseflags_hex         = 1 << 3,
    ios_baseflags_internal    = 1 << 4,
    ios_baseflags_left        = 1 << 5,
    ios_baseflags_oct         = 1 << 6,
    ios_baseflags_right       = 1 << 7,
    ios_baseflags_scientific  = 1 << 8,
    ios_baseflags_showbase    = 1 << 9,
    ios_baseflags_showpoint   = 1 << 10,
    ios_baseflags_showpos     = 1 << 11,
    ios_baseflags_skipws      = 1 << 12,
    ios_baseflags_unitbuf     = 1 << 13,
    ios_baseflags_uppercase   = 1 << 14,
    ios_baseflags_adjustfield = ios_baseflags_left | ios_baseflags_right | ios_baseflags_internal,
    ios_baseflags_basefield   = ios_baseflags_dec | ios_baseflags_oct | ios_baseflags_hex,
    ios_baseflags_floatfield  = ios_baseflags_scientific | ios_baseflags_fixed,
    ios_baseflags_end         = 1 << 15
};

// Openmode
enum IosBaseOpenmode {
    ios_baseopenmode_app    = 1 << 0,
    ios_baseopenmode_ate    = 1 << 1,
    ios_baseopenmode_binary = 1 << 2,
    ios_baseopenmode_in     = 1 << 3,
    ios_baseopenmode_out    = 1 << 4,
    ios_baseopenmode_trunc  = 1 << 5,
    ios_baseopenmode_end    = 1 << 6
};

}  // namespace android

namespace std {

/**
 * Root of the streams inheritance.
 * The STL defines ios_base as a template with 2 params char types and
 * traits. We support only char and no traits.
 * ios_base defines flags, types and fields to hold these values.
 * ios_base is extended by basic_ios which wraps a streambuf and
 * provides common methods for all streams.
 * The only mode supported for the standards streams (cout, cerr, cin)
 * is synced with stdio.
 */

class ios_base
{
  public:
    typedef std::streampos streampos;
    typedef std::streamoff streamoff;


  protected:
    ios_base();

  public:
    virtual ~ios_base();
    typedef int fmtflags;
    typedef int iostate;
    typedef int openmode;
    typedef int seekdir;

    // FLAGS

    // boolalpha:  Insert and extract bool type in alphabetic format.
    // dec:        Convert integer input or generates integer output in
    //             decimal base.
    // fixed:      Generate floating-point output in a fixed-point notation.
    // hex:        Convert integer input or generates integer output in
    //             hexadecimal base.
    // internal:   Adds fill characters as the designated interanl point
    //             in certain generated output, or identical to right
    //             if no such point is designated.
    // left:       Adds fill characters on the right (final positions) of
    //             certain generated output.
    // oct:        Convert integer input or generates integer output in octal
    //             base.
    // right:      Adds fill characters on the left (initial positions) of
    //             certain generated output.
    // scientific: Generates floating point output in scientific notation.
    // showbase:   Generates a prefix indicating the numeric base of generated
    //             integer output.
    // showpoint:  Generate a decimal point character unconditionally in
    //             generated floating point output.
    // showpos:    Generate a + sign in non-negative generated numeric output.
    // skipws:     Skips leading white space before certain input operations.
    // unitbuf:    Flushes output after each output operation.
    // uppercase:  Replaces certain lowercase letters with their upppercase
    //             equivalents in generated output.
    static const fmtflags boolalpha   = android::ios_baseflags_boolalpha;
    static const fmtflags dec         = android::ios_baseflags_dec;
    static const fmtflags fixed       = android::ios_baseflags_fixed;
    static const fmtflags hex         = android::ios_baseflags_hex;
    static const fmtflags internal    = android::ios_baseflags_internal;
    static const fmtflags left        = android::ios_baseflags_left;
    static const fmtflags oct         = android::ios_baseflags_oct;
    static const fmtflags right       = android::ios_baseflags_right;
    static const fmtflags scientific  = android::ios_baseflags_scientific;
    static const fmtflags showbase    = android::ios_baseflags_showbase;
    static const fmtflags showpoint   = android::ios_baseflags_showpoint;
    static const fmtflags showpos     = android::ios_baseflags_showpos;
    static const fmtflags skipws      = android::ios_baseflags_skipws;
    static const fmtflags unitbuf     = android::ios_baseflags_unitbuf;
    static const fmtflags uppercase   = android::ios_baseflags_uppercase;

    static const fmtflags adjustfield = android::ios_baseflags_adjustfield;
    static const fmtflags basefield   = android::ios_baseflags_basefield;
    static const fmtflags floatfield  = android::ios_baseflags_floatfield;

    // Set all the flags at once.
    // @return the previous value of the format flags
    fmtflags flags(fmtflags flags);

    // @return all the flags at once
    fmtflags flags() const { return mFlags; }

    // Set flags.
    // @return the previous value of the format flags
    fmtflags setf(fmtflags flags);

    // Clears 'mask' and set the 'flags' & 'mask'.
    // @return the previous value of the format flags
    fmtflags setf(fmtflags flags, fmtflags mask);

    // Clears 'mask'.
    void unsetf(fmtflags mask);


    // OPENMODE

    // app:    seek to end before each write.
    // ate:    open and seek to end imediately after opening.
    // binary: perform I/O in binary mode.
    // in:     open for input.
    // out:    open for output.
    // trunc:  truncate and existing stream when opening.
    static const openmode app = android::ios_baseopenmode_app;
    static const openmode ate = android::ios_baseopenmode_ate;
    static const openmode binary = android::ios_baseopenmode_binary;
    static const openmode in = android::ios_baseopenmode_in;
    static const openmode out = android::ios_baseopenmode_out;
    static const openmode trunc = android::ios_baseopenmode_trunc;

    // PRECISION and WIDTH

    /**
     * @return The precision (number of digits after the decimal
     * point) to generate on certain output conversions. 6 by default.
     */
    streamsize precision() const { return mPrecision; }

    /**
     *  @param precision The new precision value. Values < 0 are ignored.
     *  @return The previous value of precision(). 0 by default;
     */
    streamsize precision(streamsize precision);

    /**
     * @return The minimum field width to generate on output
     * operations.
     */
    streamsize width() const { return mWidth; }

    /**
     *  @param width The new width value. Values < 0 are ignored.
     *  @return The previous value of width().
     */
    streamsize width(streamsize width);

    // Helper class to initialize the standard streams. Its
    // construction ensures the initialization of the stdio streams
    // (cout, cerr,...) declared in iostream. These wrap the standard
    // C streams declared in <cstdio>.
    // The destruction of the last instance of this class will flush
    // the streams.
    class Init {
      public:
        Init();
        ~Init();

        static bool done() { return sDone; }
      private:
        static int sGuard;
        static bool sDone;
    };

  private:
    fmtflags   mFlags;
    streamsize mPrecision;
    streamsize mWidth;
};

}  // namespace std

#endif
