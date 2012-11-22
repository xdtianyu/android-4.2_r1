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

#include "../include/vector"
#ifndef ANDROID_ASTL_VECTOR__
#error "Wrong header included!!"
#endif
#include <climits>
#include <cstring>
#include <string>
#include "common.h"

namespace android {
using std::string;
using std::vector;
static const size_t kExponentialFactor = 2;
bool testConstructorInt()
{
    {
        vector<int> vec1;
        EXPECT_TRUE(vec1.empty());
        EXPECT_TRUE(vec1.size() == 0);
        EXPECT_TRUE(vec1.capacity() == 0);
    }
    {
        vector<int> vec2(100);
        EXPECT_TRUE(!vec2.empty());
        EXPECT_TRUE(vec2.size() == 100);
        EXPECT_TRUE(vec2.capacity() == 100);
        for (size_t i = 0; i < 100; ++i)
        {
            EXPECT_TRUE(vec2[i] == 0);
        }
    }
    {
        vector<int> vec3(200, 0xaa);
        EXPECT_TRUE(!vec3.empty());
        EXPECT_TRUE(vec3.size() == 200);
        EXPECT_TRUE(vec3.capacity() == 200);
        for (size_t i = 0; i < 200; ++i)
        {
            EXPECT_TRUE(vec3[i] == 0xaa);
        }
    }
    return true;
}

bool testConstructorString()
{
    {
        vector<string> vec1;
        EXPECT_TRUE(vec1.empty());
        EXPECT_TRUE(vec1.size() == 0);
        EXPECT_TRUE(vec1.capacity() == 0);
    }
    return true;
}

typedef enum { ONE = 10, TWO} TestEnum;

bool testConstructorClass()
{
    {
        vector<B> vec1;
        EXPECT_TRUE(vec1.empty());
        EXPECT_TRUE(vec1.size() == 0);
        EXPECT_TRUE(vec1.capacity() == 0);
    }
    {
        vector<B> vec1(100);
        EXPECT_TRUE(!vec1.empty());
        EXPECT_TRUE(vec1.size() == 100);
        EXPECT_TRUE(vec1.capacity() == 100);
    }
    return true;
}

bool testConstructorRepeat()
{
    {
        const vector<int> vec1(100, 10);

        for (int i = 0; i < 100; ++i)
        {
            EXPECT_TRUE(vec1[i] == 10);
        }
    }
    {
        const vector<float> vec2(100, 10.0f);

        for (int i = 0; i < 100; ++i)
        {
            EXPECT_TRUE(vec2[i] == 10.0f);
        }
    }
    {
        const vector<TestEnum> vec3(100, ONE);

        for (int i = 0; i < 100; ++i)
        {
            EXPECT_TRUE(vec3[i] == ONE);
        }
    }
    {
        const vector< A<B> > vec4;
        const vector< A<B> > vec5(10, A<B>());

        EXPECT_TRUE(vec4.size() == 0);
        EXPECT_TRUE(vec5.size() == 10);
    }
    return true;
}

bool testConstructorIterator()
{
    {
        vector<string> src;
        EXPECT_TRUE(src.empty());
        vector<string> dst(src.begin(), src.end());
        EXPECT_TRUE(dst.empty());
    }
    {
        vector<int> src;
        EXPECT_TRUE(src.empty());
        vector<int> dst(src.begin(), src.end());
        EXPECT_TRUE(dst.empty());
    }
    {
        vector<int> src;
        src.push_back(10);
        src.push_back(20);
        src.push_back(30);
        vector<int> dst(src.begin(), src.end());
        EXPECT_TRUE(dst.size() == 3);
        EXPECT_TRUE(dst[0] == 10);
        EXPECT_TRUE(dst[1] == 20);
        EXPECT_TRUE(dst[2] == 30);
    }
    {
        vector<string> src;
        src.push_back("str1");
        src.push_back("str2");
        src.push_back("str3");
        vector<string> dst(src.begin(), src.end());
        EXPECT_TRUE(dst.size() == 3);
        EXPECT_TRUE(dst[0] == "str1");
        EXPECT_TRUE(dst[1] == "str2");
        EXPECT_TRUE(dst[2] == "str3");
    }
    return true;
}

bool testReserve()
{
    { // basic reserve + shrink.
        vector<int> vec1(100, 10);

        EXPECT_TRUE(vec1.capacity() == 100);
        EXPECT_TRUE(vec1.reserve(200));
        EXPECT_TRUE(vec1.capacity() == 200);
        EXPECT_TRUE(vec1.size() == 100);

        EXPECT_TRUE(vec1.reserve());
        EXPECT_TRUE(vec1.capacity() == 100);
        EXPECT_TRUE(vec1.size() == 100);
    }
    {
        vector<int> vec2;

        EXPECT_TRUE(vec2.capacity() == 0);
        EXPECT_TRUE(vec2.reserve());
        EXPECT_TRUE(vec2.capacity() == 0);

        vec2.reserve(200);
        EXPECT_TRUE(vec2.capacity() == 200);
        vec2.reserve();
        EXPECT_TRUE(vec2.capacity() == 0);
        vec2.push_back(3);
        vec2.reserve();
        EXPECT_TRUE(vec2.capacity() == 1);
    }
    {
        vector<int> vec3;

        vec3.push_back(5);
        vec3.reserve();
        EXPECT_TRUE(vec3.capacity() == 1);
        vec3.push_back(3);
        EXPECT_TRUE(vec3.capacity() == kExponentialFactor);
        while (vec3.size() < kExponentialFactor)
            vec3.push_back(3);

        EXPECT_TRUE(vec3.size() == kExponentialFactor);
        EXPECT_TRUE(vec3.capacity() == kExponentialFactor);

        // exp increment.
        vec3.push_back(10);
        EXPECT_TRUE(vec3.capacity() == kExponentialFactor * kExponentialFactor);
    }
    {
        CopyCounter c;

        c.mCount = 0;
        vector<CopyCounter> vec4(100, c);
        EXPECT_TRUE(c.mCount == 100);
        // Growing does not do any copy via the copy assignement op.
        vec4.reserve(1000);
        EXPECT_TRUE(c.mCount == 200);
        vec4.reserve(50); // reserving less than length is a nop.
        EXPECT_TRUE(c.mCount == 200);
    }
    {
        vector<unsigned short> vec5;

        EXPECT_TRUE(!vec5.reserve(vec5.max_size() + 1));
        EXPECT_TRUE(vec5.capacity() == 0);
    }
    return true;
}


bool testPushBack()
{
    {
        vector<CtorDtorCounter> vec1;
        CtorDtorCounter c;

        c.reset();
        for (int i = 0; i < 1000; ++i)
        {
            vec1.push_back(c);
        }
        EXPECT_TRUE(vec1.capacity() == 1024);
        EXPECT_TRUE(vec1.size() == 1000);
        // Assignment should not be used, but the constructor should.
        EXPECT_TRUE(c.mAssignCount == 0);
        // Copy constructor was been invoked for each new element
        // pushed and when the capacity was increased.
        EXPECT_TRUE(c.mCopyCtorCount > 1000);
        EXPECT_TRUE(c.mCtorCount == 0);
    }
    {
        vector<int> vec2;

        vec2.push_back(10);
        EXPECT_TRUE(vec2.front() == 10);
        EXPECT_TRUE(vec2.back() == 10);
        EXPECT_TRUE(vec2.size() == 1);
        vec2.push_back(20);
        EXPECT_TRUE(vec2.front() == 10);
        EXPECT_TRUE(vec2.back() == 20);
        EXPECT_TRUE(vec2.size() == 2);
    }
    // Push back an non-pod object.
    {
        string str = "a string";
        vector<string> vec3;

        vec3.push_back(str);
        EXPECT_TRUE(vec3.size() == 1);
        EXPECT_TRUE(vec3.front() == "a string");
        EXPECT_TRUE(vec3.back() == "a string");
    }
    return true;
}


bool testPopBack()
{
    vector<int> vec1(10, 0xdeadbeef);;

    EXPECT_TRUE(vec1.capacity() == 10);
    EXPECT_TRUE(vec1.size() == 10);

    for(size_t i = 10; i > 0; --i)
    {
        EXPECT_TRUE(vec1.capacity() == 10);
        EXPECT_TRUE(vec1.size() == i);
        vec1.pop_back();
    }
    EXPECT_TRUE(vec1.empty());
    EXPECT_TRUE(vec1.begin() == vec1.end());
    vec1.pop_back(); // pop_back on empty vector
    EXPECT_TRUE(vec1.size() == 0);
    EXPECT_TRUE(vec1.capacity() == 10);

    vec1.clear();
    vec1.pop_back(); // pop_back on empty vector
    EXPECT_TRUE(vec1.size() == 0);
    EXPECT_TRUE(vec1.capacity() == 0);
    EXPECT_TRUE(vec1.begin() == vec1.end());
    EXPECT_TRUE(vec1.begin().base() == NULL);

    CtorDtorCounter instance;
    vector<CtorDtorCounter> vec2(10, instance);

    CtorDtorCounter::reset();
    for (int i = 0; i < 10; ++i)
    {
        vec2.pop_back();
    }
    EXPECT_TRUE(vec2.size() == 0);
    EXPECT_TRUE(CtorDtorCounter::mDtorCount == 10);
    return true;
}


bool testResize()
{
    {
        vector<int> vec1(10, 0xdeadbeef);
        vec1.resize(0);
        EXPECT_TRUE(vec1.capacity() == 10);
        vec1.resize(5);
        EXPECT_TRUE(vec1.capacity() == 10);
        vec1.resize(10);
        EXPECT_TRUE(vec1.capacity() == 10);
        vec1.resize(11);
        EXPECT_TRUE(vec1.capacity() == 11);
        vec1.resize(100);
        EXPECT_TRUE(vec1.capacity() == 100);
        vec1.resize(10);
        EXPECT_TRUE(vec1.capacity() == 100);
    }
    {
        vector<B> vec1(10);
        vec1.resize(0);
        EXPECT_TRUE(vec1.capacity() == 10);
        vec1.resize(5);
        EXPECT_TRUE(vec1.capacity() == 10);
        vec1.resize(10);
        EXPECT_TRUE(vec1.capacity() == 10);
        vec1.resize(11);
        EXPECT_TRUE(vec1.capacity() == 11);
        vec1.resize(100);
        EXPECT_TRUE(vec1.capacity() == 100);
        vec1.resize(10);
        EXPECT_TRUE(vec1.capacity() == 100);
    }
    {
        vector<CtorDtorCounter> vec;
        CtorDtorCounter::reset();
        vec.resize(10);
        EXPECT_TRUE(CtorDtorCounter::mCtorCount == 1);  // default arg.
        EXPECT_TRUE(CtorDtorCounter::mCopyCtorCount == 10); // copied 10 times.

        CtorDtorCounter::reset();
        vec.resize(200);
        EXPECT_TRUE(CtorDtorCounter::mCtorCount == 1);  // default arg.
        EXPECT_TRUE(CtorDtorCounter::mCopyCtorCount == 200);

        CtorDtorCounter::reset();
        vec.resize(199);
        // the copy constructor should have been called once and the
        // destructor twice (1 temp + 1 elt).
        EXPECT_TRUE(CtorDtorCounter::mCtorCount == 1);  // default arg.
        EXPECT_TRUE(CtorDtorCounter::mDtorCount == 2);

        CtorDtorCounter::reset();
        vec.resize(0);
        // the copy constructor should have been called once and the
        // destructor twice (1 temp + 199 elts).
        EXPECT_TRUE(CtorDtorCounter::mCtorCount == 1);  // default arg.
        EXPECT_TRUE(CtorDtorCounter::mDtorCount == 200);
    }
    return true;
}

bool testSwap()
{
    vector<int> vec1(100, 10);
    vector<int> vec2;

    vec1.swap(vec2);

    EXPECT_TRUE(vec1.capacity() == 0);
    EXPECT_TRUE(vec2.capacity() == 100);

    EXPECT_TRUE(vec1.size() == 0);
    EXPECT_TRUE(vec2.size() == 100);

    EXPECT_TRUE(vec1.begin() == vec1.end());
    EXPECT_TRUE(vec2.begin() != vec2.end());
    return true;
}


bool testIterators()
{
    vector<int> vec1(10);

    for (size_t i = 0; i < 10; ++i)
    {
        vec1[i] = i;
    }

    vector<int>::iterator i = vec1.begin();
    for (int c = 0; i != vec1.end(); ++i, ++c)
    {
        EXPECT_TRUE(c == *i);
    }

    vector<int>::const_iterator j = vec1.begin();
    for (int c = 0; j != vec1.end(); ++j, ++c)
    {
        EXPECT_TRUE(c == *j);
    }

    {
        const vector<int> vec1(100, 10);

        EXPECT_TRUE(vec1.end().operator-(100) == vec1.begin());
        EXPECT_TRUE(vec1.end() - 100 == vec1.begin());

        EXPECT_TRUE(100 + vec1.begin() == vec1.end());
        EXPECT_TRUE(vec1.begin() + 100 == vec1.end());

        EXPECT_TRUE(vec1.end() - vec1.begin() == 100);
        EXPECT_TRUE(std::distance(vec1.begin(), vec1.end()) == 100);
        EXPECT_TRUE(std::distance(vec1.end(), vec1.begin()) == -100);

        for (vector<int>::const_iterator i = vec1.begin();
             i != vec1.end(); ++i) {
            EXPECT_TRUE(*i == 10);
        }
    }

    {
        const vector<int> vec2;
        EXPECT_TRUE(vec2.begin() == vec2.end());
    }
    return true;
}

bool testCtorDtorForNonPod()
{
    {  // empty vector, no construction should happen.
        CtorDtorCounter::reset();
        vector<CtorDtorCounter> vec1;

        EXPECT_TRUE(CtorDtorCounter::mCtorCount == 0);
        EXPECT_TRUE(CtorDtorCounter::mCopyCtorCount == 0);
    }
    EXPECT_TRUE(CtorDtorCounter::mDtorCount == 0);

    {
        CtorDtorCounter instance;
        EXPECT_TRUE(CtorDtorCounter::mCtorCount == 1);
        CtorDtorCounter::reset();

        vector<CtorDtorCounter> vec2(200, instance);

        // 200 copies by assignement of the sample instance
        EXPECT_TRUE(CtorDtorCounter::mAssignCount == 0);
        EXPECT_TRUE(CtorDtorCounter::mCtorCount == 0);
        EXPECT_TRUE(CtorDtorCounter::mCopyCtorCount == 200);
        EXPECT_TRUE(CtorDtorCounter::mDtorCount == 0);

        CtorDtorCounter::reset();
        vec2.reserve(400);

        // 200 moves: 200 copies by copy constructor and 200 destructions.
        EXPECT_TRUE(CtorDtorCounter::mCopyCtorCount == 200);
        EXPECT_TRUE(CtorDtorCounter::mDtorCount == 200);
        EXPECT_TRUE(CtorDtorCounter::mCtorCount == 0);
        EXPECT_TRUE(CtorDtorCounter::mAssignCount == 0);

        CtorDtorCounter::reset();
    }
    // 200 + 1 for the instance
    EXPECT_TRUE(CtorDtorCounter::mDtorCount == 201);
    return true;
}

bool testEraseElt()
{
    {
        vector<B> empty;
        vector<B>::iterator res = empty.erase(empty.end());
        EXPECT_TRUE(res == empty.end());
        EXPECT_TRUE(empty.empty());
    }
    {
        vector<B> one;
        one.push_back(B());
        EXPECT_TRUE(!one.empty());

        vector<B>::iterator res = one.erase(one.begin());
        EXPECT_TRUE(res == one.end());

        EXPECT_TRUE(one.begin() == one.end());
        EXPECT_TRUE(one.empty());
    }
    {
        vector<B> two;
        two.push_back(B());
        two.push_back(B());

        vector<B>::iterator res = two.erase(two.begin());

        EXPECT_TRUE(res == two.begin());
        EXPECT_TRUE(res != two.end());

        EXPECT_TRUE(two.begin() != two.end());
        EXPECT_TRUE(two.size() == 1);
    }
    {
        vector<int> vec;
        for (int i = 0; i < 20; ++i) vec.push_back(i);
        vector<int>::iterator pos;

        pos = vec.erase(vec.begin() + 2);  // removes '2'
        EXPECT_TRUE(*pos == 3);            // returns '3'

        pos = vec.erase(vec.begin() + 18); // removes '19' now @ pos 18
        EXPECT_TRUE(pos == vec.end());     // returns end()
        EXPECT_TRUE(*(--pos) == 18);       // last one is now '18'
    }
    {
        vector<std::string> vec;

        vec.push_back("first");
        vec.push_back("second");
        vec.push_back("third");
        vec.push_back("fourth");

        vector<std::string>::iterator pos;
        pos = vec.erase(vec.begin() + 1);  // removes 'second'
        EXPECT_TRUE(vec.size() == 3);
        EXPECT_TRUE(*pos == "third");
        EXPECT_TRUE(vec[0] == "first");
        EXPECT_TRUE(vec[1] == "third");
        EXPECT_TRUE(vec[2] == "fourth");
    }
    return true;
}

bool testEraseRange()
{
    {
        vector<B> empty;
        vector<B>::iterator res = empty.erase(empty.begin(), empty.end());
        EXPECT_TRUE(res == empty.end());
        EXPECT_TRUE(empty.empty());
        EXPECT_TRUE(empty.size() == 0);
    }
    {
        vector<B> one;
        one.push_back(B());
        EXPECT_TRUE(!one.empty());

        vector<B>::iterator res = one.erase(one.begin(), one.end());
        EXPECT_TRUE(res == one.end());

        EXPECT_TRUE(one.begin() == one.end());
        EXPECT_TRUE(one.empty());
    }
    {
        vector<B> two;
        two.push_back(B());
        two.push_back(B());

        // erase the 1st one.
        vector<B>::iterator res = two.erase(two.begin(), two.begin() + 1);

        EXPECT_TRUE(res == two.begin());
        EXPECT_TRUE(res != two.end());

        EXPECT_TRUE(two.begin() != two.end());
        EXPECT_TRUE(two.size() == 1);
    }
    {
        vector<B> two;
        two.push_back(B());
        two.push_back(B());

        // erase range is empty.
        vector<B>::iterator res = two.erase(two.begin(), two.begin());

        EXPECT_TRUE(res == two.begin());
        EXPECT_TRUE(res != two.end());

        EXPECT_TRUE(two.begin() != two.end());
        EXPECT_TRUE(two.size() == 2);
    }

    {
        vector<int> vec;
        for (int i = 0; i < 20; ++i) vec.push_back(i);
        vector<int>::iterator pos;

        pos = vec.erase(vec.begin() + 2, vec.begin() + 3);  // removes '2'
        EXPECT_TRUE(*pos == 3);                             // returns '3'

        pos = vec.erase(vec.begin() + 18, vec.end()); // removes '19' now @ pos 18
        EXPECT_TRUE(pos == vec.end());                // returns end()
        EXPECT_TRUE(*(--pos) == 18);                  // last one is now '18'
    }
    {
        vector<std::string> vec;

        vec.push_back("first");
        vec.push_back("second");
        vec.push_back("third");
        vec.push_back("fourth");

        vector<std::string>::iterator pos;
        pos = vec.erase(vec.begin() + 1, vec.begin() + 3);  // removes 'second' and third.
        EXPECT_TRUE(vec.size() == 2);
        EXPECT_TRUE(*pos == "fourth");
        EXPECT_TRUE(vec[0] == "first");
        EXPECT_TRUE(vec[1] == "fourth");
        pos = vec.erase(vec.begin(), vec.end());  // clears the vector
        EXPECT_TRUE(vec.empty());
    }
    return true;
}

// Valgrind should not barf when we access element out of bound.
bool testAt() {
    vector<int> vec;

    vec.at(1000) = 0xdeadbeef;
    EXPECT_TRUE(vec.at(1000) == 0xdeadbeef);
    return true;
}
}  // namespace android

int main(int argc, char **argv)
{
    FAIL_UNLESS(testConstructorInt);
    FAIL_UNLESS(testConstructorString);
    FAIL_UNLESS(testConstructorClass);
    FAIL_UNLESS(testConstructorRepeat);
    FAIL_UNLESS(testConstructorIterator);
    FAIL_UNLESS(testReserve);
    FAIL_UNLESS(testPushBack);
    FAIL_UNLESS(testPopBack);
    FAIL_UNLESS(testResize);
    FAIL_UNLESS(testSwap);
    FAIL_UNLESS(testIterators);
    FAIL_UNLESS(testCtorDtorForNonPod);
    FAIL_UNLESS(testEraseElt);
    FAIL_UNLESS(testEraseRange);
    FAIL_UNLESS(testAt);
    return kPassed;
}
