LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

dirs := common

ifeq ($(HOST_OS),darwin)
dirs += macosx
endif

ifeq ($(HOST_OS),linux)
dirs += linux
endif

ifneq (,$(filter $(HOST_OS), freebsd))
dirs += freebsd
endif

subdirs := $(addprefix $(LOCAL_PATH)/,$(addsuffix /Android.mk, $(dirs)))

include $(subdirs)
