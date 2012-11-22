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

#include "Mips/MipsABCCompilerDriver.h"

namespace {

static const char *MipsNonPortableList[] = {
  "stat",
  "fstat",
  "lstat",
  "fstatat",
  "socket",
  "setsockopt",
  "getsockopt",
  "open",
  "mmap",
  "ioctl",

  NULL  // NUL-terminator
};

} // end anonymous namespace

namespace bcc {

const char **MipsABCCompilerDriver::getNonPortableList() const {
  return MipsNonPortableList;
}

} // end namespace bcc
