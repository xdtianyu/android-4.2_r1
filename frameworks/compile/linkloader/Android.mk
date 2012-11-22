#
# Copyright (C) 2011 The Android Open Source Project
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

LLVM_ROOT_PATH := external/llvm

#=============================================================================
# android librsloader for libbcc (Device)
#-----------------------------------------------------------------------------

include $(CLEAR_VARS)

LOCAL_MODULE := librsloader

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := \
  lib/ELFHeader.cpp \
  lib/ELFSymbol.cpp \
  lib/ELFSectionHeader.cpp \
  lib/ELFTypes.cpp \
  lib/MemChunk.cpp \
  lib/StubLayout.cpp \
  lib/GOT.cpp \
  utils/raw_ostream.cpp \
  utils/rsl_assert.cpp \
  utils/helper.cpp \
  android/librsloader.cpp

LOCAL_C_INCLUDES := \
  $(LOCAL_PATH)/ \
  $(LOCAL_PATH)/include \
  bionic \
  external/elfutils/libelf \
  external/stlport/stlport \
  $(LOCAL_C_INCLUDES)

include $(LLVM_ROOT_PATH)/llvm-device-build.mk
include $(BUILD_STATIC_LIBRARY)


#=============================================================================
# android librsloader for libbcc (Host)
#-----------------------------------------------------------------------------

include $(CLEAR_VARS)

LOCAL_MODULE := librsloader

LOCAL_SRC_FILES := \
  lib/ELFHeader.cpp \
  lib/ELFSymbol.cpp \
  lib/ELFSectionHeader.cpp \
  lib/ELFTypes.cpp \
  lib/MemChunk.cpp \
  lib/StubLayout.cpp \
  lib/GOT.cpp \
  utils/raw_ostream.cpp \
  utils/rsl_assert.cpp \
  utils/helper.cpp \
  android/librsloader.cpp

LOCAL_C_INCLUDES := \
  $(LOCAL_PATH)/ \
  $(LOCAL_PATH)/include \
  $(LOCAL_C_INCLUDES)

ifeq (darwin,$(BUILD_OS))
LOCAL_CFLAGS += -DMACOSX
endif

LOCAL_CFLAGS += -D__HOST__

include $(LLVM_ROOT_PATH)/llvm-host-build.mk
include $(BUILD_HOST_STATIC_LIBRARY)


#=============================================================================
# librsloader-test (Device)
#-----------------------------------------------------------------------------

include $(CLEAR_VARS)

LOCAL_MODULE := test-librsloader

LOCAL_MODULE_TAGS := tests

LOCAL_SHARED_LIBRARIES := \
  libstlport

LOCAL_STATIC_LIBRARIES := \
  librsloader \
  libcutils \
  libLLVMSupport

LOCAL_SRC_FILES := \
  android/test-librsloader.c

include $(LLVM_ROOT_PATH)/llvm-device-build.mk
include $(BUILD_EXECUTABLE)


#=============================================================================
# librsloader-test (Host)
#-----------------------------------------------------------------------------

include $(CLEAR_VARS)

LOCAL_MODULE := test-librsloader

LOCAL_LDLIBS := \
  -lpthread \
  -ldl

LOCAL_STATIC_LIBRARIES := \
  librsloader \
  libcutils \
  libLLVMSupport

LOCAL_SRC_FILES := \
  android/test-librsloader.c

include $(LLVM_ROOT_PATH)/llvm-host-build.mk
include $(BUILD_HOST_EXECUTABLE)


#=============================================================================
# rsloader
#-----------------------------------------------------------------------------

ifdef BUILD_RSLOADER_TOOL
include $(CLEAR_VARS)

LOCAL_MODULE := rsloader

LOCAL_MODULE_TAGS := tests

LOCAL_SHARED_LIBRARIES := \
  libstlport

LOCAL_STATIC_LIBRARIES := \
  libLLVMSupport

LOCAL_SRC_FILES := \
  lib/ELFHeader.cpp \
  lib/ELFSymbol.cpp \
  lib/ELFSectionHeader.cpp \
  lib/ELFTypes.cpp \
  lib/StubLayout.cpp \
  utils/raw_ostream.cpp \
  utils/rsl_assert.cpp \
  utils/helper.cpp \
  main.cpp

LOCAL_C_INCLUDES := \
  $(LOCAL_PATH) \
  $(LOCAL_PATH)/include \
  bionic \
  external/elfutils/libelf \
  external/stlport/stlport \
  $(LOCAL_C_INCLUDES)

include $(LLVM_ROOT_PATH)/llvm-device-build.mk
include $(BUILD_EXECUTABLE)
endif


#=============================================================================
# stub-layout-unit-test
#-----------------------------------------------------------------------------

ifdef BUILD_STUB_LAYOUT_TEST
include $(CLEAR_VARS)

LOCAL_MODULE := stub-layout-unit-test

LOCAL_MODULE_TAGS := tests

LOCAL_SHARED_LIBRARIES := \
  libstlport

LOCAL_SRC_FILES := \
  lib/StubLayout.cpp \
  utils/raw_ostream.cpp \
  utils/helper.cpp \
  tests/stub-test.cpp

LOCAL_C_INCLUDES := \
  $(LOCAL_PATH) \
  $(LOCAL_PATH)/include \
  bionic \
  external/elfutils/libelf \
  external/stlport/stlport \
  $(LOCAL_C_INCLUDES)

include $(LLVM_ROOT_PATH)/llvm-device-build.mk
include $(BUILD_EXECUTABLE)
endif
