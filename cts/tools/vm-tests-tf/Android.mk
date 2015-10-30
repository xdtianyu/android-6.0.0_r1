# Copyright (C) 2011 The Android Open Source Project
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

# test dex library
# ============================================================
include $(CLEAR_VARS)

# custom variables used to generate test description. do not touch!
LOCAL_SRC_FILES := $(call all-java-files-under, src/dot)

LOCAL_MODULE := cts-tf-dalvik-lib
LOCAL_MODULE_CLASS := JAVA_LIBRARIES
LOCAL_MODULE_TAGS := optional
LOCAL_JAVA_LIBRARIES := junit-targetdex

include $(BUILD_JAVA_LIBRARY)

cts-tf-dalvik-lib.jack := $(full_classes_jack)

private_jill_jarjar_asm := $(addprefix $(HOST_OUT_JAVA_LIBRARIES)/,jill-jarjar-asm.jar)
$(private_jill_jarjar_asm) : PRIVATE_JARJAR_RULES := $(LOCAL_PATH)/jill-jarjar-rules.txt
$(private_jill_jarjar_asm) : $(addprefix $(HOST_OUT_JAVA_LIBRARIES)/,jill.jar) | $(JARJAR)
	@echo JarJar: $@
	$(hide) java -jar $(JARJAR) process $(PRIVATE_JARJAR_RULES) $< $@

# buildutil java library
# ============================================================
include $(CLEAR_VARS)

# custom variables used to generate test description. do not touch!
LOCAL_TEST_TYPE := vmHostTest
LOCAL_JAR_PATH := android.core.vm-tests-tf.jar

LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_MODULE := cts-tf-dalvik-buildutil
LOCAL_MODULE_CLASS := JAVA_LIBRARIES
LOCAL_MODULE_TAGS := optional

LOCAL_JAVA_LIBRARIES := dx dasm cfassembler junit
LOCAL_JAVA_LIBRARIES += jack

LOCAL_CLASSPATH := $(HOST_JDK_TOOLS_JAR) $(private_jill_jarjar_asm)

include $(BUILD_HOST_JAVA_LIBRARY)

# Buid android.core.vm-tests-tf.jar
# ============================================================
#
include $(CLEAR_VARS)

LOCAL_JACK_ENABLED := $(strip $(LOCAL_JACK_ENABLED))
intermediates := $(call intermediates-dir-for,JAVA_LIBRARIES,vm-tests-tf,HOST)
vmteststf_jar := $(intermediates)/android.core.vm-tests-tf.jar
vmteststf_dep_jars := $(addprefix $(HOST_OUT_JAVA_LIBRARIES)/, cts-tf-dalvik-buildutil.jar dasm.jar dx.jar cfassembler.jar junit.jar)
vmteststf_dep_jars += $(addprefix $(HOST_OUT_JAVA_LIBRARIES)/, jack.jar)
vmteststf_dep_jars += $(private_jill_jarjar_asm)

$(vmteststf_jar): PRIVATE_JACK_VM_ARGS := $(LOCAL_JACK_VM_ARGS)
$(vmteststf_jar): PRIVATE_JACK_EXTRA_ARGS := $(LOCAL_JACK_EXTRA_ARGS)

ifdef LOCAL_JACK_ENABLED
    vmteststf_dep_jars += $(cts-tf-dalvik-lib.jack)
endif

$(vmteststf_jar): PRIVATE_SRC_FOLDER := $(LOCAL_PATH)/src
$(vmteststf_jar): PRIVATE_INTERMEDIATES_CLASSES := $(call intermediates-dir-for,JAVA_LIBRARIES,cts-tf-dalvik-buildutil,HOST)/classes
$(vmteststf_jar): PRIVATE_INTERMEDIATES := $(intermediates)/tests
$(vmteststf_jar): PRIVATE_INTERMEDIATES_DEXCORE_JAR := $(intermediates)/tests/dot/junit/dexcore.jar
$(vmteststf_jar): PRIVATE_INTERMEDIATES_MAIN_FILES := $(intermediates)/main_files
$(vmteststf_jar): PRIVATE_INTERMEDIATES_HOSTJUNIT_FILES := $(intermediates)/hostjunit_files
$(vmteststf_jar): PRIVATE_CLASS_PATH := $(subst $(space),:,$(vmteststf_dep_jars)):$(HOST_JDK_TOOLS_JAR)
ifndef LOCAL_JACK_ENABLED
$(vmteststf_jar) : $(vmteststf_dep_jars) $(JACK_JAR) $(JILL_JAR) $(HOST_OUT_JAVA_LIBRARIES)/tradefed-prebuilt.jar
	$(hide) rm -rf $(dir $@) && mkdir -p $(dir $@)
	$(hide) mkdir -p $(PRIVATE_INTERMEDIATES_HOSTJUNIT_FILES)/dot/junit $(dir $(PRIVATE_INTERMEDIATES_DEXCORE_JAR))
	# generated and compile the host side junit tests
	@echo "Write generated Main_*.java files to $(PRIVATE_INTERMEDIATES_MAIN_FILES)"
	$(hide) java -cp $(PRIVATE_CLASS_PATH) util.build.BuildDalvikSuite $(PRIVATE_SRC_FOLDER) $(PRIVATE_INTERMEDIATES) \
		$(HOST_OUT_JAVA_LIBRARIES)/cts-tf-dalvik-buildutil.jar:$(HOST_OUT_JAVA_LIBRARIES)/tradefed-prebuilt.jar \
		$(PRIVATE_INTERMEDIATES_MAIN_FILES) $(PRIVATE_INTERMEDIATES_CLASSES) $(PRIVATE_INTERMEDIATES_HOSTJUNIT_FILES) $$RUN_VM_TESTS_RTO
	@echo "Generate $(PRIVATE_INTERMEDIATES_DEXCORE_JAR)"
	$(hide) jar -cf $(PRIVATE_INTERMEDIATES_DEXCORE_JAR).jar \
		$(addprefix -C $(PRIVATE_INTERMEDIATES_CLASSES) , dot/junit/DxUtil.class dot/junit/DxAbstractMain.class)
	$(hide) $(DX) -JXms16M -JXmx768M --dex --output=$(PRIVATE_INTERMEDIATES_DEXCORE_JAR) \
		$(if $(NO_OPTIMIZE_DX), --no-optimize) $(PRIVATE_INTERMEDIATES_DEXCORE_JAR).jar && rm -f $(PRIVATE_INTERMEDIATES_DEXCORE_JAR).jar
	$(hide) cd $(PRIVATE_INTERMEDIATES_HOSTJUNIT_FILES)/classes && zip -q -r ../../android.core.vm-tests-tf.jar .
	$(hide) cd $(dir $@) && zip -q -r android.core.vm-tests-tf.jar tests
else # LOCAL_JACK_ENABLED
$(vmteststf_jar) : $(vmteststf_dep_jars) $(JACK_JAR) $(JILL_JAR) $(call intermediates-dir-for,JAVA_LIBRARIES,core-libart,,COMMON)/classes.jack $(HOST_OUT_JAVA_LIBRARIES)/tradefed-prebuilt.jar
	$(hide) rm -rf $(dir $@) && mkdir -p $(dir $@)
	$(hide) mkdir -p $(PRIVATE_INTERMEDIATES_HOSTJUNIT_FILES)/dot/junit $(dir $(PRIVATE_INTERMEDIATES_DEXCORE_JAR))
	# generated and compile the host side junit tests
	@echo "Write generated Main_*.java files to $(PRIVATE_INTERMEDIATES_MAIN_FILES)"
	$(hide) java -cp $(PRIVATE_CLASS_PATH) util.build.JackBuildDalvikSuite $(PRIVATE_SRC_FOLDER) $(PRIVATE_INTERMEDIATES) \
		$(call intermediates-dir-for,JAVA_LIBRARIES,core-libart,,COMMON)/classes.jack:$(cts-tf-dalvik-lib.jack):$(HOST_OUT_JAVA_LIBRARIES)/tradefed-prebuilt.jar \
		$(PRIVATE_INTERMEDIATES_MAIN_FILES) $(PRIVATE_INTERMEDIATES_CLASSES) $(PRIVATE_INTERMEDIATES_HOSTJUNIT_FILES) $$RUN_VM_TESTS_RTO
	@echo "Generate $(PRIVATE_INTERMEDIATES_DEXCORE_JAR)"
	$(hide) jar -cf $(PRIVATE_INTERMEDIATES_DEXCORE_JAR)-class.jar \
		$(addprefix -C $(PRIVATE_INTERMEDIATES_CLASSES) , dot/junit/DxUtil.class dot/junit/DxAbstractMain.class)
	$(hide) $(JILL) --output $(PRIVATE_INTERMEDIATES_DEXCORE_JAR).jack $(PRIVATE_INTERMEDIATES_DEXCORE_JAR)-class.jar
	$(hide) mkdir -p $(PRIVATE_INTERMEDIATES_DEXCORE_JAR).tmp
	$(hide) $(call call-jack,$(PRIVATE_JACK_VM_ARGS),$(PRIVATE_JACK_EXTRA_ARGS)) --output-dex $(PRIVATE_INTERMEDIATES_DEXCORE_JAR).tmp \
		$(if $(NO_OPTIMIZE_DX), -D jack.dex.optimize "false") --import $(PRIVATE_INTERMEDIATES_DEXCORE_JAR).jack && rm -f $(PRIVATE_INTERMEDIATES_DEXCORE_JAR).jack
	$(hide) cd $(PRIVATE_INTERMEDIATES_DEXCORE_JAR).tmp && zip -q -r $(abspath $(PRIVATE_INTERMEDIATES_DEXCORE_JAR)) .
	$(hide) cd $(PRIVATE_INTERMEDIATES_HOSTJUNIT_FILES)/classes && zip -q -r ../../android.core.vm-tests-tf.jar .
	$(hide) cd $(dir $@) && zip -q -r android.core.vm-tests-tf.jar tests
endif # LOCAL_JACK_ENABLED

# Clean up temp vars
intermediates :=
vmteststf_jar :=
vmteststf_dep_jars :=
