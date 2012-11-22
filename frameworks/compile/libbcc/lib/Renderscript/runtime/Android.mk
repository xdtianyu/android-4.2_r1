#
# Copyright (C) 2011-2012 The Android Open Source Project
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

# C/LLVM-IR source files for the library
clcore_base_files := \
    rs_allocation.c \
    rs_cl.c \
    rs_core.c \
    rs_element.c \
    rs_mesh.c \
    rs_program.c \
    rs_sample.c \
    rs_sampler.c \
    convert.ll \
    matrix.ll \
    pixel_packing.ll \
    math.ll \
    rsClamp.ll

clcore_files := \
    $(clcore_base_files) \
    arch/generic.c

clcore_neon_files := \
    $(clcore_base_files) \
    arch/neon.ll

ifeq "REL" "$(PLATFORM_VERSION_CODENAME)"
  RS_VERSION := $(PLATFORM_SDK_VERSION)
else
  # Increment by 1 whenever this is not a final release build, since we want to
  # be able to see the RS version number change during development.
  # See build/core/version_defaults.mk for more information about this.
  RS_VERSION := "(1 + $(PLATFORM_SDK_VERSION))"
endif

# Build the base version of the library
include $(CLEAR_VARS)
LOCAL_MODULE := libclcore.bc
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_CLASS := SHARED_LIBRARIES
LOCAL_SRC_FILES := $(clcore_files)

include $(LOCAL_PATH)/build_bc_lib.mk

# Build a NEON-enabled version of the library (if possible)
ifeq ($(ARCH_ARM_HAVE_NEON),true)
include $(CLEAR_VARS)
LOCAL_MODULE := libclcore_neon.bc
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_CLASS := SHARED_LIBRARIES
LOCAL_SRC_FILES := $(clcore_neon_files)

include $(LOCAL_PATH)/build_bc_lib.mk
endif
