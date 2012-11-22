LOCAL_PATH := $(call my-dir)

ifneq (,$(findstring $(TARGET_DEVICE),toroplus))

# Edify extension functions for doing modem (radio) updates on toroplus devices.

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional
LOCAL_C_INCLUDES += bootable/recovery
LOCAL_SRC_FILES := recovery_updater.c update_cdma_modem.c

# should match TARGET_RECOVERY_UPDATER_LIBS set in BoardConfig.mk
LOCAL_MODULE := librecovery_updater_toroplus

include $(BUILD_STATIC_LIBRARY)

endif
