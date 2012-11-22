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

#include <cerrno>
#include <cstdio>
#include <cstdlib>
#include <cstring>

#include <fcntl.h>

#include "bcc/Config/Config.h"
#include "bcc/Support/Initialization.h"
#include "bcc/Support/Log.h"
#include "bcc/AndroidBitcode/ABCCompilerDriver.h"

#include <cutils/process_name.h>

using namespace bcc;

static void usage() {
  fprintf(stderr, "usage: abcc [--fd output_fd|--file output_filename]\n"
#ifndef TARGET_BUILD
                  "            [--triple triple]\n"
                  "            [--android-sysroot sysroot]\n"
#endif
                  "            input_filename(s) or input_fd(s)...\n");
  return;
}

static inline bool GetIntArg(const char *arg, int &result) {
  char *endptr;

  result = ::strtol(arg, &endptr, 0);
  if (*endptr != '\0') {
    return false;
  } else {
    return true;
  }
}

enum Mode {
  kUnknownMode,
  kFdMode,
  kFileMode
};

static inline bool ParseArguments(int argc, const char *const *argv, Mode &mode,
                                  const char *&input, const char *&output,
                                  const char *&triple, const char *&sysroot) {
  if (argc < 4) {
    return false;
  }

  // Parse the mode in argv[1].
  if (::strcmp(argv[1], "--fd") == 0) {
    mode = kFdMode;
  } else if (::strcmp(argv[1], "--file") == 0) {
    mode = kFileMode;
  } else {
    ALOGE("Unknown mode '%s'!", argv[1]);
    return false;
  }

  // output is always in argv[2].
  output = argv[2];

  // On-device version cannot configure the triple and sysroot.
  int arg_idx = 3;
#ifndef TARGET_BUILD
  if (::strcmp(argv[arg_idx], "--triple") == 0) {
    if ((arg_idx + 2 /* --triple [triple] input */) >= argc) {
      ALOGE("Too few arguments when --triple was given!");
      return false;
    }

    triple = argv[arg_idx + 1];
    arg_idx += 2;
  }

  if (::strcmp(argv[arg_idx], "--android-sysroot") == 0) {
    if ((arg_idx + 2 /* --android-sysroot [sysroot] input */) >= argc) {
      ALOGE("Too few arguments when --android-sysroot was given!");
      return false;
    }

    sysroot = argv[arg_idx + 1];
    arg_idx += 2;
  }
#endif

  if (triple == NULL) {
    triple = DEFAULT_TARGET_TRIPLE_STRING;
  }

  if (sysroot == NULL) {
    sysroot = "/";
  }

  ALOGD("Triple: %s, Android sysroot: %s", triple, sysroot);

  // input is in argv[arg_idx]
  // TODO: Support multiple input files.
  input = argv[arg_idx];

  return true;
}

static bool Build(int input_fd, int output_fd,
                  const char *triple, const char *sysroot) {
  ABCCompilerDriver *driver = ABCCompilerDriver::Create(triple);

  if (driver == NULL) {
    return false;
  }

  driver->setAndroidSysroot(sysroot);

  bool build_result = driver->build(input_fd, output_fd);;

  delete driver;

  return build_result;
}

static int ProcessFromFd(const char *input, const char *output,
                         const char *triple, const char *sysroot) {
  int output_fd, input_fd;

  if (!GetIntArg(output, output_fd)) {
    ALOGE("Bad output fd '%s'", output);
    return EXIT_FAILURE;
  }

  if (!GetIntArg(input, input_fd)) {
    ALOGE("Bad input fd '%s'", input);
    return EXIT_FAILURE;
  }

  if (!Build(input_fd, output_fd, triple, sysroot)) {
    return EXIT_FAILURE;
  }

  return EXIT_SUCCESS;
}

static int ProcessFromFile(const char *input, const char *output,
                           const char *triple, const char *sysroot) {
  // TODO: Support multiple input files.
  int output_fd = -1, input_fd = -1;

  // Open the output file.
  output_fd = ::open(output, O_RDWR | O_CREAT | O_TRUNC, 0755);

  if (output_fd < 0) {
    ALOGE("Failed to open %s for output! (%s)", output, strerror(errno));
    return EXIT_FAILURE;
  }

  // Open the input file.
  input_fd = ::open(input, O_RDONLY);

  if (input_fd < 0) {
    ALOGE("Failed to open %s for input! (%s)", input, strerror(errno));
    ::close(output_fd);
    return EXIT_FAILURE;
  }

  if (!Build(input_fd, output_fd, triple, sysroot)) {
    ::close(output_fd);
    ::close(input_fd);
    return EXIT_FAILURE;
  }

  ::close(output_fd);
  ::close(input_fd);

  return EXIT_SUCCESS;
}

int main(int argc, char **argv) {
  Mode mode = kUnknownMode;
  const char *input, *output, *triple = NULL, *sysroot = NULL;

  set_process_name("abcc");

  setvbuf(stdout, NULL, _IONBF, 0);

  init::Initialize();

  if (ParseArguments(argc, argv, mode, input, output, triple, sysroot)) {
    switch (mode) {
      case kFdMode: {
        return ProcessFromFd(input, output, triple, sysroot);
      }
      case kFileMode: {
        return ProcessFromFile(input, output, triple, sysroot);
      }
      default: {
        // Unknown mode encountered. Fall-through to print usage and return
        // error.
        break;
      }
    }
    // fall-through
  }

  usage();

  return EXIT_FAILURE;
}
