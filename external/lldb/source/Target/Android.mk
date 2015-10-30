LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)
include $(CLEAR_TBLGEN_VARS)

TBLGEN_TABLES := \
  DiagnosticCommonKinds.inc \
  DeclNodes.inc \
  StmtNodes.inc

lldb_Target_SRC_FILES := \
  ABI.cpp \
  CPPLanguageRuntime.cpp \
  ExecutionContext.cpp \
  LanguageRuntime.cpp \
  Memory.cpp \
  ObjCLanguageRuntime.cpp \
  OperatingSystem.cpp \
  PathMappingList.cpp \
  Platform.cpp \
  Process.cpp \
  RegisterContext.cpp \
  SectionLoadList.cpp \
  StackFrame.cpp \
  StackFrameList.cpp \
  StackID.cpp \
  StopInfo.cpp \
  Target.cpp \
  TargetList.cpp \
  Thread.cpp \
  ThreadList.cpp \
  ThreadPlanBase.cpp \
  ThreadPlanCallFunction.cpp \
  ThreadPlanCallUserExpression.cpp \
  ThreadPlan.cpp \
  ThreadPlanRunToAddress.cpp \
  ThreadPlanShouldStopHere.cpp \
  ThreadPlanStepInRange.cpp \
  ThreadPlanStepInstruction.cpp \
  ThreadPlanStepOut.cpp \
  ThreadPlanStepOverBreakpoint.cpp \
  ThreadPlanStepOverRange.cpp \
  ThreadPlanStepRange.cpp \
  ThreadPlanStepThrough.cpp \
  ThreadPlanStepUntil.cpp \
  ThreadPlanTracer.cpp \
  ThreadSpec.cpp \
  UnixSignals.cpp \
  UnwindAssembly.cpp

LOCAL_SRC_FILES := $(lldb_Target_SRC_FILES)

LOCAL_MODULE:= liblldbTarget
LOCAL_MODULE_TAGS := optional

include $(LLDB_BUILD_MK)
include $(CLANG_VERSION_INC_MK)
include $(CLANG_TBLGEN_RULES_MK)
include $(BUILD_HOST_STATIC_LIBRARY)
