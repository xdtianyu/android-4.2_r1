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

#include <assert.h>
#include <stdio.h>
#include <string.h>

#include "src/pixelflinger2/pixelflinger2.h"
#include "src/pixelflinger2/texture.h"
#include "src/mesa/main/mtypes.h"

#if !USE_LLVM_SCANLINE

static void Saturate(Vec4<BlendComp_t> * color)
{
   color->r = MIN2(MAX2(color->r, 0), 255);
   color->g = MIN2(MAX2(color->g, 0), 255);
   color->b = MIN2(MAX2(color->b, 0), 255);
   color->a = MIN2(MAX2(color->a, 0), 255);
}

static inline void RGBAIntToRGBAIntx4(unsigned rgba, Vec4<BlendComp_t> * color) __attribute__((always_inline));
static inline void RGBAIntToRGBAIntx4(unsigned rgba, Vec4<BlendComp_t> * color)
{
   color->r = rgba & 0xff;
   color->g = (rgba >>= 8) & 0xff;
   color->b = (rgba >>= 8) & 0xff;
   color->a = (rgba >>= 8);
}

static inline void RGBAFloatx4ToRGBAIntx4(Vector4 * v, Vec4<BlendComp_t> * color)
{
   color->r = v->r * 255;
   color->g = v->g * 255;
   color->b = v->b * 255;
   color->a = v->a * 255;
}

static inline unsigned RGBAIntx4ToRGBAInt(const Vec4<BlendComp_t> * color);
static inline unsigned RGBAIntx4ToRGBAInt(const Vec4<BlendComp_t> * color)
{
   return color->r | (color->g << 8) | (color->b << 16) | (color->a << 24);
}



//static inline Pixel Vector4ToPixelRGBA(const Vector4 * color) __attribute__((always_inline));
//static inline Pixel Vector4ToPixelRGBA(const Vector4 * color)
//{
//    Pixel pixel;
//#if defined(__ARM_HAVE_NEON) && USE_NEON
//    int32x4_t  c = vcvtq_s32_f32(vmulq_n_f32(color->f4, 255.0f));
//    c = vminq_s32(c, vdupq_n_s32(255));
//    c = vmaxq_s32(c, vdupq_n_s32(0));
//    pixel.channels[0] = (unsigned char)vgetq_lane_s32(c, 0);
//    pixel.channels[1] = (unsigned char)vgetq_lane_s32(c, 1);
//    pixel.channels[2] = (unsigned char)vgetq_lane_s32(c, 2);
//    pixel.channels[3] = (unsigned char)vgetq_lane_s32(c, 3);
//#else
//    pixel.channels[0] = (unsigned char)MIN2(MAX2((short)(color->r * 255), 0), 255);
//	pixel.channels[1] = (unsigned char)MIN2(MAX2((short)(color->g * 255), 0), 255);
//	pixel.channels[2] = (unsigned char)MIN2(MAX2((short)(color->b * 255), 0), 255);
//	pixel.channels[3] = (unsigned char)MIN2(MAX2((short)(color->a * 255), 0), 255);
//#endif //#if USE_FIXED_POINT
//	return pixel;
//}

template<typename T>
static inline void BlendFactor(const unsigned mode, T & factor, const T & src,
                               const T & dst, const T & constant, const T & one,
                               const T & zero, const BlendComp_t & srcA, const BlendComp_t & dstA,
                               const BlendComp_t & constantA, const BlendComp_t & sOne) __attribute__((always_inline));
template<typename T>
static inline void BlendFactor(const unsigned mode, T & factor, const T & src,
                               const T & dst, const T & constant, const T & one,
                               const T & zero, const BlendComp_t & srcA, const BlendComp_t & dstA,
                               const BlendComp_t & constantA, const BlendComp_t & sOne)
{
   switch (mode) {
   case 0: // GL_ZERO
      factor = zero;
      return;
   case 1: // GL_ONE
      factor = one;
      return;
   case 2: // GL_SRC_COLOR:
      factor = src;
      return;
   case 3: // GL_ONE_MINUS_SRC_COLOR:
      factor = one;
      factor -= src;
      return;
   case 4: // GL_DST_COLOR:
      factor = dst;
      return;
   case 5: // GL_ONE_MINUS_DST_COLOR:
      factor = one;
      factor -= dst;
      return;
   case 6: // GL_SRC_ALPHA:
      factor = srcA;
      return;
   case 7: // GL_ONE_MINUS_SRC_ALPHA:
      factor = sOne - srcA;
      return;
   case 8: // GL_DST_ALPHA:
      factor = dstA;
      return;
   case 9: // GL_ONE_MINUS_DST_ALPHA:
      factor = sOne - dstA;
      return;
   case 10: // GL_SRC_ALPHA_SATURATE: // valid only for source color; src alpha = 1
      factor = MIN2(srcA, sOne - dstA);
      return;
   case 11: // GL_CONSTANT_COLOR:
      factor = constant;
      return;
   case 12: // GL_ONE_MINUS_CONSTANT_COLOR:
      factor = one;
      factor -= constant;
      return;
   case 13: // GL_CONSTANT_ALPHA:
      factor = constantA;
      return;
   case 14: // GL_ONE_MINUS_CONSTANT_ALPHA:
      factor = sOne - constantA;
      return;
   default:
      assert(0);
      return;
   }
}

unsigned char StencilOp(const unsigned op, unsigned char s, const unsigned char ref)
{
   switch (op) {
   case 0: // GL_ZERO
      return 0;
   case 1: // GL_KEEP
      return s;
   case 2: // GL_REPLACE
      return ref;
   case 3: // GL_INCR
      if (s < 255)
         return ++s;
      return s;
   case 4: // GL_DECR
      if (s > 0)
         return --s;
      return 0;
   case 5: // GL_INVERT
      return ~s;
   case 6: // GL_INCR_WRAP
      return ++s;
   case 7: // GL_DECR_WRAP
      return --s;
   default:
      assert(0);
      return s;
   }
}

#endif // #if !USE_LLVM_SCANLINE

#ifdef USE_LLVM_SCANLINE
typedef void (* ScanLineFunction_t)(VertexOutput * start, VertexOutput * step,
                                    const float (*constants)[4], void * frame,
                                    int * depth, unsigned char * stencil,
                                    GGLActiveStencil *, unsigned count);
#endif

void GGLScanLine(const gl_shader_program * program, const GGLPixelFormat colorFormat,
                 void * frameBuffer, int * depthBuffer, unsigned char * stencilBuffer,
                 unsigned bufferWidth, unsigned bufferHeight, GGLActiveStencil * activeStencil,
                 const VertexOutput_t * start, const VertexOutput_t * end, const float (*constants)[4])
{
#if !USE_LLVM_SCANLINE
   assert(!"only for USE_LLVM_SCANLINE");
#endif

//   ALOGD("pf2: GGLScanLine program=%p format=0x%.2X frameBuffer=%p depthBuffer=%p stencilBuffer=%p ",
//      program, colorFormat, frameBuffer, depthBuffer, stencilBuffer);

   const unsigned int varyingCount = program->VaryingSlots;
   const unsigned y = start->position.y, startX = start->position.x,
                      endX = end->position.x;

   assert(bufferWidth > startX && bufferWidth > endX);
   assert(bufferHeight > y);

   char * frame = (char *)frameBuffer;
   if (GGL_PIXEL_FORMAT_RGBA_8888 == colorFormat)
      frame += (y * bufferWidth + startX) * 4;
   else if (GGL_PIXEL_FORMAT_RGB_565 == colorFormat)
      frame += (y * bufferWidth + startX) * 2;
   else 
      assert(0);
   const VectorComp_t div = VectorComp_t_CTR(1 / (float)(endX - startX));

   //memcpy(ctx->glCtx->CurrentProgram->ValuesVertexOutput, start, sizeof(*start));
   // shader symbols are mapped to gl_shader_program_Values*
   //VertexOutput & vertex(*(VertexOutput*)ctx->glCtx->CurrentProgram->ValuesVertexOutput);
   VertexOutput vertex(*start);
   VertexOutput vertexDx(*end);

   vertexDx.position -= start->position;
   vertexDx.position *= div;
   //printf("vertexDx.position.z=%.8g \n", vertexDx.position.z);
   for (unsigned i = 0; i < varyingCount; i++) {
      vertexDx.varyings[i] -= start->varyings[i];
      vertexDx.varyings[i] *= div;
   }
   vertexDx.frontFacingPointCoord -= start->frontFacingPointCoord;
   vertexDx.frontFacingPointCoord *= div; // gl_PointCoord, only zw
   vertexDx.frontFacingPointCoord.y = 0; // gl_FrontFacing not interpolated

   int * depth = depthBuffer + y * bufferWidth + startX;
   unsigned char * stencil = stencilBuffer + y * bufferWidth + startX;

   // TODO DXL consider inverting gl_FragCoord.y
   ScanLineFunction_t scanLineFunction = (ScanLineFunction_t)
                                         program->_LinkedShaders[MESA_SHADER_FRAGMENT]->function;
//   ALOGD("pf2 GGLScanLine scanline=%p start=%p constants=%p", scanLineFunction, &vertex, constants);
   if (endX >= startX)
      scanLineFunction(&vertex, &vertexDx, constants, frame, depth, stencil, activeStencil, endX - startX + 1);

//   ALOGD("pf2: GGLScanLine end");

}

template <bool StencilTest, bool DepthTest, bool DepthWrite, bool BlendEnable>
void ScanLine(const GGLInterface * iface, const VertexOutput * start, const VertexOutput * end)
{
   GGL_GET_CONST_CONTEXT(ctx, iface);
   GGLScanLine(ctx->CurrentProgram, ctx->frameSurface.format, ctx->frameSurface.data,
               (int *)ctx->depthSurface.data, (unsigned char *)ctx->stencilSurface.data,
               ctx->frameSurface.width, ctx->frameSurface.height, &ctx->activeStencil,
               start, end, ctx->CurrentProgram->ValuesUniform);
//   GGL_GET_CONST_CONTEXT(ctx, iface);
//   //    assert((unsigned)start->position.y == (unsigned)end->position.y);
//   //
//   //    assert(GGL_PIXEL_FORMAT_RGBA_8888 == ctx->frameSurface.format);
//   //    assert(GGL_PIXEL_FORMAT_Z_32 == ctx->depthSurface.format);
//   //    assert(ctx->frameSurface.width == ctx->depthSurface.width);
//   //    assert(ctx->frameSurface.height == ctx->depthSurface.height);
//
//   const unsigned int varyingCount = ctx->glCtx->CurrentProgram->VaryingSlots;
//   const unsigned y = start->position.y, startX = start->position.x,
//                      endX = end->position.x;
//
//   //assert(ctx->frameSurface.width > startX && ctx->frameSurface.width > endX);
//   //assert(ctx->frameSurface.height > y);
//
//   unsigned * frame = (unsigned *)ctx->frameSurface.data
//                      + y * ctx->frameSurface.width + startX;
//   const VectorComp_t div = VectorComp_t_CTR(1 / (float)(endX - startX));
//
//   //memcpy(ctx->glCtx->CurrentProgram->ValuesVertexOutput, start, sizeof(*start));
//   // shader symbols are mapped to gl_shader_program_Values*
//   //VertexOutput & vertex(*(VertexOutput*)ctx->glCtx->CurrentProgram->ValuesVertexOutput);
//   VertexOutput vertex(*start);
//   VertexOutput vertexDx(*end);
//
//   vertexDx.position -= start->position;
//   vertexDx.position *= div;
//   //printf("vertexDx.position.z=%.8g \n", vertexDx.position.z);
//   for (unsigned i = 0; i < varyingCount; i++) {
//      vertexDx.varyings[i] -= start->varyings[i];
//      vertexDx.varyings[i] *= div;
//   }
//   vertexDx.frontFacingPointCoord -= start->frontFacingPointCoord;
//   vertexDx.frontFacingPointCoord *= div; // gl_PointCoord, only zw
//   vertexDx.frontFacingPointCoord.y = 0; // gl_FrontFacing not interpolated
//
//#if USE_FORCED_FIXEDPOINT
//   for (unsigned j = 0; j < 4; j++) {
//      for (unsigned i = 0; i < varyingCount; i++) {
//         vertex.varyings[i].i[j] = vertex.varyings[i].f[j] * 65536;
//         vertexDx.varyings[i].i[j] = vertexDx.varyings[i].f[j] * 65536;
//      }
//      vertex.position.i[j] = vertex.position.f[j] * 65536;
//      vertexDx.position.i[j] = vertexDx.position.f[j] * 65536;
//      vertex.frontFacingPointCoord.i[j] = vertex.frontFacingPointCoord.f[j] * 65536;
//   }
//#endif
//
//   int * depth = (int *)ctx->depthSurface.data + y * ctx->frameSurface.width + startX;
//   unsigned char * stencil = (unsigned char *)ctx->stencilSurface.data + y * ctx->frameSurface.width + startX;
//
//#if !USE_LLVM_TEXTURE_SAMPLER
//   extern const GGLContext * textureGGLContext;
//   textureGGLContext = ctx;
//#endif
//
//   // TODO DXL consider inverting gl_FragCoord.y
//
//#if USE_LLVM_SCANLINE
//   ScanLineFunction_t scanLineFunction = (ScanLineFunction_t)
//                                         ctx->glCtx->CurrentProgram->_LinkedShaders[MESA_SHADER_FRAGMENT]->function;
//   if (endX >= startX) {
//      scanLineFunction(&vertex, &vertexDx, ctx->glCtx->CurrentProgram->ValuesUniform, frame, depth, stencil, &ctx->activeStencil, endX - startX + 1);
//   }
//#else
//
//   int z;
//   bool sCmp = true; // default passed, unless failed by stencil test
//   unsigned char s; // masked stored stencil value
//   const unsigned char sMask = ctx->activeStencil.mask;
//   const unsigned char sRef = ctx->activeStencil.ref;
//   const unsigned sFunc = ctx->activeStencil.face ? 0x200 | ctx->backStencil.func :
//                          0x200 | ctx->frontStencil.func;
//   const unsigned ssFail = ctx->activeStencil.face ? ctx->backStencil.sFail :
//                           ctx->frontStencil.sFail;
//   const unsigned sdFail = ctx->activeStencil.face ? ctx->backStencil.dFail :
//                           ctx->frontStencil.dFail;
//   const unsigned sdPass = ctx->activeStencil.face ? ctx->backStencil.dPass :
//                           ctx->frontStencil.dPass;
//
//   for (unsigned x = startX; x <= endX; x++) {
//      //assert(abs((int)(vertex.position.x) - (int)x) < 2);
//      //assert((unsigned)vertex.position.y == y);
//      if (StencilTest) {
//         s = *stencil & sMask;
//         switch (sFunc) {
//         case GL_NEVER:
//            sCmp = false;
//            break;
//         case GL_LESS:
//            sCmp = sRef < s;
//            break;
//         case GL_EQUAL:
//            sCmp = sRef == s;
//            break;
//         case GL_LEQUAL:
//            sCmp = sRef <= s;
//            break;
//         case GL_GREATER:
//            sCmp = sRef > s;
//            break;
//         case GL_NOTEQUAL:
//            sCmp = sRef != s;
//            break;
//         case GL_GEQUAL:
//            sCmp = sRef >= s;
//            break;
//         case GL_ALWAYS:
//            sCmp = true;
//            break;
//         default:
//            assert(0);
//            break;
//         }
//      }
//
//      if (!StencilTest || sCmp) {
//         z = vertex.position.i[2];
//         if (z & 0x80000000)  // negative float has leading 1
//            z ^= 0x7fffffff;  // bigger negative is smaller
//         bool zCmp = true;
//         if (DepthTest) {
//            switch (0x200 | ctx->state.bufferState.depthFunc) {
//            case GL_NEVER:
//               zCmp = false;
//               break;
//            case GL_LESS:
//               zCmp = z < *depth;
//               break;
//            case GL_EQUAL:
//               zCmp = z == *depth;
//               break;
//            case GL_LEQUAL:
//               zCmp = z <= *depth;
//               break;
//            case GL_GREATER:
//               zCmp = z > *depth;
//               break;
//            case GL_NOTEQUAL:
//               zCmp = z != *depth;
//               break;
//            case GL_GEQUAL:
//               zCmp = z >= *depth;
//               break;
//            case GL_ALWAYS:
//               zCmp = true;
//               break;
//            default:
//               assert(0);
//               break;
//            }
//         }
//         if (!DepthTest || zCmp) {
//            float * varying = (float *)ctx->glCtx->CurrentProgram->ValuesVertexOutput;
//            ShaderFunction_t function = (ShaderFunction_t)ctx->glCtx->CurrentProgram->_LinkedShaders[MESA_SHADER_FRAGMENT]->function;
//            function(&vertex, &vertex, ctx->glCtx->CurrentProgram->ValuesUniform);
//            //ctx->glCtx->CurrentProgram->_LinkedShaders[MESA_SHADER_FRAGMENT]->function();
//            if (BlendEnable) {
//               BlendComp_t sOne = 255, sZero = 0;
//               Vec4<BlendComp_t> one = sOne, zero = sZero;
//
//               Vec4<BlendComp_t> src;
////                    if (outputRegDesc.IsInt32Color())
////                        RGBAIntToRGBAIntx4(vertex.fragColor[0].u[0], &src);
////                    else if (outputRegDesc.IsVectorType(Float))
//               RGBAFloatx4ToRGBAIntx4(&vertex.fragColor[0], &src);
////                    else if (outputRegDesc.IsVectorType(Fixed8))
////                    {
////                        src.u[0] = vertex.fragColor[0].u[0];
////                        src.u[1] = vertex.fragColor[0].u[1];
////                        src.u[2] = vertex.fragColor[0].u[2];
////                        src.u[3] = vertex.fragColor[0].u[3];
////                    }
////                    else
////                        assert(0);
//
//               Vec4<BlendComp_t> dst;
//               unsigned dc = *frame;
//               dst.r = dc & 255;
//               dst.g = (dc >>= 8) & 255;
//               dst.b = (dc >>= 8) & 255;
//               dst.a = (dc >>= 8) & 255;
//
//               Vec4<BlendComp_t> sf, df;
//               Vec4<BlendComp_t> blendStateColor(ctx->state.blendState.color[0], ctx->state.blendState.color[1],
//                                                 ctx->state.blendState.color[2], ctx->state.blendState.color[3]);
//
//               BlendFactor(ctx->state.blendState.scf, sf, src, dst,
//                           blendStateColor, one, zero, src.a, dst.a,
//                           blendStateColor.a, sOne);
//               if (ctx->state.blendState.scf != ctx->state.blendState.saf)
//                  BlendFactor(ctx->state.blendState.saf, sf.a, src.a, dst.a,
//                              blendStateColor.a, sOne, sZero, src.a, dst.a,
//                              blendStateColor.a, sOne);
//               BlendFactor(ctx->state.blendState.dcf, df, src, dst,
//                           blendStateColor, one, zero, src.a, dst.a,
//                           blendStateColor.a, sOne);
//               if (ctx->state.blendState.dcf != ctx->state.blendState.daf)
//                  BlendFactor(ctx->state.blendState.daf, df.a, src.a, dst.a,
//                              blendStateColor.a, sOne, sZero, src.a, dst.a,
//                              blendStateColor.a, sOne);
//
//               Vec4<BlendComp_t> sfs(sf), dfs(df);
//               sfs.LShr(7);
//               sf += sfs;
//               dfs.LShr(7);
//               df += dfs;
//
//               src *= sf;
//               dst *= df;
//               Vec4<BlendComp_t> res(src);
//               switch (ctx->state.blendState.ce + GL_FUNC_ADD) {
//               case GL_FUNC_ADD:
//                  res += dst;
//                  break;
//               case GL_FUNC_SUBTRACT:
//                  res -= dst;
//                  break;
//               case GL_FUNC_REVERSE_SUBTRACT:
//                  res = dst;
//                  res -= src;
//                  break;
//               default:
//                  assert(0);
//                  break;
//               }
//               if (ctx->state.blendState.ce != ctx->state.blendState.ae)
//                  switch (ctx->state.blendState.ce + GL_FUNC_ADD) {
//                  case GL_FUNC_ADD:
//                     res.a = src.a + dst.a;
//                     break;
//                  case GL_FUNC_SUBTRACT:
//                     res.a = src.a - dst.a;
//                     break;
//                  case GL_FUNC_REVERSE_SUBTRACT:
//                     res.a = dst.a - src.a;
//                     break;
//                  default:
//                     assert(0);
//                     break;
//                  }
//
//               res.AShr(8);
//               Saturate(&res);
//               *frame = RGBAIntx4ToRGBAInt(&res);
//            } else {
////                    if (outputRegDesc.IsInt32Color())
////                        *frame = vertex.fragColor[0].u[0];
////                    else if (outputRegDesc.IsVectorType(Float))
//               {
//                  Vec4<BlendComp_t> src;
//                  RGBAFloatx4ToRGBAIntx4(&vertex.fragColor[0], &src);
//                  Saturate(&src);
//                  *frame = RGBAIntx4ToRGBAInt(&src);
//               }
////                    else if (outputRegDesc.IsVectorType(Fixed16))
////                    {
////                        Vec4<BlendComp_t> & src = (Vec4<BlendComp_t> &)vertex.fragColor[0];
////                        src.r = (src.r * 255 >> 16);
////                        src.g = (src.g * 255 >> 16);
////                        src.b = (src.b * 255 >> 16);
////                        src.a = (src.a * 255 >> 16);
////                        Saturate(&src);
////                        *frame = RGBAIntx4ToRGBAInt(&src);
////                    }
////                    else if (outputRegDesc.IsVectorType(Fixed8))
////                    {
////                        Vec4<BlendComp_t> & src = (Vec4<BlendComp_t> &)vertex.fragColor[0];
////                        Saturate(&src);
////                        *frame = RGBAIntx4ToRGBAInt(&src);
////                    }
////                    else
////                        assert(0);
//            }
//
//            if (DepthWrite)
//               *depth = z;
//            if (StencilTest)
//               *stencil = StencilOp(sdPass, s, sRef);
//         } else if (StencilTest)
//            *stencil = StencilOp(sdFail, s, sRef);
//      } else if (StencilTest)
//         *stencil = StencilOp(ssFail, s, sRef);
//
//      frame++;
//      depth++;
//      stencil++;
//
//#if USE_FORCED_FIXEDPOINT
//      for (unsigned j = 0; j < 4; j++) {
//         if (ctx->glCtx->Shader.CurrentProgram->FragmentProgram->UsesFragCoord)
//            vertex.position.i[j] += vertexDx.position.i[j];
//         for (unsigned i = 0; i < varyingCount; i++)
//            vertex.varyings[i].i[j] += vertexDx.varyings[i].i[j];
//      }
//      vertex.position.i[2] += vertexDx.position.i[2];
//      if (ctx->glCtx->Shader.CurrentProgram->FragmentProgram->UsesPointCoord) {
//         vertex.frontFacingPointCoord.i[2] = vertexDx.frontFacingPointCoord.i[2];
//         vertex.frontFacingPointCoord.i[3] = vertexDx.frontFacingPointCoord.i[3];
//      }
//#else
//   if (ctx->glCtx->CurrentProgram->UsesFragCoord)
//      vertex.position += vertexDx.position;
//   else if (ctx->state.bufferState.depthTest)
//      vertex.position.z += vertexDx.position.z;
//
//   for (unsigned i = 0; i < varyingCount; i++)
//      vertex.varyings[i] += vertexDx.varyings[i];
//   if (ctx->glCtx->CurrentProgram->UsesPointCoord) {
//      vertex.frontFacingPointCoord.z += vertexDx.frontFacingPointCoord.z;
//      vertex.frontFacingPointCoord.w += vertexDx.frontFacingPointCoord.w;
//   }
//#endif // #if USE_FORCED_FIXEDPOINT
//   }
//
//#endif // #if USE_LLVM_SCANLINE
//
//#if !USE_LLVM_TEXTURE_SAMPLER
//   textureGGLContext = NULL;
//#endif
}

static void PickScanLine(GGLInterface * iface)
{
   GGL_GET_CONTEXT(ctx, iface);

   ctx->interface.ScanLine = NULL;
   if (ctx->state.bufferState.stencilTest) {
      if (ctx->state.bufferState.depthTest) {
         if (ctx->state.blendState.enable)
            ctx->interface.ScanLine = ScanLine<true, true, true, true>;
         else
            ctx->interface.ScanLine = ScanLine<true, true, true, false>;
      } else {
         if (ctx->state.blendState.enable)
            ctx->interface.ScanLine = ScanLine<true, false, false, true>;
         else
            ctx->interface.ScanLine = ScanLine<true, false, false, false>;
      }
   } else {
      if (ctx->state.bufferState.depthTest) {
         if (ctx->state.blendState.enable)
            ctx->interface.ScanLine = ScanLine<false, true, true, true>;
         else
            ctx->interface.ScanLine = ScanLine<false, true, true, false>;
      } else {
         if (ctx->state.blendState.enable)
            ctx->interface.ScanLine = ScanLine<false, false, false, true>;
         else
            ctx->interface.ScanLine = ScanLine<false, false, false, false>;
      }
   }

   assert(ctx->interface.ScanLine);
}

void InitializeScanLineFunctions(GGLInterface * iface)
{
   GGL_GET_CONTEXT(ctx, iface);
   ctx->PickScanLine = PickScanLine;
}
