LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := hello_llvm
LOCAL_CFLAGS    := -D NUM=7788
LOCAL_SRC_FILES := hello_llvm.c test.cpp
LOCAL_C_INCLUDES := jni/test-include

include $(BUILD_BITCODE)


include $(CLEAR_VARS)

LOCAL_MODULE    := test2
LOCAL_SRC_FILES := test2.c

include $(BUILD_BITCODE)
