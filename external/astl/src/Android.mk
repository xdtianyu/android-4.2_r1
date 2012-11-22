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
#

LOCAL_PATH := $(call my-dir)

astl_common_src_files := \
    basic_ios.cpp \
    ios_base.cpp \
    ios_globals.cpp \
    ios_pos_types.cpp \
    list.cpp \
    ostream.cpp \
    sstream.cpp \
    stdio_filebuf.cpp \
    streambuf.cpp \
    string.cpp

# Target build
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(astl_common_src_files)
LOCAL_C_INCLUDES := external/astl/include
LOCAL_CFLAGS += -I bionic/libstdc++/include -I external/astl/include
LOCAL_MODULE:= libastl

include $(BUILD_STATIC_LIBRARY)

# On linux we build a host version of the lib to run under valgrind.
ifeq ($(HOST_OS),linux)
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(astl_common_src_files)
LOCAL_C_INCLUDES := external/astl/include
LOCAL_CFLAGS += -I bionic/libstdc++/include -I external/astl/include
LOCAL_MODULE:= libastl_host

include $(BUILD_HOST_STATIC_LIBRARY)
endif
