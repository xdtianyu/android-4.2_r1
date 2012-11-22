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

#include "src/pixelflinger2/pixelflinger2.h"

#include <string.h>
#include <stdio.h>

void SetShaderVerifyFunctions(GGLInterface *);

static void DepthFunc(GGLInterface * iface, GLenum func)
{
   GGL_GET_CONTEXT(ctx, iface);
   if (GL_NEVER > func || GL_ALWAYS < func)
      return gglError(GL_INVALID_ENUM);
   ctx->state.bufferState.depthFunc = func & 0x7;
   SetShaderVerifyFunctions(iface);
}

static void StencilFuncSeparate(GGLInterface * iface, GLenum face, GLenum func, GLint ref, GLuint mask)
{
   GGL_GET_CONTEXT(ctx, iface);
   if (GL_FRONT > face || GL_FRONT_AND_BACK < face)
      return gglError(GL_INVALID_ENUM);
   if (GL_NEVER > func || GL_ALWAYS < func)
      return gglError(GL_INVALID_ENUM);
   mask &= 0xff;
   ref = MAX2(MIN2(ref, 0xff), 0);
   ref &= mask;
   if (GL_FRONT == face || GL_FRONT_AND_BACK == face) {
      ctx->state.frontStencil.ref = ref;
      ctx->state.frontStencil.mask = mask;
      ctx->state.frontStencil.func = func & 0x7;
   }
   if (GL_BACK == face || GL_FRONT_AND_BACK == face) {
      ctx->state.backStencil.ref = ref;
      ctx->state.backStencil.mask = mask;
      ctx->state.backStencil.func = func & 0x7;
   }
   SetShaderVerifyFunctions(iface);
}

static unsigned StencilOpEnum(GLenum func, unsigned oldValue)
{
   switch (func) {
   case GL_ZERO:
      return 0;
   case GL_KEEP: // fall through
   case GL_REPLACE: // fall through
   case GL_INCR: // fall through
   case GL_DECR:
      return func - GL_KEEP + 1;
      break;
   case GL_INVERT:
      return 5;
   case GL_INCR_WRAP:
      return 6;
   case GL_DECR_WRAP:
      return 7;
   default:
      gglError(GL_INVALID_ENUM);
      return oldValue;
   }
}

static void StencilOpSeparate(GGLInterface * iface, GLenum face, GLenum sfail, GLenum dpfail, GLenum dppass)
{
   GGL_GET_CONTEXT(ctx, iface);
   if (GL_FRONT > face || GL_FRONT_AND_BACK < face)
      return gglError(GL_INVALID_ENUM);
   if (GL_FRONT == face || GL_FRONT_AND_BACK == face) {
      ctx->state.frontStencil.sFail = StencilOpEnum(sfail, ctx->state.frontStencil.sFail);
      ctx->state.frontStencil.dFail = StencilOpEnum(dpfail, ctx->state.frontStencil.dFail);
      ctx->state.frontStencil.dPass = StencilOpEnum(dppass, ctx->state.frontStencil.dPass);
   }
   if (GL_BACK == face || GL_FRONT_AND_BACK == face) {
      ctx->state.backStencil.sFail = StencilOpEnum(sfail, ctx->state.backStencil.sFail);
      ctx->state.backStencil.dFail = StencilOpEnum(dpfail, ctx->state.backStencil.dFail);
      ctx->state.backStencil.dPass = StencilOpEnum(dppass, ctx->state.backStencil.dPass);
   }
   SetShaderVerifyFunctions(iface);
}

static void StencilSelect(const GGLInterface * iface, GLenum face)
{
   GGL_GET_CONTEXT(ctx, iface);
   if (GL_FRONT == face) {
      ctx->activeStencil.face = 0;
      ctx->activeStencil.ref = ctx->state.frontStencil.ref;
      ctx->activeStencil.mask = ctx->state.frontStencil.mask;
   } else if (GL_BACK == face) {
      ctx->activeStencil.face = 1;
      ctx->activeStencil.ref = ctx->state.backStencil.ref;
      ctx->activeStencil.mask = ctx->state.backStencil.mask;
   }
}

static void ClearStencil(GGLInterface * iface, GLint s)
{
   GGL_GET_CONTEXT(ctx, iface);
   ctx->clearState.stencil = 0x01010101 * ((unsigned &)s & 0xff);
}

static void ClearColor(GGLInterface * iface, GLclampf r, GLclampf g, GLclampf b, GLclampf a)
{
   GGL_GET_CONTEXT(ctx, iface);
   r = MAX2(MIN2(r, 1.0f), 0);
   g = MAX2(MIN2(g, 1.0f), 0);
   b = MAX2(MIN2(b, 1.0f), 0);
   a = MAX2(MIN2(a, 1.0f), 0);
   ctx->clearState.color = (unsigned(a * 255) << 24) | (unsigned(b * 255) << 16) |
                           (unsigned(g * 255) << 8) | unsigned(r * 255);
}

static void ClearDepthf(GGLInterface * iface, GLclampf d)
{
   GGL_GET_CONTEXT(ctx, iface);
   // assuming ieee 754 32 bit float and 32 bit 2's complement int
   assert(sizeof(d) == sizeof(ctx->clearState.depth));
   ctx->clearState.depth = (int &)d; // bit reinterpretation
   if (0x80000000 & ctx->clearState.depth) // smaller negative float has bigger int representation, so flip
      ctx->clearState.depth ^= 0x7fffffff; // since -FLT_MAX is close to -1 when bitcasted
}

static void Clear(const GGLInterface * iface, GLbitfield buf)
{
   GGL_GET_CONST_CONTEXT(ctx, iface);

   // TODO DXL scissor test
   if (GL_COLOR_BUFFER_BIT & buf && ctx->frameSurface.data) {
      if (GGL_PIXEL_FORMAT_RGBA_8888 == ctx->frameSurface.format) {
         unsigned * const end = (unsigned *)ctx->frameSurface.data +
                                ctx->frameSurface.width * ctx->frameSurface.height;
         const unsigned color = ctx->clearState.color;
         for (unsigned * start = (unsigned *)ctx->frameSurface.data; start < end; start++)
            *start = color;
      } else if (GGL_PIXEL_FORMAT_RGB_565 == ctx->frameSurface.format) {
         short * const end = (short *)ctx->frameSurface.data +
                             ctx->frameSurface.width * ctx->frameSurface.height;
         unsigned r = ctx->clearState.color & 0xf8, g = ctx->clearState.color & 0xfc00,
                      b = ctx->clearState.color & 0xf80000;
         const short color = (b >> 19) | (g >> 5) | (r >> 3);
         for (short * start = (short *)ctx->frameSurface.data; start < end; start++)
            *start = color;
      } else
         assert(0);
   }
   if (GL_DEPTH_BUFFER_BIT & buf && ctx->depthSurface.data) {
      assert(GGL_PIXEL_FORMAT_Z_32 == ctx->depthSurface.format);
      unsigned * const end = (unsigned *)ctx->depthSurface.data +
                             ctx->depthSurface.width * ctx->depthSurface.height;
      const unsigned depth = ctx->clearState.depth;
      for (unsigned * start = (unsigned *)ctx->depthSurface.data; start < end; start++)
         *start = depth;
   }
   if (GL_STENCIL_BUFFER_BIT & buf && ctx->stencilSurface.data) {
      assert(GGL_PIXEL_FORMAT_S_8 == ctx->stencilSurface.format);
      unsigned * const end = (unsigned *)((unsigned char *)ctx->stencilSurface.data +
                                          ctx->stencilSurface.width * ctx->stencilSurface.height);
      unsigned * start = (unsigned *)ctx->stencilSurface.data;
      const unsigned stencil = ctx->clearState.stencil;
      for (start; start < end; start++)
         *start = stencil;
      start--;
      for (unsigned char * i = (unsigned char *)start; i < (unsigned char *)end; i++)
         *i = stencil & 0xff;
   }
}

static void SetBuffer(GGLInterface * iface, const GLenum type, GGLSurface * surface)
{
   GGL_GET_CONTEXT(ctx, iface);
   bool changed = false;
   if (GL_COLOR_BUFFER_BIT == type) {
      if (surface) {
         ctx->frameSurface = *surface;
         changed |= ctx->frameSurface.format ^ surface->format;
         switch (surface->format) {
         case GGL_PIXEL_FORMAT_RGBA_8888:
         case GGL_PIXEL_FORMAT_RGB_565:
            break;
         case GGL_PIXEL_FORMAT_RGBX_8888:
         default:
            ALOGD("pf2: SetBuffer 0x%.04X format=0x%.02X \n", type, surface ? surface->format : 0);
            assert(0);
         }
      } else {
         memset(&ctx->frameSurface, 0, sizeof(ctx->frameSurface));
         changed = true;
      }
      ctx->state.bufferState.colorFormat = ctx->frameSurface.format;
   } else if (GL_DEPTH_BUFFER_BIT == type) {
      if (surface) {
         ctx->depthSurface = *surface;
         changed |= ctx->depthSurface.format ^ surface->format;
         assert(GGL_PIXEL_FORMAT_Z_32 == ctx->depthSurface.format);
      } else {
         memset(&ctx->depthSurface, 0, sizeof(ctx->depthSurface));
         changed = true;
      }
      ctx->state.bufferState.depthFormat = ctx->depthSurface.format;
   } else if (GL_STENCIL_BUFFER_BIT == type) {
      if (surface) {
         ctx->stencilSurface = *surface;
         changed |= ctx->stencilSurface.format ^ surface->format;
         assert(GGL_PIXEL_FORMAT_S_8 == ctx->stencilSurface.format);
      } else {
         memset(&ctx->stencilSurface, 0, sizeof(ctx->stencilSurface));
         changed = true;
      }
      ctx->state.bufferState.stencilFormat = ctx->stencilSurface.format;
   } else
      gglError(GL_INVALID_ENUM);
   if (changed) {
      SetShaderVerifyFunctions(iface);
   }
}

void InitializeBufferFunctions(GGLInterface * iface)
{
   iface->DepthFunc = DepthFunc;
   iface->StencilFuncSeparate = StencilFuncSeparate;
   iface->StencilOpSeparate = StencilOpSeparate;
   iface->StencilSelect = StencilSelect;
   iface->ClearStencil = ClearStencil;
   iface->ClearColor = ClearColor;
   iface->ClearDepthf = ClearDepthf;
   iface->Clear = Clear;
   iface->SetBuffer = SetBuffer;
}
