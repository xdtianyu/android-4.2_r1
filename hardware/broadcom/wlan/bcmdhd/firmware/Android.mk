LOCAL_PATH := $(call my-dir)
ifeq ($(BOARD_WLAN_DEVICE_REV),bcm4329)
    include $(LOCAL_PATH)/bcm4329/Android.mk
endif
