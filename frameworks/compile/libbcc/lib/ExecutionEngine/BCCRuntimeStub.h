#ifndef BCC_RUNTIME_STUB_H
#define BCC_RUNTIME_STUB_H

#include "runtime/lib/int_lib.h"

#if defined(__cplusplus)
extern "C" {
#endif

extern di_int __absvdi2(di_int);
extern si_int __absvsi2(si_int);
extern di_int __addvdi3(di_int, di_int);
extern si_int __addvsi3(si_int, si_int);
#if !defined(__i386__) && !defined(__SSE2__)
extern di_int __ashldi3(di_int, si_int);
#endif
#ifndef ANDROID
extern di_int __ashrdi3(di_int, si_int);
#endif
extern si_int __clzdi2(di_int);
extern si_int __clzsi2(si_int);
extern si_int __cmpdi2(di_int, di_int);
extern si_int __ctzdi2(di_int);
extern si_int __ctzsi2(si_int);
#ifndef ANDROID /* no complex.h */
extern double _Complex __divdc3(double, double, double, double);
#endif
extern di_int __divdi3(di_int, di_int);
extern si_int __divsi3(si_int, si_int);
#ifndef ANDROID /* no complex.h */
extern float _Complex __divsc3(float, float, float, float);
#endif
extern si_int __ffsdi2(di_int);
extern di_int __fixdfdi(double);
extern di_int __fixsfdi(float);
extern du_int __fixunsdfdi(double);
extern su_int __fixunsdfsi(double);
extern du_int __fixunssfdi(float);
extern su_int __fixunssfsi(float);
#if !defined(__i386__)
extern double __floatdidf(di_int);
extern float __floatdisf(di_int);
extern double __floatundidf(du_int);
extern float __floatundisf(du_int);
#endif
extern di_int __moddi3(di_int, di_int);
extern si_int __modsi3(si_int, si_int);
#if !defined(__i386__) && !defined(__SSE2__)
extern di_int __lshrdi3(di_int, si_int);
#endif
#ifndef ANDROID /* no complex.h */
extern double _Complex __muldc3(double, double, double, double);
#endif
#if !defined(__i386__)
extern di_int __muldi3(di_int, di_int);
#endif
#ifndef ANDROID /* no complex.h */
extern float _Complex __mulsc3(float, float, float, float);
#endif
extern di_int __mulvdi3(di_int, di_int);
extern si_int __mulvsi3(si_int, si_int);
extern di_int __negdi2(di_int);
extern di_int __negvdi2(di_int);
extern si_int __negvsi2(si_int);
extern si_int __paritydi2(di_int);
extern si_int __paritysi2(si_int);
extern si_int __popcountdi2(di_int);
extern si_int __popcountsi2(si_int);
extern double __powidf2(double, si_int);
extern float __powisf2(float, si_int);
extern di_int __subvdi3(di_int, di_int);
extern si_int __subvsi3(si_int, si_int);
extern si_int __ucmpdi2(du_int, du_int);
extern du_int __udivdi3(du_int, du_int);
extern su_int __udivsi3(su_int, su_int);
extern du_int __udivmoddi4(du_int, du_int, du_int *);
extern du_int __umoddi3(du_int, du_int);
extern su_int __umodsi3(su_int, su_int);
extern void __eprintf(char const *, char const *, char const *, char const *)
  __attribute__((visibility("hidden")));

#if defined(__cplusplus)
}
#endif

#endif /* BCC_RUNTIME_STUB_H */
