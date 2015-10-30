LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)
include $(CLEAR_TBLGEN_VARS)

TBLGEN_TABLES := \
  DiagnosticCommonKinds.inc \
  DeclNodes.inc \
  StmtNodes.inc

lldb_HostCommon_SRC_FILES := \
  Condition.cpp \
  DynamicLibrary.cpp \
  File.cpp \
  FileSpec.cpp \
  Host.cpp \
  Mutex.cpp \
  SocketAddress.cpp \
  Symbols.cpp \
  Terminal.cpp \
  TimeValue.cpp

LOCAL_SRC_FILES := $(lldb_HostCommon_SRC_FILES)

LOCAL_MODULE:= liblldbHostCommon
LOCAL_MODULE_TAGS := optional

include $(LLDB_BUILD_MK)
include $(CLANG_VERSION_INC_MK)
include $(CLANG_TBLGEN_RULES_MK)
include $(BUILD_HOST_STATIC_LIBRARY)
