# Copyright (C) 2008 The Android Open Source Project
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

# Check that LOCAL_MODULE is defined, then restore its LOCAL_XXXX values
$(call assert-defined,LOCAL_MODULE)
$(call module-restore-locals,$(LOCAL_MODULE))

# For now, only support target (device-specific modules).
# We may want to introduce support for host modules in the future
# but that is too experimental for now.
#
my := TARGET_

# LOCAL_MAKEFILE must also exist and name the Android.mk that
# included the module build script.
#
$(call assert-defined,LOCAL_MAKEFILE LOCAL_BUILD_SCRIPT LOCAL_BUILT_MODULE)

#
# Ensure that 'make <module>' and 'make clean-<module>' work
#
.PHONY: $(LOCAL_MODULE)
$(LOCAL_MODULE): $(LOCAL_BUILT_MODULE)

cleantarget := clean-$(LOCAL_MODULE)-$(TARGET_ARCH_ABI)
.PHONY: $(cleantarget)
clean: $(cleantarget)

$(cleantarget): PRIVATE_MODULE      := $(LOCAL_MODULE)
$(cleantarget): PRIVATE_TEXT        := [$(TARGET_ARCH_ABI)]
$(cleantarget): PRIVATE_CLEAN_FILES := $(LOCAL_BUILT_MODULE) \
                                       $($(my)OBJS)

$(cleantarget)::
	@echo "Clean: $(PRIVATE_MODULE) $(PRIVATE_TEXT)"
	$(hide) rm -rf $(PRIVATE_CLEAN_FILES)

# list of generated object files
LOCAL_OBJECTS :=

#
# always define ANDROID when building binaries
#
LOCAL_CFLAGS := -D__ANDROID__ -DANDROID $(LOCAL_CFLAGS)

#
# Check LOCAL_CPP_EXTENSION, use '.cpp' by default
#
LOCAL_CPP_EXTENSION := $(strip $(LOCAL_CPP_EXTENSION))
ifeq ($(LOCAL_CPP_EXTENSION),)
  LOCAL_CPP_EXTENSION := .cpp
else
  ifneq ($(words $(LOCAL_CPP_EXTENSION)),1)
    $(call __gdk_info, LOCAL_CPP_EXTENSION in $(LOCAL_MAKEFILE) must be one word only, not '$(LOCAL_CPP_EXTENSION)')
    $(call __gdk_error, Aborting)
  endif
endif

# all_source_patterns contains the list of filename patterns that correspond
# to source files recognized by our build system
all_source_extensions := .c $(LOCAL_CPP_EXTENSION)
all_source_patterns   := $(foreach _ext,$(all_source_extensions),%$(_ext))

unknown_sources := $(strip $(filter-out $(all_source_patterns),$(LOCAL_SRC_FILES)))
ifdef unknown_sources
    $(call __gdk_info,WARNING: Unsupported source file extensions in $(LOCAL_MAKEFILE) for module $(LOCAL_MODULE))
    $(call __gdk_info,  $(unknown_sources))
endif

# LOCAL_OBJECTS will list all object files corresponding to the sources
# listed in LOCAL_SRC_FILES, in the *same* order.
#
LOCAL_OBJECTS := $(LOCAL_SRC_FILES)
$(foreach _ext,$(all_source_extensions),\
    $(eval LOCAL_OBJECTS := $$(LOCAL_OBJECTS:%$(_ext)=%.bc))\
)
LOCAL_OBJECTS := $(filter %.bc,$(LOCAL_OBJECTS))
LOCAL_OBJECTS := $(foreach _obj,$(LOCAL_OBJECTS),$(LOCAL_OBJS_DIR)/$(_obj))

#
# Build the sources to object files
#

$(foreach src,$(filter %.c,$(LOCAL_SRC_FILES)), $(call compile-c-source,$(src)))
$(foreach src,$(filter %$(LOCAL_CPP_EXTENSION),$(LOCAL_SRC_FILES)),\
    $(call compile-cpp-source,$(src)))

#
# The compile-xxx-source calls updated LOCAL_OBJECTS and LOCAL_DEPENDENCY_DIRS
#
CLEAN_OBJS_DIRS     += $(LOCAL_OBJS_DIR)

$(LOCAL_BUILT_MODULE):

$(LOCAL_BUILT_MODULE): PRIVATE_OBJECTS          := $(LOCAL_OBJECTS)
$(LOCAL_BUILT_MODULE): PRIVATE_NAME := $(notdir $(LOCAL_BUILT_MODULE))

ifeq ($(call module-get-class,$(LOCAL_MODULE)),BITCODE)
$(LOCAL_BUILT_MODULE): $(LOCAL_OBJECTS)
	@ echo "BitcodeLibrary : $(PRIVATE_NAME)"
	$(hide) $(cmd-link-bitcodes)
endif

#
# If this is an installable module
#
ifeq ($(call module-is-installable,$(LOCAL_MODULE)),$(true))
$(LOCAL_INSTALLED): PRIVATE_NAME    := $(notdir $(LOCAL_BUILT_MODULE))
$(LOCAL_INSTALLED): PRIVATE_SRC     := $(LOCAL_BUILT_MODULE)
$(LOCAL_INSTALLED): PRIVATE_DST_DIR := $(GDK_APP_DST_DIR)
$(LOCAL_INSTALLED): PRIVATE_DST     := $(LOCAL_INSTALLED)

$(LOCAL_INSTALLED): $(LOCAL_BUILT_MODULE) clean-installed-binaries
	@ echo "Install BCLib  : $(PRIVATE_NAME) => $(call pretty-dir,$(PRIVATE_DST))"
	$(hide) mkdir -p $(PRIVATE_DST_DIR)
	$(hide) install -p $(PRIVATE_SRC) $(PRIVATE_DST)
endif
