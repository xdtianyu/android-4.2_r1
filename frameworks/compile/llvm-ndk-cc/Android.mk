LOCAL_PATH := $(call my-dir)
LLVM_ROOT_PATH := external/llvm
CLANG_ROOT_PATH := external/clang
llvm_static_libs_needed := \
	libclangFrontend	\
        libclangDriver \
	libclangSerialization	\
	libclangParse	\
	libclangSema	\
	libclangAnalysis	\
	libclangCodeGen	\
	libclangAST	\
	libclangLex	\
	libclangEdit	\
	libclangBasic	\
	libLLVMLinker   \
	libLLVMipo	\
	libLLVMBitWriter	\
	libLLVMBitReader	\
	libLLVMARMCodeGen	\
	libLLVMARMInfo	\
	libLLVMARMAsmParser \
	libLLVMARMDesc \
	libLLVMARMAsmPrinter	\
	libLLVMX86CodeGen	\
	libLLVMX86Info	\
	libLLVMX86Desc \
	libLLVMX86AsmParser \
	libLLVMX86AsmPrinter	\
	libLLVMX86Utils	\
	libLLVMMipsCodeGen \
	libLLVMMipsInfo \
	libLLVMMipsDesc \
	libLLVMMipsAsmParser \
	libLLVMMipsAsmPrinter \
	libLLVMAsmPrinter	\
	libLLVMSelectionDAG	\
	libLLVMCodeGen	\
	libLLVMScalarOpts	\
	libLLVMInstCombine	\
	libLLVMInstrumentation	\
	libLLVMTransformUtils	\
	libLLVMipa	\
	libLLVMAnalysis	\
	libLLVMTarget	\
	libLLVMMC	\
	libLLVMMCParser	\
	libLLVMCore	\
	libLLVMArchive \
	libLLVMAsmParser \
	libLLVMSupport  \
        libLLVMVectorize

# ========================================================
# Static library libndkpc for host
# ========================================================
include $(CLEAR_VARS)
include $(CLEAR_TBLGEN_VARS)

include $(CLANG_ROOT_PATH)/clang.mk

LOCAL_MODULE := libndkpc
LOCAL_MODULE_TAGS := optional

LOCAL_CFLAGS += -Wno-sign-promo
ifneq ($(TARGET_BUILD_VARIANT),eng)
LOCAL_CFLAGS += -D__DISABLE_ASSERTS
endif

TBLGEN_TABLES :=    \
	AttrList.inc	\
	Attrs.inc	\
	CommentNodes.inc \
	DeclNodes.inc	\
	DiagnosticCommonKinds.inc	\
	DiagnosticFrontendKinds.inc	\
	DiagnosticSemaKinds.inc	\
	StmtNodes.inc

LOCAL_SRC_FILES :=	\
	Compiler.cpp	\
	Backend.cpp

LOCAL_LDLIBS := -ldl -lpthread

include $(CLANG_HOST_BUILD_MK)
include $(CLANG_TBLGEN_RULES_MK)
include $(LLVM_GEN_INTRINSICS_MK)
include $(BUILD_HOST_STATIC_LIBRARY)

# ========================================================
# Executable llvm-ndk-cc for host
# ========================================================
include $(CLEAR_VARS)
include $(CLEAR_TBLGEN_VARS)

LOCAL_IS_HOST_MODULE := true
LOCAL_MODULE := llvm-ndk-cc
LOCAL_MODULE_TAGS := optional

LOCAL_MODULE_CLASS := EXECUTABLES

LOCAL_CFLAGS += -Wno-sign-promo
ifneq ($(TARGET_BUILD_VARIANT),eng)
LOCAL_CFLAGS += -D__DISABLE_ASSERTS
endif

TBLGEN_TABLES :=    \
	AttrList.inc    \
	Attrs.inc    \
	CommentNodes.inc \
	DeclNodes.inc    \
	DiagnosticCommonKinds.inc   \
	DiagnosticDriverKinds.inc \
	DiagnosticFrontendKinds.inc	\
	DiagnosticSemaKinds.inc	\
	StmtNodes.inc

LOCAL_SRC_FILES :=	\
	llvm-ndk-cc.cpp

LOCAL_STATIC_LIBRARIES :=	\
	libclangDriver libndkpc \
	$(llvm_static_libs_needed)

ifeq ($(HOST_OS),windows)
  LOCAL_LDLIBS := -limagehlp -lpsapi
else
  LOCAL_LDLIBS := -ldl -lpthread
endif

include $(CLANG_HOST_BUILD_MK)
include $(CLANG_TBLGEN_RULES_MK)
include $(BUILD_HOST_EXECUTABLE)

# ========================================================
# Executable llvm-ndk-link for host
# ========================================================
include $(CLEAR_VARS)

LOCAL_IS_HOST_MODULE := true
LOCAL_MODULE := llvm-ndk-link
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_CLASS := EXECUTABLES

LOCAL_SRC_FILES := llvm-ndk-link.cpp

LOCAL_LDLIBS := -ldl -lpthread

LOCAL_STATIC_LIBRARIES := \
  $(llvm_static_libs_needed)

include $(CLANG_ROOT_PATH)/clang.mk
include $(CLANG_HOST_BUILD_MK)
include $(LLVM_ROOT_PATH)/llvm.mk
include $(LLVM_HOST_BUILD_MK)

include $(BUILD_HOST_EXECUTABLE)
