/*
 * Copyright 2012, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include "bcc/Assert.h"
#include "bcc/Renderscript/RSTransforms.h"

#include <cstdlib>

#include <llvm/DerivedTypes.h>
#include <llvm/Function.h>
#include <llvm/Instructions.h>
#include <llvm/IRBuilder.h>
#include <llvm/Module.h>
#include <llvm/Pass.h>
#include <llvm/Support/raw_ostream.h>
#include <llvm/Target/TargetData.h>
#include <llvm/Type.h>

#include "bcc/Config/Config.h"
#include "bcc/Renderscript/RSInfo.h"
#include "bcc/Support/Log.h"

using namespace bcc;

namespace {

/* RSForEachExpandPass - This pass operates on functions that are able to be
 * called via rsForEach() or "foreach_<NAME>". We create an inner loop for the
 * ForEach-able function to be invoked over the appropriate data cells of the
 * input/output allocations (adjusting other relevant parameters as we go). We
 * support doing this for any ForEach-able compute kernels. The new function
 * name is the original function name followed by ".expand". Note that we
 * still generate code for the original function.
 */
class RSForEachExpandPass : public llvm::ModulePass {
private:
  static char ID;

  llvm::Module *M;
  llvm::LLVMContext *C;

  const RSInfo::ExportForeachFuncListTy &mFuncs;

  // Turns on optimization of allocation stride values.
  bool mEnableStepOpt;

  uint32_t getRootSignature(llvm::Function *F) {
    const llvm::NamedMDNode *ExportForEachMetadata =
        M->getNamedMetadata("#rs_export_foreach");

    if (!ExportForEachMetadata) {
      llvm::SmallVector<llvm::Type*, 8> RootArgTys;
      for (llvm::Function::arg_iterator B = F->arg_begin(),
                                        E = F->arg_end();
           B != E;
           ++B) {
        RootArgTys.push_back(B->getType());
      }

      // For pre-ICS bitcode, we may not have signature information. In that
      // case, we use the size of the RootArgTys to select the number of
      // arguments.
      return (1 << RootArgTys.size()) - 1;
    }

    if (ExportForEachMetadata->getNumOperands() == 0) {
      return 0;
    }

    bccAssert(ExportForEachMetadata->getNumOperands() > 0);

    // We only handle the case for legacy root() functions here, so this is
    // hard-coded to look at only the first such function.
    llvm::MDNode *SigNode = ExportForEachMetadata->getOperand(0);
    if (SigNode != NULL && SigNode->getNumOperands() == 1) {
      llvm::Value *SigVal = SigNode->getOperand(0);
      if (SigVal->getValueID() == llvm::Value::MDStringVal) {
        llvm::StringRef SigString =
            static_cast<llvm::MDString*>(SigVal)->getString();
        uint32_t Signature = 0;
        if (SigString.getAsInteger(10, Signature)) {
          ALOGE("Non-integer signature value '%s'", SigString.str().c_str());
          return 0;
        }
        return Signature;
      }
    }

    return 0;
  }

  // Get the actual value we should use to step through an allocation.
  // TD - Target Data size/layout information.
  // T - Type of allocation (should be a pointer).
  // OrigStep - Original step increment (root.expand() input from driver).
  llvm::Value *getStepValue(llvm::TargetData *TD, llvm::Type *T,
                            llvm::Value *OrigStep) {
    bccAssert(TD);
    bccAssert(T);
    bccAssert(OrigStep);
    llvm::PointerType *PT = llvm::dyn_cast<llvm::PointerType>(T);
    llvm::Type *VoidPtrTy = llvm::Type::getInt8PtrTy(*C);
    if (mEnableStepOpt && T != VoidPtrTy && PT) {
      llvm::Type *ET = PT->getElementType();
      uint64_t ETSize = TD->getTypeAllocSize(ET);
      llvm::Type *Int32Ty = llvm::Type::getInt32Ty(*C);
      return llvm::ConstantInt::get(Int32Ty, ETSize);
    } else {
      return OrigStep;
    }
  }

  static bool hasIn(uint32_t Signature) {
    return Signature & 0x01;
  }

  static bool hasOut(uint32_t Signature) {
    return Signature & 0x02;
  }

  static bool hasUsrData(uint32_t Signature) {
    return Signature & 0x04;
  }

  static bool hasX(uint32_t Signature) {
    return Signature & 0x08;
  }

  static bool hasY(uint32_t Signature) {
    return Signature & 0x10;
  }

  static bool isKernel(uint32_t Signature) {
    return Signature & 0x20;
  }


public:
  RSForEachExpandPass(const RSInfo::ExportForeachFuncListTy &pForeachFuncs,
                      bool pEnableStepOpt)
      : ModulePass(ID), M(NULL), C(NULL), mFuncs(pForeachFuncs),
        mEnableStepOpt(pEnableStepOpt) {
  }

  /* Performs the actual optimization on a selected function. On success, the
   * Module will contain a new function of the name "<NAME>.expand" that
   * invokes <NAME>() in a loop with the appropriate parameters.
   */
  bool ExpandFunction(llvm::Function *F, uint32_t Signature) {
    ALOGV("Expanding ForEach-able Function %s", F->getName().str().c_str());

    if (!Signature) {
      Signature = getRootSignature(F);
      if (!Signature) {
        // We couldn't determine how to expand this function based on its
        // function signature.
        return false;
      }
    }

    llvm::TargetData TD(M);

    llvm::Type *VoidPtrTy = llvm::Type::getInt8PtrTy(*C);
    llvm::Type *Int32Ty = llvm::Type::getInt32Ty(*C);
    llvm::Type *SizeTy = Int32Ty;

    /* Defined in frameworks/base/libs/rs/rs_hal.h:
     *
     * struct RsForEachStubParamStruct {
     *   const void *in;
     *   void *out;
     *   const void *usr;
     *   size_t usr_len;
     *   uint32_t x;
     *   uint32_t y;
     *   uint32_t z;
     *   uint32_t lod;
     *   enum RsAllocationCubemapFace face;
     *   uint32_t ar[16];
     * };
     */
    llvm::SmallVector<llvm::Type*, 9> StructTys;
    StructTys.push_back(VoidPtrTy);  // const void *in
    StructTys.push_back(VoidPtrTy);  // void *out
    StructTys.push_back(VoidPtrTy);  // const void *usr
    StructTys.push_back(SizeTy);     // size_t usr_len
    StructTys.push_back(Int32Ty);    // uint32_t x
    StructTys.push_back(Int32Ty);    // uint32_t y
    StructTys.push_back(Int32Ty);    // uint32_t z
    StructTys.push_back(Int32Ty);    // uint32_t lod
    StructTys.push_back(Int32Ty);    // enum RsAllocationCubemapFace
    StructTys.push_back(llvm::ArrayType::get(Int32Ty, 16));  // uint32_t ar[16]

    llvm::Type *ForEachStubPtrTy = llvm::StructType::create(
        StructTys, "RsForEachStubParamStruct")->getPointerTo();

    /* Create the function signature for our expanded function.
     * void (const RsForEachStubParamStruct *p, uint32_t x1, uint32_t x2,
     *       uint32_t instep, uint32_t outstep)
     */
    llvm::SmallVector<llvm::Type*, 8> ParamTys;
    ParamTys.push_back(ForEachStubPtrTy);  // const RsForEachStubParamStruct *p
    ParamTys.push_back(Int32Ty);           // uint32_t x1
    ParamTys.push_back(Int32Ty);           // uint32_t x2
    ParamTys.push_back(Int32Ty);           // uint32_t instep
    ParamTys.push_back(Int32Ty);           // uint32_t outstep

    llvm::FunctionType *FT =
        llvm::FunctionType::get(llvm::Type::getVoidTy(*C), ParamTys, false);
    llvm::Function *ExpandedFunc =
        llvm::Function::Create(FT,
                               llvm::GlobalValue::ExternalLinkage,
                               F->getName() + ".expand", M);

    // Create and name the actual arguments to this expanded function.
    llvm::SmallVector<llvm::Argument*, 8> ArgVec;
    for (llvm::Function::arg_iterator B = ExpandedFunc->arg_begin(),
                                      E = ExpandedFunc->arg_end();
         B != E;
         ++B) {
      ArgVec.push_back(B);
    }

    if (ArgVec.size() != 5) {
      ALOGE("Incorrect number of arguments to function: %zu",
            ArgVec.size());
      return false;
    }
    llvm::Value *Arg_p = ArgVec[0];
    llvm::Value *Arg_x1 = ArgVec[1];
    llvm::Value *Arg_x2 = ArgVec[2];
    llvm::Value *Arg_instep = ArgVec[3];
    llvm::Value *Arg_outstep = ArgVec[4];

    Arg_p->setName("p");
    Arg_x1->setName("x1");
    Arg_x2->setName("x2");
    Arg_instep->setName("arg_instep");
    Arg_outstep->setName("arg_outstep");

    llvm::Value *InStep = NULL;
    llvm::Value *OutStep = NULL;

    // Construct the actual function body.
    llvm::BasicBlock *Begin =
        llvm::BasicBlock::Create(*C, "Begin", ExpandedFunc);
    llvm::IRBuilder<> Builder(Begin);

    // uint32_t X = x1;
    llvm::AllocaInst *AX = Builder.CreateAlloca(Int32Ty, 0, "AX");
    Builder.CreateStore(Arg_x1, AX);

    // Collect and construct the arguments for the kernel().
    // Note that we load any loop-invariant arguments before entering the Loop.
    llvm::Function::arg_iterator Args = F->arg_begin();

    llvm::Type *InTy = NULL;
    llvm::AllocaInst *AIn = NULL;
    if (hasIn(Signature)) {
      InTy = Args->getType();
      AIn = Builder.CreateAlloca(InTy, 0, "AIn");
      InStep = getStepValue(&TD, InTy, Arg_instep);
      InStep->setName("instep");
      Builder.CreateStore(Builder.CreatePointerCast(Builder.CreateLoad(
          Builder.CreateStructGEP(Arg_p, 0)), InTy), AIn);
      Args++;
    }

    llvm::Type *OutTy = NULL;
    llvm::AllocaInst *AOut = NULL;
    if (hasOut(Signature)) {
      OutTy = Args->getType();
      AOut = Builder.CreateAlloca(OutTy, 0, "AOut");
      OutStep = getStepValue(&TD, OutTy, Arg_outstep);
      OutStep->setName("outstep");
      Builder.CreateStore(Builder.CreatePointerCast(Builder.CreateLoad(
          Builder.CreateStructGEP(Arg_p, 1)), OutTy), AOut);
      Args++;
    }

    llvm::Value *UsrData = NULL;
    if (hasUsrData(Signature)) {
      llvm::Type *UsrDataTy = Args->getType();
      UsrData = Builder.CreatePointerCast(Builder.CreateLoad(
          Builder.CreateStructGEP(Arg_p, 2)), UsrDataTy);
      UsrData->setName("UsrData");
      Args++;
    }

    if (hasX(Signature)) {
      Args++;
    }

    llvm::Value *Y = NULL;
    if (hasY(Signature)) {
      Y = Builder.CreateLoad(Builder.CreateStructGEP(Arg_p, 5), "Y");
      Args++;
    }

    bccAssert(Args == F->arg_end());

    llvm::BasicBlock *Loop = llvm::BasicBlock::Create(*C, "Loop", ExpandedFunc);
    llvm::BasicBlock *Exit = llvm::BasicBlock::Create(*C, "Exit", ExpandedFunc);

    // if (x1 < x2) goto Loop; else goto Exit;
    llvm::Value *Cond = Builder.CreateICmpSLT(Arg_x1, Arg_x2);
    Builder.CreateCondBr(Cond, Loop, Exit);

    // Loop:
    Builder.SetInsertPoint(Loop);

    // Populate the actual call to kernel().
    llvm::SmallVector<llvm::Value*, 8> RootArgs;

    llvm::Value *InPtr = NULL;
    llvm::Value *OutPtr = NULL;

    if (AIn) {
      InPtr = Builder.CreateLoad(AIn, "InPtr");
      RootArgs.push_back(InPtr);
    }

    if (AOut) {
      OutPtr = Builder.CreateLoad(AOut, "OutPtr");
      RootArgs.push_back(OutPtr);
    }

    if (UsrData) {
      RootArgs.push_back(UsrData);
    }

    // We always have to load X, since it is used to iterate through the loop.
    llvm::Value *X = Builder.CreateLoad(AX, "X");
    if (hasX(Signature)) {
      RootArgs.push_back(X);
    }

    if (Y) {
      RootArgs.push_back(Y);
    }

    Builder.CreateCall(F, RootArgs);

    if (InPtr) {
      // InPtr += instep
      llvm::Value *NewIn = Builder.CreateIntToPtr(Builder.CreateNUWAdd(
          Builder.CreatePtrToInt(InPtr, Int32Ty), InStep), InTy);
      Builder.CreateStore(NewIn, AIn);
    }

    if (OutPtr) {
      // OutPtr += outstep
      llvm::Value *NewOut = Builder.CreateIntToPtr(Builder.CreateNUWAdd(
          Builder.CreatePtrToInt(OutPtr, Int32Ty), OutStep), OutTy);
      Builder.CreateStore(NewOut, AOut);
    }

    // X++;
    llvm::Value *XPlusOne =
        Builder.CreateNUWAdd(X, llvm::ConstantInt::get(Int32Ty, 1));
    Builder.CreateStore(XPlusOne, AX);

    // If (X < x2) goto Loop; else goto Exit;
    Cond = Builder.CreateICmpSLT(XPlusOne, Arg_x2);
    Builder.CreateCondBr(Cond, Loop, Exit);

    // Exit:
    Builder.SetInsertPoint(Exit);
    Builder.CreateRetVoid();

    return true;
  }

  /* Expand a pass-by-value kernel.
   */
  bool ExpandKernel(llvm::Function *F, uint32_t Signature) {
    bccAssert(isKernel(Signature));
    ALOGV("Expanding kernel Function %s", F->getName().str().c_str());

    // TODO: Refactor this to share functionality with ExpandFunction.
    llvm::TargetData TD(M);

    llvm::Type *VoidPtrTy = llvm::Type::getInt8PtrTy(*C);
    llvm::Type *Int32Ty = llvm::Type::getInt32Ty(*C);
    llvm::Type *SizeTy = Int32Ty;

    /* Defined in frameworks/base/libs/rs/rs_hal.h:
     *
     * struct RsForEachStubParamStruct {
     *   const void *in;
     *   void *out;
     *   const void *usr;
     *   size_t usr_len;
     *   uint32_t x;
     *   uint32_t y;
     *   uint32_t z;
     *   uint32_t lod;
     *   enum RsAllocationCubemapFace face;
     *   uint32_t ar[16];
     * };
     */
    llvm::SmallVector<llvm::Type*, 9> StructTys;
    StructTys.push_back(VoidPtrTy);  // const void *in
    StructTys.push_back(VoidPtrTy);  // void *out
    StructTys.push_back(VoidPtrTy);  // const void *usr
    StructTys.push_back(SizeTy);     // size_t usr_len
    StructTys.push_back(Int32Ty);    // uint32_t x
    StructTys.push_back(Int32Ty);    // uint32_t y
    StructTys.push_back(Int32Ty);    // uint32_t z
    StructTys.push_back(Int32Ty);    // uint32_t lod
    StructTys.push_back(Int32Ty);    // enum RsAllocationCubemapFace
    StructTys.push_back(llvm::ArrayType::get(Int32Ty, 16));  // uint32_t ar[16]

    llvm::Type *ForEachStubPtrTy = llvm::StructType::create(
        StructTys, "RsForEachStubParamStruct")->getPointerTo();

    /* Create the function signature for our expanded function.
     * void (const RsForEachStubParamStruct *p, uint32_t x1, uint32_t x2,
     *       uint32_t instep, uint32_t outstep)
     */
    llvm::SmallVector<llvm::Type*, 8> ParamTys;
    ParamTys.push_back(ForEachStubPtrTy);  // const RsForEachStubParamStruct *p
    ParamTys.push_back(Int32Ty);           // uint32_t x1
    ParamTys.push_back(Int32Ty);           // uint32_t x2
    ParamTys.push_back(Int32Ty);           // uint32_t instep
    ParamTys.push_back(Int32Ty);           // uint32_t outstep

    llvm::FunctionType *FT =
        llvm::FunctionType::get(llvm::Type::getVoidTy(*C), ParamTys, false);
    llvm::Function *ExpandedFunc =
        llvm::Function::Create(FT,
                               llvm::GlobalValue::ExternalLinkage,
                               F->getName() + ".expand", M);

    // Create and name the actual arguments to this expanded function.
    llvm::SmallVector<llvm::Argument*, 8> ArgVec;
    for (llvm::Function::arg_iterator B = ExpandedFunc->arg_begin(),
                                      E = ExpandedFunc->arg_end();
         B != E;
         ++B) {
      ArgVec.push_back(B);
    }

    if (ArgVec.size() != 5) {
      ALOGE("Incorrect number of arguments to function: %zu",
            ArgVec.size());
      return false;
    }
    llvm::Value *Arg_p = ArgVec[0];
    llvm::Value *Arg_x1 = ArgVec[1];
    llvm::Value *Arg_x2 = ArgVec[2];
    llvm::Value *Arg_instep = ArgVec[3];
    llvm::Value *Arg_outstep = ArgVec[4];

    Arg_p->setName("p");
    Arg_x1->setName("x1");
    Arg_x2->setName("x2");
    Arg_instep->setName("arg_instep");
    Arg_outstep->setName("arg_outstep");

    llvm::Value *InStep = NULL;
    llvm::Value *OutStep = NULL;

    // Construct the actual function body.
    llvm::BasicBlock *Begin =
        llvm::BasicBlock::Create(*C, "Begin", ExpandedFunc);
    llvm::IRBuilder<> Builder(Begin);

    // uint32_t X = x1;
    llvm::AllocaInst *AX = Builder.CreateAlloca(Int32Ty, 0, "AX");
    Builder.CreateStore(Arg_x1, AX);

    // Collect and construct the arguments for the kernel().
    // Note that we load any loop-invariant arguments before entering the Loop.
    llvm::Function::arg_iterator Args = F->arg_begin();

    llvm::Type *OutTy = NULL;
    llvm::AllocaInst *AOut = NULL;
    bool PassOutByReference = false;
    if (hasOut(Signature)) {
      llvm::Type *OutBaseTy = F->getReturnType();
      if (OutBaseTy->isVoidTy()) {
        PassOutByReference = true;
        OutTy = Args->getType();
        Args++;
      } else {
        OutTy = OutBaseTy->getPointerTo();
        // We don't increment Args, since we are using the actual return type.
      }
      AOut = Builder.CreateAlloca(OutTy, 0, "AOut");
      OutStep = getStepValue(&TD, OutTy, Arg_outstep);
      OutStep->setName("outstep");
      Builder.CreateStore(Builder.CreatePointerCast(Builder.CreateLoad(
          Builder.CreateStructGEP(Arg_p, 1)), OutTy), AOut);
    }

    llvm::Type *InBaseTy = NULL;
    llvm::Type *InTy = NULL;
    llvm::AllocaInst *AIn = NULL;
    if (hasIn(Signature)) {
      InBaseTy = Args->getType();
      InTy =InBaseTy->getPointerTo();
      AIn = Builder.CreateAlloca(InTy, 0, "AIn");
      InStep = getStepValue(&TD, InTy, Arg_instep);
      InStep->setName("instep");
      Builder.CreateStore(Builder.CreatePointerCast(Builder.CreateLoad(
          Builder.CreateStructGEP(Arg_p, 0)), InTy), AIn);
      Args++;
    }

    // No usrData parameter on kernels.
    bccAssert(!hasUsrData(Signature));

    if (hasX(Signature)) {
      Args++;
    }

    llvm::Value *Y = NULL;
    if (hasY(Signature)) {
      Y = Builder.CreateLoad(Builder.CreateStructGEP(Arg_p, 5), "Y");
      Args++;
    }

    bccAssert(Args == F->arg_end());

    llvm::BasicBlock *Loop = llvm::BasicBlock::Create(*C, "Loop", ExpandedFunc);
    llvm::BasicBlock *Exit = llvm::BasicBlock::Create(*C, "Exit", ExpandedFunc);

    // if (x1 < x2) goto Loop; else goto Exit;
    llvm::Value *Cond = Builder.CreateICmpSLT(Arg_x1, Arg_x2);
    Builder.CreateCondBr(Cond, Loop, Exit);

    // Loop:
    Builder.SetInsertPoint(Loop);

    // Populate the actual call to kernel().
    llvm::SmallVector<llvm::Value*, 8> RootArgs;

    llvm::Value *InPtr = NULL;
    llvm::Value *In = NULL;
    llvm::Value *OutPtr = NULL;

    if (PassOutByReference) {
      OutPtr = Builder.CreateLoad(AOut, "OutPtr");
      RootArgs.push_back(OutPtr);
    }

    if (AIn) {
      InPtr = Builder.CreateLoad(AIn, "InPtr");
      In = Builder.CreateLoad(InPtr, "In");
      RootArgs.push_back(In);
    }

    // We always have to load X, since it is used to iterate through the loop.
    llvm::Value *X = Builder.CreateLoad(AX, "X");
    if (hasX(Signature)) {
      RootArgs.push_back(X);
    }

    if (Y) {
      RootArgs.push_back(Y);
    }

    llvm::Value *RetVal = Builder.CreateCall(F, RootArgs);

    if (AOut && !PassOutByReference) {
      OutPtr = Builder.CreateLoad(AOut, "OutPtr");
      Builder.CreateStore(RetVal, OutPtr);
    }

    if (InPtr) {
      // InPtr += instep
      llvm::Value *NewIn = Builder.CreateIntToPtr(Builder.CreateNUWAdd(
          Builder.CreatePtrToInt(InPtr, Int32Ty), InStep), InTy);
      Builder.CreateStore(NewIn, AIn);
    }

    if (OutPtr) {
      // OutPtr += outstep
      llvm::Value *NewOut = Builder.CreateIntToPtr(Builder.CreateNUWAdd(
          Builder.CreatePtrToInt(OutPtr, Int32Ty), OutStep), OutTy);
      Builder.CreateStore(NewOut, AOut);
    }

    // X++;
    llvm::Value *XPlusOne =
        Builder.CreateNUWAdd(X, llvm::ConstantInt::get(Int32Ty, 1));
    Builder.CreateStore(XPlusOne, AX);

    // If (X < x2) goto Loop; else goto Exit;
    Cond = Builder.CreateICmpSLT(XPlusOne, Arg_x2);
    Builder.CreateCondBr(Cond, Loop, Exit);

    // Exit:
    Builder.SetInsertPoint(Exit);
    Builder.CreateRetVoid();

    return true;
  }

  virtual bool runOnModule(llvm::Module &M) {
    bool Changed = false;
    this->M = &M;
    C = &M.getContext();

    for (RSInfo::ExportForeachFuncListTy::const_iterator
             func_iter = mFuncs.begin(), func_end = mFuncs.end();
         func_iter != func_end; func_iter++) {
      const char *name = func_iter->first;
      uint32_t signature = func_iter->second;
      llvm::Function *kernel = M.getFunction(name);
      if (kernel && isKernel(signature)) {
        Changed |= ExpandKernel(kernel, signature);
      }
      else if (kernel && kernel->getReturnType()->isVoidTy()) {
        Changed |= ExpandFunction(kernel, signature);
      }
    }

    return Changed;
  }

  virtual const char *getPassName() const {
    return "ForEach-able Function Expansion";
  }

}; // end RSForEachExpandPass

} // end anonymous namespace

char RSForEachExpandPass::ID = 0;

namespace bcc {

llvm::ModulePass *
createRSForEachExpandPass(const RSInfo::ExportForeachFuncListTy &pForeachFuncs,
                          bool pEnableStepOpt){
  return new RSForEachExpandPass(pForeachFuncs, pEnableStepOpt);
}

} // end namespace bcc
