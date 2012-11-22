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

#include "../include/type_traits.h"
#ifndef ANDROID_ASTL_TYPE_TRAITS_H__
#error "Wrong header included!!"
#endif

#include "common.h"

namespace android {

using std::true_type;
using std::false_type;

bool testTrueFalseType()
{
    EXPECT_TRUE(true_type::value == true);
    EXPECT_TRUE(false_type::value == false);
    EXPECT_TRUE(true_type::type::value == true);
    EXPECT_TRUE(false_type::type::value == false);

    typedef true_type::value_type       true_value_type;
    typedef true_type::type             true_type;
    typedef true_type::type::value_type true_type_value_type;
    typedef true_type::type::type       true_type_type;

    typedef false_type::value_type       false_value_type;
    typedef false_type::type             false_type;
    typedef false_type::type::value_type false_type_value_type;
    typedef false_type::type::type       false_type_type;
    return true;
}

bool testIsIntegral()
{
    using std::is_integral;
    EXPECT_TRUE(is_integral<bool>::value == true);
    EXPECT_TRUE(is_integral<char>::value == true);
    EXPECT_TRUE(is_integral<signed char>::value == true);
    EXPECT_TRUE(is_integral<unsigned char>::value == true);
    EXPECT_TRUE(is_integral<wchar_t>::value == true);
    EXPECT_TRUE(is_integral<short>::value == true);
    EXPECT_TRUE(is_integral<unsigned short>::value == true);
    EXPECT_TRUE(is_integral<int>::value == true);
    EXPECT_TRUE(is_integral<unsigned int>::value == true);
    EXPECT_TRUE(is_integral<long>::value == true);
    EXPECT_TRUE(is_integral<unsigned long>::value == true);
    EXPECT_TRUE(is_integral<long long>::value == true);
    EXPECT_TRUE(is_integral<unsigned long long>::value == true);

    // const
    EXPECT_TRUE(is_integral<int const>::value == true);
    EXPECT_TRUE(is_integral<const int>::value == true);

    // volatile
    EXPECT_TRUE(is_integral<int volatile>::value == true);
    EXPECT_TRUE(is_integral<int const volatile>::value == true);

    // float is not
    EXPECT_TRUE(is_integral<float>::value == false);
    return true;
}

bool testIsFloatingPoint()
{
    using std::is_floating_point;
    EXPECT_TRUE(is_floating_point<float>::value == true);
    EXPECT_TRUE(is_floating_point<double>::value == true);
    EXPECT_TRUE(is_floating_point<long double>::value == true);
    return true;
}

bool testIsPointer()
{
    using std::is_pointer;
    EXPECT_TRUE(is_pointer<float>::value == false);
    EXPECT_TRUE(is_pointer<int *>::value == true);
    return true;
}

class A {};

bool testIsPodOrClass()
{
    using std::is_class;
    using std::is_pod;
    EXPECT_TRUE(is_pod<float>::value == true);
    EXPECT_TRUE(is_pod<int *>::value == true);
    EXPECT_TRUE(is_pod<A>::value == false);

    EXPECT_TRUE(is_class<float>::value == false);
    EXPECT_TRUE(is_class<int *>::value == false);
    EXPECT_TRUE(is_class<A>::value == true);
    return true;
}
}  // namespace android

int main(int argc, char **argv)
{
    FAIL_UNLESS(testTrueFalseType);
    FAIL_UNLESS(testIsIntegral);
    FAIL_UNLESS(testIsFloatingPoint);
    FAIL_UNLESS(testIsPointer);
    FAIL_UNLESS(testIsPodOrClass);
    return kPassed;
}
