# Copyright (C) 2014 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

###############################################################################
# Builds the compatibility tradefed host library
###############################################################################

LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(call all-java-files-under, src)

#LOCAL_JAVA_RESOURCE_DIRS := res

LOCAL_MODULE := compatibility-tradefed_v2

LOCAL_MODULE_TAGS := optional

LOCAL_JAVA_LIBRARIES := tradefed-prebuilt hosttestlib compatibility-common-util-hostsidelib_v2

include $(BUILD_HOST_JAVA_LIBRARY)

###############################################################################
# Build the compatibility tradefed tests
###############################################################################

include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(call all-java-files-under, tests/src)

LOCAL_MODULE := compatibility-tradefed-tests_v2

LOCAL_MODULE_TAGS := optional

LOCAL_JAVA_LIBRARIES := tradefed-prebuilt compatibility-tradefed_v2 junit

LOCAL_STATIC_JAVA_LIBRARIES := easymock

include $(BUILD_HOST_JAVA_LIBRARY)
