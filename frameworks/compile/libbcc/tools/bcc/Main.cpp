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

#include <string>
#include <vector>

#include <stdlib.h>

#include <llvm/ADT/STLExtras.h>
#include <llvm/ADT/SmallString.h>
#include <llvm/Config/config.h>
#include <llvm/Support/CommandLine.h>
#include <llvm/Support/FileSystem.h>
#include <llvm/Support/Path.h>
#include <llvm/Support/raw_ostream.h>
#include <llvm/Support/system_error.h>

#include <bcc/BCCContext.h>
#include <bcc/Compiler.h>
#include <bcc/Config/BuildInfo.h>
#include <bcc/Config/Config.h>
#include <bcc/ExecutionEngine/BCCRuntimeSymbolResolver.h>
#include <bcc/ExecutionEngine/ObjectLoader.h>
#include <bcc/ExecutionEngine/SymbolResolverProxy.h>
#include <bcc/ExecutionEngine/SymbolResolvers.h>
#include <bcc/Script.h>
#include <bcc/Source.h>
#include <bcc/Support/CompilerConfig.h>
#include <bcc/Support/Initialization.h>
#include <bcc/Support/InputFile.h>
#include <bcc/Support/OutputFile.h>
#include <bcc/Support/TargetCompilerConfigs.h>

using namespace bcc;

//===----------------------------------------------------------------------===//
// General Options
//===----------------------------------------------------------------------===//
namespace {

llvm::cl::list<std::string>
OptInputFilenames(llvm::cl::Positional, llvm::cl::OneOrMore,
                  llvm::cl::desc("<input bitcode files>"));

llvm::cl::opt<std::string>
OptOutputFilename("o", llvm::cl::desc("Specify the output filename"),
                  llvm::cl::value_desc("filename"));

#ifdef TARGET_BUILD
const std::string OptTargetTriple(DEFAULT_TARGET_TRIPLE_STRING);
#else
llvm::cl::opt<std::string>
OptTargetTriple("mtriple",
                llvm::cl::desc("Specify the target triple (default: "
                               DEFAULT_TARGET_TRIPLE_STRING ")"),
                llvm::cl::init(DEFAULT_TARGET_TRIPLE_STRING),
                llvm::cl::value_desc("triple"));

llvm::cl::alias OptTargetTripleC("C", llvm::cl::NotHidden,
                                 llvm::cl::desc("Alias for -mtriple"),
                                 llvm::cl::aliasopt(OptTargetTriple));
#endif

//===----------------------------------------------------------------------===//
// Compiler Options
//===----------------------------------------------------------------------===//
llvm::cl::opt<bool>
OptPIC("fPIC", llvm::cl::desc("Generate fully relocatable, position independent"
                              " code"));

llvm::cl::opt<char>
OptOptLevel("O", llvm::cl::desc("Optimization level. [-O0, -O1, -O2, or -O3] "
                                "(default: -O2)"),
            llvm::cl::Prefix, llvm::cl::ZeroOrMore, llvm::cl::init('2'));

llvm::cl::opt<bool>
OptC("c", llvm::cl::desc("Compile and assemble, but do not link."));

//===----------------------------------------------------------------------===//
// Linker Options
//===----------------------------------------------------------------------===//
// FIXME: this option will be removed in the future when MCLinker is capable
//        of generating shared library directly from given bitcode. It only
//        takes effect when -shared is supplied.
llvm::cl::opt<std::string>
OptImmObjectOutput("or", llvm::cl::desc("Specify the filename for output the "
                                        "intermediate relocatable when linking "
                                        "the input bitcode to the shared "
                                        "library"), llvm::cl::ValueRequired);

llvm::cl::opt<bool>
OptShared("shared", llvm::cl::desc("Create a shared library from input bitcode "
                                   "files"));


//===----------------------------------------------------------------------===//
// Loader Options
//===----------------------------------------------------------------------===//
llvm::cl::opt<bool>
OptRunEntry("R", llvm::cl::desc("Run the entry method after successfully load "
                                "and compile."));

llvm::cl::opt<std::string>
OptEntryFunction("entry-function", llvm::cl::desc("Specify the entry function "
                                                  "for -R (default: main)"),
                 llvm::cl::value_desc("function"), llvm::cl::init("main"));

llvm::cl::opt<bool>
OptEnableGDB("enable-gdb", llvm::cl::desc("Enable GDB JIT debugging when "
                                          "runs the entry method"));

llvm::cl::list<std::string>
OptRuntimeLibs("load", llvm::cl::desc("Specify the shared libraries for "
                                      "execution (e.g., -load=c will search "
                                      "and load libc.so for execution)"),
               llvm::cl::ZeroOrMore, llvm::cl::value_desc("namespec"));

// Override "bcc -version" since the LLVM version information is not correct on
// Android build.
void BCCVersionPrinter() {
  llvm::raw_ostream &os = llvm::outs();
  os << "libbcc (The Android Open Source Project, http://www.android.com/):\n"
     << "  Build time: " << BuildInfo::GetBuildTime() << "\n"
     << "  Build revision: " << BuildInfo::GetBuildRev() << "\n"
     << "  Build source blob: " << BuildInfo::GetBuildSourceBlob() << "\n"
     << "  Default target: " << DEFAULT_TARGET_TRIPLE_STRING << "\n";

  os << "\n";

  os << "LLVM (http://llvm.org/):\n"
     << "  Version: " << PACKAGE_VERSION << "\n";
  return;
}

} // end anonymous namespace

static inline
Script *PrepareScript(BCCContext &pContext,
                      const llvm::cl::list<std::string> &pBitcodeFiles) {
  Script *result = NULL;

  for (unsigned i = 0; i < pBitcodeFiles.size(); i++) {
    const std::string &input_bitcode = pBitcodeFiles[i];
    Source *source = Source::CreateFromFile(pContext, input_bitcode);
    if (source == NULL) {
      llvm::errs() << "Failed to load llvm module from file `" << input_bitcode
                   << "'!\n";
      return NULL;
    }

    if (result != NULL) {
      if (!result->mergeSource(*source, /* pPreserveSource */false)) {
        llvm::errs() << "Failed to merge the llvm module `" << input_bitcode
                     << "' to compile!\n";
        delete source;
        return NULL;
      }
    } else {
      result = new (std::nothrow) Script(*source);
      if (result == NULL) {
        llvm::errs() << "Out of memory when create script for file `"
                     << input_bitcode << "'!\n";
        delete source;
        return NULL;
      }
    }
  }

  return result;
}

static inline
bool ConfigCompiler(Compiler &pCompiler) {
  CompilerConfig *config = NULL;

#ifdef TARGET_BUILD
  config = new (std::nothrow) DefaultCompilerConfig();
#else
  config = new (std::nothrow) CompilerConfig(OptTargetTriple);
#endif
  if (config == NULL) {
    llvm::errs() << "Out of memory when create the compiler configuration!\n";
    return false;
  }

  // Setup the config according to the valud of command line option.
  if (OptPIC) {
    config->setRelocationModel(llvm::Reloc::PIC_);
  }
  switch (OptOptLevel) {
    case '0': config->setOptimizationLevel(llvm::CodeGenOpt::None); break;
    case '1': config->setOptimizationLevel(llvm::CodeGenOpt::Less); break;
    case '3': config->setOptimizationLevel(llvm::CodeGenOpt::Aggressive); break;
    case '2':
    default: {
      config->setOptimizationLevel(llvm::CodeGenOpt::Default);
      break;
    }
  }

  Compiler::ErrorCode result = pCompiler.config(*config);

  delete config;

  if (result != Compiler::kSuccess) {
    llvm::errs() << "Failed to configure the compiler! (detail: "
                 << Compiler::GetErrorString(result) << ")\n";
    return false;
  }

  return true;
}

#define DEFAULT_OUTPUT_PATH   "/sdcard/a.out"
static inline
std::string DetermineOutputFilename(const std::string &pOutputPath) {
  if (!pOutputPath.empty()) {
    return pOutputPath;
  }

  // User doesn't specify the value to -o.
  if (OptInputFilenames.size() > 1) {
    llvm::errs() << "Use " DEFAULT_OUTPUT_PATH " for output file!\n";
    return DEFAULT_OUTPUT_PATH;
  }

  // There's only one input bitcode file.
  const std::string &input_path = OptInputFilenames[0];
  llvm::SmallString<200> output_path(input_path);

  llvm::error_code err = llvm::sys::fs::make_absolute(output_path);
  if (err != llvm::errc::success) {
    llvm::errs() << "Failed to determine the absolute path of `" << input_path
                 << "'! (detail: " << err.message() << ")\n";
    return "";
  }

  if (OptC) {
    // -c was specified. Replace the extension to .o.
    llvm::sys::path::replace_extension(output_path, "o");
  } else {
    // Use a.out under current working directory when compile executable or
    // shared library.
    llvm::sys::path::remove_filename(output_path);
    llvm::sys::path::append(output_path, "a.out");
  }

  return output_path.c_str();
}

static inline
bool CompileScript(Compiler &pCompiler, Script &pScript,
                   const std::string &pOutputPath) {
  // Open the output file.
  OutputFile output_file(pOutputPath, FileBase::kTruncate);

  if (output_file.hasError()) {
    llvm::errs() << "Failed to open the output file `" << pOutputPath
                 << "'! (detail: " << output_file.getErrorMessage() << ")\n";
    return false;
  }

  // Run the compiler.
  Compiler::ErrorCode result = pCompiler.compile(pScript, output_file);
  if (result != Compiler::kSuccess) {
    llvm::errs() << "Fatal error during compilation (detail: "
                 << Compiler::GetErrorString(result) << ".)\n";
    return false;
  }

  return true;
}

static inline
bool PrepareRuntimes(std::vector<SymbolResolverInterface *> &pRuntimes) {
  llvm::SmallVector<const char *, 2> search_paths;

#ifdef TARGET_BUILD
  search_paths.push_back("/system/lib/");
#else
  search_paths.push_back("/lib/");
  search_paths.push_back("/usr/lib/");
#endif

  // Most of the following lines comes from llvm/tools/llvm-ld.cpp.
  for (unsigned i = 0; i < OptRuntimeLibs.size(); i++) {
    const std::string &lib = OptRuntimeLibs[i];
    llvm::sys::Path lib_path;
    for (llvm::SmallVectorImpl<const char *>::const_iterator
             search_path_iter = search_paths.begin(),
             search_path_end = search_paths.end();
         search_path_iter != search_path_end; search_path_iter++) {

      lib_path = *search_path_iter;
      lib_path.appendComponent("lib" + lib);
      lib_path.appendSuffix(llvm::sys::Path::GetDLLSuffix());

      if (lib_path.isEmpty()) {
        if (!lib_path.isDynamicLibrary()) {
          lib_path = llvm::sys::Path();
        } else {
          break;
        }
      }
    } // for each search_paths
    if (lib_path.isEmpty()) {
      // FIXME: llvm::sys::Path::FindLibrary(...) is able to consume
      //        'const std::string &' instead of 'std::string &'.
      std::string lib_tmp = lib;
      lib_path = llvm::sys::Path::FindLibrary(lib_tmp);
    }
    if (lib_path.isEmpty()) {
      llvm::errs() << "Unable to find `lib" << lib << "' for execution!\n";
      llvm::DeleteContainerPointers(pRuntimes);
      return false;
    } else {
      DyldSymbolResolver *dyld_resolver =
          new (std::nothrow) DyldSymbolResolver(lib_path.str().c_str());

      if (dyld_resolver != NULL) {
        pRuntimes.push_back(dyld_resolver);
      } else {
        llvm::errs() << "Out of memory when load `" << lib_path.str() << "'!\n";
        llvm::DeleteContainerPointers(pRuntimes);
        return false;
      }
    }
  } // for each OptRuntimeLibs

  return true;
}

static inline
bool LoadAndRun(const std::string &pOutputExecutable) {
  SymbolResolverProxy runtime_resolver;

  // Include compiler runtime.
  BCCRuntimeSymbolResolver bcc_runtimes;
  runtime_resolver.chainResolver(bcc_runtimes);

  // Open the output file for execution.
  InputFile input_exec(pOutputExecutable);
  if (input_exec.hasError()) {
    llvm::errs() << "Failed to open the executable `" << pOutputExecutable
                 << "'! (detail: " << input_exec.getErrorMessage() << ")\n";
    return false;
  }

  // Load the runtime libraries given in command line.
  std::vector<SymbolResolverInterface *> lib_runtimes;
  if (!PrepareRuntimes(lib_runtimes)) {
    return false;
  }

  for (std::vector<SymbolResolverInterface *>::const_iterator
           librt_iter = lib_runtimes.begin(), librt_end = lib_runtimes.end();
       librt_iter != librt_end; librt_iter++) {
    runtime_resolver.chainResolver(*(*librt_iter));
  }

  // Load the output file.
  ObjectLoader *loader = ObjectLoader::Load(input_exec, runtime_resolver,
                                            OptEnableGDB);
  if (loader == NULL) {
    llvm::errs() << "Failed to load `" << pOutputExecutable << "'!\n";
    llvm::DeleteContainerPointers(lib_runtimes);
    return false;
  }

  // Retrieve the address of entry function.
  void *entry = loader->getSymbolAddress(OptEntryFunction.c_str());
  if (entry == NULL) {
    llvm::errs() << "Couldn't find entry method `" << OptEntryFunction
                 << "' in " << pOutputExecutable << "' for execution!\n";
    delete loader;
    llvm::DeleteContainerPointers(lib_runtimes);
    return false;
  }

  // Execute the entry function.
  int run_result = reinterpret_cast<int (*)()>(entry)();
  llvm::errs() << "result: " << run_result << "\n";

  // Clean up.
  delete loader;
  llvm::DeleteContainerPointers(lib_runtimes);

  return true;
}

int main(int argc, char **argv) {
  llvm::cl::SetVersionPrinter(BCCVersionPrinter);
  llvm::cl::ParseCommandLineOptions(argc, argv);
  init::Initialize();

  BCCContext context;
  Compiler compiler;

  Script *script = PrepareScript(context, OptInputFilenames);
  if (script == NULL) {
    return EXIT_FAILURE;
  }

  if (!ConfigCompiler(compiler)) {
    return EXIT_FAILURE;
  }

  std::string OutputFilename = DetermineOutputFilename(OptOutputFilename);
  if (OutputFilename.empty()) {
    return EXIT_FAILURE;
  }

  if (!CompileScript(compiler, *script, OutputFilename)) {
    return EXIT_FAILURE;
  }

  if (OptRunEntry && !LoadAndRun(OutputFilename)) {
    return EXIT_FAILURE;
  }

  return EXIT_SUCCESS;
}
