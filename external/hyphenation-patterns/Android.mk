# Copyright (C) 2015 The Android Open Source Project
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

HYPHENS_DIR := $(call my-dir)

pattern_locales := \
    en-US/en-us \
    Ethi/und-ethi \
    eu/eu \
    hu/hu \
    hy/hy \
    nb/nb \
    nn/nn \
    sa/sa

pattern_src_files := \
    $(foreach locale, $(pattern_locales), \
        $(addprefix $(dir $(locale)), \
            $(addprefix hyph-, $(addprefix $(notdir $(locale)), \
                .chr.txt .hyp.txt .lic.txt .pat.txt))))
pattern_locales :=

# We have to use BUILD_PREBUILT instead of PRODUCT_COPY_FILES,
# to copy over the NOTICE file.

#############################################################################
# $(1): The source file name in LOCAL_PATH.
#       It also serves as the module name and the dest file name.
#############################################################################
define build-one-pattern-module
$(eval include $(CLEAR_VARS))\
$(eval LOCAL_PATH := $(abspath $(addprefix $(HYPHENS_DIR)/, $(dir $(1)))))\
$(eval LOCAL_MODULE := $(notdir $(1)))\
$(eval LOCAL_SRC_FILES := $(notdir $(1)))\
$(eval LOCAL_MODULE_CLASS := ETC)\
$(eval LOCAL_MODULE_TAGS := optional)\
$(eval LOCAL_MODULE_PATH := $(TARGET_OUT)/usr/hyphen-data)\
$(eval include $(BUILD_PREBUILT))
endef

$(foreach f, $(pattern_src_files), $(call build-one-pattern-module, $(f)))
build-one-pattern-module :=
pattern_src_files :=
HYPHENS_DIR :=
