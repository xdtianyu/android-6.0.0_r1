# Output variables:
# libvpx_config_dir_mips
# libvpx_codec_srcs_c_mips
# libvpx_codec_srcs_asm_mips

ifneq ($(ARCH_HAS_BIGENDIAN),true)
  ifeq ($(ARCH_MIPS_DSP_REV),2)
    libvpx_target := mips-dspr2
  else
    libvpx_target := mips
  endif
else
  libvpx_target := generic
endif

libvpx_config_dir_mips := $(LOCAL_PATH)/$(libvpx_target)
libvpx_codec_srcs := $(sort $(shell cat $(libvpx_config_dir_mips)/libvpx_srcs.txt))

# vpx_config.c is an auto-generated file in $(libvpx_target).
libvpx_codec_srcs_c_mips := $(addprefix libvpx/, $(filter-out vpx_config.c, \
    $(filter %.c, $(libvpx_codec_srcs)))) \
    $(libvpx_target)/vpx_config.c

libvpx_codec_srcs_asm_mips := $(filter %.asm.s, $(libvpx_codec_srcs))
