
/*
 * Copyright 2010-2014, The Android Open Source Project
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

#include "slang_rs_reflection.h"

#include <sys/stat.h>

#include <cstdarg>
#include <cctype>

#include <algorithm>
#include <sstream>
#include <string>
#include <utility>

#include "llvm/ADT/APFloat.h"
#include "llvm/ADT/StringExtras.h"

#include "os_sep.h"
#include "slang_rs_context.h"
#include "slang_rs_export_var.h"
#include "slang_rs_export_foreach.h"
#include "slang_rs_export_func.h"
#include "slang_rs_reflect_utils.h"
#include "slang_version.h"

#define RS_SCRIPT_CLASS_NAME_PREFIX "ScriptC_"
#define RS_SCRIPT_CLASS_SUPER_CLASS_NAME "ScriptC"

#define RS_TYPE_CLASS_SUPER_CLASS_NAME ".Script.FieldBase"

#define RS_TYPE_ITEM_CLASS_NAME "Item"

#define RS_TYPE_ITEM_SIZEOF_LEGACY "Item.sizeof"
#define RS_TYPE_ITEM_SIZEOF_CURRENT "mElement.getBytesSize()"

#define RS_TYPE_ITEM_BUFFER_NAME "mItemArray"
#define RS_TYPE_ITEM_BUFFER_PACKER_NAME "mIOBuffer"
#define RS_TYPE_ELEMENT_REF_NAME "mElementCache"

#define RS_EXPORT_VAR_INDEX_PREFIX "mExportVarIdx_"
#define RS_EXPORT_VAR_PREFIX "mExportVar_"
#define RS_EXPORT_VAR_ELEM_PREFIX "mExportVarElem_"
#define RS_EXPORT_VAR_DIM_PREFIX "mExportVarDim_"
#define RS_EXPORT_VAR_CONST_PREFIX "const_"

#define RS_ELEM_PREFIX "__"

#define RS_FP_PREFIX "__rs_fp_"

#define RS_RESOURCE_NAME "__rs_resource_name"

#define RS_EXPORT_FUNC_INDEX_PREFIX "mExportFuncIdx_"
#define RS_EXPORT_FOREACH_INDEX_PREFIX "mExportForEachIdx_"

#define RS_EXPORT_VAR_ALLOCATION_PREFIX "mAlloction_"
#define RS_EXPORT_VAR_DATA_STORAGE_PREFIX "mData_"

namespace slang {

class RSReflectionJavaElementBuilder {
public:
  RSReflectionJavaElementBuilder(const char *ElementBuilderName,
                                 const RSExportRecordType *ERT,
                                 const char *RenderScriptVar,
                                 GeneratedFile *Out, const RSContext *RSContext,
                                 RSReflectionJava *Reflection);
  void generate();

private:
  void genAddElement(const RSExportType *ET, const std::string &VarName,
                     unsigned ArraySize);
  void genAddStatementStart();
  void genAddStatementEnd(const std::string &VarName, unsigned ArraySize);
  void genAddPadding(int PaddingSize);
  // TODO Will remove later due to field name information is not necessary for
  // C-reflect-to-Java
  std::string createPaddingField() {
    return mPaddingPrefix + llvm::itostr(mPaddingFieldIndex++);
  }

  const char *mElementBuilderName;
  const RSExportRecordType *mERT;
  const char *mRenderScriptVar;
  GeneratedFile *mOut;
  std::string mPaddingPrefix;
  int mPaddingFieldIndex;
  const RSContext *mRSContext;
  RSReflectionJava *mReflection;
};

static const char *GetMatrixTypeName(const RSExportMatrixType *EMT) {
  static const char *MatrixTypeJavaNameMap[] = {/* 2x2 */ "Matrix2f",
                                                /* 3x3 */ "Matrix3f",
                                                /* 4x4 */ "Matrix4f",
  };
  unsigned Dim = EMT->getDim();

  if ((Dim - 2) < (sizeof(MatrixTypeJavaNameMap) / sizeof(const char *)))
    return MatrixTypeJavaNameMap[EMT->getDim() - 2];

  slangAssert(false && "GetMatrixTypeName : Unsupported matrix dimension");
  return nullptr;
}

static const char *GetVectorAccessor(unsigned Index) {
  static const char *VectorAccessorMap[] = {/* 0 */ "x",
                                            /* 1 */ "y",
                                            /* 2 */ "z",
                                            /* 3 */ "w",
  };

  slangAssert((Index < (sizeof(VectorAccessorMap) / sizeof(const char *))) &&
              "Out-of-bound index to access vector member");

  return VectorAccessorMap[Index];
}

static const char *GetPackerAPIName(const RSExportPrimitiveType *EPT) {
  static const char *PrimitiveTypePackerAPINameMap[] = {
      "addF16",     // DataTypeFloat16
      "addF32",     // DataTypeFloat32
      "addF64",     // DataTypeFloat64
      "addI8",      // DataTypeSigned8
      "addI16",     // DataTypeSigned16
      "addI32",     // DataTypeSigned32
      "addI64",     // DataTypeSigned64
      "addU8",      // DataTypeUnsigned8
      "addU16",     // DataTypeUnsigned16
      "addU32",     // DataTypeUnsigned32
      "addU64",     // DataTypeUnsigned64
      "addBoolean", // DataTypeBoolean
      "addU16",     // DataTypeUnsigned565
      "addU16",     // DataTypeUnsigned5551
      "addU16",     // DataTypeUnsigned4444
      "addMatrix",  // DataTypeRSMatrix2x2
      "addMatrix",  // DataTypeRSMatrix3x3
      "addMatrix",  // DataTypeRSMatrix4x4
      "addObj",     // DataTypeRSElement
      "addObj",     // DataTypeRSType
      "addObj",     // DataTypeRSAllocation
      "addObj",     // DataTypeRSSampler
      "addObj",     // DataTypeRSScript
      "addObj",     // DataTypeRSMesh
      "addObj",     // DataTypeRSPath
      "addObj",     // DataTypeRSProgramFragment
      "addObj",     // DataTypeRSProgramVertex
      "addObj",     // DataTypeRSProgramRaster
      "addObj",     // DataTypeRSProgramStore
      "addObj",     // DataTypeRSFont
  };
  unsigned TypeId = EPT->getType();

  if (TypeId < (sizeof(PrimitiveTypePackerAPINameMap) / sizeof(const char *)))
    return PrimitiveTypePackerAPINameMap[EPT->getType()];

  slangAssert(false && "GetPackerAPIName : Unknown primitive data type");
  return nullptr;
}

static std::string GetTypeName(const RSExportType *ET, bool Brackets = true) {
  switch (ET->getClass()) {
  case RSExportType::ExportClassPrimitive: {
    return RSExportPrimitiveType::getRSReflectionType(
               static_cast<const RSExportPrimitiveType *>(ET))->java_name;
  }
  case RSExportType::ExportClassPointer: {
    const RSExportType *PointeeType =
        static_cast<const RSExportPointerType *>(ET)->getPointeeType();

    if (PointeeType->getClass() != RSExportType::ExportClassRecord)
      return "Allocation";
    else
      return PointeeType->getElementName();
  }
  case RSExportType::ExportClassVector: {
    const RSExportVectorType *EVT = static_cast<const RSExportVectorType *>(ET);
    std::stringstream VecName;
    VecName << EVT->getRSReflectionType(EVT)->rs_java_vector_prefix
            << EVT->getNumElement();
    return VecName.str();
  }
  case RSExportType::ExportClassMatrix: {
    return GetMatrixTypeName(static_cast<const RSExportMatrixType *>(ET));
  }
  case RSExportType::ExportClassConstantArray: {
    const RSExportConstantArrayType *CAT =
        static_cast<const RSExportConstantArrayType *>(ET);
    std::string ElementTypeName = GetTypeName(CAT->getElementType());
    if (Brackets) {
      ElementTypeName.append("[]");
    }
    return ElementTypeName;
  }
  case RSExportType::ExportClassRecord: {
    return ET->getElementName() + "." RS_TYPE_ITEM_CLASS_NAME;
  }
  default: { slangAssert(false && "Unknown class of type"); }
  }

  return "";
}

static const char *GetTypeNullValue(const RSExportType *ET) {
  switch (ET->getClass()) {
  case RSExportType::ExportClassPrimitive: {
    const RSExportPrimitiveType *EPT =
        static_cast<const RSExportPrimitiveType *>(ET);
    if (EPT->isRSObjectType())
      return "null";
    else if (EPT->getType() == DataTypeBoolean)
      return "false";
    else
      return "0";
    break;
  }
  case RSExportType::ExportClassPointer:
  case RSExportType::ExportClassVector:
  case RSExportType::ExportClassMatrix:
  case RSExportType::ExportClassConstantArray:
  case RSExportType::ExportClassRecord: {
    return "null";
    break;
  }
  default: { slangAssert(false && "Unknown class of type"); }
  }
  return "";
}

static std::string GetBuiltinElementConstruct(const RSExportType *ET) {
  if (ET->getClass() == RSExportType::ExportClassPrimitive) {
    return std::string("Element.") + ET->getElementName();
  } else if (ET->getClass() == RSExportType::ExportClassVector) {
    const RSExportVectorType *EVT = static_cast<const RSExportVectorType *>(ET);
    if (EVT->getType() == DataTypeFloat32) {
      if (EVT->getNumElement() == 2) {
        return "Element.F32_2";
      } else if (EVT->getNumElement() == 3) {
        return "Element.F32_3";
      } else if (EVT->getNumElement() == 4) {
        return "Element.F32_4";
      } else {
        slangAssert(false && "Vectors should be size 2, 3, 4");
      }
    } else if (EVT->getType() == DataTypeUnsigned8) {
      if (EVT->getNumElement() == 4)
        return "Element.U8_4";
    }
  } else if (ET->getClass() == RSExportType::ExportClassMatrix) {
    const RSExportMatrixType *EMT = static_cast<const RSExportMatrixType *>(ET);
    switch (EMT->getDim()) {
    case 2:
      return "Element.MATRIX_2X2";
    case 3:
      return "Element.MATRIX_3X3";
    case 4:
      return "Element.MATRIX_4X4";
    default:
      slangAssert(false && "Unsupported dimension of matrix");
    }
  }
  // RSExportType::ExportClassPointer can't be generated in a struct.

  return "";
}

/********************** Methods to generate script class **********************/
RSReflectionJava::RSReflectionJava(const RSContext *Context,
                                   std::vector<std::string> *GeneratedFileNames,
                                   const std::string &OutputBaseDirectory,
                                   const std::string &RSSourceFileName,
                                   const std::string &BitCodeFileName,
                                   bool EmbedBitcodeInJava)
    : mRSContext(Context), mPackageName(Context->getReflectJavaPackageName()),
      mRSPackageName(Context->getRSPackageName()),
      mOutputBaseDirectory(OutputBaseDirectory),
      mRSSourceFileName(RSSourceFileName), mBitCodeFileName(BitCodeFileName),
      mResourceId(RSSlangReflectUtils::JavaClassNameFromRSFileName(
          mBitCodeFileName.c_str())),
      mScriptClassName(RS_SCRIPT_CLASS_NAME_PREFIX +
                       RSSlangReflectUtils::JavaClassNameFromRSFileName(
                           mRSSourceFileName.c_str())),
      mEmbedBitcodeInJava(EmbedBitcodeInJava), mNextExportVarSlot(0),
      mNextExportFuncSlot(0), mNextExportForEachSlot(0), mLastError(""),
      mGeneratedFileNames(GeneratedFileNames), mFieldIndex(0) {
  slangAssert(mGeneratedFileNames && "Must supply GeneratedFileNames");
  slangAssert(!mPackageName.empty() && mPackageName != "-");

  mOutputDirectory = RSSlangReflectUtils::ComputePackagedPath(
                         OutputBaseDirectory.c_str(), mPackageName.c_str()) +
                     OS_PATH_SEPARATOR_STR;

  // mElement.getBytesSize only exists on JB+
  if (mRSContext->getTargetAPI() >= SLANG_JB_TARGET_API) {
      mItemSizeof = RS_TYPE_ITEM_SIZEOF_CURRENT;
  } else {
      mItemSizeof = RS_TYPE_ITEM_SIZEOF_LEGACY;
  }
}

bool RSReflectionJava::genScriptClass(const std::string &ClassName,
                                      std::string &ErrorMsg) {
  if (!startClass(AM_Public, false, ClassName, RS_SCRIPT_CLASS_SUPER_CLASS_NAME,
                  ErrorMsg))
    return false;

  genScriptClassConstructor();

  // Reflect export variable
  for (RSContext::const_export_var_iterator I = mRSContext->export_vars_begin(),
                                            E = mRSContext->export_vars_end();
       I != E; I++)
    genExportVariable(*I);

  // Reflect export for each functions (only available on ICS+)
  if (mRSContext->getTargetAPI() >= SLANG_ICS_TARGET_API) {
    for (RSContext::const_export_foreach_iterator
             I = mRSContext->export_foreach_begin(),
             E = mRSContext->export_foreach_end();
         I != E; I++)
      genExportForEach(*I);
  }

  // Reflect export function
  for (RSContext::const_export_func_iterator
           I = mRSContext->export_funcs_begin(),
           E = mRSContext->export_funcs_end();
       I != E; I++)
    genExportFunction(*I);

  endClass();

  return true;
}

void RSReflectionJava::genScriptClassConstructor() {
  std::string className(RSSlangReflectUtils::JavaBitcodeClassNameFromRSFileName(
      mRSSourceFileName.c_str()));
  // Provide a simple way to reference this object.
  mOut.indent() << "private static final String " RS_RESOURCE_NAME " = \""
                << getResourceId() << "\";\n";

  // Generate a simple constructor with only a single parameter (the rest
  // can be inferred from information we already have).
  mOut.indent() << "// Constructor\n";
  startFunction(AM_Public, false, nullptr, getClassName(), 1, "RenderScript",
                "rs");

  if (getEmbedBitcodeInJava()) {
    // Call new single argument Java-only constructor
    mOut.indent() << "super(rs,\n";
    mOut.indent() << "      " << RS_RESOURCE_NAME ",\n";
    mOut.indent() << "      " << className << ".getBitCode32(),\n";
    mOut.indent() << "      " << className << ".getBitCode64());\n";
  } else {
    // Call alternate constructor with required parameters.
    // Look up the proper raw bitcode resource id via the context.
    mOut.indent() << "this(rs,\n";
    mOut.indent() << "     rs.getApplicationContext().getResources(),\n";
    mOut.indent() << "     rs.getApplicationContext().getResources()."
                     "getIdentifier(\n";
    mOut.indent() << "         " RS_RESOURCE_NAME ", \"raw\",\n";
    mOut.indent()
        << "         rs.getApplicationContext().getPackageName()));\n";
    endFunction();

    // Alternate constructor (legacy) with 3 original parameters.
    startFunction(AM_Public, false, nullptr, getClassName(), 3, "RenderScript",
                  "rs", "Resources", "resources", "int", "id");
    // Call constructor of super class
    mOut.indent() << "super(rs, resources, id);\n";
  }

  // If an exported variable has initial value, reflect it

  for (RSContext::const_export_var_iterator I = mRSContext->export_vars_begin(),
                                            E = mRSContext->export_vars_end();
       I != E; I++) {
    const RSExportVar *EV = *I;
    if (!EV->getInit().isUninit()) {
      genInitExportVariable(EV->getType(), EV->getName(), EV->getInit());
    } else if (EV->getArraySize()) {
      // Always create an initial zero-init array object.
      mOut.indent() << RS_EXPORT_VAR_PREFIX << EV->getName() << " = new "
                    << GetTypeName(EV->getType(), false) << "["
                    << EV->getArraySize() << "];\n";
      size_t NumInits = EV->getNumInits();
      const RSExportConstantArrayType *ECAT =
          static_cast<const RSExportConstantArrayType *>(EV->getType());
      const RSExportType *ET = ECAT->getElementType();
      for (size_t i = 0; i < NumInits; i++) {
        std::stringstream Name;
        Name << EV->getName() << "[" << i << "]";
        genInitExportVariable(ET, Name.str(), EV->getInitArray(i));
      }
    }
    if (mRSContext->getTargetAPI() >= SLANG_JB_TARGET_API) {
      genTypeInstance(EV->getType());
    }
    genFieldPackerInstance(EV->getType());
  }

  for (RSContext::const_export_foreach_iterator
           I = mRSContext->export_foreach_begin(),
           E = mRSContext->export_foreach_end();
       I != E; I++) {
    const RSExportForEach *EF = *I;

    const RSExportForEach::InTypeVec &InTypes = EF->getInTypes();
    for (RSExportForEach::InTypeIter BI = InTypes.begin(), EI = InTypes.end();
         BI != EI; BI++) {

      if (*BI != nullptr) {
        genTypeInstanceFromPointer(*BI);
      }
    }

    const RSExportType *OET = EF->getOutType();
    if (OET) {
      genTypeInstanceFromPointer(OET);
    }
  }

  endFunction();

  for (std::set<std::string>::iterator I = mTypesToCheck.begin(),
                                       E = mTypesToCheck.end();
       I != E; I++) {
    mOut.indent() << "private Element " RS_ELEM_PREFIX << *I << ";\n";
  }

  for (std::set<std::string>::iterator I = mFieldPackerTypes.begin(),
                                       E = mFieldPackerTypes.end();
       I != E; I++) {
    mOut.indent() << "private FieldPacker " RS_FP_PREFIX << *I << ";\n";
  }
}

void RSReflectionJava::genInitBoolExportVariable(const std::string &VarName,
                                                 const clang::APValue &Val) {
  slangAssert(!Val.isUninit() && "Not a valid initializer");
  slangAssert((Val.getKind() == clang::APValue::Int) &&
              "Bool type has wrong initial APValue");

  mOut.indent() << RS_EXPORT_VAR_PREFIX << VarName << " = ";

  mOut << ((Val.getInt().getSExtValue() == 0) ? "false" : "true") << ";\n";
}

void
RSReflectionJava::genInitPrimitiveExportVariable(const std::string &VarName,
                                                 const clang::APValue &Val) {
  slangAssert(!Val.isUninit() && "Not a valid initializer");

  mOut.indent() << RS_EXPORT_VAR_PREFIX << VarName << " = ";
  genInitValue(Val, false);
  mOut << ";\n";
}

void RSReflectionJava::genInitExportVariable(const RSExportType *ET,
                                             const std::string &VarName,
                                             const clang::APValue &Val) {
  slangAssert(!Val.isUninit() && "Not a valid initializer");

  switch (ET->getClass()) {
  case RSExportType::ExportClassPrimitive: {
    const RSExportPrimitiveType *EPT =
        static_cast<const RSExportPrimitiveType *>(ET);
    if (EPT->getType() == DataTypeBoolean) {
      genInitBoolExportVariable(VarName, Val);
    } else {
      genInitPrimitiveExportVariable(VarName, Val);
    }
    break;
  }
  case RSExportType::ExportClassPointer: {
    if (!Val.isInt() || Val.getInt().getSExtValue() != 0)
      std::cout << "Initializer which is non-NULL to pointer type variable "
                   "will be ignored\n";
    break;
  }
  case RSExportType::ExportClassVector: {
    const RSExportVectorType *EVT = static_cast<const RSExportVectorType *>(ET);
    switch (Val.getKind()) {
    case clang::APValue::Int:
    case clang::APValue::Float: {
      for (unsigned i = 0; i < EVT->getNumElement(); i++) {
        std::string Name = VarName + "." + GetVectorAccessor(i);
        genInitPrimitiveExportVariable(Name, Val);
      }
      break;
    }
    case clang::APValue::Vector: {
      std::stringstream VecName;
      VecName << EVT->getRSReflectionType(EVT)->rs_java_vector_prefix
              << EVT->getNumElement();
      mOut.indent() << RS_EXPORT_VAR_PREFIX << VarName << " = new "
                    << VecName.str() << "();\n";

      unsigned NumElements = std::min(
          static_cast<unsigned>(EVT->getNumElement()), Val.getVectorLength());
      for (unsigned i = 0; i < NumElements; i++) {
        const clang::APValue &ElementVal = Val.getVectorElt(i);
        std::string Name = VarName + "." + GetVectorAccessor(i);
        genInitPrimitiveExportVariable(Name, ElementVal);
      }
      break;
    }
    case clang::APValue::MemberPointer:
    case clang::APValue::Uninitialized:
    case clang::APValue::ComplexInt:
    case clang::APValue::ComplexFloat:
    case clang::APValue::LValue:
    case clang::APValue::Array:
    case clang::APValue::Struct:
    case clang::APValue::Union:
    case clang::APValue::AddrLabelDiff: {
      slangAssert(false && "Unexpected type of value of initializer.");
    }
    }
    break;
  }
  // TODO(zonr): Resolving initializer of a record (and matrix) type variable
  // is complex. It cannot obtain by just simply evaluating the initializer
  // expression.
  case RSExportType::ExportClassMatrix:
  case RSExportType::ExportClassConstantArray:
  case RSExportType::ExportClassRecord: {
#if 0
      unsigned InitIndex = 0;
      const RSExportRecordType *ERT =
          static_cast<const RSExportRecordType*>(ET);

      slangAssert((Val.getKind() == clang::APValue::Vector) &&
          "Unexpected type of initializer for record type variable");

      mOut.indent() << RS_EXPORT_VAR_PREFIX << VarName
                 << " = new " << ERT->getElementName()
                 <<  "." RS_TYPE_ITEM_CLASS_NAME"();\n";

      for (RSExportRecordType::const_field_iterator I = ERT->fields_begin(),
               E = ERT->fields_end();
           I != E;
           I++) {
        const RSExportRecordType::Field *F = *I;
        std::string FieldName = VarName + "." + F->getName();

        if (InitIndex > Val.getVectorLength())
          break;

        genInitPrimitiveExportVariable(FieldName,
                                       Val.getVectorElt(InitIndex++));
      }
#endif
    slangAssert(false && "Unsupported initializer for record/matrix/constant "
                         "array type variable currently");
    break;
  }
  default: { slangAssert(false && "Unknown class of type"); }
  }
}

void RSReflectionJava::genExportVariable(const RSExportVar *EV) {
  const RSExportType *ET = EV->getType();

  mOut.indent() << "private final static int " << RS_EXPORT_VAR_INDEX_PREFIX
                << EV->getName() << " = " << getNextExportVarSlot() << ";\n";

  switch (ET->getClass()) {
  case RSExportType::ExportClassPrimitive: {
    genPrimitiveTypeExportVariable(EV);
    break;
  }
  case RSExportType::ExportClassPointer: {
    genPointerTypeExportVariable(EV);
    break;
  }
  case RSExportType::ExportClassVector: {
    genVectorTypeExportVariable(EV);
    break;
  }
  case RSExportType::ExportClassMatrix: {
    genMatrixTypeExportVariable(EV);
    break;
  }
  case RSExportType::ExportClassConstantArray: {
    genConstantArrayTypeExportVariable(EV);
    break;
  }
  case RSExportType::ExportClassRecord: {
    genRecordTypeExportVariable(EV);
    break;
  }
  default: { slangAssert(false && "Unknown class of type"); }
  }
}

void RSReflectionJava::genExportFunction(const RSExportFunc *EF) {
  mOut.indent() << "private final static int " << RS_EXPORT_FUNC_INDEX_PREFIX
                << EF->getName() << " = " << getNextExportFuncSlot() << ";\n";

  // invoke_*()
  ArgTy Args;

  if (EF->hasParam()) {
    for (RSExportFunc::const_param_iterator I = EF->params_begin(),
                                            E = EF->params_end();
         I != E; I++) {
      Args.push_back(
          std::make_pair(GetTypeName((*I)->getType()), (*I)->getName()));
    }
  }

  if (mRSContext->getTargetAPI() >= SLANG_M_TARGET_API) {
    startFunction(AM_Public, false, "Script.InvokeID",
                  "getInvokeID_" + EF->getName(), 0);

    mOut.indent() << "return createInvokeID(" << RS_EXPORT_FUNC_INDEX_PREFIX
                  << EF->getName() << ");\n";

    endFunction();
  }

  startFunction(AM_Public, false, "void",
                "invoke_" + EF->getName(/*Mangle=*/false),
                // We are using un-mangled name since Java
                // supports method overloading.
                Args);

  if (!EF->hasParam()) {
    mOut.indent() << "invoke(" << RS_EXPORT_FUNC_INDEX_PREFIX << EF->getName()
                  << ");\n";
  } else {
    const RSExportRecordType *ERT = EF->getParamPacketType();
    std::string FieldPackerName = EF->getName() + "_fp";

    if (genCreateFieldPacker(ERT, FieldPackerName.c_str()))
      genPackVarOfType(ERT, nullptr, FieldPackerName.c_str());

    mOut.indent() << "invoke(" << RS_EXPORT_FUNC_INDEX_PREFIX << EF->getName()
                  << ", " << FieldPackerName << ");\n";
  }

  endFunction();
}

void RSReflectionJava::genPairwiseDimCheck(std::string name0,
                                           std::string name1) {

  mOut.indent() << "// Verify dimensions\n";
  mOut.indent() << "t0 = " << name0 << ".getType();\n";
  mOut.indent() << "t1 = " << name1 << ".getType();\n";
  mOut.indent() << "if ((t0.getCount() != t1.getCount()) ||\n";
  mOut.indent() << "    (t0.getX() != t1.getX()) ||\n";
  mOut.indent() << "    (t0.getY() != t1.getY()) ||\n";
  mOut.indent() << "    (t0.getZ() != t1.getZ()) ||\n";
  mOut.indent() << "    (t0.hasFaces()   != t1.hasFaces()) ||\n";
  mOut.indent() << "    (t0.hasMipmaps() != t1.hasMipmaps())) {\n";
  mOut.indent() << "    throw new RSRuntimeException(\"Dimension mismatch "
                << "between parameters " << name0 << " and " << name1
                << "!\");\n";
  mOut.indent() << "}\n\n";
}

void RSReflectionJava::genExportForEach(const RSExportForEach *EF) {
  if (EF->isDummyRoot()) {
    // Skip reflection for dummy root() kernels. Note that we have to
    // advance the next slot number for ForEach, however.
    mOut.indent() << "//private final static int "
                  << RS_EXPORT_FOREACH_INDEX_PREFIX << EF->getName() << " = "
                  << getNextExportForEachSlot() << ";\n";
    return;
  }

  mOut.indent() << "private final static int " << RS_EXPORT_FOREACH_INDEX_PREFIX
                << EF->getName() << " = " << getNextExportForEachSlot()
                << ";\n";

  // forEach_*()
  ArgTy Args;
  bool HasAllocation = false; // at least one in/out allocation?

  const RSExportForEach::InVec     &Ins     = EF->getIns();
  const RSExportForEach::InTypeVec &InTypes = EF->getInTypes();
  const RSExportType               *OET     = EF->getOutType();

  if (Ins.size() == 1) {
    HasAllocation = true;
    Args.push_back(std::make_pair("Allocation", "ain"));

  } else if (Ins.size() > 1) {
    HasAllocation = true;
    for (RSExportForEach::InIter BI = Ins.begin(), EI = Ins.end(); BI != EI;
         BI++) {

      Args.push_back(std::make_pair("Allocation",
                                    "ain_" + (*BI)->getName().str()));
    }
  }

  if (EF->hasOut() || EF->hasReturn()) {
    HasAllocation = true;
    Args.push_back(std::make_pair("Allocation", "aout"));
  }

  const RSExportRecordType *ERT = EF->getParamPacketType();
  if (ERT) {
    for (RSExportForEach::const_param_iterator I = EF->params_begin(),
                                               E = EF->params_end();
         I != E; I++) {
      Args.push_back(
          std::make_pair(GetTypeName((*I)->getType()), (*I)->getName()));
    }
  }

  if (mRSContext->getTargetAPI() >= SLANG_JB_MR1_TARGET_API) {
    startFunction(AM_Public, false, "Script.KernelID",
                  "getKernelID_" + EF->getName(), 0);

    // TODO: add element checking
    mOut.indent() << "return createKernelID(" << RS_EXPORT_FOREACH_INDEX_PREFIX
                  << EF->getName() << ", " << EF->getSignatureMetadata()
                  << ", null, null);\n";

    endFunction();
  }

  if (mRSContext->getTargetAPI() >= SLANG_JB_MR2_TARGET_API) {
    if (HasAllocation) {
      startFunction(AM_Public, false, "void", "forEach_" + EF->getName(), Args);

      mOut.indent() << "forEach_" << EF->getName();
      mOut << "(";

      if (Ins.size() == 1) {
        mOut << "ain, ";

      } else if (Ins.size() > 1) {
        for (RSExportForEach::InIter BI = Ins.begin(), EI = Ins.end(); BI != EI;
             BI++) {

          mOut << "ain_" << (*BI)->getName().str() << ", ";
        }
      }

      if (EF->hasOut() || EF->hasReturn()) {
        mOut << "aout, ";
      }

      if (EF->hasUsrData()) {
        mOut << Args.back().second << ", ";
      }

      // No clipped bounds to pass in.
      mOut << "null);\n";

      endFunction();
    }

    // Add the clipped kernel parameters to the Args list.
    Args.push_back(std::make_pair("Script.LaunchOptions", "sc"));
  }

  startFunction(AM_Public, false, "void", "forEach_" + EF->getName(), Args);

  if (InTypes.size() == 1) {
    if (InTypes.front() != nullptr) {
      genTypeCheck(InTypes.front(), "ain");
    }

  } else if (InTypes.size() > 1) {
    size_t Index = 0;
    for (RSExportForEach::InTypeIter BI = InTypes.begin(), EI = InTypes.end();
         BI != EI; BI++, ++Index) {

      if (*BI != nullptr) {
        genTypeCheck(*BI, ("ain_" + Ins[Index]->getName()).str().c_str());
      }
    }
  }

  if (OET) {
    genTypeCheck(OET, "aout");
  }

  if (Ins.size() == 1 && (EF->hasOut() || EF->hasReturn())) {
    mOut.indent() << "Type t0, t1;";
    genPairwiseDimCheck("ain", "aout");

  } else if (Ins.size() > 1) {
    mOut.indent() << "Type t0, t1;";

    std::string In0Name = "ain_" + Ins[0]->getName().str();

    for (size_t index = 1; index < Ins.size(); ++index) {
      genPairwiseDimCheck(In0Name, "ain_" + Ins[index]->getName().str());
    }

    if (EF->hasOut() || EF->hasReturn()) {
      genPairwiseDimCheck(In0Name, "aout");
    }
  }

  std::string FieldPackerName = EF->getName() + "_fp";
  if (ERT) {
    if (genCreateFieldPacker(ERT, FieldPackerName.c_str())) {
      genPackVarOfType(ERT, nullptr, FieldPackerName.c_str());
    }
  }
  mOut.indent() << "forEach(" << RS_EXPORT_FOREACH_INDEX_PREFIX
                << EF->getName();

  if (Ins.size() == 1) {
    mOut << ", ain";
  } else if (Ins.size() > 1) {
    mOut << ", new Allocation[]{ain_" << Ins[0]->getName().str();

    for (size_t index = 1; index < Ins.size(); ++index) {
      mOut << ", ain_" << Ins[index]->getName().str();
    }

    mOut << "}";

  } else {
    mOut << ", (Allocation) null";
  }

  if (EF->hasOut() || EF->hasReturn())
    mOut << ", aout";
  else
    mOut << ", null";

  if (EF->hasUsrData())
    mOut << ", " << FieldPackerName;
  else
    mOut << ", null";

  if (mRSContext->getTargetAPI() >= SLANG_JB_MR2_TARGET_API) {
    mOut << ", sc);\n";
  } else {
    mOut << ");\n";
  }

  endFunction();
}

void RSReflectionJava::genTypeInstanceFromPointer(const RSExportType *ET) {
  if (ET->getClass() == RSExportType::ExportClassPointer) {
    // For pointer parameters to original forEach kernels.
    const RSExportPointerType *EPT =
        static_cast<const RSExportPointerType *>(ET);
    genTypeInstance(EPT->getPointeeType());
  } else {
    // For handling pass-by-value kernel parameters.
    genTypeInstance(ET);
  }
}

void RSReflectionJava::genTypeInstance(const RSExportType *ET) {
  switch (ET->getClass()) {
  case RSExportType::ExportClassPrimitive:
  case RSExportType::ExportClassVector:
  case RSExportType::ExportClassConstantArray: {
    std::string TypeName = ET->getElementName();
    if (addTypeNameForElement(TypeName)) {
      mOut.indent() << RS_ELEM_PREFIX << TypeName << " = Element." << TypeName
                    << "(rs);\n";
    }
    break;
  }

  case RSExportType::ExportClassRecord: {
    std::string ClassName = ET->getElementName();
    if (addTypeNameForElement(ClassName)) {
      mOut.indent() << RS_ELEM_PREFIX << ClassName << " = " << ClassName
                    << ".createElement(rs);\n";
    }
    break;
  }

  default:
    break;
  }
}

void RSReflectionJava::genFieldPackerInstance(const RSExportType *ET) {
  switch (ET->getClass()) {
  case RSExportType::ExportClassPrimitive:
  case RSExportType::ExportClassVector:
  case RSExportType::ExportClassConstantArray:
  case RSExportType::ExportClassRecord: {
    std::string TypeName = ET->getElementName();
    addTypeNameForFieldPacker(TypeName);
    break;
  }

  default:
    break;
  }
}

void RSReflectionJava::genTypeCheck(const RSExportType *ET,
                                    const char *VarName) {
  mOut.indent() << "// check " << VarName << "\n";

  if (ET->getClass() == RSExportType::ExportClassPointer) {
    const RSExportPointerType *EPT =
        static_cast<const RSExportPointerType *>(ET);
    ET = EPT->getPointeeType();
  }

  std::string TypeName;

  switch (ET->getClass()) {
  case RSExportType::ExportClassPrimitive:
  case RSExportType::ExportClassVector:
  case RSExportType::ExportClassRecord: {
    TypeName = ET->getElementName();
    break;
  }

  default:
    break;
  }

  if (!TypeName.empty()) {
    mOut.indent() << "if (!" << VarName
                  << ".getType().getElement().isCompatible(" RS_ELEM_PREFIX
                  << TypeName << ")) {\n";
    mOut.indent() << "    throw new RSRuntimeException(\"Type mismatch with "
                  << TypeName << "!\");\n";
    mOut.indent() << "}\n";
  }
}

void RSReflectionJava::genPrimitiveTypeExportVariable(const RSExportVar *EV) {
  slangAssert(
      (EV->getType()->getClass() == RSExportType::ExportClassPrimitive) &&
      "Variable should be type of primitive here");

  const RSExportPrimitiveType *EPT =
      static_cast<const RSExportPrimitiveType *>(EV->getType());
  std::string TypeName = GetTypeName(EPT);
  std::string VarName = EV->getName();

  genPrivateExportVariable(TypeName, EV->getName());

  if (EV->isConst()) {
    mOut.indent() << "public final static " << TypeName
                  << " " RS_EXPORT_VAR_CONST_PREFIX << VarName << " = ";
    const clang::APValue &Val = EV->getInit();
    genInitValue(Val, EPT->getType() == DataTypeBoolean);
    mOut << ";\n";
  } else {
    // set_*()
    // This must remain synchronized, since multiple Dalvik threads may
    // be calling setters.
    startFunction(AM_PublicSynchronized, false, "void", "set_" + VarName, 1,
                  TypeName.c_str(), "v");
    if ((EPT->getSize() < 4) || EV->isUnsigned()) {
      // We create/cache a per-type FieldPacker. This allows us to reuse the
      // validation logic (for catching negative inputs from Dalvik, as well
      // as inputs that are too large to be represented in the unsigned type).
      // Sub-integer types are also handled specially here, so that we don't
      // overwrite bytes accidentally.
      std::string ElemName = EPT->getElementName();
      std::string FPName;
      FPName = RS_FP_PREFIX + ElemName;
      mOut.indent() << "if (" << FPName << "!= null) {\n";
      mOut.increaseIndent();
      mOut.indent() << FPName << ".reset();\n";
      mOut.decreaseIndent();
      mOut.indent() << "} else {\n";
      mOut.increaseIndent();
      mOut.indent() << FPName << " = new FieldPacker(" << EPT->getSize()
                    << ");\n";
      mOut.decreaseIndent();
      mOut.indent() << "}\n";

      genPackVarOfType(EPT, "v", FPName.c_str());
      mOut.indent() << "setVar(" << RS_EXPORT_VAR_INDEX_PREFIX << VarName
                    << ", " << FPName << ");\n";
    } else {
      mOut.indent() << "setVar(" << RS_EXPORT_VAR_INDEX_PREFIX << VarName
                    << ", v);\n";
    }

    // Dalvik update comes last, since the input may be invalid (and hence
    // throw an exception).
    mOut.indent() << RS_EXPORT_VAR_PREFIX << VarName << " = v;\n";

    endFunction();
  }

  genGetExportVariable(TypeName, VarName);
  genGetFieldID(VarName);
}

void RSReflectionJava::genInitValue(const clang::APValue &Val, bool asBool) {
  switch (Val.getKind()) {
  case clang::APValue::Int: {
    llvm::APInt api = Val.getInt();
    if (asBool) {
      mOut << ((api.getSExtValue() == 0) ? "false" : "true");
    } else {
      // TODO: Handle unsigned correctly
      mOut << api.getSExtValue();
      if (api.getBitWidth() > 32) {
        mOut << "L";
      }
    }
    break;
  }

  case clang::APValue::Float: {
    llvm::APFloat apf = Val.getFloat();
    llvm::SmallString<30> s;
    apf.toString(s);
    mOut << s.c_str();
    if (&apf.getSemantics() == &llvm::APFloat::IEEEsingle) {
      if (s.count('.') == 0) {
        mOut << ".f";
      } else {
        mOut << "f";
      }
    }
    break;
  }

  case clang::APValue::ComplexInt:
  case clang::APValue::ComplexFloat:
  case clang::APValue::LValue:
  case clang::APValue::Vector: {
    slangAssert(false && "Primitive type cannot have such kind of initializer");
    break;
  }

  default: { slangAssert(false && "Unknown kind of initializer"); }
  }
}

void RSReflectionJava::genPointerTypeExportVariable(const RSExportVar *EV) {
  const RSExportType *ET = EV->getType();
  const RSExportType *PointeeType;

  slangAssert((ET->getClass() == RSExportType::ExportClassPointer) &&
              "Variable should be type of pointer here");

  PointeeType = static_cast<const RSExportPointerType *>(ET)->getPointeeType();
  std::string TypeName = GetTypeName(ET);
  std::string VarName = EV->getName();

  genPrivateExportVariable(TypeName, VarName);

  // bind_*()
  startFunction(AM_Public, false, "void", "bind_" + VarName, 1,
                TypeName.c_str(), "v");

  mOut.indent() << RS_EXPORT_VAR_PREFIX << VarName << " = v;\n";
  mOut.indent() << "if (v == null) bindAllocation(null, "
                << RS_EXPORT_VAR_INDEX_PREFIX << VarName << ");\n";

  if (PointeeType->getClass() == RSExportType::ExportClassRecord) {
    mOut.indent() << "else bindAllocation(v.getAllocation(), "
                  << RS_EXPORT_VAR_INDEX_PREFIX << VarName << ");\n";
  } else {
    mOut.indent() << "else bindAllocation(v, " << RS_EXPORT_VAR_INDEX_PREFIX
                  << VarName << ");\n";
  }

  endFunction();

  genGetExportVariable(TypeName, VarName);
}

void RSReflectionJava::genVectorTypeExportVariable(const RSExportVar *EV) {
  slangAssert((EV->getType()->getClass() == RSExportType::ExportClassVector) &&
              "Variable should be type of vector here");

  std::string TypeName = GetTypeName(EV->getType());
  std::string VarName = EV->getName();

  genPrivateExportVariable(TypeName, VarName);
  genSetExportVariable(TypeName, EV);
  genGetExportVariable(TypeName, VarName);
  genGetFieldID(VarName);
}

void RSReflectionJava::genMatrixTypeExportVariable(const RSExportVar *EV) {
  slangAssert((EV->getType()->getClass() == RSExportType::ExportClassMatrix) &&
              "Variable should be type of matrix here");

  const RSExportType *ET = EV->getType();
  std::string TypeName = GetTypeName(ET);
  std::string VarName = EV->getName();

  genPrivateExportVariable(TypeName, VarName);

  // set_*()
  if (!EV->isConst()) {
    const char *FieldPackerName = "fp";
    startFunction(AM_PublicSynchronized, false, "void", "set_" + VarName, 1,
                  TypeName.c_str(), "v");
    mOut.indent() << RS_EXPORT_VAR_PREFIX << VarName << " = v;\n";

    if (genCreateFieldPacker(ET, FieldPackerName))
      genPackVarOfType(ET, "v", FieldPackerName);
    mOut.indent() << "setVar(" RS_EXPORT_VAR_INDEX_PREFIX << VarName << ", "
                  << FieldPackerName << ");\n";

    endFunction();
  }

  genGetExportVariable(TypeName, VarName);
  genGetFieldID(VarName);
}

void
RSReflectionJava::genConstantArrayTypeExportVariable(const RSExportVar *EV) {
  slangAssert(
      (EV->getType()->getClass() == RSExportType::ExportClassConstantArray) &&
      "Variable should be type of constant array here");

  std::string TypeName = GetTypeName(EV->getType());
  std::string VarName = EV->getName();

  genPrivateExportVariable(TypeName, VarName);
  genSetExportVariable(TypeName, EV);
  genGetExportVariable(TypeName, VarName);
  genGetFieldID(VarName);
}

void RSReflectionJava::genRecordTypeExportVariable(const RSExportVar *EV) {
  slangAssert((EV->getType()->getClass() == RSExportType::ExportClassRecord) &&
              "Variable should be type of struct here");

  std::string TypeName = GetTypeName(EV->getType());
  std::string VarName = EV->getName();

  genPrivateExportVariable(TypeName, VarName);
  genSetExportVariable(TypeName, EV);
  genGetExportVariable(TypeName, VarName);
  genGetFieldID(VarName);
}

void RSReflectionJava::genPrivateExportVariable(const std::string &TypeName,
                                                const std::string &VarName) {
  mOut.indent() << "private " << TypeName << " " << RS_EXPORT_VAR_PREFIX
                << VarName << ";\n";
}

void RSReflectionJava::genSetExportVariable(const std::string &TypeName,
                                            const RSExportVar *EV) {
  if (!EV->isConst()) {
    const char *FieldPackerName = "fp";
    std::string VarName = EV->getName();
    const RSExportType *ET = EV->getType();
    startFunction(AM_PublicSynchronized, false, "void", "set_" + VarName, 1,
                  TypeName.c_str(), "v");
    mOut.indent() << RS_EXPORT_VAR_PREFIX << VarName << " = v;\n";

    if (genCreateFieldPacker(ET, FieldPackerName))
      genPackVarOfType(ET, "v", FieldPackerName);

    if (mRSContext->getTargetAPI() < SLANG_JB_TARGET_API) {
      // Legacy apps must use the old setVar() without Element/dim components.
      mOut.indent() << "setVar(" << RS_EXPORT_VAR_INDEX_PREFIX << VarName
                    << ", " << FieldPackerName << ");\n";
    } else {
      // We only have support for one-dimensional array reflection today,
      // but the entry point (i.e. setVar()) takes an array of dimensions.
      mOut.indent() << "int []__dimArr = new int[1];\n";
      mOut.indent() << "__dimArr[0] = " << ET->getSize() << ";\n";
      mOut.indent() << "setVar(" << RS_EXPORT_VAR_INDEX_PREFIX << VarName
                    << ", " << FieldPackerName << ", " << RS_ELEM_PREFIX
                    << ET->getElementName() << ", __dimArr);\n";
    }

    endFunction();
  }
}

void RSReflectionJava::genGetExportVariable(const std::string &TypeName,
                                            const std::string &VarName) {
  startFunction(AM_Public, false, TypeName.c_str(), "get_" + VarName, 0);

  mOut.indent() << "return " << RS_EXPORT_VAR_PREFIX << VarName << ";\n";

  endFunction();
}

void RSReflectionJava::genGetFieldID(const std::string &VarName) {
  // We only generate getFieldID_*() for non-Pointer (bind) types.
  if (mRSContext->getTargetAPI() >= SLANG_JB_MR1_TARGET_API) {
    startFunction(AM_Public, false, "Script.FieldID", "getFieldID_" + VarName,
                  0);

    mOut.indent() << "return createFieldID(" << RS_EXPORT_VAR_INDEX_PREFIX
                  << VarName << ", null);\n";

    endFunction();
  }
}

/******************* Methods to generate script class /end *******************/

bool RSReflectionJava::genCreateFieldPacker(const RSExportType *ET,
                                            const char *FieldPackerName) {
  size_t AllocSize = ET->getAllocSize();
  if (AllocSize > 0)
    mOut.indent() << "FieldPacker " << FieldPackerName << " = new FieldPacker("
                  << AllocSize << ");\n";
  else
    return false;
  return true;
}

void RSReflectionJava::genPackVarOfType(const RSExportType *ET,
                                        const char *VarName,
                                        const char *FieldPackerName) {
  switch (ET->getClass()) {
  case RSExportType::ExportClassPrimitive:
  case RSExportType::ExportClassVector: {
    mOut.indent() << FieldPackerName << "."
                  << GetPackerAPIName(
                         static_cast<const RSExportPrimitiveType *>(ET)) << "("
                  << VarName << ");\n";
    break;
  }
  case RSExportType::ExportClassPointer: {
    // Must reflect as type Allocation in Java
    const RSExportType *PointeeType =
        static_cast<const RSExportPointerType *>(ET)->getPointeeType();

    if (PointeeType->getClass() != RSExportType::ExportClassRecord) {
      mOut.indent() << FieldPackerName << ".addI32(" << VarName
                    << ".getPtr());\n";
    } else {
      mOut.indent() << FieldPackerName << ".addI32(" << VarName
                    << ".getAllocation().getPtr());\n";
    }
    break;
  }
  case RSExportType::ExportClassMatrix: {
    mOut.indent() << FieldPackerName << ".addMatrix(" << VarName << ");\n";
    break;
  }
  case RSExportType::ExportClassConstantArray: {
    const RSExportConstantArrayType *ECAT =
        static_cast<const RSExportConstantArrayType *>(ET);

    // TODO(zonr): more elegant way. Currently, we obtain the unique index
    //             variable (this method involves recursive call which means
    //             we may have more than one level loop, therefore we can't
    //             always use the same index variable name here) name given
    //             in the for-loop from counting the '.' in @VarName.
    unsigned Level = 0;
    size_t LastDotPos = 0;
    std::string ElementVarName(VarName);

    while (LastDotPos != std::string::npos) {
      LastDotPos = ElementVarName.find_first_of('.', LastDotPos + 1);
      Level++;
    }
    std::string IndexVarName("ct");
    IndexVarName.append(llvm::utostr_32(Level));

    mOut.indent() << "for (int " << IndexVarName << " = 0; " << IndexVarName
                  << " < " << ECAT->getSize() << "; " << IndexVarName << "++)";
    mOut.startBlock();

    ElementVarName.append("[" + IndexVarName + "]");
    genPackVarOfType(ECAT->getElementType(), ElementVarName.c_str(),
                     FieldPackerName);

    mOut.endBlock();
    break;
  }
  case RSExportType::ExportClassRecord: {
    const RSExportRecordType *ERT = static_cast<const RSExportRecordType *>(ET);
    // Relative pos from now on in field packer
    unsigned Pos = 0;

    for (RSExportRecordType::const_field_iterator I = ERT->fields_begin(),
                                                  E = ERT->fields_end();
         I != E; I++) {
      const RSExportRecordType::Field *F = *I;
      std::string FieldName;
      size_t FieldOffset = F->getOffsetInParent();
      const RSExportType *T = F->getType();
      size_t FieldStoreSize = T->getStoreSize();
      size_t FieldAllocSize = T->getAllocSize();

      if (VarName != nullptr)
        FieldName = VarName + ("." + F->getName());
      else
        FieldName = F->getName();

      if (FieldOffset > Pos) {
        mOut.indent() << FieldPackerName << ".skip(" << (FieldOffset - Pos)
                      << ");\n";
      }

      genPackVarOfType(F->getType(), FieldName.c_str(), FieldPackerName);

      // There is padding in the field type
      if (FieldAllocSize > FieldStoreSize) {
        mOut.indent() << FieldPackerName << ".skip("
                      << (FieldAllocSize - FieldStoreSize) << ");\n";
      }

      Pos = FieldOffset + FieldAllocSize;
    }

    // There maybe some padding after the struct
    if (ERT->getAllocSize() > Pos) {
      mOut.indent() << FieldPackerName << ".skip(" << ERT->getAllocSize() - Pos
                    << ");\n";
    }
    break;
  }
  default: { slangAssert(false && "Unknown class of type"); }
  }
}

void RSReflectionJava::genAllocateVarOfType(const RSExportType *T,
                                            const std::string &VarName) {
  switch (T->getClass()) {
  case RSExportType::ExportClassPrimitive: {
    // Primitive type like int in Java has its own storage once it's declared.
    //
    // FIXME: Should we allocate storage for RS object?
    // if (static_cast<const RSExportPrimitiveType *>(T)->isRSObjectType())
    //  mOut.indent() << VarName << " = new " << GetTypeName(T) << "();\n";
    break;
  }
  case RSExportType::ExportClassPointer: {
    // Pointer type is an instance of Allocation or a TypeClass whose value is
    // expected to be assigned by programmer later in Java program. Therefore
    // we don't reflect things like [VarName] = new Allocation();
    mOut.indent() << VarName << " = null;\n";
    break;
  }
  case RSExportType::ExportClassConstantArray: {
    const RSExportConstantArrayType *ECAT =
        static_cast<const RSExportConstantArrayType *>(T);
    const RSExportType *ElementType = ECAT->getElementType();

    mOut.indent() << VarName << " = new " << GetTypeName(ElementType) << "["
                  << ECAT->getSize() << "];\n";

    // Primitive type element doesn't need allocation code.
    if (ElementType->getClass() != RSExportType::ExportClassPrimitive) {
      mOut.indent() << "for (int $ct = 0; $ct < " << ECAT->getSize()
                    << "; $ct++)";
      mOut.startBlock();

      std::string ElementVarName(VarName);
      ElementVarName.append("[$ct]");
      genAllocateVarOfType(ElementType, ElementVarName);

      mOut.endBlock();
    }
    break;
  }
  case RSExportType::ExportClassVector:
  case RSExportType::ExportClassMatrix:
  case RSExportType::ExportClassRecord: {
    mOut.indent() << VarName << " = new " << GetTypeName(T) << "();\n";
    break;
  }
  }
}

void RSReflectionJava::genNewItemBufferIfNull(const char *Index) {
  mOut.indent() << "if (" << RS_TYPE_ITEM_BUFFER_NAME " == null) ";
  mOut << RS_TYPE_ITEM_BUFFER_NAME << " = new " << RS_TYPE_ITEM_CLASS_NAME
       << "[getType().getX() /* count */];\n";
  if (Index != nullptr) {
    mOut.indent() << "if (" << RS_TYPE_ITEM_BUFFER_NAME << "[" << Index
                  << "] == null) ";
    mOut << RS_TYPE_ITEM_BUFFER_NAME << "[" << Index << "] = new "
         << RS_TYPE_ITEM_CLASS_NAME << "();\n";
  }
}

void RSReflectionJava::genNewItemBufferPackerIfNull() {
  mOut.indent() << "if (" << RS_TYPE_ITEM_BUFFER_PACKER_NAME << " == null) ";
  mOut << RS_TYPE_ITEM_BUFFER_PACKER_NAME " = new FieldPacker("
       <<  mItemSizeof << " * getType().getX()/* count */);\n";
}

/********************** Methods to generate type class  **********************/
bool RSReflectionJava::genTypeClass(const RSExportRecordType *ERT,
                                    std::string &ErrorMsg) {
  std::string ClassName = ERT->getElementName();
  std::string superClassName = getRSPackageName();
  superClassName += RS_TYPE_CLASS_SUPER_CLASS_NAME;

  if (!startClass(AM_Public, false, ClassName, superClassName.c_str(),
                  ErrorMsg))
    return false;

  mGeneratedFileNames->push_back(ClassName);

  genTypeItemClass(ERT);

  // Declare item buffer and item buffer packer
  mOut.indent() << "private " << RS_TYPE_ITEM_CLASS_NAME << " "
                << RS_TYPE_ITEM_BUFFER_NAME << "[];\n";
  mOut.indent() << "private FieldPacker " << RS_TYPE_ITEM_BUFFER_PACKER_NAME
                << ";\n";
  mOut.indent() << "private static java.lang.ref.WeakReference<Element> "
                << RS_TYPE_ELEMENT_REF_NAME
                << " = new java.lang.ref.WeakReference<Element>(null);\n";

  genTypeClassConstructor(ERT);
  genTypeClassCopyToArrayLocal(ERT);
  genTypeClassCopyToArray(ERT);
  genTypeClassItemSetter(ERT);
  genTypeClassItemGetter(ERT);
  genTypeClassComponentSetter(ERT);
  genTypeClassComponentGetter(ERT);
  genTypeClassCopyAll(ERT);
  if (!mRSContext->isCompatLib()) {
    // Skip the resize method if we are targeting a compatibility library.
    genTypeClassResize();
  }

  endClass();

  resetFieldIndex();
  clearFieldIndexMap();

  return true;
}

void RSReflectionJava::genTypeItemClass(const RSExportRecordType *ERT) {
  mOut.indent() << "static public class " RS_TYPE_ITEM_CLASS_NAME;
  mOut.startBlock();

  // Sizeof should not be exposed for 64-bit; it is not accurate
  if (mRSContext->getTargetAPI() < 21) {
      mOut.indent() << "public static final int sizeof = " << ERT->getAllocSize()
                    << ";\n";
  }

  // Member elements
  mOut << "\n";
  for (RSExportRecordType::const_field_iterator FI = ERT->fields_begin(),
                                                FE = ERT->fields_end();
       FI != FE; FI++) {
    mOut.indent() << GetTypeName((*FI)->getType()) << " " << (*FI)->getName()
                  << ";\n";
  }

  // Constructor
  mOut << "\n";
  mOut.indent() << RS_TYPE_ITEM_CLASS_NAME << "()";
  mOut.startBlock();

  for (RSExportRecordType::const_field_iterator FI = ERT->fields_begin(),
                                                FE = ERT->fields_end();
       FI != FE; FI++) {
    const RSExportRecordType::Field *F = *FI;
    genAllocateVarOfType(F->getType(), F->getName());
  }

  // end Constructor
  mOut.endBlock();

  // end Item class
  mOut.endBlock();
}

void RSReflectionJava::genTypeClassConstructor(const RSExportRecordType *ERT) {
  const char *RenderScriptVar = "rs";

  startFunction(AM_Public, true, "Element", "createElement", 1, "RenderScript",
                RenderScriptVar);

  // TODO(all): Fix weak-refs + multi-context issue.
  // mOut.indent() << "Element e = " << RS_TYPE_ELEMENT_REF_NAME
  //            << ".get();\n";
  // mOut.indent() << "if (e != null) return e;\n";
  RSReflectionJavaElementBuilder builder("eb", ERT, RenderScriptVar, &mOut,
                                         mRSContext, this);
  builder.generate();

  mOut.indent() << "return eb.create();\n";
  // mOut.indent() << "e = eb.create();\n";
  // mOut.indent() << RS_TYPE_ELEMENT_REF_NAME
  //            << " = new java.lang.ref.WeakReference<Element>(e);\n";
  // mOut.indent() << "return e;\n";
  endFunction();

  // private with element
  startFunction(AM_Private, false, nullptr, getClassName(), 1, "RenderScript",
                RenderScriptVar);
  mOut.indent() << RS_TYPE_ITEM_BUFFER_NAME << " = null;\n";
  mOut.indent() << RS_TYPE_ITEM_BUFFER_PACKER_NAME << " = null;\n";
  mOut.indent() << "mElement = createElement(" << RenderScriptVar << ");\n";
  endFunction();

  // 1D without usage
  startFunction(AM_Public, false, nullptr, getClassName(), 2, "RenderScript",
                RenderScriptVar, "int", "count");

  mOut.indent() << RS_TYPE_ITEM_BUFFER_NAME << " = null;\n";
  mOut.indent() << RS_TYPE_ITEM_BUFFER_PACKER_NAME << " = null;\n";
  mOut.indent() << "mElement = createElement(" << RenderScriptVar << ");\n";
  // Call init() in super class
  mOut.indent() << "init(" << RenderScriptVar << ", count);\n";
  endFunction();

  // 1D with usage
  startFunction(AM_Public, false, nullptr, getClassName(), 3, "RenderScript",
                RenderScriptVar, "int", "count", "int", "usages");

  mOut.indent() << RS_TYPE_ITEM_BUFFER_NAME << " = null;\n";
  mOut.indent() << RS_TYPE_ITEM_BUFFER_PACKER_NAME << " = null;\n";
  mOut.indent() << "mElement = createElement(" << RenderScriptVar << ");\n";
  // Call init() in super class
  mOut.indent() << "init(" << RenderScriptVar << ", count, usages);\n";
  endFunction();

  // create1D with usage
  startFunction(AM_Public, true, getClassName().c_str(), "create1D", 3,
                "RenderScript", RenderScriptVar, "int", "dimX", "int",
                "usages");
  mOut.indent() << getClassName() << " obj = new " << getClassName() << "("
                << RenderScriptVar << ");\n";
  mOut.indent() << "obj.mAllocation = Allocation.createSized("
                   "rs, obj.mElement, dimX, usages);\n";
  mOut.indent() << "return obj;\n";
  endFunction();

  // create1D without usage
  startFunction(AM_Public, true, getClassName().c_str(), "create1D", 2,
                "RenderScript", RenderScriptVar, "int", "dimX");
  mOut.indent() << "return create1D(" << RenderScriptVar
                << ", dimX, Allocation.USAGE_SCRIPT);\n";
  endFunction();

  // create2D without usage
  startFunction(AM_Public, true, getClassName().c_str(), "create2D", 3,
                "RenderScript", RenderScriptVar, "int", "dimX", "int", "dimY");
  mOut.indent() << "return create2D(" << RenderScriptVar
                << ", dimX, dimY, Allocation.USAGE_SCRIPT);\n";
  endFunction();

  // create2D with usage
  startFunction(AM_Public, true, getClassName().c_str(), "create2D", 4,
                "RenderScript", RenderScriptVar, "int", "dimX", "int", "dimY",
                "int", "usages");

  mOut.indent() << getClassName() << " obj = new " << getClassName() << "("
                << RenderScriptVar << ");\n";
  mOut.indent() << "Type.Builder b = new Type.Builder(rs, obj.mElement);\n";
  mOut.indent() << "b.setX(dimX);\n";
  mOut.indent() << "b.setY(dimY);\n";
  mOut.indent() << "Type t = b.create();\n";
  mOut.indent() << "obj.mAllocation = Allocation.createTyped(rs, t, usages);\n";
  mOut.indent() << "return obj;\n";
  endFunction();

  // createTypeBuilder
  startFunction(AM_Public, true, "Type.Builder", "createTypeBuilder", 1,
                "RenderScript", RenderScriptVar);
  mOut.indent() << "Element e = createElement(" << RenderScriptVar << ");\n";
  mOut.indent() << "return new Type.Builder(rs, e);\n";
  endFunction();

  // createCustom with usage
  startFunction(AM_Public, true, getClassName().c_str(), "createCustom", 3,
                "RenderScript", RenderScriptVar, "Type.Builder", "tb", "int",
                "usages");
  mOut.indent() << getClassName() << " obj = new " << getClassName() << "("
                << RenderScriptVar << ");\n";
  mOut.indent() << "Type t = tb.create();\n";
  mOut.indent() << "if (t.getElement() != obj.mElement) {\n";
  mOut.indent() << "    throw new RSIllegalArgumentException("
                   "\"Type.Builder did not match expected element type.\");\n";
  mOut.indent() << "}\n";
  mOut.indent() << "obj.mAllocation = Allocation.createTyped(rs, t, usages);\n";
  mOut.indent() << "return obj;\n";
  endFunction();
}

void RSReflectionJava::genTypeClassCopyToArray(const RSExportRecordType *ERT) {
  startFunction(AM_Private, false, "void", "copyToArray", 2,
                RS_TYPE_ITEM_CLASS_NAME, "i", "int", "index");

  genNewItemBufferPackerIfNull();
  mOut.indent() << RS_TYPE_ITEM_BUFFER_PACKER_NAME << ".reset(index * "
                << mItemSizeof << ");\n";

  mOut.indent() << "copyToArrayLocal(i, " RS_TYPE_ITEM_BUFFER_PACKER_NAME
                   ");\n";

  endFunction();
}

void
RSReflectionJava::genTypeClassCopyToArrayLocal(const RSExportRecordType *ERT) {
  startFunction(AM_Private, false, "void", "copyToArrayLocal", 2,
                RS_TYPE_ITEM_CLASS_NAME, "i", "FieldPacker", "fp");

  genPackVarOfType(ERT, "i", "fp");

  endFunction();
}

void RSReflectionJava::genTypeClassItemSetter(const RSExportRecordType *ERT) {
  startFunction(AM_PublicSynchronized, false, "void", "set", 3,
                RS_TYPE_ITEM_CLASS_NAME, "i", "int", "index", "boolean",
                "copyNow");
  genNewItemBufferIfNull(nullptr);
  mOut.indent() << RS_TYPE_ITEM_BUFFER_NAME << "[index] = i;\n";

  mOut.indent() << "if (copyNow) ";
  mOut.startBlock();

  mOut.indent() << "copyToArray(i, index);\n";
  mOut.indent() << "FieldPacker fp = new FieldPacker(" << mItemSizeof << ");\n";
  mOut.indent() << "copyToArrayLocal(i, fp);\n";
  mOut.indent() << "mAllocation.setFromFieldPacker(index, fp);\n";

  // End of if (copyNow)
  mOut.endBlock();

  endFunction();
}

void RSReflectionJava::genTypeClassItemGetter(const RSExportRecordType *ERT) {
  startFunction(AM_PublicSynchronized, false, RS_TYPE_ITEM_CLASS_NAME, "get", 1,
                "int", "index");
  mOut.indent() << "if (" << RS_TYPE_ITEM_BUFFER_NAME
                << " == null) return null;\n";
  mOut.indent() << "return " << RS_TYPE_ITEM_BUFFER_NAME << "[index];\n";
  endFunction();
}

void
RSReflectionJava::genTypeClassComponentSetter(const RSExportRecordType *ERT) {
  for (RSExportRecordType::const_field_iterator FI = ERT->fields_begin(),
                                                FE = ERT->fields_end();
       FI != FE; FI++) {
    const RSExportRecordType::Field *F = *FI;
    size_t FieldOffset = F->getOffsetInParent();
    size_t FieldStoreSize = F->getType()->getStoreSize();
    unsigned FieldIndex = getFieldIndex(F);

    startFunction(AM_PublicSynchronized, false, "void", "set_" + F->getName(),
                  3, "int", "index", GetTypeName(F->getType()).c_str(), "v",
                  "boolean", "copyNow");
    genNewItemBufferPackerIfNull();
    genNewItemBufferIfNull("index");
    mOut.indent() << RS_TYPE_ITEM_BUFFER_NAME << "[index]." << F->getName()
                  << " = v;\n";

    mOut.indent() << "if (copyNow) ";
    mOut.startBlock();

    if (FieldOffset > 0) {
      mOut.indent() << RS_TYPE_ITEM_BUFFER_PACKER_NAME << ".reset(index * "
                    << mItemSizeof << " + " << FieldOffset
                    << ");\n";
    } else {
      mOut.indent() << RS_TYPE_ITEM_BUFFER_PACKER_NAME << ".reset(index * "
                    << mItemSizeof << ");\n";
    }
    genPackVarOfType(F->getType(), "v", RS_TYPE_ITEM_BUFFER_PACKER_NAME);

    mOut.indent() << "FieldPacker fp = new FieldPacker(" << FieldStoreSize
                  << ");\n";
    genPackVarOfType(F->getType(), "v", "fp");
    mOut.indent() << "mAllocation.setFromFieldPacker(index, " << FieldIndex
                  << ", fp);\n";

    // End of if (copyNow)
    mOut.endBlock();

    endFunction();
  }
}

void
RSReflectionJava::genTypeClassComponentGetter(const RSExportRecordType *ERT) {
  for (RSExportRecordType::const_field_iterator FI = ERT->fields_begin(),
                                                FE = ERT->fields_end();
       FI != FE; FI++) {
    const RSExportRecordType::Field *F = *FI;
    startFunction(AM_PublicSynchronized, false,
                  GetTypeName(F->getType()).c_str(), "get_" + F->getName(), 1,
                  "int", "index");
    mOut.indent() << "if (" RS_TYPE_ITEM_BUFFER_NAME << " == null) return "
                  << GetTypeNullValue(F->getType()) << ";\n";
    mOut.indent() << "return " RS_TYPE_ITEM_BUFFER_NAME << "[index]."
                  << F->getName() << ";\n";
    endFunction();
  }
}

void RSReflectionJava::genTypeClassCopyAll(const RSExportRecordType *ERT) {
  startFunction(AM_PublicSynchronized, false, "void", "copyAll", 0);

  mOut.indent() << "for (int ct = 0; ct < " << RS_TYPE_ITEM_BUFFER_NAME
                << ".length; ct++)"
                << " copyToArray(" << RS_TYPE_ITEM_BUFFER_NAME
                << "[ct], ct);\n";
  mOut.indent() << "mAllocation.setFromFieldPacker(0, "
                << RS_TYPE_ITEM_BUFFER_PACKER_NAME ");\n";

  endFunction();
}

void RSReflectionJava::genTypeClassResize() {
  startFunction(AM_PublicSynchronized, false, "void", "resize", 1, "int",
                "newSize");

  mOut.indent() << "if (mItemArray != null) ";
  mOut.startBlock();
  mOut.indent() << "int oldSize = mItemArray.length;\n";
  mOut.indent() << "int copySize = Math.min(oldSize, newSize);\n";
  mOut.indent() << "if (newSize == oldSize) return;\n";
  mOut.indent() << "Item ni[] = new Item[newSize];\n";
  mOut.indent() << "System.arraycopy(mItemArray, 0, ni, 0, copySize);\n";
  mOut.indent() << "mItemArray = ni;\n";
  mOut.endBlock();
  mOut.indent() << "mAllocation.resize(newSize);\n";

  mOut.indent() << "if (" RS_TYPE_ITEM_BUFFER_PACKER_NAME
                   " != null) " RS_TYPE_ITEM_BUFFER_PACKER_NAME " = "
                   "new FieldPacker(" << mItemSizeof << " * getType().getX()/* count */);\n";

  endFunction();
}

/******************** Methods to generate type class /end ********************/

/********** Methods to create Element in Java of given record type ***********/

RSReflectionJavaElementBuilder::RSReflectionJavaElementBuilder(
    const char *ElementBuilderName, const RSExportRecordType *ERT,
    const char *RenderScriptVar, GeneratedFile *Out, const RSContext *RSContext,
    RSReflectionJava *Reflection)
    : mElementBuilderName(ElementBuilderName), mERT(ERT),
      mRenderScriptVar(RenderScriptVar), mOut(Out), mPaddingFieldIndex(1),
      mRSContext(RSContext), mReflection(Reflection) {
  if (mRSContext->getTargetAPI() < SLANG_ICS_TARGET_API) {
    mPaddingPrefix = "#padding_";
  } else {
    mPaddingPrefix = "#rs_padding_";
  }
}

void RSReflectionJavaElementBuilder::generate() {
  mOut->indent() << "Element.Builder " << mElementBuilderName
                 << " = new Element.Builder(" << mRenderScriptVar << ");\n";
  genAddElement(mERT, "", /* ArraySize = */ 0);
}

void RSReflectionJavaElementBuilder::genAddElement(const RSExportType *ET,
                                                   const std::string &VarName,
                                                   unsigned ArraySize) {
  std::string ElementConstruct = GetBuiltinElementConstruct(ET);

  if (ElementConstruct != "") {
    genAddStatementStart();
    *mOut << ElementConstruct << "(" << mRenderScriptVar << ")";
    genAddStatementEnd(VarName, ArraySize);
  } else {

    switch (ET->getClass()) {
    case RSExportType::ExportClassPrimitive: {
      const RSExportPrimitiveType *EPT =
          static_cast<const RSExportPrimitiveType *>(ET);
      const char *DataTypeName =
          RSExportPrimitiveType::getRSReflectionType(EPT)->rs_type;
      genAddStatementStart();
      *mOut << "Element.createUser(" << mRenderScriptVar
            << ", Element.DataType." << DataTypeName << ")";
      genAddStatementEnd(VarName, ArraySize);
      break;
    }
    case RSExportType::ExportClassVector: {
      const RSExportVectorType *EVT =
          static_cast<const RSExportVectorType *>(ET);
      const char *DataTypeName =
          RSExportPrimitiveType::getRSReflectionType(EVT)->rs_type;
      genAddStatementStart();
      *mOut << "Element.createVector(" << mRenderScriptVar
            << ", Element.DataType." << DataTypeName << ", "
            << EVT->getNumElement() << ")";
      genAddStatementEnd(VarName, ArraySize);
      break;
    }
    case RSExportType::ExportClassPointer:
      // Pointer type variable should be resolved in
      // GetBuiltinElementConstruct()
      slangAssert(false && "??");
      break;
    case RSExportType::ExportClassMatrix:
      // Matrix type variable should be resolved
      // in GetBuiltinElementConstruct()
      slangAssert(false && "??");
      break;
    case RSExportType::ExportClassConstantArray: {
      const RSExportConstantArrayType *ECAT =
          static_cast<const RSExportConstantArrayType *>(ET);

      const RSExportType *ElementType = ECAT->getElementType();
      if (ElementType->getClass() != RSExportType::ExportClassRecord) {
        genAddElement(ECAT->getElementType(), VarName, ECAT->getSize());
      } else {
        std::string NewElementBuilderName(mElementBuilderName);
        NewElementBuilderName.append(1, '_');

        RSReflectionJavaElementBuilder builder(
            NewElementBuilderName.c_str(),
            static_cast<const RSExportRecordType *>(ElementType),
            mRenderScriptVar, mOut, mRSContext, mReflection);
        builder.generate();

        ArraySize = ECAT->getSize();
        genAddStatementStart();
        *mOut << NewElementBuilderName << ".create()";
        genAddStatementEnd(VarName, ArraySize);
      }
      break;
    }
    case RSExportType::ExportClassRecord: {
      // Simalar to case of RSExportType::ExportClassRecord in genPackVarOfType.
      //
      // TODO(zonr): Generalize these two function such that there's no
      //             duplicated codes.
      const RSExportRecordType *ERT =
          static_cast<const RSExportRecordType *>(ET);
      int Pos = 0; // relative pos from now on

      for (RSExportRecordType::const_field_iterator I = ERT->fields_begin(),
                                                    E = ERT->fields_end();
           I != E; I++) {
        const RSExportRecordType::Field *F = *I;
        int FieldOffset = F->getOffsetInParent();
        const RSExportType *T = F->getType();
        int FieldStoreSize = T->getStoreSize();
        int FieldAllocSize = T->getAllocSize();

        std::string FieldName;
        if (!VarName.empty())
          FieldName = VarName + "." + F->getName();
        else
          FieldName = F->getName();

        // Alignment
        genAddPadding(FieldOffset - Pos);

        // eb.add(...)
        mReflection->addFieldIndexMapping(F);
        if (F->getType()->getClass() != RSExportType::ExportClassRecord) {
          genAddElement(F->getType(), FieldName, 0);
        } else {
          std::string NewElementBuilderName(mElementBuilderName);
          NewElementBuilderName.append(1, '_');

          RSReflectionJavaElementBuilder builder(
              NewElementBuilderName.c_str(),
              static_cast<const RSExportRecordType *>(F->getType()),
              mRenderScriptVar, mOut, mRSContext, mReflection);
          builder.generate();

          genAddStatementStart();
          *mOut << NewElementBuilderName << ".create()";
          genAddStatementEnd(FieldName, ArraySize);
        }

        if (mRSContext->getTargetAPI() < SLANG_ICS_TARGET_API) {
          // There is padding within the field type. This is only necessary
          // for HC-targeted APIs.
          genAddPadding(FieldAllocSize - FieldStoreSize);
        }

        Pos = FieldOffset + FieldAllocSize;
      }

      // There maybe some padding after the struct
      size_t RecordAllocSize = ERT->getAllocSize();

      genAddPadding(RecordAllocSize - Pos);
      break;
    }
    default:
      slangAssert(false && "Unknown class of type");
      break;
    }
  }
}

void RSReflectionJavaElementBuilder::genAddPadding(int PaddingSize) {
  while (PaddingSize > 0) {
    const std::string &VarName = createPaddingField();
    genAddStatementStart();
    if (PaddingSize >= 4) {
      *mOut << "Element.U32(" << mRenderScriptVar << ")";
      PaddingSize -= 4;
    } else if (PaddingSize >= 2) {
      *mOut << "Element.U16(" << mRenderScriptVar << ")";
      PaddingSize -= 2;
    } else if (PaddingSize >= 1) {
      *mOut << "Element.U8(" << mRenderScriptVar << ")";
      PaddingSize -= 1;
    }
    genAddStatementEnd(VarName, 0);
  }
}

void RSReflectionJavaElementBuilder::genAddStatementStart() {
  mOut->indent() << mElementBuilderName << ".add(";
}

void
RSReflectionJavaElementBuilder::genAddStatementEnd(const std::string &VarName,
                                                   unsigned ArraySize) {
  *mOut << ", \"" << VarName << "\"";
  if (ArraySize > 0) {
    *mOut << ", " << ArraySize;
  }
  *mOut << ");\n";
  // TODO Review incFieldIndex.  It's probably better to assign the numbers at
  // the start rather
  // than as we're generating the code.
  mReflection->incFieldIndex();
}

/******** Methods to create Element in Java of given record type /end ********/

bool RSReflectionJava::reflect() {
  std::string ErrorMsg;
  if (!genScriptClass(mScriptClassName, ErrorMsg)) {
    std::cerr << "Failed to generate class " << mScriptClassName << " ("
              << ErrorMsg << ")\n";
    return false;
  }

  mGeneratedFileNames->push_back(mScriptClassName);

  // class ScriptField_<TypeName>
  for (RSContext::const_export_type_iterator
           TI = mRSContext->export_types_begin(),
           TE = mRSContext->export_types_end();
       TI != TE; TI++) {
    const RSExportType *ET = TI->getValue();

    if (ET->getClass() == RSExportType::ExportClassRecord) {
      const RSExportRecordType *ERT =
          static_cast<const RSExportRecordType *>(ET);

      if (!ERT->isArtificial() && !genTypeClass(ERT, ErrorMsg)) {
        std::cerr << "Failed to generate type class for struct '"
                  << ERT->getName() << "' (" << ErrorMsg << ")\n";
        return false;
      }
    }
  }

  return true;
}

const char *RSReflectionJava::AccessModifierStr(AccessModifier AM) {
  switch (AM) {
  case AM_Public:
    return "public";
    break;
  case AM_Protected:
    return "protected";
    break;
  case AM_Private:
    return "private";
    break;
  case AM_PublicSynchronized:
    return "public synchronized";
    break;
  default:
    return "";
    break;
  }
}

bool RSReflectionJava::startClass(AccessModifier AM, bool IsStatic,
                                  const std::string &ClassName,
                                  const char *SuperClassName,
                                  std::string &ErrorMsg) {
  // Open file for class
  std::string FileName = ClassName + ".java";
  if (!mOut.startFile(mOutputDirectory, FileName, mRSSourceFileName,
                      mRSContext->getLicenseNote(), true,
                      mRSContext->getVerbose())) {
    return false;
  }

  // Package
  if (!mPackageName.empty()) {
    mOut << "package " << mPackageName << ";\n";
  }
  mOut << "\n";

  // Imports
  mOut << "import " << mRSPackageName << ".*;\n";
  if (getEmbedBitcodeInJava()) {
    mOut << "import " << mPackageName << "."
          << RSSlangReflectUtils::JavaBitcodeClassNameFromRSFileName(
                 mRSSourceFileName.c_str()) << ";\n";
  } else {
    mOut << "import android.content.res.Resources;\n";
  }
  mOut << "\n";

  // All reflected classes should be annotated as hidden, so that they won't
  // be exposed in SDK.
  mOut << "/**\n";
  mOut << " * @hide\n";
  mOut << " */\n";

  mOut << AccessModifierStr(AM) << ((IsStatic) ? " static" : "") << " class "
       << ClassName;
  if (SuperClassName != nullptr)
    mOut << " extends " << SuperClassName;

  mOut.startBlock();

  mClassName = ClassName;

  return true;
}

void RSReflectionJava::endClass() {
  mOut.endBlock();
  mOut.closeFile();
  clear();
}

void RSReflectionJava::startTypeClass(const std::string &ClassName) {
  mOut.indent() << "public static class " << ClassName;
  mOut.startBlock();
}

void RSReflectionJava::endTypeClass() { mOut.endBlock(); }

void RSReflectionJava::startFunction(AccessModifier AM, bool IsStatic,
                                     const char *ReturnType,
                                     const std::string &FunctionName, int Argc,
                                     ...) {
  ArgTy Args;
  va_list vl;
  va_start(vl, Argc);

  for (int i = 0; i < Argc; i++) {
    const char *ArgType = va_arg(vl, const char *);
    const char *ArgName = va_arg(vl, const char *);

    Args.push_back(std::make_pair(ArgType, ArgName));
  }
  va_end(vl);

  startFunction(AM, IsStatic, ReturnType, FunctionName, Args);
}

void RSReflectionJava::startFunction(AccessModifier AM, bool IsStatic,
                                     const char *ReturnType,
                                     const std::string &FunctionName,
                                     const ArgTy &Args) {
  mOut.indent() << AccessModifierStr(AM) << ((IsStatic) ? " static " : " ")
                << ((ReturnType) ? ReturnType : "") << " " << FunctionName
                << "(";

  bool FirstArg = true;
  for (ArgTy::const_iterator I = Args.begin(), E = Args.end(); I != E; I++) {
    if (!FirstArg)
      mOut << ", ";
    else
      FirstArg = false;

    mOut << I->first << " " << I->second;
  }

  mOut << ")";
  mOut.startBlock();
}

void RSReflectionJava::endFunction() { mOut.endBlock(); }

bool RSReflectionJava::addTypeNameForElement(const std::string &TypeName) {
  if (mTypesToCheck.find(TypeName) == mTypesToCheck.end()) {
    mTypesToCheck.insert(TypeName);
    return true;
  } else {
    return false;
  }
}

bool RSReflectionJava::addTypeNameForFieldPacker(const std::string &TypeName) {
  if (mFieldPackerTypes.find(TypeName) == mFieldPackerTypes.end()) {
    mFieldPackerTypes.insert(TypeName);
    return true;
  } else {
    return false;
  }
}

} // namespace slang
