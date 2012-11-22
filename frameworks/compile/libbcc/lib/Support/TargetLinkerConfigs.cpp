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

#include "bcc/Config/Config.h"
#include "bcc/Support/TargetLinkerConfigs.h"

using namespace bcc;

#ifdef TARGET_BUILD
static const char* gDefaultDyld = "/system/bin/linker";
static const char* gDefaultSysroot = "/system";
#else
static const char* gDefaultDyld = "/usr/lib/ld.so.1";
static const char* gDefaultSysroot = "/";
#endif

//===----------------------------------------------------------------------===//
// ARM
//===----------------------------------------------------------------------===//
#if defined(PROVIDE_ARM_CODEGEN)
ARMLinkerConfig::ARMLinkerConfig() : LinkerConfig(DEFAULT_ARM_TRIPLE_STRING) {

  // set up target-dependent constraints of attributes
  getLDInfo()->attrFactory().constraint().enableWholeArchive();
  getLDInfo()->attrFactory().constraint().disableAsNeeded();
  getLDInfo()->attrFactory().constraint().setSharedSystem();

  // set up the predefined attributes
  getLDInfo()->attrFactory().predefined().setWholeArchive();
  getLDInfo()->attrFactory().predefined().setDynamic();

  // set up target dependent options
  if (getLDInfo()->options().sysroot().empty()) {
    getLDInfo()->options().setSysroot(gDefaultSysroot);
  }

  if (!getLDInfo()->options().hasDyld()) {
    getLDInfo()->options().setDyld(gDefaultDyld);
  }
}
#endif // defined(PROVIDE_ARM_CODEGEN)

//===----------------------------------------------------------------------===//
// Mips
//===----------------------------------------------------------------------===//
#if defined(PROVIDE_MIPS_CODEGEN)
MipsLinkerConfig::MipsLinkerConfig()
  : LinkerConfig(DEFAULT_MIPS_TRIPLE_STRING) {

  // set up target-dependent constraints of attibutes
  getLDInfo()->attrFactory().constraint().enableWholeArchive();
  getLDInfo()->attrFactory().constraint().disableAsNeeded();
  getLDInfo()->attrFactory().constraint().setSharedSystem();

  // set up the predefined attributes
  getLDInfo()->attrFactory().predefined().setWholeArchive();
  getLDInfo()->attrFactory().predefined().setDynamic();

  // set up target dependent options
  if (getLDInfo()->options().sysroot().empty()) {
    getLDInfo()->options().setSysroot(gDefaultSysroot);
  }

  if (!getLDInfo()->options().hasDyld()) {
    getLDInfo()->options().setDyld(gDefaultDyld);
  }
}
#endif // defined(PROVIDE_MIPS_CODEGEN)

//===----------------------------------------------------------------------===//
// X86 and X86_64
//===----------------------------------------------------------------------===//
#if defined(PROVIDE_X86_CODEGEN)
X86FamilyLinkerConfigBase::X86FamilyLinkerConfigBase(const std::string& pTriple)
  : LinkerConfig(pTriple) {
  // set up target-dependent constraints of attibutes
  getLDInfo()->attrFactory().constraint().enableWholeArchive();
  getLDInfo()->attrFactory().constraint().disableAsNeeded();
  getLDInfo()->attrFactory().constraint().setSharedSystem();

  // set up the predefined attributes
  getLDInfo()->attrFactory().predefined().setWholeArchive();
  getLDInfo()->attrFactory().predefined().setDynamic();

  // set up target dependent options
  if (getLDInfo()->options().sysroot().empty()) {
    getLDInfo()->options().setSysroot(gDefaultSysroot);
  }

  if (!getLDInfo()->options().hasDyld()) {
    getLDInfo()->options().setDyld(gDefaultDyld);
  }
}

X86_32LinkerConfig::X86_32LinkerConfig()
  : X86FamilyLinkerConfigBase(DEFAULT_X86_TRIPLE_STRING) {
}

X86_64LinkerConfig::X86_64LinkerConfig()
  : X86FamilyLinkerConfigBase(DEFAULT_X86_64_TRIPLE_STRING) {
}
#endif // defined(PROVIDE_X86_CODEGEN)
