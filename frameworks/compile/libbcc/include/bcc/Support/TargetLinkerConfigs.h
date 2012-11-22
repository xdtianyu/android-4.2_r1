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

#ifndef BCC_SUPPORT_TARGET_LINKER_CONFIGS_H
#define BCC_SUPPORT_TARGET_LINKER_CONFIGS_H

#include <string>

#include "bcc/Config/Config.h"
#include "bcc/Support/LinkerConfig.h"

namespace bcc {

//===----------------------------------------------------------------------===//
// ARM
//===----------------------------------------------------------------------===//
#if defined(PROVIDE_ARM_CODEGEN)
class ARMLinkerConfig : public LinkerConfig {
public:
  ARMLinkerConfig();
};
#endif // defined(PROVIDE_ARM_CODEGEN)

//===----------------------------------------------------------------------===//
// MIPS
//===----------------------------------------------------------------------===//
#if defined(PROVIDE_MIPS_CODEGEN)
class MipsLinkerConfig : public LinkerConfig {
public:
  MipsLinkerConfig();
};
#endif // defined(PROVIDE_MIPS_CODEGEN)

//===----------------------------------------------------------------------===//
// X86 and X86_64
//===----------------------------------------------------------------------===//
#if defined(PROVIDE_X86_CODEGEN)
class X86FamilyLinkerConfigBase : public LinkerConfig {
public:
  X86FamilyLinkerConfigBase(const std::string& pTriple);
};

class X86_32LinkerConfig : public X86FamilyLinkerConfigBase {
public:
  X86_32LinkerConfig();
};

class X86_64LinkerConfig : public X86FamilyLinkerConfigBase {
public:
  X86_64LinkerConfig();
};
#endif // defined(PROVIDE_X86_CODEGEN)

//===----------------------------------------------------------------------===//
// Default target
//===----------------------------------------------------------------------===//
class DefaultLinkerConfig : public
#if defined (DEFAULT_ARM_CODEGEN)
  ARMLinkerConfig
#elif defined (DEFAULT_MIPS_CODEGEN)
  MipsLinkerConfig
#elif defined (DEFAULT_X86_CODEGEN)
  X86_32LinkerConfig
#elif defined (DEFAULT_X86_64_CODEGEN)
  X86_64LinkerConfig
#else
#  error "Unsupported Default Target!"
#endif
{ };

} // end namespace bcc

#endif // BCC_SUPPORT_LINKER_CONFIG_H
