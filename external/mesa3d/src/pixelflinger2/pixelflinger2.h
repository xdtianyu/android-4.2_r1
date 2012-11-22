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

#ifndef _PIXELFLINGER2_H_
#define _PIXELFLINGER2_H_

#define USE_LLVM_TEXTURE_SAMPLER 1
#define USE_LLVM_SCANLINE 1
#ifndef USE_LLVM_EXECUTIONENGINE
#define USE_LLVM_EXECUTIONENGINE 0 // 1 to use llvm::Execution, 0 to use libBCC, requires modifying makefile
#endif
#define USE_DUAL_THREAD 1

#define debug_printf printf

#include <stdlib.h>
#include <assert.h>
#include <stdio.h>

#ifdef __arm__
#include <cutils/log.h>

#ifndef __location__
#define __HIERALLOC_STRING_0__(s)   #s
#define __HIERALLOC_STRING_1__(s)   __HIERALLOC_STRING_0__(s)
#define __HIERALLOC_STRING_2__      __HIERALLOC_STRING_1__(__LINE__)
#define __location__                __FILE__ ":" __HIERALLOC_STRING_2__
#endif
#undef assert
#define assert(EXPR) { do { if (!(EXPR)) {ALOGD("\n*\n*\n*\n* assert fail: '"#EXPR"' at "__location__"\n*\n*\n*"); exit(EXIT_FAILURE); } } while (false); }

#else // #ifdef __arm__

#ifndef ALOGD
#define ALOGD printf
#endif //#include <stdio.h>

#endif // #ifdef __arm__

#include "pixelflinger2/pixelflinger2_interface.h"

#include <string.h>

#ifndef MIN2
#  define MIN2(a, b) ((a) < (b) ? (a) : (b))
#endif
#ifndef MAX2
#  define MAX2(a, b) ((a) > (b) ? (a) : (b))
#endif

namespace bcc
{
class BCCContext;
};

#if !USE_LLVM_SCANLINE
typedef int BlendComp_t;
#endif

#if USE_DUAL_THREAD
#include <pthread.h>
#endif

typedef void (*ShaderFunction_t)(const void*,void*,const void*);

#define GGL_GET_CONTEXT(context, interface) GGLContext * context = (GGLContext *)interface;
#define GGL_GET_CONST_CONTEXT(context, interface) const GGLContext * context = \
    (const GGLContext *)interface; (void)context;

struct GGLContext {
   GGLInterface interface; // must be first member so that GGLContext * == GGLInterface *

   GGLSurface frameSurface;
   GGLSurface depthSurface;
   GGLSurface stencilSurface;

   bcc::BCCContext * bccCtx;

   struct {
      int depth; // assuming ieee 754 32 bit float and 32 bit 2's complement int; z_32
      unsigned color; // clear value; rgba_8888
      unsigned stencil; // s_8; repeated to clear 4 pixels at a time
   } clearState;

   gl_shader_program * CurrentProgram;

   mutable GGLActiveStencil activeStencil; // after primitive assembly, call StencilSelect

   GGLState state; // states affecting jit

#if USE_DUAL_THREAD
   mutable struct Worker {
      const GGLInterface * iface;
      unsigned startY, endY, varyingCount;
      VertexOutput bV, cV, bDx, cDx;
      int width, height;
      bool assignedWork; // only used by main; worker uses assignCond & quit
      bool quit;

      pthread_cond_t assignCond;
      pthread_mutex_t assignLock; // held by worker execpt for during cond_wait assign
      pthread_cond_t finishCond;
      pthread_mutex_t finishLock; // held by main except for during cond_wait finish
      pthread_t thread;

      Worker() : assignedWork(false), quit(false), thread(0)
      {
         pthread_cond_init(&assignCond, NULL);
         pthread_cond_init(&finishCond, NULL);
         pthread_mutex_init(&assignLock, NULL);
         pthread_mutex_init(&finishLock, NULL);
         pthread_mutex_lock(&finishLock);
         // actual thread is created later in raster.cpp
      }
      ~Worker()
      {
         if (0 != thread)
         {
            pthread_mutex_lock(&assignLock);
            quit = true;
            pthread_cond_signal(&assignCond); // signal thread to quit
            pthread_mutex_unlock(&assignLock);
            pthread_join(thread, NULL);
         }
         pthread_mutex_unlock(&finishLock);

         pthread_cond_destroy(&assignCond);
         pthread_cond_destroy(&finishCond);
         pthread_mutex_destroy(&assignLock);
         pthread_mutex_destroy(&finishLock);
      }
   } worker;
#endif

   // called by ShaderUse to set to proper rendering functions
   void (* PickScanLine)(GGLInterface * iface);
   void (* PickRaster)(GGLInterface * iface);

   // viewport params are transformed so that Zw = Zd * f + n
   // and Xw/Yw = x/y + Xd/Yd * w/h
   struct {
      VectorComp_t x, y, w, h, n, f;
   } viewport; // should be moved into libAgl2

   struct { // should be moved into libAgl2
unsigned enable :
      1;
unsigned frontFace :
      1; // GL_CW = 0, GL_CCW, actual value is GLenum - GL_CW
unsigned cullFace :
      2; // GL_FRONT = 0, GL_BACK, GL_FRONT_AND_BACK, value = GLenum - GL_FRONT
   } cullState;
};

#define _PF2_TEXTURE_DATA_NAME_ "gl_PF2TEXTURE_DATA" /* sampler data pointers used by LLVM */
#define _PF2_TEXTURE_DIMENSIONS_NAME_ "gl_PF2TEXTURE_DIMENSIONS" /* sampler dimensions used by LLVM */

void gglError(unsigned error); // not implmented, just an assert

void InitializeGGLState(GGLInterface * iface); // should be private
void UninitializeGGLState(GGLInterface * iface); // should be private

// they just set the function pointers
void InitializeBufferFunctions(GGLInterface * iface);
void InitializeRasterFunctions(GGLInterface * iface);
void InitializeScanLineFunctions(GGLInterface * iface);
void InitializeTextureFunctions(GGLInterface * iface);

void InitializeShaderFunctions(GGLInterface * iface); // set function pointers and create needed objects
void SetShaderVerifyFunctions(GGLInterface * iface); // called by state change functions
void DestroyShaderFunctions(GGLInterface * iface); // destroy needed objects
// actual gl_shader and gl_shader_program is created and destroyed by Shader(Program)Create/Delete,

#endif // #ifndef _PIXELFLINGER2_H_
