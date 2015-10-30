LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

lldb_PluginObjectFileMachO_SRC_FILES := \
  ObjectFileMachO.cpp

LOCAL_SRC_FILES := $(lldb_PluginObjectFileMachO_SRC_FILES)

LOCAL_MODULE:= liblldbPluginObjectFileMachO
LOCAL_MODULE_TAGS := optional

include $(LLDB_BUILD_MK)
include $(CLANG_VERSION_INC_MK)
include $(BUILD_HOST_STATIC_LIBRARY)
