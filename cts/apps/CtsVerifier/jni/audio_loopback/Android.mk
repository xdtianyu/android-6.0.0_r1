LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE      := libaudioloopback_jni
LOCAL_MODULE_TAGS := optional
LOCAL_SRC_FILES   := \
	sles.cpp \
	jni_sles.c

LOCAL_C_INCLUDES := \
        system/media/audio_utils/include \
        frameworks/wilhelm/include

LOCAL_SHARED_LIBRARIES := \
	libutils \
	libcutils \
	libOpenSLES \
	libnbaio \
	liblog \
	libaudioutils

LOCAL_PRELINK_MODULE := false

LOCAL_LDFLAGS := -Wl,--hash-style=sysv
LOCAL_CFLAGS := -DSTDC_HEADERS

include $(BUILD_SHARED_LIBRARY)
