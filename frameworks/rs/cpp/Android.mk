LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES:= \
	RenderScript.cpp \
	BaseObj.cpp \
	Element.cpp \
	Type.cpp \
	Allocation.cpp \
	Script.cpp \
	ScriptC.cpp

LOCAL_SHARED_LIBRARIES := \
	libRS \
	libz \
	libcutils \
	libutils

LOCAL_MODULE:= libRScpp

LOCAL_MODULE_TAGS := optional

intermediates := $(call intermediates-dir-for,STATIC_LIBRARIES,libRS,TARGET,)
librs_generated_headers := \
    $(intermediates)/rsgApiStructs.h \
    $(intermediates)/rsgApiFuncDecl.h
LOCAL_GENERATED_SOURCES := $(librs_generated_headers)

LOCAL_C_INCLUDES += frameworks/rs
LOCAL_C_INCLUDES += $(intermediates)


include $(BUILD_SHARED_LIBRARY)

