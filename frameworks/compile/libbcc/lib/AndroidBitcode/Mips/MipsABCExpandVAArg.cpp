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

#include <llvm/ADT/Triple.h>
#include <llvm/DerivedTypes.h>
#include <llvm/Function.h>
#include <llvm/Instructions.h>
#include <llvm/IRBuilder.h>
#include <llvm/Module.h>
#include <llvm/Pass.h>
#include <llvm/Type.h>
#include <llvm/Target/TargetData.h>

#include "bcc/AndroidBitcode/ABCExpandVAArgPass.h"

#include "Mips/MipsABCCompilerDriver.h"

namespace {

class MipsABCExpandVAArg : public bcc::ABCExpandVAArgPass {
public:
  virtual const char *getPassName() const {
    return "Mips LLVM va_arg Instruction Expansion Pass";
  }

private:
  // Derivative work from external/clang/lib/CodeGen/TargetInfo.cpp.
  virtual llvm::Value *expandVAArg(llvm::Instruction *pInst) {
    llvm::Type *pty = pInst->getType();
    llvm::Type *ty = pty->getContainedType(0);
    llvm::Value *va_list_addr = pInst->getOperand(0);
    llvm::IRBuilder<> builder(pInst);
    const llvm::TargetData *td = getAnalysisIfAvailable<llvm::TargetData>();

    llvm::Type *bp = llvm::Type::getInt8PtrTy(*mContext);
    llvm::Type *bpp = bp->getPointerTo(0);
    llvm::Value *va_list_addr_bpp = builder.CreateBitCast(va_list_addr,
                                                          bpp, "ap");
    llvm::Value *addr = builder.CreateLoad(va_list_addr_bpp, "ap.cur");
    int64_t type_align = td->getABITypeAlignment(ty);
    llvm::Value *addr_typed;
    llvm::IntegerType *int_ty = llvm::Type::getInt32Ty(*mContext);

    if (type_align > 4) {
      llvm::Value *addr_as_int = builder.CreatePtrToInt(addr, int_ty);
      llvm::Value *inc = llvm::ConstantInt::get(int_ty, type_align - 1);
      llvm::Value *mask = llvm::ConstantInt::get(int_ty, -type_align);
      llvm::Value *add_v = builder.CreateAdd(addr_as_int, inc);
      llvm::Value *and_v = builder.CreateAnd(add_v, mask);
      addr_typed = builder.CreateIntToPtr(and_v, pty);
    }
    else {
      addr_typed = builder.CreateBitCast(addr, pty);
    }

    llvm::Value *aligned_addr = builder.CreateBitCast(addr_typed, bp);
    type_align = std::max((unsigned)type_align, (unsigned) 4);
    uint64_t offset =
      llvm::RoundUpToAlignment(td->getTypeSizeInBits(ty) / 8, type_align);
    llvm::Value *next_addr =
      builder.CreateGEP(aligned_addr, llvm::ConstantInt::get(int_ty, offset),
                        "ap.next");
    builder.CreateStore(next_addr, va_list_addr_bpp);

    return addr_typed;
  }

};

} // end anonymous namespace

namespace bcc {

ABCExpandVAArgPass *MipsABCCompilerDriver::createExpandVAArgPass() const {
  return new MipsABCExpandVAArg();
}

} // end namespace bcc
