ifeq ($(LLDB_ROOT_PATH),)
$(error Must set variable LLDB_ROOT_PATH before including this! $(LOCAL_PATH))
endif

LLDB_BUILD_MK := $(LLDB_ROOT_PATH)/lldb-build.mk
