/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <stdio.h>
#include <stdlib.h>
#include <math.h>

#if !defined(__GDK__) && !defined(__NOGDK__)
#include <bcc/bcc.h>
#include <dlfcn.h>
#endif // !__GDK__ && !__NOGDK__

#if !defined(__GDK__)
#include <jni.h>
#include <time.h>
#include <android/bitmap.h>
#endif // !__GDK__

#include <android/log.h>

#define  LOG_TAG    "libplasma"
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

/* Set to 1 to enable debug log traces. */
#define DEBUG 0

/* Set to 1 to optimize memory stores when generating plasma. */
#define OPTIMIZE_WRITES  1


/* We're going to perform computations for every pixel of the target
 * bitmap. floating-point operations are very slow on ARMv5, and not
 * too bad on ARMv7 with the exception of trigonometric functions.
 *
 * For better performance on all platforms, we're going to use fixed-point
 * arithmetic and all kinds of tricks
 */

typedef int32_t  Fixed;

#define  FIXED_BITS           16
#define  FIXED_ONE            (1 << FIXED_BITS)
#define  FIXED_AVERAGE(x,y)   (((x) + (y)) >> 1)

#define  FIXED_FROM_INT(x)    ((x) << FIXED_BITS)
#define  FIXED_TO_INT(x)      ((x) >> FIXED_BITS)

#define  FIXED_FROM_FLOAT(x)  ((Fixed)((x)*FIXED_ONE))
#define  FIXED_TO_FLOAT(x)    ((x)/(1.*FIXED_ONE))

#define  FIXED_MUL(x,y)       (((int64_t)(x) * (y)) >> FIXED_BITS)
#define  FIXED_DIV(x,y)       (((int64_t)(x) * FIXED_ONE) / (y))

#define  FIXED_DIV2(x)        ((x) >> 1)
#define  FIXED_AVERAGE(x,y)   (((x) + (y)) >> 1)

#define  FIXED_FRAC(x)        ((x) & ((1 << FIXED_BITS)-1))
#define  FIXED_TRUNC(x)       ((x) & ~((1 << FIXED_BITS)-1))

#define  FIXED_FROM_INT_FLOAT(x,f)   (Fixed)((x)*(FIXED_ONE*(f)))

typedef int32_t  Angle;

#define  ANGLE_BITS              9

#if ANGLE_BITS < 8
#  error ANGLE_BITS must be at least 8
#endif

#define  ANGLE_2PI               (1 << ANGLE_BITS)
#define  ANGLE_PI                (1 << (ANGLE_BITS-1))
#define  ANGLE_PI2               (1 << (ANGLE_BITS-2))
#define  ANGLE_PI4               (1 << (ANGLE_BITS-3))

#define  ANGLE_FROM_FLOAT(x)   (Angle)((x)*ANGLE_PI/M_PI)
#define  ANGLE_TO_FLOAT(x)     ((x)*M_PI/ANGLE_PI)

#if ANGLE_BITS <= FIXED_BITS
#  define  ANGLE_FROM_FIXED(x)     (Angle)((x) >> (FIXED_BITS - ANGLE_BITS))
#  define  ANGLE_TO_FIXED(x)       (Fixed)((x) << (FIXED_BITS - ANGLE_BITS))
#else
#  define  ANGLE_FROM_FIXED(x)     (Angle)((x) << (ANGLE_BITS - FIXED_BITS))
#  define  ANGLE_TO_FIXED(x)       (Fixed)((x) >> (ANGLE_BITS - FIXED_BITS))
#endif

#if defined(__GDK__)
static Fixed  *angle_sin_tab;
#else
static Fixed  angle_sin_tab[ANGLE_2PI+1];
#endif // !__GDK__

static void init_angles(void)
{
    int  nn;
    for (nn = 0; nn < ANGLE_2PI+1; nn++) {
        double  radians = nn*M_PI/ANGLE_PI;
        angle_sin_tab[nn] = FIXED_FROM_FLOAT(sin(radians));
    }
}

static __inline__ Fixed angle_sin( Angle  a )
{
    return angle_sin_tab[(uint32_t)a & (ANGLE_2PI-1)];
}

static __inline__ Fixed angle_cos( Angle  a )
{
    return angle_sin(a + ANGLE_PI2);
}

static __inline__ Fixed fixed_sin( Fixed  f )
{
    return angle_sin(ANGLE_FROM_FIXED(f));
}

static __inline__ Fixed  fixed_cos( Fixed  f )
{
    return angle_cos(ANGLE_FROM_FIXED(f));
}

/* Color palette used for rendering the plasma */
#define  PALETTE_BITS   8
#define  PALETTE_SIZE   (1 << PALETTE_BITS)

#if PALETTE_BITS > FIXED_BITS
#  error PALETTE_BITS must be smaller than FIXED_BITS
#endif

static uint16_t  palette[PALETTE_SIZE];

static uint16_t  make565(int red, int green, int blue)
{
    return (uint16_t)( ((red   << 8) & 0xf800) |
                       ((green << 2) & 0x03e0) |
                       ((blue  >> 3) & 0x001f) );
}

static void init_palette(void)
{
    int  nn, mm = 0;
    /* fun with colors */
    for (nn = 0; nn < PALETTE_SIZE/4; nn++) {
        int  jj = (nn-mm)*4*255/PALETTE_SIZE;
        palette[nn] = make565(255, jj, 255-jj);
    }

    for ( mm = nn; nn < PALETTE_SIZE/2; nn++ ) {
        int  jj = (nn-mm)*4*255/PALETTE_SIZE;
        palette[nn] = make565(255-jj, 255, jj);
    }

    for ( mm = nn; nn < PALETTE_SIZE*3/4; nn++ ) {
        int  jj = (nn-mm)*4*255/PALETTE_SIZE;
        palette[nn] = make565(0, 255-jj, 255);
    }

    for ( mm = nn; nn < PALETTE_SIZE; nn++ ) {
        int  jj = (nn-mm)*4*255/PALETTE_SIZE;
        palette[nn] = make565(jj, 0, 255);
    }
}

static __inline__ uint16_t  palette_from_fixed( uint16_t* palette, Fixed  x )
{
    if (x < 0) x = -x;
    if (x >= FIXED_ONE) x = FIXED_ONE-1;
    int  idx = FIXED_FRAC(x) >> (FIXED_BITS - PALETTE_BITS);
    return palette[idx & (PALETTE_SIZE-1)];
}

/* Angles expressed as fixed point radians */

static void init_tables(void)
{
    init_palette();
    init_angles();
}


extern "C" void fill_plasma( 
    uint32_t width, uint32_t height, uint32_t stride, double t, uint16_t* palette, void* pixels, Fixed *_angle_sin_tab )
{
  #if defined(__GDK__)
    angle_sin_tab = _angle_sin_tab;
  #endif // !__GDK__
    Fixed ft  = FIXED_FROM_FLOAT(t/1000.);
    Fixed yt1 = FIXED_FROM_FLOAT(t/1230.);
    Fixed yt2 = yt1;
    Fixed xt10 = FIXED_FROM_FLOAT(t/3000.);
    Fixed xt20 = xt10;

#define  YT1_INCR   FIXED_FROM_FLOAT(1/100.)
#define  YT2_INCR   FIXED_FROM_FLOAT(1/163.)

    int  yy;
    for (yy = 0; yy < height; yy++) {
        uint16_t*  line = (uint16_t*)pixels;
        Fixed      base = fixed_sin(yt1) + fixed_sin(yt2);
        Fixed      xt1 = xt10;
        Fixed      xt2 = xt20;

        yt1 += YT1_INCR;
        yt2 += YT2_INCR;

#define  XT1_INCR  FIXED_FROM_FLOAT(1/173.)
#define  XT2_INCR  FIXED_FROM_FLOAT(1/242.)

#if OPTIMIZE_WRITES
        /* optimize memory writes by generating one aligned 32-bit store
         * for every pair of pixels.
         */
        uint16_t*  line_end = line + width;

        if (line < line_end) {
            if (((uint32_t)line & 3) != 0) {
                Fixed ii = base + fixed_sin(xt1) + fixed_sin(xt2);

                xt1 += XT1_INCR;
                xt2 += XT2_INCR;

                line[0] = palette_from_fixed(palette, ii >> 2);
                line++;
            }

            while (line + 2 <= line_end) {
                Fixed i1 = base + fixed_sin(xt1) + fixed_sin(xt2);
                xt1 += XT1_INCR;
                xt2 += XT2_INCR;

                Fixed i2 = base + fixed_sin(xt1) + fixed_sin(xt2);
                xt1 += XT1_INCR;
                xt2 += XT2_INCR;

                uint32_t  pixel = ((uint32_t)palette_from_fixed(palette, i1 >> 2) << 16) |
                                   (uint32_t)palette_from_fixed(palette, i2 >> 2);

                ((uint32_t*)line)[0] = pixel;
                line += 2;
            }

            if (line < line_end) {
                Fixed ii = base + fixed_sin(xt1) + fixed_sin(xt2);
                line[0] = palette_from_fixed(palette, ii >> 2);
                line++;
            }
        }
#else /* !OPTIMIZE_WRITES */
        int xx;
        for (xx = 0; xx < width; xx++) {

            Fixed ii = base + fixed_sin(xt1) + fixed_sin(xt2);

            xt1 += XT1_INCR;
            xt2 += XT2_INCR;

            line[xx] = palette_from_fixed(palette, ii / 4);
        }
#endif /* !OPTIMIZE_WRITES */

        // go to next line
        pixels = (char*)pixels + stride;
    }
}

#if !defined(__GDK__)

/* Return current time in milliseconds */
static double now_ms(void)
{
    struct timeval tv;
    gettimeofday(&tv, NULL);
    return tv.tv_sec*1000. + tv.tv_usec/1000.;
}

/* simple stats management */
typedef struct {
    double  renderTime;
    double  frameTime;
} FrameStats;

#define  MAX_FRAME_STATS  200
#define  MAX_PERIOD_MS    1500

typedef struct {
    double  firstTime;
    double  lastTime;
    double  frameTime;

    int         firstFrame;
    int         numFrames;
    FrameStats  frames[ MAX_FRAME_STATS ];
} Stats;

static void
stats_init( Stats*  s )
{
    s->lastTime = now_ms();
    s->firstTime = 0.;
    s->firstFrame = 0;
    s->numFrames  = 0;
}

static void
stats_startFrame( Stats*  s )
{
    s->frameTime = now_ms();
}

static void
stats_endFrame( Stats*  s )
{
    double now = now_ms();
    double renderTime = now - s->frameTime;
    double frameTime  = now - s->lastTime;
    int nn;

    if (now - s->firstTime >= MAX_PERIOD_MS) {
        if (s->numFrames > 0) {
            double minRender, maxRender, avgRender;
            double minFrame, maxFrame, avgFrame;
            int count;

            nn = s->firstFrame;
            minRender = maxRender = avgRender = s->frames[nn].renderTime;
            minFrame  = maxFrame  = avgFrame  = s->frames[nn].frameTime;
            for (count = s->numFrames; count > 0; count-- ) {
                nn += 1;
                if (nn >= MAX_FRAME_STATS)
                    nn -= MAX_FRAME_STATS;
                double render = s->frames[nn].renderTime;
                if (render < minRender) minRender = render;
                if (render > maxRender) maxRender = render;
                double frame = s->frames[nn].frameTime;
                if (frame < minFrame) minFrame = frame;
                if (frame > maxFrame) maxFrame = frame;
                avgRender += render;
                avgFrame  += frame;
            }
            avgRender /= s->numFrames;
            avgFrame  /= s->numFrames;

            LOGI("frame/s (avg,min,max) = (%.1f,%.1f,%.1f) "
                 "render time ms (avg,min,max) = (%.1f,%.1f,%.1f)\n",
                 1000./avgFrame, 1000./maxFrame, 1000./minFrame,
                 avgRender, minRender, maxRender);
        }
        s->numFrames  = 0;
        s->firstFrame = 0;
        s->firstTime  = now;
    }

    nn = s->firstFrame + s->numFrames;
    if (nn >= MAX_FRAME_STATS)
        nn -= MAX_FRAME_STATS;

    s->frames[nn].renderTime = renderTime;
    s->frames[nn].frameTime  = frameTime;

    if (s->numFrames < MAX_FRAME_STATS) {
        s->numFrames += 1;
    } else {
        s->firstFrame += 1;
        if (s->firstFrame >= MAX_FRAME_STATS)
            s->firstFrame -= MAX_FRAME_STATS;
    }

    s->lastTime = now;
}

typedef void (*pPlasmaType)(uint32_t, uint32_t, uint32_t, double, uint16_t*, void*, Fixed*);

#if !defined(__GDK__) && !defined(__NOGDK__)
static void* lookupSymbol(void* pContext, const char* name)
{
    return (void*) dlsym(RTLD_DEFAULT, name);
}
#endif // !__GDK__ && !__NOGDK__

extern "C" JNIEXPORT jboolean JNICALL Java_com_example_plasma_llvm_PlasmaView_gdk(JNIEnv *env, jobject obj)
{
#if !defined(__NOGDK__)
   return JNI_TRUE;
#else
   return JNI_FALSE;
#endif
}

extern "C" JNIEXPORT jint JNICALL Java_com_example_plasma_llvm_PlasmaView_nativeRenderPlasma
    (JNIEnv * env, jobject  obj,
     jobject bitmap,  jlong  time_ms, jbyteArray scriptRef, jint length, jboolean use_llvm)
{
    AndroidBitmapInfo  info;
    void*              pixels;
    int                ret;
    static Stats       stats;
    static int         init;
    static double      time_sum = 0;
    static int         count = 0;
#if !defined(__NOGDK__)
    static bool        last_mode = false;
    static pPlasmaType native_function = NULL;
    static BCCScriptRef script_ref;

    if (last_mode != use_llvm)
      count = 0, time_sum = 0;
    last_mode = use_llvm;
#endif // !__NOGDK__

    if (!init) {
        init_tables();
        stats_init(&stats);
        init = 1;
    }

    if ((ret = AndroidBitmap_getInfo(env, bitmap, &info)) < 0) {
        LOGE("AndroidBitmap_getInfo() failed ! error=%d", ret);
        return -1;
    }

    if (info.format != ANDROID_BITMAP_FORMAT_RGB_565) {
        LOGE("Bitmap format is not RGB_565 !");
        return -1;
    }

    if ((ret = AndroidBitmap_lockPixels(env, bitmap, &pixels)) < 0) {
        LOGE("AndroidBitmap_lockPixels() failed ! error=%d", ret);
    }

#if !defined(__NOGDK__)
    if (use_llvm) {
      double start_jit = now_ms();

      if (native_function == NULL) {
        script_ref = bccCreateScript();

        jbyte* script_ptr = (jbyte *)env->GetPrimitiveArrayCritical(scriptRef, (jboolean *)0);

        LOGI("BCC Script Len: %d", length);
        if(bccReadBC(script_ref, "libplasma_portable.bc", (const char*)script_ptr, length, 0)) {
          LOGE("Error! Cannot bccReadBc");
          return -1;
        }
        if (script_ptr) {
          env->ReleasePrimitiveArrayCritical(scriptRef, script_ptr, 0);
        }

     #if 0
        if (bccLinkFile(script_ref, "/system/lib/libclcore.bc", 0)) {
          LOGE("Error! Cannot bccLinkBC");
          return -1;
        }
     #endif

        bccRegisterSymbolCallback(script_ref, lookupSymbol, NULL);

     #ifdef OLD_BCC
        if (bccPrepareExecutable(script_ref, "/data/data/com.example.plasma.llvm/plasmaLLVM.oBCC", 0)) {
          LOGE("Error! Cannot bccPrepareExecutable");
          return -1;
        }
     #else
        if (bccPrepareExecutable(script_ref, "/data/data/com.example.plasma.llvm/", "plasmaLLVM", 0)) {
          LOGE("Error! Cannot bccPrepareExecutable");
          return -1;
        }
     #endif // OLD_BCC

        native_function = (pPlasmaType)bccGetFuncAddr(script_ref, "fill_plasma");
        if (native_function == NULL) {
          LOGE("Error! Cannot find fill_plasma()");
          return -1;
        }
      }

      double start_run = now_ms();
      native_function(info.width, info.height, info.stride, time_ms, palette, pixels, angle_sin_tab);
      double diff = now_ms()-start_run;
      if (((count+1) % 30) == 0)
          LOGI("LLVM Time JIT: %.2lf , Run: %.2lf, Avg: %.2lf, count=%d", start_run-start_jit, diff, time_sum / count, count+1);
      time_sum += diff + start_run - start_jit;
    } 
    else
#endif // !__NOGDK__

    {
      double start_run = now_ms();
      fill_plasma(info.width, info.height, info.stride, time_ms, palette, pixels, angle_sin_tab);
      double diff = now_ms()-start_run;
      if (((count+1) % 30) == 0)
          LOGI("GCC Time Run: %.2lf, Avg: %.2lf, count=%d", diff, time_sum / count, count+1);
      time_sum += diff;
    }
    count++;

    AndroidBitmap_unlockPixels(env, bitmap);

    return count * 1000.0 / time_sum;
}

#endif // !__GDK
