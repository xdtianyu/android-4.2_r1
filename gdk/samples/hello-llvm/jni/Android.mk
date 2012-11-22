LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := libhello_llvm

LOCAL_MODULE_TAGS := optional

LOCAL_CFLAGS    := -D NUM=7788

LOCAL_SRC_FILES := hello_llvm.c test.cpp

include $(BUILD_SHARED_LIBRARY)
