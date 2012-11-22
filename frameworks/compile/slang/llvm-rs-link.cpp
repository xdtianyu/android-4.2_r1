/*
 * Copyright 2010, The Android Open Source Project
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

#include <list>
#include <memory>
#include <string>
#include <vector>

#include "llvm/Analysis/Verifier.h"

#include "llvm/Bitcode/ReaderWriter.h"

#include "llvm/Linker.h"
#include "llvm/LLVMContext.h"
#include "llvm/Metadata.h"
#include "llvm/Module.h"

#include "llvm/Support/CommandLine.h"
#include "llvm/Support/ManagedStatic.h"
#include "llvm/Support/MemoryBuffer.h"
#include "llvm/Support/raw_ostream.h"
#include "llvm/Support/ToolOutputFile.h"
#include "llvm/Support/system_error.h"

#include "llvm/PassManager.h"
#include "llvm/Transforms/IPO.h"
#include "llvm/Transforms/IPO/PassManagerBuilder.h"

#include "llvm/Target/TargetData.h"

#include "slang_rs_metadata.h"

using llvm::errs;
using llvm::LLVMContext;
using llvm::MemoryBuffer;
using llvm::Module;

static llvm::cl::list<std::string>
InputFilenames(llvm::cl::Positional, llvm::cl::OneOrMore,
               llvm::cl::desc("<input bitcode files>"));

static llvm::cl::list<std::string>
OutputFilenames("o", llvm::cl::desc("Override output filename"),
                llvm::cl::value_desc("<output bitcode file>"));

static llvm::cl::opt<bool>
NoStdLib("nostdlib", llvm::cl::desc("Don't link RS default libraries"));

static llvm::cl::list<std::string>
    AdditionalLibs("l", llvm::cl::Prefix,
                   llvm::cl::desc("Specify additional libraries to link to"),
                   llvm::cl::value_desc("<library bitcode>"));

static bool GetExportSymbolNames(llvm::NamedMDNode *N,
                                 unsigned NameOpIdx,
                                 std::vector<const char *> &Names) {
  if (N == NULL)
    return true;

  for (unsigned i = 0, e = N->getNumOperands(); i != e; ++i) {
    llvm::MDNode *V = N->getOperand(i);
    if (V == NULL)
      continue;

    if (V->getNumOperands() < (NameOpIdx + 1)) {
      errs() << "Invalid metadata spec of " << N->getName()
             << " in Renderscript executable. (#op)\n";
      return false;
    }

    llvm::MDString *Name =
        llvm::dyn_cast<llvm::MDString>(V->getOperand(NameOpIdx));
    if (Name == NULL) {
      errs() << "Invalid metadata spec of " << N->getName()
             << " in Renderscript executable. (#name)\n";
      return false;
    }

    Names.push_back(Name->getString().data());
  }
  return true;
}

static bool GetExportSymbols(Module *M, std::vector<const char *> &Names) {
  bool Result = true;
  // Variables marked as export must be externally visible
  if (llvm::NamedMDNode *EV = M->getNamedMetadata(RS_EXPORT_VAR_MN))
    Result |= GetExportSymbolNames(EV, RS_EXPORT_VAR_NAME, Names);
  // So are those exported functions
  if (llvm::NamedMDNode *EF = M->getNamedMetadata(RS_EXPORT_FUNC_MN))
    Result |= GetExportSymbolNames(EF, RS_EXPORT_FUNC_NAME, Names);
  return Result;
}

static inline MemoryBuffer *LoadFileIntoMemory(const std::string &F) {
  llvm::OwningPtr<MemoryBuffer> MB;

  if (llvm::error_code EC = MemoryBuffer::getFile(F, MB)) {
    errs() << "Failed to load `" << F << "' (" + EC.message() + ")\n";
  }

  return MB.take();
}

static inline Module *ParseBitcodeFromMemoryBuffer(MemoryBuffer *MB,
                                                   LLVMContext& Context) {
  std::string Err;
  Module *M = ParseBitcodeFile(MB, Context, &Err);

  if (M == NULL)
    errs() << "Corrupted bitcode file `" << MB->getBufferIdentifier()
           <<  "' (" << Err << ")\n";

  return M;
}

// LoadBitcodeFile - Read the specified bitcode file in and return it.
static inline Module *LoadBitcodeFile(const std::string &F,
                                      LLVMContext& Context) {
  MemoryBuffer *MB = LoadFileIntoMemory(F);
  if (MB == NULL)
    return NULL;

  Module *M = ParseBitcodeFromMemoryBuffer(MB, Context);
  if (M == NULL)
    delete MB;

  return M;
}

extern const char rslib_bc[];
extern unsigned rslib_bc_size;

static bool PreloadLibraries(bool NoStdLib,
                             const std::vector<std::string> &AdditionalLibs,
                             std::list<MemoryBuffer *> &LibBitcode) {
  MemoryBuffer *MB;

  LibBitcode.clear();

  if (!NoStdLib) {
    // rslib.bc
    MB = MemoryBuffer::getMemBuffer(llvm::StringRef(rslib_bc, rslib_bc_size),
                                    "rslib.bc");
    if (MB == NULL) {
      errs() << "Failed to load (in-memory) `rslib.bc'!\n";
      return false;
    }

    LibBitcode.push_back(MB);
  }

  // Load additional libraries
  for (std::vector<std::string>::const_iterator
          I = AdditionalLibs.begin(), E = AdditionalLibs.end();
       I != E;
       I++) {
    MB = LoadFileIntoMemory(*I);
    if (MB == NULL)
      return false;
    LibBitcode.push_back(MB);
  }

  return true;
}

static void UnloadLibraries(std::list<MemoryBuffer *>& LibBitcode) {
  for (std::list<MemoryBuffer *>::iterator
          I = LibBitcode.begin(), E = LibBitcode.end();
       I != E;
       I++)
    delete *I;
  LibBitcode.clear();
  return;
}

Module *PerformLinking(const std::string &InputFile,
                       const std::list<MemoryBuffer *> &LibBitcode,
                       LLVMContext &Context) {
  std::string Err;
  std::auto_ptr<Module> Composite(LoadBitcodeFile(InputFile, Context));

  if (Composite.get() == NULL)
    return NULL;

  for (std::list<MemoryBuffer *>::const_iterator I = LibBitcode.begin(),
          E = LibBitcode.end();
       I != E;
       I++) {
    Module *Lib = ParseBitcodeFromMemoryBuffer(*I, Context);
    if (Lib == NULL)
      return NULL;

    if (llvm::Linker::LinkModules(Composite.get(), Lib,
                                  llvm::Linker::DestroySource, &Err)) {
      errs() << "Failed to link `" << InputFile << "' with library bitcode `"
             << (*I)->getBufferIdentifier() << "' (" << Err << ")\n";
      return NULL;
    }
  }

  return Composite.release();
}

bool OptimizeModule(Module *M) {
  llvm::PassManager Passes;

  const std::string &ModuleDataLayout = M->getDataLayout();
  if (!ModuleDataLayout.empty())
    if (llvm::TargetData *TD = new llvm::TargetData(ModuleDataLayout))
      Passes.add(TD);

  // Some symbols must not be internalized
  std::vector<const char *> ExportList;
  ExportList.push_back("init");
  ExportList.push_back("root");
  ExportList.push_back(".rs.dtor");

  if (!GetExportSymbols(M, ExportList)) {
    return false;
  }

  Passes.add(llvm::createInternalizePass(ExportList));

  // TODO(sliao): Do we need to run all LTO passes?
  llvm::PassManagerBuilder PMBuilder;
  PMBuilder.populateLTOPassManager(Passes,
                                   /* Internalize = */false,
                                   /* RunInliner = */true);
  Passes.run(*M);

  return true;
}

int main(int argc, char **argv) {
  llvm::llvm_shutdown_obj X;  // Call llvm_shutdown() on exit.

  llvm::cl::ParseCommandLineOptions(argc, argv, "llvm-rs-link\n");

  std::list<MemoryBuffer *> LibBitcode;

  if (!PreloadLibraries(NoStdLib, AdditionalLibs, LibBitcode))
    return 1;

  // No libraries specified to be linked
  if (LibBitcode.size() == 0)
    return 0;

  LLVMContext &Context = llvm::getGlobalContext();
  bool HasError = true;
  std::string Err;

  for (unsigned i = 0, e = InputFilenames.size(); i != e; i++) {
    std::auto_ptr<Module> Linked(
        PerformLinking(InputFilenames[i], LibBitcode, Context));

    // Failed to link with InputFilenames[i] with LibBitcode
    if (Linked.get() == NULL)
      break;

    // Verify linked module
    if (verifyModule(*Linked, llvm::ReturnStatusAction, &Err)) {
      errs() << InputFilenames[i] << " linked, but does not verify as "
                                     "correct! (" << Err << ")\n";
      break;
    }

    if (!OptimizeModule(Linked.get()))
      break;

    // Write out the module
    llvm::tool_output_file Out(InputFilenames[i].c_str(), Err,
                               llvm::raw_fd_ostream::F_Binary);

    if (!Err.empty()) {
      errs() << InputFilenames[i] << " linked, but failed to write out! "
                                     "(" << Err << ")\n";
      break;
    }

    WriteBitcodeToFile(Linked.get(), Out.os());

    Out.keep();
    Linked.reset();

    if (i == (InputFilenames.size() - 1))
      // This is the last file and no error occured.
      HasError = false;
  }

  UnloadLibraries(LibBitcode);

  return HasError;
}
