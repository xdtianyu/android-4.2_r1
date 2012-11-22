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

#include "bcc/ExecutionEngine/SymbolResolvers.h"

#include <dlfcn.h>

#include <cassert>
#include <cstdio>
#include <new>

using namespace bcc;

//===----------------------------------------------------------------------===//
// DyldSymbolResolver
//===----------------------------------------------------------------------===//
DyldSymbolResolver::DyldSymbolResolver(const char *pFileName,
                                       bool pLazyBinding) : mError(NULL) {
  int flags = (pLazyBinding) ? RTLD_LAZY : RTLD_NOW;

  // Make the symbol within the given library to be local such that it won't
  // be available for symbol resolution of subsequently loaded libraries.
  flags |= RTLD_LOCAL;

  mHandle = ::dlopen(pFileName, flags);
  if (mHandle == NULL) {
    const char *err = ::dlerror();

#define DYLD_ERROR_MSG_PATTERN  "Failed to load %s! (%s)"
    size_t error_length = ::strlen(DYLD_ERROR_MSG_PATTERN) +
                          ::strlen(pFileName) + 1;
    if (err != NULL) {
      error_length += ::strlen(err);
    }

    mError = new (std::nothrow) char [error_length];
    if (mError != NULL) {
      ::snprintf(mError, error_length, DYLD_ERROR_MSG_PATTERN, pFileName,
                 ((err != NULL) ? err : ""));
    }
  }
#undef DYLD_ERROR_MSG_PATTERN
}

void *DyldSymbolResolver::getAddress(const char *pName) {
  assert((mHandle != NULL) && "Invalid DyldSymbolResolver!");
  return ::dlsym(mHandle, pName);
}

DyldSymbolResolver::~DyldSymbolResolver() {
  ::dlclose(mHandle);
  delete [] mError;
}
