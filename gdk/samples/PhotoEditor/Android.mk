LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_PACKAGE_NAME := PhotoEditor

LOCAL_JNI_SHARED_LIBRARIES := libjni_photoeditor

LOCAL_REQUIRED_MODULES := libjni_photoeditor

LOCAL_SDK_VERSION := 11

include $(BUILD_PACKAGE)

ifeq ($(strip $(LOCAL_PACKAGE_OVERRIDES)),)
include $(call all-makefiles-under, $(LOCAL_PATH))
endif
