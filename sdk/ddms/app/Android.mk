# Copyright 2007 The Android Open Source Project
#
LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(call all-java-files-under, src)
LOCAL_JAVA_RESOURCE_DIRS := src

LOCAL_JAR_MANIFEST := etc/manifest.txt

# If the dependency list is changed, etc/manifest.txt
# MUST be updated as well (Except for swt.jar which is dynamically
# added based on whether the VM is 32 or 64 bit)
LOCAL_JAVA_LIBRARIES := \
	common \
	sdkstats \
	ddmlib \
	ddmuilib \
	swt \
	swtmenubar \
	org.eclipse.jface_3.6.2.M20110210-1200 \
	org.eclipse.equinox.common_3.6.0.v20100503 \
	org.eclipse.core.commands_3.6.0.I20100512-1500
LOCAL_MODULE := ddms

include $(BUILD_HOST_JAVA_LIBRARY)


# Build all sub-directories
include $(call all-makefiles-under,$(LOCAL_PATH))
