LOCAL_PATH:= $(call my-dir)

clang_static_analyzer_checkers_TBLGEN_TABLES := \
  AttrKinds.inc \
  AttrList.inc \
  AttrParsedAttrList.inc \
  Attrs.inc \
  Checkers.inc \
  CommentNodes.inc \
  DeclNodes.inc \
  DiagnosticCommonKinds.inc \
  StmtNodes.inc

clang_static_analyzer_checkers_SRC_FILES := \
  AnalyzerStatsChecker.cpp \
  ArrayBoundChecker.cpp \
  ArrayBoundCheckerV2.cpp \
  AttrNonNullChecker.cpp \
  BasicObjCFoundationChecks.cpp \
  BoolAssignmentChecker.cpp \
  BuiltinFunctionChecker.cpp \
  CStringChecker.cpp \
  CStringSyntaxChecker.cpp \
  CallAndMessageChecker.cpp \
  CastSizeChecker.cpp \
  CastToStructChecker.cpp \
  CheckObjCDealloc.cpp \
  CheckObjCInstMethSignature.cpp \
  CheckSecuritySyntaxOnly.cpp \
  CheckSizeofPointer.cpp \
  CheckerDocumentation.cpp \
  ChrootChecker.cpp \
  ClangCheckers.cpp \
  CommonBugCategories.cpp \
  DeadStoresChecker.cpp \
  DebugCheckers.cpp \
  DereferenceChecker.cpp \
  DivZeroChecker.cpp \
  DynamicTypePropagation.cpp \
  ExprInspectionChecker.cpp \
  FixedAddressChecker.cpp \
  GenericTaintChecker.cpp \
  IdempotentOperationChecker.cpp \
  LLVMConventionsChecker.cpp \
  MacOSKeychainAPIChecker.cpp \
  MacOSXAPIChecker.cpp \
  MallocChecker.cpp \
  MallocOverflowSecurityChecker.cpp \
  MallocSizeofChecker.cpp \
  NSAutoreleasePoolChecker.cpp \
  NSErrorChecker.cpp \
  NoReturnFunctionChecker.cpp \
  OSAtomicChecker.cpp \
  ObjCAtSyncChecker.cpp \
  ObjCContainersASTChecker.cpp \
  ObjCContainersChecker.cpp \
  ObjCSelfInitChecker.cpp \
  ObjCUnusedIVarsChecker.cpp \
  PointerArithChecker.cpp \
  PointerSubChecker.cpp \
  PthreadLockChecker.cpp \
  RetainCountChecker.cpp \
  ReturnPointerRangeChecker.cpp \
  ReturnUndefChecker.cpp \
  StackAddrEscapeChecker.cpp \
  StreamChecker.cpp \
  TaintTesterChecker.cpp \
  TraversalChecker.cpp \
  UndefBranchChecker.cpp \
  UndefCapturedBlockVarChecker.cpp \
  UndefResultChecker.cpp \
  UndefinedArraySubscriptChecker.cpp \
  UndefinedAssignmentChecker.cpp \
  UnixAPIChecker.cpp \
  UnreachableCodeChecker.cpp \
  VirtualCallChecker.cpp \
  VLASizeChecker.cpp

# For the host only
# =====================================================
include $(CLEAR_VARS)
include $(CLEAR_TBLGEN_VARS)

TBLGEN_TABLES := $(clang_static_analyzer_checkers_TBLGEN_TABLES)

LOCAL_SRC_FILES := $(clang_static_analyzer_checkers_SRC_FILES)

LOCAL_MODULE:= libclangStaticAnalyzerCheckers

LOCAL_MODULE_TAGS := optional

include $(CLANG_HOST_BUILD_MK)
include $(CLANG_TBLGEN_RULES_MK)
include $(CLANG_VERSION_INC_MK)
include $(BUILD_HOST_STATIC_LIBRARY)
