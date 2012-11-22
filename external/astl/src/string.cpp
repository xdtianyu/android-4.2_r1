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

#include <string>
#include <algorithm>
#include <climits>
#include <cstddef>
#include <cstring>
#include <malloc.h>
#include <ostream>

#ifndef MAX_SIZE_T
#define MAX_SIZE_T           (~(size_t)0)
#endif

namespace {
char kEmptyString[1] = { '\0' };
// Dummy char used in the 'at' accessor when the index is out of
// range.
char sDummy;
}

namespace std {
// Implementation of the std::string class.
//
// mData points either to a heap allocated array of bytes or the constant
// kEmptyString when empty and reserve has not been called.
//
// The size of the buffer pointed by mData is mCapacity + 1.
// The extra byte is need to store the '\0'.
//
// mCapacity is either mLength or the number of bytes reserved using
// reserve(int)).
//
// mLength is the number of char in the string, excluding the terminating '\0'.
//
// TODO: replace the overflow checks with some better named macros.
//
// Allocate n + 1 number of bytes for the string. Update mCapacity.
// Ensure that mCapacity + 1 and mLength + 1 is accessible.
// In case of error the original state of the string is restored.
// @param n Number of bytes requested. String allocate n + 1 to hold
//            the terminating '\0'.
// @return true if the buffer could be allocated, false otherwise.
bool string::SafeMalloc(size_type n)
{
    // Not empty and no overflow
    if (n > 0 && n + 1 > n)
    {
        value_type *oldData = mData;

        mData = static_cast<value_type *>(::malloc(n + 1));
        if (NULL != mData)
        {
            mCapacity = n;
            return true;
        }
        mData = oldData;  // roll back
    }
    return false;
}

// Resize the buffer pointed by mData if n >= mLength.
// mData points to an allocated buffer or the empty string.
// @param n The number of bytes for the internal buffer.
//            Must be > mLength and > 0.
void string::SafeRealloc(size_type n)
{
    // truncation or nothing to do or n too big (overflow)
    if (n < mLength || n == mCapacity || n + 1 < n) {
        return;
    }

    if (kEmptyString == mData)
    {
        if (SafeMalloc(n)) {
            *mData = '\0';
        }
        return;
    }

    value_type *oldData = mData;

    mData = static_cast<char*>(::realloc(mData, n + 1));
    if (NULL == mData) // reallocate failed.
    {
        mData = oldData;
    }
    else
    {
        mCapacity = n;
    }
}

void string::SafeFree(value_type *buffer)
{
    if (buffer != kEmptyString)
    {
        ::free(buffer);
    }
}

// If the memory is on the heap, release it. Do nothing we we point at the empty
// string. On return mData points to str.
void string::ResetTo(value_type *str)
{
    SafeFree(mData);
    mData = str;
}

void string::ConstructEmptyString()
{
    mData = kEmptyString;
    mLength = 0;
    mCapacity = 0;
}

void string::Constructor(const value_type *str, size_type n)
{
    Constructor(str, 0, n);
}


void string::Constructor(const value_type *str, size_type pos, size_type n)
{
    // Enough data and no overflow
    if (SafeMalloc(n))
    {
        memcpy(mData, str + pos, n);
        mLength = n;
        mData[mLength] = '\0';
        return;  // Success
    }
    ConstructEmptyString();
}

void string::Constructor(size_type n, char c)
{
    // Enough data and no overflow

    if (SafeMalloc(n))
    {
        memset(mData, c, n);
        mLength = n;
        mData[mLength] = '\0';
        return;  // Success
    }
    ConstructEmptyString();
}

string::string()
{
    ConstructEmptyString();
}

string::string(const string& str)
{
    Constructor(str.mData, str.mLength);
}

string::string(const string& str, size_type pos, size_type n)
{
    if (pos < str.mLength)
    {
        if (n > (str.mLength - pos)) {
            n = str.mLength - pos;
        }
        Constructor(str.mData + pos , n);
    }
    else
    {
        ConstructEmptyString();
    }
}

string::string(const string& str, size_type pos)
{
    if (pos < str.mLength)
    {
        Constructor(str.mData, pos, str.mLength - pos);
    }
    else
    {
        ConstructEmptyString();
    }
}

string::string(const value_type *str)
{
    if (NULL != str)
    {
        Constructor(str, traits_type::length(str));
    }
    else
    {
        ConstructEmptyString();
    }
}

string::string(const value_type *str, size_type n)
{
    Constructor(str, n);
}

// Char repeat constructor.
string::string(size_type n, char c)
{
    Constructor(n, c);
}

string::string(const value_type *begin, const value_type *end)
{
    if (begin < end)
    {
        Constructor(begin, end - begin);
    }
    else
    {
        ConstructEmptyString();
    }
}

string::~string()
{
    clear();  // non virtual, ok to call.
}

void string::clear()
{
    mCapacity = 0;
    mLength = 0;
    ResetTo(kEmptyString);
}

string& string::erase(size_type pos, size_type n)
{
    if (pos >= mLength || 0 == n)
    {
        return *this;
    }
    // start of the characters left which need to be moved down.
    const size_t remainder = pos + n;

    // Truncate, even if there is an overflow.
    if (remainder >= mLength || remainder < pos)
    {
        *(mData + pos) = '\0';
        mLength = pos;
        return *this;
    }
    // remainder < mLength and allocation guarantees to be at least
    // mLength + 1
    size_t left = mLength - remainder + 1;
    value_type *d = mData + pos;
    value_type *s = mData + remainder;
    memmove(d, s, left);
    mLength -= n;
    return *this;
}

void string::Append(const value_type *str, size_type n)
{
    const size_type total_len = mLength + n;

    // n > 0 and no overflow for the string length + terminating null.
    if (n > 0 && (total_len + 1) > mLength)
    {
        if (total_len > mCapacity)
        {
            reserve(total_len);
            if (total_len > mCapacity)
            {  // something went wrong in the reserve call.
                return;
            }
        }
        memcpy(mData + mLength, str, n);
        mLength = total_len;
        mData[mLength] = '\0';
    }
}

string& string::append(const value_type *str)
{
    if (NULL != str)
    {
        Append(str, traits_type::length(str));
    }
    return *this;
}

string& string::append(const value_type *str, size_type n)
{
    if (NULL != str)
    {
        Append(str, n);
    }
    return *this;
}

string& string::append(const value_type *str, size_type pos, size_type n)
{
    if (NULL != str)
    {
        Append(str + pos, n);
    }
    return *this;
}

string& string::append(const string& str)
{
    Append(str.mData, str.mLength);
    return *this;
}

// Specialization to append from other strings' iterators.
template<>
string& string::append<__wrapper_iterator<const char *,string> >(
    __wrapper_iterator<const char *,string> first,
    __wrapper_iterator<const char *,string> last) {
    Append(&*first, std::distance(first, last));
    return *this;
}
template<>
string& string::append<__wrapper_iterator<char *,string> >(
    __wrapper_iterator<char *,string> first,
    __wrapper_iterator<char *,string> last) {
    Append(&*first, std::distance(first, last));
    return *this;
}

void string::push_back(const char c)
{
    // Check we don't overflow.
    if (mLength + 2 > mLength)
    {
        const size_type total_len = mLength + 1;

        if (total_len > mCapacity)
        {
            reserve(total_len);
            if (total_len > mCapacity)
            {  // something went wrong in the reserve call.
                return;
            }
        }
        *(mData + mLength) = c;
        ++mLength;
        mData[mLength] = '\0';
    }
}


int string::compare(const string& other) const
{
    if (this == &other)
    {
        return 0;
    }
    else if (mLength == other.mLength)
    {
        return memcmp(mData, other.mData, mLength);
    }
    else
    {
        return mLength < other.mLength ? -1 : 1;
    }
}

int string::compare(const value_type *other) const
{
    if (NULL == other)
    {
        return 1;
    }
    return strcmp(mData, other);
}

bool operator==(const string& left, const string& right)
{
    if (&left == &right) {
        return true;
    }
    return (left.size() == right.size() &&
            !char_traits<char>::compare(left.mData, right.mData, left.size()));
}

bool operator==(const string& left, const string::value_type *right)
{
    if (NULL == right) {
        return false;
    }
    // We can use strcmp here because even when the string is build from an
    // array of char we insert the terminating '\0'.
    return std::strcmp(left.mData, right) == 0;
}

void string::reserve(size_type size)
{
    if (0 == size)
    {
        if (0 == mCapacity)
        {
            return;
        }
        else if (0 == mLength)
        {  // Shrink to fit an empty string.
            mCapacity = 0;
            ResetTo(kEmptyString);
        }
        else
        {  // Shrink to fit a non empty string
            SafeRealloc(mLength);
        }
    }
    else if (size > mLength)
    {
        SafeRealloc(size);
    }
}

void string::swap(string& other)
{
    if (this == &other) return;
    value_type *const tmp_mData = mData;
    const size_type tmp_mCapacity = mCapacity;
    const size_type tmp_mLength = mLength;

    mData = other.mData;
    mCapacity = other.mCapacity;
    mLength = other.mLength;

    other.mData = tmp_mData;
    other.mCapacity = tmp_mCapacity;
    other.mLength = tmp_mLength;
}

const char& string::operator[](const size_type pos) const
{
    return mData[pos];
}

char& string::operator[](const size_type pos)
{
    return mData[pos];
}

const char& string::at(const size_type pos) const
{
    if (pos < mLength) {
        return mData[pos];
    } else {
        sDummy = 'X';
        return sDummy;
    }
}

char& string::at(const size_type pos)
{
    if (pos < mLength) {
        return mData[pos];
    } else {
        sDummy = 'X';
        return sDummy;
    }
}

string& string::assign(const string& str)
{
    clear();
    Constructor(str.mData, str.mLength);
    return *this;
}

string& string::assign(const string& str, size_type pos, size_type n)
{
    if (pos >= str.mLength)
    {  // pos is out of bound
        return *this;
    }
    if (n <= str.mLength - pos)
    {
        clear();
        Constructor(str.mData, pos, n);
    }
    return *this;
}

string& string::assign(const value_type *str)
{
    if (NULL == str)
    {
        return *this;
    }
    clear();
    Constructor(str, traits_type::length(str));
    return *this;
}

string& string::assign(const value_type *array, size_type n)
{
    if (NULL == array || 0 == n)
    {
        return *this;
    }
    clear();
    Constructor(array, n);
    return *this;
}

string::iterator string::insert(iterator iter, char c) {
    const size_type new_len = mLength + 1;
    char *base = iter.base();

    if (base < mData || base > mData + mLength || new_len < mLength) {
        return iterator(&sDummy);  // out of bound || overflow
    }

    const size_type pos = base - mData;
    if (new_len > mCapacity) {
        reserve(new_len);
        if (new_len > mCapacity) {
            return iterator(&sDummy);  // not enough memory?
        }
    }
    // At this point 'iter' and 'base' are not valid anymore since
    // realloc could have taken place.
    base = mData + pos;
    std::memmove(base + 1, base, mLength - pos);
    *base = c;
    mLength = new_len;
    mData[mLength] = 0;
    return iterator(base);
}

string& string::operator=(char c)
{
    clear();
    Constructor(1, c);
    return *this;
}

string::size_type string::find(const value_type *str, size_type pos) const
{

    if (NULL == str)
    {
        return string::npos;
    }

    // Empty string is found everywhere except beyond the end. It is
    // possible to find the empty string right after the last char,
    // hence we used mLength and not mLength - 1 in the comparison.
    if (*str == '\0')
    {
        return pos > mLength ? string::npos : pos;
    }

    if (mLength == 0 || pos > mLength - 1)
    {
        return string::npos;
    }

    value_type *idx = std::strstr(mData + pos, str);

    if (NULL == idx)
    {
        return string::npos;
    }

    const std::ptrdiff_t delta = idx - mData;

    return static_cast<size_type>(delta);
}

string string::substr(size_type pos, size_type n) const {
    return string(*this, pos, n);
}

string::size_type string::find_first_of(value_type c, size_type pos) const {
    if (pos >= mLength) {
        return npos;
    }
    const char *res;
    // The last parameter represents a number of chars, not a index.
    res = static_cast<const char *>(std::memchr(mData + pos, c, mLength - pos));
    return res != NULL ? res - mData : npos;
}

string::size_type string::find_last_of(value_type c, size_type pos) const {
    if (mLength == 0) {
        return npos;
    } else if (pos >= mLength) {
        pos = mLength - 1;  // >= 0
    }

    const char *res;
    // Note:memrchr is not in the std namepace.
    // The last parameter represents a number of chars, not a index.
    res = static_cast<const char *>(memrchr(mData, c, pos + 1));
    return res != NULL ? res - mData : npos;
}

string::size_type string::find_first_not_of(value_type c, size_type pos) const {
    char *curr = mData + pos;
    for (size_type i = pos; i < mLength; ++i, ++curr) {
        if (c != *curr) {
            return i;
        }
    }
    return npos;
}

string::size_type string::find_last_not_of(value_type c, size_type pos) const {
    if (mLength == 0) {
        return npos;
    } else if (pos >= mLength) {
        pos = mLength - 1;  // >= 0
    }

    char *curr = mData + pos;
    size_type i = pos;

    for (;; --i, --curr) {
        if (c != *curr) {
            return i;
        } else if (i == 0) {
            return npos;
        }
    }
}

bool operator<(const string& lhs, const string& rhs) {
    return lhs.compare(rhs) < 0;
}

bool operator<=(const string& lhs, const string& rhs) {
    return lhs.compare(rhs) <= 0;
}

bool operator>(const string& lhs, const string& rhs) {
    return lhs.compare(rhs) > 0;
}

bool operator>=(const string& lhs, const string& rhs) {
    return lhs.compare(rhs) >= 0;
}

void swap(string& lhs, string& rhs) {
    lhs.swap(rhs);
}

ostream& operator<<(ostream& os, const string& str) {
    return os.write_formatted(str.data(), str.size());
}

}  // namespace std
