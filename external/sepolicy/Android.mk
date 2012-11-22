ifeq ($(HAVE_SELINUX),true)

LOCAL_PATH:= $(call my-dir)

include $(call all-makefiles-under,$(LOCAL_PATH))

include $(CLEAR_VARS)

# SELinux policy version.
# Must be <= /selinux/policyvers reported by the Android kernel.
# Must be within the compatibility range reported by checkpolicy -V.
POLICYVERS ?= 24

MLS_SENS=1
MLS_CATS=1024

LOCAL_POLICY_DIRS := $(SRC_TARGET_DIR)/board/$(TARGET_DEVICE)/ device/*/$(TARGET_DEVICE)/ vendor/*/$(TARGET_DEVICE)/

LOCAL_POLICY_FC := $(wildcard $(addsuffix sepolicy.fc, $(LOCAL_POLICY_DIRS)))
LOCAL_POLICY_TE := $(wildcard $(addsuffix sepolicy.te, $(LOCAL_POLICY_DIRS)))
LOCAL_POLICY_PC := $(wildcard $(addsuffix sepolicy.pc, $(LOCAL_POLICY_DIRS)))
LOCAL_POLICY_FS_USE := $(wildcard $(addsuffix sepolicy.fs_use, $(LOCAL_POLICY_DIRS)))
LOCAL_POLICY_PORT_CONTEXTS := $(wildcard $(addsuffix sepolicy.port_contexts, $(LOCAL_POLICY_DIRS)))
LOCAL_POLICY_GENFS_CONTEXTS := $(wildcard $(addsuffix sepolicy.genfs_contexts, $(LOCAL_POLICY_DIRS)))
LOCAL_POLICY_INITIAL_SID_CONTEXTS := $(wildcard $(addsuffix sepolicy.initial_sid_contexts, $(LOCAL_POLICY_DIRS)))
LOCAL_POLICY_SC := $(wildcard $(addsuffix seapp_contexts, $(LOCAL_POLICY_DIRS)))

##################################
include $(CLEAR_VARS)

LOCAL_MODULE := sepolicy
LOCAL_MODULE_CLASS := ETC
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_PATH := $(TARGET_ROOT_OUT)

include $(BUILD_SYSTEM)/base_rules.mk

sepolicy_policy.conf := $(intermediates)/policy.conf

# Build up the list of policy files (the order matters, since they will all be
# cat'd together)
POLICY_DEPENDS := $(wildcard $(addprefix $(LOCAL_PATH)/,security_classes initial_sids access_vectors global_macros mls_macros mls policy_capabilities te_macros attributes *.te))

# Add extra policy for "su", but only for eng and userdebug builds
ifneq (,$(filter userdebug eng,$(TARGET_BUILD_VARIANT)))
POLICY_DEPENDS += $(wildcard $(addprefix $(LOCAL_PATH)/conditional/, su.te))
endif

# Add in the rest of the policy
POLICY_DEPENDS += $(wildcard $(LOCAL_POLICY_TE) $(addprefix $(LOCAL_PATH)/, roles users initial_sid_contexts) $(LOCAL_POLICY_INITIAL_SID_CONTEXTS) $(addprefix $(LOCAL_PATH)/,fs_use) $(LOCAL_POLICY_FS_USE) $(addprefix $(LOCAL_PATH)/,genfs_contexts) $(LOCAL_POLICY_GENFS_CONTEXTS) $(addprefix $(LOCAL_PATH)/,port_contexts) $(LOCAL_POLICY_PORT_CONTEXTS))

$(sepolicy_policy.conf): PRIVATE_MLS_SENS := $(MLS_SENS)
$(sepolicy_policy.conf): PRIVATE_MLS_CATS := $(MLS_CATS)
$(sepolicy_policy.conf) : $(POLICY_DEPENDS)
	@mkdir -p $(dir $@)
	$(hide) m4 -D mls_num_sens=$(PRIVATE_MLS_SENS) -D mls_num_cats=$(PRIVATE_MLS_CATS) -s $(POLICY_DEPENDS) > $@

$(LOCAL_BUILT_MODULE) : $(sepolicy_policy.conf) $(HOST_OUT_EXECUTABLES)/checkpolicy
	@mkdir -p $(dir $@)
	$(hide) $(HOST_OUT_EXECUTABLES)/checkpolicy -M -c $(POLICYVERS) -o $@ $<

sepolicy_policy.conf :=
##################################
include $(CLEAR_VARS)

LOCAL_MODULE := file_contexts
LOCAL_MODULE_CLASS := ETC
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_PATH := $(TARGET_ROOT_OUT)

include $(BUILD_SYSTEM)/base_rules.mk

file_contexts := $(intermediates)/file_contexts
$(file_contexts): $(LOCAL_PATH)/file_contexts $(LOCAL_POLICY_FC)
	@mkdir -p $(dir $@)
	$(hide) m4 -s $^ > $@

file_contexts :=

##################################
include $(CLEAR_VARS)
LOCAL_MODULE := seapp_contexts
LOCAL_MODULE_CLASS := ETC
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_PATH := $(TARGET_ROOT_OUT)

include $(BUILD_SYSTEM)/base_rules.mk

seapp_contexts.tmp := $(intermediates)/seapp_contexts.tmp
$(seapp_contexts.tmp): $(LOCAL_PATH)/seapp_contexts $(LOCAL_POLICY_SC)
	@mkdir -p $(dir $@)
	$(hide) m4 -s $^ > $@

$(LOCAL_BUILT_MODULE) : $(seapp_contexts.tmp) $(TARGET_ROOT_OUT)/sepolicy $(HOST_OUT_EXECUTABLES)/checkseapp
	@mkdir -p $(dir $@)
	$(HOST_OUT_EXECUTABLES)/checkseapp -p $(TARGET_ROOT_OUT)/sepolicy -o $@ $<

seapp_contexts.tmp :=
##################################
include $(CLEAR_VARS)

LOCAL_MODULE := property_contexts
LOCAL_MODULE_CLASS := ETC
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_PATH := $(TARGET_ROOT_OUT)

include $(BUILD_SYSTEM)/base_rules.mk

property_contexts := $(intermediates)/property_contexts
$(property_contexts): $(LOCAL_PATH)/property_contexts $(LOCAL_POLICY_PC)
	@mkdir -p $(dir $@)
	$(hide) m4 -s $^ > $@

property_contexts :=
##################################

##################################
include $(CLEAR_VARS)

LOCAL_MODULE := selinux-network.sh
LOCAL_SRC_FILES := $(LOCAL_MODULE)
LOCAL_MODULE_CLASS := EXECUTABLES
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_PATH := $(TARGET_OUT_EXECUTABLES)

include $(BUILD_PREBUILT)

##################################
include $(CLEAR_VARS)

LOCAL_MODULE := mac_permissions.xml
LOCAL_MODULE_CLASS := ETC
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_PATH := $(TARGET_OUT_ETC)/security

LOCAL_SRC_FILES := $(LOCAL_MODULE)

include $(BUILD_PREBUILT)

##################################

endif #ifeq ($(HAVE_SELINUX),true)
