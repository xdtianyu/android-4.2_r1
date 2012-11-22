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

#include "bcc/AndroidBitcode/ABCCompiler.h"

#include <llvm/Module.h>
#include <llvm/PassManager.h>
#include <llvm/Target/TargetData.h>
#include <llvm/Target/TargetMachine.h>

#include "bcc/AndroidBitcode/ABCCompilerDriver.h"
#include "bcc/AndroidBitcode/ABCExpandVAArgPass.h"
#include "bcc/Script.h"
#include "bcc/Source.h"


namespace bcc {

bool ABCCompiler::beforeAddCodeGenPasses(Script &pScript,
                                         llvm::PassManager &pPM) {
  llvm::PassManager pm;
  llvm::Module &module = pScript.getSource().getModule();
  const llvm::TargetMachine &tm = getTargetMachine();
  llvm::TargetData *target_data =
    new (std::nothrow) llvm::TargetData(*(tm.getTargetData()));

  if (target_data == NULL) {
    return false;
  }

  pm.add(target_data);
  pm.add(mDriver.createExpandVAArgPass());
  pm.run(module);

  return true;
}

} // namespace bcc
