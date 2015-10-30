/*
 * Copyright 2011-2012, The Android Open Source Project
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

#include "slang_rs_export_foreach.h"

#include <string>

#include "clang/AST/ASTContext.h"
#include "clang/AST/Attr.h"
#include "clang/AST/Decl.h"
#include "clang/AST/TypeLoc.h"

#include "llvm/IR/DerivedTypes.h"

#include "bcinfo/MetadataExtractor.h"

#include "slang_assert.h"
#include "slang_rs_context.h"
#include "slang_rs_export_type.h"
#include "slang_version.h"

namespace {

const size_t RS_KERNEL_INPUT_LIMIT = 8; // see frameworks/base/libs/rs/cpu_ref/rsCpuCoreRuntime.h

enum SpecialParameterKind {
  SPK_LOCATION, // 'int' or 'unsigned int'
  SPK_CONTEXT,  // rs_kernel_context
};

struct SpecialParameter {
  const char *name;
  bcinfo::MetadataSignatureBitval bitval;
  SpecialParameterKind kind;
  SlangTargetAPI minAPI;
};

// Table entries are in the order parameters must occur in a kernel parameter list.
const SpecialParameter specialParameterTable[] = {
  { "context", bcinfo::MD_SIG_Ctxt, SPK_CONTEXT, SLANG_M_TARGET_API },
  { "x", bcinfo::MD_SIG_X, SPK_LOCATION, SLANG_MINIMUM_TARGET_API },
  { "y", bcinfo::MD_SIG_Y, SPK_LOCATION, SLANG_MINIMUM_TARGET_API },
  { "z", bcinfo::MD_SIG_Z, SPK_LOCATION, SLANG_M_TARGET_API },
  { nullptr, bcinfo::MD_SIG_None, SPK_LOCATION, SLANG_MINIMUM_TARGET_API }, // marks end of table
};

// If the specified name matches the name of an entry in
// specialParameterTable, return the corresponding table index.
// Return -1 if not found.
int lookupSpecialParameter(const llvm::StringRef name) {
  for (int i = 0; specialParameterTable[i].name != nullptr; ++i) {
    if (name.equals(specialParameterTable[i].name)) {
      return i;
    }
  }

  return -1;
}

// Return a comma-separated list of names in specialParameterTable
// that are available at the specified API level.
std::string listSpecialParameters(unsigned int api) {
  std::string ret;
  bool first = true;
  for (int i = 0; specialParameterTable[i].name != nullptr; ++i) {
    if (specialParameterTable[i].minAPI > api)
      continue;
    if (first)
      first = false;
    else
      ret += ", ";
    ret += "'";
    ret += specialParameterTable[i].name;
    ret += "'";
  }
  return ret;
}

}

namespace slang {

// This function takes care of additional validation and construction of
// parameters related to forEach_* reflection.
bool RSExportForEach::validateAndConstructParams(
    RSContext *Context, const clang::FunctionDecl *FD) {
  slangAssert(Context && FD);
  bool valid = true;

  numParams = FD->getNumParams();

  if (Context->getTargetAPI() < SLANG_JB_TARGET_API) {
    // Before JellyBean, we allowed only one kernel per file.  It must be called "root".
    if (!isRootRSFunc(FD)) {
      Context->ReportError(FD->getLocation(),
                           "Non-root compute kernel %0() is "
                           "not supported in SDK levels %1-%2")
          << FD->getName() << SLANG_MINIMUM_TARGET_API
          << (SLANG_JB_TARGET_API - 1);
      return false;
    }
  }

  mResultType = FD->getReturnType().getCanonicalType();
  // Compute kernel functions are defined differently when the
  // "__attribute__((kernel))" is set.
  if (FD->hasAttr<clang::KernelAttr>()) {
    valid |= validateAndConstructKernelParams(Context, FD);
  } else {
    valid |= validateAndConstructOldStyleParams(Context, FD);
  }

  valid |= setSignatureMetadata(Context, FD);
  return valid;
}

bool RSExportForEach::validateAndConstructOldStyleParams(
    RSContext *Context, const clang::FunctionDecl *FD) {
  slangAssert(Context && FD);
  // If numParams is 0, we already marked this as a graphics root().
  slangAssert(numParams > 0);

  bool valid = true;

  // Compute kernel functions of this style are required to return a void type.
  clang::ASTContext &C = Context->getASTContext();
  if (mResultType != C.VoidTy) {
    Context->ReportError(FD->getLocation(),
                         "Compute kernel %0() is required to return a "
                         "void type")
        << FD->getName();
    valid = false;
  }

  // Validate remaining parameter types

  size_t IndexOfFirstSpecialParameter = numParams;
  valid |= processSpecialParameters(Context, FD, &IndexOfFirstSpecialParameter);

  // Validate the non-special parameters, which should all be found before the
  // first special parameter.
  for (size_t i = 0; i < IndexOfFirstSpecialParameter; i++) {
    const clang::ParmVarDecl *PVD = FD->getParamDecl(i);
    clang::QualType QT = PVD->getType().getCanonicalType();

    if (!QT->isPointerType()) {
      Context->ReportError(PVD->getLocation(),
                           "Compute kernel %0() cannot have non-pointer "
                           "parameters besides special parameters (%1). Parameter '%2' is "
                           "of type: '%3'")
          << FD->getName() << listSpecialParameters(Context->getTargetAPI())
          << PVD->getName() << PVD->getType().getAsString();
      valid = false;
      continue;
    }

    // The only non-const pointer should be out.
    if (!QT->getPointeeType().isConstQualified()) {
      if (mOut == nullptr) {
        mOut = PVD;
      } else {
        Context->ReportError(PVD->getLocation(),
                             "Compute kernel %0() can only have one non-const "
                             "pointer parameter. Parameters '%1' and '%2' are "
                             "both non-const.")
            << FD->getName() << mOut->getName() << PVD->getName();
        valid = false;
      }
    } else {
      if (mIns.empty() && mOut == nullptr) {
        mIns.push_back(PVD);
      } else if (mUsrData == nullptr) {
        mUsrData = PVD;
      } else {
        Context->ReportError(
            PVD->getLocation(),
            "Unexpected parameter '%0' for compute kernel %1()")
            << PVD->getName() << FD->getName();
        valid = false;
      }
    }
  }

  if (mIns.empty() && !mOut) {
    Context->ReportError(FD->getLocation(),
                         "Compute kernel %0() must have at least one "
                         "parameter for in or out")
        << FD->getName();
    valid = false;
  }

  return valid;
}

bool RSExportForEach::validateAndConstructKernelParams(
    RSContext *Context, const clang::FunctionDecl *FD) {
  slangAssert(Context && FD);
  bool valid = true;
  clang::ASTContext &C = Context->getASTContext();

  if (Context->getTargetAPI() < SLANG_JB_MR1_TARGET_API) {
    Context->ReportError(FD->getLocation(),
                         "Compute kernel %0() targeting SDK levels "
                         "%1-%2 may not use pass-by-value with "
                         "__attribute__((kernel))")
        << FD->getName() << SLANG_MINIMUM_TARGET_API
        << (SLANG_JB_MR1_TARGET_API - 1);
    return false;
  }

  // Denote that we are indeed a pass-by-value kernel.
  mIsKernelStyle = true;
  mHasReturnType = (mResultType != C.VoidTy);

  if (mResultType->isPointerType()) {
    Context->ReportError(
        FD->getTypeSpecStartLoc(),
        "Compute kernel %0() cannot return a pointer type: '%1'")
        << FD->getName() << mResultType.getAsString();
    valid = false;
  }

  // Validate remaining parameter types

  size_t IndexOfFirstSpecialParameter = numParams;
  valid |= processSpecialParameters(Context, FD, &IndexOfFirstSpecialParameter);

  // Validate the non-special parameters, which should all be found before the
  // first special.
  for (size_t i = 0; i < IndexOfFirstSpecialParameter; i++) {
    const clang::ParmVarDecl *PVD = FD->getParamDecl(i);

    if (Context->getTargetAPI() >= SLANG_M_TARGET_API || i == 0) {
      if (i >= RS_KERNEL_INPUT_LIMIT) {
        Context->ReportError(PVD->getLocation(),
                             "Invalid parameter '%0' for compute kernel %1(). "
                             "Kernels targeting SDK levels %2+ may not use "
                             "more than %3 input parameters.") << PVD->getName() <<
                             FD->getName() << SLANG_M_TARGET_API <<
                             int(RS_KERNEL_INPUT_LIMIT);

      } else {
        mIns.push_back(PVD);
      }
    } else {
      Context->ReportError(PVD->getLocation(),
                           "Invalid parameter '%0' for compute kernel %1(). "
                           "Kernels targeting SDK levels %2-%3 may not use "
                           "multiple input parameters.") << PVD->getName() <<
                           FD->getName() << SLANG_MINIMUM_TARGET_API <<
                           (SLANG_M_TARGET_API - 1);
      valid = false;
    }
    clang::QualType QT = PVD->getType().getCanonicalType();
    if (QT->isPointerType()) {
      Context->ReportError(PVD->getLocation(),
                           "Compute kernel %0() cannot have "
                           "parameter '%1' of pointer type: '%2'")
          << FD->getName() << PVD->getName() << PVD->getType().getAsString();
      valid = false;
    }
  }

  // Check that we have at least one allocation to use for dimensions.
  if (valid && mIns.empty() && !mHasReturnType && Context->getTargetAPI() < SLANG_M_TARGET_API) {
    Context->ReportError(FD->getLocation(),
                         "Compute kernel %0() targeting SDK levels "
                         "%1-%2 must have at least one "
                         "input parameter or a non-void return "
                         "type")
        << FD->getName() << SLANG_MINIMUM_TARGET_API
        << (SLANG_M_TARGET_API - 1);
    valid = false;
  }

  return valid;
}

// Process the optional special parameters:
// - Sets *IndexOfFirstSpecialParameter to the index of the first special parameter, or
//     FD->getNumParams() if none are found.
// - Sets mSpecialParameterSignatureMetadata for the found special parameters.
// Returns true if no errors.
bool RSExportForEach::processSpecialParameters(
    RSContext *Context, const clang::FunctionDecl *FD,
    size_t *IndexOfFirstSpecialParameter) {
  slangAssert(IndexOfFirstSpecialParameter != nullptr);
  slangAssert(mSpecialParameterSignatureMetadata == 0);
  clang::ASTContext &C = Context->getASTContext();

  // Find all special parameters if present.
  int LastSpecialParameterIdx = -1;     // index into specialParameterTable
  int FirstLocationSpecialParameterIdx = -1; // index into specialParameterTable
  clang::QualType FirstLocationSpecialParameterType;
  size_t NumParams = FD->getNumParams();
  *IndexOfFirstSpecialParameter = NumParams;
  bool valid = true;
  for (size_t i = 0; i < NumParams; i++) {
    const clang::ParmVarDecl *PVD = FD->getParamDecl(i);
    const llvm::StringRef ParamName = PVD->getName();
    const clang::QualType Type = PVD->getType();
    const clang::QualType QT = Type.getCanonicalType();
    const clang::QualType UT = QT.getUnqualifiedType();
    int SpecialParameterIdx = lookupSpecialParameter(ParamName);

    static const char KernelContextUnqualifiedTypeName[] =
        "const struct rs_kernel_context_t *";
    static const char KernelContextTypeName[] = "rs_kernel_context";

    // If the type is rs_context, it should have been named "context" and classified
    // as a special parameter.
    if (SpecialParameterIdx < 0 && UT.getAsString() == KernelContextUnqualifiedTypeName) {
      Context->ReportError(
          PVD->getLocation(),
          "The special parameter of type '%0' must be called "
          "'context' instead of '%1'.")
          << KernelContextTypeName << ParamName;
      SpecialParameterIdx = lookupSpecialParameter("context");
    }

    // If it's not a special parameter, check that it appears before any special
    // parameter.
    if (SpecialParameterIdx < 0) {
      if (*IndexOfFirstSpecialParameter < NumParams) {
        Context->ReportError(PVD->getLocation(),
                             "In compute kernel %0(), parameter '%1' cannot "
                             "appear after any of the special parameters (%2).")
            << FD->getName() << ParamName << listSpecialParameters(Context->getTargetAPI());
        valid = false;
      }
      continue;
    }

    const SpecialParameter &SP = specialParameterTable[SpecialParameterIdx];

    // Verify that this special parameter is OK for the current API level.
    if (Context->getTargetAPI() < SP.minAPI) {
      Context->ReportError(PVD->getLocation(),
                           "Compute kernel %0() targeting SDK levels "
                           "%1-%2 may not use special parameter '%3'.")
          << FD->getName() << SLANG_MINIMUM_TARGET_API << (SP.minAPI - 1)
          << SP.name;
      valid = false;
    }

    // Check that the order of the special parameters is correct.
    if (SpecialParameterIdx < LastSpecialParameterIdx) {
      Context->ReportError(
          PVD->getLocation(),
          "In compute kernel %0(), special parameter '%1' must "
          "be defined before special parameter '%2'.")
          << FD->getName() << SP.name
          << specialParameterTable[LastSpecialParameterIdx].name;
      valid = false;
    }

    // Validate the data type of the special parameter.
    switch (SP.kind) {
    case SPK_LOCATION: {
      // Location special parameters can only be int or uint.
      if (UT != C.UnsignedIntTy && UT != C.IntTy) {
        Context->ReportError(PVD->getLocation(),
                             "Special parameter '%0' must be of type 'int' or "
                             "'unsigned int'. It is of type '%1'.")
            << ParamName << Type.getAsString();
        valid = false;
      }

      // Ensure that all location special parameters have the same type.
      if (FirstLocationSpecialParameterIdx >= 0) {
        if (Type != FirstLocationSpecialParameterType) {
          Context->ReportError(
              PVD->getLocation(),
              "Special parameters '%0' and '%1' must be of the same type. "
              "'%0' is of type '%2' while '%1' is of type '%3'.")
              << specialParameterTable[FirstLocationSpecialParameterIdx].name
              << SP.name << FirstLocationSpecialParameterType.getAsString()
              << Type.getAsString();
          valid = false;
        }
      } else {
        FirstLocationSpecialParameterIdx = SpecialParameterIdx;
        FirstLocationSpecialParameterType = Type;
      }
    } break;
    case SPK_CONTEXT: {
      // Check that variables named "context" are of type rs_context.
      if (UT.getAsString() != KernelContextUnqualifiedTypeName) {
        Context->ReportError(PVD->getLocation(),
                             "Special parameter '%0' must be of type '%1'. "
                             "It is of type '%2'.")
            << ParamName << KernelContextTypeName
            << Type.getAsString();
        valid = false;
      }
    } break;
    default:
      slangAssert(!"Unexpected special parameter type");
    }

    // We should not be invoked if two parameters of the same name are present.
    slangAssert(!(mSpecialParameterSignatureMetadata & SP.bitval));
    mSpecialParameterSignatureMetadata |= SP.bitval;

    LastSpecialParameterIdx = SpecialParameterIdx;
    // If this is the first time we find a special parameter, save it.
    if (*IndexOfFirstSpecialParameter >= NumParams) {
      *IndexOfFirstSpecialParameter = i;
    }
  }
  return valid;
}

bool RSExportForEach::setSignatureMetadata(RSContext *Context,
                                           const clang::FunctionDecl *FD) {
  mSignatureMetadata = 0;
  bool valid = true;

  if (mIsKernelStyle) {
    slangAssert(mOut == nullptr);
    slangAssert(mUsrData == nullptr);
  } else {
    slangAssert(!mHasReturnType);
  }

  // Set up the bitwise metadata encoding for runtime argument passing.
  const bool HasOut = mOut || mHasReturnType;
  mSignatureMetadata |= (hasIns() ?       bcinfo::MD_SIG_In     : 0);
  mSignatureMetadata |= (HasOut ?         bcinfo::MD_SIG_Out    : 0);
  mSignatureMetadata |= (mUsrData ?       bcinfo::MD_SIG_Usr    : 0);
  mSignatureMetadata |= (mIsKernelStyle ? bcinfo::MD_SIG_Kernel : 0);  // pass-by-value
  mSignatureMetadata |= mSpecialParameterSignatureMetadata;

  if (Context->getTargetAPI() < SLANG_ICS_TARGET_API) {
    // APIs before ICS cannot skip between parameters. It is ok, however, for
    // them to omit further parameters (i.e. skipping X is ok if you skip Y).
    if (mSignatureMetadata != (bcinfo::MD_SIG_In | bcinfo::MD_SIG_Out | bcinfo::MD_SIG_Usr |
                               bcinfo::MD_SIG_X | bcinfo::MD_SIG_Y) &&
        mSignatureMetadata != (bcinfo::MD_SIG_In | bcinfo::MD_SIG_Out | bcinfo::MD_SIG_Usr |
                               bcinfo::MD_SIG_X) &&
        mSignatureMetadata != (bcinfo::MD_SIG_In | bcinfo::MD_SIG_Out | bcinfo::MD_SIG_Usr) &&
        mSignatureMetadata != (bcinfo::MD_SIG_In | bcinfo::MD_SIG_Out) &&
        mSignatureMetadata != (bcinfo::MD_SIG_In)) {
      Context->ReportError(FD->getLocation(),
                           "Compute kernel %0() targeting SDK levels "
                           "%1-%2 may not skip parameters")
          << FD->getName() << SLANG_MINIMUM_TARGET_API
          << (SLANG_ICS_TARGET_API - 1);
      valid = false;
    }
  }
  return valid;
}

RSExportForEach *RSExportForEach::Create(RSContext *Context,
                                         const clang::FunctionDecl *FD) {
  slangAssert(Context && FD);
  llvm::StringRef Name = FD->getName();
  RSExportForEach *FE;

  slangAssert(!Name.empty() && "Function must have a name");

  FE = new RSExportForEach(Context, Name);

  if (!FE->validateAndConstructParams(Context, FD)) {
    return nullptr;
  }

  clang::ASTContext &Ctx = Context->getASTContext();

  std::string Id = CreateDummyName("helper_foreach_param", FE->getName());

  // Extract the usrData parameter (if we have one)
  if (FE->mUsrData) {
    const clang::ParmVarDecl *PVD = FE->mUsrData;
    clang::QualType QT = PVD->getType().getCanonicalType();
    slangAssert(QT->isPointerType() &&
                QT->getPointeeType().isConstQualified());

    const clang::ASTContext &C = Context->getASTContext();
    if (QT->getPointeeType().getCanonicalType().getUnqualifiedType() ==
        C.VoidTy) {
      // In the case of using const void*, we can't reflect an appopriate
      // Java type, so we fall back to just reflecting the ain/aout parameters
      FE->mUsrData = nullptr;
    } else {
      clang::RecordDecl *RD =
          clang::RecordDecl::Create(Ctx, clang::TTK_Struct,
                                    Ctx.getTranslationUnitDecl(),
                                    clang::SourceLocation(),
                                    clang::SourceLocation(),
                                    &Ctx.Idents.get(Id));

      clang::FieldDecl *FD =
          clang::FieldDecl::Create(Ctx,
                                   RD,
                                   clang::SourceLocation(),
                                   clang::SourceLocation(),
                                   PVD->getIdentifier(),
                                   QT->getPointeeType(),
                                   nullptr,
                                   /* BitWidth = */ nullptr,
                                   /* Mutable = */ false,
                                   /* HasInit = */ clang::ICIS_NoInit);
      RD->addDecl(FD);
      RD->completeDefinition();

      // Create an export type iff we have a valid usrData type
      clang::QualType T = Ctx.getTagDeclType(RD);
      slangAssert(!T.isNull());

      RSExportType *ET = RSExportType::Create(Context, T.getTypePtr());

      slangAssert(ET && "Failed to export a kernel");

      slangAssert((ET->getClass() == RSExportType::ExportClassRecord) &&
                  "Parameter packet must be a record");

      FE->mParamPacketType = static_cast<RSExportRecordType *>(ET);
    }
  }

  if (FE->hasIns()) {

    for (InIter BI = FE->mIns.begin(), EI = FE->mIns.end(); BI != EI; BI++) {
      const clang::Type *T = (*BI)->getType().getCanonicalType().getTypePtr();
      RSExportType *InExportType = RSExportType::Create(Context, T);

      if (FE->mIsKernelStyle) {
        slangAssert(InExportType != nullptr);
      }

      FE->mInTypes.push_back(InExportType);
    }
  }

  if (FE->mIsKernelStyle && FE->mHasReturnType) {
    const clang::Type *T = FE->mResultType.getTypePtr();
    FE->mOutType = RSExportType::Create(Context, T);
    slangAssert(FE->mOutType);
  } else if (FE->mOut) {
    const clang::Type *T = FE->mOut->getType().getCanonicalType().getTypePtr();
    FE->mOutType = RSExportType::Create(Context, T);
  }

  return FE;
}

RSExportForEach *RSExportForEach::CreateDummyRoot(RSContext *Context) {
  slangAssert(Context);
  llvm::StringRef Name = "root";
  RSExportForEach *FE = new RSExportForEach(Context, Name);
  FE->mDummyRoot = true;
  return FE;
}

bool RSExportForEach::isGraphicsRootRSFunc(unsigned int targetAPI,
                                           const clang::FunctionDecl *FD) {
  if (FD->hasAttr<clang::KernelAttr>()) {
    return false;
  }

  if (!isRootRSFunc(FD)) {
    return false;
  }

  if (FD->getNumParams() == 0) {
    // Graphics root function
    return true;
  }

  // Check for legacy graphics root function (with single parameter).
  if ((targetAPI < SLANG_ICS_TARGET_API) && (FD->getNumParams() == 1)) {
    const clang::QualType &IntType = FD->getASTContext().IntTy;
    if (FD->getReturnType().getCanonicalType() == IntType) {
      return true;
    }
  }

  return false;
}

bool RSExportForEach::isRSForEachFunc(unsigned int targetAPI,
                                      slang::RSContext* Context,
                                      const clang::FunctionDecl *FD) {
  slangAssert(Context && FD);
  bool hasKernelAttr = FD->hasAttr<clang::KernelAttr>();

  if (FD->getStorageClass() == clang::SC_Static) {
    if (hasKernelAttr) {
      Context->ReportError(FD->getLocation(),
                           "Invalid use of attribute kernel with "
                           "static function declaration: %0")
          << FD->getName();
    }
    return false;
  }

  // Anything tagged as a kernel is definitely used with ForEach.
  if (hasKernelAttr) {
    return true;
  }

  if (isGraphicsRootRSFunc(targetAPI, FD)) {
    return false;
  }

  // Check if first parameter is a pointer (which is required for ForEach).
  unsigned int numParams = FD->getNumParams();

  if (numParams > 0) {
    const clang::ParmVarDecl *PVD = FD->getParamDecl(0);
    clang::QualType QT = PVD->getType().getCanonicalType();

    if (QT->isPointerType()) {
      return true;
    }

    // Any non-graphics root() is automatically a ForEach candidate.
    // At this point, however, we know that it is not going to be a valid
    // compute root() function (due to not having a pointer parameter). We
    // still want to return true here, so that we can issue appropriate
    // diagnostics.
    if (isRootRSFunc(FD)) {
      return true;
    }
  }

  return false;
}

bool
RSExportForEach::validateSpecialFuncDecl(unsigned int targetAPI,
                                         slang::RSContext *Context,
                                         clang::FunctionDecl const *FD) {
  slangAssert(Context && FD);
  bool valid = true;
  const clang::ASTContext &C = FD->getASTContext();
  const clang::QualType &IntType = FD->getASTContext().IntTy;

  if (isGraphicsRootRSFunc(targetAPI, FD)) {
    if ((targetAPI < SLANG_ICS_TARGET_API) && (FD->getNumParams() == 1)) {
      // Legacy graphics root function
      const clang::ParmVarDecl *PVD = FD->getParamDecl(0);
      clang::QualType QT = PVD->getType().getCanonicalType();
      if (QT != IntType) {
        Context->ReportError(PVD->getLocation(),
                             "invalid parameter type for legacy "
                             "graphics root() function: %0")
            << PVD->getType();
        valid = false;
      }
    }

    // Graphics root function, so verify that it returns an int
    if (FD->getReturnType().getCanonicalType() != IntType) {
      Context->ReportError(FD->getLocation(),
                           "root() is required to return "
                           "an int for graphics usage");
      valid = false;
    }
  } else if (isInitRSFunc(FD) || isDtorRSFunc(FD)) {
    if (FD->getNumParams() != 0) {
      Context->ReportError(FD->getLocation(),
                           "%0(void) is required to have no "
                           "parameters")
          << FD->getName();
      valid = false;
    }

    if (FD->getReturnType().getCanonicalType() != C.VoidTy) {
      Context->ReportError(FD->getLocation(),
                           "%0(void) is required to have a void "
                           "return type")
          << FD->getName();
      valid = false;
    }
  } else {
    slangAssert(false && "must be called on root, init or .rs.dtor function!");
  }

  return valid;
}

}  // namespace slang
