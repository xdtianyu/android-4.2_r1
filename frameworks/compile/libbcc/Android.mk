#
# Copyright (C) 2010-2012 The Android Open Source Project
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
LIBBCC_ROOT_PATH := $(LOCAL_PATH)
include $(LIBBCC_ROOT_PATH)/libbcc.mk

#=====================================================================
# Whole Static Library to Be Linked In
#=====================================================================

libbcc_WHOLE_STATIC_LIBRARIES += \
  libbccAndroidBitcode \
  libbccRenderscript \
  libbccExecutionEngine \
  libbccCore \
  libbccSupport

libmcld_STATIC_LIBRARIES += \
  libmcldCodeGen \
  libmcldTarget \
  libmcldLDVariant \
  libmcldMC \
  libmcldSupport \
  libmcldADT \
  libmcldLD

#=====================================================================
# Calculate SHA1 checksum for libbcc.so, libRS.so and libclcore.bc
#=====================================================================

include $(CLEAR_VARS)

LOCAL_MODULE := libbcc.sha1
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_CLASS := SHARED_LIBRARIES

libbcc_SHA1_SRCS := \
  $(TARGET_OUT_INTERMEDIATE_LIBRARIES)/libbcc.so \
  $(TARGET_OUT_INTERMEDIATE_LIBRARIES)/libRS.so \
  $(call intermediates-dir-for,SHARED_LIBRARIES,libclcore.bc,,)/libclcore.bc

ifeq ($(ARCH_ARM_HAVE_NEON),true)
libbcc_SHA1_SRCS += \
  $(call intermediates-dir-for,SHARED_LIBRARIES,libclcore_neon.bc,,)/libclcore_neon.bc
endif

libbcc_GEN_SHA1_STAMP := $(LOCAL_PATH)/tools/build/gen-sha1-stamp.py
intermediates := $(call local-intermediates-dir)

libbcc_SHA1_ASM := $(intermediates)/libbcc.sha1.S
LOCAL_GENERATED_SOURCES += $(libbcc_SHA1_ASM)
$(libbcc_SHA1_ASM): PRIVATE_SHA1_SRCS := $(libbcc_SHA1_SRCS)
$(libbcc_SHA1_ASM): $(libbcc_SHA1_SRCS) $(libbcc_GEN_SHA1_STAMP)
	@echo libbcc.sha1: $@
	$(hide) mkdir -p $(dir $@)
	$(hide) $(libbcc_GEN_SHA1_STAMP) $(PRIVATE_SHA1_SRCS) > $@

LOCAL_CFLAGS += -D_REENTRANT -DPIC -fPIC
LOCAL_CFLAGS += -O3 -nodefaultlibs -nostdlib

include $(BUILD_SHARED_LIBRARY)

#=====================================================================
# Device Shared Library libbcc
#=====================================================================

include $(CLEAR_VARS)

LOCAL_MODULE := libbcc
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_CLASS := SHARED_LIBRARIES

LOCAL_WHOLE_STATIC_LIBRARIES := $(libbcc_WHOLE_STATIC_LIBRARIES)

ifeq ($(TARGET_ARCH),$(filter $(TARGET_ARCH),arm x86))
LOCAL_WHOLE_STATIC_LIBRARIES += libbccCompilerRT
endif

LOCAL_WHOLE_STATIC_LIBRARIES += librsloader

ifeq ($(libbcc_USE_DISASSEMBLER),1)
  ifeq ($(TARGET_ARCH),arm)
    LOCAL_WHOLE_STATIC_LIBRARIES += \
      libLLVMARMDisassembler
  else
    ifeq ($(TARGET_ARCH),mips)
      LOCAL_WHOLE_STATIC_LIBRARIES += \
        libLLVMMipsDisassembler
    else
      ifeq ($(TARGET_ARCH),x86)
        LOCAL_WHOLE_STATIC_LIBRARIES += \
          libLLVMX86Disassembler
      else
        $(error Unsupported TARGET_ARCH $(TARGET_ARCH))
      endif
    endif
  endif
endif

ifeq ($(TARGET_ARCH),arm)
  LOCAL_WHOLE_STATIC_LIBRARIES += \
    libmcldARMTarget \
    libmcldARMInfo \
    $(libmcld_STATIC_LIBRARIES) \
    libLLVMARMAsmParser \
    libLLVMARMCodeGen \
    libLLVMARMDesc \
    libLLVMARMInfo
else
  ifeq ($(TARGET_ARCH), mips)
    LOCAL_WHOLE_STATIC_LIBRARIES += \
      libmcldMipsTarget \
      libmcldMipsInfo \
      $(libmcld_STATIC_LIBRARIES) \
      libLLVMMipsAsmParser \
      libLLVMMipsCodeGen \
      libLLVMMipsAsmPrinter \
      libLLVMMipsDesc \
      libLLVMMipsInfo
  else
    ifeq ($(TARGET_ARCH),x86) # We don't support x86-64 right now
      LOCAL_WHOLE_STATIC_LIBRARIES += \
        libmcldX86Target \
        libmcldX86Info \
        $(libmcld_STATIC_LIBRARIES) \
        libLLVMX86AsmParser \
        libLLVMX86CodeGen \
        libLLVMX86Desc \
        libLLVMX86Info \
        libLLVMX86Utils \
        libLLVMX86AsmPrinter
    else
      $(error Unsupported TARGET_ARCH $(TARGET_ARCH))
    endif
  endif
endif

LOCAL_WHOLE_STATIC_LIBRARIES += \
  libLLVMObject \
  libLLVMAsmPrinter \
  libLLVMBitWriter \
  libLLVMBitReader \
  libLLVMSelectionDAG \
  libLLVMCodeGen \
  libLLVMLinker \
  libLLVMScalarOpts \
  libLLVMInstCombine \
  libLLVMipo \
  libLLVMipa \
  libLLVMVectorize \
  libLLVMInstrumentation \
  libLLVMTransformUtils \
  libLLVMAnalysis \
  libLLVMTarget \
  libLLVMMCParser \
  libLLVMMC \
  libLLVMCore \
  libLLVMSupport

LOCAL_SHARED_LIBRARIES := libbcinfo libdl libutils libcutils libstlport

# Modules that need get installed if and only if the target libbcc.so is
# installed.
LOCAL_REQUIRED_MODULES := libclcore.bc libbcc.sha1

ifeq ($(ARCH_ARM_HAVE_NEON),true)
LOCAL_REQUIRED_MODULES += libclcore_neon.bc
endif

# Link-Time Optimization on libbcc.so
#
# -Wl,--exclude-libs=ALL only applies to library archives. It would hide most
# of the symbols in this shared library. As a result, it reduced the size of
# libbcc.so by about 800k in 2010.
#
# Note that libLLVMBitReader:libLLVMCore:libLLVMSupport are used by
# pixelflinger2.

#LOCAL_LDFLAGS += -Wl,--exclude-libs=libmcldARMTarget:libmcldARMInfo:libmcldMipsTarget:libmcldMipsInfo:libmcldX86Target:libmcldX86Info:libmcldCodeGen:libmcldTarget:libmcldLDVariant:libmcldMC:libmcldSupport:libmcldLD:libmcldADT:libLLVMARMDisassembler:libLLVMARMAsmPrinter:libLLVMX86Disassembler:libLLVMX86AsmPrinter:libLLVMMipsDisassembler:libLLVMMipsAsmPrinter:libLLVMMCParser:libLLVMARMCodeGen:libLLVMARMDesc:libLLVMARMInfo:libLLVMX86CodeGen:libLLVMX86Desc:libLLVMX86Info:libLLVMX86Utils:libLLVMMipsCodeGen:libLLVMMipsDesc:libLLVMMipsInfo:libLLVMSelectionDAG:libLLVMAsmPrinter:libLLVMCodeGen:libLLVMLinker:libLLVMTarget:libLLVMMC:libLLVMScalarOpts:libLLVMInstCombine:libLLVMipo:libLLVMipa:libLLVMTransformUtils:libLLVMAnalysis

# Generate build information (Build time + Build git revision + Build Semi SHA1)
include $(LIBBCC_ROOT_PATH)/libbcc-gen-build-info.mk

include $(LIBBCC_DEVICE_BUILD_MK)
include $(BUILD_SHARED_LIBRARY)


#=====================================================================
# Host Shared Library libbcc
#=====================================================================

include $(CLEAR_VARS)

LOCAL_MODULE := libbcc
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_CLASS := SHARED_LIBRARIES
LOCAL_IS_HOST_MODULE := true

LOCAL_WHOLE_STATIC_LIBRARIES += $(libbcc_WHOLE_STATIC_LIBRARIES)

LOCAL_WHOLE_STATIC_LIBRARIES += librsloader

ifeq ($(libbcc_USE_DISASSEMBLER),1)
  LOCAL_WHOLE_STATIC_LIBRARIES += \
    libLLVMARMDisassembler \
    libLLVMMipsDisassembler \
    libLLVMX86Disassembler
endif

LOCAL_WHOLE_STATIC_LIBRARIES += \
  libmcldARMTarget \
  libmcldARMInfo \
  libmcldMipsTarget \
  libmcldMipsInfo \
  libmcldX86Target \
  libmcldX86Info

LOCAL_WHOLE_STATIC_LIBRARIES += $(libmcld_STATIC_LIBRARIES)

LOCAL_WHOLE_STATIC_LIBRARIES += \
  libLLVMARMAsmParser \
  libLLVMARMCodeGen \
  libLLVMARMDesc \
  libLLVMARMInfo

LOCAL_WHOLE_STATIC_LIBRARIES += \
  libLLVMMipsAsmParser \
  libLLVMMipsCodeGen \
  libLLVMMipsAsmPrinter \
  libLLVMMipsDesc \
  libLLVMMipsInfo

LOCAL_WHOLE_STATIC_LIBRARIES += \
  libLLVMX86AsmParser \
  libLLVMX86CodeGen \
  libLLVMX86Desc \
  libLLVMX86AsmPrinter \
  libLLVMX86Info \
  libLLVMX86Utils

LOCAL_WHOLE_STATIC_LIBRARIES += \
  libLLVMObject \
  libLLVMAsmPrinter \
  libLLVMBitWriter \
  libLLVMBitReader \
  libLLVMSelectionDAG \
  libLLVMCodeGen \
  libLLVMLinker \
  libLLVMArchive \
  libLLVMScalarOpts \
  libLLVMInstCombine \
  libLLVMipo \
  libLLVMipa \
  libLLVMVectorize \
  libLLVMInstrumentation \
  libLLVMTransformUtils \
  libLLVMAnalysis \
  libLLVMTarget \
  libLLVMMCParser \
  libLLVMMC \
  libLLVMCore \
  libLLVMSupport

LOCAL_STATIC_LIBRARIES += \
  libutils \
  libcutils

LOCAL_SHARED_LIBRARIES := libbcinfo

LOCAL_LDLIBS := -ldl -lpthread

# Generate build information (Build time + Build git revision + Build Semi SHA1)
include $(LIBBCC_ROOT_PATH)/libbcc-gen-build-info.mk

include $(LIBBCC_HOST_BUILD_MK)
include $(BUILD_HOST_SHARED_LIBRARY)


#=====================================================================
# Include Subdirectories
#=====================================================================
include $(call all-makefiles-under,$(LOCAL_PATH))
