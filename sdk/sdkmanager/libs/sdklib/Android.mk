#
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
LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(call all-java-files-under, src)
LOCAL_JAVA_RESOURCE_DIRS := src

LOCAL_JAR_MANIFEST := manifest.txt

# IMPORTANT: if you add a new dependency here, please make sure
# to also check the following files:
#   sdkmanager/sdklib/manifest.txt
#   sdkmanager/app/etc/android.bat
LOCAL_JAVA_LIBRARIES := \
        common \
        commons-codec-1.4 \
        commons-compress-1.0 \
        commons-logging-1.1.1 \
        dvlib \
        guava-tools \
        httpclient-4.1.1 \
        httpcore-4.1 \
        httpmime-4.1.1 \
        mkidentity-prebuilt \
        layoutlib_api

LOCAL_MODULE := sdklib

include $(BUILD_HOST_JAVA_LIBRARY)


# Build all sub-directories
include $(call all-makefiles-under,$(LOCAL_PATH))
