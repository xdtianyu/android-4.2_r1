# Copyright 2012 The Android Open Source Project
#
LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_PREBUILT_EXECUTABLES := device_validator
LOCAL_MODULE_TAGS := optional

include $(BUILD_HOST_PREBUILT)

