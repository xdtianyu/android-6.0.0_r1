# Build the unit tests.

LOCAL_PATH:= $(call my-dir)

test_executable := NativeMediaTest_SL
list_executable := $(test_executable)_list

include $(CLEAR_VARS)
LOCAL_ADDITIONAL_DEPENDENCIES := $(LOCAL_PATH)/Android.mk

LOCAL_MODULE := $(test_executable)
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_PATH := $(TARGET_OUT_DATA)/nativetest
LOCAL_MULTILIB := both
LOCAL_MODULE_STEM_32 := $(LOCAL_MODULE)32
LOCAL_MODULE_STEM_64 := $(LOCAL_MODULE)64

LOCAL_C_INCLUDES := \
    external/gtest/include \
    $(call include-path-for, wilhelm) \
    $(call include-path-for, wilhelm-ut)

LOCAL_SRC_FILES := \
    src/SLObjectCreationTest.cpp

LOCAL_SHARED_LIBRARIES := \
    libutils \
    liblog \
    libOpenSLES \

LOCAL_STATIC_LIBRARIES := \
    libOpenSLESUT \
    libgtest

LOCAL_CTS_TEST_PACKAGE := android.nativemedia.sl
include $(BUILD_CTS_EXECUTABLE)

include $(CLEAR_VARS)
LOCAL_ADDITIONAL_DEPENDENCIES := $(LOCAL_PATH)/Android.mk

LOCAL_MODULE := $(list_executable)
LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := \
    src/SLObjectCreationTest.cpp

LOCAL_CFLAGS := \
    -DBUILD_ONLY \

LOCAL_SHARED_LIBRARIES := \
    liblog \

include $(BUILD_HOST_NATIVE_TEST)
