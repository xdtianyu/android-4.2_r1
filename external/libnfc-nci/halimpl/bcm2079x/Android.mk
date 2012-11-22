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


# function to find all *.cpp files under a directory
define all-cpp-files-under
$(patsubst ./%,%, \
  $(shell cd $(LOCAL_PATH) ; \
          find $(1) -name "*.cpp" -and -not -name ".*") \
 )
endef


HAL_SUFFIX := $(TARGET_DEVICE)
ifeq ($(TARGET_DEVICE),crespo)
	HAL_SUFFIX := herring
endif


######################################
######################################
# build shared library system/lib/hw/nfc_nci.*.so
# which is linked by libhardware.so


LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)
LOCAL_MODULE := nfc_nci.$(HAL_SUFFIX)
LOCAL_MODULE_PATH := $(TARGET_OUT_SHARED_LIBRARIES)/hw
LOCAL_SRC_FILES := $(call all-c-files-under, .)  $(call all-cpp-files-under, .)
LOCAL_SHARED_LIBRARIES := liblog libcutils libhardware_legacy libstlport
LOCAL_MODULE_TAGS := optional

LOCAL_C_INCLUDES += external/stlport/stlport  bionic/  bionic/libstdc++/include \
	$(LOCAL_PATH)/include \
	$(LOCAL_PATH)/gki/ulinux \
 	$(LOCAL_PATH)/gki/common \
	$(LOCAL_PATH)/udrv/include \
	$(LOCAL_PATH)/hal/include \
	$(LOCAL_PATH)/hal/int

LOCAL_CFLAGS += -DANDROID \
	-DBUILDCFG=1 -DNFC_HAL_TARGET=TRUE -DNFC_RW_ONLY=TRUE

include $(BUILD_SHARED_LIBRARY)
