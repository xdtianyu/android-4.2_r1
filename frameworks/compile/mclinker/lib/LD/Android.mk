LOCAL_PATH:= $(call my-dir)

# =====================================================
# Static library: libmcldLD
# =====================================================

mcld_ld_SRC_FILES := \
  AlignFragment.cpp \
  Archive.cpp \
  ArchiveReader.cpp \
  BranchIsland.cpp  \
  CIE.cpp \
  DWARFLineInfo.cpp \
  Diagnostic.cpp  \
  DiagnosticEngine.cpp  \
  DiagnosticInfos.cpp \
  DiagnosticLineInfo.cpp  \
  DiagnosticPrinter.cpp \
  DynObjReader.cpp  \
  DynObjWriter.cpp  \
  ELFSegment.cpp  \
  ELFSegmentFactory.cpp \
  EhFrame.cpp \
  EhFrameHdr.cpp  \
  ExecWriter.cpp  \
  FDE.cpp \
  FillFragment.cpp \
  Fragment.cpp \
  FragmentRef.cpp \
  Layout.cpp  \
  LDContext.cpp \
  LDFileFormat.cpp  \
  LDReader.cpp  \
  LDSection.cpp \
  LDSectionFactory.cpp  \
  LDSymbol.cpp  \
  LDWriter.cpp  \
  MsgHandler.cpp  \
  NamePool.cpp  \
  ObjectWriter.cpp  \
  RegionFragment.cpp \
  Relocation.cpp  \
  RelocationFactory.cpp \
  ResolveInfo.cpp \
  ResolveInfoFactory.cpp  \
  Resolver.cpp  \
  SectionData.cpp \
  SectionMap.cpp  \
  SectionMerger.cpp \
  StaticResolver.cpp  \
  TextDiagnosticPrinter.cpp

# For the host
# =====================================================
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(mcld_ld_SRC_FILES)
LOCAL_MODULE:= libmcldLD

LOCAL_MODULE_TAGS := optional

include $(MCLD_HOST_BUILD_MK)
include $(BUILD_HOST_STATIC_LIBRARY)

# For the device
# =====================================================
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(mcld_ld_SRC_FILES)
LOCAL_MODULE:= libmcldLD

LOCAL_MODULE_TAGS := optional

include $(MCLD_DEVICE_BUILD_MK)
include $(BUILD_STATIC_LIBRARY)

# =====================================================
# Static library: libmcldLDVariant
# =====================================================

mcld_ld_variant_SRC_FILES := \
  BSDArchiveReader.cpp  \
  GNUArchiveReader.cpp  \
  ELFDynObjFileFormat.cpp \
  ELFDynObjReader.cpp \
  ELFDynObjWriter.cpp \
  ELFExecFileFormat.cpp \
  ELFExecWriter.cpp \
  ELFFileFormat.cpp \
  ELFObjectReader.cpp \
  ELFObjectWriter.cpp \
  ELFReader.cpp \
  ELFWriter.cpp

# For the host
# =====================================================
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(mcld_ld_variant_SRC_FILES)
LOCAL_MODULE:= libmcldLDVariant

LOCAL_MODULE_TAGS := optional

include $(MCLD_HOST_BUILD_MK)
include $(BUILD_HOST_STATIC_LIBRARY)

# For the device
# =====================================================
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(mcld_ld_variant_SRC_FILES)
LOCAL_MODULE:= libmcldLDVariant

LOCAL_MODULE_TAGS := optional

include $(MCLD_DEVICE_BUILD_MK)
include $(BUILD_STATIC_LIBRARY)
