LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)
include $(CLEAR_TBLGEN_VARS)

TBLGEN_TABLES := \
  AttrParsedAttrList.inc \
  CommentCommandList.inc \
  DiagnosticCommonKinds.inc \
  DeclNodes.inc \
  StmtNodes.inc

LOCAL_SRC_FILES := \
  DWARFAbbreviationDeclaration.cpp \
  DWARFCompileUnit.cpp \
  DWARFDebugAbbrev.cpp \
  DWARFDebugAranges.cpp \
  DWARFDebugArangeSet.cpp \
  DWARFDebugInfo.cpp \
  DWARFDebugInfoEntry.cpp \
  DWARFDebugLine.cpp \
  DWARFDebugMacinfo.cpp \
  DWARFDebugMacinfoEntry.cpp \
  DWARFDebugPubnames.cpp \
  DWARFDebugPubnamesSet.cpp \
  DWARFDebugRanges.cpp \
  DWARFDeclContext.cpp \
  DWARFDefines.cpp \
  DWARFDIECollection.cpp \
  DWARFFormValue.cpp \
  DWARFLocationDescription.cpp \
  DWARFLocationList.cpp \
  LogChannelDWARF.cpp \
  NameToDIE.cpp \
  SymbolFileDWARF.cpp \
  SymbolFileDWARFDebugMap.cpp \
  UniqueDWARFASTType.cpp

LOCAL_MODULE := liblldbPluginSymbolFileDWARF
LOCAL_MODULE_TAGS := optional

include $(LLDB_BUILD_MK)
include $(CLANG_VERSION_INC_MK)
include $(CLANG_TBLGEN_RULES_MK)
include $(BUILD_HOST_STATIC_LIBRARY)
