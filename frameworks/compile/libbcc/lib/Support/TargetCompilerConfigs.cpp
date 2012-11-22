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

#include "bcc/Support/TargetCompilerConfigs.h"

using namespace bcc;

//===----------------------------------------------------------------------===//
// ARM
//===----------------------------------------------------------------------===//
#if defined(PROVIDE_ARM_CODEGEN)

void ARMCompilerConfig::GetFeatureVector(std::vector<std::string> &pAttributes,
                                         bool pEnableNEON) {
#if defined(ARCH_ARM_HAVE_VFP)
  pAttributes.push_back("+vfp3");
#  if !defined(ARCH_ARM_HAVE_VFP_D32)
  pAttributes.push_back("+d16");
#  endif
#endif

#if defined(ARCH_ARM_HAVE_NEON) && !defined(DISABLE_ARCH_ARM_HAVE_NEON)
  if (pEnableNEON) {
    pAttributes.push_back("+neon");
    pAttributes.push_back("+neonfp");
  } else {
    pAttributes.push_back("-neon");
    pAttributes.push_back("-neonfp");
  }
#else
  pAttributes.push_back("-neon");
  pAttributes.push_back("-neonfp");
#endif

  return;
}

ARMCompilerConfig::ARMCompilerConfig()
  : CompilerConfig(DEFAULT_ARM_TRIPLE_STRING) {

  // Enable NEON by default.
  mEnableNEON = true;

  std::vector<std::string> attributes;
  GetFeatureVector(attributes, /* pEnableNEON */mEnableNEON);
  setFeatureString(attributes);

  return;
}

bool ARMCompilerConfig::enableNEON(bool pEnable) {
#if defined(ARCH_ARM_HAVE_NEON) && !defined(DISABLE_ARCH_ARM_HAVE_NEON)
  if (mEnableNEON != pEnable) {
    std::vector<std::string> attributes;
    GetFeatureVector(attributes, pEnable);
    setFeatureString(attributes);
    mEnableNEON = pEnable;
    return true;
  }
  // Fall-through
#endif
  return false;
}
#endif // defined(PROVIDE_ARM_CODEGEN)
