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

# We have to use PRODUCT_PACKAGES (together with BUILD_PREBUILT) instead of
# PRODUCT_COPY_FILES to install the pattern files, so that the NOTICE file can
# get installed too.

pattern_locales := \
    en-us eu hu hy \
    nb nn sa und-ethi

PRODUCT_PACKAGES := \
    $(foreach locale, $(pattern_locales), \
        $(addprefix hyph-, $(addprefix $(locale), \
            .chr.txt .hyp.txt .lic.txt .pat.txt)))

pattern_locales :=
