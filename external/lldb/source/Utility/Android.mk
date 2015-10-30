LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

lldb_Utility_SRC_FILES := \
  ARM_DWARF_Registers.cpp \
  KQueue.cpp \
  PseudoTerminal.cpp \
  Range.cpp \
  RefCounter.cpp \
  SharingPtr.cpp \
  StringExtractor.cpp \
  StringExtractorGDBRemote.cpp \
  TimeSpecTimeout.cpp

LOCAL_SRC_FILES := $(lldb_Utility_SRC_FILES)

LOCAL_MODULE:= liblldbUtility
LOCAL_MODULE_TAGS := optional

include $(LLDB_BUILD_MK)
include $(CLANG_VERSION_INC_MK)
include $(BUILD_HOST_STATIC_LIBRARY)
