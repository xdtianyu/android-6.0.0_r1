# Don't build the library unless forced to.
ifeq (true,$(FORCE_BUILD_LLVM_COMPONENTS))
# Don't build the library in unbundled branches.
ifeq (,$(TARGET_BUILD_APPS))

LOCAL_PATH:= $(call my-dir)

clang_whole_static_libraries := \
	libclangAnalysis \
	libclangAST \
	libclangASTMatchers \
	libclangBasic \
	libclangCodeGen \
	libclangDriver \
	libclangEdit \
	libclangFormat \
	libclangFrontend \
	libclangIndex \
	libclangLex \
	libclangLibclang \
	libclangParse \
	libclangRewrite \
	libclangRewriteFrontend \
	libclangSema \
	libclangSerialization \
	libclangTooling

# host
include $(CLEAR_VARS)

LOCAL_IS_HOST_MODULE := true
LOCAL_MODULE:= libclang
LOCAL_MODULE_TAGS := optional
LOCAL_WHOLE_STATIC_LIBRARIES := $(clang_whole_static_libraries)

ifeq ($(HOST_OS),windows)
  LOCAL_SHARED_LIBRARIES := libLLVM
  LOCAL_LDLIBS := -limagehlp -lpsapi
else
  LOCAL_SHARED_LIBRARIES := libLLVM libc++
  LOCAL_LDLIBS := -ldl -lpthread
endif

include $(CLANG_HOST_BUILD_MK)
include $(BUILD_HOST_SHARED_LIBRARY)

# device
include $(CLEAR_VARS)

LOCAL_MODULE:= libclang
LOCAL_MODULE_TAGS := optional
LOCAL_WHOLE_STATIC_LIBRARIES := $(clang_whole_static_libraries)

LOCAL_SHARED_LIBRARIES := libLLVM libc++
LOCAL_LDLIBS := -ldl

include $(CLANG_DEVICE_BUILD_MK)
include $(BUILD_SHARED_LIBRARY)

endif # don't build in unbundled branches
endif # don't build unless forced to
