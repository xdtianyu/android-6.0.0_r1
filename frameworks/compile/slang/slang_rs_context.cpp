/*
 * Copyright 2010-2012, The Android Open Source Project
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

#include "slang_rs_context.h"

#include <string>

#include "clang/AST/ASTContext.h"
#include "clang/AST/Decl.h"
#include "clang/AST/DeclBase.h"
#include "clang/AST/Mangle.h"
#include "clang/AST/Type.h"

#include "clang/Basic/Linkage.h"
#include "clang/Basic/TargetInfo.h"

#include "llvm/IR/LLVMContext.h"
#include "llvm/IR/DataLayout.h"

#include "slang.h"
#include "slang_assert.h"
#include "slang_rs_export_foreach.h"
#include "slang_rs_export_func.h"
#include "slang_rs_export_type.h"
#include "slang_rs_export_var.h"
#include "slang_rs_exportable.h"
#include "slang_rs_pragma_handler.h"
#include "slang_rs_reflection.h"

namespace slang {

RSContext::RSContext(clang::Preprocessor &PP,
                     clang::ASTContext &Ctx,
                     const clang::TargetInfo &Target,
                     PragmaList *Pragmas,
                     unsigned int TargetAPI,
                     bool Verbose)
    : mPP(PP),
      mCtx(Ctx),
      mPragmas(Pragmas),
      mTargetAPI(TargetAPI),
      mVerbose(Verbose),
      mDataLayout(nullptr),
      mLLVMContext(llvm::getGlobalContext()),
      mLicenseNote(nullptr),
      mRSPackageName("android.renderscript"),
      version(0),
      mMangleCtx(Ctx.createMangleContext()),
      mIs64Bit(Target.getPointerWidth(0) == 64) {

  AddPragmaHandlers(PP, this);

  // Prepare target data
  mDataLayout = new llvm::DataLayout(Target.getTargetDescription());
}

bool RSContext::processExportVar(const clang::VarDecl *VD) {
  slangAssert(!VD->getName().empty() && "Variable name should not be empty");

  // TODO(zonr): some check on variable

  RSExportType *ET = RSExportType::CreateFromDecl(this, VD);
  if (!ET)
    return false;

  RSExportVar *EV = new RSExportVar(this, VD, ET);
  if (EV == nullptr)
    return false;
  else
    mExportVars.push_back(EV);

  return true;
}

bool RSContext::processExportFunc(const clang::FunctionDecl *FD) {
  slangAssert(!FD->getName().empty() && "Function name should not be empty");

  if (!FD->isThisDeclarationADefinition()) {
    return true;
  }

  if (FD->getStorageClass() != clang::SC_None) {
    fprintf(stderr, "RSContext::processExportFunc : cannot export extern or "
                    "static function '%s'\n", FD->getName().str().c_str());
    return false;
  }

  if (RSExportForEach::isSpecialRSFunc(mTargetAPI, FD)) {
    // Do not reflect specialized functions like init, dtor, or graphics root.
    return RSExportForEach::validateSpecialFuncDecl(mTargetAPI, this, FD);
  } else if (RSExportForEach::isRSForEachFunc(mTargetAPI, this, FD)) {
    RSExportForEach *EFE = RSExportForEach::Create(this, FD);
    if (EFE == nullptr)
      return false;
    else
      mExportForEach.push_back(EFE);
    return true;
  }

  RSExportFunc *EF = RSExportFunc::Create(this, FD);
  if (EF == nullptr)
    return false;
  else
    mExportFuncs.push_back(EF);

  return true;
}


bool RSContext::processExportType(const llvm::StringRef &Name) {
  clang::TranslationUnitDecl *TUDecl = mCtx.getTranslationUnitDecl();

  slangAssert(TUDecl != nullptr && "Translation unit declaration (top-level "
                                   "declaration) is null object");

  const clang::IdentifierInfo *II = mPP.getIdentifierInfo(Name);
  if (II == nullptr)
    // TODO(zonr): alert identifier @Name mark as an exportable type cannot be
    //             found
    return false;

  clang::DeclContext::lookup_result R = TUDecl->lookup(II);
  RSExportType *ET = nullptr;

  for (clang::DeclContext::lookup_iterator I = R.begin(), E = R.end();
       I != E;
       I++) {
    clang::NamedDecl *const ND = *I;
    const clang::Type *T = nullptr;

    switch (ND->getKind()) {
      case clang::Decl::Typedef: {
        T = static_cast<const clang::TypedefDecl*>(
            ND)->getCanonicalDecl()->getUnderlyingType().getTypePtr();
        break;
      }
      case clang::Decl::Record: {
        T = static_cast<const clang::RecordDecl*>(ND)->getTypeForDecl();
        break;
      }
      default: {
        // unsupported, skip
        break;
      }
    }

    if (T != nullptr)
      ET = RSExportType::Create(this, T);
  }

  return (ET != nullptr);
}


// Possibly re-order ForEach exports (maybe generating a dummy "root" function).
// We require "root" to be listed as slot 0 of our exported compute kernels,
// so this only needs to be created if we have other non-root kernels.
void RSContext::cleanupForEach() {
  bool foundNonRoot = false;
  ExportForEachList::iterator begin = mExportForEach.begin();

  for (ExportForEachList::iterator I = begin, E = mExportForEach.end();
       I != E;
       I++) {
    RSExportForEach *EFE = *I;
    if (!EFE->getName().compare("root")) {
      if (I == begin) {
        // Nothing to do, since it is the first function
        return;
      }

      mExportForEach.erase(I);
      mExportForEach.push_front(EFE);
      return;
    } else {
      foundNonRoot = true;
    }
  }

  // If we found a non-root kernel, but no root() function, we need to add a
  // dummy version (so that script->script calls of rsForEach don't behave
  // erratically).
  if (foundNonRoot) {
    RSExportForEach *DummyRoot = RSExportForEach::CreateDummyRoot(this);
    mExportForEach.push_front(DummyRoot);
  }
}


bool RSContext::processExport() {
  bool valid = true;

  if (getDiagnostics()->hasErrorOccurred()) {
    return false;
  }

  // Export variable
  clang::TranslationUnitDecl *TUDecl = mCtx.getTranslationUnitDecl();
  for (clang::DeclContext::decl_iterator DI = TUDecl->decls_begin(),
           DE = TUDecl->decls_end();
       DI != DE;
       DI++) {
    if (DI->getKind() == clang::Decl::Var) {
      clang::VarDecl *VD = (clang::VarDecl*) (*DI);
      if (VD->getFormalLinkage() == clang::ExternalLinkage) {
        if (!processExportVar(VD)) {
          valid = false;
        }
      }
    } else if (DI->getKind() == clang::Decl::Function) {
      // Export functions
      clang::FunctionDecl *FD = (clang::FunctionDecl*) (*DI);
      if (FD->getFormalLinkage() == clang::ExternalLinkage) {
        if (!processExportFunc(FD)) {
          valid = false;
        }
      }
    }
  }

  if (valid) {
    cleanupForEach();
  }

  // Finally, export type forcely set to be exported by user
  for (NeedExportTypeSet::const_iterator EI = mNeedExportTypes.begin(),
           EE = mNeedExportTypes.end();
       EI != EE;
       EI++) {
    if (!processExportType(EI->getKey())) {
      valid = false;
    }
  }

  return valid;
}

bool RSContext::insertExportType(const llvm::StringRef &TypeName,
                                 RSExportType *ET) {
  ExportTypeMap::value_type *NewItem =
      ExportTypeMap::value_type::Create(TypeName,
                                        mExportTypes.getAllocator(),
                                        ET);

  if (mExportTypes.insert(NewItem)) {
    return true;
  } else {
    free(NewItem);
    return false;
  }
}

RSContext::~RSContext() {
  delete mLicenseNote;
  delete mDataLayout;
  for (ExportableList::iterator I = mExportables.begin(),
          E = mExportables.end();
       I != E;
       I++) {
    if (!(*I)->isKeep())
      delete *I;
  }
}

}  // namespace slang
