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

#include "../include/iterator"
#ifndef ANDROID_ASTL_ITERATOR__
#error "Wrong header included!!"
#endif
#include "common.h"

namespace android {

// Iterators used in tests.
struct Input {
    typedef std::input_iterator_tag iterator_category;
    typedef int                     value_type;
    typedef ptrdiff_t               difference_type;
    typedef int*                    pointer;
    typedef int&                    reference;
};

struct Forward {
    typedef std::forward_iterator_tag iterator_category;
    typedef int                       value_type;
    typedef ptrdiff_t                 difference_type;
    typedef int*                      pointer;
    typedef int&                      reference;
};

struct Bidirectional {
    typedef std::bidirectional_iterator_tag iterator_category;
    typedef int                             value_type;
    typedef ptrdiff_t                       difference_type;
    typedef int*                            pointer;
    typedef int&                            reference;
};

struct Random {
    typedef std::random_access_iterator_tag iterator_category;
    typedef int                             value_type;
    typedef ptrdiff_t                       difference_type;
    typedef int*                            pointer;
    typedef int&                            reference;
};

// Enum and helper functions to map an iterator tag to an int.
enum Category {UNKNOWN, INPUT, FORWARD, BIDIRECTIONAL, RANDOM};

template<typename _Category>
Category category(_Category) {
    return UNKNOWN;
}

template<>
Category
category<std::input_iterator_tag>(std::input_iterator_tag) {
    return INPUT;
}

template<>
Category
category<std::forward_iterator_tag>(std::forward_iterator_tag) {
    return FORWARD;
}

template<>
Category
category<std::bidirectional_iterator_tag>(std::bidirectional_iterator_tag) {
    return BIDIRECTIONAL;
}

template<>
Category
category<std::random_access_iterator_tag>(std::random_access_iterator_tag) {
    return RANDOM;
}

// Check if the custom method to get the category works as expected.
bool testCategory()
{
    EXPECT_TRUE(category(android::iterator_category(Input())) == INPUT);
    EXPECT_TRUE(category(android::iterator_category(Forward())) == FORWARD);
    EXPECT_TRUE(category(android::iterator_category(Bidirectional())) == BIDIRECTIONAL);
    EXPECT_TRUE(category(android::iterator_category(Random())) == RANDOM);
    return true;
}

typedef std::__wrapper_iterator<int *, int *> WrapperIterator;

// Check we can distinguish wrapper iterators.
bool testWrapperIterator()
{
    EXPECT_FALSE(android::is_wrapper_iterator<android::Random>::value);
    EXPECT_TRUE(android::is_wrapper_iterator<android::WrapperIterator>::value);
    return true;
}

}  // namespace android

int main(int argc, char **argv)
{
    FAIL_UNLESS(testCategory);
    FAIL_UNLESS(testWrapperIterator);
    return kPassed;
}
