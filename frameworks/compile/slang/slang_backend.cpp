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

#include "slang_backend.h"

#include <string>
#include <vector>

#include "bcinfo/BitcodeWrapper.h"

#include "clang/AST/ASTContext.h"
#include "clang/AST/Decl.h"
#include "clang/AST/DeclGroup.h"

#include "clang/Basic/Diagnostic.h"
#include "clang/Basic/TargetInfo.h"
#include "clang/Basic/TargetOptions.h"

#include "clang/CodeGen/ModuleBuilder.h"

#include "clang/Frontend/CodeGenOptions.h"
#include "clang/Frontend/FrontendDiagnostic.h"

#include "llvm/ADT/Twine.h"
#include "llvm/ADT/StringExtras.h"

#include "llvm/Bitcode/ReaderWriter.h"

#include "llvm/CodeGen/RegAllocRegistry.h"
#include "llvm/CodeGen/SchedulerRegistry.h"

#include "llvm/IR/Constant.h"
#include "llvm/IR/Constants.h"
#include "llvm/IR/DataLayout.h"
#include "llvm/IR/DebugLoc.h"
#include "llvm/IR/DerivedTypes.h"
#include "llvm/IR/Function.h"
#include "llvm/IR/IRBuilder.h"
#include "llvm/IR/IRPrintingPasses.h"
#include "llvm/IR/LLVMContext.h"
#include "llvm/IR/Metadata.h"
#include "llvm/IR/Module.h"

#include "llvm/Transforms/IPO/PassManagerBuilder.h"

#include "llvm/Target/TargetMachine.h"
#include "llvm/Target/TargetOptions.h"
#include "llvm/Support/TargetRegistry.h"

#include "llvm/MC/SubtargetFeature.h"

#include "slang_assert.h"
#include "slang.h"
#include "slang_rs_context.h"
#include "slang_rs_export_foreach.h"
#include "slang_rs_export_func.h"
#include "slang_rs_export_type.h"
#include "slang_rs_export_var.h"
#include "slang_rs_metadata.h"

#include "strip_unknown_attributes.h"
#include "BitWriter_2_9/ReaderWriter_2_9.h"
#include "BitWriter_2_9_func/ReaderWriter_2_9_func.h"
#include "BitWriter_3_2/ReaderWriter_3_2.h"

namespace slang {

void Backend::CreateFunctionPasses() {
  if (!mPerFunctionPasses) {
    mPerFunctionPasses = new llvm::legacy::FunctionPassManager(mpModule);

    llvm::PassManagerBuilder PMBuilder;
    PMBuilder.OptLevel = mCodeGenOpts.OptimizationLevel;
    PMBuilder.populateFunctionPassManager(*mPerFunctionPasses);
  }
}

void Backend::CreateModulePasses() {
  if (!mPerModulePasses) {
    mPerModulePasses = new llvm::legacy::PassManager();

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

    PMBuilder.populateModulePassManager(*mPerModulePasses);
    // Add a pass to strip off unknown/unsupported attributes.
    mPerModulePasses->add(createStripUnknownAttributesPass());
  }
}

bool Backend::CreateCodeGenPasses() {
  if ((mOT != Slang::OT_Assembly) && (mOT != Slang::OT_Object))
    return true;

  // Now we add passes for code emitting
  if (mCodeGenPasses) {
    return true;
  } else {
    mCodeGenPasses = new llvm::legacy::FunctionPassManager(mpModule);
  }

  // Create the TargetMachine for generating code.
  std::string Triple = mpModule->getTargetTriple();

  std::string Error;
  const llvm::Target* TargetInfo =
      llvm::TargetRegistry::lookupTarget(Triple, Error);
  if (TargetInfo == nullptr) {
    mDiagEngine.Report(clang::diag::err_fe_unable_to_create_target) << Error;
    return false;
  }

  // Target Machine Options
  llvm::TargetOptions Options;

  Options.NoFramePointerElim = mCodeGenOpts.DisableFPElim;

  // Use hardware FPU.
  //
  // FIXME: Need to detect the CPU capability and decide whether to use softfp.
  // To use softfp, change following 2 lines to
  //
  // Options.FloatABIType = llvm::FloatABI::Soft;
  // Options.UseSoftFloat = true;
  Options.FloatABIType = llvm::FloatABI::Hard;
  Options.UseSoftFloat = false;

  // BCC needs all unknown symbols resolved at compilation time. So we don't
  // need any relocation model.
  llvm::Reloc::Model RM = llvm::Reloc::Static;

  // This is set for the linker (specify how large of the virtual addresses we
  // can access for all unknown symbols.)
  llvm::CodeModel::Model CM;
  if (mpModule->getDataLayout().getPointerSize() == 4) {
    CM = llvm::CodeModel::Small;
  } else {
    // The target may have pointer size greater than 32 (e.g. x86_64
    // architecture) may need large data address model
    CM = llvm::CodeModel::Medium;
  }

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

  llvm::TargetMachine *TM =
    TargetInfo->createTargetMachine(Triple, mTargetOpts.CPU, FeaturesStr,
                                    Options, RM, CM);

  // Register scheduler
  llvm::RegisterScheduler::setDefault(llvm::createDefaultScheduler);

  // Register allocation policy:
  //  createFastRegisterAllocator: fast but bad quality
  //  createGreedyRegisterAllocator: not so fast but good quality
  llvm::RegisterRegAlloc::setDefault((mCodeGenOpts.OptimizationLevel == 0) ?
                                     llvm::createFastRegisterAllocator :
                                     llvm::createGreedyRegisterAllocator);

  llvm::CodeGenOpt::Level OptLevel = llvm::CodeGenOpt::Default;
  if (mCodeGenOpts.OptimizationLevel == 0) {
    OptLevel = llvm::CodeGenOpt::None;
  } else if (mCodeGenOpts.OptimizationLevel == 3) {
    OptLevel = llvm::CodeGenOpt::Aggressive;
  }

  llvm::TargetMachine::CodeGenFileType CGFT =
      llvm::TargetMachine::CGFT_AssemblyFile;
  if (mOT == Slang::OT_Object) {
    CGFT = llvm::TargetMachine::CGFT_ObjectFile;
  }
  if (TM->addPassesToEmitFile(*mCodeGenPasses, mBufferOutStream,
                              CGFT, OptLevel)) {
    mDiagEngine.Report(clang::diag::err_fe_unable_to_interface_with_target);
    return false;
  }

  return true;
}

Backend::Backend(RSContext *Context, clang::DiagnosticsEngine *DiagEngine,
                 const clang::CodeGenOptions &CodeGenOpts,
                 const clang::TargetOptions &TargetOpts, PragmaList *Pragmas,
                 llvm::raw_ostream *OS, Slang::OutputType OT,
                 clang::SourceManager &SourceMgr, bool AllowRSPrefix,
                 bool IsFilterscript)
    : ASTConsumer(), mTargetOpts(TargetOpts), mpModule(nullptr), mpOS(OS),
      mOT(OT), mGen(nullptr), mPerFunctionPasses(nullptr),
      mPerModulePasses(nullptr), mCodeGenPasses(nullptr),
      mBufferOutStream(*mpOS), mContext(Context),
      mSourceMgr(SourceMgr), mAllowRSPrefix(AllowRSPrefix),
      mIsFilterscript(IsFilterscript), mExportVarMetadata(nullptr),
      mExportFuncMetadata(nullptr), mExportForEachNameMetadata(nullptr),
      mExportForEachSignatureMetadata(nullptr), mExportTypeMetadata(nullptr),
      mRSObjectSlotsMetadata(nullptr), mRefCount(mContext->getASTContext()),
      mASTChecker(Context, Context->getTargetAPI(), IsFilterscript),
      mLLVMContext(llvm::getGlobalContext()), mDiagEngine(*DiagEngine),
      mCodeGenOpts(CodeGenOpts), mPragmas(Pragmas) {
  mGen = CreateLLVMCodeGen(mDiagEngine, "", mCodeGenOpts, mLLVMContext);
}

void Backend::Initialize(clang::ASTContext &Ctx) {
  mGen->Initialize(Ctx);

  mpModule = mGen->GetModule();
}

// Encase the Bitcode in a wrapper containing RS version information.
void Backend::WrapBitcode(llvm::raw_string_ostream &Bitcode) {
  bcinfo::AndroidBitcodeWrapper wrapper;
  size_t actualWrapperLen = bcinfo::writeAndroidBitcodeWrapper(
      &wrapper, Bitcode.str().length(), getTargetAPI(),
      SlangVersion::CURRENT, mCodeGenOpts.OptimizationLevel);

  slangAssert(actualWrapperLen > 0);

  // Write out the bitcode wrapper.
  mBufferOutStream.write(reinterpret_cast<char*>(&wrapper), actualWrapperLen);

  // Write out the actual encoded bitcode.
  mBufferOutStream << Bitcode.str();
}

void Backend::HandleTranslationUnit(clang::ASTContext &Ctx) {
  HandleTranslationUnitPre(Ctx);

  mGen->HandleTranslationUnit(Ctx);

  // Here, we complete a translation unit (whole translation unit is now in LLVM
  // IR). Now, interact with LLVM backend to generate actual machine code (asm
  // or machine code, whatever.)

  // Silently ignore if we weren't initialized for some reason.
  if (!mpModule)
    return;

  llvm::Module *M = mGen->ReleaseModule();
  if (!M) {
    // The module has been released by IR gen on failures, do not double free.
    mpModule = nullptr;
    return;
  }

  slangAssert(mpModule == M &&
              "Unexpected module change during LLVM IR generation");

  // Insert #pragma information into metadata section of module
  if (!mPragmas->empty()) {
    llvm::NamedMDNode *PragmaMetadata =
        mpModule->getOrInsertNamedMetadata(Slang::PragmaMetadataName);
    for (PragmaList::const_iterator I = mPragmas->begin(), E = mPragmas->end();
         I != E;
         I++) {
      llvm::SmallVector<llvm::Metadata*, 2> Pragma;
      // Name goes first
      Pragma.push_back(llvm::MDString::get(mLLVMContext, I->first));
      // And then value
      Pragma.push_back(llvm::MDString::get(mLLVMContext, I->second));

      // Create MDNode and insert into PragmaMetadata
      PragmaMetadata->addOperand(
          llvm::MDNode::get(mLLVMContext, Pragma));
    }
  }

  HandleTranslationUnitPost(mpModule);

  // Create passes for optimization and code emission

  // Create and run per-function passes
  CreateFunctionPasses();
  if (mPerFunctionPasses) {
    mPerFunctionPasses->doInitialization();

    for (llvm::Module::iterator I = mpModule->begin(), E = mpModule->end();
         I != E;
         I++)
      if (!I->isDeclaration())
        mPerFunctionPasses->run(*I);

    mPerFunctionPasses->doFinalization();
  }

  // Create and run module passes
  CreateModulePasses();
  if (mPerModulePasses)
    mPerModulePasses->run(*mpModule);

  switch (mOT) {
    case Slang::OT_Assembly:
    case Slang::OT_Object: {
      if (!CreateCodeGenPasses())
        return;

      mCodeGenPasses->doInitialization();

      for (llvm::Module::iterator I = mpModule->begin(), E = mpModule->end();
          I != E;
          I++)
        if (!I->isDeclaration())
          mCodeGenPasses->run(*I);

      mCodeGenPasses->doFinalization();
      break;
    }
    case Slang::OT_LLVMAssembly: {
      llvm::legacy::PassManager *LLEmitPM = new llvm::legacy::PassManager();
      LLEmitPM->add(llvm::createPrintModulePass(mBufferOutStream));
      LLEmitPM->run(*mpModule);
      break;
    }
    case Slang::OT_Bitcode: {
      llvm::legacy::PassManager *BCEmitPM = new llvm::legacy::PassManager();
      std::string BCStr;
      llvm::raw_string_ostream Bitcode(BCStr);
      unsigned int TargetAPI = getTargetAPI();
      switch (TargetAPI) {
        case SLANG_HC_TARGET_API:
        case SLANG_HC_MR1_TARGET_API:
        case SLANG_HC_MR2_TARGET_API: {
          // Pre-ICS targets must use the LLVM 2.9 BitcodeWriter
          BCEmitPM->add(llvm_2_9::createBitcodeWriterPass(Bitcode));
          break;
        }
        case SLANG_ICS_TARGET_API:
        case SLANG_ICS_MR1_TARGET_API: {
          // ICS targets must use the LLVM 2.9_func BitcodeWriter
          BCEmitPM->add(llvm_2_9_func::createBitcodeWriterPass(Bitcode));
          break;
        }
        default: {
          if (TargetAPI != SLANG_DEVELOPMENT_TARGET_API &&
              (TargetAPI < SLANG_MINIMUM_TARGET_API ||
               TargetAPI > SLANG_MAXIMUM_TARGET_API)) {
            slangAssert(false && "Invalid target API value");
          }
          // Switch to the 3.2 BitcodeWriter by default, and don't use
          // LLVM's included BitcodeWriter at all (for now).
          BCEmitPM->add(llvm_3_2::createBitcodeWriterPass(Bitcode));
          //BCEmitPM->add(llvm::createBitcodeWriterPass(Bitcode));
          break;
        }
      }

      BCEmitPM->run(*mpModule);
      WrapBitcode(Bitcode);
      break;
    }
    case Slang::OT_Nothing: {
      return;
    }
    default: {
      slangAssert(false && "Unknown output type");
    }
  }

  mBufferOutStream.flush();
}

void Backend::HandleTagDeclDefinition(clang::TagDecl *D) {
  mGen->HandleTagDeclDefinition(D);
}

void Backend::CompleteTentativeDefinition(clang::VarDecl *D) {
  mGen->CompleteTentativeDefinition(D);
}

Backend::~Backend() {
  delete mpModule;
  delete mGen;
  delete mPerFunctionPasses;
  delete mPerModulePasses;
  delete mCodeGenPasses;
}

// 1) Add zero initialization of local RS object types
void Backend::AnnotateFunction(clang::FunctionDecl *FD) {
  if (FD &&
      FD->hasBody() &&
      !Slang::IsLocInRSHeaderFile(FD->getLocation(), mSourceMgr)) {
    mRefCount.Init();
    mRefCount.Visit(FD->getBody());
  }
}

bool Backend::HandleTopLevelDecl(clang::DeclGroupRef D) {
  // Disallow user-defined functions with prefix "rs"
  if (!mAllowRSPrefix) {
    // Iterate all function declarations in the program.
    for (clang::DeclGroupRef::iterator I = D.begin(), E = D.end();
         I != E; I++) {
      clang::FunctionDecl *FD = llvm::dyn_cast<clang::FunctionDecl>(*I);
      if (FD == nullptr)
        continue;
      if (!FD->getName().startswith("rs"))  // Check prefix
        continue;
      if (!Slang::IsLocInRSHeaderFile(FD->getLocation(), mSourceMgr))
        mContext->ReportError(FD->getLocation(),
                              "invalid function name prefix, "
                              "\"rs\" is reserved: '%0'")
            << FD->getName();
    }
  }

  // Process any non-static function declarations
  for (clang::DeclGroupRef::iterator I = D.begin(), E = D.end(); I != E; I++) {
    clang::FunctionDecl *FD = llvm::dyn_cast<clang::FunctionDecl>(*I);
    if (FD && FD->isGlobal()) {
      // Check that we don't have any array parameters being misintrepeted as
      // kernel pointers due to the C type system's array to pointer decay.
      size_t numParams = FD->getNumParams();
      for (size_t i = 0; i < numParams; i++) {
        const clang::ParmVarDecl *PVD = FD->getParamDecl(i);
        clang::QualType QT = PVD->getOriginalType();
        if (QT->isArrayType()) {
          mContext->ReportError(
              PVD->getTypeSpecStartLoc(),
              "exported function parameters may not have array type: %0")
              << QT;
        }
      }
      AnnotateFunction(FD);
    }
  }
  return mGen->HandleTopLevelDecl(D);
}

void Backend::HandleTranslationUnitPre(clang::ASTContext &C) {
  clang::TranslationUnitDecl *TUDecl = C.getTranslationUnitDecl();

  // If we have an invalid RS/FS AST, don't check further.
  if (!mASTChecker.Validate()) {
    return;
  }

  if (mIsFilterscript) {
    mContext->addPragma("rs_fp_relaxed", "");
  }

  int version = mContext->getVersion();
  if (version == 0) {
    // Not setting a version is an error
    mDiagEngine.Report(
        mSourceMgr.getLocForEndOfFile(mSourceMgr.getMainFileID()),
        mDiagEngine.getCustomDiagID(
            clang::DiagnosticsEngine::Error,
            "missing pragma for version in source file"));
  } else {
    slangAssert(version == 1);
  }

  if (mContext->getReflectJavaPackageName().empty()) {
    mDiagEngine.Report(
        mSourceMgr.getLocForEndOfFile(mSourceMgr.getMainFileID()),
        mDiagEngine.getCustomDiagID(clang::DiagnosticsEngine::Error,
                                    "missing \"#pragma rs "
                                    "java_package_name(com.foo.bar)\" "
                                    "in source file"));
    return;
  }

  // Create a static global destructor if necessary (to handle RS object
  // runtime cleanup).
  clang::FunctionDecl *FD = mRefCount.CreateStaticGlobalDtor();
  if (FD) {
    HandleTopLevelDecl(clang::DeclGroupRef(FD));
  }

  // Process any static function declarations
  for (clang::DeclContext::decl_iterator I = TUDecl->decls_begin(),
          E = TUDecl->decls_end(); I != E; I++) {
    if ((I->getKind() >= clang::Decl::firstFunction) &&
        (I->getKind() <= clang::Decl::lastFunction)) {
      clang::FunctionDecl *FD = llvm::dyn_cast<clang::FunctionDecl>(*I);
      if (FD && !FD->isGlobal()) {
        AnnotateFunction(FD);
      }
    }
  }
}

///////////////////////////////////////////////////////////////////////////////
void Backend::dumpExportVarInfo(llvm::Module *M) {
  int slotCount = 0;
  if (mExportVarMetadata == nullptr)
    mExportVarMetadata = M->getOrInsertNamedMetadata(RS_EXPORT_VAR_MN);

  llvm::SmallVector<llvm::Metadata *, 2> ExportVarInfo;

  // We emit slot information (#rs_object_slots) for any reference counted
  // RS type or pointer (which can also be bound).

  for (RSContext::const_export_var_iterator I = mContext->export_vars_begin(),
          E = mContext->export_vars_end();
       I != E;
       I++) {
    const RSExportVar *EV = *I;
    const RSExportType *ET = EV->getType();
    bool countsAsRSObject = false;

    // Variable name
    ExportVarInfo.push_back(
        llvm::MDString::get(mLLVMContext, EV->getName().c_str()));

    // Type name
    switch (ET->getClass()) {
      case RSExportType::ExportClassPrimitive: {
        const RSExportPrimitiveType *PT =
            static_cast<const RSExportPrimitiveType*>(ET);
        ExportVarInfo.push_back(
            llvm::MDString::get(
              mLLVMContext, llvm::utostr_32(PT->getType())));
        if (PT->isRSObjectType()) {
          countsAsRSObject = true;
        }
        break;
      }
      case RSExportType::ExportClassPointer: {
        ExportVarInfo.push_back(
            llvm::MDString::get(
              mLLVMContext, ("*" + static_cast<const RSExportPointerType*>(ET)
                ->getPointeeType()->getName()).c_str()));
        break;
      }
      case RSExportType::ExportClassMatrix: {
        ExportVarInfo.push_back(
            llvm::MDString::get(
              mLLVMContext, llvm::utostr_32(
                  /* TODO Strange value.  This pushes just a number, quite
                   * different than the other cases.  What is this used for?
                   * These are the metadata values that some partner drivers
                   * want to reference (for TBAA, etc.). We may want to look
                   * at whether these provide any reasonable value (or have
                   * distinct enough values to actually depend on).
                   */
                DataTypeRSMatrix2x2 +
                static_cast<const RSExportMatrixType*>(ET)->getDim() - 2)));
        break;
      }
      case RSExportType::ExportClassVector:
      case RSExportType::ExportClassConstantArray:
      case RSExportType::ExportClassRecord: {
        ExportVarInfo.push_back(
            llvm::MDString::get(mLLVMContext,
              EV->getType()->getName().c_str()));
        break;
      }
    }

    mExportVarMetadata->addOperand(
        llvm::MDNode::get(mLLVMContext, ExportVarInfo));
    ExportVarInfo.clear();

    if (mRSObjectSlotsMetadata == nullptr) {
      mRSObjectSlotsMetadata =
          M->getOrInsertNamedMetadata(RS_OBJECT_SLOTS_MN);
    }

    if (countsAsRSObject) {
      mRSObjectSlotsMetadata->addOperand(llvm::MDNode::get(mLLVMContext,
          llvm::MDString::get(mLLVMContext, llvm::utostr_32(slotCount))));
    }

    slotCount++;
  }
}

void Backend::dumpExportFunctionInfo(llvm::Module *M) {
  if (mExportFuncMetadata == nullptr)
    mExportFuncMetadata =
        M->getOrInsertNamedMetadata(RS_EXPORT_FUNC_MN);

  llvm::SmallVector<llvm::Metadata *, 1> ExportFuncInfo;

  for (RSContext::const_export_func_iterator
          I = mContext->export_funcs_begin(),
          E = mContext->export_funcs_end();
       I != E;
       I++) {
    const RSExportFunc *EF = *I;

    // Function name
    if (!EF->hasParam()) {
      ExportFuncInfo.push_back(llvm::MDString::get(mLLVMContext,
                                                   EF->getName().c_str()));
    } else {
      llvm::Function *F = M->getFunction(EF->getName());
      llvm::Function *HelperFunction;
      const std::string HelperFunctionName(".helper_" + EF->getName());

      slangAssert(F && "Function marked as exported disappeared in Bitcode");

      // Create helper function
      {
        llvm::StructType *HelperFunctionParameterTy = nullptr;
        std::vector<bool> isStructInput;

        if (!F->getArgumentList().empty()) {
          std::vector<llvm::Type*> HelperFunctionParameterTys;
          for (llvm::Function::arg_iterator AI = F->arg_begin(),
                   AE = F->arg_end(); AI != AE; AI++) {
              if (AI->getType()->isPointerTy() && AI->getType()->getPointerElementType()->isStructTy()) {
                  HelperFunctionParameterTys.push_back(AI->getType()->getPointerElementType());
                  isStructInput.push_back(true);
              } else {
                  HelperFunctionParameterTys.push_back(AI->getType());
                  isStructInput.push_back(false);
              }
          }
          HelperFunctionParameterTy =
              llvm::StructType::get(mLLVMContext, HelperFunctionParameterTys);
        }

        if (!EF->checkParameterPacketType(HelperFunctionParameterTy)) {
          fprintf(stderr, "Failed to export function %s: parameter type "
                          "mismatch during creation of helper function.\n",
                  EF->getName().c_str());

          const RSExportRecordType *Expected = EF->getParamPacketType();
          if (Expected) {
            fprintf(stderr, "Expected:\n");
            Expected->getLLVMType()->dump();
          }
          if (HelperFunctionParameterTy) {
            fprintf(stderr, "Got:\n");
            HelperFunctionParameterTy->dump();
          }
        }

        std::vector<llvm::Type*> Params;
        if (HelperFunctionParameterTy) {
          llvm::PointerType *HelperFunctionParameterTyP =
              llvm::PointerType::getUnqual(HelperFunctionParameterTy);
          Params.push_back(HelperFunctionParameterTyP);
        }

        llvm::FunctionType * HelperFunctionType =
            llvm::FunctionType::get(F->getReturnType(),
                                    Params,
                                    /* IsVarArgs = */false);

        HelperFunction =
            llvm::Function::Create(HelperFunctionType,
                                   llvm::GlobalValue::ExternalLinkage,
                                   HelperFunctionName,
                                   M);

        HelperFunction->addFnAttr(llvm::Attribute::NoInline);
        HelperFunction->setCallingConv(F->getCallingConv());

        // Create helper function body
        {
          llvm::Argument *HelperFunctionParameter =
              &(*HelperFunction->arg_begin());
          llvm::BasicBlock *BB =
              llvm::BasicBlock::Create(mLLVMContext, "entry", HelperFunction);
          llvm::IRBuilder<> *IB = new llvm::IRBuilder<>(BB);
          llvm::SmallVector<llvm::Value*, 6> Params;
          llvm::Value *Idx[2];

          Idx[0] =
              llvm::ConstantInt::get(llvm::Type::getInt32Ty(mLLVMContext), 0);

          // getelementptr and load instruction for all elements in
          // parameter .p
          for (size_t i = 0; i < EF->getNumParameters(); i++) {
            // getelementptr
            Idx[1] = llvm::ConstantInt::get(
              llvm::Type::getInt32Ty(mLLVMContext), i);

            llvm::Value *Ptr = NULL;

            Ptr = IB->CreateInBoundsGEP(HelperFunctionParameter, Idx);

            // Load is only required for non-struct ptrs
            if (isStructInput[i]) {
                Params.push_back(Ptr);
            } else {
                llvm::Value *V = IB->CreateLoad(Ptr);
                Params.push_back(V);
            }
          }

          // Call and pass the all elements as parameter to F
          llvm::CallInst *CI = IB->CreateCall(F, Params);

          CI->setCallingConv(F->getCallingConv());

          if (F->getReturnType() == llvm::Type::getVoidTy(mLLVMContext))
            IB->CreateRetVoid();
          else
            IB->CreateRet(CI);

          delete IB;
        }
      }

      ExportFuncInfo.push_back(
          llvm::MDString::get(mLLVMContext, HelperFunctionName.c_str()));
    }

    mExportFuncMetadata->addOperand(
        llvm::MDNode::get(mLLVMContext, ExportFuncInfo));
    ExportFuncInfo.clear();
  }
}

void Backend::dumpExportForEachInfo(llvm::Module *M) {
  if (mExportForEachNameMetadata == nullptr) {
    mExportForEachNameMetadata =
        M->getOrInsertNamedMetadata(RS_EXPORT_FOREACH_NAME_MN);
  }
  if (mExportForEachSignatureMetadata == nullptr) {
    mExportForEachSignatureMetadata =
        M->getOrInsertNamedMetadata(RS_EXPORT_FOREACH_MN);
  }

  llvm::SmallVector<llvm::Metadata *, 1> ExportForEachName;
  llvm::SmallVector<llvm::Metadata *, 1> ExportForEachInfo;

  for (RSContext::const_export_foreach_iterator
          I = mContext->export_foreach_begin(),
          E = mContext->export_foreach_end();
       I != E;
       I++) {
    const RSExportForEach *EFE = *I;

    ExportForEachName.push_back(
        llvm::MDString::get(mLLVMContext, EFE->getName().c_str()));

    mExportForEachNameMetadata->addOperand(
        llvm::MDNode::get(mLLVMContext, ExportForEachName));
    ExportForEachName.clear();

    ExportForEachInfo.push_back(
        llvm::MDString::get(mLLVMContext,
                            llvm::utostr_32(EFE->getSignatureMetadata())));

    mExportForEachSignatureMetadata->addOperand(
        llvm::MDNode::get(mLLVMContext, ExportForEachInfo));
    ExportForEachInfo.clear();
  }
}

void Backend::dumpExportTypeInfo(llvm::Module *M) {
  llvm::SmallVector<llvm::Metadata *, 1> ExportTypeInfo;

  for (RSContext::const_export_type_iterator
          I = mContext->export_types_begin(),
          E = mContext->export_types_end();
       I != E;
       I++) {
    // First, dump type name list to export
    const RSExportType *ET = I->getValue();

    ExportTypeInfo.clear();
    // Type name
    ExportTypeInfo.push_back(
        llvm::MDString::get(mLLVMContext, ET->getName().c_str()));

    if (ET->getClass() == RSExportType::ExportClassRecord) {
      const RSExportRecordType *ERT =
          static_cast<const RSExportRecordType*>(ET);

      if (mExportTypeMetadata == nullptr)
        mExportTypeMetadata =
            M->getOrInsertNamedMetadata(RS_EXPORT_TYPE_MN);

      mExportTypeMetadata->addOperand(
          llvm::MDNode::get(mLLVMContext, ExportTypeInfo));

      // Now, export struct field information to %[struct name]
      std::string StructInfoMetadataName("%");
      StructInfoMetadataName.append(ET->getName());
      llvm::NamedMDNode *StructInfoMetadata =
          M->getOrInsertNamedMetadata(StructInfoMetadataName);
      llvm::SmallVector<llvm::Metadata *, 3> FieldInfo;

      slangAssert(StructInfoMetadata->getNumOperands() == 0 &&
                  "Metadata with same name was created before");
      for (RSExportRecordType::const_field_iterator FI = ERT->fields_begin(),
              FE = ERT->fields_end();
           FI != FE;
           FI++) {
        const RSExportRecordType::Field *F = *FI;

        // 1. field name
        FieldInfo.push_back(llvm::MDString::get(mLLVMContext,
                                                F->getName().c_str()));

        // 2. field type name
        FieldInfo.push_back(
            llvm::MDString::get(mLLVMContext,
                                F->getType()->getName().c_str()));

        StructInfoMetadata->addOperand(
            llvm::MDNode::get(mLLVMContext, FieldInfo));
        FieldInfo.clear();
      }
    }   // ET->getClass() == RSExportType::ExportClassRecord
  }
}

void Backend::HandleTranslationUnitPost(llvm::Module *M) {

  if (!mContext->is64Bit()) {
    M->setDataLayout("e-p:32:32-i64:64-v128:64:128-n32-S64");
  }

  if (!mContext->processExport()) {
    return;
  }

  if (mContext->hasExportVar())
    dumpExportVarInfo(M);

  if (mContext->hasExportFunc())
    dumpExportFunctionInfo(M);

  if (mContext->hasExportForEach())
    dumpExportForEachInfo(M);

  if (mContext->hasExportType())
    dumpExportTypeInfo(M);
}

}  // namespace slang
