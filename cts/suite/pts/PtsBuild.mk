#
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
#

# several makefiles for CTS merged for PTS

LOCAL_PATH:= $(call my-dir)

# New packages should be added here
PTS_TEST_PACKAGES := \
    PtsDeviceFilePerf \
    PtsDeviceUi \
    PtsDeviceDram


PTS_SUPPORT_PACKAGES := \
    TestDeviceSetup \
    PtsDeviceTaskswitchingAppA \
    PtsDeviceTaskswitchingAppB \
    PtsDeviceTaskswitchingControl \
    com.replica.replicaisland \
    PtsDeviceBrowserLauncher

PTS_HOST_CASES := \
    PtsHostBootup \
    PtsHostUi \
    PtsHostBrowser

PTS_HOST_LIBS := \
    $(HOST_OUT_JAVA_LIBRARIES)/ptscommonutilhost.jar \
    $(HOST_OUT_JAVA_LIBRARIES)/ptshostutil.jar

BUILD_PTS_PACKAGE := cts/suite/pts/build/test_package.mk
BUILD_PTS_HOST_JAVA_LIBRARY := cts/suite/pts/build/test_host_java_library.mk

PTS_JAVA_TEST_SCANNER := $(HOST_OUT_EXECUTABLES)/cts-java-scanner
PTS_JAVA_TEST_SCANNER_DOCLET := $(HOST_OUT_JAVA_LIBRARIES)/cts-java-scanner-doclet.jar

# Generator of test XMLs from scanner output.
PTS_XML_GENERATOR := $(HOST_OUT_EXECUTABLES)/cts-xml-generator

# File indicating which tests should be blacklisted due to problems.
PTS_EXPECTATIONS := cts/suite/pts/expectations/knownfailures.txt

PTS_TESTCASES_OUT := $(HOST_OUT)/pts-testcases

define pts-get-package-paths
	$(foreach pkg,$(1),$(PTS_TESTCASES_OUT)/$(pkg).apk)
endef

define pts-get-test-xmls
	$(foreach name,$(1),$(PTS_TESTCASES_OUT)/$(name).xml)
endef

define pts-get-lib-paths
	$(foreach lib,$(1),$(HOST_OUT_JAVA_LIBRARIES)/$(lib).jar)
endef

PTS_TEST_CASE_LIST := \
	$(PTS_SUPPORT_PACKAGES)

PTS_TEST_CASES := \
		$(call pts-get-package-paths,$(PTS_TEST_PACKAGES)) \
		$(call pts-get-lib-paths,$(PTS_HOST_CASES))

PTS_TEST_XMLS := \
    $(call pts-get-test-xmls,$(PTS_TEST_PACKAGES)) \
    $(call pts-get-test-xmls,$(PTS_HOST_CASES))

pts_dir := $(HOST_OUT)/pts
pts_tools_src_dir := cts/tools

pts_name := android-pts

DDMLIB_JAR := $(HOST_OUT_JAVA_LIBRARIES)/ddmlib-prebuilt.jar
junit_host_jar := $(HOST_OUT_JAVA_LIBRARIES)/junit.jar
HOSTTESTLIB_JAR := $(HOST_OUT_JAVA_LIBRARIES)/hosttestlib.jar
TF_JAR := $(HOST_OUT_JAVA_LIBRARIES)/tradefed-prebuilt.jar
PTS_TF_JAR := $(HOST_OUT_JAVA_LIBRARIES)/cts-tradefed.jar
PTS_TF_EXEC := $(HOST_OUT_EXECUTABLES)/pts-tradefed
PTS_TF_README := $(pts_tools_src_dir)/tradefed-host/README


DEFAULT_TEST_PLAN := $(pts_dir)/$(pts_name)/resource/plans/PTS.xml

$(pts_dir)/all_pts_files_stamp: PRIVATE_JUNIT_HOST_JAR := $(junit_host_jar)

$(pts_dir)/all_pts_files_stamp: $(PTS_TEST_CASES) $(PTS_TEST_CASE_LIST) $(junit_host_jar) $(HOSTTESTLIB_JAR) $(PTS_HOST_LIBRARY_JARS) $(TF_JAR) $(VMTESTSTF_JAR) $(PTS_TF_JAR) $(PTS_TF_EXEC) $(PTS_TF_README) $(ACP) $(PTS_HOST_LIBS)
# Make necessary directory for PTS
	$(hide) rm -rf $(PRIVATE_PTS_DIR)
	$(hide) mkdir -p $(TMP_DIR)
	$(hide) mkdir -p $(PRIVATE_DIR)/docs
	$(hide) mkdir -p $(PRIVATE_DIR)/tools
	$(hide) mkdir -p $(PRIVATE_DIR)/repository/testcases
	$(hide) mkdir -p $(PRIVATE_DIR)/repository/plans
# Copy executable and JARs to PTS directory
	$(hide) $(ACP) -fp $(DDMLIB_JAR) $(PRIVATE_JUNIT_HOST_JAR) $(HOSTTESTLIB_JAR) $(PTS_HOST_LIBRARY_JARS) $(TF_JAR) $(PTS_TF_JAR) $(PTS_TF_EXEC) $(PTS_TF_README) $(PTS_HOST_LIBS) $(PRIVATE_DIR)/tools
# Change mode of the executables
	$(foreach apk,$(PTS_TEST_CASE_LIST),$(call copy-testcase-apk,$(apk)))
	$(foreach testcase,$(PTS_TEST_CASES),$(call copy-testcase,$(testcase)))
	$(hide) touch $@

# Generate the default test plan for User.
# Usage: buildCts.py <testRoot> <ctsOutputDir> <tempDir> <androidRootDir> <docletPath>

$(DEFAULT_TEST_PLAN): $(pts_dir)/all_pts_files_stamp $(pts_tools_src_dir)/utils/buildCts.py $(HOST_OUT_JAVA_LIBRARIES)/descGen.jar $(PTS_TEST_XMLS) | $(ACP)
	$(hide) $(ACP) -fp $(PTS_TEST_XMLS) $(PRIVATE_DIR)/repository/testcases
	$(hide) $(pts_tools_src_dir)/utils/buildCts.py cts/suite/pts $(PRIVATE_DIR) $(TMP_DIR) \
		$(TOP) $(HOST_OUT_JAVA_LIBRARIES)/descGen.jar -pts

# Package PTS and clean up.
INTERNAL_PTS_TARGET := $(pts_dir)/$(pts_name).zip
$(INTERNAL_PTS_TARGET): PRIVATE_NAME := $(pts_name)
$(INTERNAL_PTS_TARGET): PRIVATE_PTS_DIR := $(pts_dir)
$(INTERNAL_PTS_TARGET): PRIVATE_DIR := $(pts_dir)/$(pts_name)
$(INTERNAL_PTS_TARGET): TMP_DIR := $(pts_dir)/temp
$(INTERNAL_PTS_TARGET): $(pts_dir)/all_pts_files_stamp $(DEFAULT_TEST_PLAN)
	$(hide) echo "Package PTS: $@"
	$(hide) cd $(dir $@) && zip -rq $(notdir $@) $(PRIVATE_NAME)

.PHONY: pts
pts: $(INTERNAL_PTS_TARGET)
cts: pts
# generate PTS during CTS build
ifneq ($(filter cts, $(MAKECMDGOALS)),)
$(call dist-for-goals,cts,$(INTERNAL_PTS_TARGET))
endif

define copy-testcase-apk

$(hide) $(ACP) -fp $(call intermediates-dir-for,APPS,$(1))/package.apk \
	$(PRIVATE_DIR)/repository/testcases/$(1).apk

endef

define copy-testcase

$(hide) $(ACP) -fp $(1) $(PRIVATE_DIR)/repository/testcases/$(notdir $1)

endef
