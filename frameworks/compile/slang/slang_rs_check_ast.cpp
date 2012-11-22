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

#include "slang_rs_check_ast.h"

#include "slang_assert.h"
#include "slang_rs.h"
#include "slang_rs_export_type.h"

namespace slang {

void RSCheckAST::VisitStmt(clang::Stmt *S) {
  // This function does the actual iteration through all sub-Stmt's within
  // a given Stmt. Note that this function is skipped by all of the other
  // Visit* functions if we have already found a higher-level match.
  for (clang::Stmt::child_iterator I = S->child_begin(), E = S->child_end();
       I != E;
       I++) {
    if (clang::Stmt *Child = *I) {
      Visit(Child);
    }
  }
}

void RSCheckAST::ValidateFunctionDecl(clang::FunctionDecl *FD) {
  if (!FD) {
    return;
  }

  if (!mIsFilterscript) {
    // No additional validation for non-Filterscript functions.
    if (clang::Stmt *Body = FD->getBody()) {
      Visit(Body);
    }
    return;
  }

  size_t numParams = FD->getNumParams();

  clang::QualType resultType = FD->getResultType().getCanonicalType();

  // We use FD as our NamedDecl in the case of a bad return type.
  if (!RSExportType::ValidateType(C, resultType, FD,
                                  FD->getLocStart(), mTargetAPI,
                                  mIsFilterscript)) {
    mValid = false;
  }

  for (size_t i = 0; i < numParams; i++) {
    clang::ParmVarDecl *PVD = FD->getParamDecl(i);
    clang::QualType QT = PVD->getType().getCanonicalType();
    if (!RSExportType::ValidateType(C, QT, PVD, PVD->getLocStart(),
                                    mTargetAPI, mIsFilterscript)) {
      mValid = false;
    }
  }

  if (clang::Stmt *Body = FD->getBody()) {
    Visit(Body);
  }
}


void RSCheckAST::ValidateVarDecl(clang::VarDecl *VD) {
  if (!VD) {
    return;
  }

  const clang::Type *T = VD->getType().getTypePtr();

  if (VD->getLinkage() == clang::ExternalLinkage) {
    llvm::StringRef TypeName;
    if (!RSExportType::NormalizeType(T, TypeName, &mDiagEngine, VD)) {
      mValid = false;
    }
  }

  if (!RSExportType::ValidateVarDecl(VD, mTargetAPI, mIsFilterscript)) {
    mValid = false;
  } else if (clang::Expr *Init = VD->getInit()) {
    // Only check the initializer if the decl is already ok.
    Visit(Init);
  }
}


void RSCheckAST::VisitDeclStmt(clang::DeclStmt *DS) {
  if (!SlangRS::IsLocInRSHeaderFile(DS->getLocStart(), mSM)) {
    for (clang::DeclStmt::decl_iterator I = DS->decl_begin(),
                                        E = DS->decl_end();
         I != E;
         ++I) {
      if (clang::VarDecl *VD = llvm::dyn_cast<clang::VarDecl>(*I)) {
        ValidateVarDecl(VD);
      } else if (clang::FunctionDecl *FD =
            llvm::dyn_cast<clang::FunctionDecl>(*I)) {
        ValidateFunctionDecl(FD);
      }
    }
  }
}


void RSCheckAST::VisitExpr(clang::Expr *E) {
  // This is where FS checks for code using pointer and/or 64-bit expressions
  // (i.e. things like casts).

  // First we skip implicit casts (things like function calls and explicit
  // array accesses rely heavily on them and they are valid.
  E = E->IgnoreImpCasts();
  if (mIsFilterscript &&
      !SlangRS::IsLocInRSHeaderFile(E->getExprLoc(), mSM) &&
      !RSExportType::ValidateType(C, E->getType(), NULL, E->getExprLoc(),
                                  mTargetAPI, mIsFilterscript)) {
    mValid = false;
  } else {
    // Only visit sub-expressions if we haven't already seen a violation.
    VisitStmt(E);
  }
}


bool RSCheckAST::Validate() {
  clang::TranslationUnitDecl *TUDecl = C.getTranslationUnitDecl();
  for (clang::DeclContext::decl_iterator DI = TUDecl->decls_begin(),
          DE = TUDecl->decls_end();
       DI != DE;
       DI++) {
    if (!SlangRS::IsLocInRSHeaderFile(DI->getLocStart(), mSM)) {
      if (clang::VarDecl *VD = llvm::dyn_cast<clang::VarDecl>(*DI)) {
        ValidateVarDecl(VD);
      } else if (clang::FunctionDecl *FD =
            llvm::dyn_cast<clang::FunctionDecl>(*DI)) {
        ValidateFunctionDecl(FD);
      } else if (clang::Stmt *Body = (*DI)->getBody()) {
        Visit(Body);
      }
    }
  }

  return mValid;
}

}  // namespace slang
