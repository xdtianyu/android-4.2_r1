# Copyright (C) 2009 The Android Open Source Project
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

# this file is included repeatedly from build/core/setup-abi.mk and is used
# to setup the target toolchain for a given platform/abi combination.
#

$(call assert-defined,TARGET_PLATFORM TARGET_ARCH TARGET_ARCH_ABI)
$(call assert-defined,GDK_APPS)

# Check that we have a toolchain that supports the current ABI.
# NOTE: If GDK_TOOLCHAIN is defined, we're going to use it.
#
ifndef GDK_TOOLCHAIN
    TARGET_TOOLCHAIN_LIST := $(strip $(sort $(GDK_ABI.$(TARGET_ARCH_ABI).toolchains)))
    ifndef TARGET_TOOLCHAIN_LIST
        $(call __gdk_info,There is no toolchain that supports the $(TARGET_ARCH_ABI) ABI.)
        $(call __gdk_info,Please modify the APP_ABI definition in $(GDK_APP_APPLICATION_MK) to use)
        $(call __gdk_info,a set of the following values: $(GDK_ALL_ABIS))
        $(call __gdk_error,Aborting)
    endif
    # Select the last toolchain from the sorted list.
    # For now, this is enough to select armeabi-4.4.0 by default for ARM
    TARGET_TOOLCHAIN := $(lastword $(TARGET_TOOLCHAIN_LIST))
    $(call gdk_log,Using target toolchain '$(TARGET_TOOLCHAIN)' for '$(TARGET_ARCH_ABI)' ABI)
else # GDK_TOOLCHAIN is not empty
    TARGET_TOOLCHAIN_LIST := $(strip $(filter $(GDK_TOOLCHAIN),$(GDK_ABI.$(TARGET_ARCH_ABI).toolchains)))
    ifndef TARGET_TOOLCHAIN_LIST
        $(call __gdk_info,The selected toolchain ($(GDK_TOOLCHAIN)) does not support the $(TARGET_ARCH_ABI) ABI.)
        $(call __gdk_info,Please modify the APP_ABI definition in $(GDK_APP_APPLICATION_MK) to use)
        $(call __gdk_info,a set of the following values: $(GDK_TOOLCHAIN.$(GDK_TOOLCHAIN).abis))
        $(call __gdk_info,Or change your GDK_TOOLCHAIN definition.)
        $(call __gdk_error,Aborting)
    endif
    TARGET_TOOLCHAIN := $(GDK_TOOLCHAIN)
endif # GDK_TOOLCHAIN is not empty

TARGET_ABI := $(TARGET_PLATFORM)-$(TARGET_ARCH_ABI)

# setup sysroot-related variables. The SYSROOT point to a directory
# that contains all public header files for a given platform, plus
# some libraries and object files used for linking the generated
# target files properly.
#
SYSROOT := $(GDK_PLATFORMS_ROOT)/$(TARGET_PLATFORM)/arch-$(TARGET_ARCH)

# Define default values for TOOLCHAIN_NAME, this can be overriden in
# the setup file.
TOOLCHAIN_NAME   := $(TARGET_TOOLCHAIN)

# Define the root path of the toolchain in the GDK tree.
TOOLCHAIN_ROOT   := $(GDK_ROOT)/toolchains/$(TOOLCHAIN_NAME)

# Define the root path where toolchain prebuilts are stored
TOOLCHAIN_PREBUILT_ROOT := $(TOOLCHAIN_ROOT)/prebuilt/bin

# now call the toolchain-specific setup script
include $(GDK_TOOLCHAIN.$(TARGET_TOOLCHAIN).setup)

# compute GDK_APP_DST_DIR as the destination directory for the generated files
GDK_APP_DST_DIR := $(GDK_APP_PROJECT_PATH)/res/raw

clean-installed-binaries::

# free the dictionary of LOCAL_MODULE definitions
$(call modules-clear)

# now parse the Android.mk for the application, this records all
# module declarations, but does not populate the dependency graph yet.
include $(GDK_APP_BUILD_SCRIPT)

# now, really build the modules, the second pass allows one to deal
# with exported values
$(foreach __pass2_module,$(__gdk_modules),\
    $(eval LOCAL_MODULE := $(__pass2_module))\
    $(eval include $(BUILD_SYSTEM)/build-binary.mk)\
)

# Now compute the closure of all module dependencies.
#
# If APP_MODULES is not defined in the Application.mk, we
# will build all modules that were listed from the top-level Android.mk
#
ifeq ($(strip $(GDK_APP_MODULES)),)
    WANTED_MODULES := $(call modules-get-top-list)
else
    WANTED_MODULES := $(call module-get-all-dependencies,$(GDK_APP_MODULES))
endif

WANTED_INSTALLED_MODULES += $(call map,module-get-installed,$(WANTED_MODULES))
