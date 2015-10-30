# clang populates much of what we need
LOCAL_CFLAGS := \
	-fvisibility-inlines-hidden \
	-Wno-missing-field-initializers \
	-Wno-sequence-point \
	-Wno-sign-compare \
	-Wno-uninitialized \
	-Wno-unused-function \
	-Wno-unused-variable \
	$(LOCAL_CFLAGS)

# TODO change this when clang or gcc > 2.6 support is added
CPLUSPLUS_STANDARD := -std=c++0x

LOCAL_CPPFLAGS := \
	$(CPLUSPLUS_STANDARD) \
	$(LOCAL_CPPFLAGS)

PYTHON_BASE_PATH := prebuilts/python/linux-x86/2.7.5
PYTHON_INCLUDE_PATH := $(PYTHON_BASE_PATH)/include/python2.7

LOCAL_C_INCLUDES := \
	$(PYTHON_INCLUDE_PATH) \
	$(LLDB_ROOT_PATH)/include \
	$(LLDB_ROOT_PATH)/source \
	$(LLDB_ROOT_PATH)/source/Utility \
	$(LLDB_ROOT_PATH)/source/Plugins/Process/Utility \
	$(LLDB_ROOT_PATH)/source/Plugins/Process/POSIX \
	$(LOCAL_C_INCLUDES)

LLVM_ROOT_PATH := external/llvm
include $(LLVM_ROOT_PATH)/llvm.mk

CLANG_ROOT_PATH := external/clang
include $(CLANG_ROOT_PATH)/clang.mk

ifneq ($(LLVM_HOST_BUILD_MK),)
include $(LLVM_HOST_BUILD_MK)
endif

ifneq ($(CLANG_HOST_BUILD_MK),)
include $(CLANG_HOST_BUILD_MK)
endif

# strip out flags from clang/llvm that we know we don't handle
LOCAL_CFLAGS := $(subst -pedantic,,$(LOCAL_CFLAGS))
LOCAL_CPPFLAGS := $(subst -pedantic,,$(LOCAL_CPPFLAGS))
