==============================================
libbcc Compiler Runtime Function Documentation
==============================================

Integer Arithmetic Routines
---------------------------

*Arithmetic Functions*::

  __ashldi3 : long (long a, int b)  // a << b [[llvm]]
  __ashrdi3 : long (long a, int b)  // a / 2**b (i.e. arithmetic a >> b) [[llvm]]
  __divdi3 : long (long a, long b)  // a / b [[llvm]]
  __lshrdi3 : long (long a, int b)  // a >> b (without sign extension) [[llvm]]
  __moddi3 : long (long a, long b)  // a % b [[llvm]]
  __muldi3 : long (long a, long b)  // a * b [[llvm]]
  __muldsi3 : long (int a, int b)   // (long)a * (long)b  [[llvm compiler-rt extension]] [[llvm]]
  __negdi2 : long (long a)          // -a [[llvm]]
  __udivsi3 : unsigned int (unsigned int a, unsigned int b)     // unsigned int a / b [[llvm]]
  __udivdi3 : unsigned long (unsigned long a, unsigned long a)  // unsigned long a / b [[llvm]]
  __udivmoddi4 : unsigned long (unsigned long a, unsigned long b, unsigned long *rem)  // unsigned long a/b and a%b [[llvm]]
  __umoddi3 : unsigned long (unsigned long a, unsigned long b)  // unsigned long a % b [[llvm]]

*Comparison functions*::

  __cmpdi2 : int (long a, long b)                     // Signed comparison of a and b.  (if a < b then return 0.  if a == b then return 1.  if a > b then return 2.) [[llvm]]
  __ucmpdi2 : int (unsigned long a, unsigned long a)  // Unsigned comparison of a and b.  (if a < b then return 0.  if a == b then return 1.  if a > b then return 2.) [[llvm]]

*Trapping arithmetic functions*::

  __absvsi2 : int (int a)            // |a| [[llvm]]
  __absvdi2 : long (long a)          // |a| [[llvm]]
  __addvsi3 : int (int a, int b)     // a + b [[llvm]]
  __addvdi3 : long (long a, long b)  // a + b [[llvm]]
  __mulvsi3 : int (int a, int b)     // a * b [[llvm]]
  __mulvdi3 : long (long a, long b)  // a * b [[llvm]]
  __negvsi2 : int (int a)            // -a [[llvm]]
  __negvdi2 : long (long a)          // -a [[llvm]]
  __subvsi3 : int (int a, int b)     // a - b [[llvm]]
  __subvdi3 : long (long a, long b)  // a - b [[llvm]]

*Bit operations*::

  __clzsi2 : int (int a)      // number of leading 0-bits in a from MSB [[llvm]]
  __clzdi2 : int (long a)     // number of leading 0-bits in a from MSB [[llvm]]
  __ctzsi2 : int (int a)      // number of trailing 0-bits in a from LSB [[llvm]]
  __ctzdi2 : int (long a)     // number of trailing 0-bits in a from LSB [[llvm]]
  __ffsdi2 : int (long a)     // index of the least significant 1-bit [[llvm]]
  __paritysi2 : int (int a)   // parity (if number of 1-bits is even, then return 0) [[llvm]]
  __paritydi2 : int (long a)  // parity (if number of 1-bits is even, then return 0) [[llvm]]
  __popcountsi2 : int (int a)       // number of 1-bits [[llvm]]
  __popcountdi2 : int (long a)      // number of 1-bits [[llvm]]
  __bswapsi2 : uint32_t (uint32_t)  // reverse the byte order [[generic]]
  __bswapdi2 : uint64_t (uint64_t)  // reverse the byte order [[generic]]

Floating Point Emulation Routines
---------------------------------

*Arithmetic functions*::

  __addsf3 : float (float a, float b)      // a + b [[vfp]] [[generic]]
  __adddf3 : double (double a, double b)   // a + b [[vfp]] [[generic]]
  __subsf3 : float (float a, float b)      // a - b [[vfp]] [[generic]]
  __subdf3 : double (double a, double b)   // a - b [[vfp]] [[generic]]
  __mulsf3 : float (float a, float b)      // a * b [[vfp]] [[generic]]
  __muldf3 : double (double a, double b)   // a * b [[vfp]] [[generic]]
  __divsf3 : float (float a, float b)      // a / b [[vfp]] [[generic]]
  __divdf3 : double (double a, double b)   // a / b [[vfp]] [[generic]]
  __negsf2 : float (float a)               // -a [[vfp]] [[generic]]
  __negdf2 : double (double a)             // -a [[vfp]] [[generic]]

*Conversion functions*::

  __extendsfdf2 : double (float a)         // (double)a [[vfp]] [[generic]]
  __truncdfsf2 : float (double a)          // (float)a [[vfp]] [[generic]]
  __fixsfsi : int (float a)                // (int)a, rounding toward 0 [[vfp]] [[generic]]
  __fixdfsi : int (double a)               // (int)a, rounding toward 0 [[vfp]] [[generic]]
  __fixsfdi : long (float a)               // (long)a, rounding toward 0 [[llvm]]
  __fixdfdi : long (double a)              // (long)a, rounding toward 0 [[llvm]]
  __fixunssfsi : unsigned int (float a)    // (unsigned int)a, rounding toward 0, negative number will be 0. [[vfp]] [[llvm]]
  __fixunsdfsi : unsigned int (double a)   // (unsigned int)a, rounding toward 0, negative number will be 0. [[vfp]] [[llvm]]
  __fixunssfdi : unsigned long (float a)   // (unsigned long)a, rounding toward 0, negative number will be 0. [[llvm]]
  __fixunsdfdi : unsigned long (double a)  // (unsigned long)a, rounding toward 0, negative number will be 0. [[llvm]]
  __floatsisf : float (int i)              // (float)i [[vfp]] [[generic]]
  __floatsidf : double (int i)             // (double)i [[vfp]] [[generic]]
  __floatdisf : float (long i)             // (float) i [[llvm]]
  __floatdidf : double (long i)            // (double)i [[llvm]]
  __floatunsisf : float (unsigned int i)   // (float)i [[generic]]
  __floatunsidf : double (unsigned int i)  // (double)i [[generic]]
  __floatunssisfvfp : float (unsigned int i)   // (float)i  [[llvm compiler-rt extension]] [[vfp]]
  __floatunssidfvfp : double (unsigned int i)  // (double)i  [[llvm compiler-rt extension]] [[vfp]]
  __floatundisf : float (unsigned long i)  // (float)i [[llvm]]
  __floatundidf : double (unsigned long i) // (double)i [[llvm]]
  __unordsf2 : int (float a, float b)      // if a == NaN or b == NaN, then return non-zero. [[vfp]] [[generic]]
  __unorddf2 : int (double a, double b)    // if a == NaN or b == NaN, then return non-zero. [[vfp]] [[generic]]
  __eqsf2 : int (float a, float b)         // a != b  (i.e. return 0 when a == b) (note: NaN != NaN) [[vfp]] [[generic]]
  __eqdf2 : int (double a, double b)       // a != b (i.e. return 0 when a == b) (note: NaN != NaN) [[vfp]] [[generic]]
  __nesf2 : int (float a, float b)         // a != b || a == NaN || b == NaN [[vfp]]
  __nedf2 : int (double a, double b)       // a != b || a == NaN || b == NaN [[vfp]]
  __gesf2 : int (float a, float b)         // (a >= b) ? nonnegative_value : negative_value [[vfp]] [[generic]]
  __gedf2 : int (double a, double b)       // (a >= b) ? nonnegative_value : negative_value [[vfp]] [[generic]]
  __ltsf2 : int (float a, float b)         // (a < b) ? negative_value : nonnegative_value [[vfp]]
  __ltdf2 : int (double a, double b)       // (a < b) ? negative_value : nonnegative_value [[vfp]]
  __lesf2 : int (float a, float b)         // (a <= b) ? nonpositive_value : positive_value [[vfp]] [[generic]]
  __ledf2 : int (double a, double b)       // (a <= b) ? nonpositive_value : positive_value [[vfp]] [[generic]]
  __gtsf2 : int (float a, float b)         // (a > b) ? positive_value : nonpositive_value [[vfp]]
  __gtdf2 : int (double a, double b)       // (a > b) ? positive_value : nonpositive_value [[vfp]]

*Other floating-point functions*::

  __powisf2 : float (float a, int b)       // a**b [[llvm]]
  __powidf2 : double (double a, int b)     // a**b [[llvm]]
  __mulsc3 : complex_float (float a, float b, float c, float d)       // (a+bi) * (c+di) [[llvm]]
  __muldc3 : complex_double (double a, double b, double c, double d)  // (a+bi) * (c+di) [[llvm]]
  __divsc3 : complex_float (float a, float b, float c, float d)       // (a+bi) / (c+di) [[llvm]]
  __divdc3 : complex_double (double a, double b, double c, double d)  // (a+bi) / (c+di) [[llvm]]



Miscellaneous Routines
----------------------

::

  __eprintf : void (char const *, char const *, char const *, char const *)  // fprintf for assertions [[llvm compiler-rt extension]] [[llvm]]
