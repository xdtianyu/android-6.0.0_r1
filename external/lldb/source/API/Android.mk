LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)
include $(CLEAR_TBLGEN_VARS)

TBLGEN_TABLES := \
  CommentCommandList.inc \
  DiagnosticCommonKinds.inc \
  DeclNodes.inc \
  StmtNodes.inc

lldb_API_SRC_FILES := \
  SBAddress.cpp \
  SBBlock.cpp \
  SBBreakpoint.cpp \
  SBBreakpointLocation.cpp \
  SBBroadcaster.cpp \
  SBCommandInterpreter.cpp \
  SBCommandReturnObject.cpp \
  SBCommunication.cpp \
  SBCompileUnit.cpp \
  SBData.cpp \
  SBDebugger.cpp \
  SBDeclaration.cpp \
  SBError.cpp \
  SBEvent.cpp \
  SBExpressionOptions.cpp \
  SBFileSpec.cpp \
  SBFileSpecList.cpp \
  SBFrame.cpp \
  SBFunction.cpp \
  SBHostOS.cpp \
  SBInputReader.cpp \
  SBInstruction.cpp \
  SBInstructionList.cpp \
  SBLineEntry.cpp \
  SBListener.cpp \
  SBModule.cpp \
  SBModuleSpec.cpp \
  SBProcess.cpp \
  SBSection.cpp \
  SBSourceManager.cpp \
  SBStream.cpp \
  SBStringList.cpp \
  SBSymbolContext.cpp \
  SBSymbolContextList.cpp \
  SBSymbol.cpp \
  SBTarget.cpp \
  SBThread.cpp \
  SBTypeCategory.cpp \
  SBType.cpp \
  SBTypeFilter.cpp \
  SBTypeFormat.cpp \
  SBTypeNameSpecifier.cpp \
  SBTypeSummary.cpp \
  SBTypeSynthetic.cpp \
  SBValue.cpp \
  SBValueList.cpp \
  SBWatchpoint.cpp

LOCAL_SRC_FILES := $(lldb_API_SRC_FILES)

LOCAL_MODULE:= liblldbAPI
LOCAL_MODULE_TAGS := optional

include $(LLDB_BUILD_MK)
include $(CLANG_VERSION_INC_MK)
include $(CLANG_TBLGEN_RULES_MK)
include $(BUILD_HOST_STATIC_LIBRARY)
