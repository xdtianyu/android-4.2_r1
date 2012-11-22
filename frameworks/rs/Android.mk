
LOCAL_PATH:=$(call my-dir)

rs_base_CFLAGS := -Werror -Wall -Wno-unused-parameter -Wno-unused-variable
ifeq ($(TARGET_BUILD_PDK), true)
  rs_base_CFLAGS += -D__RS_PDK__
endif

ifneq ($(OVERRIDE_RS_DRIVER),)
  rs_base_CFLAGS += -DOVERRIDE_RS_DRIVER=$(OVERRIDE_RS_DRIVER)
endif

include $(CLEAR_VARS)
LOCAL_CLANG := true
LOCAL_MODULE := libRSDriver

LOCAL_SRC_FILES:= \
	driver/rsdAllocation.cpp \
	driver/rsdBcc.cpp \
	driver/rsdCore.cpp \
	driver/rsdFrameBuffer.cpp \
	driver/rsdFrameBufferObj.cpp \
	driver/rsdGL.cpp \
	driver/rsdIntrinsics.cpp \
	driver/rsdIntrinsicBlend.cpp \
	driver/rsdIntrinsicBlur.cpp \
	driver/rsdIntrinsicConvolve3x3.cpp \
	driver/rsdIntrinsicConvolve5x5.cpp \
	driver/rsdIntrinsicLUT.cpp \
	driver/rsdIntrinsicColorMatrix.cpp \
	driver/rsdIntrinsicYuvToRGB.cpp \
	driver/rsdMesh.cpp \
	driver/rsdMeshObj.cpp \
	driver/rsdPath.cpp \
	driver/rsdProgram.cpp \
	driver/rsdProgramRaster.cpp \
	driver/rsdProgramStore.cpp \
	driver/rsdRuntimeMath.cpp \
	driver/rsdRuntimeStubs.cpp \
	driver/rsdSampler.cpp \
	driver/rsdScriptGroup.cpp \
	driver/rsdShader.cpp \
	driver/rsdShaderCache.cpp \
	driver/rsdVertexArray.cpp

ifeq ($(ARCH_ARM_HAVE_NEON),true)
    LOCAL_CFLAGS += -DARCH_ARM_HAVE_NEON
    LOCAL_SRC_FILES+= \
        driver/rsdIntrinsics_Convolve.S
endif

LOCAL_SHARED_LIBRARIES += libRS
LOCAL_SHARED_LIBRARIES += libcutils libutils libEGL libGLESv1_CM libGLESv2
LOCAL_SHARED_LIBRARIES += libbcc libbcinfo libui libgui libsync

LOCAL_C_INCLUDES += frameworks/compile/libbcc/include

LOCAL_CFLAGS += $(rs_base_CFLAGS)

LOCAL_LDLIBS := -lpthread -ldl
LOCAL_MODULE_TAGS := optional

include $(BUILD_SHARED_LIBRARY)

# Build rsg-generator ====================
include $(CLEAR_VARS)

LOCAL_MODULE := rsg-generator

# These symbols are normally defined by BUILD_XXX, but we need to define them
# here so that local-intermediates-dir works.

LOCAL_IS_HOST_MODULE := true
LOCAL_MODULE_CLASS := EXECUTABLES
intermediates := $(local-intermediates-dir)

LOCAL_SRC_FILES:= \
    spec.l \
    rsg_generator.c

include $(BUILD_HOST_EXECUTABLE)

# TODO: This should go into build/core/config.mk
RSG_GENERATOR:=$(LOCAL_BUILT_MODULE)

include $(CLEAR_VARS)
LOCAL_CLANG := true
LOCAL_MODULE := libRS

LOCAL_MODULE_CLASS := SHARED_LIBRARIES
intermediates:= $(local-intermediates-dir)

# Generate custom headers

GEN := $(addprefix $(intermediates)/, \
            rsgApiStructs.h \
            rsgApiFuncDecl.h \
        )

$(GEN) : PRIVATE_PATH := $(LOCAL_PATH)
$(GEN) : PRIVATE_CUSTOM_TOOL = $(RSG_GENERATOR) $< $@ <$(PRIVATE_PATH)/rs.spec
$(GEN) : $(RSG_GENERATOR) $(LOCAL_PATH)/rs.spec
$(GEN): $(intermediates)/%.h : $(LOCAL_PATH)/%.h.rsg
	$(transform-generated-source)

# used in jni/Android.mk
rs_generated_source += $(GEN)
LOCAL_GENERATED_SOURCES += $(GEN)

# Generate custom source files

GEN := $(addprefix $(intermediates)/, \
            rsgApi.cpp \
            rsgApiReplay.cpp \
        )

$(GEN) : PRIVATE_PATH := $(LOCAL_PATH)
$(GEN) : PRIVATE_CUSTOM_TOOL = $(RSG_GENERATOR) $< $@ <$(PRIVATE_PATH)/rs.spec
$(GEN) : $(RSG_GENERATOR) $(LOCAL_PATH)/rs.spec
$(GEN): $(intermediates)/%.cpp : $(LOCAL_PATH)/%.cpp.rsg
	$(transform-generated-source)

# used in jni/Android.mk
rs_generated_source += $(GEN)

LOCAL_GENERATED_SOURCES += $(GEN)

LOCAL_SRC_FILES:= \
	rsAdapter.cpp \
	rsAllocation.cpp \
	rsAnimation.cpp \
	rsComponent.cpp \
	rsContext.cpp \
	rsDevice.cpp \
	rsElement.cpp \
	rsFBOCache.cpp \
	rsFifoSocket.cpp \
	rsFileA3D.cpp \
	rsFont.cpp \
	rsObjectBase.cpp \
	rsMatrix2x2.cpp \
	rsMatrix3x3.cpp \
	rsMatrix4x4.cpp \
	rsMesh.cpp \
	rsMutex.cpp \
	rsPath.cpp \
	rsProgram.cpp \
	rsProgramFragment.cpp \
	rsProgramStore.cpp \
	rsProgramRaster.cpp \
	rsProgramVertex.cpp \
	rsSampler.cpp \
	rsScript.cpp \
	rsScriptC.cpp \
	rsScriptC_Lib.cpp \
	rsScriptC_LibGL.cpp \
	rsScriptGroup.cpp \
	rsScriptIntrinsic.cpp \
	rsSignal.cpp \
	rsStream.cpp \
	rsThreadIO.cpp \
	rsType.cpp

LOCAL_SHARED_LIBRARIES += libcutils libutils libEGL libGLESv1_CM libGLESv2 libbcc
LOCAL_SHARED_LIBRARIES += libui libbcinfo libgui libsync libdl

LOCAL_STATIC_LIBRARIES := libft2

LOCAL_C_INCLUDES += external/freetype/include
LOCAL_C_INCLUDES += frameworks/compile/libbcc/include

LOCAL_CFLAGS += $(rs_base_CFLAGS)

LOCAL_LDLIBS := -lpthread -ldl
LOCAL_MODULE_TAGS := optional

include $(BUILD_SHARED_LIBRARY)

# Now build a host version for serialization
include $(CLEAR_VARS)
LOCAL_MODULE:= libRS
LOCAL_MODULE_TAGS := optional

intermediates := $(call intermediates-dir-for,STATIC_LIBRARIES,libRS,HOST,)

# Generate custom headers

GEN := $(addprefix $(intermediates)/, \
            rsgApiStructs.h \
            rsgApiFuncDecl.h \
        )

$(GEN) : PRIVATE_PATH := $(LOCAL_PATH)
$(GEN) : PRIVATE_CUSTOM_TOOL = $(RSG_GENERATOR) $< $@ <$(PRIVATE_PATH)/rs.spec
$(GEN) : $(RSG_GENERATOR) $(LOCAL_PATH)/rs.spec
$(GEN): $(intermediates)/%.h : $(LOCAL_PATH)/%.h.rsg
	$(transform-generated-source)

LOCAL_GENERATED_SOURCES += $(GEN)

# Generate custom source files

GEN := $(addprefix $(intermediates)/, \
            rsgApi.cpp \
            rsgApiReplay.cpp \
        )

$(GEN) : PRIVATE_PATH := $(LOCAL_PATH)
$(GEN) : PRIVATE_CUSTOM_TOOL = $(RSG_GENERATOR) $< $@ <$(PRIVATE_PATH)/rs.spec
$(GEN) : $(RSG_GENERATOR) $(LOCAL_PATH)/rs.spec
$(GEN): $(intermediates)/%.cpp : $(LOCAL_PATH)/%.cpp.rsg
	$(transform-generated-source)

LOCAL_GENERATED_SOURCES += $(GEN)

LOCAL_CFLAGS += $(rs_base_CFLAGS)
LOCAL_CFLAGS += -DANDROID_RS_SERIALIZE
LOCAL_CFLAGS += -fPIC

LOCAL_SRC_FILES:= \
	rsAdapter.cpp \
	rsAllocation.cpp \
	rsAnimation.cpp \
	rsComponent.cpp \
	rsContext.cpp \
	rsDevice.cpp \
	rsElement.cpp \
	rsFBOCache.cpp \
	rsFifoSocket.cpp \
	rsFileA3D.cpp \
	rsFont.cpp \
	rsObjectBase.cpp \
	rsMatrix2x2.cpp \
	rsMatrix3x3.cpp \
	rsMatrix4x4.cpp \
	rsMesh.cpp \
	rsMutex.cpp \
	rsPath.cpp \
	rsProgram.cpp \
	rsProgramFragment.cpp \
	rsProgramStore.cpp \
	rsProgramRaster.cpp \
	rsProgramVertex.cpp \
	rsSampler.cpp \
	rsScript.cpp \
	rsScriptC.cpp \
	rsScriptC_Lib.cpp \
	rsScriptC_LibGL.cpp \
	rsScriptGroup.cpp \
	rsScriptIntrinsic.cpp \
	rsSignal.cpp \
	rsStream.cpp \
	rsThreadIO.cpp \
	rsType.cpp

LOCAL_STATIC_LIBRARIES := libcutils libutils

LOCAL_LDLIBS := -lpthread

include $(BUILD_HOST_STATIC_LIBRARY)
