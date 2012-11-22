LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := libplasma

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := plasma.cpp

LOCAL_C_INCLUDES := $(OUT)/../../../../frameworks/compile/libbcc/include

# Workaround. libbcc is not part of NDK
LOCAL_LDLIBS := -lm -llog -ljnigraphics -lbcc
LOCAL_LDFLAGS := -L$(OUT)/system/lib

include $(BUILD_SHARED_LIBRARY)
