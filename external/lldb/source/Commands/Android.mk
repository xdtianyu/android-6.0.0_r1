LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)
include $(CLEAR_TBLGEN_VARS)

TBLGEN_TABLES := \
  DiagnosticCommonKinds.inc \
  DeclNodes.inc \
  StmtNodes.inc

lldb_Commands_SRC_FILES := \
  CommandCompletions.cpp \
  CommandObjectApropos.cpp \
  CommandObjectArgs.cpp \
  CommandObjectBreakpointCommand.cpp \
  CommandObjectBreakpoint.cpp \
  CommandObjectCommands.cpp \
  CommandObjectDisassemble.cpp \
  CommandObjectExpression.cpp \
  CommandObjectFrame.cpp \
  CommandObjectHelp.cpp \
  CommandObjectLog.cpp \
  CommandObjectMemory.cpp \
  CommandObjectMultiword.cpp \
  CommandObjectPlatform.cpp \
  CommandObjectPlugin.cpp \
  CommandObjectProcess.cpp \
  CommandObjectQuit.cpp \
  CommandObjectRegister.cpp \
  CommandObjectSettings.cpp \
  CommandObjectSource.cpp \
  CommandObjectSyntax.cpp \
  CommandObjectTarget.cpp \
  CommandObjectThread.cpp \
  CommandObjectType.cpp \
  CommandObjectVersion.cpp \
  CommandObjectWatchpointCommand.cpp \
  CommandObjectWatchpoint.cpp

LOCAL_SRC_FILES := $(lldb_Commands_SRC_FILES)

LOCAL_MODULE:= liblldbCommands
LOCAL_MODULE_TAGS := optional

include $(LLDB_BUILD_MK)
include $(CLANG_VERSION_INC_MK)
include $(CLANG_TBLGEN_RULES_MK)
include $(BUILD_HOST_STATIC_LIBRARY)
