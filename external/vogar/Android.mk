# Copyright (C) 2014 The Android Open Source Project
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

# build vogar jar
# ============================================================

include $(CLEAR_VARS)

LOCAL_MODULE := vogar.jar
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_CLASS := JAVA_LIBRARIES
intermediates := $(call local-intermediates-dir,COMMON)
LOCAL_SRC_FILES := $(call all-java-files-under, src/)
LOCAL_JAVA_RESOURCE_DIRS := resources

LOCAL_STATIC_JAVA_LIBRARIES := \
  vogar-caliper \
  vogar-gson-1.7.1 \
  vogar-guava \
  vogar-jsr305 \
  vogar-kxml-libcore-20110123 \
  vogar-miniguice \
  vogar-mockito-all-1.8.5 \

# Vogar uses android.jar.
LOCAL_SDK_VERSION := 9

# This is really a host java library which pretends to be a target
# java library to pull in the Android SDK. We don't want to use jack
# because jack doesn't produce jar files for STATIC_JAVA_LIBRARIES,
# and produces its own intermediate representation instead.
LOCAL_JACK_ENABLED := disabled
include $(BUILD_STATIC_JAVA_LIBRARY)

# Build dependencies.
# ============================================================
include $(CLEAR_VARS)

LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := \
    vogar-caliper:lib/caliper$(COMMON_JAVA_PACKAGE_SUFFIX) \
    vogar-gson-1.7.1:lib/gson-1.7.1$(COMMON_JAVA_PACKAGE_SUFFIX) \
    vogar-guava:lib/guava$(COMMON_JAVA_PACKAGE_SUFFIX) \
    vogar-jsr305:lib/jsr305$(COMMON_JAVA_PACKAGE_SUFFIX) \
    vogar-kxml-libcore-20110123:lib/kxml-libcore-20110123$(COMMON_JAVA_PACKAGE_SUFFIX) \
    vogar-miniguice:lib/miniguice$(COMMON_JAVA_PACKAGE_SUFFIX) \
    vogar-mockito-all-1.8.5:lib/mockito-all-1.8.5$(COMMON_JAVA_PACKAGE_SUFFIX) \

LOCAL_MODULE_TAGS := optional

include $(BUILD_MULTI_PREBUILT)

# copy vogar script
# ============================================================
include $(CLEAR_VARS)
LOCAL_IS_HOST_MODULE := true
LOCAL_MODULE_CLASS := EXECUTABLES
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE := vogar

include $(BUILD_SYSTEM)/base_rules.mk

$(LOCAL_BUILT_MODULE): $(LOCAL_PATH)/bin/vogar-android vogar.jar | $(ACP)
	@echo "Copy: $(PRIVATE_MODULE) ($@)"
	$(copy-file-to-new-target)
	$(hide) chmod 755 $@
