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

#include <ostream>
#include <streambuf>
#include <cstring>
#include <limits>

namespace std {
// Defined in bionic/libstdc++/src/one_time_construction.cpp

ostream::ostream() { }

ostream::~ostream() { }

ostream& ostream::operator<<(const char_type *str) {
    if (this->rdbuf() && str) {
        this->rdbuf()->sputn(str, strlen(str));
    }
    return *this;
}

ostream& ostream::operator<<(char_type c) {
    // TODO: Should format according to flags.
    return put(c);
}

ostream& ostream::operator<<(bool val) {
    // TODO: Should format according to flags (e.g write "true" or "false").
    return put(val?'1':'0');
}

// 64 bits int in octal is 22 digits. There is one for the sign and 2
// for the format specifier + 1 for the terminating \0. A total of 26
// chars should be enough to hold any integer representation.
static const size_t kNumSize = 26;
ostream& ostream::operator<<(int val) {
    const char *fmt = "%d";
    char buf[kNumSize];
    int size = snprintf(buf, kNumSize, fmt, val);
    return write(buf, size);
}

ostream& ostream::operator<<(unsigned int val) {
    const char *fmt = "%u";
    char buf[kNumSize];
    int size = snprintf(buf, kNumSize, fmt, val);
    return write(buf, size);
}

ostream& ostream::operator<<(long int val) {
    const char *fmt = "%ld";
    char buf[kNumSize];
    int size = snprintf(buf, kNumSize, fmt, val);
    return write(buf, size);
}

ostream& ostream::operator<<(unsigned long int val) {
    const char *fmt = "%lu";
    char buf[kNumSize];
    int size = snprintf(buf, kNumSize, fmt, val);
    return write(buf, size);
}

ostream& ostream::operator<<(long long int val) {
    const char *fmt = "%lld";
    char buf[kNumSize];
    int size = snprintf(buf, kNumSize, fmt, val);
    return write(buf, size);
}

ostream& ostream::operator<<(unsigned long long int val) {
    const char *fmt = "%llu";
    char buf[kNumSize];
    int size = snprintf(buf, kNumSize, fmt, val);
    return write(buf, size);
}

// Double max 1.7976931348623157E+308 = 23 < kNumSize so we reuse it.
ostream& ostream::operator<<(double val) {
    const char *fmt = "%.*e";
    char buf[kNumSize];
    int size = snprintf(buf, kNumSize, fmt, precision(), val);
    return write(buf, size);
}

ostream& ostream::operator<<(float val) {
    const char *fmt = "%.*e";
    char buf[kNumSize];
    int size = snprintf(buf, kNumSize, fmt, precision(), val);
    return write(buf, size);
}

ostream& ostream::operator<<(const void *p) {
    const char *fmt = "%p";
    char buf[kNumSize];
    int size = snprintf(buf, kNumSize, fmt, p);
    return write(buf, size);
}

ostream& ostream::write_formatted(const char_type *str, streamsize num) {
    // TODO: Should format the string according to the flags.
    return write(str, num);
}

ostream& ostream::put(char_type c) {
    if (this->rdbuf()) {
        this->rdbuf()->sputn(&c, 1);
    }
    return *this;
}

ostream& ostream::write(const char_type *str, streamsize num) {
    if (this->rdbuf()) {
        this->rdbuf()->sputn(str, num);
    }
    return *this;
}

ostream& ostream::flush() {
    if (this->rdbuf()) {
        // TODO: if pubsync returns -1 should mark this stream as
        // 'bad'.
        this->rdbuf()->pubsync();
    }
    return *this;
}

}  // namespace std
