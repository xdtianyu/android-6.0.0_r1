LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)
include $(CLEAR_TBLGEN_VARS)

TBLGEN_TABLES := \
  DiagnosticCommonKinds.inc \
  DeclNodes.inc \
  StmtNodes.inc

lldb_PluginProcessDarwin_SRC_FILES := \
  CommunicationKDP.cpp \
  ProcessKDP.cpp \
  ProcessKDPLog.cpp \
  RegisterContextKDP_arm.cpp \
  RegisterContextKDP_i386.cpp \
  RegisterContextKDP_x86_64.cpp \
  ThreadKDP.cpp

LOCAL_SRC_FILES := $(lldb_PluginProcessDarwin_SRC_FILES)

LOCAL_MODULE:= liblldbPluginProcessDarwin
LOCAL_MODULE_TAGS := optional

include $(LLDB_BUILD_MK)
include $(CLANG_VERSION_INC_MK)
include $(CLANG_TBLGEN_RULES_MK)
include $(BUILD_HOST_STATIC_LIBRARY)
