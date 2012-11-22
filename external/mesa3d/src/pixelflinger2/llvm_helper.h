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

#ifndef _PIXELFLINGER2_LLVM_HELPER_H_
#define _PIXELFLINGER2_LLVM_HELPER_H_

#include <stack>
#include <stdio.h>
#include <llvm/IRBuilder.h>

using namespace llvm;

static const char * name(const char * str)
{
   return str;
}

static Value * minIntScalar(IRBuilder<> &builder, Value * in1, Value * in2)
{
   Value * cmp = builder.CreateICmpSLT(in1, in2);
   return builder.CreateSelect(cmp, in1, in2);
}

static Value * maxIntScalar(IRBuilder<> &builder, Value * in1, Value * in2)
{
   Value * cmp = builder.CreateICmpSGT(in1, in2);
   return builder.CreateSelect(cmp, in1, in2);
}

static Constant * constFloat(IRBuilder<> & builder, float x)
{
   return ConstantFP::get(builder.getContext(), APFloat(x));
}

static VectorType * intVecType(IRBuilder<> & builder)
{
   return VectorType::get(Type::getInt32Ty(builder.getContext()), 4);
}

static VectorType * floatVecType(IRBuilder<> & builder)
{
   return VectorType::get(Type::getFloatTy(builder.getContext()), 4);
}

static Value * constIntVec(IRBuilder<> & builder, int x, int y, int z, int w)
{
   std::vector<Constant *> vec(4);
   vec[0] = builder.getInt32(x);
   vec[1] = builder.getInt32(y);
   vec[2] = builder.getInt32(z);
   vec[3] = builder.getInt32(w);

   llvm::ArrayRef<llvm::Constant*> ConstantArray(vec);
   return ConstantVector::get(ConstantArray);
}

static Value * intVec(IRBuilder<> & builder, Value * x, Value * y, Value * z, Value * w)
{
   Value * res = Constant::getNullValue(intVecType(builder));
   res = builder.CreateInsertElement(res, x, builder.getInt32(0), name("vecx"));
   res = builder.CreateInsertElement(res, y, builder.getInt32(1), name("vecy"));
   res = builder.CreateInsertElement(res, z, builder.getInt32(2), name("vecz"));
   if (w)
      res = builder.CreateInsertElement(res, w, builder.getInt32(3), name("vecw"));
   return res;
}

static Value * constFloatVec(IRBuilder<> & builder, float x, float y, float z, float w)
{
   std::vector<Constant *> vec(4);
   vec[0] = constFloat(builder, x);
   vec[1] = constFloat(builder, y);
   vec[2] = constFloat(builder, z);
   vec[3] = constFloat(builder, w);

   llvm::ArrayRef<llvm::Constant*> ConstantArray(vec);
   return ConstantVector::get(ConstantArray);
}

static std::vector<Value *> extractVector(IRBuilder<> & builder, Value *vec)
{
   const VectorType * type = (const VectorType *)vec->getType();
   std::vector<Value*> elems(4);
   assert(type->getNumElements() <= 4);
   for (unsigned i = 0; i < type->getNumElements(); i++)
      elems[i] = builder.CreateExtractElement(vec, builder.getInt32(i), name("xtract"));
   return elems;
}

static Value * intVecMax(IRBuilder<> & builder, Value * in1, Value * in2)
{
   std::vector<Value *> vec1 = extractVector(builder, in1);
   std::vector<Value *> vec2 = extractVector(builder, in2);
   for (unsigned i = 0; i < 4; i++) {
      Value * cmp = builder.CreateICmpSGT(vec1[i], vec2[i], name("iVecSelCmp"));
      vec1[i] = builder.CreateSelect(cmp, vec1[i], vec2[i], name("iVecSel"));
   }
   return intVec(builder, vec1[0], vec1[1], vec1[2], vec1[3]);
}

static Value * intVecMin(IRBuilder<> & builder, Value * in1, Value * in2)
{
   std::vector<Value *> vec1 = extractVector(builder, in1);
   std::vector<Value *> vec2 = extractVector(builder, in2);
   for (unsigned i = 0; i < 4; i++) {
      Value * cmp = builder.CreateICmpSLT(vec1[i], vec2[i], name("iVecSelCmp"));
      vec1[i] = builder.CreateSelect(cmp, vec1[i], vec2[i], name("iVecSel"));
   }
   return intVec(builder, vec1[0], vec1[1], vec1[2], vec1[3]);
}

// <4 x i32> [0, 255] to <4 x float> [0.0, 1.0]
static Value * intColorVecToFloatColorVec(IRBuilder<> & builder, Value * vec)
{
   vec = builder.CreateUIToFP(vec, floatVecType(builder));
   return builder.CreateFMul(vec, constFloatVec(builder, 1 / 255.0f,  1 / 255.0f,
                             1 / 255.0f, 1 / 255.0f));
}

class CondBranch
{
   IRBuilder<> & m_builder;
   std::stack<BasicBlock *> m_ifStack;

   struct Loop {
      BasicBlock *begin;
      BasicBlock *end;
   };
   std::stack<Loop> m_loopStack;

   CondBranch();

public:
   CondBranch(IRBuilder<> & builder) : m_builder(builder) {}
   ~CondBranch() {
      assert(m_ifStack.empty());
      assert(m_loopStack.empty());
   }

   void ifCond(Value * cmp, const char * trueBlock = "ifT", const char * falseBlock = "ifF") {
      Function * function = m_builder.GetInsertBlock()->getParent();
      BasicBlock * ifthen = BasicBlock::Create(m_builder.getContext(), name(trueBlock), function, NULL);
      BasicBlock * ifend = BasicBlock::Create(m_builder.getContext(), name(falseBlock), function, NULL);
      m_builder.CreateCondBr(cmp, ifthen, ifend);
      m_builder.SetInsertPoint(ifthen);
      m_ifStack.push(ifend);
   }

   void elseop() {
      assert(!m_ifStack.empty());
      BasicBlock *ifend = BasicBlock::Create(m_builder.getContext(), name("else_end"), m_builder.GetInsertBlock()->getParent(),0);
      if (!m_builder.GetInsertBlock()->getTerminator()) // ret void is a block terminator
         m_builder.CreateBr(ifend); // branch is also a block terminator
      else {
         debug_printf("Instructions::elseop block alread has terminator \n");
         m_builder.GetInsertBlock()->getTerminator()->dump();
         assert(0);
      }
      m_builder.SetInsertPoint(m_ifStack.top());
      m_builder.GetInsertBlock()->setName(name("else_then"));
      m_ifStack.pop();
      m_ifStack.push(ifend);
   }

   void endif() {
      assert(!m_ifStack.empty());
      if (!m_builder.GetInsertBlock()->getTerminator()) // ret void is a block terminator
         m_builder.CreateBr(m_ifStack.top()); // branch is also a block terminator
      else {
         debug_printf("Instructions::endif block alread has terminator");
         m_builder.GetInsertBlock()->getTerminator()->dump();
         assert(0);
      }
      m_builder.SetInsertPoint(m_ifStack.top());
      m_ifStack.pop();
   }

   void beginLoop() {
      Function * function = m_builder.GetInsertBlock()->getParent();
      BasicBlock *begin = BasicBlock::Create(m_builder.getContext(), name("loop"), function,0);
      BasicBlock *end = BasicBlock::Create(m_builder.getContext(), name("endloop"), function,0);

      m_builder.CreateBr(begin);
      Loop loop;
      loop.begin = begin;
      loop.end   = end;
      m_builder.SetInsertPoint(begin);
      m_loopStack.push(loop);
   }

   void endLoop() {
      assert(!m_loopStack.empty());
      Loop loop = m_loopStack.top();
      m_builder.CreateBr(loop.begin);
      loop.end->moveAfter(m_builder.GetInsertBlock());
      m_builder.SetInsertPoint(loop.end);
      m_loopStack.pop();
   }

   void brk() {
      assert(!m_loopStack.empty());
      BasicBlock *unr = BasicBlock::Create(m_builder.getContext(), name("unreachable"), m_builder.GetInsertBlock()->getParent(),0);
      m_builder.CreateBr(m_loopStack.top().end);
      m_builder.SetInsertPoint(unr);
   }
};

#endif // #ifndef _PIXELFLINGER2_LLVM_HELPER_H_
