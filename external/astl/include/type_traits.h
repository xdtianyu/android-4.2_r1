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

#ifndef ANDROID_ASTL_TYPE_TRAITS_H__
#define ANDROID_ASTL_TYPE_TRAITS_H__

// GNU C++ compiler?
#ifndef __GNUG__
#error "__GNUG__ is not defined"
#endif

#ifdef _T
#error "_T is defined"
#endif

// In this files is a set of templates used to instrospect some
// template arguments properties.
//
// For instance to provide a specialized implementation of a template
// function foo<_T> when its template argument is a pointer type:
//
// template<typename _T> void foo(_T val) {  // template function
//   const bool is_pointer = is_pointer<_T>::value;
//   __foo<is_pointer>::foo(val);              // concrete impl
// }
//
// template<bool> struct __foo {
//   template<typename _T> static void foo(_T val) {
//     .... default implementation ...
//   }
// }
//
// Specialization of the above when the bool parameter is true (i.e is
// a pointer)
//
// template<> struct __foo<true> {
//   template<typename _T> static void foo(_T val) {
//     .... pointer specific implementation ...
//   }
// }
//

namespace std {

template<typename _T, _T _value>
struct integral_constant
{
    static const _T                       value = _value;
    typedef _T                            value_type;
    typedef integral_constant<_T, _value> type;
};

// typedef for true and false types
typedef integral_constant<bool, true>  true_type;
typedef integral_constant<bool, false> false_type;


// is_integral
template<typename> struct is_integral : public false_type { };

#define DEFINE_IS_INTEGRAL_TO_TRUE_TYPE_FOR_TYPE(_Type)                 \
    template<> struct is_integral<_Type>: public true_type { }; \
    template<> struct is_integral<_Type const>: public true_type { }; \
    template<> struct is_integral<_Type volatile>: public true_type { }; \
    template<> struct is_integral<_Type const volatile>: public true_type { };

DEFINE_IS_INTEGRAL_TO_TRUE_TYPE_FOR_TYPE(bool)
DEFINE_IS_INTEGRAL_TO_TRUE_TYPE_FOR_TYPE(char)
DEFINE_IS_INTEGRAL_TO_TRUE_TYPE_FOR_TYPE(signed char)
DEFINE_IS_INTEGRAL_TO_TRUE_TYPE_FOR_TYPE(unsigned char)
DEFINE_IS_INTEGRAL_TO_TRUE_TYPE_FOR_TYPE(wchar_t)
DEFINE_IS_INTEGRAL_TO_TRUE_TYPE_FOR_TYPE(short)
DEFINE_IS_INTEGRAL_TO_TRUE_TYPE_FOR_TYPE(unsigned short)
DEFINE_IS_INTEGRAL_TO_TRUE_TYPE_FOR_TYPE(int)
DEFINE_IS_INTEGRAL_TO_TRUE_TYPE_FOR_TYPE(unsigned int)
DEFINE_IS_INTEGRAL_TO_TRUE_TYPE_FOR_TYPE(long)
DEFINE_IS_INTEGRAL_TO_TRUE_TYPE_FOR_TYPE(unsigned long)
DEFINE_IS_INTEGRAL_TO_TRUE_TYPE_FOR_TYPE(long long)
DEFINE_IS_INTEGRAL_TO_TRUE_TYPE_FOR_TYPE(unsigned long long)
#undef DEFINE_IS_INTEGRAL_TO_TRUE_TYPE_FOR_TYPE

// is_floating_point
template<typename> struct is_floating_point : public false_type { };
#define DEFINE_IS_FLOATING_POINT_TO_TRUE_TYPE_FOR_TYPE(_Type)           \
    template<> struct is_floating_point<_Type>: public true_type { }; \
    template<> struct is_floating_point<_Type const>: public true_type { }; \
    template<> struct is_floating_point<_Type volatile>: public true_type { }; \
    template<> struct is_floating_point<_Type const volatile>: public true_type { };

DEFINE_IS_FLOATING_POINT_TO_TRUE_TYPE_FOR_TYPE(float)
DEFINE_IS_FLOATING_POINT_TO_TRUE_TYPE_FOR_TYPE(double)
DEFINE_IS_FLOATING_POINT_TO_TRUE_TYPE_FOR_TYPE(long double)
#undef DEFINE_IS_FLOATING_POINT_TO_TRUE_TYPE_FOR_TYPE

// is_pointer
template<typename> struct is_pointer : public false_type { };

template<typename _T>
struct is_pointer<_T*>: public true_type { };

template<typename _T>
struct is_pointer<_T* const>: public true_type { };

template<typename _T>
struct is_pointer<_T* volatile>: public true_type { };

template<typename _T>
struct is_pointer<_T* const volatile>: public true_type { };


// is_arithmetic
template<typename _T>
struct is_arithmetic : public integral_constant<bool, (is_integral<_T>::value || is_floating_point<_T>::value)> { };

// is_scalar
// TODO: Add is_enum and is_member_pointer when gcc > 4.1.3
template<typename _T>
struct is_scalar
        : public integral_constant<bool, (is_arithmetic<_T>::value || is_pointer<_T>::value)> { };

// Substitution Failure Is Not An Error used in is_pod.
struct sfinae_types
{
    typedef char one;
    typedef struct { char arr[2]; } two;
};

// Only classes will match the first declaration (pointer to member).
// TODO: newer version of gcc have these is_class built in.
template<typename _T>  sfinae_types::one test_pod_type(int _T::*);
template<typename _T>  sfinae_types::two& test_pod_type(...);

template<typename _T>
struct is_pod: public integral_constant<bool, sizeof(test_pod_type<_T>(0)) != sizeof(sfinae_types::one)> { };

template<typename _T>
struct is_class: public integral_constant<bool, sizeof(test_pod_type<_T>(0)) == sizeof(sfinae_types::one)> { };

}  // namespace std

#endif  // ANDROID_ASTL_TYPE_TRAITS_H__
