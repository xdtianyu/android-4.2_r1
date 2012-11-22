#include "rs_core.rsh"
#include "rs_graphics.rsh"
#include "rs_structs.h"

/**
* Allocation sampling
*/
static const void * __attribute__((overloadable))
        getElementAt(rs_allocation a, uint32_t x, uint32_t lod) {
    Allocation_t *alloc = (Allocation_t *)a.p;
    const Type_t *type = (const Type_t*)alloc->mHal.state.type;
    const uint8_t *p = (const uint8_t *)alloc->mHal.drvState.mallocPtr;

    const uint32_t offset = type->mHal.state.lodOffset[lod];
    const uint32_t eSize = alloc->mHal.state.elementSizeBytes;

    return &p[offset + eSize * x];
}

static const void * __attribute__((overloadable))
        getElementAt(rs_allocation a, uint32_t x, uint32_t y, uint32_t lod) {
    Allocation_t *alloc = (Allocation_t *)a.p;
    const Type_t *type = (const Type_t*)alloc->mHal.state.type;
    const uint8_t *p = (const uint8_t *)alloc->mHal.drvState.mallocPtr;

    const uint32_t eSize = alloc->mHal.state.elementSizeBytes;
    const uint32_t offset = type->mHal.state.lodOffset[lod];
    uint32_t stride;
    if(lod == 0) {
        stride = alloc->mHal.drvState.stride;
    } else {
        stride = type->mHal.state.lodDimX[lod] * eSize;
    }

    return &p[offset + (eSize * x) + (y * stride)];
}

static const void * __attribute__((overloadable))
        getElementAt(rs_allocation a, uint2 uv, uint32_t lod) {
    return getElementAt(a, uv.x, uv.y, lod);
}

static uint32_t wrapI(rs_sampler_value wrap, int32_t coord, int32_t size) {
    if (wrap == RS_SAMPLER_WRAP) {
        coord = coord % size;
        if (coord < 0) {
            coord += size;
        }
    }
    return (uint32_t)max(0, min(coord, size - 1));
}

// 565 Conversion bits taken from SkBitmap
#define SK_R16_BITS     5
#define SK_G16_BITS     6
#define SK_B16_BITS     5

#define SK_R16_SHIFT    (SK_B16_BITS + SK_G16_BITS)
#define SK_G16_SHIFT    (SK_B16_BITS)
#define SK_B16_SHIFT    0

#define SK_R16_MASK     ((1 << SK_R16_BITS) - 1)
#define SK_G16_MASK     ((1 << SK_G16_BITS) - 1)
#define SK_B16_MASK     ((1 << SK_B16_BITS) - 1)

#define SkGetPackedR16(color)   (((unsigned)(color) >> SK_R16_SHIFT) & SK_R16_MASK)
#define SkGetPackedG16(color)   (((unsigned)(color) >> SK_G16_SHIFT) & SK_G16_MASK)
#define SkGetPackedB16(color)   (((unsigned)(color) >> SK_B16_SHIFT) & SK_B16_MASK)

static inline unsigned SkR16ToR32(unsigned r) {
    return (r << (8 - SK_R16_BITS)) | (r >> (2 * SK_R16_BITS - 8));
}

static inline unsigned SkG16ToG32(unsigned g) {
    return (g << (8 - SK_G16_BITS)) | (g >> (2 * SK_G16_BITS - 8));
}

static inline unsigned SkB16ToB32(unsigned b) {
    return (b << (8 - SK_B16_BITS)) | (b >> (2 * SK_B16_BITS - 8));
}

#define SkPacked16ToR32(c)      SkR16ToR32(SkGetPackedR16(c))
#define SkPacked16ToG32(c)      SkG16ToG32(SkGetPackedG16(c))
#define SkPacked16ToB32(c)      SkB16ToB32(SkGetPackedB16(c))

static float3 getFrom565(uint16_t color) {
    float3 result;
    result.x = (float)SkPacked16ToR32(color);
    result.y = (float)SkPacked16ToG32(color);
    result.z = (float)SkPacked16ToB32(color);
    return result;
}

#define SAMPLE_1D_FUNC(vecsize, intype, outtype, convert)                                       \
        static outtype __attribute__((overloadable))                                            \
                getSample##vecsize(rs_allocation a, float2 weights,                             \
                                   uint32_t iPixel, uint32_t next, uint32_t lod) {              \
            intype *p0c = (intype*)getElementAt(a, iPixel, lod);                                \
            intype *p1c = (intype*)getElementAt(a, next, lod);                                  \
            outtype p0 = convert(*p0c);                                                         \
            outtype p1 = convert(*p1c);                                                         \
            return p0 * weights.x + p1 * weights.y;                                             \
        }
#define SAMPLE_2D_FUNC(vecsize, intype, outtype, convert)                                       \
        static outtype __attribute__((overloadable))                                            \
                    getSample##vecsize(rs_allocation a, float4 weights,                         \
                                       uint2 iPixel, uint2 next, uint32_t lod) {                \
            intype *p0c = (intype*)getElementAt(a, iPixel.x, iPixel.y, lod);                    \
            intype *p1c = (intype*)getElementAt(a, next.x, iPixel.y, lod);                      \
            intype *p2c = (intype*)getElementAt(a, iPixel.x, next.y, lod);                      \
            intype *p3c = (intype*)getElementAt(a, next.x, next.y, lod);                        \
            outtype p0 = convert(*p0c);                                                         \
            outtype p1 = convert(*p1c);                                                         \
            outtype p2 = convert(*p2c);                                                         \
            outtype p3 = convert(*p3c);                                                         \
            return p0 * weights.x + p1 * weights.y + p2 * weights.z + p3 * weights.w;           \
        }

SAMPLE_1D_FUNC(1, uchar, float, (float))
SAMPLE_1D_FUNC(2, uchar2, float2, convert_float2)
SAMPLE_1D_FUNC(3, uchar3, float3, convert_float3)
SAMPLE_1D_FUNC(4, uchar4, float4, convert_float4)
SAMPLE_1D_FUNC(565, uint16_t, float3, getFrom565)

SAMPLE_2D_FUNC(1, uchar, float, (float))
SAMPLE_2D_FUNC(2, uchar2, float2, convert_float2)
SAMPLE_2D_FUNC(3, uchar3, float3, convert_float3)
SAMPLE_2D_FUNC(4, uchar4, float4, convert_float4)
SAMPLE_2D_FUNC(565, uint16_t, float3, getFrom565)

// Sampler function body is the same for all dimensions
#define SAMPLE_FUNC_BODY()                                                                      \
{                                                                                               \
    rs_element elem = rsAllocationGetElement(a);                                                \
    rs_data_kind dk = rsElementGetDataKind(elem);                                               \
    rs_data_type dt = rsElementGetDataType(elem);                                               \
                                                                                                \
    if (dk == RS_KIND_USER || (dt != RS_TYPE_UNSIGNED_8 && dt != RS_TYPE_UNSIGNED_5_6_5)) {     \
        float4 zero = {0.0f, 0.0f, 0.0f, 0.0f};                                                 \
        return zero;                                                                            \
    }                                                                                           \
                                                                                                \
    uint32_t vecSize = rsElementGetVectorSize(elem);                                            \
    Allocation_t *alloc = (Allocation_t *)a.p;                                                  \
    const Type_t *type = (const Type_t*)alloc->mHal.state.type;                                 \
                                                                                                \
    rs_sampler_value sampleMin = rsSamplerGetMinification(s);                                  \
    rs_sampler_value sampleMag = rsSamplerGetMagnification(s);                                 \
                                                                                                \
    if (lod <= 0.0f) {                                                                          \
        if (sampleMag == RS_SAMPLER_NEAREST) {                                                  \
            return sample_LOD_NearestPixel(a, type, vecSize, dt, s, uv, 0);                     \
        }                                                                                       \
        return sample_LOD_LinearPixel(a, type, vecSize, dt, s, uv, 0);                          \
    }                                                                                           \
                                                                                                \
    if (sampleMin == RS_SAMPLER_LINEAR_MIP_NEAREST) {                                           \
        uint32_t maxLOD = type->mHal.state.lodCount - 1;                                        \
        lod = min(lod, (float)maxLOD);                                                          \
        uint32_t nearestLOD = (uint32_t)round(lod);                                             \
        return sample_LOD_LinearPixel(a, type, vecSize, dt, s, uv, nearestLOD);                 \
    }                                                                                           \
                                                                                                \
    if (sampleMin == RS_SAMPLER_LINEAR_MIP_LINEAR) {                                            \
        uint32_t lod0 = (uint32_t)floor(lod);                                                   \
        uint32_t lod1 = (uint32_t)ceil(lod);                                                    \
        uint32_t maxLOD = type->mHal.state.lodCount - 1;                                        \
        lod0 = min(lod0, maxLOD);                                                               \
        lod1 = min(lod1, maxLOD);                                                               \
        float4 sample0 = sample_LOD_LinearPixel(a, type, vecSize, dt, s, uv, lod0);             \
        float4 sample1 = sample_LOD_LinearPixel(a, type, vecSize, dt, s, uv, lod1);             \
        float frac = lod - (float)lod0;                                                         \
        return sample0 * (1.0f - frac) + sample1 * frac;                                        \
    }                                                                                           \
                                                                                                \
    return sample_LOD_NearestPixel(a, type, vecSize, dt, s, uv, 0);                             \
} // End of sampler function body is the same for all dimensions

// Body of the bilinear sampling function
#define BILINEAR_SAMPLE_BODY()                                                                  \
{                                                                                               \
    float4 result;                                                                              \
    if (dt == RS_TYPE_UNSIGNED_5_6_5) {                                                         \
        result.xyz = getSample565(a, weights, iPixel, next, lod);                               \
        return result;                                                                          \
    }                                                                                           \
                                                                                                \
    switch(vecSize) {                                                                           \
    case 1:                                                                                     \
        result.x = getSample1(a, weights, iPixel, next, lod);                                   \
        break;                                                                                  \
    case 2:                                                                                     \
        result.xy = getSample2(a, weights, iPixel, next, lod);                                  \
        break;                                                                                  \
    case 3:                                                                                     \
        result.xyz = getSample3(a, weights, iPixel, next, lod);                                 \
        break;                                                                                  \
    case 4:                                                                                     \
        result = getSample4(a, weights, iPixel, next, lod);                                     \
        break;                                                                                  \
    }                                                                                           \
                                                                                                \
    return result * 0.003921569f;                                                                              \
} // End of body of the bilinear sampling function

// Body of the nearest sampling function
#define NEAREST_SAMPLE_BODY()                                                                   \
{                                                                                               \
    float4 result;                                                                              \
    if (dt == RS_TYPE_UNSIGNED_5_6_5) {                                                         \
        result.xyz = getFrom565(*(uint16_t*)getElementAt(a, iPixel, lod));                      \
       return result;                                                                           \
    }                                                                                           \
                                                                                                \
    switch(vecSize) {                                                                           \
    case 1:                                                                                     \
        result.x = (float)(*((uchar*)getElementAt(a, iPixel, lod)));                            \
        break;                                                                                  \
    case 2:                                                                                     \
        result.xy = convert_float2(*((uchar2*)getElementAt(a, iPixel, lod)));                   \
        break;                                                                                  \
    case 3:                                                                                     \
        result.xyz = convert_float3(*((uchar3*)getElementAt(a, iPixel, lod)));                  \
        break;                                                                                  \
    case 4:                                                                                     \
        result = convert_float4(*((uchar4*)getElementAt(a, iPixel, lod)));                      \
        break;                                                                                  \
    }                                                                                           \
                                                                                                \
    return result * 0.003921569f;                                                                              \
} // End of body of the nearest sampling function

static float4 __attribute__((overloadable))
        getBilinearSample(rs_allocation a, float2 weights,
                          uint32_t iPixel, uint32_t next,
                          uint32_t vecSize, rs_data_type dt, uint32_t lod) {
    BILINEAR_SAMPLE_BODY()
}

static float4 __attribute__((overloadable))
        getBilinearSample(rs_allocation a, float4 weights,
                          uint2 iPixel, uint2 next,
                          uint32_t vecSize, rs_data_type dt, uint32_t lod) {
    BILINEAR_SAMPLE_BODY()
}

static float4  __attribute__((overloadable))
        getNearestSample(rs_allocation a, uint32_t iPixel, uint32_t vecSize,
                         rs_data_type dt, uint32_t lod) {
    NEAREST_SAMPLE_BODY()
}

static float4  __attribute__((overloadable))
        getNearestSample(rs_allocation a, uint2 iPixel, uint32_t vecSize,
                         rs_data_type dt, uint32_t lod) {
    NEAREST_SAMPLE_BODY()
}

static float4 __attribute__((overloadable))
        sample_LOD_LinearPixel(rs_allocation a, const Type_t *type,
                               uint32_t vecSize, rs_data_type dt,
                               rs_sampler s,
                               float uv, uint32_t lod) {
    rs_sampler_value wrapS = rsSamplerGetWrapS(s);
    int32_t sourceW = type->mHal.state.lodDimX[lod];
    float pixelUV = uv * (float)(sourceW);
    int32_t iPixel = (int32_t)(pixelUV);
    float frac = pixelUV - (float)iPixel;

    if (frac < 0.5f) {
        iPixel -= 1;
        frac += 0.5f;
    } else {
        frac -= 0.5f;
    }

    float oneMinusFrac = 1.0f - frac;

    float2 weights;
    weights.x = oneMinusFrac;
    weights.y = frac;

    uint32_t next = wrapI(wrapS, iPixel + 1, sourceW);
    uint32_t location = wrapI(wrapS, iPixel, sourceW);

    return getBilinearSample(a, weights, location, next, vecSize, dt, lod);
}

static float4 __attribute__((overloadable))
        sample_LOD_NearestPixel(rs_allocation a, const Type_t *type,
                                uint32_t vecSize, rs_data_type dt,
                                rs_sampler s,
                                float uv, uint32_t lod) {
    rs_sampler_value wrapS = rsSamplerGetWrapS(s);
    int32_t sourceW = type->mHal.state.lodDimX[lod];
    int32_t iPixel = (int32_t)(uv * (float)(sourceW));
    uint32_t location = wrapI(wrapS, iPixel, sourceW);

    return getNearestSample(a, location, vecSize, dt, lod);
}

static float4 __attribute__((overloadable))
        sample_LOD_LinearPixel(rs_allocation a, const Type_t *type,
                               uint32_t vecSize, rs_data_type dt,
                               rs_sampler s,
                               float2 uv, uint32_t lod) {
    rs_sampler_value wrapS = rsSamplerGetWrapS(s);
    rs_sampler_value wrapT = rsSamplerGetWrapT(s);

    int32_t sourceW = type->mHal.state.lodDimX[lod];
    int32_t sourceH = type->mHal.state.lodDimY[lod];

    float2 dimF;
    dimF.x = (float)(sourceW);
    dimF.y = (float)(sourceH);
    float2 pixelUV = uv * dimF;
    int2 iPixel = convert_int2(pixelUV);

    float2 frac = pixelUV - convert_float2(iPixel);

    if (frac.x < 0.5f) {
        iPixel.x -= 1;
        frac.x += 0.5f;
    } else {
        frac.x -= 0.5f;
    }
    if (frac.y < 0.5f) {
        iPixel.y -= 1;
        frac.y += 0.5f;
    } else {
        frac.y -= 0.5f;
    }
    float2 oneMinusFrac = 1.0f - frac;

    float4 weights;
    weights.x = oneMinusFrac.x * oneMinusFrac.y;
    weights.y = frac.x * oneMinusFrac.y;
    weights.z = oneMinusFrac.x * frac.y;
    weights.w = frac.x * frac.y;

    uint2 next;
    next.x = wrapI(wrapS, iPixel.x + 1, sourceW);
    next.y = wrapI(wrapT, iPixel.y + 1, sourceH);
    uint2 location;
    location.x = wrapI(wrapS, iPixel.x, sourceW);
    location.y = wrapI(wrapT, iPixel.y, sourceH);

    return getBilinearSample(a, weights, location, next, vecSize, dt, lod);
}

static float4 __attribute__((overloadable))
        sample_LOD_NearestPixel(rs_allocation a, const Type_t *type,
                                uint32_t vecSize, rs_data_type dt,
                                rs_sampler s,
                                float2 uv, uint32_t lod) {
    rs_sampler_value wrapS = rsSamplerGetWrapS(s);
    rs_sampler_value wrapT = rsSamplerGetWrapT(s);

    int32_t sourceW = type->mHal.state.lodDimX[lod];
    int32_t sourceH = type->mHal.state.lodDimY[lod];

    float2 dimF;
    dimF.x = (float)(sourceW);
    dimF.y = (float)(sourceH);
    int2 iPixel = convert_int2(uv * dimF);

    uint2 location;
    location.x = wrapI(wrapS, iPixel.x, sourceW);
    location.y = wrapI(wrapT, iPixel.y, sourceH);
    return getNearestSample(a, location, vecSize, dt, lod);
}

extern const float4 __attribute__((overloadable))
        rsSample(rs_allocation a, rs_sampler s, float location) {
    return rsSample(a, s, location, 0);
}

extern const float4 __attribute__((overloadable))
        rsSample(rs_allocation a, rs_sampler s, float uv, float lod) {
    SAMPLE_FUNC_BODY()
}

extern const float4 __attribute__((overloadable))
        rsSample(rs_allocation a, rs_sampler s, float2 location) {
    return rsSample(a, s, location, 0.0f);
}

extern const float4 __attribute__((overloadable))
        rsSample(rs_allocation a, rs_sampler s, float2 uv, float lod) {
    SAMPLE_FUNC_BODY()
}
