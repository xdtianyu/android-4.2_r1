# This directory contains various host tests to be used with the emulator
# NOTE: Most of these are only built and run on Linux.

LOCAL_PATH := $(call my-dir)

# The test-qemud-pipes program is used to check the execution of QEMUD Pipes
# See external/qemu/docs/ANDROID-QEMUD-PIPES.TXT for details.
#
ifeq ($(HOST_OS),XXXXlinux)

include $(CLEAR_VARS)
LOCAL_MODULE     := test-qemud-pipes
LOCAL_SRC_FILES  := test-qemud-pipes.c
LOCAL_MODULE_TAGS := debug
include $(BUILD_HOST_EXECUTABLE)

endif # HOST_OS == linux