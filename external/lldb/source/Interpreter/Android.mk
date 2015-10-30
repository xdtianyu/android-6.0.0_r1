LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)
include $(CLEAR_TBLGEN_VARS)

TBLGEN_TABLES := \
  CommentCommandList.inc \
  DiagnosticCommonKinds.inc \
  DeclNodes.inc \
  StmtNodes.inc

LOCAL_SRC_FILES := \
  Args.cpp \
  CommandHistory.cpp \
  CommandInterpreter.cpp \
  CommandObject.cpp \
  CommandObjectRegexCommand.cpp \
  CommandObjectScript.cpp \
  CommandReturnObject.cpp \
  OptionGroupArchitecture.cpp \
  OptionGroupBoolean.cpp \
  OptionGroupFile.cpp \
  OptionGroupFormat.cpp \
  OptionGroupOutputFile.cpp \
  OptionGroupPlatform.cpp \
  OptionGroupString.cpp \
  OptionGroupUInt64.cpp \
  OptionGroupUUID.cpp \
  OptionGroupValueObjectDisplay.cpp \
  OptionGroupVariable.cpp \
  OptionGroupWatchpoint.cpp \
  Options.cpp \
  OptionValueArch.cpp \
  OptionValueArgs.cpp \
  OptionValueArray.cpp \
  OptionValueBoolean.cpp \
  OptionValue.cpp \
  OptionValueDictionary.cpp \
  OptionValueEnumeration.cpp \
  OptionValueFileSpec.cpp \
  OptionValueFileSpecLIst.cpp \
  OptionValueFormat.cpp \
  OptionValuePathMappings.cpp \
  OptionValueProperties.cpp \
  OptionValueRegex.cpp \
  OptionValueSInt64.cpp \
  OptionValueString.cpp \
  OptionValueUInt64.cpp \
  OptionValueUUID.cpp \
  Property.cpp \
  PythonDataObjects.cpp \
  ScriptInterpreter.cpp \
  ScriptInterpreterNone.cpp \
  ScriptInterpreterPython.cpp

LOCAL_MODULE := liblldbInterpreter
LOCAL_MODULE_TAGS := optional

include $(LLDB_BUILD_MK)
include $(CLANG_VERSION_INC_MK)
include $(CLANG_TBLGEN_RULES_MK)

WRAP_PYTHON_MK := $(LOCAL_PATH)/wrap_python.mk
include $(WRAP_PYTHON_MK)

# SWIG binding generates a bunch of these, so squelch
# them.
LOCAL_CPPFLAGS := \
	-Wno-cast-qual \
	-Wno-format \
	-Wno-unused-but-set-variable \
	$(LOCAL_CPPFLAGS)

include $(BUILD_HOST_STATIC_LIBRARY)
