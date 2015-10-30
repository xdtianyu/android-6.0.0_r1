# Don't build LLDB unless we explicitly ask for it.
# This guard will be removed once lldb is working
# against Android devices.
ifeq (true,$(ANDROID_BUILD_LLDB))

LOCAL_PATH := $(call my-dir)
LLDB_ROOT_PATH := $(LOCAL_PATH)

include $(CLEAR_VARS)

subdirs := $(addprefix $(LOCAL_PATH)/,$(addsuffix /Android.mk, \
  source \
  source/API \
  source/Breakpoint \
  source/Commands \
  source/Core \
  source/DataFormatters \
  source/Expression \
  source/Host \
  source/Interpreter \
  source/Plugins \
  source/Symbol \
  source/Target \
  source/Utility \
  tools/driver \
  ))

include $(LOCAL_PATH)/lldb.mk
include $(LOCAL_PATH)/host_shared_lldb.mk

include $(subdirs)

endif # don't build LLDB unless forced to
