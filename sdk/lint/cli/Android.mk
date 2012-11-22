# Copyright 2011 The Android Open Source Project
#
LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(call all-java-files-under, src)
LOCAL_JAVA_RESOURCE_DIRS := src

LOCAL_JAR_MANIFEST := etc/manifest.txt

# If the dependency list is changed, etc/manifest.txt
LOCAL_JAVA_LIBRARIES := \
	common \
	sdklib \
	lint_api \
	lint_checks \
	lombok-ast-0.2 \
	asm-tools \
	asm-tree-tools \
	asm-analysis-tools \
	guava-tools

LOCAL_MODULE := lint
LOCAL_MODULE_TAGS := optional

include $(BUILD_HOST_JAVA_LIBRARY)


# Build all sub-directories
include $(call all-makefiles-under,$(LOCAL_PATH))
