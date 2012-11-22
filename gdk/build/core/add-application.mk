# Copyright (C) 2009 The Android Open Source Project
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

# this script is used to record an application definition in the
# GDK build system, before performing any build whatsoever.
#
# It is included repeatedly from build/core/main.mk and expects a
# variable named '_application_mk' which points to a given Application.mk
# file that will be included here. The latter must define a few variables
# to describe the application to the build system, and the rest of the
# code here will perform book-keeping and basic checks
#

$(call assert-defined, _application_mk _app)
$(call gdk_log,Parsing $(_application_mk))

$(call clear-vars, $(GDK_APP_VARS))

# Check that GDK_DEBUG is properly defined. If it is
# the only valid states are: undefined, 0, 1, false and true
#
# We set APP_DEBUG to <undefined>, 'true' or 'false'.
#
APP_DEBUG := $(strip $(GDK_DEBUG))
ifeq ($(APP_DEBUG),0)
  APP_DEBUG:= false
endif
ifeq ($(APP_DEBUG),1)
  APP_DEBUG := true
endif
ifdef APP_DEBUG
  ifneq (,$(filter-out true false,$(APP_DEBUG)))
    $(call __gdk_warning,GDK_DEBUG is defined to the unsupported value '$(GDK_DEBUG)', will be ignored!)
  endif
endif

include $(_application_mk)

$(call check-required-vars,$(GDK_APP_VARS_REQUIRED),$(_application_mk))

_map := GDK_APP.$(_app)

# strip the 'lib' prefix in front of APP_MODULES modules
APP_MODULES := $(call strip-lib-prefix,$(APP_MODULES))

APP_PROJECT_PATH := $(strip $(APP_PROJECT_PATH))
ifndef APP_PROJECT_PATH
    APP_PROJECT_PATH := $(GDK_PROJECT_PATH)
endif

# check whether APP_PLATFORM is defined. If not, look for default.properties in
# the $(APP_PROJECT_PATH) and extract the value with awk's help. If nothing is here,
# revert to the default value (i.e. "android-3").
#
# NOTE: APP_PLATFORM is an experimental feature for now.
#
APP_PLATFORM := android-portable

# If APP_BUILD_SCRIPT is defined, check that the file exists.
# If undefined, look in $(APP_PROJECT_PATH)/jni/Android.mk
#
APP_BUILD_SCRIPT := $(strip $(APP_BUILD_SCRIPT))
ifdef APP_BUILD_SCRIPT
    _build_script := $(strip $(wildcard $(APP_BUILD_SCRIPT)))
    ifndef _build_script
        $(call __gdk_info,Your APP_BUILD_SCRIPT points to an unknown file: $(APP_BUILD_SCRIPT))
        $(call __gdk_error,Aborting...)
    endif
    APP_BUILD_SCRIPT := $(_build_script)
    $(call gdk_log,  Using build script $(APP_BUILD_SCRIPT))
else
    _build_script := $(strip $(wildcard $(APP_PROJECT_PATH)/jni/Android-portable.mk))
    ifndef _build_script
        $(call __gdk_info,There is no Android-portable.mk under $(APP_PROJECT_PATH)/jni)
        $(call __gdk_info,If this is intentional, please define APP_BUILD_SCRIPT to point)
        $(call __gdk_info,to a valid GDK build script.)
        $(call __gdk_error,Aborting...)
    endif
    APP_BUILD_SCRIPT := $(_build_script)
    $(call gdk_log,  Defaulted to APP_BUILD_SCRIPT=$(APP_BUILD_SCRIPT))
endif

# Determine whether the application should be debuggable.
# - If APP_DEBUG is set to 'true', then it always should.
# - If APP_DEBUG is set to 'false', then it never should
# - Otherwise, extract the android:debuggable attribute from the manifest.
#
ifdef APP_DEBUG
  APP_DEBUGGABLE := $(APP_DEBUG)
  ifdef GDK_LOG
    ifeq ($(APP_DEBUG),true)
      $(call gdk_log,Application '$(_app)' forced debuggable through GDK_DEBUG)
    else
      $(call gdk_log,Application '$(_app)' forced *not* debuggable through GDK_DEBUG)
    endif
  endif
else
  # NOTE: To make unit-testing simpler, handle the case where there is no manifest.
  APP_DEBUGGABLE := false
  APP_MANIFEST := $(strip $(wildcard $(APP_PROJECT_PATH)/AndroidManifest.xml))
  ifdef APP_MANIFEST
    APP_DEBUGGABLE := $(shell $(HOST_AWK) -f $(BUILD_AWK)/extract-debuggable.awk $(APP_MANIFEST))
  endif
  ifdef GDK_LOG
    ifeq ($(APP_DEBUGGABLE),true)
      $(call gdk_log,Application '$(_app)' *is* debuggable)
    else
      $(call gdk_log,Application '$(_app)' is not debuggable)
    endif
  endif
endif

# LOCAL_BUILD_MODE will be either release or debug
#
# If APP_OPTIM is defined in the Application.mk, just use this.
#
# Otherwise, set to 'debug' if android:debuggable is set to TRUE,
# and to 'release' if not.
#
ifneq ($(APP_OPTIM),)
    # check that APP_OPTIM, if defined, is either 'release' or 'debug'
    $(if $(filter-out release debug,$(APP_OPTIM)),\
        $(call __gdk_info, The APP_OPTIM defined in $(_application_mk) must only be 'release' or 'debug')\
        $(call __gdk_error,Aborting)\
    )
    $(call gdk_log,Selecting optimization mode through Application.mk: $(APP_OPTIM))
else
    ifeq ($(APP_DEBUGGABLE),true)
        $(call gdk_log,Selecting debug optimization mode (app is debuggable))
        APP_OPTIM := debug
    else
        $(call gdk_log,Selecting release optimization mode (app is not debuggable))
        APP_OPTIM := release
    endif
endif

# set release/debug build flags. We always use the -g flag because
# we generate symbol versions of the binaries that are later stripped
# when they are copied to the final project's libs/<abi> directory.
#
ifeq ($(APP_OPTIM),debug)
  APP_CFLAGS := -O0 -g $(APP_CFLAGS)
else
  APP_CFLAGS := -O2 -DNDEBUG -g $(APP_CFLAGS)
endif

$(if $(call get,$(_map),defined),\
  $(call __gdk_info,Weird, the application $(_app) is already defined by $(call get,$(_map),defined))\
  $(call __gdk_error,Aborting)\
)

$(call set,$(_map),defined,$(_application_mk))

# Record all app-specific variable definitions
$(foreach __name,$(GDK_APP_VARS),\
  $(call set,$(_map),$(__name),$($(__name)))\
)

GDK_ALL_APPS += $(_app)
