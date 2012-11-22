#include "Compiler.h"

#include <cassert>
#include <cstdlib>
#include <string>
#include <vector>

#include "clang/AST/ASTConsumer.h"
#include "clang/AST/ASTContext.h"

#include "clang/Basic/DiagnosticIDs.h"
#include "clang/Basic/FileManager.h"
#include "clang/Basic/FileSystemOptions.h"
#include "clang/Basic/LangOptions.h"
#include "clang/Basic/SourceManager.h"
#include "clang/Basic/TargetInfo.h"
#include "clang/Basic/TargetOptions.h"

#include "clang/Frontend/CodeGenOptions.h"
#include "clang/Frontend/DiagnosticOptions.h"
#include "clang/Frontend/DependencyOutputOptions.h"
#include "clang/Frontend/CompilerInstance.h"
#include "clang/Frontend/FrontendDiagnostic.h"
#include "clang/Frontend/TextDiagnosticPrinter.h"
#include "clang/Frontend/Utils.h"

#include "clang/Lex/Preprocessor.h"
#include "clang/Lex/HeaderSearch.h"

#include "clang/Parse/ParseAST.h"

#include "llvm/LLVMContext.h"

#include "llvm/ADT/IntrusiveRefCntPtr.h"

#include "llvm/Bitcode/ReaderWriter.h"

#include "llvm/Support/raw_ostream.h"
#include "llvm/Support/MemoryBuffer.h"
#include "llvm/Support/ErrorHandling.h"
#include "llvm/Support/ManagedStatic.h"
#include "llvm/Support/ToolOutputFile.h"
#include "llvm/Support/Path.h"

#include "llvm/Support/TargetSelect.h"

#include "Backend.h"

namespace ndkpc {

static inline llvm::tool_output_file *openOutputFile(const char *OutputFile,
                                                     unsigned Flags,
                                                     std::string* Error,
                                                     clang::DiagnosticsEngine* Diag) {
  assert((OutputFile != NULL) && (Error != NULL) && (Diag != NULL) &&
              "Invalid parameter!");

  llvm::tool_output_file *F =
        new llvm::tool_output_file(OutputFile, *Error, Flags);
  if (F != NULL)
    return F;

  // Report error here.
  Diag->Report(clang::diag::err_fe_error_opening) << OutputFile << *Error;

  return NULL;
}

void Compiler::LLVMErrorHandler(void *UserData, const std::string &Message) {
  clang::DiagnosticsEngine* Diags = static_cast<clang::DiagnosticsEngine*>(UserData);
  Diags->Report(clang::diag::err_fe_error_backend) << Message;
  exit(1);
}

void Compiler::createDiagnostic() {
  mpDiagClient = new clang::TextDiagnosticPrinter(llvm::errs(),
                                                 clang::DiagnosticOptions());
  mDiagIDs = new clang::DiagnosticIDs();
  mDiagnostics = new clang::DiagnosticsEngine(mDiagIDs, mpDiagClient);
  initDiagnostic();
  return;
}

void Compiler::createTarget(const std::string &Triple, const std::string &CPU,
                         const std::vector<std::string> &Features) {
  if (!Triple.empty())
    mTargetOpts.Triple = Triple;
  else
    mTargetOpts.Triple = llvm::Triple::normalize(DEFAULT_TARGET_TRIPLE_STRING);

  if (!CPU.empty())
    mTargetOpts.CPU = CPU;

  if (!Features.empty())
    mTargetOpts.Features = Features;

  mTarget.reset(clang::TargetInfo::CreateTargetInfo(*mDiagnostics,
                                                    mTargetOpts));

  return;
}

void Compiler::createFileManager() {
  mFileSysOpt.reset(new clang::FileSystemOptions());
  mFileMgr.reset(new clang::FileManager(*mFileSysOpt));
}

void Compiler::createSourceManager() {
  mSourceMgr.reset(new clang::SourceManager(*mDiagnostics, *mFileMgr));
  return;
}

void Compiler::createPreprocessor() {
  clang::HeaderSearch *HS = new clang::HeaderSearch(*mFileMgr,
						    *mDiagnostics,
						    mLangOpts,
						    mTarget.get());

  llvm::OwningPtr<clang::CompilerInstance> Clang(new clang::CompilerInstance());

  mPP.reset(new clang::Preprocessor(*mDiagnostics,
                                    mLangOpts,
                                    mTarget.get(),
                                    *mSourceMgr,
                                    *HS,
				    *Clang,
                                    /* IILookup */0,
                                    /* OwnsHeaderSearch = */true,
                                    /*DelayInitialization=*/true));

  std::vector<clang::DirectoryLookup> SearchList;
  for (unsigned i = 0, e = mIncludePaths.size(); i != e; i++) {
    if (const clang::DirectoryEntry *DE =
            mFileMgr->getDirectory(mIncludePaths[i])) {
      SearchList.push_back(clang::DirectoryLookup(DE,
                                                  clang::SrcMgr::C_System,
                                                  false, /* isUser */
                                                  false /* isFramework */));
    }
  }

  HS->SetSearchPaths(SearchList, 0/* angledDirIdx FIXME CHECK */, 0/* systemDirIdx */, false/* noCurDirSearch */);

  initPreprocessor();
  return;
}

void Compiler::createASTContext() {
  mASTContext.reset(new clang::ASTContext(mLangOpts,
                                          *mSourceMgr,
                                          mTarget.get(),
                                          mPP->getIdentifierTable(),
                                          mPP->getSelectorTable(),
                                          mPP->getBuiltinInfo(),
                                          /* size_reserve = */0,
                                          /*DelayInitialization=*/true));
  initASTContext();
  return;
}

clang::ASTConsumer
*Compiler::createBackend(const clang::CodeGenOptions& CodeGenOpts,
                      llvm::raw_ostream *OS,
                      OutputType OT) {
  return new Backend(CodeGenOpts,
                     mTargetOpts,
                     mDiagnostics.getPtr(),
                     OS,
                     OT);
}

Compiler::Compiler() : mInitialized(false), mpDiagClient(NULL), mOT(OT_Default) {
}

void Compiler::injectPreDefined() {
  typedef std::map<std::string, std::string> SymbolMapTy;
  for (SymbolMapTy::iterator
          it = mPreDefinedSymbolMap.begin(), et = mPreDefinedSymbolMap.end();
       it != et; ++it) {
    std::string Str = "#define "+it->first+" "+it->second+"\n";
    mPP->setPredefines(Str);
  }
}

void Compiler::init(const std::string &Triple, const std::string &CPU,
                    const std::vector<std::string> &Features, bool isCXX) {
  mLangOpts.RTTI = 0;  // Turn off the RTTI information support
  mLangOpts.C99 = 1;
  if (isCXX) {
    mLangOpts.CPlusPlus = 1;
  }

  mCodeGenOpts.OptimizationLevel = 3;  /* -O3 */

  createDiagnostic();
  llvm::install_fatal_error_handler(LLVMErrorHandler, mDiagnostics.getPtr());

  createTarget(Triple, CPU, Features);
  createFileManager();
  createSourceManager();

  mInitialized = true;

  return;
}

bool Compiler::setInputSource(llvm::StringRef InputFile,
                              const char *Text,
                              size_t TextLength) {
  mInputFileName = InputFile.str();

  // Reset the ID tables if we are reusing the SourceManager
  mSourceMgr->clearIDTables();

  // Load the source
  llvm::MemoryBuffer *SB =
      llvm::MemoryBuffer::getMemBuffer(Text, Text + TextLength);
  mSourceMgr->createMainFileIDForMemBuffer(SB);

  if (mSourceMgr->getMainFileID().isInvalid()) {
    mDiagnostics->Report(clang::diag::err_fe_error_reading) << InputFile;
    return false;
  }
  return true;
}

bool Compiler::setInputSource(llvm::StringRef InputFile) {
  mInputFileName = InputFile.str();

  mSourceMgr->clearIDTables();

  const clang::FileEntry *File = mFileMgr->getFile(InputFile);
  if (File)
    mSourceMgr->createMainFileID(File);

  if (mSourceMgr->getMainFileID().isInvalid()) {
    mDiagnostics->Report(clang::diag::err_fe_error_reading) << InputFile;
    return false;
  }

  return true;
}

bool Compiler::setOutput(const char *OutputFile) {
  llvm::sys::Path OutputFilePath(OutputFile);
  std::string Error;
  llvm::tool_output_file *OS = NULL;

  switch (mOT) {
    case OT_Dependency:
    case OT_Assembly:
    case OT_LLVMAssembly: {
      OS = openOutputFile(OutputFile, 0, &Error, mDiagnostics.getPtr());
      break;
    }
    case OT_Nothing: {
      break;
    }
    case OT_Object:
    case OT_Bitcode: {
      OS = openOutputFile(OutputFile,
                          llvm::raw_fd_ostream::F_Binary,
                          &Error,
                          mDiagnostics.getPtr());
      break;
    }
    default: {
      llvm_unreachable("Unknown compiler output type");
    }
  }

  if (!Error.empty())
    return false;

  mOS.reset(OS);

  mOutputFileName = OutputFile;

  return true;
}

int Compiler::compile() {
  if (mDiagnostics->hasErrorOccurred())
    return 1;
  if (mOS.get() == NULL)
    return 1;

  // Here is per-compilation needed initialization
  createPreprocessor();
  createASTContext();

  mBackend.reset(createBackend(mCodeGenOpts, &mOS->os(), mOT));

  // Inform the diagnostic client we are processing a source file
  mpDiagClient->BeginSourceFile(mLangOpts, mPP.get());

  if (mLangOpts.CPlusPlus == 1) {
    mPP->setPredefines("#define __cplusplus\n");
  }

  this->injectPreDefined();

  // The core of the slang compiler
  ParseAST(*mPP, mBackend.get(), *mASTContext);

  // Inform the diagnostic client we are done with previous source file
  mpDiagClient->EndSourceFile();

  // Declare success if no error
  if (!mDiagnostics->hasErrorOccurred())
    mOS->keep();

  // The compilation ended, clear
  mBackend.reset();
  mASTContext.reset();
  mPP.reset();
  mOS.reset();

  return mDiagnostics->hasErrorOccurred() ? 1 : 0;
}

void Compiler::reset() {
  mDiagnostics->Reset();
  return;
}

Compiler::~Compiler() {
  llvm::llvm_shutdown();
  return;
}

}
