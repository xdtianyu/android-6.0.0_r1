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

LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_JAVA_LIBRARIES := compatibility-common-util-hostsidelib_v2

LOCAL_STATIC_JAVA_LIBRARIES := vogarexpectlib

LOCAL_JAR_MANIFEST := MANIFEST.mf

LOCAL_CLASSPATH := $(HOST_JDK_TOOLS_JAR)

LOCAL_MODULE := compatibility-xml-plan-generator_v2

LOCAL_MODULE_TAGS := optional

include $(BUILD_HOST_JAVA_LIBRARY)

###############################################################################
# Build the tests
###############################################################################

include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(call all-java-files-under, tests/src)

LOCAL_JAVA_LIBRARIES := compatibility-tradefed_v2 compatibility-xml-plan-generator_v2 junit

LOCAL_MODULE := compatibility-xml-plan-generator-tests_v2

LOCAL_MODULE_TAGS := optional

include $(BUILD_HOST_JAVA_LIBRARY)
