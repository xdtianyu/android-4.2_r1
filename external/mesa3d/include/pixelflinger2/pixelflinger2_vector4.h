/** 
 **
 ** Copyright 2010, The Android Open Source Project
 **
 ** Licensed under the Apache License, Version 2.0 (the "License"); 
 ** you may not use this file except in compliance with the License. 
 ** You may obtain a copy of the License at 
 **
 **     http://www.apache.org/licenses/LICENSE-2.0 
 **
 ** Unless required by applicable law or agreed to in writing, software 
 ** distributed under the License is distributed on an "AS IS" BASIS, 
 ** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 ** See the License for the specific language governing permissions and 
 ** limitations under the License.
 */

#ifndef _PIXELFLINGER2_VECTOR4_H_
#define _PIXELFLINGER2_VECTOR4_H_

#ifdef __cplusplus

template <typename Type> struct Vec4 {
    union {
        struct { Type x, y, z, w; };
        struct { Type r, g, b, a; };
        struct { Type S, T, R, Q; };
#if !USE_FIXED_POINT        
        float f[4];
		unsigned u[4];
        int i[4];
#endif
#if defined(__ARM_HAVE_NEON) && USE_NEON
        float32x4_t f4;
#endif
    };
    
    //Vec4() : x(0), y(0), z(0), w(0) {}
    Vec4() {}
    Vec4(Type X, Type Y, Type Z, Type W) : x(X), y(Y), z(Z), w(W) {}
    Vec4(Type X) : x(X), y(X), z(X), w(X) {}
    
#define VECTOR4_OP_UNARY(op,rhs) { \
x op rhs.x; \
y op rhs.y; \
z op rhs.z; \
w op rhs.w; }
    
#define VECTOR4_OP_UNARY_SCALAR(op,rhs) { \
x op rhs; \
y op rhs; \
z op rhs; \
w op rhs; }
    
    inline void operator += (const Vec4<Type> & rhs) __attribute__((always_inline))
    VECTOR4_OP_UNARY(+=,rhs)
    inline void operator -= (const Vec4<Type> & rhs) __attribute__((always_inline))
    VECTOR4_OP_UNARY(-=,rhs)
    inline void operator *= (const Vec4<Type> & rhs) __attribute__((always_inline))
    VECTOR4_OP_UNARY(*=,rhs)
    inline void operator /= (const Vec4<Type> & rhs) __attribute__((always_inline))
    VECTOR4_OP_UNARY(/=,rhs)
    inline void operator *= (Type rhs) __attribute__((always_inline))
    VECTOR4_OP_UNARY_SCALAR(*=,rhs)
    inline void operator /= (Type rhs) __attribute__((always_inline))
    VECTOR4_OP_UNARY_SCALAR(/=,rhs)
    
    inline Vec4 operator+(const Vec4 & rhs) const
    { Vec4 res = *this; res += rhs; return res; }
    
#undef VECTOR4_OP_UNARY
#undef VECTOR4_OP_UNARY_SCALAR
    
    void CrossProduct3(const Vec4<Type> & lhs, const Vec4<Type> & rhs)
    {
        x = lhs.y * rhs.z - lhs.z * rhs.y;
        y = lhs.z * rhs.x - lhs.x * rhs.z;
        z = lhs.y * rhs.x - lhs.x * rhs.y;
        w = 0;
    }
    
    void LShr(const unsigned shift) { u[0] >>= shift; u[1] >>= shift; u[2] >>= shift; u[3] >>= shift; }
    void AShr(const unsigned shift) { i[0] >>= shift; i[1] >>= shift; i[2] >>= shift; i[3] >>= shift; }
    
    bool operator==(const Vec4 & rhs) const { return u[0] == rhs.u[0] && u[1] == rhs.u[1] && u[2] == rhs.u[2] && u[3] == rhs.u[3]; }
    bool operator!=(const Vec4 & rhs) const { return !(*this == rhs); }
};

#if defined(__ARM_HAVE_NEON) && USE_NEON
template <> inline void Vec4<float>::operator += (const Vec4<float> & rhs) __attribute__((always_inline));
template <> inline void Vec4<float>::operator += (const Vec4<float> & rhs)
{ f4 = vaddq_f32(f4, rhs.f4); }
template <> inline void Vec4<float>::operator -= (const Vec4<float> & rhs)  __attribute__((always_inline));
template <> inline void Vec4<float>::operator -= (const Vec4<float> & rhs)
{ f4 = vsubq_f32(f4, rhs.f4); }
template <> inline void Vec4<float>::operator *= (float rhs) __attribute__((always_inline));
template <> inline void Vec4<float>::operator *= (float rhs)
{ f4 = vmulq_n_f32(f4, rhs); }
template <> inline void Vec4<float>::operator /= (float rhs) __attribute__((always_inline));
template <> inline void Vec4<float>::operator /= (float rhs)
{ f4 = vmulq_n_f32(f4, 1 / rhs); }
#endif // #if defined(__ARM_HAVE_NEON) && USE_NEON

#if USE_FIXED_POINT
deprecated, should be removed
/*#define FIXED_POINT_ONE 0x10000
#define FIXED_POINT_SHIFT 16
struct FixedPoint
{
    int val;
    //FixedPoint() {}
    //explicit FixedPoint(int v) : val(v << FIXED_POINT_SHIFT) {}
    //explicit FixedPoint(float v) : val(v * (2 << FIXED_POINT_SHIFT)) {}
    //explicit FixedPoint(double v) : val(v * (2 << FIXED_POINT_SHIFT)) {}
    static FixedPoint From(int v) { FixedPoint x; x.val = v << FIXED_POINT_SHIFT; return x; }
    static FixedPoint From(unsigned v) { FixedPoint x; x.val = v << FIXED_POINT_SHIFT; return x; }
    static FixedPoint From(float v) { FixedPoint x; x.val = v * (2 << FIXED_POINT_SHIFT); return x; }
    static FixedPoint One() { FixedPoint x; x.val = FIXED_POINT_ONE; return x; }
    static FixedPoint Zero() { FixedPoint x; x.val = 0; return x; }
    FixedPoint operator-() const
    {
        FixedPoint res;
        res.val = -val;
        return res;
    }
    FixedPoint operator+(const FixedPoint & rhs) const
    {
        FixedPoint res;
        res.val = val + rhs.val;
        return res;
    }
    FixedPoint operator-(const FixedPoint & rhs) const
    {
        FixedPoint res;
        res.val = val - rhs.val;
        return res;
    }
    FixedPoint operator*(const FixedPoint & rhs) const
    {
        FixedPoint res;
        res.val = (val >> 8) * (rhs.val >> 8);
        return res;
    }
    FixedPoint operator/(const FixedPoint & rhs) const
    {
        FixedPoint res;
        
        long long lh = (long long)val << 32, rh = rhs.val | 1;
        lh /= rh;
        rh = (lh >> 16) & 0xffffffffL;
        res.val = rh;
        return res;
        
        //res.val = ((val << 2) / (rhs.val >> 6 | 1)) << 8;
        //return res;
    }
    void operator+=(const FixedPoint & rhs) { val += rhs.val; }
    void operator-=(const FixedPoint & rhs) { val += rhs.val; }
    void operator*=(const FixedPoint & rhs) { *this = *this * rhs; }
    void operator/=(const FixedPoint & rhs) { *this = *this / rhs; }
    
    bool operator<(const FixedPoint & rhs) const { return val < rhs.val; }
    bool operator>(const FixedPoint & rhs) const { return val > rhs.val; }
    bool operator<=(const FixedPoint & rhs) const { return val <= rhs.val; }
    bool operator>=(const FixedPoint & rhs) const { return val >= rhs.val; }
    bool operator==(const FixedPoint & rhs) const { return val == rhs.val; }
    bool operator!=(const FixedPoint & rhs) const { return val != rhs.val; }
    
    operator int() const { return val >> FIXED_POINT_SHIFT; }
    operator unsigned() const { return val >> FIXED_POINT_SHIFT; }
    operator float() const { return (float)val / FIXED_POINT_ONE; }
};

typedef FixedPoint VectorComp_t;
typedef Vec4<VectorComp_t> Vector4;
#define Vector4_CTR(x,y,z,w) Vector4(FixedPoint::From(x), FixedPoint::From(y), \
                                        FixedPoint::From(z), FixedPoint::From(w))
#define VectorComp_t_CTR(x) FixedPoint::From(x)
#define VectorComp_t_Zero FixedPoint::Zero()
#define VectorComp_t_One FixedPoint::One()*/

#else // if USE_FIXED_POINT

typedef float VectorComp_t;
typedef struct Vec4<VectorComp_t> Vector4;
#define Vector4_CTR(x,y,z,w) Vector4(x,y,z,w)
#define VectorComp_t_CTR(x) (float)(x)
#define VectorComp_t_Zero 0
#define VectorComp_t_One 1

#endif // if USE_FIXED_POINT

#else // #ifdef __cplusplus

//typedef float Vector4 [4];
typedef struct { float x, y, z, w; } Vector4;

#define VECTOR4_OP_UNARY(v,op,s) \
   v.x op s.x; \
   v.y op s.y; \
   v.z op s.z; \
   v.w op s.w;
   
#define VECTOR4_OP_UNARY_SCALAR(v,op,s) \
   v.x op s; \
   v.y op s; \
   v.z op s; \
   v.w op s;

#define VECTOR4_CTR(x,y,z,w) \
   ((Vector4){x,y,z,w})
      
#endif // #ifdef __cplusplus

#endif // #ifndef _PIXELFLINGER2_VECTOR4_H_