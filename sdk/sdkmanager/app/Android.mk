# Copyright 2007 The Android Open Source Project
#
LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(call all-java-files-under, src)
LOCAL_JAVA_RESOURCE_DIRS := src

LOCAL_JAR_MANIFEST := etc/manifest.txt

# IMPORTANT: if you add a new dependency here, please make sure
# to also check the following files:
#   sdkmanager/app/etc/manifest.txt
#   sdkmanager/app/etc/android.bat
# (Note that we don't reference swt.jar in these files since
#  it is dynamically added by android.bat/.sh based on whether the
#  current VM is 32 or 64 bit.)
LOCAL_JAVA_LIBRARIES := \
	common \
	sdklib \
	sdkuilib \
	swt \
	org.eclipse.jface_3.6.2.M20110210-1200 \
	org.eclipse.equinox.common_3.6.0.v20100503 \
	org.eclipse.core.commands_3.6.0.I20100512-1500

LOCAL_MODULE := sdkmanager

include $(BUILD_HOST_JAVA_LIBRARY)

# Build all sub-directories
include $(call all-makefiles-under,$(LOCAL_PATH))

