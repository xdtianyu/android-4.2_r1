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
COMPILER_RT_PATH := $(LOCAL_PATH)

#=====================================================================
# Device Static Library: libbccCompilerRT
#=====================================================================

include $(CLEAR_VARS)

LOCAL_MODULE := libcompiler-rt
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_CLASS := STATIC_LIBRARIES
LOCAL_CLANG := true
LOCAL_CFLAGS := -integrated-as

# Pull in platform-independent functionality
LOCAL_WHOLE_STATIC_LIBRARIES += libcompiler-rt-builtins libcompiler-rt-extras

ifeq ($(TARGET_ARCH),arm)
  LOCAL_SRC_FILES += \
    lib/arm/adddf3vfp.S \
    lib/arm/addsf3vfp.S \
    lib/arm/divdf3vfp.S \
    lib/arm/divsf3vfp.S \
    lib/arm/eqdf2vfp.S \
    lib/arm/eqsf2vfp.S \
    lib/arm/extendsfdf2vfp.S \
    lib/arm/fixdfsivfp.S \
    lib/arm/fixsfsivfp.S \
    lib/arm/fixunsdfsivfp.S \
    lib/arm/fixunssfsivfp.S \
    lib/arm/floatsidfvfp.S \
    lib/arm/floatsisfvfp.S \
    lib/arm/floatunssidfvfp.S \
    lib/arm/floatunssisfvfp.S \
    lib/arm/gedf2vfp.S \
    lib/arm/gesf2vfp.S \
    lib/arm/gtdf2vfp.S \
    lib/arm/gtsf2vfp.S \
    lib/arm/ledf2vfp.S \
    lib/arm/lesf2vfp.S \
    lib/arm/ltdf2vfp.S \
    lib/arm/ltsf2vfp.S \
    lib/arm/muldf3vfp.S \
    lib/arm/mulsf3vfp.S \
    lib/arm/nedf2vfp.S \
    lib/arm/negdf2vfp.S \
    lib/arm/negsf2vfp.S \
    lib/arm/nesf2vfp.S \
    lib/arm/subdf3vfp.S \
    lib/arm/subsf3vfp.S \
    lib/arm/truncdfsf2vfp.S \
    lib/arm/unorddf2vfp.S \
    lib/arm/unordsf2vfp.S
else
  ifeq ($(TARGET_ARCH),x86) # We don't support x86-64 right now
    LOCAL_SRC_FILES += \
      lib/i386/ashldi3.S \
      lib/i386/ashrdi3.S \
      lib/i386/divdi3.S \
      lib/i386/floatdidf.S \
      lib/i386/floatdisf.S \
      lib/i386/floatdixf.S \
      lib/i386/floatundidf.S \
      lib/i386/floatundisf.S \
      lib/i386/floatundixf.S \
      lib/i386/lshrdi3.S \
      lib/i386/moddi3.S \
      lib/i386/muldi3.S \
      lib/i386/udivdi3.S \
      lib/i386/umoddi3.S
  else
    ifeq ($(TARGET_ARCH),mips)
      # nothing to add
    else
      $(error Unsupported TARGET_ARCH $(TARGET_ARCH))
    endif
  endif
endif

include $(BUILD_STATIC_LIBRARY)

include $(COMPILER_RT_PATH)/lib/Android.mk

include $(COMPILER_RT_PATH)/lib/asan/Android.mk
