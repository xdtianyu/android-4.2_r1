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

ifeq ($(TARGET_BOARD_PLATFORM),omap4)
ifeq ($(BOARD_USES_SECURE_SERVICES),true)

LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := keystore.tuna

LOCAL_MODULE_PATH := $(TARGET_OUT_SHARED_LIBRARIES)/hw

LOCAL_SRC_FILES := \
	keymaster_tuna.cpp

LOCAL_C_INCLUDES := \
	libcore/include \
	external/openssl/include \
	hardware/ti/omap4xxx/security/tf_sdk/include

LOCAL_CFLAGS := -fvisibility=hidden -Wall -Werror

LOCAL_SHARED_LIBRARIES := libcutils liblog libcrypto libtf_crypto_sst

LOCAL_MODULE_TAGS := optional

include $(BUILD_SHARED_LIBRARY)

endif # ifeq ($(BOARD_USES_SECURE_SERVICES),true)
endif # ifeq ($(TARGET_BOARD_PLATFORM),omap4)
