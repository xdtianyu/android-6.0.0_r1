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

#include "bcinfo/MetadataExtractor.h"

#include "bcinfo/BitcodeWrapper.h"
#include "rsDefines.h"

#define LOG_TAG "bcinfo"
#include <cutils/log.h>
#ifdef HAVE_ANDROID_OS
#include <cutils/properties.h>
#endif

#include "llvm/Bitcode/ReaderWriter.h"
#include "llvm/IR/Constants.h"
#include "llvm/IR/LLVMContext.h"
#include "llvm/IR/Module.h"
#include "llvm/IR/Function.h"
#include "llvm/Support/MemoryBuffer.h"

#include <cstdlib>

namespace bcinfo {

namespace {

llvm::StringRef getStringOperand(const llvm::Metadata *node) {
  if (auto *mds = llvm::dyn_cast_or_null<const llvm::MDString>(node)) {
    return mds->getString();
  }
  return llvm::StringRef();
}

bool extractUIntFromMetadataString(uint32_t *value,
    const llvm::Metadata *m) {
  llvm::StringRef SigString = getStringOperand(m);
  if (SigString != "") {
    if (!SigString.getAsInteger(10, *value)) {
      return true;
    }
  }
  return false;
}

}

// Name of metadata node where pragma info resides (should be synced with
// slang.cpp)
static const llvm::StringRef PragmaMetadataName = "#pragma";

// Name of metadata node where exported variable names reside (should be
// synced with slang_rs_metadata.h)
static const llvm::StringRef ExportVarMetadataName = "#rs_export_var";

// Name of metadata node where exported function names reside (should be
// synced with slang_rs_metadata.h)
static const llvm::StringRef ExportFuncMetadataName = "#rs_export_func";

// Name of metadata node where exported ForEach name information resides
// (should be synced with slang_rs_metadata.h)
static const llvm::StringRef ExportForEachNameMetadataName =
    "#rs_export_foreach_name";

// Name of metadata node where exported ForEach signature information resides
// (should be synced with slang_rs_metadata.h)
static const llvm::StringRef ExportForEachMetadataName = "#rs_export_foreach";

// Name of metadata node where RS object slot info resides (should be
// synced with slang_rs_metadata.h)
static const llvm::StringRef ObjectSlotMetadataName = "#rs_object_slots";

static const llvm::StringRef ThreadableMetadataName = "#rs_is_threadable";

// Name of metadata node where the checksum for this build is stored.  (should
// be synced with libbcc/lib/Core/Source.cpp)
static const llvm::StringRef ChecksumMetadataName = "#rs_build_checksum";

MetadataExtractor::MetadataExtractor(const char *bitcode, size_t bitcodeSize)
    : mModule(nullptr), mBitcode(bitcode), mBitcodeSize(bitcodeSize),
      mExportVarCount(0), mExportFuncCount(0), mExportForEachSignatureCount(0),
      mExportVarNameList(nullptr), mExportFuncNameList(nullptr),
      mExportForEachNameList(nullptr), mExportForEachSignatureList(nullptr),
      mExportForEachInputCountList(nullptr), mPragmaCount(0),
      mPragmaKeyList(nullptr), mPragmaValueList(nullptr), mObjectSlotCount(0),
      mObjectSlotList(nullptr), mRSFloatPrecision(RS_FP_Full),
      mIsThreadable(true), mBuildChecksum(nullptr) {
  BitcodeWrapper wrapper(bitcode, bitcodeSize);
  mTargetAPI = wrapper.getTargetAPI();
  mCompilerVersion = wrapper.getCompilerVersion();
  mOptimizationLevel = wrapper.getOptimizationLevel();
}


MetadataExtractor::MetadataExtractor(const llvm::Module *module)
    : mModule(module), mBitcode(nullptr), mBitcodeSize(0), mExportVarCount(0),
      mExportFuncCount(0), mExportForEachSignatureCount(0),
      mExportVarNameList(nullptr), mExportFuncNameList(nullptr),
      mExportForEachNameList(nullptr), mExportForEachSignatureList(nullptr),
      mExportForEachInputCountList(nullptr), mPragmaCount(0),
      mPragmaKeyList(nullptr), mPragmaValueList(nullptr), mObjectSlotCount(0),
      mObjectSlotList(nullptr), mRSFloatPrecision(RS_FP_Full),
      mIsThreadable(true), mBuildChecksum(nullptr) {
  mCompilerVersion = RS_VERSION;  // Default to the actual current version.
  mOptimizationLevel = 3;
}


MetadataExtractor::~MetadataExtractor() {
  if (mExportVarNameList) {
    for (size_t i = 0; i < mExportVarCount; i++) {
        delete [] mExportVarNameList[i];
        mExportVarNameList[i] = nullptr;
    }
  }
  delete [] mExportVarNameList;
  mExportVarNameList = nullptr;

  if (mExportFuncNameList) {
    for (size_t i = 0; i < mExportFuncCount; i++) {
        delete [] mExportFuncNameList[i];
        mExportFuncNameList[i] = nullptr;
    }
  }
  delete [] mExportFuncNameList;
  mExportFuncNameList = nullptr;

  if (mExportForEachNameList) {
    for (size_t i = 0; i < mExportForEachSignatureCount; i++) {
        delete [] mExportForEachNameList[i];
        mExportForEachNameList[i] = nullptr;
    }
  }
  delete [] mExportForEachNameList;
  mExportForEachNameList = nullptr;

  delete [] mExportForEachSignatureList;
  mExportForEachSignatureList = nullptr;

  for (size_t i = 0; i < mPragmaCount; i++) {
    if (mPragmaKeyList) {
      delete [] mPragmaKeyList[i];
      mPragmaKeyList[i] = nullptr;
    }
    if (mPragmaValueList) {
      delete [] mPragmaValueList[i];
      mPragmaValueList[i] = nullptr;
    }
  }
  delete [] mPragmaKeyList;
  mPragmaKeyList = nullptr;
  delete [] mPragmaValueList;
  mPragmaValueList = nullptr;

  delete [] mObjectSlotList;
  mObjectSlotList = nullptr;

  delete [] mBuildChecksum;

  return;
}


bool MetadataExtractor::populateObjectSlotMetadata(
    const llvm::NamedMDNode *ObjectSlotMetadata) {
  if (!ObjectSlotMetadata) {
    return true;
  }

  mObjectSlotCount = ObjectSlotMetadata->getNumOperands();

  if (!mObjectSlotCount) {
    return true;
  }

  uint32_t *TmpSlotList = new uint32_t[mObjectSlotCount];
  memset(TmpSlotList, 0, mObjectSlotCount * sizeof(*TmpSlotList));

  for (size_t i = 0; i < mObjectSlotCount; i++) {
    llvm::MDNode *ObjectSlot = ObjectSlotMetadata->getOperand(i);
    if (ObjectSlot != nullptr && ObjectSlot->getNumOperands() == 1) {
      if (!extractUIntFromMetadataString(&TmpSlotList[i], ObjectSlot->getOperand(0))) {
        ALOGE("Non-integer object slot value");
        return false;
      }
    } else {
      ALOGE("Corrupt object slot information");
      return false;
    }
  }

  mObjectSlotList = TmpSlotList;

  return true;
}


static const char *createStringFromValue(llvm::Metadata *m) {
  auto ref = getStringOperand(m);
  char *c = new char[ref.size() + 1];
  memcpy(c, ref.data(), ref.size());
  c[ref.size()] = '\0';
  return c;
}


void MetadataExtractor::populatePragmaMetadata(
    const llvm::NamedMDNode *PragmaMetadata) {
  if (!PragmaMetadata) {
    return;
  }

  mPragmaCount = PragmaMetadata->getNumOperands();
  if (!mPragmaCount) {
    return;
  }

  const char **TmpKeyList = new const char*[mPragmaCount];
  const char **TmpValueList = new const char*[mPragmaCount];

  for (size_t i = 0; i < mPragmaCount; i++) {
    llvm::MDNode *Pragma = PragmaMetadata->getOperand(i);
    if (Pragma != nullptr && Pragma->getNumOperands() == 2) {
      llvm::Metadata *PragmaKeyMDS = Pragma->getOperand(0);
      TmpKeyList[i] = createStringFromValue(PragmaKeyMDS);
      llvm::Metadata *PragmaValueMDS = Pragma->getOperand(1);
      TmpValueList[i] = createStringFromValue(PragmaValueMDS);
    }
  }

  mPragmaKeyList = TmpKeyList;
  mPragmaValueList = TmpValueList;

  // Check to see if we have any FP precision-related pragmas.
  std::string Relaxed("rs_fp_relaxed");
  std::string Imprecise("rs_fp_imprecise");
  std::string Full("rs_fp_full");
  bool RelaxedPragmaSeen = false;
  bool FullPragmaSeen = false;
  for (size_t i = 0; i < mPragmaCount; i++) {
    if (!Relaxed.compare(mPragmaKeyList[i])) {
      RelaxedPragmaSeen = true;
    } else if (!Imprecise.compare(mPragmaKeyList[i])) {
      ALOGW("rs_fp_imprecise is deprecated.  Assuming rs_fp_relaxed instead.");
      RelaxedPragmaSeen = true;
    } else if (!Full.compare(mPragmaKeyList[i])) {
      FullPragmaSeen = true;
    }
  }

  if (RelaxedPragmaSeen && FullPragmaSeen) {
    ALOGE("Full and relaxed precision specified at the same time!");
  }
  mRSFloatPrecision = RelaxedPragmaSeen ? RS_FP_Relaxed : RS_FP_Full;

#ifdef HAVE_ANDROID_OS
  // Provide an override for precsiion via adb shell setprop
  // adb shell setprop debug.rs.precision rs_fp_full
  // adb shell setprop debug.rs.precision rs_fp_relaxed
  // adb shell setprop debug.rs.precision rs_fp_imprecise
  char PrecisionPropBuf[PROPERTY_VALUE_MAX];
  const std::string PrecisionPropName("debug.rs.precision");
  property_get("debug.rs.precision", PrecisionPropBuf, "");
  if (PrecisionPropBuf[0]) {
    if (!Relaxed.compare(PrecisionPropBuf)) {
      ALOGI("Switching to RS FP relaxed mode via setprop");
      mRSFloatPrecision = RS_FP_Relaxed;
    } else if (!Imprecise.compare(PrecisionPropBuf)) {
      ALOGW("Switching to RS FP relaxed mode via setprop. rs_fp_imprecise was "
            "specified but is deprecated ");
      mRSFloatPrecision = RS_FP_Relaxed;
    } else if (!Full.compare(PrecisionPropBuf)) {
      ALOGI("Switching to RS FP full mode via setprop");
      mRSFloatPrecision = RS_FP_Full;
    } else {
      ALOGE("Unrecognized debug.rs.precision %s", PrecisionPropBuf);
    }
  }
#endif
}


bool MetadataExtractor::populateVarNameMetadata(
    const llvm::NamedMDNode *VarNameMetadata) {
  if (!VarNameMetadata) {
    return true;
  }

  mExportVarCount = VarNameMetadata->getNumOperands();
  if (!mExportVarCount) {
    return true;
  }

  const char **TmpNameList = new const char *[mExportVarCount];

  for (size_t i = 0; i < mExportVarCount; i++) {
    llvm::MDNode *Name = VarNameMetadata->getOperand(i);
    if (Name != nullptr && Name->getNumOperands() > 1) {
      TmpNameList[i] = createStringFromValue(Name->getOperand(0));
    }
  }

  mExportVarNameList = TmpNameList;

  return true;
}


bool MetadataExtractor::populateFuncNameMetadata(
    const llvm::NamedMDNode *FuncNameMetadata) {
  if (!FuncNameMetadata) {
    return true;
  }

  mExportFuncCount = FuncNameMetadata->getNumOperands();
  if (!mExportFuncCount) {
    return true;
  }

  const char **TmpNameList = new const char*[mExportFuncCount];

  for (size_t i = 0; i < mExportFuncCount; i++) {
    llvm::MDNode *Name = FuncNameMetadata->getOperand(i);
    if (Name != nullptr && Name->getNumOperands() == 1) {
      TmpNameList[i] = createStringFromValue(Name->getOperand(0));
    }
  }

  mExportFuncNameList = TmpNameList;

  return true;
}


uint32_t MetadataExtractor::calculateNumInputs(const llvm::Function *Function,
                                               uint32_t Signature) {

  if (hasForEachSignatureIn(Signature)) {
    uint32_t OtherCount = 0;

    OtherCount += hasForEachSignatureUsrData(Signature);
    OtherCount += hasForEachSignatureX(Signature);
    OtherCount += hasForEachSignatureY(Signature);
    OtherCount += hasForEachSignatureZ(Signature);
    OtherCount += hasForEachSignatureCtxt(Signature);
    OtherCount += hasForEachSignatureOut(Signature) &&
                  Function->getReturnType()->isVoidTy();

    return Function->arg_size() - OtherCount;

  } else {
    return 0;
  }
}


bool MetadataExtractor::populateForEachMetadata(
    const llvm::NamedMDNode *Names,
    const llvm::NamedMDNode *Signatures) {
  if (!Names && !Signatures && mCompilerVersion == 0) {
    // Handle legacy case for pre-ICS bitcode that doesn't contain a metadata
    // section for ForEach. We generate a full signature for a "root" function
    // which means that we need to set the bottom 5 bits in the mask.
    mExportForEachSignatureCount = 1;
    char **TmpNameList = new char*[mExportForEachSignatureCount];
    size_t RootLen = strlen(kRoot) + 1;
    TmpNameList[0] = new char[RootLen];
    strncpy(TmpNameList[0], kRoot, RootLen);

    uint32_t *TmpSigList = new uint32_t[mExportForEachSignatureCount];
    TmpSigList[0] = 0x1f;

    mExportForEachNameList = (const char**)TmpNameList;
    mExportForEachSignatureList = TmpSigList;
    return true;
  }

  if (Signatures) {
    mExportForEachSignatureCount = Signatures->getNumOperands();
    if (!mExportForEachSignatureCount) {
      return true;
    }
  } else {
    mExportForEachSignatureCount = 0;
    mExportForEachSignatureList = nullptr;
    return true;
  }

  uint32_t *TmpSigList = new uint32_t[mExportForEachSignatureCount];
  const char **TmpNameList = new const char*[mExportForEachSignatureCount];
  uint32_t *TmpInputCountList = new uint32_t[mExportForEachSignatureCount];

  for (size_t i = 0; i < mExportForEachSignatureCount; i++) {
    llvm::MDNode *SigNode = Signatures->getOperand(i);
    if (SigNode != nullptr && SigNode->getNumOperands() == 1) {
      if (!extractUIntFromMetadataString(&TmpSigList[i], SigNode->getOperand(0))) {
        ALOGE("Non-integer signature value");
        return false;
      }
    } else {
      ALOGE("Corrupt signature information");
      return false;
    }
  }

  if (Names) {
    for (size_t i = 0; i < mExportForEachSignatureCount; i++) {
      llvm::MDNode *Name = Names->getOperand(i);
      if (Name != nullptr && Name->getNumOperands() == 1) {
        TmpNameList[i] = createStringFromValue(Name->getOperand(0));

        llvm::Function *Func =
            mModule->getFunction(llvm::StringRef(TmpNameList[i]));

        TmpInputCountList[i] = (Func != nullptr) ?
          calculateNumInputs(Func, TmpSigList[i]) : 0;
      }
    }
  } else {
    if (mExportForEachSignatureCount != 1) {
      ALOGE("mExportForEachSignatureCount = %zu, but should be 1",
            mExportForEachSignatureCount);
    }
    char *RootName = new char[5];
    strncpy(RootName, "root", 5);
    TmpNameList[0] = RootName;
  }

  mExportForEachNameList = TmpNameList;
  mExportForEachSignatureList = TmpSigList;
  mExportForEachInputCountList = TmpInputCountList;

  return true;
}


void MetadataExtractor::readThreadableFlag(
    const llvm::NamedMDNode *ThreadableMetadata) {

  // Scripts are threadable by default.  If we read a valid metadata value for
  // 'ThreadableMetadataName' and it is set to 'no', we mark script as non
  // threadable.  All other exception paths retain the default value.

  mIsThreadable = true;
  if (ThreadableMetadata == nullptr)
    return;

  llvm::MDNode *mdNode = ThreadableMetadata->getOperand(0);
  if (mdNode == nullptr)
    return;

  llvm::Metadata *mdValue = mdNode->getOperand(0);
  if (mdValue == nullptr)
    return;

  if (getStringOperand(mdValue) == "no")
    mIsThreadable = false;
}

void MetadataExtractor::readBuildChecksumMetadata(
    const llvm::NamedMDNode *ChecksumMetadata) {

  if (ChecksumMetadata == nullptr)
    return;

  llvm::MDNode *mdNode = ChecksumMetadata->getOperand(0);
  if (mdNode == nullptr)
    return;

  llvm::Metadata *mdValue = mdNode->getOperand(0);
  if (mdValue == nullptr)
    return;

  mBuildChecksum = createStringFromValue(mdValue);
}

bool MetadataExtractor::extract() {
  if (!(mBitcode && mBitcodeSize) && !mModule) {
    ALOGE("Invalid/empty bitcode/module");
    return false;
  }

  std::unique_ptr<llvm::LLVMContext> mContext;

  if (!mModule) {
    mContext.reset(new llvm::LLVMContext());
    std::unique_ptr<llvm::MemoryBuffer> MEM(
      llvm::MemoryBuffer::getMemBuffer(
        llvm::StringRef(mBitcode, mBitcodeSize), "", false));
    std::string error;

    // Module ownership is handled by the context, so we don't need to free it.
    llvm::ErrorOr<llvm::Module* > errval = llvm::parseBitcodeFile(MEM.get()->getMemBufferRef(),
                                                                  *mContext);
    if (std::error_code ec = errval.getError()) {
        ALOGE("Could not parse bitcode file");
        ALOGE("%s", ec.message().c_str());
        return false;
    }
    mModule = errval.get();
  }

  const llvm::NamedMDNode *ExportVarMetadata =
      mModule->getNamedMetadata(ExportVarMetadataName);
  const llvm::NamedMDNode *ExportFuncMetadata =
      mModule->getNamedMetadata(ExportFuncMetadataName);
  const llvm::NamedMDNode *ExportForEachNameMetadata =
      mModule->getNamedMetadata(ExportForEachNameMetadataName);
  const llvm::NamedMDNode *ExportForEachMetadata =
      mModule->getNamedMetadata(ExportForEachMetadataName);
  const llvm::NamedMDNode *PragmaMetadata =
      mModule->getNamedMetadata(PragmaMetadataName);
  const llvm::NamedMDNode *ObjectSlotMetadata =
      mModule->getNamedMetadata(ObjectSlotMetadataName);
  const llvm::NamedMDNode *ThreadableMetadata =
      mModule->getNamedMetadata(ThreadableMetadataName);
  const llvm::NamedMDNode *ChecksumMetadata =
      mModule->getNamedMetadata(ChecksumMetadataName);


  if (!populateVarNameMetadata(ExportVarMetadata)) {
    ALOGE("Could not populate export variable metadata");
    return false;
  }

  if (!populateFuncNameMetadata(ExportFuncMetadata)) {
    ALOGE("Could not populate export function metadata");
    return false;
  }

  if (!populateForEachMetadata(ExportForEachNameMetadata,
                               ExportForEachMetadata)) {
    ALOGE("Could not populate ForEach signature metadata");
    return false;
  }

  populatePragmaMetadata(PragmaMetadata);

  if (!populateObjectSlotMetadata(ObjectSlotMetadata)) {
    ALOGE("Could not populate object slot metadata");
    return false;
  }

  readThreadableFlag(ThreadableMetadata);
  readBuildChecksumMetadata(ChecksumMetadata);

  return true;
}

}  // namespace bcinfo
