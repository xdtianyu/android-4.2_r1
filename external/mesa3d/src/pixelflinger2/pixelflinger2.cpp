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

#include "pixelflinger2.h"

#include "src/talloc/hieralloc.h"
#include <string>

void gglError(unsigned error)
{
   std::string str;
   if (GL_NO_ERROR == error)
      return;
   ALOGD("\n*\n*\n pf2: gglError 0x%.4X \n*\n*\n", error);
   assert(0);
}

static void DepthRangef(GGLInterface * iface, GLclampf zNear, GLclampf zFar)
{
   GGL_GET_CONTEXT(ctx, iface);
   ctx->viewport.n = VectorComp_t_CTR((zNear + zFar) / 2);
   ctx->viewport.f = VectorComp_t_CTR((zFar - zNear) / 2);
}

static void Viewport(GGLInterface * iface, GLint x, GLint y, GLsizei width, GLsizei height)
{
   GGL_GET_CONTEXT(ctx, iface);
   ctx->viewport.x = VectorComp_t_CTR(x + width / 2);
   ctx->viewport.y = VectorComp_t_CTR(y + height / 2);
   ctx->viewport.w = VectorComp_t_CTR(width / 2);
   ctx->viewport.h = VectorComp_t_CTR(height / 2);
}

static void CullFace(GGLInterface * iface, GLenum mode)
{
   GGL_GET_CONTEXT(ctx, iface);
   if (GL_FRONT > mode || GL_FRONT_AND_BACK < mode)
      gglError(GL_INVALID_ENUM);
   else
      ctx->cullState.cullFace = mode - GL_FRONT;
}

static void FrontFace(GGLInterface * iface, GLenum mode)
{
   GGL_GET_CONTEXT(ctx, iface);
   if (GL_CW > mode || GL_CCW < mode)
      gglError(GL_INVALID_ENUM);
   else
      ctx->cullState.frontFace = mode - GL_CW;
}

static void BlendColor(GGLInterface * iface, GLclampf red, GLclampf green, GLclampf blue, GLclampf alpha)
{
   GGL_GET_CONTEXT(ctx, iface);
   ctx->state.blendState.color[0] = MIN2(MAX2(red * 255, 0.0f), 255.0f);
   ctx->state.blendState.color[1] = MIN2(MAX2(green * 255, 0.0f), 255.0f);
   ctx->state.blendState.color[2] = MIN2(MAX2(blue * 255, 0.0f), 255.0f);
   ctx->state.blendState.color[3] = MIN2(MAX2(alpha * 255, 0.0f), 255.0f);
   SetShaderVerifyFunctions(iface);
}

static void BlendEquationSeparate(GGLInterface * iface, GLenum modeRGB, GLenum modeAlpha)
{
   GGL_GET_CONTEXT(ctx, iface);
   if (GL_FUNC_ADD != modeRGB && (GL_FUNC_SUBTRACT > modeRGB ||
                                  GL_FUNC_REVERSE_SUBTRACT < modeRGB))
      return gglError(GL_INVALID_ENUM);
   if (GL_FUNC_ADD != modeRGB && (GL_FUNC_SUBTRACT > modeRGB ||
                                  GL_FUNC_REVERSE_SUBTRACT < modeRGB))
      return gglError(GL_INVALID_ENUM);
   ctx->state.blendState.ce = (GGLBlendState::GGLBlendFunc)(modeRGB - GL_FUNC_ADD);
   ctx->state.blendState.ae = (GGLBlendState::GGLBlendFunc)(modeAlpha - GL_FUNC_ADD);
   SetShaderVerifyFunctions(iface);
}

static inline GGLBlendState::GGLBlendFactor GLBlendFactor(const GLenum factor)
{
#define SWITCH_LINE(c) case c: return GGLBlendState::G##c;
   switch (factor)
   {
      SWITCH_LINE(GL_ZERO);
      SWITCH_LINE(GL_ONE);
      SWITCH_LINE(GL_SRC_COLOR);
      SWITCH_LINE(GL_ONE_MINUS_SRC_COLOR);
      SWITCH_LINE(GL_DST_COLOR);
      SWITCH_LINE(GL_ONE_MINUS_DST_COLOR);
      SWITCH_LINE(GL_SRC_ALPHA);
      SWITCH_LINE(GL_ONE_MINUS_SRC_ALPHA);
      SWITCH_LINE(GL_DST_ALPHA);
      SWITCH_LINE(GL_ONE_MINUS_DST_ALPHA);
      SWITCH_LINE(GL_SRC_ALPHA_SATURATE);
      SWITCH_LINE(GL_CONSTANT_COLOR);
      SWITCH_LINE(GL_ONE_MINUS_CONSTANT_COLOR);
      SWITCH_LINE(GL_CONSTANT_ALPHA);
      SWITCH_LINE(GL_ONE_MINUS_CONSTANT_ALPHA);
      default: assert(0); return GGLBlendState::GGL_ZERO;
   }
#undef SWITCH_LINE
}

static void BlendFuncSeparate(GGLInterface * iface, GLenum srcRGB, GLenum dstRGB, GLenum srcAlpha, GLenum dstAlpha)
{
   GGL_GET_CONTEXT(ctx, iface);
   if (GL_ZERO != srcRGB && GL_ONE != srcRGB &&
         (GL_SRC_COLOR > srcRGB || GL_SRC_ALPHA_SATURATE < srcRGB) &&
         (GL_CONSTANT_COLOR > srcRGB || GL_ONE_MINUS_CONSTANT_ALPHA < srcRGB))
      return gglError(GL_INVALID_ENUM);
   if (GL_ZERO != srcAlpha && GL_ONE != srcAlpha &&
         (GL_SRC_COLOR > srcAlpha || GL_SRC_ALPHA_SATURATE < srcAlpha) &&
         (GL_CONSTANT_COLOR > dstRGB || GL_ONE_MINUS_CONSTANT_ALPHA < dstRGB))
      return gglError(GL_INVALID_ENUM);
   if (GL_ZERO != dstRGB && GL_ONE != dstRGB &&
         (GL_SRC_COLOR > dstRGB || GL_ONE_MINUS_DST_COLOR < dstRGB) && // GL_SRC_ALPHA_SATURATE only for source
         (GL_CONSTANT_COLOR > dstRGB || GL_ONE_MINUS_CONSTANT_ALPHA < dstRGB))
      return gglError(GL_INVALID_ENUM);
   if (GL_ZERO != dstAlpha && GL_ONE != dstAlpha &&
         (GL_SRC_COLOR > dstAlpha || GL_ONE_MINUS_DST_COLOR < dstAlpha) &&
         (GL_CONSTANT_COLOR > dstRGB || GL_ONE_MINUS_CONSTANT_ALPHA < dstRGB))
      return gglError(GL_INVALID_ENUM);
   if (srcAlpha == GL_SRC_ALPHA_SATURATE) // it's just 1 instead of min(sa, 1 - da) for alpha channel
      srcAlpha = GL_ONE;
   // in c++ it's templated function for color and alpha,
   // so it requires setting srcAlpha to GL_ONE to run template again only for alpha
   ctx->state.blendState.scf = GLBlendFactor(srcRGB);
   ctx->state.blendState.saf = GLBlendFactor(srcAlpha);
   ctx->state.blendState.dcf = GLBlendFactor(dstRGB);
   ctx->state.blendState.daf = GLBlendFactor(dstAlpha);
   SetShaderVerifyFunctions(iface);

}

static void EnableDisable(GGLInterface * iface, GLenum cap, GLboolean enable)
{
   GGL_GET_CONTEXT(ctx, iface);
   bool changed = false;
   switch (cap) {
   case GL_BLEND:
      changed |= ctx->state.blendState.enable ^ enable;
      ctx->state.blendState.enable = enable;
      break;
   case GL_CULL_FACE:
      changed |= ctx->cullState.enable ^ enable;
      ctx->cullState.enable = enable;
      break;
   case GL_DEPTH_TEST:
      changed |= ctx->state.bufferState.depthTest ^ enable;
      ctx->state.bufferState.depthTest = enable;
      break;
   case GL_STENCIL_TEST:
      changed |= ctx->state.bufferState.stencilTest ^ enable;
      ctx->state.bufferState.stencilTest = enable;
      break;
   case GL_DITHER:
//      ALOGD("pf2: EnableDisable GL_DITHER \n");
      break;
   case GL_SCISSOR_TEST:
//      ALOGD("pf2: EnableDisable GL_SCISSOR_TEST \n");
      break;
   case GL_TEXTURE_2D:
//      ALOGD("pf2: EnableDisable GL_SCISSOR_TEST %d", enable);
      break;
   default:
      ALOGD("pf2: EnableDisable 0x%.4X causes GL_INVALID_ENUM (maybe not implemented or ES 1.0) \n", cap);
//      gglError(GL_INVALID_ENUM);
      assert(0);
      break;
   }
   if (changed)
      SetShaderVerifyFunctions(iface);
}

void InitializeGGLState(GGLInterface * iface)
{
#if USE_DUAL_THREAD
   reinterpret_cast<GGLContext *>(iface)->worker = GGLContext::Worker();
#endif
   iface->DepthRangef = DepthRangef;
   iface->Viewport = Viewport;
   iface->CullFace = CullFace;
   iface->FrontFace = FrontFace;
   iface->BlendColor = BlendColor;
   iface->BlendEquationSeparate = BlendEquationSeparate;
   iface->BlendFuncSeparate = BlendFuncSeparate;
   iface->EnableDisable = EnableDisable;

   InitializeBufferFunctions(iface);
   InitializeRasterFunctions(iface);
   InitializeScanLineFunctions(iface);
   InitializeShaderFunctions(iface);
   InitializeTextureFunctions(iface);

   iface->EnableDisable(iface, GL_DEPTH_TEST, false);
   iface->DepthFunc(iface, GL_LESS);
   iface->ClearColor(iface, 0, 0, 0, 0);
   iface->ClearDepthf(iface, 1.0f);

   iface->EnableDisable(iface, GL_STENCIL_TEST, false);
   iface->StencilFuncSeparate(iface, GL_FRONT_AND_BACK, GL_ALWAYS, 0, 0xff);
   iface->StencilOpSeparate(iface, GL_FRONT_AND_BACK, GL_KEEP, GL_KEEP, GL_KEEP);

   iface->FrontFace(iface, GL_CCW);
   iface->CullFace(iface, GL_BACK);
   iface->EnableDisable(iface, GL_CULL_FACE, false);

   iface->EnableDisable(iface, GL_BLEND, false);
   iface->BlendColor(iface, 0, 0, 0, 0);
   iface->BlendEquationSeparate(iface, GL_FUNC_ADD, GL_FUNC_ADD);
   iface->BlendFuncSeparate(iface, GL_ONE, GL_ZERO, GL_ONE, GL_ZERO);

   for (unsigned i = 0; i < GGL_MAXCOMBINEDTEXTUREIMAGEUNITS; i++)
      iface->SetSampler(iface, i, NULL);

   iface->SetBuffer(iface, GL_COLOR_BUFFER_BIT, NULL);
   iface->SetBuffer(iface, GL_DEPTH_BUFFER_BIT, NULL);
   iface->SetBuffer(iface, GL_STENCIL_BUFFER_BIT, NULL);

   SetShaderVerifyFunctions(iface);
}

GGLInterface * CreateGGLInterface()
{
   GGLContext * const ctx = (GGLContext *)calloc(1, sizeof(GGLContext));
   if (!ctx)
      return NULL;
   assert((void *)ctx == (void *)&ctx->interface);

   //_glapi_set_context(ctx->glCtx);
   //_mesa_init_constants(&Const);

   puts("InitializeGGLState");
   InitializeGGLState(&ctx->interface);
   return &ctx->interface;
}

void UninitializeGGLState(GGLInterface * iface)
{
#if USE_DUAL_THREAD
   reinterpret_cast<GGLContext *>(iface)->worker.~Worker();
#endif
   DestroyShaderFunctions(iface);

#if USE_LLVM_TEXTURE_SAMPLER
   puts("USE_LLVM_TEXTURE_SAMPLER");
#endif
#if USE_LLVM_SCANLINE
   puts("USE_LLVM_SCANLINE");
#endif
#if USE_LLVM_EXECUTIONENGINE
   puts("USE_LLVM_EXECUTIONENGINE");
#endif
#if USE_DUAL_THREAD
   puts("USE_DUAL_THREAD");
#endif
   hieralloc_report_brief(NULL, stdout);
}

void DestroyGGLInterface(GGLInterface * iface)
{
   GGLContext * const ctx = reinterpret_cast<GGLContext *>(iface);
   UninitializeGGLState(iface);
   free(ctx);
}
