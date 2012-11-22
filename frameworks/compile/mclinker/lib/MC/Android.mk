LOCAL_PATH:= $(call my-dir)

mcld_mc_SRC_FILES := \
  AttributeFactory.cpp  \
  ContextFactory.cpp  \
  InputFactory.cpp  \
  InputTree.cpp \
  MCLDAttribute.cpp \
  MCLDDirectory.cpp \
  MCLDDriver.cpp  \
  MCLDFile.cpp  \
  MCLDInfo.cpp  \
  MCLDInput.cpp \
  MCLDOptions.cpp \
  MCLDOutput.cpp  \
  MCLinker.cpp  \
  SearchDirs.cpp  \
  SymbolCategory.cpp  \
  ZOption.cpp

# For the host
# =====================================================
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(mcld_mc_SRC_FILES)
LOCAL_MODULE:= libmcldMC

LOCAL_MODULE_TAGS := optional

include $(MCLD_HOST_BUILD_MK)
include $(BUILD_HOST_STATIC_LIBRARY)

# For the device
# =====================================================
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(mcld_mc_SRC_FILES)
LOCAL_MODULE:= libmcldMC

LOCAL_MODULE_TAGS := optional

include $(MCLD_DEVICE_BUILD_MK)
include $(BUILD_STATIC_LIBRARY)
