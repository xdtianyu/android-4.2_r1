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

#ifndef BCC_ABC_COMPILER_DRIVER_H
#define BCC_ABC_COMPILER_DRIVER_H

#include "bcc/AndroidBitcode/ABCCompiler.h"
#include "bcc/BCCContext.h"
#include "bcc/Compiler.h"
#include "bcc/Linker.h"

#include <string>

namespace bcc {

class ABCExpandVAArgPass;
class CompilerConfig;
class LinkerConfig;

class ABCCompilerDriver {
private:
  BCCContext mContext;
  ABCCompiler mCompiler;
  Linker mLinker;

  CompilerConfig *mCompilerConfig;
  LinkerConfig *mLinkerConfig;

  std::string mTriple;
  std::string mAndroidSysroot;

private:
  bool configCompiler();
  bool configLinker();

private:
  Script *prepareScript(int pInputFd);
  bool compile(Script &pScript, llvm::raw_ostream &pOutput);
  bool link(const Script &pScript, const std::string &input_relocatable,
            int pOutputFd);

protected:
  virtual const char **getNonPortableList() const {
    return NULL;
  }

public:
  virtual ABCExpandVAArgPass *createExpandVAArgPass() const = 0;

protected:
  ABCCompilerDriver(const std::string &pTriple);

public:
  static ABCCompilerDriver *Create(const std::string &pTriple);

  virtual ~ABCCompilerDriver();

  inline const std::string &getAndroidSysroot() const {
    return mAndroidSysroot;
  }

  inline void setAndroidSysroot(const std::string &pAndroidSysroot) {
    mAndroidSysroot = pAndroidSysroot;
  }

  inline const std::string &getTriple() const {
    return mTriple;
  }

  // Compile the bitcode and link the shared object
  bool build(int pInputFd, int pOutputFd);
};

} // end namespace bcc

#endif // BCC_ABC_COMPILER_DRIVER_H
