# Copyright (C) 2013 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

LOCAL_PATH := $(call my-dir)

ifeq ($(TARGET_PRODUCT),sdk)
supported_platforms := none
else
supported_platforms := linux
endif

cur_platform := $(filter $(HOST_OS),$(supported_platforms))

ifdef cur_platform

perf_arch := $(TARGET_ARCH)
ifeq ($(TARGET_ARCH), x86_64)
perf_arch := x86
endif

ifeq ($(TARGET_ARCH), mips64)
perf_arch := mips
endif

perf_src_files := \
    perf.c \
    arch/common.c \
    arch/$(perf_arch)/util/dwarf-regs.c \
    bench/mem-memcpy.c \
    bench/mem-memset.c \
    bench/sched-messaging.c \
    bench/sched-pipe.c \
    builtin-annotate.c \
    builtin-bench.c \
    builtin-buildid-cache.c \
    builtin-buildid-list.c \
    builtin-diff.c \
    builtin-evlist.c \
    builtin-help.c \
    builtin-inject.c \
    builtin-kmem.c \
    builtin-kvm.c \
    builtin-list.c \
    builtin-lock.c \
    builtin-mem.c \
    builtin-probe.c \
    builtin-record.c \
    builtin-report.c \
    builtin-sched.c \
    builtin-script.c \
    builtin-stat.c \
    builtin-timechart.c \
    builtin-top.c \
    tests/attr.c \
    tests/bp_signal.c \
    tests/bp_signal_overflow.c \
    tests/builtin-test.c \
    tests/code-reading.c \
    tests/dso-data.c \
    tests/evsel-roundtrip-name.c \
    tests/evsel-tp-sched.c \
    tests/hists_link.c \
    tests/keep-tracking.c \
    tests/mmap-basic.c \
    tests/open-syscall-all-cpus.c \
    tests/open-syscall.c \
    tests/open-syscall-tp-fields.c \
    tests/parse-events.c \
    tests/parse-no-sample-id-all.c \
    tests/perf-record.c \
    tests/pmu.c \
    tests/python-use.c \
    tests/rdpmc.c \
    tests/sample-parsing.c \
    tests/sw-clock.c \
    tests/task-exit.c \
    tests/vmlinux-kallsyms.c \
    ui/helpline.c \
    ui/hist.c \
    ui/progress.c \
    ui/setup.c \
    ui/stdio/hist.c \
    ui/util.c \
    util/abspath.c \
    util/alias.c \
    util/annotate.c \
    util/bitmap.c \
    util/build-id.c \
    util/callchain.c \
    util/cgroup.c \
    util/color.c \
    util/config.c \
    util/cpumap.c \
    util/ctype.c \
    util/debug.c \
    util/dso.c \
    util/dwarf-aux.c \
    util/environment.c \
    util/event.c \
    util/evlist.c \
    util/evsel.c \
    util/exec_cmd.c \
    util/header.c \
    util/help.c \
    util/hist.c \
    util/hweight.c \
    util/intlist.c \
    util/levenshtein.c \
    util/machine.c \
    util/map.c \
    util/pager.c \
    util/parse-events.c \
    util/parse-events-bison.c \
    util/parse-events-flex.c \
    util/parse-options.c \
    util/path.c \
    util/pmu.c \
    util/pmu-bison.c \
    util/pmu-flex.c \
    util/probe-event.c \
    util/probe-finder.c \
    util/quote.c \
    util/rblist.c \
    util/record.c \
    util/run-command.c \
    util/sigchain.c \
    util/session.c \
    util/sort.c \
    util/stat.c \
    util/strbuf.c \
    util/string.c \
    util/strfilter.c \
    util/strlist.c \
    util/svghelper.c \
    util/symbol.c \
    util/symbol-elf.c \
    util/sysfs.c \
    util/target.c \
    util/thread.c \
    util/thread_map.c \
    util/top.c \
    util/trace-event-info.c \
    util/trace-event-parse.c \
    util/trace-event-read.c \
    util/trace-event-scripting.c \
    util/usage.c \
    util/util.c \
    util/values.c \
    util/vdso.c \
    util/wrapper.c \
    util/xyarray.c \
    ../lib/lk/debugfs.c \
    ../lib/traceevent/event-parse.c \
    ../lib/traceevent/parse-utils.c \
    ../lib/traceevent/trace-seq.c \
    ../../lib/rbtree.c

perf_src_files_x86 = \
    arch/x86/util/tsc.c \
    tests/perf-time-to-tsc.c \

common_perf_headers := \
    $(LOCAL_PATH)/../lib \
    $(LOCAL_PATH)/util/include \
    $(LOCAL_PATH)/util \

common_clang_compiler_flags := \
    -Wno-int-conversion \
    -Wno-tautological-pointer-compare \
    -Wno-tautological-constant-out-of-range-compare \
    -Wno-pointer-bool-conversion \

common_compiler_flags := \
    -include external/linux-tools-perf/android-fixes.h \
    -Wno-error \
    -std=gnu99 \
    -Wno-attributes \
    -Wno-implicit-function-declaration \
    -Wno-maybe-uninitialized \
    -Wno-missing-field-initializers \
    -Wno-pointer-arith \
    -Wno-pointer-sign \
    -Wno-return-type \
    -Wno-sign-compare \
    -Wno-unused-parameter \

common_predefined_macros := \
    -D_GNU_SOURCE \
    -DDWARF_SUPPORT \
    -DPYTHON='""' \
    -DPYTHONPATH='""' \
    -DBINDIR='""' \
    -DETC_PERFCONFIG='""' \
    -DPREFIX='""' \
    -DPERF_EXEC_PATH='""' \
    -DPERF_HTML_PATH='""' \
    -DPERF_MAN_PATH='""' \
    -DPERF_INFO_PATH='""' \
    -DPERF_VERSION='"perf.3.12_android"' \
    -DHAVE_ELF_GETPHDRNUM \
    -DHAVE_CPLUS_DEMANGLE \
    -DHAVE_STRLCPY \
    -DLIBELF_SUPPORT \
    -DLIBELF_MMAP \
    -DNO_NEWT_SUPPORT \
    -DNO_LIBPERL \
    -DNO_LIBPYTHON \
    -DNO_GTK2 \
    -DNO_LIBNUMA \
    -DNO_LIBAUDIT \

include $(CLEAR_VARS)
ifeq ($(TARGET_ARCH),arm)
# b/17167262, builtin-report.c and builtin-top.c have undefined __aeabi_read_tp
# when compiled with clang -fpie.
LOCAL_CLANG := false
endif

LOCAL_SRC_FILES := $(perf_src_files)
LOCAL_SRC_FILES_x86 := $(perf_src_files_x86)
LOCAL_SRC_FILES_x86_64 := $(perf_src_files_x86)

# TODO: this is only needed because of libebl below, which seems like a mistake on the target.
LOCAL_SHARED_LIBRARIES := libdl

# TODO: there's probably more stuff here than is strictly necessary on the target.
LOCAL_STATIC_LIBRARIES := \
    libdwfl \
    libdw \
    libdwelf \
    libebl \
    libelf \
    libz \

LOCAL_CFLAGS += $(common_predefined_macros)
LOCAL_CFLAGS += $(common_compiler_flags)
LOCAL_CLANG_CFLAGS += $(common_clang_compiler_flags)
LOCAL_C_INCLUDES := $(common_perf_headers) external/elfutils/include/

LOCAL_MODULE := perf
LOCAL_MODULE_TAGS := eng

include $(BUILD_EXECUTABLE)

endif #cur_platform
