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

#include "../include/set"
#ifndef ANDROID_ASTL_SET__
#error "Wrong header included!!"
#endif
#include <climits>
#include <cstring>
#include <string>
#include "common.h"

namespace android {
using std::pair;
using std::set;
using std::string;

bool testConstructor()
{
    set<int> s;
    EXPECT_TRUE(s.empty());
    EXPECT_TRUE(s.size() == 0);
    EXPECT_TRUE(s.begin() == s.end());
    EXPECT_TRUE(s.count(10) == 0);
    return true;
}

bool testInsertPOD()
{
    set<int> s;
    pair<set<int>::iterator, bool> res;

    EXPECT_TRUE(s.count(10) == 0);

    res = s.insert(10);

    // begin should point to the element inserted.
    EXPECT_TRUE(res.first == s.begin());
    EXPECT_TRUE(s.end() != s.begin());
    EXPECT_TRUE(*(res.first) == 10);
    set<int>::iterator elt_in_set = res.first;
    EXPECT_TRUE(*(s.begin()) == 10);

    // insert was a success.
    EXPECT_TRUE(res.second);

    // element can be found
    EXPECT_TRUE(s.count(10) == 1);

    // Try to insert the same element again, this time it should fail.
    res = s.insert(10);
    // insert was a failure.
    EXPECT_TRUE(!res.second);

    // Insert should return an iterator pointing to the element
    // already in the set.
    EXPECT_TRUE(res.first == elt_in_set);

    // element can still be found
    EXPECT_TRUE(s.count(10) == 1);
    return true;
}

bool testInsertString()
{
    set<string> s;
    pair<set<string>::iterator, bool> res;
    string str("a string");
    string str_equiv("a string");
    string str_missing("a string not in the set");

    EXPECT_TRUE(s.count(str) == 0);

    res = s.insert(str);

    // begin should point to the element inserted.
    EXPECT_TRUE(res.first == s.begin());
    set<string>::iterator marker = res.first;
    EXPECT_TRUE(s.end() != s.begin());
    EXPECT_TRUE(*(res.first) == str);
    EXPECT_TRUE(*(s.begin()) == str);

    // insert was a success.
    EXPECT_TRUE(res.second);

    // element can be found
    EXPECT_TRUE(s.count(str) == 1);

    // Try to insert an element equivalent.
    res = s.insert(str_equiv);

    // insert did not happen since there is one string equivalent
    // already.
    EXPECT_TRUE(!res.second);

    // The iterator points to the copy already in the set.
    EXPECT_TRUE(res.first == marker);

    // element can still be found
    EXPECT_TRUE(s.count(str) == 1);
    EXPECT_TRUE(s.count(str_equiv) == 1);
    return true;
}

}  // namespace android

int main(int argc, char **argv)
{
    FAIL_UNLESS(testConstructor);
    FAIL_UNLESS(testInsertPOD);
    FAIL_UNLESS(testInsertString);
    return kPassed;
}
