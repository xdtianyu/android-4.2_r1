#ifndef IR_TO_LLVM_H_
#define IR_TO_LLVM_H_

#include "llvm/Module.h"
#include "ir.h"

struct llvm::Module * glsl_ir_to_llvm_module(struct exec_list *ir, llvm::Module * mod,
               const struct GGLState * gglCtx, const char * shaderSuffix);

#endif /* IR_TO_LLVM_H_ */
