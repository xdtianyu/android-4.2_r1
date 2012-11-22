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

#include "bcc/ExecutionEngine/BCCRuntimeSymbolResolver.h"

// int_lib.h included by BCCRuntimeStub.h has the following line:
//
//            typedef      int si_int;
//
// However, there's already a macro also named "si_int" defined in siginfo.h
// bionic/libc/kernel/common/asm-generic/. This is #undef is a hack to avoid
// compiler mistakenly recognize the identifier "si_int" as a macro and abort
// the compilation.
//
// This line of hack should put in the header file since it invalidate a
// "system" scope macro definition.
#undef si_int
#include "BCCRuntimeStub.h"

using namespace bcc;

#if defined(__arm__) || defined(__mips__)
  #define DEF_GENERIC_RUNTIME(func)   \
    extern void *func;
  #define DEF_VFP_RUNTIME(func) \
    extern void *func ## vfp;
  #define DEF_LLVM_RUNTIME(func)
  #define DEF_BCC_RUNTIME(func)
#include "BCCRuntime.def"
#endif

const BCCRuntimeSymbolResolver::SymbolMap BCCRuntimeSymbolResolver::SymbolArray[] = {
#if defined(__arm__) || defined(__mips__)
  #define DEF_GENERIC_RUNTIME(func)   \
    { #func, (void*) &func },
  // TODO: enable only when target support VFP
  #define DEF_VFP_RUNTIME(func) \
    { #func, (void*) &func ## vfp },
#else
  // host compiler library must contain generic runtime
  #define DEF_GENERIC_RUNTIME(func)
  #define DEF_VFP_RUNTIME(func)
#endif
#define DEF_LLVM_RUNTIME(func)   \
  { #func, (void*) &func },
#define DEF_BCC_RUNTIME(func) \
  { #func, &func ## _bcc },
#include "BCCRuntime.def"
};

const size_t BCCRuntimeSymbolResolver::NumSymbols =
  sizeof(BCCRuntimeSymbolResolver::SymbolArray) /
    sizeof(BCCRuntimeSymbolResolver::SymbolArray[0]);
