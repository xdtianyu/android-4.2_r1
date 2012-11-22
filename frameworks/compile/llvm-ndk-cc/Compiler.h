#ifndef _LLVM_NDK_CC_COMPILER_H
#define _LLVM_NDK_CC_COMPILER_H

#include <cstdio>
#include <map>
#include <string>
#include <vector>

#include "clang/Basic/DiagnosticIDs.h"
#include "clang/Basic/LangOptions.h"
#include "clang/Basic/TargetOptions.h"

#include "clang/Frontend/CodeGenOptions.h"

#include "llvm/ADT/IntrusiveRefCntPtr.h"
#include "llvm/ADT/OwningPtr.h"
#include "llvm/ADT/StringRef.h"

namespace llvm {
  class tool_output_file;
  class raw_ostream;
}

namespace clang {
  class Diagnostic;
  class DiagnosticClient;
  class FileManager;
  class FileSystemOptions;
  class SourceManager;
  class Preprocessor;
  class TargetOptions;
  class ASTContext;
  class ASTConsumer;
  class Backend;
  class TargetInfo;
  class TextDiagnosticPrinter;
}

namespace ndkpc {

#define DEFAULT_TARGET_TRIPLE_STRING "armv7-none-linux-gnueabi"

class Compiler {
public:
  typedef enum {
    OT_Dependency,
    OT_Assembly,
    OT_LLVMAssembly,
    OT_Bitcode,
    OT_Nothing,
    OT_Object,

    OT_Default = OT_Bitcode
  } OutputType;

  Compiler();
  ~Compiler();

  void init(const std::string &Triple, const std::string &CPU,
            const std::vector<std::string> &Features,
            bool isCXX);

  bool setInputSource(llvm::StringRef InputFile, const char *Text,
                      size_t TextLength);

  bool setInputSource(llvm::StringRef InputFile);

  inline const std::string &getInputFileName() const { return mInputFileName; }

  inline void setIncludePaths(const std::vector<std::string> &IncludePaths) {
    mIncludePaths = IncludePaths;
  }

  inline void setPreDefinedSymbol(const std::map<std::string, std::string>& M) {
    mPreDefinedSymbolMap = M;
  }

  inline void setOutputType(OutputType OT) { mOT = OT; }

  bool setOutput(const char *OutputFile);
  inline const std::string &getOutputFileName() const {
    return mOutputFileName;
  }

  int compile();

  // Reset the slang compiler state such that it can be reused to compile
  // another file
  void reset();

private:
  clang::LangOptions mLangOpts;
  clang::CodeGenOptions mCodeGenOpts;

  static void LLVMErrorHandler(void *UserData, const std::string &Message);

  bool mInitialized;

  // The diagnostics engine instance (for status reporting during compilation)
  llvm::IntrusiveRefCntPtr<clang::DiagnosticsEngine> mDiagnostics;
  // The diagnostics id
  llvm::IntrusiveRefCntPtr<clang::DiagnosticIDs> mDiagIDs;
  // The clients of diagnostics engine. The ownership is taken by the
  // mDiagnostics after creation.
  clang::TextDiagnosticPrinter *mpDiagClient;
  void createDiagnostic();

  // The target being compiled for
  clang::TargetOptions mTargetOpts;
  llvm::OwningPtr<clang::TargetInfo> mTarget;
  void createTarget(const std::string &Triple, const std::string &CPU,
                    const std::vector<std::string> &Features);

  // Below is for parsing and code generation

  // The file manager (for prepocessor doing the job such as header file search)
  llvm::OwningPtr<clang::FileManager> mFileMgr;
  llvm::OwningPtr<clang::FileSystemOptions> mFileSysOpt;
  void createFileManager();

  // The source manager (responsible for the source code handling)
  llvm::OwningPtr<clang::SourceManager> mSourceMgr;
  void createSourceManager();

  // The preprocessor (source code preprocessor)
  llvm::OwningPtr<clang::Preprocessor> mPP;
  void createPreprocessor();

  // The AST context (the context to hold long-lived AST nodes)
  llvm::OwningPtr<clang::ASTContext> mASTContext;
  void createASTContext();

  // The AST consumer, responsible for code generation
  llvm::OwningPtr<clang::ASTConsumer> mBackend;

  // Input file name
  std::string mInputFileName;
  std::string mOutputFileName;

  OutputType mOT;

  // Output stream
  llvm::OwningPtr<llvm::tool_output_file> mOS;

  std::vector<std::string> mIncludePaths;

  std::map<std::string, std::string> mPreDefinedSymbolMap;
  void injectPreDefined();

  void initDiagnostic() {}
  void initPreprocessor() {}
  void initASTContext() {}

  clang::ASTConsumer *createBackend(const clang::CodeGenOptions& CodeGenOpts,
                                    llvm::raw_ostream *OS,
                                    OutputType OT);
};

}

#endif // _LLVM_NDK_CC_COMPILER_H
