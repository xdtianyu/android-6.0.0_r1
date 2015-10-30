LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)
include $(CLEAR_TBLGEN_VARS)

TBLGEN_TABLES := \
  DiagnosticCommonKinds.inc \
  DeclNodes.inc \
  StmtNodes.inc

lldb_HostMacOSX_SRC_FILES := \
  Host.mm \
  Symbols.cpp

lldb_HostMacOSX_CFCPP_SRC_FILES := \
	$(addprefix cfcpp/,$(notdir $(wildcard $(LOCAL_PATH)/cfcpp/*.cpp)))

LOCAL_SRC_FILES := $(lldb_HostMacOSX_SRC_FILES) \
	$(lldb_HostMacOSX_CFCPP_SRC_FILES)

LOCAL_MODULE:= liblldbHostMacOSX
LOCAL_MODULE_TAGS := optional

include $(LLDB_BUILD_MK)
include $(CLANG_VERSION_INC_MK)
include $(CLANG_TBLGEN_RULES_MK)
include $(BUILD_HOST_STATIC_LIBRARY)
