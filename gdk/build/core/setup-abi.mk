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

# this file is included multiple times by build/core/setup-app.mk
#

$(call gdk_log,Building application '$(GDK_APP_NAME)' for ABI '$(TARGET_ARCH_ABI)')

# Map ABIs to a target architecture
TARGET_ARCH_for_llvm := llvm

TARGET_ARCH := $(TARGET_ARCH_for_$(TARGET_ARCH_ABI))

TARGET_OUT  := $(GDK_APP_OUT)/$(_app)/$(TARGET_ARCH_ABI)

# Separate the debug and release objects. This prevents rebuilding
# everything when you switch between these two modes. For projects
# with lots of C++ sources, this can be a considerable time saver.
ifeq ($(GDK_APP_OPTIM),debug)
TARGET_OBJS := $(TARGET_OUT)/objs-debug
else
TARGET_OBJS := $(TARGET_OUT)/objs
endif

include $(BUILD_SYSTEM)/setup-toolchain.mk
