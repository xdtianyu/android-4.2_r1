# Copyright 2011 The Android Open Source Project
#
LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_PREBUILT_EXECUTABLES := lint
LOCAL_MODULE_TAGS := optional

include $(BUILD_HOST_PREBUILT)

