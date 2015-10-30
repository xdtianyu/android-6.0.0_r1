# Don't build the library unless forced to.
ifeq (true,$(ANDROID_BUILD_LLDB))
# Don't build the library in unbundled branches.
ifeq (,$(TARGET_BUILD_APPS))

LOCAL_PATH:= $(call my-dir)

LOCAL_IS_HOST_MODULE := true

LOCAL_MODULE:= liblldb

LOCAL_MODULE_TAGS := optional

LOCAL_WHOLE_STATIC_LIBRARIES := \
	liblldbInitAndLog \
	liblldbAPI \
	liblldbBreakpoint \
	liblldbCommands \
	liblldbCore \
	liblldbDataFormatters \
	liblldbExpression \
	liblldbHostCommon \
	liblldbHostLinux \
	liblldbInterpreter \
	liblldbPluginABIMacOSX_arm \
	liblldbPluginABIMacOSX_i386 \
	liblldbPluginABISysV_x86_64 \
	liblldbPluginDisassemblerLLVM \
	liblldbPluginDynamicLoaderMacOSX \
	liblldbPluginDynamicLoaderPOSIX \
	liblldbPluginDynamicLoaderStatic \
	liblldbPluginEmulateInstructionARM \
	liblldbPluginLanguageRuntimeCPlusPlusItaniumABI \
	liblldbPluginLanguageRuntimeObjCAppleObjCRuntime \
	liblldbPluginObjectContainerBSDArchive \
	liblldbPluginObjectFileELF \
	liblldbPluginObjectFilePECOFF \
	liblldbPluginOperatingSystemPython \
	liblldbPluginPlatformFreeBSD \
	liblldbPluginPlatformGDBServer \
	liblldbPluginPlatformLinux \
	liblldbPluginPlatformMacOSX \
	liblldbPluginProcessElfCore \
	liblldbPluginProcessGDBRemote \
	liblldbPluginProcessLinux \
	liblldbPluginProcessPOSIX \
	liblldbPluginSymbolFileDWARF \
	liblldbPluginSymbolFileSymtab \
	liblldbPluginSymbolVendorELF \
	liblldbPluginUnwindAssemblyInstEmulation \
	liblldbPluginUnwindAssemblyx86 \
	liblldbPluginUtility \
	liblldbSymbol \
	liblldbTarget \
	liblldbUtility

LOCAL_SHARED_LIBRARIES := \
	libLLVM \
	libclang

ifeq ($(HOST_OS),windows)
  LOCAL_LDLIBS := -limagehlp -lpsapi
else
  LOCAL_LDLIBS := \
	-ldl \
	-lm \
	-lpthread \
	-lrt \
	-lutil \
	-lz
endif

PYTHON_BASE_PATH := prebuilts/python/linux-x86/2.7.5
LOCAL_LDLIBS += $(PYTHON_BASE_PATH)/lib/libpython2.7.a

include $(LLDB_BUILD_MK)
include $(BUILD_HOST_SHARED_LIBRARY)

endif # don't build in unbundled branches
endif # don't build unless forced to
