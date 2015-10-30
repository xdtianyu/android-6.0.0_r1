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
# Build the common utility library for use device-side
###############################################################################

LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_MODULE_TAGS := optional

LOCAL_MODULE := compatibility-common-util-devicesidelib_v2

LOCAL_SDK_VERSION := current

include $(BUILD_STATIC_JAVA_LIBRARY)

###############################################################################
# Build the common utility library for use host-side
###############################################################################

include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_MODULE_TAGS := optional

LOCAL_MODULE := compatibility-common-util-hostsidelib_v2

LOCAL_STATIC_JAVA_LIBRARIES := kxml2-2.3.0

include $(BUILD_HOST_JAVA_LIBRARY)

###############################################################################
# Build the tests
###############################################################################

include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(call all-java-files-under, tests/src)

LOCAL_STATIC_JAVA_LIBRARIES := \
                        junit \
                        kxml2-2.3.0 \
                        compatibility-common-util-hostsidelib_v2

LOCAL_MODULE := compatibility-common-util-tests_v2

LOCAL_MODULE_TAGS := optional

include $(BUILD_HOST_JAVA_LIBRARY)
