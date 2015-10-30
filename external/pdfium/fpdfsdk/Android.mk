LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := libpdfium

LOCAL_ARM_MODE := arm
LOCAL_NDK_STL_VARIANT := gnustl_static

LOCAL_CFLAGS += -O3 -fstrict-aliasing -fprefetch-loop-arrays -fexceptions
LOCAL_CFLAGS += -Wno-non-virtual-dtor -Wall
LOCAL_CFLAGS += -DFOXIT_CHROME_BUILD

LOCAL_STATIC_LIBRARIES := libpdfiumcore

# TODO: figure out why turning on exceptions requires manually linking libdl
LOCAL_SHARED_LIBRARIES := libdl libft2

LOCAL_SRC_FILES := \
    src/fpdfdoc.cpp \
    src/fpdfeditimg.cpp \
    src/fpdfeditpage.cpp \
    src/fpdfppo.cpp \
    src/fpdfsave.cpp \
    src/fpdftext.cpp \
    src/fpdfview.cpp \
    src/fpdf_dataavail.cpp \
    src/fpdf_ext.cpp \
    src/fpdf_flatten.cpp \
    src/fpdf_progressive.cpp \
    src/fpdf_searchex.cpp \
    src/fpdf_transformpage.cpp \
    src/fsdk_rendercontext.cpp

LOCAL_C_INCLUDES := \
    external/pdfium/core/include

include $(BUILD_SHARED_LIBRARY)
