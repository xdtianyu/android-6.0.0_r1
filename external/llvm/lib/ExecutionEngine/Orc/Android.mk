LOCAL_PATH:= $(call my-dir)

# For the host
# =====================================================
include $(CLEAR_VARS)

LOCAL_SRC_FILES := \
  CloneSubModule.cpp \
  ExecutionUtils.cpp \
  IndirectionUtils.cpp \
  OrcMCJITReplacement.cpp \
  OrcTargetSupport.cpp

LOCAL_MODULE:= libLLVMOrcJIT

LOCAL_MODULE_TAGS := optional

include $(LLVM_HOST_BUILD_MK)
include $(BUILD_HOST_STATIC_LIBRARY)
