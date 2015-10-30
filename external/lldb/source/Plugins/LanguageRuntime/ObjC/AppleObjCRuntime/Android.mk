LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)
include $(CLEAR_TBLGEN_VARS)

TBLGEN_TABLES := \
  CommentCommandList.inc \
  DiagnosticCommonKinds.inc \
  DeclNodes.inc \
  StmtNodes.inc

lldb_PluginLanguageRuntimeObjCAppleObjCRuntime_SRC_FILES := \
  AppleObjCRuntime.cpp \
  AppleObjCRuntimeV1.cpp \
  AppleObjCRuntimeV2.cpp \
  AppleObjCTrampolineHandler.cpp \
  AppleObjCTypeVendor.cpp \
  AppleThreadPlanStepThroughObjCTrampoline.cpp

LOCAL_SRC_FILES := $(lldb_PluginLanguageRuntimeObjCAppleObjCRuntime_SRC_FILES)

LOCAL_MODULE:= liblldbPluginLanguageRuntimeObjCAppleObjCRuntime
LOCAL_MODULE_TAGS := optional

include $(LLDB_BUILD_MK)
include $(CLANG_VERSION_INC_MK)
include $(CLANG_TBLGEN_RULES_MK)
include $(BUILD_HOST_STATIC_LIBRARY)
