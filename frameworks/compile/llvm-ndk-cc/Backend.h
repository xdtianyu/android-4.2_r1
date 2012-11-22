#ifndef _LLVM_NDK_CC_BACKEND_H
#define _LLVM_NDK_CC_BACKEND_H

#include "clang/AST/ASTConsumer.h"

#include "llvm/PassManager.h"

#include "llvm/Support/FormattedStream.h"
#include "llvm/Transforms/IPO/PassManagerBuilder.h"

#include "Compiler.h"

namespace llvm {
  class formatted_raw_ostream;
  class LLVMContext;
  class NamedMDNode;
  class Module;
  class PassManager;
  class FunctionPassManager;
}

namespace clang {
  class CodeGenOptions;
  class CodeGenerator;
  class DeclGroupRef;
  class TagDecl;
  class VarDecl;
}

namespace ndkpc {

class Backend : public clang::ASTConsumer {
public:
  Backend(const clang::CodeGenOptions &CodeGenOpts,
          const clang::TargetOptions &TargetOpts,
          clang::DiagnosticsEngine *Diags,
          llvm::raw_ostream *OS,
          Compiler::OutputType OT);

  virtual ~Backend();

  // Initialize - This is called to initialize the consumer, providing the
  // ASTContext.
  virtual void Initialize(clang::ASTContext &Ctx);

  // HandleTopLevelDecl - Handle the specified top-level declaration.  This is
  // called by the parser to process every top-level Decl*. Note that D can be
  // the head of a chain of Decls (e.g. for `int a, b` the chain will have two
  // elements). Use Decl::getNextDeclarator() to walk the chain.
  virtual bool HandleTopLevelDecl(clang::DeclGroupRef D);

  // HandleTranslationUnit - This method is called when the ASTs for entire
  // translation unit have been parsed.
  virtual void HandleTranslationUnit(clang::ASTContext &Ctx);

  // HandleTagDeclDefinition - This callback is invoked each time a TagDecl
  // (e.g. struct, union, enum, class) is completed.  This allows the client to
  // hack on the type, which can occur at any point in the file (because these
  // can be defined in declspecs).
  virtual void HandleTagDeclDefinition(clang::TagDecl *D);

  // CompleteTentativeDefinition - Callback invoked at the end of a translation
  // unit to notify the consumer that the given tentative definition should be
  // completed.
  virtual void CompleteTentativeDefinition(clang::VarDecl *D);

private:
  const clang::CodeGenOptions &mCodeGenOpts;
  const clang::TargetOptions &mTargetOpts;

  llvm::LLVMContext &mLLVMContext;
  clang::DiagnosticsEngine &mDiags;

  llvm::Module *mpModule;

  // Output stream
  llvm::raw_ostream *mpOS;
  Compiler::OutputType mOT;

  // This helps us translate Clang AST using into LLVM IR
  clang::CodeGenerator *mpGen;

  // Passes apply on function scope in a translation unit
  llvm::FunctionPassManager *mpPerFunctionPasses;
  void CreateFunctionPasses();

  // Passes apply on module scope
  llvm::PassManager *mpPerModulePasses;
  void CreateModulePasses();

  // Passes for code emission
  llvm::FunctionPassManager *mpCodeGenPasses;
  bool CreateCodeGenPasses();

  llvm::formatted_raw_ostream FormattedOutStream;
};

}

#endif //  _LLVM_NDK_CC_BACKEND_H
