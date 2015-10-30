LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)
include $(CLEAR_TBLGEN_VARS)

TBLGEN_TABLES := \
  DiagnosticCommonKinds.inc \
  DeclNodes.inc \
  StmtNodes.inc

lldb_PluginProcessElfCore_SRC_FILES := \
  ProcessElfCore.cpp \
  RegisterContextCoreFreeBSD_x86_64.cpp \
  RegisterContextCoreLinux_x86_64.cpp \
  ThreadElfCore.cpp

LOCAL_SRC_FILES := $(lldb_PluginProcessElfCore_SRC_FILES)

LOCAL_MODULE:= liblldbPluginProcessElfCore
LOCAL_MODULE_TAGS := optional

include $(LLDB_BUILD_MK)
include $(CLANG_VERSION_INC_MK)
include $(CLANG_TBLGEN_RULES_MK)
include $(BUILD_HOST_STATIC_LIBRARY)
