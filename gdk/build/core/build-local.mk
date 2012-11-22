# Detect the GDK installation path by processing this Makefile's location.
# This assumes we are located under $GDK_ROOT/build/core/main.mk
#
GDK_ROOT := $(dir $(lastword $(MAKEFILE_LIST)))
GDK_ROOT := $(strip $(GDK_ROOT:%build/core/=%))
GDK_ROOT := $(GDK_ROOT:%/=%)
ifeq ($(GDK_ROOT),)
    # for the case when we're invoked from the GDK install path
    GDK_ROOT := .
endif
ifdef GDK_LOG
    $(info Android GDK: GDK installation path auto-detected: '$(GDK_ROOT)')
endif
ifneq ($(words $(GDK_ROOT)),1)
    $(info Android GDK: You GDK installation path contains spaces.)
    $(info Android GDK: Please re-install to a different location to fix the issue !)
    $(error Aborting.)
endif

include $(GDK_ROOT)/build/core/init.mk

# ====================================================================
#
# If GDK_PROJECT_PATH is not defined, find the application's project
# path by looking at the manifest file in the current directory or
# any of its parents. If none is found, try again with 'jni/Android.mk'
#
# It turns out that some people use gdk-build to generate static
# libraries without a full Android project tree.
#
# ====================================================================

find-project-dir = $(strip $(call find-project-dir-inner,$1,$2))

find-project-dir-inner = \
    $(eval __found_project_path := )\
    $(eval __find_project_path := $1)\
    $(eval __find_project_file := $2)\
    $(call find-project-dir-inner-2)\
    $(__found_project_path)

find-project-dir-inner-2 = \
    $(call gdk_log,Looking for $(__find_project_file) in $(__find_project_path))\
    $(eval __find_project_manifest := $(strip $(wildcard $(__find_project_path)/$(__find_project_file))))\
    $(if $(__find_project_manifest),\
        $(call gdk_log,    Found it !)\
        $(eval __found_project_path := $(__find_project_path))\
        ,\
        $(eval __find_project_parent := $(patsubst %/,%,$(dir $(__find_project_path))))\
        $(if $(__find_project_parent),\
            $(eval __find_project_path := $(__find_project_parent))\
            $(call find-project-dir-inner-2)\
        )\
    )

GDK_PROJECT_PATH := $(strip $(GDK_PROJECT_PATH))
ifndef GDK_PROJECT_PATH
    GDK_PROJECT_PATH := $(call find-project-dir,$(strip $(shell pwd)),AndroidManifest.xml)
endif
ifndef GDK_PROJECT_PATH
    GDK_PROJECT_PATH := $(call find-project-dir,$(strip $(shell pwd)),jni/Android-portable.mk)
endif
ifndef GDK_PROJECT_PATH
    $(call __gdk_info,Could not find application project directory !)
    $(call __gdk_info,Please define the GDK_PROJECT_PATH variable to point to it.)
    $(call __gdk_error,Aborting)
endif

# Check that there are no spaces in the project path, or bad things will happen
ifneq ($(words $(GDK_PROJECT_PATH)),1)
    $(call __gdk_info,Your Android application project path contains spaces: '$(GDK_PROJECT_PATH)')
    $(call __gdk_info,The Android GDK build cannot work here. Please move your project to a different location.)
    $(call __gdk_error,Aborting.)
endif

GDK_APPLICATION_MK := $(GDK_ROOT)/build/core/default-application.mk

$(call gdk_log,Found project path: $(GDK_PROJECT_PATH))

# Place all generated files here
GDK_APP_OUT := $(GDK_PROJECT_PATH)/obj

# Fake an application named 'local'
_app            := local
_application_mk := $(GDK_APPLICATION_MK)
GDK_APPS        := $(_app)

include $(BUILD_SYSTEM)/add-application.mk

# Build it
include $(BUILD_SYSTEM)/build-all.mk
