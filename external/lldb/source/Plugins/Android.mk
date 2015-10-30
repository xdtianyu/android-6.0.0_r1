LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

dirs := \
  ABI/MacOSX-arm \
  ABI/MacOSX-i386 \
  ABI/SysV-x86_64 \
  Disassembler/llvm \
  DynamicLoader/POSIX-DYLD \
  DynamicLoader/Static \
  Instruction/ARM \
  LanguageRuntime/CPlusPlus/ItaniumABI \
  LanguageRuntime/ObjC/AppleObjCRuntime \
  ObjectContainer/BSD-Archive \
  ObjectFile/ELF \
  ObjectFile/PECOFF \
  OperatingSystem/Python \
  Platform/gdb-server \
  Platform/MacOSX \
  Platform/Linux \
  Platform/FreeBSD \
  Process/gdb-remote \
  Process/Utility \
  SymbolFile/DWARF \
  SymbolFile/Symtab \
  UnwindAssembly/InstEmulation \
  UnwindAssembly/x86

ifeq ($(HOST_OS),darwin)
dirs += \
  DynamicLoader/Darwin-Kernel \
  DynamicLoader/MacOSX-DYLD \
  ObjectContainer/Universal-Mach-O \
  ObjectFile/Mach-O \
  Process/mach-core \
  Process/MacOSX-Kernel \
  SymbolVendor/MacOSX
endif

ifeq ($(HOST_OS),linux)
dirs += \
  DynamicLoader/MacOSX-DYLD \
  Process/elf-core \
  Process/Linux \
  Process/POSIX \
  SymbolVendor/ELF
endif

ifneq (,$(filter $(HOST_OS), freebsd))
dirs += \
  Process/elf-core \
  Process/FreeBSD \
  Process/POSIX \
  SymbolVendor/ELF
endif

subdirs := $(addprefix $(LOCAL_PATH)/,$(addsuffix /Android.mk, $(dirs)))

include $(subdirs)
