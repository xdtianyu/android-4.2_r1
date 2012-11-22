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

#include "bcc/Renderscript/RSScript.h"

#include "bcc/Renderscript/RSInfo.h"
#include "bcc/Source.h"
#include "bcc/Support/Log.h"

using namespace bcc;

bool RSScript::LinkRuntime(RSScript &pScript) {
  // Using the same context with the source in pScript.
  BCCContext &context = pScript.getSource().getContext();
  const char* core_lib = RSInfo::LibCLCorePath;

  // NEON-capable devices can use an accelerated math library for all
  // reduced precision scripts.
#if defined(ARCH_ARM_HAVE_NEON)
  const RSInfo* info = pScript.getInfo();
  if ((info != NULL) &&
      (info->getFloatPrecisionRequirement() != RSInfo::FP_Full)) {
    core_lib = RSInfo::LibCLCoreNEONPath;
  }
#endif

  Source *libclcore_source = Source::CreateFromFile(context, core_lib);
  if (libclcore_source == NULL) {
    ALOGE("Failed to load Renderscript library '%s' to link!", core_lib);
    return false;
  }

  if (!pScript.getSource().merge(*libclcore_source,
                                 /* pPreserveSource */false)) {
    ALOGE("Failed to link Renderscript library '%s'!", core_lib);
    delete libclcore_source;
    return false;
  }

  return true;
}

RSScript::RSScript(Source &pSource)
  : Script(pSource), mInfo(NULL), mCompilerVersion(0),
    mOptimizationLevel(kOptLvl3) { }

bool RSScript::doReset() {
  mInfo = NULL;
  mCompilerVersion = 0;
  mOptimizationLevel = kOptLvl3;
  return true;
}
