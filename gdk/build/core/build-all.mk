# Copyright (C) 2009-2010 The Android Open Source Project
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
#

#
# This script is used to build all wanted GDK binaries. It is included
# by several scripts.
#

# ensure that the following variables are properly defined
$(call assert-defined,GDK_APPS GDK_APP_OUT)

# ====================================================================
#
# Prepare the build for parsing Android.mk files
#
# ====================================================================

# These phony targets are used to control various stages of the build
.PHONY: all \
        installed_modules \
        clean distclean \
        clean-installed-binaries

# These macros are used in Android.mk to include the corresponding
# build script that will parse the LOCAL_XXX variable definitions.
#
CLEAR_VARS    := $(BUILD_SYSTEM)/clear-vars.mk
BUILD_BITCODE := $(BUILD_SYSTEM)/build-bitcode.mk

ANDROID_MK_INCLUDED := \
  $(CLEAR_VARS) \
  $(BUILD_BITCODE)

WANTED_INSTALLED_MODULES  :=

# the first rule
all: installed_modules


$(foreach _app,$(GDK_APPS),\
  $(eval include $(BUILD_SYSTEM)/setup-app.mk)\
)

# ====================================================================
#
# Now finish the build preparation with a few rules that depend on
# what has been effectively parsed and recorded previously
#
# ====================================================================

clean: clean-installed-binaries

distclean: clean

installed_modules: clean-installed-binaries $(WANTED_INSTALLED_MODULES)
