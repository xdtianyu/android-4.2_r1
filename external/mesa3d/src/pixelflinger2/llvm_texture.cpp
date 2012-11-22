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

#include <stack>

#include "src/pixelflinger2/pixelflinger2.h"

#include <llvm/IRBuilder.h>
#include <llvm/Module.h>

#include "src/pixelflinger2/llvm_helper.h"

using namespace llvm;

// texture data is int pointer to surface (will cast to short for 16bpp), index is linear texel index,
// format is GGLPixelFormat for surface, return type is <4 x i32> rgba
static Value * pointSample(IRBuilder<> & builder, Value * textureData, Value * index, const GGLPixelFormat format)
{
   Value * texel = NULL;
   switch (format) {
   case GGL_PIXEL_FORMAT_RGBA_8888:
      textureData = builder.CreateGEP(textureData, index);
      texel = builder.CreateLoad(textureData, "texel");
      break;
   case GGL_PIXEL_FORMAT_RGBX_8888:
      textureData = builder.CreateGEP(textureData, index);
      texel = builder.CreateLoad(textureData, "texel");
      texel = builder.CreateOr(texel, builder.getInt32(0xff000000));
      break;
   case GGL_PIXEL_FORMAT_RGB_565: {
      textureData = builder.CreateBitCast(textureData, PointerType::get(builder.getInt16Ty(), 0));
      textureData = builder.CreateGEP(textureData, index);
      texel = builder.CreateLoad(textureData, "texel565");
      texel = builder.CreateZExt(texel, Type::getInt32Ty(builder.getContext()));

      Value * b = builder.CreateAnd(texel, builder.getInt32(0x1f));
      b = builder.CreateShl(b, builder.getInt32(3));
      b = builder.CreateOr(b, builder.CreateLShr(b, builder.getInt32(5)));

      Value * g = builder.CreateAnd(texel, builder.getInt32(0x7e0));
      g = builder.CreateShl(g, builder.getInt32(5));
      g = builder.CreateOr(g, builder.CreateLShr(g, builder.getInt32(6)));
      g = builder.CreateAnd(g, builder.getInt32(0xff00));

      Value * r = builder.CreateAnd(texel, builder.getInt32(0xF800));
      r = builder.CreateShl(r, builder.getInt32(8));
      r = builder.CreateOr(r, builder.CreateLShr(r, builder.getInt32(5)));
      r = builder.CreateAnd(r, builder.getInt32(0xff0000));

      texel = builder.CreateOr(r, builder.CreateOr(g, b));
      texel = builder.CreateOr(texel, builder.getInt32(0xff000000), name("texel"));
      break;
   }
   case GGL_PIXEL_FORMAT_A_8: {
      textureData = builder.CreateBitCast(textureData, PointerType::get(builder.getInt8Ty(),0));
      textureData = builder.CreateGEP(textureData, index);
      texel = builder.CreateLoad(textureData, "texel_a8");
      texel = builder.CreateZExt(texel, builder.getInt32Ty());
      texel = builder.CreateShl(texel, builder.getInt32(24));
      break;
   }
   case GGL_PIXEL_FORMAT_L_8: {
      textureData = builder.CreateBitCast(textureData, PointerType::get(builder.getInt8Ty(),0));
      textureData = builder.CreateGEP(textureData, index);
      texel = builder.CreateLoad(textureData, "texel_l8");
      texel = builder.CreateZExt(texel, builder.getInt32Ty());
      texel = builder.CreateOr(texel, builder.CreateShl(texel, 8));
      texel = builder.CreateOr(texel, builder.CreateShl(texel, 8));
      texel = builder.CreateOr(texel, builder.getInt32(0xff000000));
      break;
   }
   case GGL_PIXEL_FORMAT_LA_88: {
      textureData = builder.CreateBitCast(textureData, PointerType::get(builder.getInt16Ty(),0));
      textureData = builder.CreateGEP(textureData, index);
      texel = builder.CreateLoad(textureData, "texel_la8");
      texel = builder.CreateZExt(texel, builder.getInt32Ty());
      Value * alpha = builder.CreateAnd(texel, builder.getInt32(0xff00));
      texel = builder.CreateAnd(texel, builder.getInt32(0xff));
      texel = builder.CreateOr(texel, builder.CreateShl(texel, 8));
      texel = builder.CreateOr(texel, builder.CreateShl(texel, 8));
      texel = builder.CreateOr(texel, builder.CreateShl(alpha, 16));
      break;
   }
   case GGL_PIXEL_FORMAT_UNKNOWN: // usually means texture not set yet
      ALOGD("pf2: pointSample: unknown format, default to 0xffff00ff \n");
      texel = builder.getInt32(0xffff00ff);
      break;
   default:
      assert(0);
      break;
   }
   Value * channels = Constant::getNullValue(intVecType(builder));

//   if (dstDesc && dstDesc->IsInt32Color()) {
//      channels = builder.CreateInsertElement(channels, texel, builder.getInt32(0));
//      channels = builder.CreateBitCast(channels, floatVecType(builder));
//      return channels;
//   } else if (!dstDesc || dstDesc->IsVectorType()) {
   channels = builder.CreateInsertElement(channels, texel, builder.getInt32(0));
   channels = builder.CreateInsertElement(channels, texel, builder.getInt32(1));
   channels = builder.CreateInsertElement(channels, texel, builder.getInt32(2));
   channels = builder.CreateInsertElement(channels, texel, builder.getInt32(3));
//      if (dstDesc && dstDesc->IsVectorType(Fixed8)) {
//         channels = builder.CreateLShr(channels, constIntVec(builder, 0, 8, 16, 24));
//         channels = builder.CreateAnd(channels, constIntVec(builder, 0xff, 0xff, 0xff, 0xff));
//         channels = builder.CreateBitCast(channels, floatVecType(builder));
//      } else if (dstDesc && dstDesc->IsVectorType(Fixed16)) {
//         channels = builder.CreateShl(channels, constIntVec(builder, 8, 0, 0, 0));
//         channels = builder.CreateLShr(channels, constIntVec(builder, 0, 0, 8, 16));
//         channels = builder.CreateAnd(channels, constIntVec(builder, 0xff00, 0xff00, 0xff00, 0xff00));
//         channels = builder.CreateBitCast(channels, floatVecType(builder));
//      } else if (!dstDesc || dstDesc->IsVectorType(Float)) { // no analysis done in vertex shader, so use default float [0,1] output
   channels = builder.CreateLShr(channels, constIntVec(builder, 0, 8, 16, 24));
   channels = builder.CreateAnd(channels, constIntVec(builder, 0xff, 0xff, 0xff, 0xff));
//   channels = builder.CreateUIToFP(channels, floatVecType(builder));
//   channels = builder.CreateFMul(channels, constFloatVec(builder, 1 / 255.0f,  1 / 255.0f,
//                                 1 / 255.0f, 1 / 255.0f));
//      } else
//         assert(0);
//   } else
//      assert(0);

   return channels;
}

static const unsigned SHIFT = 16;

// w  = width - 1, h = height - 1; similar to pointSample; returns <4 x i32> rgba
static Value * linearSample(IRBuilder<> & builder, Value * textureData, Value * indexOffset,
                            Value * x0, Value * y0, Value * xLerp, Value * yLerp,
                            Value * w, Value * h,  Value * width, Value * height,
                            const GGLPixelFormat format/*, const RegDesc * dstDesc*/)
{
   // TODO: linear filtering needs to be fixed for texcoord outside of [0,1]
   Value * x1 = builder.CreateAdd(x0, builder.getInt32(1));
   x1 = minIntScalar(builder, x1, w);
   Value * y1 = builder.CreateAdd(y0, builder.getInt32(1));
   y1 = minIntScalar(builder, y1, h);

//   RegDesc regDesc;
//   regDesc.SetVectorType(Fixed8);

   Value * index = builder.CreateMul(y0, width);
   index = builder.CreateAdd(index, x0);
   index = builder.CreateAdd(index, indexOffset);
   Value * s0 = pointSample(builder, textureData, index, format/*, &regDesc*/);
//   s0 = builder.CreateBitCast(s0, intVecType(builder));

   index = builder.CreateMul(y0, width);
   index = builder.CreateAdd(index, x1);
   index = builder.CreateAdd(index, indexOffset);
   Value * s1 = pointSample(builder, textureData, index, format/*, &regDesc*/);
//   s1 = builder.CreateBitCast(s1, intVecType(builder));

   index = builder.CreateMul(y1, width);
   index = builder.CreateAdd(index, x1);
   index = builder.CreateAdd(index, indexOffset);
   Value * s2 = pointSample(builder, textureData, index, format/*, &regDesc*/);
//   s2 = builder.CreateBitCast(s2, intVecType(builder));

   index = builder.CreateMul(y1, width);
   index = builder.CreateAdd(index, x0);
   index = builder.CreateAdd(index, indexOffset);
   Value * s3 = pointSample(builder, textureData, index, format/*, &regDesc*/);
//   s3 = builder.CreateBitCast(s3, intVecType(builder));

   Value * xLerpVec = intVec(builder, xLerp, xLerp, xLerp, xLerp);

   Value * h0 = builder.CreateMul(builder.CreateSub(s1, s0), xLerpVec);
   // arithmetic shift right, since it's the result of subtraction, which could be negative
   h0 = builder.CreateAShr(h0, constIntVec(builder, SHIFT, SHIFT, SHIFT, SHIFT));
   h0 = builder.CreateAdd(h0, s0);

   Value * h1 = builder.CreateMul(builder.CreateSub(s2, s3), xLerpVec);
   h1 = builder.CreateAShr(h1, constIntVec(builder, SHIFT, SHIFT, SHIFT, SHIFT));
   h1 = builder.CreateAdd(h1, s3);

   Value * sample = builder.CreateMul(builder.CreateSub(h1, h0),
                                      intVec(builder, yLerp, yLerp, yLerp, yLerp));
   sample = builder.CreateAShr(sample, constIntVec(builder, SHIFT, SHIFT, SHIFT, SHIFT));
   sample = builder.CreateAdd(sample, h0);

   return sample;
//   if (!dstDesc || dstDesc->IsVectorType(Float)) {
//      sample = builder.CreateUIToFP(sample, floatVecType(builder));
//      return builder.CreateFMul(sample, constFloatVec(builder, 1 / 255.0f,  1 / 255.0f,
//                                1 / 255.0f, 1 / 255.0f));
//   } else if (dstDesc && dstDesc->IsVectorType(Fixed16)) {
//      sample = builder.CreateShl(sample, constIntVec(builder, 8, 8, 8, 8));
//      return builder.CreateBitCast(sample, floatVecType(builder));
//   } else if (dstDesc && dstDesc->IsVectorType(Fixed8))
//      return builder.CreateBitCast(sample, floatVecType(builder));
//   else if (dstDesc && dstDesc->IsInt32Color()) {
//      sample = builder.CreateShl(sample, constIntVec(builder, 0, 8, 16, 24));
//      std::vector<llvm::Value*> samples = extractVector(sample);
//      samples[0] = builder.CreateOr(samples[0], samples[1]);
//      samples[0] = builder.CreateOr(samples[0], samples[2]);
//      samples[0] = builder.CreateOr(samples[0], samples[3]);
//      sample = builder.CreateInsertElement(sample, samples[0], builder.getInt32(0));
//      return builder.CreateBitCast(sample, floatVecType(builder));
//   } else
//      assert(0);
}

// dim is size - 1, since [0.0f,1.0f]->[0, size - 1]
static Value * texcoordWrap(IRBuilder<> & builder, const unsigned wrap,
                            /*const ChannelType type,*/ Value * r, Value * size, Value * dim,
                            Value ** texelLerp)
{
   Type * intType = Type::getInt32Ty(builder.getContext());
   Value * tc = NULL;
   Value * odd = NULL;
//   if (Float == type) {
   // convert float to fixed16 so that 16LSB are the remainder, and bit 16 is one
   // mantissa is the amount between two texels, used for linear interpolation
   tc = ConstantFP::get(builder.getContext(), APFloat(float(1 << SHIFT)));
   tc = builder.CreateFMul(tc, r);
   tc = builder.CreateFPToSI(tc, intType);
//   } else if (Fixed16 == type) {
//      assert(16 == SHIFT);
//      tc = builder.CreateBitCast(r, Type::getInt32Ty(builder.getContext()));
//   } else
//      assert(0);

   odd = builder.CreateAnd(tc, builder.getInt32(1 << SHIFT), name("tc_odd"));

   if (0 == wrap || 2 == wrap) // just the mantissa for wrap and mirrored
      tc = builder.CreateAnd(tc, builder.getInt32((1 << SHIFT) - 1));

   tc = builder.CreateMul(tc, dim);

   *texelLerp = builder.CreateAnd(tc, builder.getInt32((1 << SHIFT) - 1));

   tc = builder.CreateLShr(tc, builder.getInt32(SHIFT));

   if (0 == wrap) // GL_REPEAT
   { } else if (1 == wrap) { // GL_CLAMP_TO_EDGE
      tc = maxIntScalar(builder, tc, builder.getInt32(0));
      tc = minIntScalar(builder, tc, dim);
   } else if (2 == wrap) { // GL_MIRRORER_REPEAT
      Value * tcPtr = builder.CreateAlloca(intType);
      builder.CreateStore(tc, tcPtr);
      odd = builder.CreateICmpNE(odd, builder.getInt32(0));

      CondBranch condBranch(builder);
      condBranch.ifCond(odd);

      tc = builder.CreateSub(dim, tc, name("tc_mirrored"));
      builder.CreateStore(tc, tcPtr);

      condBranch.endif();

      tc = builder.CreateLoad(tcPtr);
   } else
      assert(0);

   return tc;
}

Value * tex2D(IRBuilder<> & builder, Value * in1, const unsigned sampler,
              /*const RegDesc * in1Desc, const RegDesc * dstDesc,*/
              const GGLState * gglCtx)
{
   Type * intType = builder.getInt32Ty();
   PointerType * intPointerType = PointerType::get(intType, 0);

   llvm::Module * module = builder.GetInsertBlock()->getParent()->getParent();
   std::vector<Value * > texcoords = extractVector(builder, in1);

   Value * textureDimensions = module->getGlobalVariable(_PF2_TEXTURE_DIMENSIONS_NAME_);
   if (!textureDimensions)
      textureDimensions = new GlobalVariable(*module, intType, true,
                                             GlobalValue::ExternalLinkage,
                                             NULL, _PF2_TEXTURE_DIMENSIONS_NAME_);
   Value * textureWidth = builder.CreateConstInBoundsGEP1_32(textureDimensions,
                          sampler * 2);
   textureWidth = builder.CreateLoad(textureWidth, name("textureWidth"));
   Value * textureHeight = builder.CreateConstInBoundsGEP1_32(textureDimensions,
                           sampler * 2 + 1);
   textureHeight = builder.CreateLoad(textureHeight, name("textureHeight"));
   Value * textureW = builder.CreateSub(textureWidth, builder.getInt32(1));
   Value * textureH = builder.CreateSub(textureHeight, builder.getInt32(1));
//   ChannelType sType = Float, tType = Float;
//   if (in1Desc) {
//      sType = in1Desc->channels[0];
//      tType = in1Desc->channels[1];
//   }

   Value * xLerp = NULL, * yLerp = NULL;
   Value * x = texcoordWrap(builder, gglCtx->textureState.textures[sampler].wrapS,
                            /*sType, */texcoords[0], textureWidth, textureW, &xLerp);
   Value * y = texcoordWrap(builder, gglCtx->textureState.textures[sampler].wrapT,
                            /*tType, */texcoords[1], textureHeight, textureH, &yLerp);

   Value * index = builder.CreateMul(y, textureWidth);
   index = builder.CreateAdd(index, x);

   Value * textureData = module->getGlobalVariable(_PF2_TEXTURE_DATA_NAME_);
   if (!textureData)
      textureData = new GlobalVariable(*module, intPointerType,
                                       true, GlobalValue::ExternalLinkage,
                                       NULL, _PF2_TEXTURE_DATA_NAME_);

   textureData = builder.CreateConstInBoundsGEP1_32(textureData, sampler);
   textureData = builder.CreateLoad(textureData);

   if (0 == gglCtx->textureState.textures[sampler].minFilter &&
         0 == gglCtx->textureState.textures[sampler].magFilter) { // GL_NEAREST
      Value * ret = pointSample(builder, textureData, index,
                                gglCtx->textureState.textures[sampler].format/*, dstDesc*/);
      return intColorVecToFloatColorVec(builder, ret);
   } else if (1 == gglCtx->textureState.textures[sampler].minFilter &&
              1 == gglCtx->textureState.textures[sampler].magFilter) { // GL_LINEAR
      Value * ret = linearSample(builder, textureData, builder.getInt32(0), x, y, xLerp, yLerp,
                                 textureW, textureH,  textureWidth, textureHeight,
                                 gglCtx->textureState.textures[sampler].format/*, dstDesc*/);
      return intColorVecToFloatColorVec(builder, ret);
   } else
      assert(!"unsupported texture filter");
   return NULL;
}

// only positive float; used in cube map since major axis is positive
static Value * FCmpGT(IRBuilder<> & builder, Value * lhs, Value * rhs)
{
   Type * const intType = Type::getInt32Ty(builder.getContext());
   lhs = builder.CreateBitCast(lhs, intType);
   rhs = builder.CreateBitCast(rhs, intType);
   return builder.CreateICmpUGT(lhs, rhs);
}

static Value * FPositive(IRBuilder<> & builder, Value * val)
{
   // float cmp faster here
   return builder.CreateFCmpOGE(val, Constant::getNullValue(builder.getFloatTy()));
   //val = builder.CreateBitCast(val, Type::getInt32Ty(builder.getContext()));
   //return builder.CreateICmpSGE(val, storage->constantInt(0));
   //val = builder.CreateAnd(val, storage->constantInt(0x80000000));
   //return builder.CreateICmpNE(val, storage->constantInt(0));
}

static Value * Fabs(IRBuilder<> & builder, Value * val)
{
   val = builder.CreateBitCast(val, builder.getInt32Ty());
   val = builder.CreateAnd(val, builder.getInt32(~0x80000000));
   return builder.CreateBitCast(val, builder.getFloatTy());
   //return builder.CreateICmpSGE(val, storage->constantInt(0));
}

Value * texCube(IRBuilder<> & builder, Value * in1, const unsigned sampler,
                /*const RegDesc * in1Desc, const RegDesc * dstDesc,*/
                const GGLState * gglCtx)
{
//   if (in1Desc) // the major axis determination code is only float for now
//      assert(in1Desc->IsVectorType(Float));

   Type * const intType = builder.getInt32Ty();
   PointerType * const intPointerType = PointerType::get(intType, 0);
   Type * const floatType = builder.getFloatTy();

   Constant * const float1 = constFloat(builder, 1.0f);
   Constant * const float0_5 = constFloat(builder, 0.5f);

   Module * module = builder.GetInsertBlock()->getParent()->getParent();
   std::vector<Value * > texcoords = extractVector(builder, in1);

   Value * textureDimensions = module->getGlobalVariable(_PF2_TEXTURE_DIMENSIONS_NAME_);
   if (!textureDimensions)
      textureDimensions = new GlobalVariable(*module, intType, true,
                                             GlobalValue::ExternalLinkage,
                                             NULL, _PF2_TEXTURE_DIMENSIONS_NAME_);
   Value * textureWidth = builder.CreateConstInBoundsGEP1_32(textureDimensions,
                          sampler * 2);
   textureWidth = builder.CreateLoad(textureWidth, name("textureWidth"));
   Value * textureHeight = builder.CreateConstInBoundsGEP1_32(textureDimensions,
                           sampler * 2 + 1);
   textureHeight = builder.CreateLoad(textureHeight, name("textureHeight"));
   Value * textureW = builder.CreateSub(textureWidth, builder.getInt32(1));
   Value * textureH = builder.CreateSub(textureHeight, builder.getInt32(1));

   Value * mx = Fabs(builder, texcoords[0]), * my = Fabs(builder, texcoords[1]);
   Value * mz = Fabs(builder, texcoords[2]);
   Value * sPtr = builder.CreateAlloca(floatType);
   Value * tPtr = builder.CreateAlloca(floatType);
   Value * maPtr = builder.CreateAlloca(floatType);
   Value * facePtr = builder.CreateAlloca(intType);

   Value * mxGmyCmp = FCmpGT(builder, mx, my);
   Value * mxGmzCmp = FCmpGT(builder, mx, mz);

   CondBranch condBranch(builder);
   condBranch.ifCond(builder.CreateAnd(mxGmyCmp, mxGmzCmp)); // if (mx > my && mx > mz)
//   m_storage->setCurrentBlock(currentBlock(), false);
   {
      condBranch.ifCond(FPositive(builder, texcoords[0]));
//      m_storage->setCurrentBlock(currentBlock(), false);
      {
         builder.CreateStore(builder.CreateFNeg(texcoords[2]), sPtr);
         builder.CreateStore(builder.CreateFNeg(texcoords[1]), tPtr);
         builder.CreateStore(builder.getInt32(0), facePtr);
      }
      condBranch.elseop();
//      m_storage->setCurrentBlock(currentBlock(), false);
      {
         builder.CreateStore((texcoords[2]), sPtr);
         builder.CreateStore(builder.CreateFNeg(texcoords[1]), tPtr);
         builder.CreateStore(builder.getInt32(1), facePtr);
      }
      condBranch.endif(); // end if (x >= 0)
//      m_storage->setCurrentBlock(currentBlock(), false);

      builder.CreateStore(mx, maPtr);
   }
   condBranch.elseop(); // !(mx > my && mx > mz)
//   m_storage->setCurrentBlock(currentBlock(), false);
   {
      Value * myGmxCmp = FCmpGT(builder, my, mx);
      Value * myGmzCmp = FCmpGT(builder, my, mz);
      condBranch.ifCond(builder.CreateAnd(myGmxCmp, myGmzCmp)); // my > mx && my > mz
//      m_storage->setCurrentBlock(currentBlock(), false);
      {
         condBranch.ifCond(FPositive(builder, texcoords[1]));
//         m_storage->setCurrentBlock(currentBlock(), false);
         {
            builder.CreateStore((texcoords[0]), sPtr);
            builder.CreateStore((texcoords[2]), tPtr);
            builder.CreateStore(builder.getInt32(2), facePtr);
         }
         condBranch.elseop();
//         m_storage->setCurrentBlock(currentBlock(), false);
         {
            builder.CreateStore(texcoords[0], sPtr);
            builder.CreateStore(builder.CreateFNeg(texcoords[2]), tPtr);
            builder.CreateStore(builder.getInt32(3), facePtr);
         }
         condBranch.endif();
//         m_storage->setCurrentBlock(currentBlock(), false);

         builder.CreateStore(my, maPtr);
      }
      condBranch.elseop(); // !(my > mx && my > mz)
//      m_storage->setCurrentBlock(currentBlock(), false);
      {
         //ifCond(builder.CreateFCmpOGE(texcoords[2], float0, name("zPositive")));
         condBranch.ifCond(FPositive(builder, texcoords[2]));
//         m_storage->setCurrentBlock(currentBlock(), false);
         {
            builder.CreateStore((texcoords[0]), sPtr);
            builder.CreateStore(builder.CreateFNeg(texcoords[1]), tPtr);
            builder.CreateStore(builder.getInt32(4), facePtr);
         }
         condBranch.elseop();
//        m_storage->setCurrentBlock(currentBlock(), false);
         {
            builder.CreateStore(builder.CreateFNeg(texcoords[0]), sPtr);
            builder.CreateStore(builder.CreateFNeg(texcoords[1]), tPtr);
            builder.CreateStore(builder.getInt32(5), facePtr);
         }
         condBranch.endif(); // end if (x >= 0)
//         m_storage->setCurrentBlock(currentBlock(), false);

         builder.CreateStore(mz, maPtr);
      }
      condBranch.endif();
//      m_storage->setCurrentBlock(currentBlock(), false);
   }
   condBranch.endif();
//   m_storage->setCurrentBlock(currentBlock(), false);


   Value * s = builder.CreateLoad(sPtr);
   Value * t = builder.CreateLoad(tPtr);
   Value * ma = builder.CreateLoad(maPtr);
   Value * face = builder.CreateLoad(facePtr);

   s = builder.CreateFDiv(s, ma);
   s = builder.CreateFAdd(s, float1);
   s = builder.CreateFMul(s, float0_5);

   t = builder.CreateFDiv(t, ma);
   t = builder.CreateFAdd(t, float1);
   t = builder.CreateFMul(t, float0_5);

//   ChannelType sType = Float, tType = Float;
   Value * xLerp = NULL, * yLerp = NULL;
   Value * x = texcoordWrap(builder, gglCtx->textureState.textures[sampler].wrapS,
                            /*sType, */s, textureWidth, textureW, &xLerp);
   Value * y = texcoordWrap(builder, gglCtx->textureState.textures[sampler].wrapT,
                            /*tType, */t, textureHeight, textureH, &yLerp);
   Value * indexOffset = builder.CreateMul(builder.CreateMul(textureHeight, textureWidth), face);
   Value * index = builder.CreateAdd(builder.CreateMul(y, textureWidth), x);

   Value * textureData = module->getGlobalVariable(_PF2_TEXTURE_DATA_NAME_);
   if (!textureData)
      textureData = new GlobalVariable(*module, intPointerType,
                                       true, GlobalValue::ExternalLinkage,
                                       NULL, _PF2_TEXTURE_DATA_NAME_);

   textureData = builder.CreateConstInBoundsGEP1_32(textureData, sampler);
   textureData = builder.CreateLoad(textureData);

   if (0 == gglCtx->textureState.textures[sampler].minFilter &&
         0 == gglCtx->textureState.textures[sampler].magFilter) { // GL_NEAREST
      textureData = pointSample(builder, textureData, builder.CreateAdd(indexOffset, index),
                                gglCtx->textureState.textures[sampler].format/*, dstDesc*/);
      return intColorVecToFloatColorVec(builder, textureData);

   } else if (1 == gglCtx->textureState.textures[sampler].minFilter &&
              1 == gglCtx->textureState.textures[sampler].magFilter) { // GL_LINEAR
      textureData = linearSample(builder, textureData, indexOffset, x, y, xLerp, yLerp,
                                 textureW, textureH,  textureWidth, textureHeight,
                                 gglCtx->textureState.textures[sampler].format/*, dstDesc*/);
      return intColorVecToFloatColorVec(builder, textureData);
   } else
      assert(!"unsupported texture filter");
   return NULL;
}
