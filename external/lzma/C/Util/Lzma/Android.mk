# Copyright 2008 The Android Open Source Project
#
LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE := liblzma
LOCAL_SRC_FILES := LzmaUtil.c ../../Alloc.c ../../LzFind.c ../../LzmaDec.c ../../LzmaEnc.c ../../7zFile.c ../../7zStream.c
LOCAL_CFLAGS := -c -O2 -Wall -D_7ZIP_ST
LOCAL_C_INCLUDES := $(LOCAL_PATH)/../../
include $(BUILD_HOST_STATIC_LIBRARY)
