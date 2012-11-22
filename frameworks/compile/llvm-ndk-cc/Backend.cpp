#include "Backend.h"

#include <cassert>
#include <string>
#include <vector>

#include "clang/AST/ASTContext.h"
#include "clang/AST/Decl.h"
#include "clang/AST/DeclGroup.h"

#include "clang/Basic/Diagnostic.h"
#include "clang/Basic/TargetInfo.h"
#include "clang/Basic/TargetOptions.h"

#include "clang/CodeGen/ModuleBuilder.h"

#include "clang/Frontend/CodeGenOptions.h"
#include "clang/Frontend/FrontendDiagnostic.h"

#include "llvm/Assembly/PrintModulePass.h"

#include "llvm/Bitcode/ReaderWriter.h"

#include "llvm/CodeGen/RegAllocRegistry.h"
#include "llvm/CodeGen/SchedulerRegistry.h"

#include "llvm/Instructions.h"
#include "llvm/LLVMContext.h"
#include "llvm/Module.h"
#include "llvm/Metadata.h"

#include "llvm/MC/SubtargetFeature.h"

#include "llvm/Support/Casting.h"
#include "llvm/Support/InstIterator.h"

#include "llvm/Target/TargetData.h"
#include "llvm/Target/TargetMachine.h"
#include "llvm/Target/TargetOptions.h"
#include "llvm/Support/TargetRegistry.h"

namespace ndkpc {

void Backend::CreateFunctionPasses() {
  if (!mpPerFunctionPasses) {
    mpPerFunctionPasses = new llvm::FunctionPassManager(mpModule);
    mpPerFunctionPasses->add(new llvm::TargetData(mpModule));

    // FIXME REMOVE
    //llvm::createStandardFunctionPasses(mpPerFunctionPasses,
    //                                   mCodeGenOpts.OptimizationLevel);
    llvm::PassManagerBuilder PMBuilder;
    PMBuilder.OptLevel = mCodeGenOpts.OptimizationLevel;
    PMBuilder.populateFunctionPassManager(*mpPerFunctionPasses);
  }
  return;
}

void Backend::CreateModulePasses() {
  if (!mpPerModulePasses) {
	  mpPerModulePasses = new llvm::PassManager();
	  mpPerModulePasses->add(new llvm::TargetData(mpModule));

	  llvm::PassManagerBuilder PMBuilder;
	  PMBuilder.OptLevel = mCodeGenOpts.OptimizationLevel;
	  PMBuilder.SizeLevel = mCodeGenOpts.OptimizeSize;
	  if (mCodeGenOpts.UnitAtATime) {
		  PMBuilder.DisableUnitAtATime = 0;
	  } else {
		  PMBuilder.DisableUnitAtATime = 1;
	  }

	  if (mCodeGenOpts.UnrollLoops) {
		  PMBuilder.DisableUnrollLoops = 0;
	  } else {
		  PMBuilder.DisableUnrollLoops = 1;
	  }

	  PMBuilder.DisableSimplifyLibCalls = false;
	  PMBuilder.populateModulePassManager(*mpPerModulePasses);
  }
  return;
}

bool Backend::CreateCodeGenPasses() {
  if ((mOT != Compiler::OT_Assembly) && (mOT != Compiler::OT_Object))
    return true;

  // Now we add passes for code emitting
  if (mpCodeGenPasses) {
    return true;
  } else {
    mpCodeGenPasses = new llvm::FunctionPassManager(mpModule);
    mpCodeGenPasses->add(new llvm::TargetData(mpModule));
  }

  // Create the TargetMachine for generating code.
  std::string Triple = mpModule->getTargetTriple();

  std::string Error;
  const llvm::Target* TargetInfo =
      llvm::TargetRegistry::lookupTarget(Triple, Error);
  if (TargetInfo == NULL) {
    mDiags.Report(clang::diag::err_fe_unable_to_create_target) << Error;
    return false;
  }

  llvm::TargetOptions Options;
  Options.NoFramePointerElim = mCodeGenOpts.DisableFPElim;

  // Use hardware FPU.
  //
  // FIXME: Need to detect the CPU capability and decide whether to use softfp.
  // To use softfp, change following 2 lines to
  //
  //  llvm::FloatABIType = llvm::FloatABI::Soft;
  //  llvm::UseSoftFloat = true;
  Options.FloatABIType = llvm::FloatABI::Hard;
  Options.UseSoftFloat = false;

  // BCC needs all unknown symbols resolved at compilation time. So we don't
  // need any relocation model.
  llvm::Reloc::Model RelocModel = llvm::Reloc::Static;

  // This is set for the linker (specify how large of the virtual addresses we
  // can access for all unknown symbols.)
  llvm::CodeModel::Model CodeModel;
  if (mpModule->getPointerSize() == llvm::Module::Pointer32)
    CodeModel = llvm::CodeModel::Small;
  else
    // The target may have pointer size greater than 32 (e.g. x86_64
    // architecture) may need large data address model
    CodeModel = llvm::CodeModel::Medium;

  // Setup feature string
  std::string FeaturesStr;
  if (mTargetOpts.CPU.size() || mTargetOpts.Features.size()) {
    llvm::SubtargetFeatures Features;

    for (std::vector<std::string>::const_iterator
             I = mTargetOpts.Features.begin(), E = mTargetOpts.Features.end();
         I != E;
         I++)
      Features.AddFeature(*I);

    FeaturesStr = Features.getString();
  }
  // Setup optimization level
  llvm::CodeGenOpt::Level OptLevel = llvm::CodeGenOpt::Default;
  if (mCodeGenOpts.OptimizationLevel == 0)
    OptLevel = llvm::CodeGenOpt::None;
  else if (mCodeGenOpts.OptimizationLevel == 3)
    OptLevel = llvm::CodeGenOpt::Aggressive;

  llvm::TargetMachine *TM =
      TargetInfo->createTargetMachine(Triple, mTargetOpts.CPU, FeaturesStr, 
				      Options, RelocModel, CodeModel, OptLevel);

  // Register scheduler
  llvm::RegisterScheduler::setDefault(llvm::createDefaultScheduler);

  // Register allocation policy:
  //  createFastRegisterAllocator: fast but bad quality
  //  createLinearScanRegisterAllocator: not so fast but good quality
  llvm::RegisterRegAlloc::setDefault((mCodeGenOpts.OptimizationLevel == 0) ?
                                     llvm::createFastRegisterAllocator :
                                     llvm::createGreedyRegisterAllocator);

  llvm::TargetMachine::CodeGenFileType CGFT =
      llvm::TargetMachine::CGFT_AssemblyFile;
  if (mOT == Compiler::OT_Object)
    CGFT = llvm::TargetMachine::CGFT_ObjectFile;
  if (TM->addPassesToEmitFile(*mpCodeGenPasses, FormattedOutStream,
                              CGFT, OptLevel)) {
    mDiags.Report(clang::diag::err_fe_unable_to_interface_with_target);
    return false;
  }

  return true;
}

Backend::Backend(const clang::CodeGenOptions &CodeGenOpts,
                 const clang::TargetOptions &TargetOpts,
                 clang::DiagnosticsEngine *Diags,
                 llvm::raw_ostream *OS,
                 Compiler::OutputType OT)
    : ASTConsumer(),
      mCodeGenOpts(CodeGenOpts),
      mTargetOpts(TargetOpts),
      mLLVMContext(llvm::getGlobalContext()),
      mDiags(*Diags),
      mpModule(NULL),
      mpOS(OS),
      mOT(OT),
      mpGen(NULL),
      mpPerFunctionPasses(NULL),
      mpPerModulePasses(NULL),
      mpCodeGenPasses(NULL) {
  FormattedOutStream.setStream(*mpOS,
                               llvm::formatted_raw_ostream::PRESERVE_STREAM);
  mpGen = CreateLLVMCodeGen(mDiags, "", mCodeGenOpts, mLLVMContext);
  return;
}

void Backend::Initialize(clang::ASTContext &Ctx) {
  mpGen->Initialize(Ctx);
  mpModule = mpGen->GetModule();
  return;
}

bool Backend::HandleTopLevelDecl(clang::DeclGroupRef D) {
  mpGen->HandleTopLevelDecl(D);
  return true;
}

void Backend::HandleTranslationUnit(clang::ASTContext &Ctx) {
  mpGen->HandleTranslationUnit(Ctx);

  // Here, we complete a translation unit (whole translation unit is now in LLVM
  // IR). Now, interact with LLVM backend to generate actual machine code (asm
  // or machine code, whatever.)

  // Silently ignore if we weren't initialized for some reason.
  if (!mpModule)
    return;

  llvm::Module *M = mpGen->ReleaseModule();
  if (!M) {
    // The module has been released by IR gen on failures, do not double free.
    mpModule = NULL;
    return;
  }

  assert(mpModule == M &&
              "Unexpected module change during LLVM IR generation");

  // Handle illigal CallSite
  for (llvm::Module::iterator I = mpModule->begin(), E = mpModule->end();
       I != E;
       ++I) {
    for (llvm::inst_iterator i = llvm::inst_begin(*I), e = llvm::inst_end(*I);
         i != e;
         ++i) {
      if (llvm::CallInst* CallInst = llvm::dyn_cast<llvm::CallInst>(&*i)) {
        if (CallInst->isInlineAsm()) {
          // TODO: Should we reflect source location information to diagnostic
          //       class and show to users?
          llvm::errs() << "Inline assembly is illigal. Please don't use it." << "\n";
          exit(1);
        }
      }
    }
  }

  // Create and run per-function passes
  CreateFunctionPasses();
  if (mpPerFunctionPasses) {
    mpPerFunctionPasses->doInitialization();

    for (llvm::Module::iterator I = mpModule->begin(), E = mpModule->end();
         I != E;
         I++)
      if (!I->isDeclaration())
        mpPerFunctionPasses->run(*I);

    mpPerFunctionPasses->doFinalization();
  }

  // Create and run module passes
  CreateModulePasses();
  if (mpPerModulePasses)
    mpPerModulePasses->run(*mpModule);

  switch (mOT) {
    case Compiler::OT_Assembly:
    case Compiler::OT_Object: {
      if (!CreateCodeGenPasses())
        return;

      mpCodeGenPasses->doInitialization();

      for (llvm::Module::iterator I = mpModule->begin(), E = mpModule->end();
          I != E;
          I++)
        if (!I->isDeclaration())
          mpCodeGenPasses->run(*I);

      mpCodeGenPasses->doFinalization();
      break;
    }
    case Compiler::OT_LLVMAssembly: {
      llvm::PassManager *LLEmitPM = new llvm::PassManager();
      LLEmitPM->add(llvm::createPrintModulePass(&FormattedOutStream));
      LLEmitPM->run(*mpModule);
      break;
    }
    case Compiler::OT_Bitcode: {
      llvm::PassManager *BCEmitPM = new llvm::PassManager();
      BCEmitPM->add(llvm::createBitcodeWriterPass(FormattedOutStream));
      BCEmitPM->run(*mpModule);
      break;
    }
    case Compiler::OT_Nothing: {
      return;
    }
    default: {
      assert(false && "Unknown output type");
    }
  }

  FormattedOutStream.flush();
  return;
}

void Backend::HandleTagDeclDefinition(clang::TagDecl *D) {
  mpGen->HandleTagDeclDefinition(D);
  return;
}

void Backend::CompleteTentativeDefinition(clang::VarDecl *D) {
  mpGen->CompleteTentativeDefinition(D);
  return;
}

Backend::~Backend() {
  delete mpModule;
  delete mpGen;
  delete mpPerFunctionPasses;
  delete mpPerModulePasses;
  delete mpCodeGenPasses;
  return;
}

}
