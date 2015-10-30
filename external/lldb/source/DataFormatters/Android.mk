LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)
include $(CLEAR_TBLGEN_VARS)

TBLGEN_TABLES := \
  CommentCommandList.inc \
  DiagnosticCommonKinds.inc \
  DeclNodes.inc \
  StmtNodes.inc

lldb_DataFormatters_SRC_FILES := \
  CF.cpp \
  Cocoa.cpp \
  CXXFormatterFunctions.cpp \
  DataVisualization.cpp \
  FormatCache.cpp \
  FormatClasses.cpp \
  FormatManager.cpp \
  LibCxx.cpp \
  LibCxxList.cpp \
  LibCxxMap.cpp \
  LibStdcpp.cpp \
  NSArray.cpp \
  NSDictionary.cpp \
  NSSet.cpp \
  TypeCategory.cpp \
  TypeCategoryMap.cpp \
  TypeFormat.cpp \
  TypeSummary.cpp \
  TypeSynthetic.cpp

LOCAL_SRC_FILES := $(lldb_DataFormatters_SRC_FILES)

LOCAL_MODULE:= liblldbDataFormatters
LOCAL_MODULE_TAGS := optional

include $(LLDB_BUILD_MK)
include $(CLANG_VERSION_INC_MK)
include $(CLANG_TBLGEN_RULES_MK)
include $(BUILD_HOST_STATIC_LIBRARY)
