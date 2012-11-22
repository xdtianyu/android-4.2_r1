LOCAL_PATH := $(my-dir)
TARGET_OUT_WLAN_FW := $(TARGET_OUT_ETC)/firmware/ti-connectivity

# WLAN FW file for wl1271
include $(CLEAR_VARS)
LOCAL_MODULE := wl1271-fw-2.bin
LOCAL_MODULE_CLASS := ETC
LOCAL_MODULE_PATH := $(TARGET_OUT_WLAN_FW)
LOCAL_SRC_FILES := $(LOCAL_MODULE)
LOCAL_MODULE_TAGS := optional
include $(BUILD_PREBUILT)

# WLAN NVS file for wl1271
include $(CLEAR_VARS)
LOCAL_MODULE := wl1271-nvs.bin
LOCAL_MODULE_CLASS := ETC
LOCAL_MODULE_PATH := $(TARGET_OUT_WLAN_FW)
LOCAL_SRC_FILES := $(LOCAL_MODULE)
LOCAL_MODULE_TAGS := optional
include $(BUILD_PREBUILT)

#############################################
# Install from ti-wpan.tgz:
# TIInit_10.6.15.bts
# TIInit_7.2.31.bts
# TIInit_7.6.15.bts

ti-wpan.untarred_intermediates := $(call intermediates-dir-for, ETC, ti-wpan.untarred)
ti-wpan.untarred_timestamp := $(ti-wpan.untarred_intermediates)/stamp
ti-wpan.untarred_bluetooth_dir := $(ti-wpan.untarred_intermediates)/bluetooth

$(ti-wpan.untarred_timestamp) : $(LOCAL_PATH)/ti-wpan.tgz
	@echo "Unzip $(dir $@) <- $<)"
	$(hide) rm -rf $(dir $@) && mkdir -p $(dir $@)
	$(hide) tar -C $(dir $@) -zxf $<
	$(hide) touch $@


#############################################
include $(CLEAR_VARS)
LOCAL_MODULE := TIInit_10.6.15.bts
LOCAL_MODULE_CLASS := ETC
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_PATH := $(TARGET_OUT_ETC)/firmware

include $(BUILD_SYSTEM)/base_rules.mk

$(LOCAL_BUILT_MODULE): PRIVATE_SRC := $(ti-wpan.untarred_bluetooth_dir)/$(LOCAL_MODULE)
$(LOCAL_BUILT_MODULE) : $(ti-wpan.untarred_timestamp) | $(ACP)
	@echo "Copy $@ <- $(PRIVATE_SRC)"
	@mkdir -p $(dir $@)
	$(hide) $(ACP) -fp $(PRIVATE_SRC) $@

#############################################
include $(CLEAR_VARS)
LOCAL_MODULE := TIInit_7.2.31.bts
LOCAL_MODULE_CLASS := ETC
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_PATH := $(TARGET_OUT_ETC)/firmware

include $(BUILD_SYSTEM)/base_rules.mk

$(LOCAL_BUILT_MODULE): PRIVATE_SRC := $(ti-wpan.untarred_bluetooth_dir)/$(LOCAL_MODULE)
$(LOCAL_BUILT_MODULE) : $(ti-wpan.untarred_timestamp) | $(ACP)
	@echo "Copy $@ <- $(PRIVATE_SRC)"
	@mkdir -p $(dir $@)
	$(hide) $(ACP) -fp $(PRIVATE_SRC) $@

#############################################
include $(CLEAR_VARS)
LOCAL_MODULE := TIInit_7.6.15.bts
LOCAL_MODULE_CLASS := ETC
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_PATH := $(TARGET_OUT_ETC)/firmware

include $(BUILD_SYSTEM)/base_rules.mk

$(LOCAL_BUILT_MODULE): PRIVATE_SRC := $(ti-wpan.untarred_bluetooth_dir)/$(LOCAL_MODULE)
$(LOCAL_BUILT_MODULE) : $(ti-wpan.untarred_timestamp) | $(ACP)
	@echo "Copy $@ <- $(PRIVATE_SRC)"
	@mkdir -p $(dir $@)
	$(hide) $(ACP) -fp $(PRIVATE_SRC) $@

#############################################

# Clean up tmp vars
ti-wpan.untarred_intermediates :=
ti-wpan.untarred_timestamp :=
ti-wpan.untarred_bluetooth_dir :=
