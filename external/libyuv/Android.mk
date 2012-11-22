# This is the Android makefile for google3/third_party/libsrtp so that we can
# build it with the Android NDK.
ifeq ($(TARGET_ARCH),arm)

LOCAL_PATH := $(call my-dir)

common_SRC_FILES := \
    files/source/convert.cc \
    files/source/format_conversion.cc \
    files/source/planar_functions.cc \
    files/source/row_posix.cc \
    files/source/video_common.cc \
    files/source/cpu_id.cc \
    files/source/general.cc \
    files/source/rotate.cc \
    files/source/row_table.cc \
    files/source/scale.cc

common_CFLAGS := -Wall -fexceptions

common_C_INCLUDES = $(LOCAL_PATH)/files/include

# For the device
# =====================================================
# Device static library

include $(CLEAR_VARS)

LOCAL_CPP_EXTENSION := .cc

LOCAL_SDK_VERSION := 9
LOCAL_NDK_STL_VARIANT := stlport_static

LOCAL_SRC_FILES := $(common_SRC_FILES)
LOCAL_CFLAGS += $(common_CFLAGS)
LOCAL_C_INCLUDES += $(common_C_INCLUDES)

LOCAL_MODULE:= libyuv_static
LOCAL_MODULE_TAGS := optional

include $(BUILD_STATIC_LIBRARY)

endif
