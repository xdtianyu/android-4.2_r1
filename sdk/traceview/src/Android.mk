# Copyright 2007 The Android Open Source Project
#
LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_JAVA_RESOURCE_DIRS := resources

LOCAL_SRC_FILES := $(call all-subdir-java-files)
LOCAL_JAR_MANIFEST := ../etc/manifest.txt
LOCAL_JAVA_LIBRARIES := \
	common \
	sdkstats \
	swt \
	org.eclipse.jface_3.6.2.M20110210-1200 \
	org.eclipse.equinox.common_3.6.0.v20100503 \
	org.eclipse.core.commands_3.6.0.I20100512-1500
LOCAL_MODULE := traceview

include $(BUILD_HOST_JAVA_LIBRARY)
