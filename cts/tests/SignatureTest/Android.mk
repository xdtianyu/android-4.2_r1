# Copyright (C) 2008 The Android Open Source Project
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

LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

# don't include this package in any target
LOCAL_MODULE_TAGS := optional
# and when built explicitly put it in the data partition
LOCAL_MODULE_PATH := $(TARGET_OUT_DATA_APPS)
# and because it is in data, do not strip classes.dex
LOCAL_DEX_PREOPT := false

LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_JAVA_LIBRARIES := android.test.runner

LOCAL_PACKAGE_NAME := SignatureTest

LOCAL_SDK_VERSION := current

# To be passed in on command line
CTS_API_VERSION ?= current

android_api_description := $(SRC_API_DIR)/$(CTS_API_VERSION).txt

# Can't call local-intermediates-dir directly here because we have to
# include BUILD_PACAKGE first.  Can't include BUILD_PACKAGE first
# because we have to override LOCAL_RESOURCE_DIR first.  Hence this
# hack.
intermediates.COMMON := $(call intermediates-dir-for,APPS,$(LOCAL_PACKAGE_NAME),,COMMON)
signature_res_dir := $(intermediates.COMMON)/genres
LOCAL_RESOURCE_DIR := $(signature_res_dir)

# These shell commands allow us to get around the package.mk check for
# empty/non-existent resouce dirs (which ours would be).  If it finds
# an empty/non-existent resource dir, R_file_stamp doesn't contain a
# target and our stuff never gets copied.
# TODO: fix package.mk so we can avoid this hack
fake_resource_check := $(signature_res_dir)/raw/fake_resource_check

$(shell \
 if [ ! -f $(fake_resource_check) ]; then \
   mkdir -p $(dir $(fake_resource_check)); \
   touch $(fake_resource_check); \
 fi \
 )

include $(BUILD_PACKAGE)

copied_res_stamp := $(intermediates.COMMON)/copyres.stamp
generated_res_stamp := $(intermediates.COMMON)/genres.stamp
api_ver_path := $(intermediates.COMMON)
api_ver_file := $(api_ver_path)/api_ver_is_$(CTS_API_VERSION)

# The api_ver_file keeps track of which api version was last built.
# By only ever having one of these magic files in existance and making
# sure the generated resources rule depend on it, we can ensure that
# the proper version of the api resource gets generated.
$(api_ver_file):
	$(hide) rm -f $(api_ver_path)/api_ver_is_*
	$(hide) touch $@

android_api_xml_description := $(intermediates.COMMON)/api.xml
$(android_api_xml_description): PRIVATE_INPUT_FILE := $(android_api_description)
$(android_api_xml_description): $(android_api_description) $(APICHECK)
	$(hide) echo "Convert api file to xml: $@"
	$(hide) $(APICHECK_COMMAND) -convert2xml $(PRIVATE_INPUT_FILE) $@

static_res_deps := $(call find-subdir-assets,$(LOCAL_PATH)/res)
$(copied_res_stamp): PRIVATE_PATH := $(LOCAL_PATH)
$(copied_res_stamp): PRIVATE_MODULE := $(LOCAL_MODULE)
$(copied_res_stamp): PRIVATE_RES_DIR := $(signature_res_dir)
$(copied_res_stamp): FAKE_RESOURCE_DIR := $(dir $(fake_resource_check))
$(copied_res_stamp): FAKE_RESOURCE_CHECK := $(fake_resource_check)
$(copied_res_stamp): $(foreach res,$(static_res_deps),$(LOCAL_PATH)/res/${res}) | $(ACP)
	$(hide) echo "Copy resources: $(PRIVATE_MODULE)"
	$(hide) rm -f $@
	$(hide) rm -rf $(PRIVATE_RES_DIR)
	$(hide) mkdir -p $(PRIVATE_RES_DIR)
	$(hide) if [ ! -f $(FAKE_RESOURCE_CHECK) ]; \
	  then mkdir -p $(FAKE_RESOURCE_DIR); \
	  touch $(FAKE_RESOURCE_CHECK); \
	fi
	$(hide) $(ACP) -rd $(PRIVATE_PATH)/res/* $(PRIVATE_RES_DIR)/
	$(hide) touch $@

# Split up config/api/1.xml by "package" and save the files as the
# resource files of SignatureTest.
$(generated_res_stamp): PRIVATE_PATH := $(LOCAL_PATH)
$(generated_res_stamp): PRIVATE_MODULE := $(LOCAL_MODULE)
$(generated_res_stamp): PRIVATE_RES_DIR := $(signature_res_dir)
$(generated_res_stamp): $(api_ver_file)
$(generated_res_stamp): $(copied_res_stamp) $(android_api_xml_description)
	$(hide) echo "Copy generated resources: $(PRIVATE_MODULE)"
	$(hide) rm -f $@
	$(hide) python cts/tools/utils/android_api_description_splitter.py \
		$(android_api_xml_description) $(PRIVATE_RES_DIR) package
	$(hide) touch $@

$(R_file_stamp): $(generated_res_stamp) $(copied_res_stamp)

# Use the folloing include to make our test apk.
include $(call all-makefiles-under,$(LOCAL_PATH))
