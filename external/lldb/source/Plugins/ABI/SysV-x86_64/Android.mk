LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)
include $(CLEAR_TBLGEN_VARS)

TBLGEN_TABLES := \
  DiagnosticCommonKinds.inc \
  DeclNodes.inc \
  StmtNodes.inc

lldb_PluginABISysV_x86_64_SRC_FILES := \
  ABISysV_x86_64.cpp

LOCAL_SRC_FILES := $(lldb_PluginABISysV_x86_64_SRC_FILES)

LOCAL_MODULE:= liblldbPluginABISysV_x86_64
LOCAL_MODULE_TAGS := optional

include $(LLDB_BUILD_MK)
include $(CLANG_VERSION_INC_MK)
include $(CLANG_TBLGEN_RULES_MK)
include $(BUILD_HOST_STATIC_LIBRARY)
