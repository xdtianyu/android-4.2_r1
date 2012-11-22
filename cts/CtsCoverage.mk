#
# Copyright (C) 2010 The Android Open Source Project
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

# Makefile for producing CTS coverage reports.
# Run "make cts-test-coverage" in the $ANDROID_BUILD_TOP directory.

include cts/CtsTestCaseList.mk

cts_api_coverage_exe := $(HOST_OUT_EXECUTABLES)/cts-api-coverage
dexdeps_exe := $(HOST_OUT_EXECUTABLES)/dexdeps

coverage_out := $(HOST_OUT)/cts-api-coverage

api_text_description := $(SRC_API_DIR)/current.txt
api_xml_description := $(coverage_out)/api.xml
$(api_xml_description) : $(api_text_description) $(APICHECK)
	$(hide) echo "Converting API file to XML: $@"
	$(hide) mkdir -p $(coverage_out)
	$(hide) $(APICHECK_COMMAND) -convert2xml $(api_text_description) $(api_xml_description)

cts-test-coverage-report := $(coverage_out)/test-coverage.html
cts-verifier-coverage-report := $(coverage_out)/verifier-coverage.html
cts-combined-coverage-report := $(coverage_out)/combined-coverage.html

cts_api_coverage_dependencies := $(cts_api_coverage_exe) $(dexdeps_exe) $(api_xml_description) $(ACP)

$(cts-test-coverage-report) : $(CTS_COVERAGE_TEST_CASE_LIST) $(cts_api_coverage_dependencies)
	$(call generate-coverage-report,"CTS Tests API Coverage Report",\
			$(CTS_COVERAGE_TEST_CASE_LIST),cts-test-apks,html,test-coverage.html)

$(cts-verifier-coverage-report) : CtsVerifier $(cts_api_coverage_dependencies)
	$(call generate-coverage-report,"CTS Verifier API Coverage Report",\
			CtsVerifier,cts-verifier-apks,html,verifier-coverage.html)

$(cts-combined-coverage-report) : CtsVerifier $(cts_api_coverage_dependencies) $(CTS_COVERAGE_TEST_CASE_LIST) $(cts_api_coverage_dependencies)
	$(call generate-coverage-report,"CTS Combined API Coverage Report",\
			$(CTS_COVERAGE_TEST_CASE_LIST) CtsVerifier,cts-combined-apks,html,combined-coverage.html)

.PHONY: cts-test-coverage
cts-test-coverage : $(cts-test-coverage-report)

.PHONY: cts-verifier-coverage
cts-verifier-coverage : $(cts-verifier-coverage-report)

.PHONY: cts-combined-coverage
cts-combined-coverage : $(cts-combined-coverage-report)

# Put the test coverage report in the dist dir if "cts" is among the build goals.
ifneq ($(filter cts, $(MAKECMDGOALS)),)
  $(call dist-for-goals, cts, $(cts-test-coverage-report):cts-test-coverage-report.html)
  $(call dist-for-goals, cts, $(cts-verifier-coverage-report):cts-verifier-coverage-report.html)
  $(call dist-for-goals, cts, $(cts-combined-coverage-report):cts-combined-coverage-report.html)
endif

# Arguments;
#  1 - Name of the report printed out on the screen
#  2 - Name of APK packages that will be scanned to generate the report
#  3 - Name of variable to hold the calculated paths of the APKs
#  4 - Format of the report
#  5 - Output file name of the report
define generate-coverage-report
	$(foreach testcase,$(2),$(eval $(call add-testcase-apk,$(3),$(testcase))))
	$(hide) mkdir -p $(coverage_out)
	$(hide) $(cts_api_coverage_exe) -d $(dexdeps_exe) -a $(api_xml_description) -f $(4) -o $(coverage_out)/$(5) $($(3))
	$(hide) echo $(1): file://$(ANDROID_BUILD_TOP)/$(coverage_out)/$(5)
endef

# classes.dex is stripped from package.apk if dex-preopt is enabled,
# so we use the copy that definitely includes classes.dex.
define add-testcase-apk
	$(1) += $(call intermediates-dir-for,APPS,$(2))/package.apk.unaligned
endef
