ifeq (0,1)
LOCAL_PATH := $(call my-dir)

define _add-ducati-vendor-bin
include $$(CLEAR_VARS)
$(if $(word 2,$1),$(error Invalid DUCATI module name $1))
LOCAL_MODULE := $(basename $(notdir $1))
LOCAL_SRC_FILES := $1
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_SUFFIX := $(suffix $1)
LOCAL_MODULE_CLASS := EXECUTABLES
LOCAL_MODULE_PATH := $$(TARGET_OUT)$(abspath /$(dir $1))
OVERRIDE_BUILT_MODULE_PATH := $$(TARGET_OUT_INTERMEDIATE_EXECUTABLES)
include $$(BUILD_PREBUILT)
endef

prebuilt_ducati_vendor_bins := \
	etc/firmware/ducati-m3.bin 

prebuilt_ducati_modules := \
  $(foreach _file, $(prebuilt_ducati_vendor_bins),\
    $(notdir $(basename $(_file))))

include $(CLEAR_VARS)
LOCAL_MODULE := ti_omap4_ducati_bins
LOCAL_MODULE_TAGS := optional
LOCAL_REQUIRED_MODULES := $(prebuilt_ducati_modules)
include $(BUILD_PHONY_PACKAGE)

$(foreach _file,$(prebuilt_ducati_vendor_bins),\
  $(eval $(call _add-ducati-vendor-bin,$(_file))))

prebuilt_ducati_modules :=
prebuilt_ducati_vendor_bins :=
_add-ducati-vendor-bin :=
endif
