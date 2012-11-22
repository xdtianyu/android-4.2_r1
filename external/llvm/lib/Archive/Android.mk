LOCAL_PATH:= $(call my-dir)

archive_SRC_FILES := \
	Archive.cpp \
	ArchiveReader.cpp \
	ArchiveWriter.cpp

# For the host
# =====================================================
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(archive_SRC_FILES)

LOCAL_MODULE:= libLLVMArchive

LOCAL_MODULE_TAGS := optional

include $(LLVM_HOST_BUILD_MK)
include $(BUILD_HOST_STATIC_LIBRARY)
