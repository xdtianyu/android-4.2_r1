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

#include "ARM/ARMABCCompilerDriver.h"

namespace {

class ARMABCExpandVAArg : public bcc::ABCExpandVAArgPass {
public:
  virtual const char *getPassName() const {
    return "ARM LLVM va_arg Instruction Expansion Pass";
  }

private:
  // Derivative work from external/clang/lib/CodeGen/TargetInfo.cpp.
  llvm::Value *expandVAArg(llvm::Instruction *pInst) {
    llvm::Type *pty = pInst->getType();
    llvm::Type *ty = pty->getContainedType(0);
    llvm::Value *va_list_addr = pInst->getOperand(0);
    llvm::IRBuilder<> builder(pInst);
    const llvm::TargetData *td = getAnalysisIfAvailable<llvm::TargetData>();

    llvm::Type *bp = llvm::Type::getInt8PtrTy(*mContext);
    llvm::Type *bpp = bp->getPointerTo(0);

    llvm::Value *va_list_addr_bpp =
        builder.CreateBitCast(va_list_addr, bpp, "ap");
    llvm::Value *addr = builder.CreateLoad(va_list_addr_bpp, "ap.cur");
    // Handle address alignment for type alignment > 32 bits.
    uint64_t ty_align = td->getABITypeAlignment(ty);

    if (ty_align > 4) {
      assert((ty_align & (ty_align - 1)) == 0 &&
        "Alignment is not power of 2!");
      llvm::Value *addr_as_int =
        builder.CreatePtrToInt(addr, llvm::Type::getInt32Ty(*mContext));
      addr_as_int = builder.CreateAdd(addr_as_int,
        builder.getInt32(ty_align-1));
      addr_as_int = builder.CreateAnd(addr_as_int,
        builder.getInt32(~(ty_align-1)));
      addr = builder.CreateIntToPtr(addr_as_int, bp);
    }
    llvm::Value *addr_typed = builder.CreateBitCast(addr, pty);

    uint64_t offset = llvm::RoundUpToAlignment(td->getTypeSizeInBits(ty)/8, 4);
    llvm::Value *next_addr = builder.CreateGEP(addr,
      llvm::ConstantInt::get(llvm::Type::getInt32Ty(*mContext), offset),
      "ap.next");
    builder.CreateStore(next_addr, va_list_addr_bpp);
    return addr_typed;
  }

};

} // end anonymous namespace

namespace bcc {

ABCExpandVAArgPass *ARMABCCompilerDriver::createExpandVAArgPass() const {
  return new ARMABCExpandVAArg();
}

} // end namespace bcc
