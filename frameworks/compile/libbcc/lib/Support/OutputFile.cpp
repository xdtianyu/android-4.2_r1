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

#include "bcc/Support/OutputFile.h"

#include <cstdlib>

#include <llvm/Support/raw_ostream.h>

#include "bcc/Support/Log.h"

using namespace bcc;

OutputFile *OutputFile::CreateTemporary(const std::string &pFileTemplate,
                                        unsigned pFlags) {
  char *tmp_filename = NULL;
  int tmp_fd;
  OutputFile *result = NULL;

  // Allocate memory to hold the generated unique temporary filename.
  tmp_filename =
      new (std::nothrow) char [ pFileTemplate.length() + /* .XXXXXX */7 + 1 ];
  if (tmp_filename == NULL) {
    ALOGE("Out of memory when allocates memory for filename %s in "
          "OutputFile::CreateTemporary()!", pFileTemplate.c_str());
    return NULL;
  }

  // Construct filename template for mkstemp().
  if (pFileTemplate.length() > 0)
    ::memcpy(tmp_filename, pFileTemplate.c_str(), pFileTemplate.length());
  ::strncpy(tmp_filename + pFileTemplate.length(), ".XXXXXX", 7);

  // POSIX mkstemp() never returns EINTR.
  tmp_fd = ::mkstemp(tmp_filename);
  if (tmp_fd < 0) {
    llvm::error_code err(errno, llvm::posix_category());
    ALOGE("Failed to create temporary file using mkstemp() for %s! (%s)",
          tmp_filename, err.message().c_str());
    delete [] tmp_filename;
    return NULL;
  }

  // Create result OutputFile. Temporary file is always truncated.
  result = new (std::nothrow) OutputFile(tmp_filename,
                                         pFlags | FileBase::kTruncate);
  if (result == NULL) {
    ALOGE("Out of memory when creates OutputFile for %s!", tmp_filename);
    // Fall through to the clean-up codes.
  } else {
    if (result->hasError()) {
      ALOGE("Failed to open temporary output file %s! (%s)",
            result->getName().c_str(), result->getErrorMessage().c_str());
      delete result;
      result = NULL;
      // Fall through to the clean-up codes.
    }
  }

  // Clean up.
  delete [] tmp_filename;
  ::close(tmp_fd);

  return result;
}

OutputFile::OutputFile(const std::string &pFilename, unsigned pFlags)
  : super(pFilename, pFlags) { }

ssize_t OutputFile::write(const void *pBuf, size_t count) {
  if ((mFD < 0) || hasError()) {
    return -1;
  }

  if ((count <= 0) || (pBuf == NULL)) {
    // Keep safe and issue a warning.
    ALOGW("OutputFile::write: count = %zu, buffer = %p", count, pBuf);
    return 0;
  }

  while (count > 0) {
    ssize_t write_size = ::write(mFD, pBuf, count);

    if (write_size > 0) {
      return write_size;
    } else if ((errno == EAGAIN) || (errno == EINTR)) {
      // If the errno is EAGAIN or EINTR, then we try to write again.
      //
      // Fall-through
    } else {
      detectError();
      return -1;
    }
  }
  // unreachable
  return 0;
}

void OutputFile::truncate() {
  if (mFD < 0) {
    return;
  }

  do {
    if (::ftruncate(mFD, 0) == 0) {
      return;
    }
  } while (errno == EINTR);
  detectError();

  return;
}

llvm::raw_fd_ostream *OutputFile::dup() {
  int newfd;

  do {
    newfd = ::dup(mFD);
    if (newfd < 0) {
      if (errno != EINTR) {
        detectError();
        return NULL;
      }
      // EINTR
      continue;
    }
    // dup() returns ok.
    break;
  } while (true);

  llvm::raw_fd_ostream *result =
      new (std::nothrow) llvm::raw_fd_ostream(newfd, /* shouldClose */true);

  if (result == NULL) {
    mError.assign(llvm::errc::not_enough_memory, llvm::system_category());
  }

  return result;
}
