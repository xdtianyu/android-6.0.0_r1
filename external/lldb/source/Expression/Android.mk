LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)
include $(CLEAR_TBLGEN_VARS)

TBLGEN_TABLES := \
  AttrParsedAttrList.inc \
  Attrs.inc \
  CommentCommandList.inc \
  DiagnosticCommonKinds.inc \
  DiagnosticFrontendKinds.inc \
  DiagnosticSemaKinds.inc \
  DeclNodes.inc \
  StmtNodes.inc

lldb_Expression_SRC_FILES := \
  ASTDumper.cpp \
  ASTResultSynthesizer.cpp \
  ASTStructExtractor.cpp \
  ClangASTSource.cpp \
  ClangExpressionDeclMap.cpp \
  ClangExpressionParser.cpp \
  ClangExpressionVariable.cpp \
  ClangFunction.cpp \
  ClangPersistentVariables.cpp \
  ClangUserExpression.cpp \
  ClangUtilityFunction.cpp \
  DWARFExpression.cpp \
  ExpressionSourceCode.cpp \
  IRDynamicChecks.cpp \
  IRExecutionUnit.cpp \
  IRForTarget.cpp \
  IRInterpreter.cpp \
  IRMemoryMap.cpp \
  Materializer.cpp

LOCAL_SRC_FILES := $(lldb_Expression_SRC_FILES)

LOCAL_MODULE:= liblldbExpression
LOCAL_MODULE_TAGS := optional

include $(LLDB_BUILD_MK)
include $(CLANG_VERSION_INC_MK)
include $(CLANG_TBLGEN_RULES_MK)
include $(LLVM_GEN_INTRINSICS_MK)
include $(BUILD_HOST_STATIC_LIBRARY)
