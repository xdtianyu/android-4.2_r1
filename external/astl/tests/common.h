/* -*- c++ -*- */
/*
 * Copyright (C) 2009 The Android Open Source Project
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

#ifndef ANDROID_ASTL_TESTS_COMMON__
#define ANDROID_ASTL_TESTS_COMMON__
#include <cstdio>

// Classes and macros used in tests.
namespace {
const size_t kMaxSizeT = ~((size_t)0);
const int kPassed = 0;
const int kFailed = 1;
#define FAIL_UNLESS(v) if (!android::v()) return kFailed;

#define EXPECT_TRUE(expr)                                   \
    if (!(expr)) {                                          \
        std::fprintf(stderr, "%d: %s\n", __LINE__, #expr);	\
        return false;                                       \
    }

#define EXPECT_FALSE(expr) EXPECT_TRUE(!(expr))


#ifndef ARRAYSIZE
#define ARRAYSIZE(array) (sizeof(array)/sizeof(array[0]))
#endif

// Cannot be copied.
struct NoCopy {
  private:
    NoCopy(const NoCopy& nc) {}
};

// Count the number of assignement.
struct CopyCounter {
    static size_t mCount;

    CopyCounter() { }
    CopyCounter& operator=(const CopyCounter& cc) {return *this; }
    CopyCounter(const CopyCounter& nc) {++mCount;}
  private:
};

class CtorDtorCounter {
  public:
    static size_t mCtorCount;
    static size_t mCopyCtorCount;
    static size_t mAssignCount;
    static size_t mDtorCount;

    CtorDtorCounter() {++mCtorCount;}
    CtorDtorCounter(const CtorDtorCounter& nc) {++mCopyCtorCount;}
    CtorDtorCounter& operator=(const CtorDtorCounter& nc) {++mAssignCount; return *this;}
    ~CtorDtorCounter() {++mDtorCount;}
    static void reset() {mCtorCount = 0; mCopyCtorCount = 0; mAssignCount = 0; mDtorCount = 0;}
    static void printf() {
        std::fprintf(stderr, "CtorDtorCounter: %d %d %d %d\n",
                     mCtorCount, mCopyCtorCount, mAssignCount, mDtorCount);
    }
  private:
};

size_t CopyCounter::mCount;
size_t CtorDtorCounter::mCtorCount;
size_t CtorDtorCounter::mCopyCtorCount;
size_t CtorDtorCounter::mAssignCount;
size_t CtorDtorCounter::mDtorCount;

// These class allocate chunks to detect memory leaks.
template<typename T> struct A {
  public:
    A() {mChunk = new T[2046];}
    A(const A<T>& a) {mChunk = new T[2046];}
    virtual ~A() {delete [] mChunk;}
    A& operator=(const A& o) { return *this;}
    T *mChunk;
};

struct B {
  public:
    B() {mChunk = new char[2046];}
    B(const B& b) {mChunk = new char[2046];}
    virtual ~B() {delete [] mChunk;}
    B& operator=(const B& o) { return *this;}
    char *mChunk;
};

}  // anonymous namespace


#endif  // ANDROID_ASTL_TEST_COMMON__
