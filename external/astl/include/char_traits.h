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

#ifndef ANDROID_ASTL_CHAR_TRAITS_H__
#define ANDROID_ASTL_CHAR_TRAITS_H__

#include <ios_pos_types.h>  // For streampos
#include <cstdio>           // For EOF
#include <cstring>          // For memcmp, memchr, strlen

namespace std {

/**
 * char_traits defines the basic types and constants (eof) used in
 * string and stream as well as basic char manipulations.
 * Android's support only char. The state_type is missing because we
 * don't support multibyte strings.
 */

template<class _CharT> struct char_traits {
    // Empty on purpose. You should use char_traits<char> only.
};

template<>
struct char_traits<char>
{
    typedef char       char_type;
    typedef int        int_type;
    typedef streampos  pos_type;
    typedef streamoff  off_type;

    static void assign(char& lhs, const char& rhs) { lhs = rhs; }

    static bool eq(const char& lhs, const char& rhs) { return lhs == rhs; }

    static bool lt(const char& lhs, const char& rhs) { return lhs < rhs; }

    static int compare(const char* lhs, const char* rhs, size_t n)
    { return std::memcmp(lhs, rhs, n); }

    static size_t length(const char* str) { return std::strlen(str); }

    static const char* find(const char* str, size_t n, const char& c)
    { return static_cast<const char*>(std::memchr(str, c, n)); }

    static char* move(char* lhs, const char* rhs, size_t n)
    { return static_cast<char*>(std::memmove(lhs, rhs, n)); }

    static char* copy(char* lhs, const char* rhs, size_t n)
    { return static_cast<char*>(std::memcpy(lhs, rhs, n)); }

    // Fill 'lhs' with 'n' occurences of 'c'
    static char* assign(char* lhs, size_t n, char c)
    { return static_cast<char*>(std::memset(lhs, c, n)); }

    static char to_char(const int_type& c) { return static_cast<char>(c); }

    static int_type to_int_type(const char& c)
    { return static_cast<int_type>(static_cast<unsigned char>(c)); }

    static bool eq_int_type(const int_type& lhs, const int_type& rhs)
    { return lhs == rhs; }

    static int_type eof() { return static_cast<int_type>(EOF); }

    static int_type not_eof(const int_type& c)
    { return (c == eof()) ? 0 : c; }
  };

}  // namespace std

#endif
