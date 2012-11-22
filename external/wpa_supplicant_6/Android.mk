ifndef WPA_SUPPLICANT_VERSION
WPA_SUPPLICANT_VERSION := VER_0_6_X
endif
ifeq ($(WPA_SUPPLICANT_VERSION),VER_0_6_X)
    include $(call all-subdir-makefiles)
endif
