LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

#LOCAL_SHARED_LIBRARIES := libm liblog libjnigraphics
#LOCAL_LDLIBS := -lm -llog -ljnigraphics -lbcc

LOCAL_MODULE_TAGS := optional

LOCAL_MODULE := libjni_photoeditor_portable

LOCAL_SRC_FILES := _jni.cpp \
    utils.cpp \
    backlight.cpp \
    blur.cpp \
    colortemp.cpp \
    convolution.cpp \
    copy.cpp \
    crossprocess.cpp \
    duotone.cpp \
    fisheye.cpp \
    flip.cpp \
    grain.cpp \
    grayscale.cpp \
    heq.cpp \
    negative.cpp \
    quantize.cpp \
    redeye.cpp \
    saturate.cpp \
    sepia.cpp \
    sharpen.cpp \
    tint.cpp \
    vignetting.cpp \
    warmify.cpp \
    whiteblack.cpp

# This doesn't work on non-ARM yet.
ifeq ($(TARGET_ARCH), arm)
    LOCAL_SDK_VERSION := 9
endif

LOCAL_CFLAGS := -D__GDK__

LOCAL_C_INCLUDES := $(OUT)/../../../../frameworks/compile/libbcc/include
#LOCAL_LDFLAGS := -L$(OUT)/system/lib

include $(BUILD_BITCODE)
