LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)
include $(CLEAR_TBLGEN_VARS)

TBLGEN_TABLES := \
  DiagnosticCommonKinds.inc \
  DeclNodes.inc \
  StmtNodes.inc

lldb_PluginUtility_SRC_FILES := \
  DynamicRegisterInfo.cpp \
  InferiorCallPOSIX.cpp \
  RegisterContextDarwin_arm.cpp \
  RegisterContextDarwin_i386.cpp \
  RegisterContextDarwin_x86_64.cpp \
  RegisterContextDummy.cpp \
  RegisterContextLLDB.cpp \
  RegisterContextMach_arm.cpp \
  RegisterContextMach_i386.cpp \
  RegisterContextMach_x86_64.cpp \
  RegisterContextMacOSXFrameBackchain.cpp \
  RegisterContextMemory.cpp \
  RegisterContextThreadMemory.cpp \
  StopInfoMachException.cpp \
  ThreadMemory.cpp \
  UnwindLLDB.cpp \
  UnwindMacOSXFrameBackchain.cpp

LOCAL_SRC_FILES := $(lldb_PluginUtility_SRC_FILES)

LOCAL_MODULE:= liblldbPluginUtility
LOCAL_MODULE_TAGS := optional

include $(LLDB_BUILD_MK)
include $(CLANG_VERSION_INC_MK)
include $(CLANG_TBLGEN_RULES_MK)
include $(BUILD_HOST_STATIC_LIBRARY)
