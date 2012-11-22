LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := plasma_portable
LOCAL_SRC_FILES := plasma.cpp

LOCAL_CFLAGS := -D__GDK__

include $(BUILD_BITCODE)
