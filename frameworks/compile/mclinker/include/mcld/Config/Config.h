//===- Config.h.in --------------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//

//===----------------------------------------------------------------------===//
// Hand-coded for Android build
//===----------------------------------------------------------------------===//

#ifndef MCLD_CONFIG_H
#define MCLD_CONFIG_H

#include <llvm/Config/llvm-config.h>

#ifdef LLVM_ON_UNIX
# define MCLD_ON_UNIX 1
#else
// Assume on Win32 otherwise.
# define MCLD_ON_WIN32 1
#endif

#define MCLD_VERSION "Phoenix - 1.4.0"

#endif

