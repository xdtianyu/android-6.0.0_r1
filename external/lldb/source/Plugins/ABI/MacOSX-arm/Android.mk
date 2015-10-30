LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)
include $(CLEAR_TBLGEN_VARS)

TBLGEN_TABLES := \
  DiagnosticCommonKinds.inc \
  DeclNodes.inc \
  StmtNodes.inc

lldb_PluginABIMacOSX_arm_SRC_FILES := \
  ABIMacOSX_arm.cpp

LOCAL_SRC_FILES := $(lldb_PluginABIMacOSX_arm_SRC_FILES)

LOCAL_MODULE:= liblldbPluginABIMacOSX_arm
LOCAL_MODULE_TAGS := optional

include $(LLDB_BUILD_MK)
include $(CLANG_VERSION_INC_MK)
include $(CLANG_TBLGEN_RULES_MK)
include $(BUILD_HOST_STATIC_LIBRARY)
