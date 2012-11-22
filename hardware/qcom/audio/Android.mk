ifneq ($(filter msm8960,$(TARGET_BOARD_PLATFORM)),)

AUDIO_ROOT := $(call my-dir)
include $(call all-subdir-makefiles)

endif
