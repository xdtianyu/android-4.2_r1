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

# copied from cts/build. modified for PTS

# Disable by default so "m pts" will work in emulator builds
LOCAL_DEX_PREOPT := false

include $(BUILD_PACKAGE)

pts_package_apk := $(PTS_TESTCASES_OUT)/$(LOCAL_PACKAGE_NAME).apk
pts_package_xml := $(PTS_TESTCASES_OUT)/$(LOCAL_PACKAGE_NAME).xml

$(pts_package_apk): PRIVATE_PACKAGE := $(LOCAL_PACKAGE_NAME)
$(pts_package_apk): $(call intermediates-dir-for,APPS,$(LOCAL_PACKAGE_NAME))/package.apk | $(ACP)
	$(hide) mkdir -p $(PTS_TESTCASES_OUT)
	$(hide) $(ACP) -fp $< $@

$(pts_package_xml): PRIVATE_PATH := $(LOCAL_PATH)
$(pts_package_xml): PRIVATE_INSTRUMENTATION := $(LOCAL_INSTRUMENTATION_FOR)
$(pts_package_xml): PRIVATE_PACKAGE := $(LOCAL_PACKAGE_NAME)
$(pts_package_xml): PRIVATE_TEST_PACKAGE := com.android.pts.$(notdir $(LOCAL_PATH))
$(pts_package_xml): PRIVATE_MANIFEST := $(LOCAL_PATH)/AndroidManifest.xml
$(pts_package_xml): PRIVATE_TEST_TYPE := $(if $(LOCAL_PTS_TEST_RUNNER),$(LOCAL_PTS_TEST_RUNNER),'')
$(pts_package_xml): $(call intermediates-dir-for,APPS,$(LOCAL_PACKAGE_NAME))/package.apk $(PTS_EXPECTATIONS) $(PTS_JAVA_TEST_SCANNER_DOCLET) $(PTS_JAVA_TEST_SCANNER) $(PTS_XML_GENERATOR)
	$(hide) echo Generating test description for java package $(PRIVATE_PACKAGE)
	$(hide) mkdir -p $(PTS_TESTCASES_OUT)
	$(hide) $(PTS_JAVA_TEST_SCANNER) \
						-s $(PRIVATE_PATH) \
						-d $(PTS_JAVA_TEST_SCANNER_DOCLET) | \
			$(PTS_XML_GENERATOR) \
						-t $(PRIVATE_TEST_TYPE) \
						-m $(PRIVATE_MANIFEST) \
						-i "$(PRIVATE_INSTRUMENTATION)" \
						-n $(PRIVATE_PACKAGE) \
						-p $(PRIVATE_TEST_PACKAGE) \
						-e $(PTS_EXPECTATIONS) \
						-o $@
