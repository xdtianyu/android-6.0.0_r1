ifneq ($(filter msm8960 msm8x27 msm8974 msm8226,$(TARGET_BOARD_PLATFORM)),)
include $(call all-named-subdir-makefiles,msm8960)
else
ifneq ($(filter msm8994 msm8992,$(TARGET_BOARD_PLATFORM)),)
include $(call all-named-subdir-makefiles,msm8992)
endif
endif
