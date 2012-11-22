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

#ifndef BCC_ABC_COMPILER_H
#define BCC_ABC_COMPILER_H

#include "bcc/Compiler.h"

namespace bcc {

class ABCCompilerDriver;

class ABCCompiler : public Compiler {
private:
  const ABCCompilerDriver &mDriver;

public:
  ABCCompiler(const ABCCompilerDriver &pDriver) : mDriver(pDriver) { }

  virtual ~ABCCompiler() { }

private:
  virtual bool beforeAddCodeGenPasses(Script &pScript, llvm::PassManager &pPM);
};

} // end namespace bcc

#endif // BCC_ABC_COMPILER_H
