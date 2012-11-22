#include "BCCRuntimeStub.h"

#include "runtime/lib/int_lib.h"

#include "runtime/lib/absvdi2.c"
#include "runtime/lib/absvsi2.c"
#include "runtime/lib/addvdi3.c"
#include "runtime/lib/addvsi3.c"
#if !defined(__i386__) && !defined(__SSE2__)
#   include "runtime/lib/ashldi3.c"
#endif
#ifndef ANDROID
#   include "runtime/lib/ashrdi3.c"
#endif
#include "runtime/lib/clzdi2.c"
#include "runtime/lib/clzsi2.c"
#include "runtime/lib/cmpdi2.c"
#include "runtime/lib/ctzdi2.c"
#include "runtime/lib/ctzsi2.c"
#ifndef ANDROID // no complex.h
#   include "runtime/lib/divdc3.c"
#endif
#if !defined(__i386__)
#   include "runtime/lib/divdi3.c"
#endif
#include "runtime/lib/divsi3.c"
#ifndef ANDROID // no complex.h
#   include "runtime/lib/divsc3.c"
#endif
#include "runtime/lib/ffsdi2.c"
#include "runtime/lib/fixdfdi.c"
#include "runtime/lib/fixsfdi.c"
#include "runtime/lib/fixunsdfdi.c"
#include "runtime/lib/fixunsdfsi.c"
#include "runtime/lib/fixunssfdi.c"
#include "runtime/lib/fixunssfsi.c"
#if !defined(__i386__)
#   include "runtime/lib/floatdidf.c"
#   include "runtime/lib/floatdisf.c"
#   include "runtime/lib/floatundidf.c"
#   include "runtime/lib/floatundisf.c"
#   include "runtime/lib/moddi3.c"
#endif
#include "runtime/lib/modsi3.c"
#if !defined(__i386__) && !defined(__SSE2__)
#   include "runtime/lib/lshrdi3.c"
#endif
#ifndef ANDROID // no complex.h
#   include "runtime/lib/muldc3.c"
#endif
#if !defined(__i386__)
#   include "runtime/lib/muldi3.c"
#endif
#ifndef ANDROID // no complex.h
#   include "runtime/lib/mulsc3.c"
#endif
#include "runtime/lib/mulvdi3.c"
#include "runtime/lib/mulvsi3.c"
#include "runtime/lib/negdi2.c"
#include "runtime/lib/negvdi2.c"
#include "runtime/lib/negvsi2.c"
#include "runtime/lib/paritydi2.c"
#include "runtime/lib/paritysi2.c"
#include "runtime/lib/popcountdi2.c"
#include "runtime/lib/popcountsi2.c"
#include "runtime/lib/powidf2.c"
#include "runtime/lib/powisf2.c"
#include "runtime/lib/subvdi3.c"
#include "runtime/lib/subvsi3.c"
#include "runtime/lib/ucmpdi2.c"
#if !defined(__i386__)
#   include "runtime/lib/udivdi3.c"
#endif
#include "runtime/lib/udivsi3.c"
#include "runtime/lib/udivmoddi4.c"
#if !defined(__i386__)
#   include "runtime/lib/umoddi3.c"
#endif
#include "runtime/lib/umodsi3.c"
#include "runtime/lib/eprintf.c"

#if defined(__arm__)
// NOTE: __aeabi_f2uiz is missing from libgcc which comes with android,
// so here's the workaround.  Remove this when __aeabi_f2uiz is available
// from libgcc.
unsigned int __aeabi_f2uiz(float a)
  __attribute__((weak, alias("__fixunssfsi")));
#endif
