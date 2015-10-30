LOCAL_PATH:= $(call my-dir)

# For the host only
# =====================================================
include $(CLEAR_VARS)

LOCAL_MODULE := lldb

LOCAL_MODULE_CLASS := EXECUTABLES

LOCAL_SRC_FILES := \
  Driver.cpp \
  IOChannel.cpp

LOCAL_SHARED_LIBRARIES := \
  liblldb \
  libclang \
  libLLVM

LOCAL_LDLIBS += -lm
ifdef USE_MINGW
LOCAL_LDLIBS += -limagehlp
else
LOCAL_LDLIBS += -lpthread -ldl -ltermcap
endif

include $(LLDB_BUILD_MK)

LIBEDIT_BASE_DIR := $(LLDB_ROOT_PATH)/../../prebuilts/libs/libedit
LOCAL_LDLIBS += $(LIBEDIT_BASE_DIR)/$(HOST_OS)-$(HOST_ARCH)/lib/libedit.a
LOCAL_C_INCLUDES += $(LIBEDIT_BASE_DIR)/include

include $(BUILD_HOST_EXECUTABLE)
