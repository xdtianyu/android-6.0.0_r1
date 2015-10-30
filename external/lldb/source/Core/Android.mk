LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)
include $(CLEAR_TBLGEN_VARS)

TBLGEN_TABLES := \
  CommentCommandList.inc \
  DiagnosticCommonKinds.inc \
  DeclNodes.inc \
  StmtNodes.inc

lldb_Core_SRC_FILES := \
  Address.cpp \
  AddressRange.cpp \
  AddressResolver.cpp \
  AddressResolverFileLine.cpp \
  AddressResolverName.cpp \
  ArchSpec.cpp \
  Baton.cpp \
  Broadcaster.cpp \
  Communication.cpp \
  Connection.cpp \
  ConnectionFileDescriptor.cpp \
  ConnectionMachPort.cpp \
  ConnectionSharedMemory.cpp \
  ConstString.cpp \
  DataBufferHeap.cpp \
  DataBufferMemoryMap.cpp \
  DataEncoder.cpp \
  DataExtractor.cpp \
  Debugger.cpp \
  Disassembler.cpp \
  DynamicLoader.cpp \
  EmulateInstruction.cpp \
  Error.cpp \
  Event.cpp \
  FileLineResolver.cpp \
  FileSpecList.cpp \
  History.cpp \
  InputReader.cpp \
  InputReaderEZ.cpp \
  InputReaderStack.cpp \
  Language.cpp \
  Listener.cpp \
  Log.cpp \
  Mangled.cpp \
  ModuleChild.cpp \
  Module.cpp \
  ModuleList.cpp \
  Opcode.cpp \
  PluginManager.cpp \
  RegisterValue.cpp \
  RegularExpression.cpp \
  Scalar.cpp \
  SearchFilter.cpp \
  Section.cpp \
  SourceManager.cpp \
  State.cpp \
  StreamAsynchronousIO.cpp \
  StreamCallback.cpp \
  Stream.cpp \
  StreamFile.cpp \
  StreamString.cpp \
  StringList.cpp \
  Timer.cpp \
  UserID.cpp \
  UserSettingsController.cpp \
  UUID.cpp \
  Value.cpp \
  ValueObjectCast.cpp \
  ValueObjectChild.cpp \
  ValueObjectConstResultChild.cpp \
  ValueObjectConstResult.cpp \
  ValueObjectConstResultImpl.cpp \
  ValueObject.cpp \
  ValueObjectDynamicValue.cpp \
  ValueObjectList.cpp \
  ValueObjectMemory.cpp \
  ValueObjectRegister.cpp \
  ValueObjectSyntheticFilter.cpp \
  ValueObjectVariable.cpp \
  VMRange.cpp

LOCAL_SRC_FILES := $(lldb_Core_SRC_FILES)

LOCAL_MODULE:= liblldbCore
LOCAL_MODULE_TAGS := optional

include $(LLDB_BUILD_MK)
include $(CLANG_VERSION_INC_MK)
include $(CLANG_TBLGEN_RULES_MK)
include $(BUILD_HOST_STATIC_LIBRARY)
