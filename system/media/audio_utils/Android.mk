LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := libaudioutils
LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES:= \
	fixedfft.cpp.arm \
	primitives.c \
	resampler.c \
	echo_reference.c

LOCAL_C_INCLUDES += $(call include-path-for, speex)
LOCAL_C_INCLUDES += \
	$(call include-path-for, speex) \
	$(call include-path-for, audio-utils)

LOCAL_SHARED_LIBRARIES := \
	libcutils \
	libspeexresampler

include $(BUILD_SHARED_LIBRARY)
