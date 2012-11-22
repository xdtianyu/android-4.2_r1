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

# To integrate with the nightly build, we use
#   LOCAL_MODULE_PATH := $(TARGET_OUT_DATA_APPS)
# to put the tests under /data and not /system/bin (default).

LOCAL_PATH := $(call my-dir)

libastl_test_includes:= \
	bionic/libstdc++/include \
	external/astl/include
libastl_test_static_lib := libastl
libastl_test_host_static_lib := libastl_host

# $(3) and $(5) must be set or cleared in sync. $(3) is used to
# generate the right make target (host vs device). $(5) is used in the
# module's name to have different name for the host vs device
# builds. Also $(5) is used to pickup the right set of libraries,
# typically the host libs have a _host suffix in their names.
# $(1): source list
# $(2): tags
# $(3): "HOST_" or empty
# $(4): extra CFLAGS or empty
# $(5): "_host" or empty
define _define-test
$(foreach file,$(1), \
  $(eval include $(CLEAR_VARS)) \
  $(eval LOCAL_SRC_FILES := $(file)) \
  $(eval LOCAL_C_INCLUDES := $(libastl_test_includes)) \
  $(eval LOCAL_MODULE := $(notdir $(file:%.cpp=%))$(5)) \
  $(eval LOCAL_CFLAGS += $(4)) \
  $(eval LOCAL_STATIC_LIBRARIES := $(libastl_test$(5)_static_lib)) \
  $(eval LOCAL_MODULE_TAGS := $(2) ) \
  $(eval $(if $(3),,LOCAL_MODULE_PATH := $(TARGET_OUT_DATA_APPS))) \
  $(eval include $(BUILD_$(3)EXECUTABLE)) \
)
endef

ifeq ($(HOST_OS),linux)
# Compile using the host only on linux for valgrind support.
define host-test
$(call _define-test,$(1),optional,HOST_,-O0 -g,_host)
endef
endif

define target-test
$(call _define-test,$(1),eng tests)
endef

sources := \
   test_algorithm.cpp \
   test_char_traits.cpp \
   test_functional.cpp \
   test_ios_base.cpp \
   test_iomanip.cpp \
   test_ios_pos_types.cpp \
   test_iostream.cpp \
   test_iterator.cpp \
   test_limits.cpp \
   test_list.cpp \
   test_memory.cpp \
   test_set.cpp \
   test_sstream.cpp \
   test_streambuf.cpp \
   test_string.cpp \
   test_type_traits.cpp \
   test_uninitialized.cpp \
   test_vector.cpp

ifeq ($(HOST_OS),linux)
$(call host-test, $(sources))
endif

$(call device-test, $(sources))
