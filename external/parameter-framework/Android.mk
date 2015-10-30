# Recursive call sub-folder Android.mk
#
ifeq ($(HOST_OS),linux)
include $(call all-subdir-makefiles)
endif
