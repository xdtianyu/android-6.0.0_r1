LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)
include $(CLEAR_TBLGEN_VARS)

TBLGEN_TABLES := \
  DiagnosticCommonKinds.inc \
  DeclNodes.inc \
  StmtNodes.inc

lldb_Breakpoint_SRC_FILES := \
  Breakpoint.cpp \
  BreakpointID.cpp \
  BreakpointIDList.cpp \
  BreakpointList.cpp \
  BreakpointLocationCollection.cpp \
  BreakpointLocation.cpp \
  BreakpointLocationList.cpp \
  BreakpointOptions.cpp \
  BreakpointResolverAddress.cpp \
  BreakpointResolver.cpp \
  BreakpointResolverFileLine.cpp \
  BreakpointResolverFileRegex.cpp \
  BreakpointResolverName.cpp \
  BreakpointSite.cpp \
  BreakpointSiteList.cpp \
  StoppointCallbackContext.cpp \
  Stoppoint.cpp \
  StoppointLocation.cpp \
  Watchpoint.cpp \
  WatchpointList.cpp \
  WatchpointOptions.cpp

LOCAL_SRC_FILES := $(lldb_Breakpoint_SRC_FILES)

LOCAL_MODULE:= liblldbBreakpoint
LOCAL_MODULE_TAGS := optional

include $(LLDB_BUILD_MK)
include $(CLANG_VERSION_INC_MK)
include $(CLANG_TBLGEN_RULES_MK)
include $(BUILD_HOST_STATIC_LIBRARY)
