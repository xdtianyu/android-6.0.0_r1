LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)
include $(CLEAR_TBLGEN_VARS)

TBLGEN_TABLES := \
  DiagnosticCommonKinds.inc \
  DeclNodes.inc \
  StmtNodes.inc

lldb_PluginProcessPOSIX_SRC_FILES := \
  POSIXStopInfo.cpp \
  POSIXThread.cpp \
  ProcessMessage.cpp \
  ProcessPOSIX.cpp \
  ProcessPOSIXLog.cpp \
  RegisterContextFreeBSD_x86_64.cpp \
  RegisterContext_i386.cpp \
  RegisterContextLinux_x86_64.cpp \
  RegisterContext_x86_64.cpp

LOCAL_SRC_FILES := $(lldb_PluginProcessPOSIX_SRC_FILES)

LOCAL_MODULE:= liblldbPluginProcessPOSIX
LOCAL_MODULE_TAGS := optional

include $(LLDB_BUILD_MK)
include $(CLANG_VERSION_INC_MK)
include $(CLANG_TBLGEN_RULES_MK)

# =====
# tweak local include paths not present in $(LLDB_BUILD_MK)
# =====
LOCAL_C_INCLUDES += $(LLDB_ROOT_PATH)/source/Plugins/Utility

ifeq ($(HOST_OS),linux)
LOCAL_C_INCLUDES += $(LLDB_ROOT_PATH)/source/Plugins/Process/Linux
LOCAL_CFLAGS += -Wno-extended-offsetof
endif

ifneq (,$(filter $(HOST_OS), freebsd))
LOCAL_C_INCLUDES += $(LLDB_ROOT_PATH)/source/Plugins/Process/FreeBSD
endif
# =====

include $(BUILD_HOST_STATIC_LIBRARY)
