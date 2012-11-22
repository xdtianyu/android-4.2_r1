#include <cassert>
#include <list>
#include <string>

#include "llvm/Linker.h"
#include "llvm/LLVMContext.h"
#include "llvm/Module.h"
#include "llvm/PassManager.h"

#include "llvm/ADT/OwningPtr.h"

#include "llvm/Bitcode/ReaderWriter.h"

#include "llvm/Support/CommandLine.h"
#include "llvm/Support/ManagedStatic.h"
#include "llvm/Support/MemoryBuffer.h"
#include "llvm/Support/raw_ostream.h"
#include "llvm/Transforms/IPO.h"
#include "llvm/Transforms/IPO/PassManagerBuilder.h"
#include "llvm/Support/system_error.h"

#include "llvm/Target/TargetData.h"

static llvm::cl::list<std::string>
InputFilenames(llvm::cl::Positional, llvm::cl::OneOrMore,
               llvm::cl::desc("<input bitcode files>"));

static llvm::cl::list<std::string>
OutputFilenames("o", llvm::cl::desc("Override output filename"),
                llvm::cl::value_desc("output bitcode file"));


static llvm::Module* getModuleFromFilename(std::string& Filename,
                                           llvm::LLVMContext& Ctx,
                                           std::string& ErrMsg) {
  llvm::OwningPtr<llvm::MemoryBuffer> MB;
  llvm::MemoryBuffer::getFile(Filename, MB);
  llvm::Module* M = llvm::ParseBitcodeFile(MB.get(), Ctx, &ErrMsg);
  assert(M && ErrMsg);
  return M;
}

static void optimizeModule(llvm::Module* M) {
  llvm::PassManager Passes;

  const std::string &ModuleDataLayout = M->getDataLayout();
  if (!ModuleDataLayout.empty())
    if (llvm::TargetData *TD = new llvm::TargetData(ModuleDataLayout))
      Passes.add(TD);

  Passes.add(llvm::createInternalizePass(true/* AllButMain*/));
#if 0
  FIXME REMOVE
  createStandardLTOPasses(&Passes,
                          /* Internalize = */false,
                          /* RunInliner = */true,
                          /* VerifyEach = */false);
#endif
  llvm::PassManagerBuilder PMBuilder;
  PMBuilder.populateLTOPassManager(Passes, false, true);
  Passes.run(*M);
}

static llvm::Module* linkFilesToModule(llvm::cl::list<std::string>& Inputs,
                                       llvm::LLVMContext& Ctx) {
  std::string ErrMsg;
  llvm::Module* M = getModuleFromFilename(Inputs[0], Ctx, ErrMsg);
  llvm::Linker Linker("llvm-ndk-link", M);

  for (unsigned i=1; i<Inputs.size(); ++i) {
    llvm::Module* M = getModuleFromFilename(Inputs[i], Ctx, ErrMsg);
    if (!Linker.LinkInModule(M, &ErrMsg)) {
      assert(false && ErrMsg);
    }
    optimizeModule(M);
  }
  M = Linker.releaseModule();

  llvm::PassManager PM;
  const std::string &ModuleDataLayout = M->getDataLayout();
  if (!ModuleDataLayout.empty())
    if (llvm::TargetData *TD = new llvm::TargetData(ModuleDataLayout))
      PM.add(TD);

#if 0
  FIXME REMOVE
  llvm::createStandardFunctionPasses(&PM, 3 /* OptLevel*/);
  llvm::createStandardModulePasses(&PM,
                                   3, /* OptimizationLevel */
                                   true, /* OptimizeSize */
                                   true, /* UnitAtATime */
                                   true, /* UnrollLoops */
                                   true, /* SimplifyLibCalls */
                                   false, /* HaveExceptions */
                                   NULL /* InliningPass */);
#endif

  llvm::PassManagerBuilder PMBuilder;
  //PMBuilder.OptLevel = 3;
  //PMBuilder.populateFunctionPassManager(PM);

  PMBuilder.OptLevel = 3;
  PMBuilder.SizeLevel = true;
  PMBuilder.DisableUnitAtATime = false;
  PMBuilder.DisableUnrollLoops = false;
  PMBuilder.DisableSimplifyLibCalls = false;
  PMBuilder.populateModulePassManager(PM);

  PM.run(*M);
  return M;
}

int main(int argc, char** argv) {
  llvm::llvm_shutdown_obj _ShutdownObj;
  llvm::cl::ParseCommandLineOptions(argc, argv, "P-NDK Link Tool");

  llvm::LLVMContext& Ctx = llvm::getGlobalContext();
  std::string ErrMsg;
  llvm::raw_fd_ostream FOS(OutputFilenames[0].c_str(), ErrMsg);
  assert(!FOS.has_error());

  // No need to link (just one file).
  // Output Directly.
  if (InputFilenames.size() == 1) {
    llvm::OwningPtr<llvm::Module> M(getModuleFromFilename(InputFilenames[0],
                                                           Ctx,
                                                           ErrMsg));
    llvm::WriteBitcodeToFile(M.get(), FOS);
    return 0;
  }

  llvm::OwningPtr<llvm::Module> M(linkFilesToModule(InputFilenames, Ctx));
  llvm::WriteBitcodeToFile(M.get(), FOS);
  assert(!FOS.has_error());
  return 0;
}
