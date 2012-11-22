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
LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := okhttp
LOCAL_MODULE_TAGS := optional

# Include all files under our snapshot except for Libcore.java
# because it contains OpenJDK dependencies. This file is replaced
# by an android specific version, see below.
LOCAL_SRC_FILES := $(call all-java-files-under, src/main/java)
LOCAL_SRC_FILES := $(filter-out %/Libcore.java, $(LOCAL_SRC_FILES))

LOCAL_SRC_FILES += $(call all-java-files-under, android/main/java)
LOCAL_SDK_VERSION := 16

LOCAL_JARJAR_RULES := ${LOCAL_PATH}/jarjar-rules.txt
include $(BUILD_JAVA_LIBRARY)
