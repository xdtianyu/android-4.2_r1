LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)
LOCAL_SRC_FILES:= ping.c
LOCAL_MODULE := ping
include $(BUILD_EXECUTABLE)
