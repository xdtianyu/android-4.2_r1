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

#include "../include/string"
#ifndef ANDROID_ASTL_STRING__
#error "Wrong header included!!"
#endif
#include <climits>
#include <cstring>
#include <algorithm>
#include <list>
#include "common.h"


namespace android {
using std::string;

bool testConstructorCString()
{
    string empty_str1;
    EXPECT_TRUE(empty_str1.size() == 0);
    EXPECT_TRUE(empty_str1.capacity() == 0);

    string empty_str2("");
    EXPECT_TRUE(empty_str2.size() == 0);

    const char empty_as_array[] = "";
    string empty_str3(empty_as_array);
    EXPECT_TRUE(empty_str3.size() == 0);

    const char literal[] = "scott mills cracks me up";
    string str1(literal);
    EXPECT_TRUE(strcmp(literal, str1.c_str()) == 0);

    string str2(literal, 11);
    EXPECT_TRUE(strcmp("scott mills", str2.c_str()) == 0);

    string str3(literal, sizeof(literal));
    EXPECT_TRUE(strcmp(literal, str3.c_str()) == 0);

    // Pass the end of the string => still ok, there is \0
    string str4(literal, sizeof(literal) + 1);
    EXPECT_TRUE(str4.size() == sizeof(literal) + 1);

    string str5(literal, literal + 11);
    EXPECT_TRUE(strcmp("scott mills", str5.c_str()) == 0);

    const char text[] = {'l','a','d','y',' ','g','a','g','a'};

    string str6(text, ARRAYSIZE(text));
    EXPECT_TRUE(str6 == "lady gaga");

    string str7(NULL);
    EXPECT_TRUE(empty_str1.size() == 0);
    EXPECT_TRUE(empty_str1.empty());
    return true;
}

bool testConstructorString()
{
    string empty_str1;
    string empty_str2;
    EXPECT_TRUE(empty_str1.c_str() == empty_str2.c_str());

    string empty_str3(empty_str2);
    EXPECT_TRUE(empty_str3.size() == 0);

    const char string_with_nulls[] = "contains 2 \0 bytes \0.";
    string str1 (string_with_nulls, 21);
    EXPECT_TRUE(str1.size() == 21);

    string str2 (str1);
    EXPECT_TRUE(str1.size() == 21);

    const string str3("scott mills cracks me up");
    string str4(str3, 12);
    EXPECT_TRUE(strcmp("cracks me up", str4.c_str()) == 0);

    string str5(str3, 12, 6);
    EXPECT_TRUE(strcmp("cracks", str5.c_str()) == 0);

    string str6(str3, 23);
    EXPECT_TRUE(strcmp("p", str6.c_str()) == 0);

    string str7(str3, 24);
    EXPECT_TRUE(strcmp("", str7.c_str()) == 0);

    string str8(str3, 23, 1);
    EXPECT_TRUE(strcmp("p", str8.c_str()) == 0);

    string str9(str3, 24, 1);
    EXPECT_TRUE(strcmp("", str9.c_str()) == 0);

    return true;
}

bool testConstructorPointers()
{
    const string empty;
    char data[] = "a 16 char string";

    string str01(data, data + 0);
    EXPECT_TRUE(str01.c_str() == empty.c_str());

    string str02(data, data + 1);
    EXPECT_TRUE(str02 == "a");

    string str03(data + 2, data + 16);
    EXPECT_TRUE(str03 == "16 char string");

    string str04(data + 15, data + 16);
    EXPECT_TRUE(str04 == "g");

    string str05(data + 16, data + 16);
    EXPECT_TRUE(str05 == "");

    return true;
}

bool testConstructorRepeatChar()
{
    string str01(0, 'c');

    EXPECT_TRUE(str01.empty());
    EXPECT_TRUE(str01.size() == 0);
    EXPECT_TRUE(str01.capacity() == 0);

    string str02(10, 'c');

    EXPECT_TRUE(!str02.empty());
    EXPECT_TRUE(str02.size() == 10);
    EXPECT_TRUE(str02.capacity() == 10);

    for (size_t i = 0; i < 100; ++i)
    {
        string str03(i, 'x');

        EXPECT_TRUE(str03[i] == '\0');
        EXPECT_TRUE(str03.length() == i);

        str03.reserve(i + 20);
        EXPECT_TRUE(str03.capacity() == i + 20);
        EXPECT_TRUE(str03.length() == i);
        EXPECT_TRUE(str03[i] == '\0');
    }

    return true;
}

bool testConstructorInvalidValues()
{
    const string empty;
    const string str01("a 16 char string");

    EXPECT_TRUE(str01.size() == 16);

    string str02(str01, 17, 1);  // invalid index
    EXPECT_TRUE(str02.c_str() == empty.c_str());

    string str03(str01, 17, 0);  // invalid index
    EXPECT_TRUE(str03.c_str() == empty.c_str());

    string str04(str01, -1, 0);  // invalid index
    EXPECT_TRUE(str04.c_str() == empty.c_str());

    string str05(str01, 0, 17);  // invalid length -> clamped
    EXPECT_TRUE(str05 == str01);

    string str06(str01, 17);  // invalid index
    EXPECT_TRUE(str06.c_str() == empty.c_str());

    char end[] = "a string";
    char *begin = end + 1;  // begin after end.

    string str07(begin, end);
    EXPECT_TRUE(str07.c_str() == empty.c_str());

    return true;
}

bool testSize()
{
    string str01;
    EXPECT_TRUE(str01.size() == 0);
    EXPECT_TRUE(str01.length() == 0);

    str01 += "a string.";

    EXPECT_TRUE(str01.size() == 9);
    EXPECT_TRUE(str01.length() == 9);

    return true;
}

bool testCString()
{
    string str01;
    string str02;

    // Should point to the same empty string.
    EXPECT_TRUE(str01.c_str() == str02.c_str());
    // c_str() == data()
    EXPECT_TRUE(str01.c_str() == str01.data());
    EXPECT_TRUE(str01.empty());

    const char text[] = "a string";
    str01 += text;
    EXPECT_TRUE(strcmp(str01.c_str(), text) == 0);
    EXPECT_TRUE(strcmp(str01.data(), text) == 0);
    EXPECT_TRUE(!str01.empty());

    // after a clear, points back to the original empty string.
    str01.clear();
    EXPECT_TRUE(str01.c_str() == str02.c_str());
    EXPECT_TRUE(str01.empty());

    return true;
}

bool testReserve()
{
    string str01;
    size_t capacity = str01.capacity();

    EXPECT_TRUE(0 == capacity);

    str01.reserve(5);
    EXPECT_TRUE(5 == str01.capacity());
    str01.reserve(0);
    EXPECT_TRUE(0 == str01.capacity());

    string str02("7 chars");
    EXPECT_TRUE(7 == str02.capacity());
    EXPECT_TRUE(7 == str02.size());

    str02.reserve(10);
    EXPECT_TRUE(str02 == "7 chars");
    EXPECT_TRUE(10 == str02.capacity());
    EXPECT_TRUE(7 == str02.size());

    str02.reserve(6); // no effect
    EXPECT_TRUE(str02 == "7 chars");
    EXPECT_TRUE(10 == str02.capacity());
    EXPECT_TRUE(7 == str02.size());

    string str03;
    const string str04;

    // Both point to kEmptyString.
    EXPECT_TRUE(str03.c_str() == str04.c_str());

    str03.reserve();
    EXPECT_TRUE(0 == str03.capacity());
    EXPECT_TRUE(str03.c_str() == str04.c_str());

    str03.reserve(10);
    EXPECT_TRUE(10 == str03.capacity());
    // Not pointing at the empty string anymore.
    EXPECT_TRUE(str03.c_str() != str04.c_str());

    str03.reserve();
    EXPECT_TRUE(0 == str03.capacity());
    // c_str() points back to the empty string.
    EXPECT_TRUE(str03.c_str() == str04.c_str());

    str03.reserve(10);
    str03.append("7 chars");
    EXPECT_TRUE(str03 == "7 chars");
    str03.reserve();  // shrink to fit.
    EXPECT_TRUE(7 == str03.capacity());

    string str05 = "twelve chars";
    string str06 = str05;
    str05.reserve(1);
    EXPECT_TRUE(str05.capacity() == 12);


    for (size_t i = 1; i <= 100; i *= 2)
    {
        string str(i, 'x');
        str.reserve(3 * i);
        EXPECT_TRUE(str.capacity() == 3 * i);

        str.reserve(2 * i);
        EXPECT_TRUE(str.capacity() == 2 * i);

        str.reserve();
        EXPECT_TRUE(str.capacity() == i);
    }

    // Check overflow.
    string str07;

    str07.reserve(10);
    EXPECT_TRUE(str07.capacity() == 10);

    str07.reserve(kMaxSizeT);

    EXPECT_TRUE(str07.capacity() == 10);

    return true;
}

bool testAppend()
{
    string str1;
    const char *text = "You spin my head right round.";

    str1.append(text);
    EXPECT_TRUE(str1 == text);

    str1.append(" Flo Rida.");
    EXPECT_TRUE(str1 == "You spin my head right round. Flo Rida.");

    string str2;
    str2.append(str1);
    EXPECT_TRUE(str2 == "You spin my head right round. Flo Rida.");

    string str3("You spin ");
    str3.append("my head right round.");
    EXPECT_TRUE(str3 == "You spin my head right round.");

    string str4("You spin ");
    string str5("my head right round.");
    str4.append(str5);
    EXPECT_TRUE(str4 == "You spin my head right round.");

    string str6("");
    string str7("");
    str6.append(str7);
    EXPECT_TRUE(str6 == "");
    EXPECT_TRUE(str6.empty());

    string str8;
    str8.append("a");
    EXPECT_TRUE(str8 == "a");

    const char more_text[] = {'l','a','d','y',' ','g','a','g','a'};

    string str9;
    str9.append(more_text, ARRAYSIZE(more_text));
    EXPECT_TRUE(str9 == string(more_text, ARRAYSIZE(more_text)));

    string str10;
    str10.append("", 0);
    EXPECT_TRUE(str10.size() == 0 );
    str10.append(text, strlen(text));
    EXPECT_TRUE(str10 == "You spin my head right round.");

    string str11;
    str11.append("You spin my head right round.", 5, 11);

    EXPECT_TRUE(str11 == "pin my head");

    // Append overflow
    string str12("original");
    char dummy[] = "unused";
    // We lie about the size but that is ok. Since the lenght of the new string
    // is going to be kMaxSizeT, the call will have not effect (there is no
    // space for the trailing '\0').
    str12.append(dummy, kMaxSizeT);
    EXPECT_TRUE(str12 == "original");

    // Append iterator.
    {
        string str1("once upon ");
        const string str2("a time");

        str1.append(str2.begin(), str2.end());
        EXPECT_TRUE(str1.size() == 16);
        EXPECT_TRUE(str1 == "once upon a time");
    }
    {
        string str1("once upon ");
        string str2("a time");

        str1.append(str2.begin(), str2.begin());
        EXPECT_TRUE(str1.size() == 10);
        EXPECT_TRUE(str1 == "once upon ");
    }
    {
        string str1;
        string str2("hello");

        str1.append(str2.begin(), str2.end());
        EXPECT_TRUE(str1.size() == 5);
        EXPECT_TRUE(str1 == "hello");
    }
    {
        string str1("hello ");
        std::list<char> list1;
        list1.push_back('w');
        list1.push_back('o');
        list1.push_back('r');
        list1.push_back('l');
        list1.push_back('d');
        str1.append(list1.begin(), list1.end());
        EXPECT_TRUE(str1 == "hello world");
    }
    return true;
}

bool testAppendOperator()
{
    string str1;
    const char *text = "You spin my head right round.";

    str1 += text;
    EXPECT_TRUE(str1 == text);

    str1 += " Flo Rida.";
    EXPECT_TRUE(str1 == "You spin my head right round. Flo Rida.");

    string str2;
    str2 += str1;
    EXPECT_TRUE(str2 == "You spin my head right round. Flo Rida.");

    string str3("You spin ");
    str3 += "my head right round.";
    EXPECT_TRUE(str3 == "You spin my head right round.");

    string str4("You spin ");
    string str5("my head right round.");
    str4 += str5;
    EXPECT_TRUE(str4 == "You spin my head right round.");

    string str6("");
    string str7("");
    str6 += str7;
    EXPECT_TRUE(str6 == "");
    EXPECT_TRUE(str6.empty());

    string str8;
    str8 += "a";
    EXPECT_TRUE(str8 == "a");

    const char more_text[] = {'l','a','d','y',' ','g','a','g','a'};

    string str9;
    for (size_t i = 0; i < ARRAYSIZE(more_text); ++i)
    {
        str9 += more_text[i];
    }
    EXPECT_TRUE(str9 == "lady gaga");

    str9 += (const char *)NULL;
    EXPECT_TRUE(str9 == "lady gaga");

    string str10(more_text, ARRAYSIZE(more_text));
    EXPECT_TRUE(str10 == "lady gaga");
    str10 += '\0';
    EXPECT_TRUE(str10 == "lady gaga");
    EXPECT_TRUE(str10 == string("lady gaga\0", 10));
    str10 += 'x';
    EXPECT_TRUE(str10 == string("lady gaga\0x", 11));
    EXPECT_TRUE(str10[11] == '\0');

    return true;
}


bool testCompare()
{
    string str01("bell helmet");
    string str02("bell moto");
    string str03("bell");
    string str04("bell pants");
    string str05;

    str05 = str01;
    // Compare with self.
    EXPECT_TRUE(str01 == str01);
    EXPECT_TRUE(!(str01 != str01));

    EXPECT_TRUE(str01 == str05);
    EXPECT_TRUE(str05 == str01);
    EXPECT_TRUE(!(str01 != str05));
    EXPECT_TRUE(!(str05 != str01));

    EXPECT_TRUE(str01 != str02);
    EXPECT_TRUE(str01 != str03);
    EXPECT_TRUE(str01 != str04);

    // Compare with literals.
    EXPECT_TRUE(str01 == "bell helmet");
    EXPECT_TRUE(!(str01 != "bell helmet"));
    EXPECT_TRUE("bell helmet" == str01);
    EXPECT_TRUE(!("bell helmet" != str01));

    // Compare with char array.
    char array[] = { 'a', ' ', 'b', 'u', 'g', '\0'};
    str01 = "a bug";
    EXPECT_TRUE(array == str01);

    EXPECT_TRUE(strcmp("a bug", "a bugg") < 0);

    char array2[] = { 'a', 'b', 'u', 'g', 'g' };
    EXPECT_TRUE(str01.compare(array2) < 0);

    string str06;
    EXPECT_TRUE(str06 != NULL);
    {
        string str_long("this is");
        string str_short("it");
        EXPECT_TRUE(str_long > str_short);
        EXPECT_TRUE(str_long >= str_short);
        EXPECT_FALSE(str_long < str_short);
        EXPECT_FALSE(str_long <= str_short);
    }
    {
        string str_lhs("this is");
        string str_rhs("this is");
        EXPECT_FALSE(str_lhs > str_rhs);
        EXPECT_TRUE(str_lhs >= str_rhs);
        EXPECT_FALSE(str_lhs < str_rhs);
        EXPECT_TRUE(str_lhs <= str_rhs);
    }
    return true;
}

bool testSwap()
{
    string str01;
    string str02("test");

    str01.swap(str02);
    EXPECT_TRUE(str02.empty());
    EXPECT_TRUE(str01 == "test");

    string str03("altima");
    string str04("versa");
    str03.swap(str04);
    EXPECT_TRUE(str03 == "versa");
    EXPECT_TRUE(str04 == "altima");

    {
        string empty;
        // swap can be used to clean strings
        str04.swap(empty);
    }
    EXPECT_TRUE(str04.empty());

    return true;
}

bool testAccessor()
{
    string str01 = "earmarks";

    EXPECT_TRUE(str01[0] == 'e');
    EXPECT_TRUE(str01[7] == 's');

    str01[0] = 'E';
    str01[7] = 'S';
    EXPECT_TRUE(str01 == "EarmarkS");

    for (int i = 0; i < 100; ++i)
    {
        string str02(i, 'x');

        str02.reserve(20);

        EXPECT_TRUE(str02[i] == '\0');

        const string str03(str02);
        EXPECT_TRUE(str03[i] == '\0');
    }

    string str05;
    str05.reserve(100);
    str05[99] = 'a';

    // 'at'
    EXPECT_TRUE(str01.at(0) == 'E');
    EXPECT_TRUE(str01.at(7) == 'S');
    EXPECT_TRUE(str01.at(8) == 'X');  // 'X' is the dummy value returned.

    str01.at(1) = 'A';
    str01.at(6) = 'K';
    EXPECT_TRUE(str01 == "EArmarKS");
    return true;
}


bool testAssignment()
{
    const char *literal = "Need to buy a full face helmet for Lilie.";
    const string str01 = literal;

    EXPECT_TRUE(str01.length() == strlen(literal));
    EXPECT_TRUE(str01.size() == strlen(literal));
    EXPECT_TRUE(str01.capacity() == strlen(literal));
    EXPECT_TRUE(str01 == literal);

    string str02;

    str02.assign(str01, 8, 33);
    EXPECT_TRUE(str02 == "buy a full face helmet for Lilie.");

    str02.assign(str01, 8, 0);
    EXPECT_TRUE(str02 == "");

    str02.assign(str01, 0, 7);
    EXPECT_TRUE(str02 == "Need to");

    str02.assign("unchanged");
    str02.assign(str01, 35, 1000);
    EXPECT_TRUE(str02 == "unchanged");

    str02.assign(str01, 35, 6);
    EXPECT_TRUE(str02 == "Lilie.");


    str02.assign(str01, 35, 5);
    EXPECT_TRUE(str02 == "Lilie");

    string str03;

    str03.assign(literal);
    EXPECT_TRUE(str03 == "Need to buy a full face helmet for Lilie.");

    string str04;

    str04.assign(str03.c_str());
    EXPECT_TRUE(str04 == "Need to buy a full face helmet for Lilie.");

    str04.assign(str03.c_str() + 5, 10);
    EXPECT_TRUE(str04 == "to buy a f");

    str04.assign("noop");
    str04.assign(NULL);
    EXPECT_TRUE(str04 == "noop");

    str04.assign(str01, str01.size() - 1, 1);
    EXPECT_TRUE(str04 == ".");

    str04.assign("unchanged");
    str04.assign(str01, str01.size(), 1);
    str04.assign(NULL, 4, 1);
    str04.assign(NULL, 4);
    EXPECT_TRUE(str04 == "unchanged");

    return true;
}

bool testCopy()
{
    string data[] = {"one", "two", "three", "four", "five", "six"};
    std::copy(data + 2, data + 5, data);
    EXPECT_TRUE(data[0] == "three");
    EXPECT_TRUE(data[1] == "four");
    EXPECT_TRUE(data[2] == "five");
    EXPECT_TRUE(data[3] == "four");
    EXPECT_TRUE(data[4] == "five");
    EXPECT_TRUE(data[5] == "six");
    return true;
}


bool testConcat()
{
    string str01("The full");
    string str02(" sentence.");
    string str03;

    str03 = str01 + str02;
    EXPECT_TRUE(str03 == "The full sentence.");

    str03 = str02 + str01;
    EXPECT_TRUE(str03 == " sentence.The full");


    str03 = str01 + " sentence.";
    EXPECT_TRUE(str03 == "The full sentence.");

    str03 = "The full" + str02;
    EXPECT_TRUE(str03 == "The full sentence.");

    str03 = 'l' + str02;
    str03 = 'l' + str03;
    str03 = 'u' + str03;
    str03 = 'f' + str03;
    str03 = ' ' + str03;
    str03 = 'e' + str03;
    str03 = 'h' + str03;
    str03 = 'T' + str03;
    EXPECT_TRUE(str03 == "The full sentence.");

    str03 = "The full ";
    str03 = str03 + 's';
    str03 = str03 + 'e';
    str03 = str03 + 'n';
    str03 = str03 + 't';
    str03 = str03 + 'e';
    str03 = str03 + 'n';
    str03 = str03 + 'c';
    str03 = str03 + 'e';
    str03 = str03 + '.';
    EXPECT_TRUE(str03 == "The full sentence.");

    // Check the new string buffer is not the same as the original one.
    string str04("left and");
    string str05(" right");
    string str06(str04 + str05);

    EXPECT_TRUE(str06 == "left and right");
    EXPECT_TRUE(str06.c_str() != str04.c_str());
    EXPECT_TRUE(str06.c_str() != str05.c_str());

    str06 = str04 + str05;
    EXPECT_TRUE(str06 == "left and right");
    EXPECT_TRUE(str06.c_str() != str04.c_str());
    EXPECT_TRUE(str06.c_str() != str05.c_str());
    return true;
}

bool testPushBack()
{
    string str01;

    str01.push_back('a');
    EXPECT_TRUE(str01 == "a");
    EXPECT_TRUE(str01.capacity() == 1);

    str01.reserve(10);
    str01.push_back('b');
    EXPECT_TRUE(str01 == "ab");
    EXPECT_TRUE(str01.capacity() == 10);
    EXPECT_TRUE(str01[2] == '\0');

    str01.reserve();
    EXPECT_TRUE(str01 == "ab");
    EXPECT_TRUE(str01.capacity() == 2);
    EXPECT_TRUE(str01[2] == '\0');

    return true;
}

bool testFind()
{
  string haystack("one two three one two three");

  // Don't die on null strings
  EXPECT_TRUE(haystack.find((char*)NULL) == string::npos);
  EXPECT_TRUE(haystack.find((char*)NULL, 10) == string::npos);

  // C strings.
  EXPECT_TRUE(haystack.find("one") == 0);
  EXPECT_TRUE(haystack.find("two") == 4);
  EXPECT_TRUE(haystack.find("t") == 4);
  EXPECT_TRUE(haystack.find("four") == string::npos);
  EXPECT_TRUE(haystack.find("one", string::npos) == string::npos);

  // with offset
  EXPECT_TRUE(haystack.find("one", 13) == 14);
  EXPECT_TRUE(haystack.find("one", 14) == 14);
  EXPECT_TRUE(haystack.find("one", 15) == string::npos);
  EXPECT_TRUE(haystack.find("e", haystack.size() - 1) == haystack.size() - 1);
  EXPECT_TRUE(haystack.find("e", haystack.size()) == string::npos);
  EXPECT_TRUE(haystack.find("one", string::npos) == string::npos);

  // std::string
  EXPECT_TRUE(haystack.find(string("one")) == 0);
  EXPECT_TRUE(haystack.find(string("two")) == 4);
  EXPECT_TRUE(haystack.find(string("t")) == 4);
  EXPECT_TRUE(haystack.find(string("four")) == string::npos);
  EXPECT_TRUE(haystack.find(string("one"), string::npos) == string::npos);

  // with offset
  EXPECT_TRUE(haystack.find(string("one"), 13) == 14);
  EXPECT_TRUE(haystack.find(string("one"), 14) == 14);
  EXPECT_TRUE(haystack.find(string("one"), 15) == string::npos);
  EXPECT_TRUE(haystack.find(string("e"), haystack.size() - 1) == haystack.size() - 1);
  EXPECT_TRUE(haystack.find(string("e"), haystack.size()) == string::npos);
  EXPECT_TRUE(haystack.find(string("one"), string::npos) == string::npos);

  // Emtpy string should be found at every position in a string except
  // past the end.
  EXPECT_TRUE(string().find("", 0) == 0);
  EXPECT_TRUE(string().find(string(), 0) == 0);
  EXPECT_TRUE(string().find(string(), 10) == string::npos);

  string foo = "foo";
  EXPECT_TRUE(foo.find("", 0) == 0);
  EXPECT_TRUE(foo.find(string(), 0) == 0);
  EXPECT_TRUE(foo.find(string(""), 0) == 0);

  EXPECT_TRUE(foo.find("", 1) == 1);
  EXPECT_TRUE(foo.find(string(), 1) == 1);
  EXPECT_TRUE(foo.find(string(""), 1) == 1);

  EXPECT_TRUE(foo.find("", foo.size()) == foo.size());
  EXPECT_TRUE(foo.find(string(), foo.size()) == foo.size());
  EXPECT_TRUE(foo.find(string(""), foo.size()) == foo.size());

  EXPECT_TRUE(foo.find("", foo.size() + 1) == string::npos);
  EXPECT_TRUE(foo.find(string(), foo.size() + 1) == string::npos);
  EXPECT_TRUE(foo.find(string(""), foo.size() + 1) == string::npos);

  // Find on an empty string a non empty one should fail
  EXPECT_TRUE(string().find("f", 0) == string::npos);
  EXPECT_TRUE(string().find(string("f"), 0) == string::npos);
  return true;
}

bool testCapacity()
{
  string empty_string;

  EXPECT_TRUE(empty_string.capacity() == 0);
  EXPECT_TRUE(empty_string.size() == 0);

  const char *text = "non empty string";
  const size_t len = strlen(text);
  string str01(text);

  EXPECT_TRUE(str01.capacity() == len);
  EXPECT_TRUE(str01.size() == len);
  return true;
}

bool testClear()
{
  string empty_string;

  empty_string.clear();
  EXPECT_TRUE(empty_string.capacity() == 0);
  EXPECT_TRUE(empty_string.size() == 0);

  string str01("non empty string");

  str01.clear();
  EXPECT_TRUE(str01.capacity() == 0);
  EXPECT_TRUE(str01.size() == 0);
  EXPECT_TRUE(str01.empty());
  return true;
}

bool testErase()
{
  {
    string empty_string;

    empty_string.erase();
    EXPECT_TRUE(empty_string.capacity() == 0);
    EXPECT_TRUE(empty_string.size() == 0);

    empty_string.erase(kMaxSizeT);
    EXPECT_TRUE(empty_string.capacity() == 0);
    EXPECT_TRUE(empty_string.size() == 0);

    empty_string.erase(kMaxSizeT, kMaxSizeT);
    EXPECT_TRUE(empty_string.capacity() == 0);
    EXPECT_TRUE(empty_string.size() == 0);
  }

  {
    string str01("a");

    str01.erase();
    EXPECT_TRUE(str01.capacity() == 1);
    EXPECT_TRUE(str01.size() == 0);
  }

  {
    string str02("a");

    str02.erase(kMaxSizeT);
    EXPECT_TRUE(str02.capacity() == 1);
    EXPECT_TRUE(str02.size() == 1);
  }

  {
    string str03("a");

    str03.erase(0, kMaxSizeT);
    EXPECT_TRUE(str03.capacity() == 1);
    EXPECT_TRUE(str03.size() == 0);
  }

  {
    string str04("a");

    str04.erase(1, kMaxSizeT);
    EXPECT_TRUE(str04.capacity() == 1);
    EXPECT_TRUE(str04.size() == 1);
  }

  {
    string str05("abcd");

    str05.erase(1, 2);
    EXPECT_TRUE(str05.capacity() == 4);
    EXPECT_TRUE(str05.size() == 2);
    EXPECT_TRUE(str05 == "ad");
  }

  {
    string str06("abcd");

    str06.erase(0, 1);
    EXPECT_TRUE(str06.capacity() == 4);
    EXPECT_TRUE(str06.size() == 3);
    EXPECT_TRUE(str06 == "bcd");
  }

  {
    // overlap
    string str07("oh my god (You think I'm in control)");

    str07.erase(0, strlen("oh my god "));
    EXPECT_TRUE(str07.size() == 26);
    EXPECT_TRUE(str07 == "(You think I'm in control)");
  }

  return true;
}

// Checks an iterator can be cast to a const one.
bool testConstIterator()
{
    string s("a string");
    string::iterator i = s.begin();
    string::const_iterator ci = s.begin();
    return true;
}

bool testForwardIterator()
{
    string s("a string");
    char chars[] = "a string";
    string::iterator iter = s.begin();
    for (int i = 0; iter != s.end(); ++i) {
        EXPECT_TRUE(*iter == chars[i]);
        ++iter;
    }
    EXPECT_TRUE(iter == s.end());

    string empty;
    EXPECT_TRUE(empty.begin() == empty.end());
    return true;
}

bool testSubstr() {
    {
        string s;
        string res = s.substr(10, 1);
        EXPECT_TRUE(res.empty());
    }
    {
        string s = "pandora radio";
        string res = s.substr(string::npos, 1);
        EXPECT_TRUE(res.empty());
    }
    {
        string s = "pandora radio";
        string res = s.substr(5, 1000);
        EXPECT_TRUE(res == "ra radio");
    }
    {
        string s = "pandora radio";
        string res = s.substr(5, 0);
        EXPECT_TRUE(res.empty());
    }
    {
        string s = "pandora radio";
        string res = s.substr(5, 5);
        EXPECT_TRUE(res == "ra ra");
    }
    return true;
}

bool testCharSearch() {
    {
        string s;
        EXPECT_TRUE(s.find_first_of('a') == string::npos);
        s = "abracadabra";
        EXPECT_TRUE(s.find_first_of('a') == 0);
        EXPECT_TRUE(s.find_first_of('a', 0) == 0);
        EXPECT_TRUE(s.find_first_of('a', 1) == 3);
        EXPECT_TRUE(s.find_first_of('a', 8) == 10);
        s = "zzzzzzza";
        EXPECT_TRUE(s.find_first_of('a') == 7);
        EXPECT_TRUE(s.find_first_of('a', 8) == string::npos); // out of bound
    }
    // For char (set of size 1) find_first_of is equive to find(char, pos)
    {
        string s;
        EXPECT_TRUE(s.find('a') == string::npos);
        s = "abracadabra";
        EXPECT_TRUE(s.find('a') == 0);
        EXPECT_TRUE(s.find('a', 0) == 0);
        EXPECT_TRUE(s.find('a', 1) == 3);
        EXPECT_TRUE(s.find('a', 8) == 10);
        s = "zzzzzzza";
        EXPECT_TRUE(s.find('a') == 7);
        EXPECT_TRUE(s.find('a', 8) == string::npos); // out of bound
    }
    {
        string s;
        EXPECT_TRUE(s.find_last_of('a') == string::npos);
        EXPECT_TRUE(s.find_last_of('a', 0) == string::npos);
        EXPECT_TRUE(s.find_last_of('a', 10) == string::npos);
        s = "abracadabra";
        EXPECT_TRUE(s.find_last_of('a', 10) == 10);
        EXPECT_TRUE(s.find_last_of('a', 9) == 7);
        EXPECT_TRUE(s.find_last_of('a', 0) == 0);
        s = "azzzzzzz";
        EXPECT_TRUE(s.find_last_of('a') == 0);
    }
    // For char (set of size 1) find_last_of is equiv to rfind(char, pos).
    {
        string s;
        EXPECT_TRUE(s.rfind('a') == string::npos);
        EXPECT_TRUE(s.rfind('a', 0) == string::npos);
        EXPECT_TRUE(s.rfind('a', 10) == string::npos);
        s = "abracadabra";
        EXPECT_TRUE(s.rfind('a', 10) == 10);
        EXPECT_TRUE(s.rfind('a', 9) == 7);
        EXPECT_TRUE(s.rfind('a', 0) == 0);
        s = "azzzzzzz";
        EXPECT_TRUE(s.rfind('a') == 0);
    }
    {
        string s;
        EXPECT_TRUE(s.find_first_not_of('a') == string::npos);
        s = "abracadabra";
        EXPECT_TRUE(s.find_first_not_of('a') == 1);
        EXPECT_TRUE(s.find_first_not_of('a', 0) == 1);
        EXPECT_TRUE(s.find_first_not_of('a', 1) == 1);
        EXPECT_TRUE(s.find_first_not_of('a', 7) == 8);
        s = "zzzzzzza";
        EXPECT_TRUE(s.find_first_not_of('a') == 0);
        EXPECT_TRUE(s.find_first_not_of('a', 8) == string::npos); // out of bound
    }
    {
        string s;
        EXPECT_TRUE(s.find_last_not_of('a') == string::npos);
        EXPECT_TRUE(s.find_last_not_of('a', 0) == string::npos);
        EXPECT_TRUE(s.find_last_not_of('a', 10) == string::npos);
        s = "abracadabra";
        EXPECT_TRUE(s.find_last_not_of('a') == 9);
        EXPECT_TRUE(s.find_last_not_of('a', 10) == 9);
        EXPECT_TRUE(s.find_last_not_of('a', 9) == 9);
        EXPECT_TRUE(s.find_last_not_of('a', 0) == string::npos);
        s = "azzzzzzz";
        EXPECT_TRUE(s.find_last_not_of('a') == 7);
    }
    return true;
}


bool testInsert() {
    {
        string::iterator res;
        string str("zzzzzz");
        res = str.insert(str.begin(), 'a');
        EXPECT_TRUE(str == "azzzzzz");
        EXPECT_TRUE(*res == 'a');

        res = str.insert(str.begin() + 3, 'b');
        EXPECT_TRUE(str == "azzbzzzz");
        EXPECT_TRUE(*res == 'b');

        res = str.insert(str.end(), 'c');
        EXPECT_TRUE(str == "azzbzzzzc");
        EXPECT_TRUE(*res == 'c');
    }
    {
        string str;
        string::iterator res = str.insert(str.begin(), 'a');
        EXPECT_TRUE(str == "a");
        EXPECT_TRUE(*res == 'a');
    }
    return true;
}

}  // namespace android

int main(int argc, char **argv)
{
    FAIL_UNLESS(testConstructorCString);
    FAIL_UNLESS(testConstructorString);
    FAIL_UNLESS(testConstructorRepeatChar);
    FAIL_UNLESS(testConstructorPointers);
    FAIL_UNLESS(testConstructorInvalidValues);
    FAIL_UNLESS(testSize);
    FAIL_UNLESS(testCString);
    FAIL_UNLESS(testAppend);
    FAIL_UNLESS(testAppendOperator);
    FAIL_UNLESS(testConcat);
    FAIL_UNLESS(testAssignment);
    FAIL_UNLESS(testCopy);
    FAIL_UNLESS(testReserve);
    FAIL_UNLESS(testCompare);
    FAIL_UNLESS(testAccessor);
    FAIL_UNLESS(testSwap);
    FAIL_UNLESS(testPushBack);
    FAIL_UNLESS(testFind);
    FAIL_UNLESS(testCapacity);
    FAIL_UNLESS(testClear);
    FAIL_UNLESS(testErase);
    FAIL_UNLESS(testConstIterator);
    FAIL_UNLESS(testForwardIterator);
    FAIL_UNLESS(testSubstr);
    FAIL_UNLESS(testCharSearch);
    FAIL_UNLESS(testInsert);
    return kPassed;
}
