#
# Copyright (C) 2011 The Android Open Source Project
#
# Licensed under the Eclipse Public License, Version 1.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.eclipse.org/org/documents/epl-v10.php
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

ifeq ($(HOST_OS),darwin)
LOCAL_SRC_FILES += $(call all-java-files-under, src-$(HOST_OS))
LOCAL_JAVA_RESOURCE_DIRS += src-$(HOST_OS)
endif

LOCAL_MODULE := swtmenubar
LOCAL_MODULE_TAGS := optional

LOCAL_JAVA_LIBRARIES := \
	swt \
	org.eclipse.jface_3.6.2.M20110210-1200


include $(BUILD_HOST_JAVA_LIBRARY)

