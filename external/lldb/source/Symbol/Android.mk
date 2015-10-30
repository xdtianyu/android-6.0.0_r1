LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)
include $(CLEAR_TBLGEN_VARS)

TBLGEN_TABLES := \
  AttrList.inc \
  Attrs.inc \
  CommentCommandList.inc \
  DiagnosticCommonKinds.inc \
  DeclNodes.inc \
  StmtNodes.inc

lldb_Symbol_SRC_FILES := \
  Block.cpp \
  ClangASTContext.cpp \
  ClangASTImporter.cpp \
  ClangASTType.cpp \
  ClangExternalASTSourceCallbacks.cpp \
  ClangExternalASTSourceCommon.cpp \
  ClangNamespaceDecl.cpp \
  CompileUnit.cpp \
  Declaration.cpp \
  DWARFCallFrameInfo.cpp \
  Function.cpp \
  FuncUnwinders.cpp \
  LineEntry.cpp \
  LineTable.cpp \
  ObjectFile.cpp \
  SymbolContext.cpp \
  Symbol.cpp \
  SymbolFile.cpp \
  SymbolVendor.cpp \
  Symtab.cpp \
  Type.cpp \
  TypeList.cpp \
  UnwindPlan.cpp \
  UnwindTable.cpp \
  Variable.cpp \
  VariableList.cpp \
  VerifyDecl.cpp

LOCAL_SRC_FILES := $(lldb_Symbol_SRC_FILES)

LOCAL_MODULE:= liblldbSymbol
LOCAL_MODULE_TAGS := optional

include $(LLDB_BUILD_MK)
include $(CLANG_VERSION_INC_MK)
include $(CLANG_TBLGEN_RULES_MK)
include $(BUILD_HOST_STATIC_LIBRARY)
