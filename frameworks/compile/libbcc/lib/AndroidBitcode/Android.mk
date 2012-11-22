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
#

LOCAL_PATH := $(call my-dir)

#=====================================================================
# Common: libbccAndroidBitcode
#=====================================================================

libbcc_androidbitcode_SRC_FILES := \
  ABCCompiler.cpp \
  ABCExpandVAArgPass.cpp \
  ABCCompilerDriver.cpp

libbcc_arm_androidbitcode_SRC_FILES := \
  ARM/ARMABCExpandVAArg.cpp

libbcc_mips_androidbitcode_SRC_FILES := \
  Mips/MipsABCCompilerDriver.cpp \
  Mips/MipsABCExpandVAArg.cpp

libbcc_x86_androidbitcode_SRC_FILES := \
  X86/X86ABCCompilerDriver.cpp \
  X86/X86ABCExpandVAArg.cpp


#=====================================================================
# Device Static Library: libbccAndroidBitcode
#=====================================================================

include $(CLEAR_VARS)

LOCAL_MODULE := libbccAndroidBitcode
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_CLASS := STATIC_LIBRARIES
LOCAL_SRC_FILES := $(libbcc_androidbitcode_SRC_FILES)

ifeq ($(TARGET_ARCH),arm)
  LOCAL_SRC_FILES += $(libbcc_arm_androidbitcode_SRC_FILES)
else
  ifeq ($(TARGET_ARCH),mips)
    LOCAL_SRC_FILES += $(libbcc_mips_androidbitcode_SRC_FILES)
  else
    ifeq ($(TARGET_ARCH),x86) # We don't support x86-64 right now
      LOCAL_SRC_FILES += $(libbcc_x86_androidbitcode_SRC_FILES)
    else
      $(error Unsupported TARGET_ARCH $(TARGET_ARCH))
    endif
  endif
endif

include $(LIBBCC_DEVICE_BUILD_MK)
include $(LIBBCC_GEN_CONFIG_MK)
include $(MCLD_DEVICE_BUILD_MK)
include $(BUILD_STATIC_LIBRARY)

#=====================================================================
# Host Static Library: libbccAndroidBitcode
#=====================================================================

include $(CLEAR_VARS)

LOCAL_MODULE := libbccAndroidBitcode
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_CLASS := STATIC_LIBRARIES
LOCAL_SRC_FILES := \
  $(libbcc_androidbitcode_SRC_FILES) \
  $(libbcc_arm_androidbitcode_SRC_FILES) \
  $(libbcc_mips_androidbitcode_SRC_FILES) \
  $(libbcc_x86_androidbitcode_SRC_FILES) \

include $(LIBBCC_HOST_BUILD_MK)
include $(LIBBCC_GEN_CONFIG_MK)
include $(MCLD_HOST_BUILD_MK)
include $(BUILD_HOST_STATIC_LIBRARY)
