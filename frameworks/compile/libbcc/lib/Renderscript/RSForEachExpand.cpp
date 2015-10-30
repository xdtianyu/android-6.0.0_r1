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

#include "bcc/Assert.h"
#include "bcc/Renderscript/RSTransforms.h"

#include <cstdlib>
#include <functional>

#include <llvm/IR/DerivedTypes.h>
#include <llvm/IR/Function.h>
#include <llvm/IR/Instructions.h>
#include <llvm/IR/IRBuilder.h>
#include <llvm/IR/MDBuilder.h>
#include <llvm/IR/Module.h>
#include <llvm/Pass.h>
#include <llvm/Support/raw_ostream.h>
#include <llvm/IR/DataLayout.h>
#include <llvm/IR/Function.h>
#include <llvm/IR/Type.h>
#include <llvm/Transforms/Utils/BasicBlockUtils.h>

#include "bcc/Config/Config.h"
#include "bcc/Support/Log.h"

#include "bcinfo/MetadataExtractor.h"

#define NUM_EXPANDED_FUNCTION_PARAMS 4

using namespace bcc;

namespace {

static const bool gEnableRsTbaa = true;

/* RSForEachExpandPass - This pass operates on functions that are able to be
 * called via rsForEach() or "foreach_<NAME>". We create an inner loop for the
 * ForEach-able function to be invoked over the appropriate data cells of the
 * input/output allocations (adjusting other relevant parameters as we go). We
 * support doing this for any ForEach-able compute kernels. The new function
 * name is the original function name followed by ".expand". Note that we
 * still generate code for the original function.
 */
class RSForEachExpandPass : public llvm::ModulePass {
public:
  static char ID;

private:
  static const size_t RS_KERNEL_INPUT_LIMIT = 8; // see frameworks/base/libs/rs/cpu_ref/rsCpuCoreRuntime.h

  enum RsLaunchDimensionsField {
    RsLaunchDimensionsFieldX,
    RsLaunchDimensionsFieldY,
    RsLaunchDimensionsFieldZ,
    RsLaunchDimensionsFieldLod,
    RsLaunchDimensionsFieldFace,
    RsLaunchDimensionsFieldArray,

    RsLaunchDimensionsFieldCount
  };

  enum RsExpandKernelDriverInfoPfxField {
    RsExpandKernelDriverInfoPfxFieldInPtr,
    RsExpandKernelDriverInfoPfxFieldInStride,
    RsExpandKernelDriverInfoPfxFieldInLen,
    RsExpandKernelDriverInfoPfxFieldOutPtr,
    RsExpandKernelDriverInfoPfxFieldOutStride,
    RsExpandKernelDriverInfoPfxFieldOutLen,
    RsExpandKernelDriverInfoPfxFieldDim,
    RsExpandKernelDriverInfoPfxFieldCurrent,
    RsExpandKernelDriverInfoPfxFieldUsr,
    RsExpandKernelDriverInfoPfxFieldUsLenr,

    RsExpandKernelDriverInfoPfxFieldCount
  };

  llvm::Module *Module;
  llvm::LLVMContext *Context;

  /*
   * Pointer to LLVM type information for the the function signature
   * for expanded kernels.  This must be re-calculated for each
   * module the pass is run on.
   */
  llvm::FunctionType *ExpandedFunctionType;

  uint32_t mExportForEachCount;
  const char **mExportForEachNameList;
  const uint32_t *mExportForEachSignatureList;

  // Turns on optimization of allocation stride values.
  bool mEnableStepOpt;

  uint32_t getRootSignature(llvm::Function *Function) {
    const llvm::NamedMDNode *ExportForEachMetadata =
        Module->getNamedMetadata("#rs_export_foreach");

    if (!ExportForEachMetadata) {
      llvm::SmallVector<llvm::Type*, 8> RootArgTys;
      for (llvm::Function::arg_iterator B = Function->arg_begin(),
                                        E = Function->arg_end();
           B != E;
           ++B) {
        RootArgTys.push_back(B->getType());
      }

      // For pre-ICS bitcode, we may not have signature information. In that
      // case, we use the size of the RootArgTys to select the number of
      // arguments.
      return (1 << RootArgTys.size()) - 1;
    }

    if (ExportForEachMetadata->getNumOperands() == 0) {
      return 0;
    }

    bccAssert(ExportForEachMetadata->getNumOperands() > 0);

    // We only handle the case for legacy root() functions here, so this is
    // hard-coded to look at only the first such function.
    llvm::MDNode *SigNode = ExportForEachMetadata->getOperand(0);
    if (SigNode != nullptr && SigNode->getNumOperands() == 1) {
      llvm::Metadata *SigMD = SigNode->getOperand(0);
      if (llvm::MDString *SigS = llvm::dyn_cast<llvm::MDString>(SigMD)) {
        llvm::StringRef SigString = SigS->getString();
        uint32_t Signature = 0;
        if (SigString.getAsInteger(10, Signature)) {
          ALOGE("Non-integer signature value '%s'", SigString.str().c_str());
          return 0;
        }
        return Signature;
      }
    }

    return 0;
  }

  bool isStepOptSupported(llvm::Type *AllocType) {

    llvm::PointerType *PT = llvm::dyn_cast<llvm::PointerType>(AllocType);
    llvm::Type *VoidPtrTy = llvm::Type::getInt8PtrTy(*Context);

    if (mEnableStepOpt) {
      return false;
    }

    if (AllocType == VoidPtrTy) {
      return false;
    }

    if (!PT) {
      return false;
    }

    // remaining conditions are 64-bit only
    if (VoidPtrTy->getPrimitiveSizeInBits() == 32) {
      return true;
    }

    // coerce suggests an upconverted struct type, which we can't support
    if (AllocType->getStructName().find("coerce") != llvm::StringRef::npos) {
      return false;
    }

    // 2xi64 and i128 suggest an upconverted struct type, which are also unsupported
    llvm::Type *V2xi64Ty = llvm::VectorType::get(llvm::Type::getInt64Ty(*Context), 2);
    llvm::Type *Int128Ty = llvm::Type::getIntNTy(*Context, 128);
    if (AllocType == V2xi64Ty || AllocType == Int128Ty) {
      return false;
    }

    return true;
  }

  // Get the actual value we should use to step through an allocation.
  //
  // Normally the value we use to step through an allocation is given to us by
  // the driver. However, for certain primitive data types, we can derive an
  // integer constant for the step value. We use this integer constant whenever
  // possible to allow further compiler optimizations to take place.
  //
  // DL - Target Data size/layout information.
  // T - Type of allocation (should be a pointer).
  // OrigStep - Original step increment (root.expand() input from driver).
  llvm::Value *getStepValue(llvm::DataLayout *DL, llvm::Type *AllocType,
                            llvm::Value *OrigStep) {
    bccAssert(DL);
    bccAssert(AllocType);
    bccAssert(OrigStep);
    llvm::PointerType *PT = llvm::dyn_cast<llvm::PointerType>(AllocType);
    if (isStepOptSupported(AllocType)) {
      llvm::Type *ET = PT->getElementType();
      uint64_t ETSize = DL->getTypeAllocSize(ET);
      llvm::Type *Int32Ty = llvm::Type::getInt32Ty(*Context);
      return llvm::ConstantInt::get(Int32Ty, ETSize);
    } else {
      return OrigStep;
    }
  }

  /// Builds the types required by the pass for the given context.
  void buildTypes(void) {
    // Create the RsLaunchDimensionsTy and RsExpandKernelDriverInfoPfxTy structs.

    llvm::Type *Int8Ty                   = llvm::Type::getInt8Ty(*Context);
    llvm::Type *Int8PtrTy                = Int8Ty->getPointerTo();
    llvm::Type *Int8PtrArrayInputLimitTy = llvm::ArrayType::get(Int8PtrTy, RS_KERNEL_INPUT_LIMIT);
    llvm::Type *Int32Ty                  = llvm::Type::getInt32Ty(*Context);
    llvm::Type *Int32ArrayInputLimitTy   = llvm::ArrayType::get(Int32Ty, RS_KERNEL_INPUT_LIMIT);
    llvm::Type *VoidPtrTy                = llvm::Type::getInt8PtrTy(*Context);
    llvm::Type *Int32Array4Ty            = llvm::ArrayType::get(Int32Ty, 4);

    /* Defined in frameworks/base/libs/rs/cpu_ref/rsCpuCore.h:
     *
     * struct RsLaunchDimensions {
     *   uint32_t x;
     *   uint32_t y;
     *   uint32_t z;
     *   uint32_t lod;
     *   uint32_t face;
     *   uint32_t array[4];
     * };
     */
    llvm::SmallVector<llvm::Type*, RsLaunchDimensionsFieldCount> RsLaunchDimensionsTypes;
    RsLaunchDimensionsTypes.push_back(Int32Ty);       // uint32_t x
    RsLaunchDimensionsTypes.push_back(Int32Ty);       // uint32_t y
    RsLaunchDimensionsTypes.push_back(Int32Ty);       // uint32_t z
    RsLaunchDimensionsTypes.push_back(Int32Ty);       // uint32_t lod
    RsLaunchDimensionsTypes.push_back(Int32Ty);       // uint32_t face
    RsLaunchDimensionsTypes.push_back(Int32Array4Ty); // uint32_t array[4]
    llvm::StructType *RsLaunchDimensionsTy =
        llvm::StructType::create(RsLaunchDimensionsTypes, "RsLaunchDimensions");

    /* Defined as the beginning of RsExpandKernelDriverInfo in frameworks/base/libs/rs/cpu_ref/rsCpuCoreRuntime.h:
     *
     * struct RsExpandKernelDriverInfoPfx {
     *     const uint8_t *inPtr[RS_KERNEL_INPUT_LIMIT];
     *     uint32_t inStride[RS_KERNEL_INPUT_LIMIT];
     *     uint32_t inLen;
     *
     *     uint8_t *outPtr[RS_KERNEL_INPUT_LIMIT];
     *     uint32_t outStride[RS_KERNEL_INPUT_LIMIT];
     *     uint32_t outLen;
     *
     *     // Dimension of the launch
     *     RsLaunchDimensions dim;
     *
     *     // The walking iterator of the launch
     *     RsLaunchDimensions current;
     *
     *     const void *usr;
     *     uint32_t usrLen;
     *
     *     // Items below this line are not used by the compiler and can be change in the driver.
     *     // So the compiler must assume there are an unknown number of fields of unknown type
     *     // beginning here.
     * };
     *
     * The name "RsExpandKernelDriverInfoPfx" is known to RSInvariantPass (RSInvariant.cpp).
     */
    llvm::SmallVector<llvm::Type*, RsExpandKernelDriverInfoPfxFieldCount> RsExpandKernelDriverInfoPfxTypes;
    RsExpandKernelDriverInfoPfxTypes.push_back(Int8PtrArrayInputLimitTy); // const uint8_t *inPtr[RS_KERNEL_INPUT_LIMIT]
    RsExpandKernelDriverInfoPfxTypes.push_back(Int32ArrayInputLimitTy);   // uint32_t inStride[RS_KERNEL_INPUT_LIMIT]
    RsExpandKernelDriverInfoPfxTypes.push_back(Int32Ty);                  // uint32_t inLen
    RsExpandKernelDriverInfoPfxTypes.push_back(Int8PtrArrayInputLimitTy); // uint8_t *outPtr[RS_KERNEL_INPUT_LIMIT]
    RsExpandKernelDriverInfoPfxTypes.push_back(Int32ArrayInputLimitTy);   // uint32_t outStride[RS_KERNEL_INPUT_LIMIT]
    RsExpandKernelDriverInfoPfxTypes.push_back(Int32Ty);                  // uint32_t outLen
    RsExpandKernelDriverInfoPfxTypes.push_back(RsLaunchDimensionsTy);     // RsLaunchDimensions dim
    RsExpandKernelDriverInfoPfxTypes.push_back(RsLaunchDimensionsTy);     // RsLaunchDimensions current
    RsExpandKernelDriverInfoPfxTypes.push_back(VoidPtrTy);                // const void *usr
    RsExpandKernelDriverInfoPfxTypes.push_back(Int32Ty);                  // uint32_t usrLen
    llvm::StructType *RsExpandKernelDriverInfoPfxTy =
        llvm::StructType::create(RsExpandKernelDriverInfoPfxTypes, "RsExpandKernelDriverInfoPfx");

    // Create the function type for expanded kernels.

    llvm::Type *RsExpandKernelDriverInfoPfxPtrTy = RsExpandKernelDriverInfoPfxTy->getPointerTo();

    llvm::SmallVector<llvm::Type*, 8> ParamTypes;
    ParamTypes.push_back(RsExpandKernelDriverInfoPfxPtrTy); // const RsExpandKernelDriverInfoPfx *p
    ParamTypes.push_back(Int32Ty);                          // uint32_t x1
    ParamTypes.push_back(Int32Ty);                          // uint32_t x2
    ParamTypes.push_back(Int32Ty);                          // uint32_t outstep

    ExpandedFunctionType =
        llvm::FunctionType::get(llvm::Type::getVoidTy(*Context), ParamTypes,
                                false);
  }

  /// @brief Create skeleton of the expanded function.
  ///
  /// This creates a function with the following signature:
  ///
  ///   void (const RsForEachStubParamStruct *p, uint32_t x1, uint32_t x2,
  ///         uint32_t outstep)
  ///
  llvm::Function *createEmptyExpandedFunction(llvm::StringRef OldName) {
    llvm::Function *ExpandedFunction =
      llvm::Function::Create(ExpandedFunctionType,
                             llvm::GlobalValue::ExternalLinkage,
                             OldName + ".expand", Module);

    bccAssert(ExpandedFunction->arg_size() == NUM_EXPANDED_FUNCTION_PARAMS);

    llvm::Function::arg_iterator AI = ExpandedFunction->arg_begin();

    (AI++)->setName("p");
    (AI++)->setName("x1");
    (AI++)->setName("x2");
    (AI++)->setName("arg_outstep");

    llvm::BasicBlock *Begin = llvm::BasicBlock::Create(*Context, "Begin",
                                                       ExpandedFunction);
    llvm::IRBuilder<> Builder(Begin);
    Builder.CreateRetVoid();

    return ExpandedFunction;
  }

  /// @brief Create an empty loop
  ///
  /// Create a loop of the form:
  ///
  /// for (i = LowerBound; i < UpperBound; i++)
  ///   ;
  ///
  /// After the loop has been created, the builder is set such that
  /// instructions can be added to the loop body.
  ///
  /// @param Builder The builder to use to build this loop. The current
  ///                position of the builder is the position the loop
  ///                will be inserted.
  /// @param LowerBound The first value of the loop iterator
  /// @param UpperBound The maximal value of the loop iterator
  /// @param LoopIV A reference that will be set to the loop iterator.
  /// @return The BasicBlock that will be executed after the loop.
  llvm::BasicBlock *createLoop(llvm::IRBuilder<> &Builder,
                               llvm::Value *LowerBound,
                               llvm::Value *UpperBound,
                               llvm::PHINode **LoopIV) {
    assert(LowerBound->getType() == UpperBound->getType());

    llvm::BasicBlock *CondBB, *AfterBB, *HeaderBB;
    llvm::Value *Cond, *IVNext;
    llvm::PHINode *IV;

    CondBB = Builder.GetInsertBlock();
    AfterBB = llvm::SplitBlock(CondBB, Builder.GetInsertPoint(), nullptr, nullptr);
    HeaderBB = llvm::BasicBlock::Create(*Context, "Loop", CondBB->getParent());

    // if (LowerBound < Upperbound)
    //   goto LoopHeader
    // else
    //   goto AfterBB
    CondBB->getTerminator()->eraseFromParent();
    Builder.SetInsertPoint(CondBB);
    Cond = Builder.CreateICmpULT(LowerBound, UpperBound);
    Builder.CreateCondBr(Cond, HeaderBB, AfterBB);

    // iv = PHI [CondBB -> LowerBound], [LoopHeader -> NextIV ]
    // iv.next = iv + 1
    // if (iv.next < Upperbound)
    //   goto LoopHeader
    // else
    //   goto AfterBB
    Builder.SetInsertPoint(HeaderBB);
    IV = Builder.CreatePHI(LowerBound->getType(), 2, "X");
    IV->addIncoming(LowerBound, CondBB);
    IVNext = Builder.CreateNUWAdd(IV, Builder.getInt32(1));
    IV->addIncoming(IVNext, HeaderBB);
    Cond = Builder.CreateICmpULT(IVNext, UpperBound);
    Builder.CreateCondBr(Cond, HeaderBB, AfterBB);
    AfterBB->setName("Exit");
    Builder.SetInsertPoint(HeaderBB->getFirstNonPHI());
    *LoopIV = IV;
    return AfterBB;
  }

  // Finish building the outgoing argument list for calling a ForEach-able function.
  //
  // ArgVector - on input, the non-special arguments
  //             on output, the non-special arguments combined with the special arguments
  //               from SpecialArgVector
  // SpecialArgVector - special arguments (from ExpandSpecialArguments())
  // SpecialArgContextIdx - return value of ExpandSpecialArguments()
  //                          (position of context argument in SpecialArgVector)
  // CalleeFunction - the ForEach-able function being called
  // Builder - for inserting code into the caller function
  template<unsigned int ArgVectorLen, unsigned int SpecialArgVectorLen>
  void finishArgList(      llvm::SmallVector<llvm::Value *, ArgVectorLen>        &ArgVector,
                     const llvm::SmallVector<llvm::Value *, SpecialArgVectorLen> &SpecialArgVector,
                     const int SpecialArgContextIdx,
                     const llvm::Function &CalleeFunction,
                     llvm::IRBuilder<> &CallerBuilder) {
    /* The context argument (if any) is a pointer to an opaque user-visible type that differs from
     * the RsExpandKernelDriverInfoPfx type used in the function we are generating (although the
     * two types represent the same thing).  Therefore, we must introduce a pointer cast when
     * generating a call to the kernel function.
     */
    const int ArgContextIdx =
        SpecialArgContextIdx >= 0 ? (ArgVector.size() + SpecialArgContextIdx) : SpecialArgContextIdx;
    ArgVector.append(SpecialArgVector.begin(), SpecialArgVector.end());
    if (ArgContextIdx >= 0) {
      llvm::Type *ContextArgType = nullptr;
      int ArgIdx = ArgContextIdx;
      for (const auto &Arg : CalleeFunction.getArgumentList()) {
        if (!ArgIdx--) {
          ContextArgType = Arg.getType();
          break;
        }
      }
      bccAssert(ContextArgType);
      ArgVector[ArgContextIdx] = CallerBuilder.CreatePointerCast(ArgVector[ArgContextIdx], ContextArgType);
    }
  }

public:
  RSForEachExpandPass(bool pEnableStepOpt = true)
      : ModulePass(ID), Module(nullptr), Context(nullptr),
        mEnableStepOpt(pEnableStepOpt) {

  }

  virtual void getAnalysisUsage(llvm::AnalysisUsage &AU) const override {
    // This pass does not use any other analysis passes, but it does
    // add/wrap the existing functions in the module (thus altering the CFG).
  }

  // Build contribution to outgoing argument list for calling a
  // ForEach-able function, based on the special parameters of that
  // function.
  //
  // Signature - metadata bits for the signature of the ForEach-able function
  // X, Arg_p - values derived directly from expanded function,
  //            suitable for computing arguments for the ForEach-able function
  // CalleeArgs - contribution is accumulated here
  // Bump - invoked once for each contributed outgoing argument
  //
  // Return value is the (zero-based) position of the context (Arg_p)
  // argument in the CalleeArgs vector, or a negative value if the
  // context argument is not placed in the CalleeArgs vector.
  int ExpandSpecialArguments(uint32_t Signature,
                             llvm::Value *X,
                             llvm::Value *Arg_p,
                             llvm::IRBuilder<> &Builder,
                             llvm::SmallVector<llvm::Value*, 8> &CalleeArgs,
                             std::function<void ()> Bump) {

    bccAssert(CalleeArgs.empty());

    int Return = -1;
    if (bcinfo::MetadataExtractor::hasForEachSignatureCtxt(Signature)) {
      CalleeArgs.push_back(Arg_p);
      Bump();
      Return = CalleeArgs.size() - 1;
    }

    if (bcinfo::MetadataExtractor::hasForEachSignatureX(Signature)) {
      CalleeArgs.push_back(X);
      Bump();
    }

    if (bcinfo::MetadataExtractor::hasForEachSignatureY(Signature) ||
        bcinfo::MetadataExtractor::hasForEachSignatureZ(Signature)) {

      llvm::Value *Current = Builder.CreateStructGEP(nullptr, Arg_p, RsExpandKernelDriverInfoPfxFieldCurrent);

      if (bcinfo::MetadataExtractor::hasForEachSignatureY(Signature)) {
        llvm::Value *Y = Builder.CreateLoad(
            Builder.CreateStructGEP(nullptr, Current, RsLaunchDimensionsFieldY), "Y");

        CalleeArgs.push_back(Y);
        Bump();
      }

      if (bcinfo::MetadataExtractor::hasForEachSignatureZ(Signature)) {
        llvm::Value *Z = Builder.CreateLoad(
            Builder.CreateStructGEP(nullptr, Current, RsLaunchDimensionsFieldZ), "Z");
        CalleeArgs.push_back(Z);
        Bump();
      }
    }

    return Return;
  }

  /* Performs the actual optimization on a selected function. On success, the
   * Module will contain a new function of the name "<NAME>.expand" that
   * invokes <NAME>() in a loop with the appropriate parameters.
   */
  bool ExpandFunction(llvm::Function *Function, uint32_t Signature) {
    ALOGV("Expanding ForEach-able Function %s",
          Function->getName().str().c_str());

    if (!Signature) {
      Signature = getRootSignature(Function);
      if (!Signature) {
        // We couldn't determine how to expand this function based on its
        // function signature.
        return false;
      }
    }

    llvm::DataLayout DL(Module);

    llvm::Function *ExpandedFunction =
      createEmptyExpandedFunction(Function->getName());

    /*
     * Extract the expanded function's parameters.  It is guaranteed by
     * createEmptyExpandedFunction that there will be five parameters.
     */

    bccAssert(ExpandedFunction->arg_size() == NUM_EXPANDED_FUNCTION_PARAMS);

    llvm::Function::arg_iterator ExpandedFunctionArgIter =
      ExpandedFunction->arg_begin();

    llvm::Value *Arg_p       = &*(ExpandedFunctionArgIter++);
    llvm::Value *Arg_x1      = &*(ExpandedFunctionArgIter++);
    llvm::Value *Arg_x2      = &*(ExpandedFunctionArgIter++);
    llvm::Value *Arg_outstep = &*(ExpandedFunctionArgIter);

    llvm::Value *InStep  = nullptr;
    llvm::Value *OutStep = nullptr;

    // Construct the actual function body.
    llvm::IRBuilder<> Builder(ExpandedFunction->getEntryBlock().begin());

    // Collect and construct the arguments for the kernel().
    // Note that we load any loop-invariant arguments before entering the Loop.
    llvm::Function::arg_iterator FunctionArgIter = Function->arg_begin();

    llvm::Type  *InTy      = nullptr;
    llvm::Value *InBasePtr = nullptr;
    if (bcinfo::MetadataExtractor::hasForEachSignatureIn(Signature)) {
      llvm::Value *InsBasePtr  = Builder.CreateStructGEP(nullptr, Arg_p, RsExpandKernelDriverInfoPfxFieldInPtr, "inputs_base");

      llvm::Value *InStepsBase = Builder.CreateStructGEP(nullptr, Arg_p, RsExpandKernelDriverInfoPfxFieldInStride, "insteps_base");

      llvm::Value    *InStepAddr = Builder.CreateConstInBoundsGEP2_32(nullptr, InStepsBase, 0, 0);
      llvm::LoadInst *InStepArg  = Builder.CreateLoad(InStepAddr,
                                                      "instep_addr");

      InTy = (FunctionArgIter++)->getType();
      InStep = getStepValue(&DL, InTy, InStepArg);

      InStep->setName("instep");

      llvm::Value *InputAddr = Builder.CreateConstInBoundsGEP2_32(nullptr, InsBasePtr, 0, 0);
      InBasePtr = Builder.CreateLoad(InputAddr, "input_base");
    }

    llvm::Type *OutTy = nullptr;
    llvm::Value *OutBasePtr = nullptr;
    if (bcinfo::MetadataExtractor::hasForEachSignatureOut(Signature)) {
      OutTy = (FunctionArgIter++)->getType();
      OutStep = getStepValue(&DL, OutTy, Arg_outstep);
      OutStep->setName("outstep");
      OutBasePtr = Builder.CreateLoad(
                     Builder.CreateConstInBoundsGEP2_32(nullptr,
                         Builder.CreateStructGEP(nullptr, Arg_p, RsExpandKernelDriverInfoPfxFieldOutPtr),
                         0, 0));
    }

    llvm::Value *UsrData = nullptr;
    if (bcinfo::MetadataExtractor::hasForEachSignatureUsrData(Signature)) {
      llvm::Type *UsrDataTy = (FunctionArgIter++)->getType();
      UsrData = Builder.CreatePointerCast(Builder.CreateLoad(
          Builder.CreateStructGEP(nullptr, Arg_p,  RsExpandKernelDriverInfoPfxFieldUsr)), UsrDataTy);
      UsrData->setName("UsrData");
    }

    llvm::PHINode *IV;
    createLoop(Builder, Arg_x1, Arg_x2, &IV);

    llvm::SmallVector<llvm::Value*, 8> CalleeArgs;
    const int CalleeArgsContextIdx = ExpandSpecialArguments(Signature, IV, Arg_p, Builder, CalleeArgs,
                                                            [&FunctionArgIter]() { FunctionArgIter++; });

    bccAssert(FunctionArgIter == Function->arg_end());

    // Populate the actual call to kernel().
    llvm::SmallVector<llvm::Value*, 8> RootArgs;

    llvm::Value *InPtr  = nullptr;
    llvm::Value *OutPtr = nullptr;

    // Calculate the current input and output pointers
    //
    // We always calculate the input/output pointers with a GEP operating on i8
    // values and only cast at the very end to OutTy. This is because the step
    // between two values is given in bytes.
    //
    // TODO: We could further optimize the output by using a GEP operation of
    // type 'OutTy' in cases where the element type of the allocation allows.
    if (OutBasePtr) {
      llvm::Value *OutOffset = Builder.CreateSub(IV, Arg_x1);
      OutOffset = Builder.CreateMul(OutOffset, OutStep);
      OutPtr = Builder.CreateGEP(OutBasePtr, OutOffset);
      OutPtr = Builder.CreatePointerCast(OutPtr, OutTy);
    }

    if (InBasePtr) {
      llvm::Value *InOffset = Builder.CreateSub(IV, Arg_x1);
      InOffset = Builder.CreateMul(InOffset, InStep);
      InPtr = Builder.CreateGEP(InBasePtr, InOffset);
      InPtr = Builder.CreatePointerCast(InPtr, InTy);
    }

    if (InPtr) {
      RootArgs.push_back(InPtr);
    }

    if (OutPtr) {
      RootArgs.push_back(OutPtr);
    }

    if (UsrData) {
      RootArgs.push_back(UsrData);
    }

    finishArgList(RootArgs, CalleeArgs, CalleeArgsContextIdx, *Function, Builder);

    Builder.CreateCall(Function, RootArgs);

    return true;
  }

  /* Expand a pass-by-value kernel.
   */
  bool ExpandKernel(llvm::Function *Function, uint32_t Signature) {
    bccAssert(bcinfo::MetadataExtractor::hasForEachSignatureKernel(Signature));
    ALOGV("Expanding kernel Function %s", Function->getName().str().c_str());

    // TODO: Refactor this to share functionality with ExpandFunction.
    llvm::DataLayout DL(Module);

    llvm::Function *ExpandedFunction =
      createEmptyExpandedFunction(Function->getName());

    /*
     * Extract the expanded function's parameters.  It is guaranteed by
     * createEmptyExpandedFunction that there will be five parameters.
     */

    bccAssert(ExpandedFunction->arg_size() == NUM_EXPANDED_FUNCTION_PARAMS);

    llvm::Function::arg_iterator ExpandedFunctionArgIter =
      ExpandedFunction->arg_begin();

    llvm::Value *Arg_p       = &*(ExpandedFunctionArgIter++);
    llvm::Value *Arg_x1      = &*(ExpandedFunctionArgIter++);
    llvm::Value *Arg_x2      = &*(ExpandedFunctionArgIter++);
    llvm::Value *Arg_outstep = &*(ExpandedFunctionArgIter);

    // Construct the actual function body.
    llvm::IRBuilder<> Builder(ExpandedFunction->getEntryBlock().begin());

    // Create TBAA meta-data.
    llvm::MDNode *TBAARenderScriptDistinct, *TBAARenderScript,
                 *TBAAAllocation, *TBAAPointer;
    llvm::MDBuilder MDHelper(*Context);

    TBAARenderScriptDistinct =
      MDHelper.createTBAARoot("RenderScript Distinct TBAA");
    TBAARenderScript = MDHelper.createTBAANode("RenderScript TBAA",
        TBAARenderScriptDistinct);
    TBAAAllocation = MDHelper.createTBAAScalarTypeNode("allocation",
                                                       TBAARenderScript);
    TBAAAllocation = MDHelper.createTBAAStructTagNode(TBAAAllocation,
                                                      TBAAAllocation, 0);
    TBAAPointer = MDHelper.createTBAAScalarTypeNode("pointer",
                                                    TBAARenderScript);
    TBAAPointer = MDHelper.createTBAAStructTagNode(TBAAPointer, TBAAPointer, 0);

    llvm::MDNode *AliasingDomain, *AliasingScope;
    AliasingDomain = MDHelper.createAnonymousAliasScopeDomain("RS argument scope domain");
    AliasingScope = MDHelper.createAnonymousAliasScope(AliasingDomain, "RS argument scope");

    /*
     * Collect and construct the arguments for the kernel().
     *
     * Note that we load any loop-invariant arguments before entering the Loop.
     */
    size_t NumInputs = Function->arg_size();

    // No usrData parameter on kernels.
    bccAssert(
        !bcinfo::MetadataExtractor::hasForEachSignatureUsrData(Signature));

    llvm::Function::arg_iterator ArgIter = Function->arg_begin();

    // Check the return type
    llvm::Type     *OutTy            = nullptr;
    llvm::Value    *OutStep          = nullptr;
    llvm::LoadInst *OutBasePtr       = nullptr;
    llvm::Value    *CastedOutBasePtr = nullptr;

    bool PassOutByPointer = false;

    if (bcinfo::MetadataExtractor::hasForEachSignatureOut(Signature)) {
      llvm::Type *OutBaseTy = Function->getReturnType();

      if (OutBaseTy->isVoidTy()) {
        PassOutByPointer = true;
        OutTy = ArgIter->getType();

        ArgIter++;
        --NumInputs;
      } else {
        // We don't increment Args, since we are using the actual return type.
        OutTy = OutBaseTy->getPointerTo();
      }

      OutStep = getStepValue(&DL, OutTy, Arg_outstep);
      OutStep->setName("outstep");
      OutBasePtr = Builder.CreateLoad(
                     Builder.CreateConstInBoundsGEP2_32(nullptr,
                         Builder.CreateStructGEP(nullptr, Arg_p, RsExpandKernelDriverInfoPfxFieldOutPtr),
                         0, 0));

      if (gEnableRsTbaa) {
        OutBasePtr->setMetadata("tbaa", TBAAPointer);
      }

      OutBasePtr->setMetadata("alias.scope", AliasingScope);

      CastedOutBasePtr = Builder.CreatePointerCast(OutBasePtr, OutTy, "casted_out");
    }

    llvm::PHINode *IV;
    createLoop(Builder, Arg_x1, Arg_x2, &IV);

    llvm::SmallVector<llvm::Value*, 8> CalleeArgs;
    const int CalleeArgsContextIdx = ExpandSpecialArguments(Signature, IV, Arg_p, Builder, CalleeArgs,
                                                            [&NumInputs]() { --NumInputs; });

    llvm::SmallVector<llvm::Type*,  8> InTypes;
    llvm::SmallVector<llvm::Value*, 8> InSteps;
    llvm::SmallVector<llvm::Value*, 8> InBasePtrs;
    llvm::SmallVector<llvm::Value*, 8> InStructTempSlots;

    bccAssert(NumInputs <= RS_KERNEL_INPUT_LIMIT);

    if (NumInputs > 0) {
      llvm::Value *InsBasePtr  = Builder.CreateStructGEP(nullptr, Arg_p, RsExpandKernelDriverInfoPfxFieldInPtr, "inputs_base");

      llvm::Value *InStepsBase = Builder.CreateStructGEP(nullptr, Arg_p, RsExpandKernelDriverInfoPfxFieldInStride, "insteps_base");

      llvm::Instruction *AllocaInsertionPoint = &*ExpandedFunction->getEntryBlock().begin();
      for (size_t InputIndex = 0; InputIndex < NumInputs;
           ++InputIndex, ArgIter++) {

        llvm::Value    *InStepAddr = Builder.CreateConstInBoundsGEP2_32(nullptr, InStepsBase, 0, InputIndex);
        llvm::LoadInst *InStepArg  = Builder.CreateLoad(InStepAddr,
                                                          "instep_addr");

        llvm::Type *InType = ArgIter->getType();

        /*
         * AArch64 calling conventions dictate that structs of sufficient size
         * get passed by pointer instead of passed by value.  This, combined
         * with the fact that we don't allow kernels to operate on pointer
         * data means that if we see a kernel with a pointer parameter we know
         * that it is struct input that has been promoted.  As such we don't
         * need to convert its type to a pointer.  Later we will need to know
         * to create a temporary copy on the stack, so we save this information
         * in InStructTempSlots.
         */
        if (auto PtrType = llvm::dyn_cast<llvm::PointerType>(InType)) {
          llvm::Type *ElementType = PtrType->getElementType();
          uint64_t Alignment = DL.getABITypeAlignment(ElementType);
          llvm::Value *Slot = new llvm::AllocaInst(ElementType,
                                                   nullptr,
                                                   Alignment,
                                                   "input_struct_slot",
                                                   AllocaInsertionPoint);
          InStructTempSlots.push_back(Slot);
        } else {
          InType = InType->getPointerTo();
          InStructTempSlots.push_back(nullptr);
        }

        llvm::Value *InStep = getStepValue(&DL, InType, InStepArg);

        InStep->setName("instep");

        llvm::Value    *InputAddr = Builder.CreateConstInBoundsGEP2_32(nullptr, InsBasePtr, 0, InputIndex);
        llvm::LoadInst *InBasePtr = Builder.CreateLoad(InputAddr,
                                                         "input_base");
        llvm::Value    *CastInBasePtr = Builder.CreatePointerCast(InBasePtr,
                                                                    InType, "casted_in");
        if (gEnableRsTbaa) {
          InBasePtr->setMetadata("tbaa", TBAAPointer);
        }

        InBasePtr->setMetadata("alias.scope", AliasingScope);

        InTypes.push_back(InType);
        InSteps.push_back(InStep);
        InBasePtrs.push_back(CastInBasePtr);
      }
    }

    // Populate the actual call to kernel().
    llvm::SmallVector<llvm::Value*, 8> RootArgs;

    // Calculate the current input and output pointers
    //
    //
    // We always calculate the input/output pointers with a GEP operating on i8
    // values combined with a multiplication and only cast at the very end to
    // OutTy.  This is to account for dynamic stepping sizes when the value
    // isn't apparent at compile time.  In the (very common) case when we know
    // the step size at compile time, due to haveing complete type information
    // this multiplication will optmized out and produces code equivalent to a
    // a GEP on a pointer of the correct type.

    // Output

    llvm::Value *OutPtr = nullptr;
    if (CastedOutBasePtr) {
      llvm::Value *OutOffset = Builder.CreateSub(IV, Arg_x1);

      OutPtr    = Builder.CreateGEP(CastedOutBasePtr, OutOffset);

      if (PassOutByPointer) {
        RootArgs.push_back(OutPtr);
      }
    }

    // Inputs

    if (NumInputs > 0) {
      llvm::Value *Offset = Builder.CreateSub(IV, Arg_x1);

      for (size_t Index = 0; Index < NumInputs; ++Index) {
        llvm::Value *InPtr    = Builder.CreateGEP(InBasePtrs[Index], Offset);
        llvm::Value *Input;

        if (llvm::Value *TemporarySlot = InStructTempSlots[Index]) {
          // Pass a pointer to a temporary on the stack, rather than
          // passing a pointer to the original value. We do not want
          // the kernel to potentially modify the input data.

          llvm::Type *ElementType = llvm::cast<llvm::PointerType>(
                                        InPtr->getType())->getElementType();
          uint64_t StoreSize = DL.getTypeStoreSize(ElementType);
          uint64_t Alignment = DL.getABITypeAlignment(ElementType);

          Builder.CreateMemCpy(TemporarySlot, InPtr, StoreSize, Alignment,
                               /* isVolatile = */ false,
                               /* !tbaa = */ gEnableRsTbaa ? TBAAAllocation : nullptr,
                               /* !tbaa.struct = */ nullptr,
                               /* !alias.scope = */ AliasingScope);
          Input = TemporarySlot;
        } else {
          llvm::LoadInst *InputLoad = Builder.CreateLoad(InPtr, "input");

          if (gEnableRsTbaa) {
            InputLoad->setMetadata("tbaa", TBAAAllocation);
          }

          InputLoad->setMetadata("alias.scope", AliasingScope);

          Input = InputLoad;
        }

        RootArgs.push_back(Input);
      }
    }

    finishArgList(RootArgs, CalleeArgs, CalleeArgsContextIdx, *Function, Builder);

    llvm::Value *RetVal = Builder.CreateCall(Function, RootArgs);

    if (OutPtr && !PassOutByPointer) {
      llvm::StoreInst *Store = Builder.CreateStore(RetVal, OutPtr);
      if (gEnableRsTbaa) {
        Store->setMetadata("tbaa", TBAAAllocation);
      }
      Store->setMetadata("alias.scope", AliasingScope);
    }

    return true;
  }

  /// @brief Checks if pointers to allocation internals are exposed
  ///
  /// This function verifies if through the parameters passed to the kernel
  /// or through calls to the runtime library the script gains access to
  /// pointers pointing to data within a RenderScript Allocation.
  /// If we know we control all loads from and stores to data within
  /// RenderScript allocations and if we know the run-time internal accesses
  /// are all annotated with RenderScript TBAA metadata, only then we
  /// can safely use TBAA to distinguish between generic and from-allocation
  /// pointers.
  bool allocPointersExposed(llvm::Module &Module) {
    // Old style kernel function can expose pointers to elements within
    // allocations.
    // TODO: Extend analysis to allow simple cases of old-style kernels.
    for (size_t i = 0; i < mExportForEachCount; ++i) {
      const char *Name = mExportForEachNameList[i];
      uint32_t Signature = mExportForEachSignatureList[i];
      if (Module.getFunction(Name) &&
          !bcinfo::MetadataExtractor::hasForEachSignatureKernel(Signature)) {
        return true;
      }
    }

    // Check for library functions that expose a pointer to an Allocation or
    // that are not yet annotated with RenderScript-specific tbaa information.
    static std::vector<std::string> Funcs;

    // rsGetElementAt(...)
    Funcs.push_back("_Z14rsGetElementAt13rs_allocationj");
    Funcs.push_back("_Z14rsGetElementAt13rs_allocationjj");
    Funcs.push_back("_Z14rsGetElementAt13rs_allocationjjj");
    // rsSetElementAt()
    Funcs.push_back("_Z14rsSetElementAt13rs_allocationPvj");
    Funcs.push_back("_Z14rsSetElementAt13rs_allocationPvjj");
    Funcs.push_back("_Z14rsSetElementAt13rs_allocationPvjjj");
    // rsGetElementAtYuv_uchar_Y()
    Funcs.push_back("_Z25rsGetElementAtYuv_uchar_Y13rs_allocationjj");
    // rsGetElementAtYuv_uchar_U()
    Funcs.push_back("_Z25rsGetElementAtYuv_uchar_U13rs_allocationjj");
    // rsGetElementAtYuv_uchar_V()
    Funcs.push_back("_Z25rsGetElementAtYuv_uchar_V13rs_allocationjj");

    for (std::vector<std::string>::iterator FI = Funcs.begin(),
                                            FE = Funcs.end();
         FI != FE; ++FI) {
      llvm::Function *Function = Module.getFunction(*FI);

      if (!Function) {
        ALOGE("Missing run-time function '%s'", FI->c_str());
        return true;
      }

      if (Function->getNumUses() > 0) {
        return true;
      }
    }

    return false;
  }

  /// @brief Connect RenderScript TBAA metadata to C/C++ metadata
  ///
  /// The TBAA metadata used to annotate loads/stores from RenderScript
  /// Allocations is generated in a separate TBAA tree with a
  /// "RenderScript Distinct TBAA" root node. LLVM does assume may-alias for
  /// all nodes in unrelated alias analysis trees. This function makes the
  /// "RenderScript TBAA" node (which is parented by the Distinct TBAA root),
  /// a subtree of the normal C/C++ TBAA tree aside of normal C/C++ types. With
  /// the connected trees every access to an Allocation is resolved to
  /// must-alias if compared to a normal C/C++ access.
  void connectRenderScriptTBAAMetadata(llvm::Module &Module) {
    llvm::MDBuilder MDHelper(*Context);
    llvm::MDNode *TBAARenderScriptDistinct =
      MDHelper.createTBAARoot("RenderScript Distinct TBAA");
    llvm::MDNode *TBAARenderScript = MDHelper.createTBAANode(
        "RenderScript TBAA", TBAARenderScriptDistinct);
    llvm::MDNode *TBAARoot     = MDHelper.createTBAARoot("Simple C/C++ TBAA");
    TBAARenderScript->replaceOperandWith(1, TBAARoot);
  }

  virtual bool runOnModule(llvm::Module &Module) {
    bool Changed  = false;
    this->Module  = &Module;
    this->Context = &Module.getContext();

    this->buildTypes();

    bcinfo::MetadataExtractor me(&Module);
    if (!me.extract()) {
      ALOGE("Could not extract metadata from module!");
      return false;
    }
    mExportForEachCount = me.getExportForEachSignatureCount();
    mExportForEachNameList = me.getExportForEachNameList();
    mExportForEachSignatureList = me.getExportForEachSignatureList();

    bool AllocsExposed = allocPointersExposed(Module);

    for (size_t i = 0; i < mExportForEachCount; ++i) {
      const char *name = mExportForEachNameList[i];
      uint32_t signature = mExportForEachSignatureList[i];
      llvm::Function *kernel = Module.getFunction(name);
      if (kernel) {
        if (bcinfo::MetadataExtractor::hasForEachSignatureKernel(signature)) {
          Changed |= ExpandKernel(kernel, signature);
          kernel->setLinkage(llvm::GlobalValue::InternalLinkage);
        } else if (kernel->getReturnType()->isVoidTy()) {
          Changed |= ExpandFunction(kernel, signature);
          kernel->setLinkage(llvm::GlobalValue::InternalLinkage);
        } else {
          // There are some graphics root functions that are not
          // expanded, but that will be called directly. For those
          // functions, we can not set the linkage to internal.
        }
      }
    }

    if (gEnableRsTbaa && !AllocsExposed) {
      connectRenderScriptTBAAMetadata(Module);
    }

    return Changed;
  }

  virtual const char *getPassName() const {
    return "ForEach-able Function Expansion";
  }

}; // end RSForEachExpandPass

} // end anonymous namespace

char RSForEachExpandPass::ID = 0;
static llvm::RegisterPass<RSForEachExpandPass> X("foreachexp", "ForEach Expand Pass");

namespace bcc {

llvm::ModulePass *
createRSForEachExpandPass(bool pEnableStepOpt){
  return new RSForEachExpandPass(pEnableStepOpt);
}

} // end namespace bcc
