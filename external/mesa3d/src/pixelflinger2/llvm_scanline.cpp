/**
 **
 ** Copyright 2011, The Android Open Source Project
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
#include "src/pixelflinger2/llvm_helper.h"
#include "src/mesa/main/mtypes.h"

#include <llvm/Module.h>

//#undef ALOGD
//#define ALOGD(...)

using namespace llvm;

static void StencilOp(IRBuilder<> &builder, const unsigned char op,
                      Value * sPtr, Value * sRef)
{
   CondBranch condBranch(builder);
   Value * s = builder.CreateLoad(sPtr, "stenciOpS");
   switch (op) {
   case 0 : // GL_ZERO
      builder.CreateStore(builder.getInt8(0), sPtr);
      break;
   case 1 : // GL_KEEP
      builder.CreateStore(s, sPtr);
      break;
   case 2 : // GL_REPLACE
      builder.CreateStore(sRef, sPtr);
      break;
   case 3 : // GL_INCR
      condBranch.ifCond(builder.CreateICmpEQ(s, builder.getInt8(255)));
      builder.CreateStore(s, sPtr);
      condBranch.elseop();
      builder.CreateStore(builder.CreateAdd(s, builder.getInt8(1)), sPtr);
      condBranch.endif();
      break;
   case 4 : // GL_DECR
      condBranch.ifCond(builder.CreateICmpEQ(s, builder.getInt8(0)));
      builder.CreateStore(s, sPtr);
      condBranch.elseop();
      builder.CreateStore(builder.CreateSub(s, builder.getInt8(1)), sPtr);
      condBranch.endif();
      break;
   case 5 : // GL_INVERT
      builder.CreateStore(builder.CreateNot(s), sPtr);
      break;
   case 6 : // GL_INCR_WRAP
      builder.CreateStore(builder.CreateAdd(s, builder.getInt8(1)), sPtr);
      break;
   case 7 : // GL_DECR_WRAP
      builder.CreateStore(builder.CreateSub(s, builder.getInt8(1)), sPtr);
      break;
   default:
      assert(0);
      break;
   }
}

static Value * StencilOp(IRBuilder<> & builder, Value * face,
                         const unsigned char frontOp, const unsigned char backOp,
                         Value * sPtr, Value * sRef)
{
   CondBranch condBranch(builder);
   if (frontOp != backOp)
      condBranch.ifCond(builder.CreateICmpEQ(face, builder.getInt8(0)));

   StencilOp(builder, frontOp, sPtr, sRef);

   if (frontOp != backOp) {
      condBranch.elseop();
      StencilOp(builder, backOp, sPtr, sRef);
      condBranch.endif();
   }
   return builder.CreateLoad(sPtr);
}

static void StencilFunc(IRBuilder<> & builder, const unsigned char func,
                        Value * s, Value * sRef, Value * sCmpPtr)
{
   switch (func) {
   case GL_NEVER & 0x7:
      builder.CreateStore(builder.getFalse(), sCmpPtr);
      break;
   case GL_LESS & 0x7:
      builder.CreateStore(builder.CreateICmpULT(sRef, s), sCmpPtr);
      break;
   case GL_EQUAL & 0x7:
      builder.CreateStore(builder.CreateICmpEQ(sRef, s), sCmpPtr);
      break;
   case GL_LEQUAL & 0x7:
      builder.CreateStore(builder.CreateICmpULE(sRef, s), sCmpPtr);
      break;
   case GL_GREATER & 0x7:
      builder.CreateStore(builder.CreateICmpUGT(sRef, s), sCmpPtr);
      break;
   case GL_NOTEQUAL & 0x7:
      builder.CreateStore(builder.CreateICmpNE(sRef, s), sCmpPtr);
      break;
   case GL_GEQUAL & 0x7:
      builder.CreateStore(builder.CreateICmpUGE(sRef, s), sCmpPtr);
      break;
   case GL_ALWAYS & 0x7:
      builder.CreateStore(builder.getTrue(), sCmpPtr);
      break;
   default:
      assert(0);
      break;
   }
}

static Value * BlendFactor(const unsigned mode, Value * src, Value * dst,
                           Value * constant, Value * one, Value * zero,
                           Value * srcA, Value * dstA, Value * constantA,
                           Value * sOne, const bool isVector, IRBuilder<> & builder)
{
   Value * factor = NULL;
   switch (mode) {
   case GGLBlendState::GGL_ZERO:
      factor = zero;
      break;
   case GGLBlendState::GGL_ONE:
      factor = one;
      break;
   case GGLBlendState::GGL_SRC_COLOR:
      factor = src;
      break;
   case GGLBlendState::GGL_ONE_MINUS_SRC_COLOR:
      factor = builder.CreateSub(one, src);
      break;
   case GGLBlendState::GGL_DST_COLOR:
      factor = dst;
      break;
   case GGLBlendState::GGL_ONE_MINUS_DST_COLOR:
      factor = builder.CreateSub(one, dst);
      break;
   case GGLBlendState::GGL_SRC_ALPHA:
      factor = srcA;
      if (isVector)
         factor = intVec(builder, factor, factor, factor, factor);
      break;
   case GGLBlendState::GGL_ONE_MINUS_SRC_ALPHA:
      factor = builder.CreateSub(sOne, srcA);
      if (isVector)
         factor = intVec(builder, factor, factor, factor, factor);
      break;
   case GGLBlendState::GGL_DST_ALPHA:
      factor = dstA;
      if (isVector)
         factor = intVec(builder, factor, factor, factor, factor);
      break;
   case GGLBlendState::GGL_ONE_MINUS_DST_ALPHA:
      factor = builder.CreateSub(sOne, dstA);
      if (isVector)
         factor = intVec(builder, factor, factor, factor, factor);
      break;
   case GGLBlendState::GGL_SRC_ALPHA_SATURATE:
      // valid only for source color and alpha
      factor = minIntScalar(builder, srcA, builder.CreateSub(sOne, dstA));
      if (isVector)
         factor = intVec(builder, factor, factor, factor, sOne);
      else
         factor = sOne; // when it's used for source alpha, it's just 1
      break;
   case GGLBlendState::GGL_CONSTANT_COLOR:
      factor = constant;
      break;
   case GGLBlendState::GGL_ONE_MINUS_CONSTANT_COLOR:
      factor = builder.CreateSub(one, constant);
      break;
   case GGLBlendState::GGL_CONSTANT_ALPHA:
      factor = constantA;
      if (isVector)
         factor = intVec(builder, factor, factor, factor, factor);
      break;
   case GGLBlendState::GGL_ONE_MINUS_CONSTANT_ALPHA:
      factor = builder.CreateSub(sOne, constantA);
      if (isVector)
         factor = intVec(builder, factor, factor, factor, factor);
      break;
   default:
      assert(0);
      break;
   }
   return factor;
}

static Value * Saturate(IRBuilder<> & builder, Value * intVector)
{
   intVector = intVecMax(builder, intVector, constIntVec(builder, 0,0,0,0));
   return intVecMin(builder, intVector, constIntVec(builder, 255,255,255,255));
}

// src is int32x4 [0,255] rgba vector, and combines them into int32
// RGB_565 channel order is weird
static Value * IntVectorToScreenColor(IRBuilder<> & builder, const GGLPixelFormat format, Value * src)
{
   if (GGL_PIXEL_FORMAT_RGBA_8888 == format) {
      src = builder.CreateShl(src, constIntVec(builder, 0, 8, 16, 24));
      std::vector<Value *> comps = extractVector(builder, src);
      comps[0] = builder.CreateOr(comps[0], comps[1]);
      comps[0] = builder.CreateOr(comps[0], comps[2]);
      comps[0] = builder.CreateOr(comps[0], comps[3]);
      return comps[0];
   } else if (GGL_PIXEL_FORMAT_RGB_565 == format) {
      src = builder.CreateAnd(src, constIntVec(builder, 0xf8, 0xfc, 0xf8, 0));
      std::vector<Value *> comps = extractVector(builder, src);
      // channel order is weird
      for (unsigned i = 0; i < 4; i++)
         comps[i] = builder.CreateTrunc(comps[i], builder.getInt16Ty());
      comps[2] = builder.CreateLShr(comps[2], 3);
      comps[1] = builder.CreateShl(comps[1], 3);
      comps[0] = builder.CreateShl(comps[0], 8);

      comps[0] = builder.CreateOr(comps[0], comps[1]);
      comps[0] = builder.CreateOr(comps[0], comps[2]);
      return comps[0];
   } else if (GGL_PIXEL_FORMAT_UNKNOWN == format)
      return builder.getInt32(0);
   else
      assert(0);
   return NULL;
}

// src is int32 or int16, return is int32x4 [0,255] rgba
// RGB_565 channel order is weird
static Value * ScreenColorToIntVector(IRBuilder<> & builder, const GGLPixelFormat format, Value * src)
{
   src = builder.CreateZExt(src, builder.getInt32Ty());
   Value * dst = intVec(builder, src, src, src, src);
   if (GGL_PIXEL_FORMAT_RGBA_8888 == format) {
      dst = builder.CreateLShr(dst, constIntVec(builder, 0, 8, 16, 24));
      dst = builder.CreateAnd(dst, constIntVec(builder, 0xff, 0xff, 0xff, 0xff));
   } else if (GGL_PIXEL_FORMAT_RGB_565 == format) {
      // channel order is weird
      dst = builder.CreateAnd(dst, constIntVec(builder, 0xf800, 0x7e0, 0x1f, 0));
      dst = builder.CreateLShr(dst, constIntVec(builder, 8, 3, 0, 0));
      dst = builder.CreateShl(dst, constIntVec(builder, 0, 0, 3, 0));
      dst = builder.CreateOr(dst, constIntVec(builder, 0, 0, 0, 0xff));
   } else if (GGL_PIXEL_FORMAT_UNKNOWN == format)
      ALOGD("pf2: ScreenColorToIntVector GGL_PIXEL_FORMAT_UNKNOWN"); // not set yet, do nothing
   else
      assert(0);
   return dst;
}

// src is <4 x float> approx [0,1]; dst is <4 x i32> [0,255] from frame buffer; return is i32
Value * GenerateFSBlend(const GGLState * gglCtx, const GGLPixelFormat format, /*const RegDesc * regDesc,*/
                        IRBuilder<> & builder, Value * src, Value * dst)
{
   Type * const intType = builder.getInt32Ty();

   // TODO cast the outputs pointer type to int for writing to minimize bandwidth
   if (!gglCtx->blendState.enable) {
//        if (regDesc->IsInt32Color())
//        {
//            debug_printf("GenerateFixedFS dst is already scalar fixed0 \n");
//            src = builder.CreateExtractElement(src, builder.getInt32(0));
//            src = builder.CreateBitCast(src, intType); // it's already RGBA int32
//        }
//        else if (regDesc->IsVectorType(Float))
//        {
      src = builder.CreateFMul(src, constFloatVec(builder,255,255,255,255));
      src = builder.CreateFPToSI(src, intVecType(builder));
      src = Saturate(builder, src);
      src = IntVectorToScreenColor(builder, format, src);
//        }
//        else if (regDesc->IsVectorType(Fixed8))
//        {
//            src = builder.CreateBitCast(src, instr->GetIntVectorType());
//            src = Saturate(instr, src);
//            src = IntVectorToColor(instr, storage, src);
//        }
//        else if (regDesc->IsVectorType(Fixed16))
//        {
//            src = builder.CreateBitCast(src, instr->GetIntVectorType());
//            src = builder.CreateAShr(src, constIntVec(builder,8,8,8,8));
//            src = Saturate(instr, src);
//            src = IntVectorToColor(instr, storage, src);
//        }
//        else
//            assert(0);
      return src;
   }
   // blending, so convert src to <4 x i32>
//    if (regDesc->IsInt32Color())
//    {
//        src = builder.CreateExtractElement(src, builder.getInt32(0));
//        src = builder.CreateBitCast(src, intType); // it's already RGBA int32
//
//        Value * channels = Constant::getNullValue(instr->GetIntVectorType());
//        channels = builder.CreateInsertElement(channels, src, builder.getInt32(0));
//        channels = builder.CreateInsertElement(channels, src, builder.getInt32(1));
//        channels = builder.CreateInsertElement(channels, src, builder.getInt32(2));
//        channels = builder.CreateInsertElement(channels, src, builder.getInt32(3));
//        channels = builder.CreateLShr(channels, constIntVec(builder,0, 8, 16, 24));
//        channels = builder.CreateAnd(channels, constIntVec(builder,0xff, 0xff, 0xff, 0xff));
//        src = channels;
//    }
//    else if (regDesc->IsVectorType(Fixed8)) // it's already int32x4 RGBA
//        src = builder.CreateBitCast(src, instr->GetIntVectorType());
//    else if (regDesc->IsVectorType(Fixed16))
//    {
//        src = builder.CreateBitCast(src, instr->GetIntVectorType());
//        // TODO DXL consider shl dst by 8 and ashr by 16 in the end for more precision
//        src = builder.CreateAShr(src, constIntVec(builder,8,8,8,8));
//    }
//    else if (regDesc->IsVectorType(Float))
//    {
   src = builder.CreateFMul(src, constFloatVec(builder,255,255,255,255));
   src = builder.CreateFPToSI(src, intVecType(builder));
//    }
//    else
//        assert(0);

   Value * const one = constIntVec(builder,255,255,255,255);
   Value * const zero = constIntVec(builder,0,0,0,0);
   Value * const sOne = builder.getInt32(255);
   Value * const sZero = builder.getInt32(0);

#if USE_LLVM_SCANLINE
   Value * constant = constIntVec(builder,gglCtx->blendState.color[0],
                                  gglCtx->blendState.color[1],
                                  gglCtx->blendState.color[2],
                                  gglCtx->blendState.color[3]);
#else
   Value * constant = NULL;
   assert(0);
#endif

   Value * srcA = extractVector(builder,src)[3];
   Value * dstA = extractVector(builder,dst)[3];
   Value * constantA = extractVector(builder,constant)[3];

   Value * sf = BlendFactor(gglCtx->blendState.scf, src, dst,
                            constant, one, zero, srcA, dstA,
                            constantA, sOne, true, builder);
   if (gglCtx->blendState.scf != gglCtx->blendState.saf) {
      Value * sfA = BlendFactor(gglCtx->blendState.saf, srcA, dstA,
                                constantA, sOne, sZero, srcA, dstA,
                                constantA, sOne, false, builder);
      sf = builder.CreateInsertElement(sf, sfA, builder.getInt32(3),
                                       name("sfAStore"));
   }

   Value * df = BlendFactor(gglCtx->blendState.dcf, src, dst,
                            constant, one, zero, srcA, dstA,
                            constantA, sOne, true, builder);
   if (gglCtx->blendState.dcf != gglCtx->blendState.daf) {
      Value * dfA = BlendFactor(gglCtx->blendState.daf, srcA, dstA,
                                constantA, sOne, sZero, srcA, dstA,
                                constantA, sOne, false, builder);
      df = builder.CreateInsertElement(df, dfA, builder.getInt32(3),
                                       name("dfAStore"));
   }

   // this is factor *= 256 / 255; factors have a chance of constant folding
   sf = builder.CreateAdd(sf, builder.CreateLShr(sf, constIntVec(builder,7,7,7,7)));
   df = builder.CreateAdd(df, builder.CreateLShr(df, constIntVec(builder,7,7,7,7)));

   src = builder.CreateMul(src, sf);
   dst = builder.CreateMul(dst, df);

   Value * res = NULL;
   switch (gglCtx->blendState.ce + GL_FUNC_ADD) {
   case GL_FUNC_ADD:
      res = builder.CreateAdd(src, dst);
      break;
   case GL_FUNC_SUBTRACT:
      res = builder.CreateSub(src, dst);
      break;
   case GL_FUNC_REVERSE_SUBTRACT:
      res = builder.CreateSub(dst, src);
      break;
   default:
      assert(0);
      break;
   }
   if (gglCtx->blendState.ce != gglCtx->blendState.ae) {
      srcA = extractVector(builder,src)[3];
      dstA = extractVector(builder,dst)[3];
      Value * resA = NULL;
      switch (gglCtx->blendState.ae + GL_FUNC_ADD) {
      case GL_FUNC_ADD:
         resA = builder.CreateAdd(srcA, dstA);
         break;
      case GL_FUNC_SUBTRACT:
         resA = builder.CreateSub(srcA, dstA);
         break;
      case GL_FUNC_REVERSE_SUBTRACT:
         resA = builder.CreateSub(dstA, srcA);
         break;
      default:
         assert(0);
         break;
      }
      res = builder.CreateInsertElement(res, resA, builder.getInt32(3),
                                        name("resAStore"));
   }

   res = builder.CreateAShr(res, constIntVec(builder,8,8,8,8));
   res = Saturate(builder, res);
   res = IntVectorToScreenColor(builder, format, res);
   return res;
}

static FunctionType * ScanLineFunctionType(IRBuilder<> & builder)
{
   std::vector<Type*> funcArgs;
   VectorType * vectorType = floatVecType(builder);
   PointerType * vectorPtr = PointerType::get(vectorType, 0);
   Type * intType = builder.getInt32Ty();
   PointerType * intPointerType = PointerType::get(intType, 0);
   PointerType * bytePointerType = PointerType::get(builder.getInt8Ty(), 0);

   funcArgs.push_back(vectorPtr); // start
   funcArgs.push_back(vectorPtr); // step
   funcArgs.push_back(vectorPtr); // constants
   funcArgs.push_back(intPointerType); // frame
   funcArgs.push_back(intPointerType); // depth
   funcArgs.push_back(bytePointerType); // stencil
   funcArgs.push_back(bytePointerType); // stencil state
   funcArgs.push_back(intType); // count

   FunctionType *functionType = FunctionType::get(/*Result=*/builder.getVoidTy(),
                                                  llvm::ArrayRef<Type*>(funcArgs),
                                                  /*isVarArg=*/false);

   return functionType;
}

// generated scanline function parameters are VertexOutput * start, VertexOutput * step,
// unsigned * frame, int * depth, unsigned char * stencil,
// GGLActiveStencilState * stencilState, unsigned count
void GenerateScanLine(const GGLState * gglCtx, const gl_shader_program * program, Module * mod,
                      const char * shaderName, const char * scanlineName)
{
   IRBuilder<> builder(mod->getContext());
//   debug_printf("GenerateScanLine %s \n", scanlineName);

   Type * intType = builder.getInt32Ty();
   PointerType * intPointerType = PointerType::get(intType, 0);
   Type * byteType = builder.getInt8Ty();
   PointerType * bytePointerType = PointerType::get(byteType, 0);

   Function * func = mod->getFunction(scanlineName);
   if (func)
      return;

   func = llvm::cast<Function>(mod->getOrInsertFunction(scanlineName,
                               ScanLineFunctionType(builder)));

   BasicBlock *label_entry = BasicBlock::Create(builder.getContext(), "entry", func, 0);
   builder.SetInsertPoint(label_entry);
   CondBranch condBranch(builder);

   Function::arg_iterator args = func->arg_begin();
   Value * start = args++;
   start->setName("start");
   Value * step = args++;
   step->setName("step");
   Value * constants = args++;
   constants->setName("constants");

   // need alloc to be able to assign to it by using store
   Value * framePtr = builder.CreateAlloca(intPointerType);
   builder.CreateStore(args++, framePtr);
   Value * depthPtr = builder.CreateAlloca(intPointerType);
   builder.CreateStore(args++, depthPtr);
   Value * stencilPtr = builder.CreateAlloca(bytePointerType);
   builder.CreateStore(args++, stencilPtr);
   Value * stencilState = args++;
   stencilState->setName("stencilState");
   Value * countPtr = builder.CreateAlloca(intType);
   builder.CreateStore(args++, countPtr);

   Value * sFace = NULL, * sRef = NULL, *sMask = NULL, * sFunc = NULL;
   if (gglCtx->bufferState.stencilTest) {
      sFace = builder.CreateLoad(builder.CreateConstInBoundsGEP1_32(stencilState, 0), "sFace");
      if (gglCtx->frontStencil.ref == gglCtx->backStencil.ref)
         sRef = builder.getInt8(gglCtx->frontStencil.ref);
      else
         sRef = builder.CreateLoad(builder.CreateConstInBoundsGEP1_32(stencilState, 1), "sRef");
      if (gglCtx->frontStencil.mask == gglCtx->backStencil.mask)
         sMask = builder.getInt8(gglCtx->frontStencil.mask);
      else
         sMask = builder.CreateLoad(builder.CreateConstInBoundsGEP1_32(stencilState, 2), "sMask");
      if (gglCtx->frontStencil.func == gglCtx->backStencil.func)
         sFunc = builder.getInt8(gglCtx->frontStencil.func);
      else
         sFunc = builder.CreateLoad(builder.CreateConstInBoundsGEP1_32(stencilState, 3), "sFunc");
   }

   condBranch.beginLoop(); // while (count > 0)

   assert(framePtr && gglCtx);
   // get values
   Value * frame = NULL;
   if (GGL_PIXEL_FORMAT_RGBA_8888 == gglCtx->bufferState.colorFormat)
      frame = builder.CreateLoad(framePtr);
   else if (GGL_PIXEL_FORMAT_RGB_565 == gglCtx->bufferState.colorFormat) {
      frame = builder.CreateLoad(framePtr);
      frame = builder.CreateBitCast(frame, PointerType::get(builder.getInt16Ty(), 0));
   } else if (GGL_PIXEL_FORMAT_UNKNOWN == gglCtx->bufferState.colorFormat)
      frame = builder.CreateLoad(framePtr); // color buffer not set yet
   else
      assert(0);

   frame->setName("frame");
   Value * depth = NULL, * stencil = NULL;
   if (gglCtx->bufferState.depthTest) {
      assert(GGL_PIXEL_FORMAT_Z_32 == gglCtx->bufferState.depthFormat);
      depth = builder.CreateLoad(depthPtr);
      depth->setName("depth");
   }

   Value * count = builder.CreateLoad(countPtr);
   count->setName("count");

   Value * cmp = builder.CreateICmpEQ(count, builder.getInt32(0));
   condBranch.ifCond(cmp, "if_break_loop"); // if (count == 0)
   condBranch.brk(); // break;
   condBranch.endif();

   Value * sCmpPtr = NULL, * sCmp = NULL, * sPtr = NULL, * s = NULL;
   if (gglCtx->bufferState.stencilTest) {
      stencil = builder.CreateLoad(stencilPtr);
      stencil->setName("stencil");

      // temporaries to load/store value
      sCmpPtr = builder.CreateAlloca(builder.getInt1Ty());
      sCmpPtr->setName("sCmpPtr");
      sPtr = builder.CreateAlloca(byteType);
      sPtr->setName("sPtr");

      s = builder.CreateLoad(stencil);
      s = builder.CreateAnd(s, sMask);
      builder.CreateStore(s, sPtr);

      if (gglCtx->frontStencil.func != gglCtx->backStencil.func)
         condBranch.ifCond(builder.CreateICmpEQ(sFace, builder.getInt8(0)));

      StencilFunc(builder, gglCtx->frontStencil.func, s, sRef, sCmpPtr);

      if (gglCtx->frontStencil.func != gglCtx->backStencil.func) {
         condBranch.elseop();
         StencilFunc(builder, gglCtx->backStencil.func, s, sRef, sCmpPtr);
         condBranch.endif();
      }

      sCmp = builder.CreateLoad(sCmpPtr);
   } else
      sCmp = ConstantInt::getTrue(mod->getContext());
   sCmp->setName("sCmp");

   Value * depthZ = NULL, * zPtr = NULL, * z = NULL, * zCmp = NULL;
   if (gglCtx->bufferState.depthTest) {
      depthZ  = builder.CreateLoad(depth, "depthZ"); // z stored in buffer
      zPtr = builder.CreateAlloca(intType); // temp store for modifying incoming z
      zPtr->setName("zPtr");

      // modified incoming z
      z = builder.CreateBitCast(start, intPointerType);
      z = builder.CreateConstInBoundsGEP1_32(z, (GGL_FS_INPUT_OFFSET +
                                             GGL_FS_INPUT_FRAGCOORD_INDEX) * 4 + 2);
      z = builder.CreateLoad(z, "z");

      builder.CreateStore(z, zPtr);

      Value * zNegative = builder.CreateICmpSLT(z, builder.getInt32(0));
      condBranch.ifCond(zNegative);
      // if (0x80000000 & z) z ^= 0x7fffffff since smaller -ve float means bigger -ve int
      z = builder.CreateXor(z, builder.getInt32(0x7fffffff));
      builder.CreateStore(z, zPtr);

      condBranch.endif();

      z = builder.CreateLoad(zPtr, "z");

      switch (0x200 | gglCtx->bufferState.depthFunc) {
      case GL_NEVER:
         zCmp = ConstantInt::getFalse(mod->getContext());
         break;
      case GL_LESS:
         zCmp = builder.CreateICmpSLT(z, depthZ);
         break;
      case GL_EQUAL:
         zCmp = builder.CreateICmpEQ(z, depthZ);
         break;
      case GL_LEQUAL:
         zCmp = builder.CreateICmpSLE(z, depthZ);
         break;
      case GL_GREATER:
         zCmp = builder.CreateICmpSGT(z, depthZ);
         break;
      case GL_NOTEQUAL:
         zCmp = builder.CreateICmpNE(z, depthZ);
         break;
      case GL_GEQUAL:
         zCmp = builder.CreateICmpSGE(z, depthZ);
         break;
      case GL_ALWAYS:
         zCmp = ConstantInt::getTrue(mod->getContext());
         break;
      default:
         assert(0);
         break;
      }
   } else // no depth test means always pass
      zCmp = ConstantInt::getTrue(mod->getContext());
   zCmp->setName("zCmp");

   condBranch.ifCond(sCmp, "if_sCmp", "sCmp_fail");
   condBranch.ifCond(zCmp, "if_zCmp", "zCmp_fail");

   Value * inputs = start;
   Value * outputs = start;

   Value * fsOutputs = builder.CreateConstInBoundsGEP1_32(start,
                       offsetof(VertexOutput,fragColor)/sizeof(Vector4));

   Function * fsFunction = mod->getFunction(shaderName);
   assert(fsFunction);
   CallInst *call = builder.CreateCall3(fsFunction,inputs, outputs, constants);
   call->setCallingConv(CallingConv::C);
   call->setTailCall(false);

   Value * dst = Constant::getNullValue(intVecType(builder));
   if (gglCtx->blendState.enable && (0 != gglCtx->blendState.dcf || 0 != gglCtx->blendState.daf)) {
      Value * frameColor = builder.CreateLoad(frame, "frameColor");
      dst = ScreenColorToIntVector(builder, gglCtx->bufferState.colorFormat, frameColor);
   }

   Value * src = builder.CreateConstInBoundsGEP1_32(fsOutputs, 0);
   src = builder.CreateLoad(src);

   Value * color = GenerateFSBlend(gglCtx, gglCtx->bufferState.colorFormat,/*&prog->outputRegDesc,*/ builder, src, dst);
   builder.CreateStore(color, frame);
   // TODO DXL depthmask check
   if (gglCtx->bufferState.depthTest) {
      z = builder.CreateBitCast(z, intType);
      builder.CreateStore(z, depth); // store z
   }

   if (gglCtx->bufferState.stencilTest)
      builder.CreateStore(StencilOp(builder, sFace, gglCtx->frontStencil.dPass,
                                    gglCtx->backStencil.dPass, sPtr, sRef), stencil);

   condBranch.elseop(); // failed z test

   if (gglCtx->bufferState.stencilTest)
      builder.CreateStore(StencilOp(builder, sFace, gglCtx->frontStencil.dFail,
                                    gglCtx->backStencil.dFail, sPtr, sRef), stencil);
   condBranch.endif();
   condBranch.elseop(); // failed s test

   if (gglCtx->bufferState.stencilTest)
      builder.CreateStore(StencilOp(builder, sFace, gglCtx->frontStencil.sFail,
                                    gglCtx->backStencil.sFail, sPtr, sRef), stencil);

   condBranch.endif();
   assert(frame);
   frame = builder.CreateConstInBoundsGEP1_32(frame, 1); // frame++
   // frame may have been casted to short* from int*, so cast back
   frame = builder.CreateBitCast(frame, PointerType::get(builder.getInt32Ty(), 0));
   builder.CreateStore(frame, framePtr);
   if (gglCtx->bufferState.depthTest) {
      depth = builder.CreateConstInBoundsGEP1_32(depth, 1); // depth++
      builder.CreateStore(depth, depthPtr);
   }
   if (gglCtx->bufferState.stencilTest) {
      stencil = builder.CreateConstInBoundsGEP1_32(stencil, 1); // stencil++
      builder.CreateStore(stencil, stencilPtr);
   }
   Value * vPtr = NULL, * v = NULL, * dx = NULL;
   if (program->UsesFragCoord) {
      vPtr = builder.CreateConstInBoundsGEP1_32(start, GGL_FS_INPUT_OFFSET +
             GGL_FS_INPUT_FRAGCOORD_INDEX);
      v = builder.CreateLoad(vPtr);
      dx = builder.CreateConstInBoundsGEP1_32(step, GGL_FS_INPUT_OFFSET +
                                              GGL_FS_INPUT_FRAGCOORD_INDEX);
      dx = builder.CreateLoad(dx);
      v = builder.CreateFAdd(v, dx);
      builder.CreateStore(v, vPtr);
   } else if (gglCtx->bufferState.depthTest) {
      Type * floatType = builder.getFloatTy();
      PointerType * floatPointerType = PointerType::get(floatType, 0);
      vPtr = builder.CreateBitCast(start, floatPointerType);
      vPtr = builder.CreateConstInBoundsGEP1_32(vPtr,
             (GGL_FS_INPUT_OFFSET + GGL_FS_INPUT_FRAGCOORD_INDEX) * 4 + 2);
      v = builder.CreateLoad(vPtr);
      dx = builder.CreateBitCast(step, floatPointerType);
      dx = builder.CreateConstInBoundsGEP1_32(dx,
                                              (GGL_FS_INPUT_OFFSET + GGL_FS_INPUT_FRAGCOORD_INDEX) * 4 + 2);
      dx = builder.CreateLoad(dx);
      v = builder.CreateFAdd(v, dx);
      builder.CreateStore(v, vPtr);
   }

   if (program->UsesPointCoord) {
      vPtr = builder.CreateConstInBoundsGEP1_32(start, GGL_FS_INPUT_OFFSET +
             GGL_FS_INPUT_FRONTFACINGPOINTCOORD_INDEX);
      v = builder.CreateLoad(vPtr);
      dx = builder.CreateConstInBoundsGEP1_32(step, GGL_FS_INPUT_OFFSET +
                                              GGL_FS_INPUT_FRONTFACINGPOINTCOORD_INDEX);
      dx = builder.CreateLoad(dx);
      v = builder.CreateFAdd(v, dx);
      builder.CreateStore(v, vPtr);
   }

   for (unsigned i = 0; i < program->VaryingSlots; ++i) {
      vPtr = builder.CreateConstInBoundsGEP1_32(start, offsetof(VertexOutput,varyings)/sizeof(Vector4) + i);
      v = builder.CreateLoad(vPtr);
      dx = builder.CreateConstInBoundsGEP1_32(step, GGL_FS_INPUT_OFFSET +
                                              GGL_FS_INPUT_VARYINGS_INDEX + i);
      dx = builder.CreateLoad(dx);
      v = builder.CreateFAdd(v, dx);
      builder.CreateStore(v, vPtr);
   }

   count = builder.CreateSub(count, builder.getInt32(1));
   builder.CreateStore(count, countPtr); // count--;

   condBranch.endLoop();

   builder.CreateRetVoid();
}
