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

#ifndef BCC_SUPPORT_MEMORY_FACTORY_H
#define BCC_SUPPORT_MEMORY_FACTORY_H

#include <mcld/Support/FileHandle.h>
#include <mcld/Support/MemoryAreaFactory.h>

namespace mcld {
class MemoryArea;
} // end namespace mcld

namespace bcc {

class MemoryFactory : public mcld::MemoryAreaFactory {
public:
  MemoryFactory() : mcld::MemoryAreaFactory(32) { }

  ~MemoryFactory() { }

  using mcld::MemoryAreaFactory::produce;

  mcld::MemoryArea* produce(void *pMemBuffer, size_t pSize)
  { return mcld::MemoryAreaFactory::create(pMemBuffer, pSize); }

  mcld::MemoryArea* produce(int pFD)
  { return mcld::MemoryAreaFactory::create(pFD, mcld::FileHandle::Unknown); }
};

} // end namespace bcc

#endif // BCC_SUPPORT_MEMORY_FACTORY_H
