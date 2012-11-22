# Copyright 2011 The Android Open Source Project
#
LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_JAVA_RESOURCE_DIRS :=

LOCAL_SRC_FILES := $(call all-java-files-under,src)

LOCAL_JAR_MANIFEST := etc/manifest.txt
LOCAL_JAVA_LIBRARIES :=  \
	common \
	sdklib
LOCAL_MODULE := manifmerger
LOCAL_MODULE_TAGS := optional

include $(BUILD_HOST_JAVA_LIBRARY)
