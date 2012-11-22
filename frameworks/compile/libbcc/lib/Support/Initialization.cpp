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

#include "bcc/Support/Initialization.h"

#include <cstdlib>

#include <llvm/Support/ErrorHandling.h>
#include <llvm/Support/TargetSelect.h>

#include <mcld/Support/TargetSelect.h>
#include <mcld/Support/TargetRegistry.h>

#include "bcc/Config/Config.h"
#include "bcc/Support/Log.h"

namespace {

void llvm_error_handler(void *pUserData, const std::string &pMessage) {
  ALOGE("%s", pMessage.c_str());
  ::exit(1);
}

} // end anonymous namespace

void bcc::init::Initialize() {
  static bool is_initialized = false;

  if (is_initialized) {
    return;
  }

  // Setup error handler for LLVM.
  llvm::remove_fatal_error_handler();
  llvm::install_fatal_error_handler(llvm_error_handler, NULL);

#if defined(PROVIDE_ARM_CODEGEN)
  LLVMInitializeARMAsmPrinter();
# if USE_DISASSEMBLER
  LLVMInitializeARMDisassembler();
# endif
  LLVMInitializeARMTargetMC();
  LLVMInitializeARMTargetInfo();
  LLVMInitializeARMTarget();
  LLVMInitializeARMLDTargetInfo();
  LLVMInitializeARMLDTarget();
  LLVMInitializeARMLDBackend();
  LLVMInitializeARMDiagnosticLineInfo();
#endif

#if defined(PROVIDE_MIPS_CODEGEN)
  LLVMInitializeMipsAsmPrinter();
# if USE_DISASSEMBLER
  LLVMInitializeMipsDisassembler();
# endif
  LLVMInitializeMipsTargetMC();
  LLVMInitializeMipsTargetInfo();
  LLVMInitializeMipsTarget();
  LLVMInitializeMipsLDTargetInfo();
  LLVMInitializeMipsLDTarget();
  LLVMInitializeMipsLDBackend();
  LLVMInitializeMipsDiagnosticLineInfo();
#endif

#if defined(PROVIDE_X86_CODEGEN)
  LLVMInitializeX86AsmPrinter();
# if USE_DISASSEMBLER
  LLVMInitializeX86Disassembler();
# endif
  LLVMInitializeX86TargetMC();
  LLVMInitializeX86TargetInfo();
  LLVMInitializeX86Target();
  LLVMInitializeX86LDTargetInfo();
  LLVMInitializeX86LDTarget();
  LLVMInitializeX86LDBackend();
  LLVMInitializeX86DiagnosticLineInfo();
#endif

  is_initialized = true;

  return;
}
