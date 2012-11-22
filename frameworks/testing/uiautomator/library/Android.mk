#
# Copyright (C) 2012 The Android Open Source Project
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

LOCAL_PATH:= $(call my-dir)

uiautomator.core_src_files := $(call all-java-files-under, src)
uiautomator.core_java_libraries := android.test.runner core-junit

INTERNAL_UIAUTOMATOR_API_FILE := $(TARGET_OUT_COMMON_INTERMEDIATES)/PACKAGING/uiautomator_api.txt

###############################################
include $(CLEAR_VARS)
LOCAL_SRC_FILES := $(uiautomator.core_src_files)
LOCAL_MODULE := uiautomator.core
LOCAL_JAVA_LIBRARIES := android.test.runner
include $(BUILD_STATIC_JAVA_LIBRARY)

###############################################
# Generate the stub source files
include $(CLEAR_VARS)
LOCAL_SRC_FILES := $(uiautomator.core_src_files)
LOCAL_JAVA_LIBRARIES := $(uiautomator.core_java_libraries)
LOCAL_MODULE_CLASS := JAVA_LIBRARIES
LOCAL_DROIDDOC_SOURCE_PATH := $(LOCAL_PATH)/src
LOCAL_DROIDDOC_HTML_DIR :=

LOCAL_DROIDDOC_OPTIONS:= \
    -stubs $(TARGET_OUT_COMMON_INTERMEDIATES)/JAVA_LIBRARIES/android_uiautomator_intermediates/src \
    -stubpackages com.android.uiautomator.core:com.android.uiautomator.testrunner \
    -api $(INTERNAL_UIAUTOMATOR_API_FILE) \
    -nodocs

LOCAL_DROIDDOC_CUSTOM_TEMPLATE_DIR := build/tools/droiddoc/templates-sdk
LOCAL_UNINSTALLABLE_MODULE := true

LOCAL_MODULE := uiautomator-stubs

include $(BUILD_DROIDDOC)
uiautomator-stubs-stamp := $(full_target)

###############################################
# Build the stub source files into a jar.
include $(CLEAR_VARS)
LOCAL_MODULE := android_uiautomator
LOCAL_JAVA_LIBRARIES := $(uiautomator.core_java_libraries)
LOCAL_SOURCE_FILES_ALL_GENERATED := true
include $(BUILD_STATIC_JAVA_LIBRARY)
# Make sure to run droiddoc first to generate the stub source files.
$(full_classes_compiled_jar) : $(uiautomator-stubs-stamp)

###############################################
# clean up temp vars
uiautomator.core_src_files :=
uiautomator.core_java_libraries :=
uiautomator-stubs-stamp :=
