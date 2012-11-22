/*
 * Copyright 2010-2012, The Android Open Source Project
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

#ifndef BCC_LINKER_H
#define BCC_LINKER_H

#include <string>

namespace mcld {

class TargetLDBackend;
class MCLDDriver;
class MemoryFactory;
class MCLDInfo;
class TreeIteratorBase;
class Input;

namespace sys { namespace fs {

class Path;

} } // end namespace sys::fs

} // end namespace mcld

namespace bcc {

class MemoryFactory;
class LinkerConfig;

class Linker {
public:
  enum ErrorCode {
    kSuccess,
    kDoubleConfig,
    kCreateBackend,
    kDelegateLDInfo,
    kFindNameSpec,
    kOpenNameSpec,
    kOpenObjectFile,
    kNotConfig,
    kNotSetUpOutput,
    kOpenOutput,
    kReadSections,
    kReadSymbols,
    kAddAdditionalSymbols,
    kMaxErrorCode,
  };

  static const char *GetErrorString(enum ErrorCode pErrCode);

private:
  mcld::TargetLDBackend *mBackend;
  mcld::MCLDDriver *mDriver;
  MemoryFactory *mMemAreaFactory;
  mcld::MCLDInfo *mLDInfo;
  mcld::TreeIteratorBase *mRoot;
  bool mShared;
  std::string mSOName;

public:
  Linker();

  Linker(const LinkerConfig& pConfig);

  ~Linker();

  enum ErrorCode config(const LinkerConfig& pConfig);

  enum ErrorCode addNameSpec(const std::string &pNameSpec);

  enum ErrorCode addObject(const std::string &pObjectPath);

  enum ErrorCode addObject(void* pMemory, size_t pSize);

  enum ErrorCode addCode(void* pMemory, size_t pSize);

  enum ErrorCode setOutput(const std::string &pPath);

  enum ErrorCode setOutput(int pFileHandler);

  enum ErrorCode link();

private:
  enum ErrorCode extractFiles(const LinkerConfig& pConfig);

  enum ErrorCode openFile(const mcld::sys::fs::Path& pPath,
                          enum ErrorCode pCode,
                          mcld::Input& pInput);

  void advanceRoot();
};

} // end namespace bcc

#endif // BCC_LINKER_H
