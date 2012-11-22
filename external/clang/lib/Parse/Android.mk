LOCAL_PATH:= $(call my-dir)

# For the host only
# =====================================================
include $(CLEAR_VARS)
include $(CLEAR_TBLGEN_VARS)

TBLGEN_TABLES :=  \
	AttrList.inc  \
	AttrLateParsed.inc  \
	AttrParsedAttrList.inc  \
	Attrs.inc  \
	CommentNodes.inc \
	DeclNodes.inc  \
	DiagnosticParseKinds.inc  \
        DiagnosticCommonKinds.inc  \
	StmtNodes.inc

clang_parse_SRC_FILES :=  \
	ParseAST.cpp  \
	ParseCXXInlineMethods.cpp  \
	ParseDecl.cpp  \
	ParseDeclCXX.cpp  \
	ParseExpr.cpp  \
	ParseExprCXX.cpp  \
	ParseInit.cpp  \
	ParseObjc.cpp  \
	ParsePragma.cpp  \
	ParseStmt.cpp  \
	ParseTemplate.cpp  \
	ParseTentative.cpp  \
	Parser.cpp

LOCAL_SRC_FILES := $(clang_parse_SRC_FILES)

LOCAL_MODULE:= libclangParse
LOCAL_MODULE_TAGS := optional

LOCAL_MODULE_TAGS := optional

include $(CLANG_HOST_BUILD_MK)
include $(CLANG_TBLGEN_RULES_MK)
include $(BUILD_HOST_STATIC_LIBRARY)
