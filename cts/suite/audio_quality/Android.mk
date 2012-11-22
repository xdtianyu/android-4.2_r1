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

# build only for linux
ifeq ($(HOST_OS),linux)

CTS_AUDIO_TOP:= $(call my-dir)

CTS_AUDIO_INSTALL_DIR := $(HOST_OUT)/cts-audio-quality/android-cts-audio-quality
CTS_AUDIO_QUALITY_ZIP := $(HOST_OUT)/cts-audio-quality/android-cts-audio-quality.zip

$(CTS_AUDIO_QUALITY_ZIP): cts_audio_quality_test cts_audio_quality \
  CtsAudioClient $(CTS_AUDIO_TOP)/test_description
	$(hide) mkdir -p $(CTS_AUDIO_INSTALL_DIR)
	$(hide) mkdir -p $(CTS_AUDIO_INSTALL_DIR)/client
	$(hide) $(ACP) -fp $(PRODUCT_OUT)/data/app/CtsAudioClient.apk \
        $(CTS_AUDIO_INSTALL_DIR)/client
	$(hide) $(ACP) -fp $(HOST_OUT)/bin/cts_audio_quality_test $(CTS_AUDIO_INSTALL_DIR)
	$(hide) $(ACP) -fp $(HOST_OUT)/bin/cts_audio_quality $(CTS_AUDIO_INSTALL_DIR)
	$(hide) $(ACP) -fr $(CTS_AUDIO_TOP)/test_description $(CTS_AUDIO_INSTALL_DIR)
	$(hide) echo "Package cts_audio: $@"
	$(hide) cd $(HOST_OUT)/cts-audio-quality && \
        zip -rq android-cts-audio-quality.zip android-cts-audio-quality -x android-cts-audio-quality/reports/\*

# target to build only this package
.PHONY: cts_audio_quality_package
cts_audio_quality_package: $(CTS_AUDIO_QUALITY_ZIP)

cts: $(CTS_AUDIO_QUALITY_ZIP)

ifneq ($(filter cts, $(MAKECMDGOALS)),)
$(call dist-for-goals, cts, $(CTS_AUDIO_QUALITY_ZIP))
endif # cts

include $(call all-subdir-makefiles)

endif # linux
