/*
 * Copyright (C) 2005-2007  Brian Paul   All Rights Reserved.
 * Copyright (C) 2008  VMware, Inc.   All Rights Reserved.
 * Copyright © 2010 Intel Corporation
 * Copyright © 2010 Luca Barbieri
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice (including the next
 * paragraph) shall be included in all copies or substantial portions of the
 * Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

/**
 * \file ir_to_llvm.cpp
 *
 * Translates the IR to LLVM
 */

/* this tends to get set as part of LLVM_CFLAGS, but we definitely want asserts */
#ifdef NDEBUG
#undef NDEBUG
#endif

#include "llvm/ADT/ArrayRef.h"
#include "llvm/DerivedTypes.h"
#include "llvm/IRBuilder.h"
#include "llvm/LLVMContext.h"
#include "llvm/Module.h"
#include "llvm/Analysis/Verifier.h"
//#include "llvm/Intrinsics.h"

#include <vector>
#include <stdio.h>
#include <map>
/*
#ifdef _MSC_VER
#include <unordered_map>
#else
#include <tr1/unordered_map>
#endif
// use C++0x/Microsoft convention
namespace std
{
using namespace tr1;
}
//*/

#include "ir.h"
#include "ir_visitor.h"
#include "glsl_types.h"
#include "src/mesa/main/mtypes.h"

// Helper function to convert array to llvm::ArrayRef
template <typename T, size_t N>
static inline llvm::ArrayRef<T> pack(T const (&array)[N]) {
   return llvm::ArrayRef<T>(array);
}

// Helper function to convert pointer + size to llvm::ArrayRef
template <typename T>
static inline llvm::ArrayRef<T> pack(T const *ptr, size_t n) {
   return llvm::ArrayRef<T>(ptr, n);
}

struct GGLState;

llvm::Value * tex2D(llvm::IRBuilder<> & builder, llvm::Value * in1, const unsigned sampler,
                     const GGLState * gglCtx);
llvm::Value * texCube(llvm::IRBuilder<> & builder, llvm::Value * in1, const unsigned sampler,
                     const GGLState * gglCtx);

class ir_to_llvm_visitor : public ir_visitor {
   ir_to_llvm_visitor();
public:


   llvm::LLVMContext& ctx;
   llvm::Module* mod;
   llvm::Function* fun;
   // could easily support more loops, but GLSL doesn't support multiloop break/continue
   std::pair<llvm::BasicBlock*, llvm::BasicBlock*> loop;
   llvm::BasicBlock* bb;
   llvm::Value* result;
   llvm::IRBuilder<> bld;

   const GGLState * gglCtx;
   const char * shaderSuffix;
   llvm::Value * inputsPtr, * outputsPtr, * constantsPtr; // internal globals to store inputs/outputs/constants pointers
   llvm::Value * inputs, * outputs, * constants;

   ir_to_llvm_visitor(llvm::Module* p_mod, const GGLState * GGLCtx, const char * suffix)
   : ctx(p_mod->getContext()), mod(p_mod), fun(0), loop(std::make_pair((llvm::BasicBlock*)0,
      (llvm::BasicBlock*)0)), bb(0), bld(ctx), gglCtx(GGLCtx), shaderSuffix(suffix),
      inputsPtr(NULL), outputsPtr(NULL), constantsPtr(NULL),
      inputs(NULL), outputs(NULL), constants(NULL)
   {
      llvm::PointerType * const floatVecPtrType = llvm::PointerType::get(llvm::VectorType::get(bld.getFloatTy(),4), 0);
      llvm::Constant * const nullFloatVecPtr = llvm::Constant::getNullValue(floatVecPtrType);
      // make input, output and consts global pointers so they can be used in
      // different LLVM functions since the shader shares these "registers" across "functions"

      inputsPtr = new llvm::GlobalVariable(*mod, floatVecPtrType, false,
         llvm::GlobalValue::InternalLinkage, nullFloatVecPtr, "gl_inputPtr");

      outputsPtr = new llvm::GlobalVariable(*mod, floatVecPtrType, false,
         llvm::GlobalValue::InternalLinkage, nullFloatVecPtr, "gl_outputsPtr");

      constantsPtr = new llvm::GlobalVariable(*mod, floatVecPtrType, false,
         llvm::GlobalValue::InternalLinkage, nullFloatVecPtr, "gl_constantsPtr");
   }

   llvm::Type* llvm_base_type(unsigned base_type)
   {
      switch(base_type)
      {
      case GLSL_TYPE_VOID:
         return llvm::Type::getVoidTy(ctx);
      case GLSL_TYPE_UINT:
      case GLSL_TYPE_INT:
         return llvm::Type::getInt32Ty(ctx);
      case GLSL_TYPE_FLOAT:
         return llvm::Type::getFloatTy(ctx);
      case GLSL_TYPE_BOOL:
         return llvm::Type::getInt1Ty(ctx);
      case GLSL_TYPE_SAMPLER:
         return llvm::PointerType::getUnqual(llvm::Type::getVoidTy(ctx));
      default:
         assert(0);
         return 0;
      }
   }

   llvm::Type* llvm_vec_type(const glsl_type* type)
   {
      if (type->is_array())
         return llvm::ArrayType::get(llvm_type(type->fields.array), type->array_size());

      if (type->is_record())
      {
         std::vector<llvm::Type*> fields;
         for (unsigned i = 0; i < type->length; i++)
            fields.push_back(llvm_type(type->fields.structure[i].type));
         return llvm::StructType::get(ctx, llvm::ArrayRef<llvm::Type*>(
             fields));
      }

      llvm::Type* base_type = llvm_base_type(type->base_type);
      if (type->vector_elements <= 1) {
         return base_type;
      } else {
         return llvm::VectorType::get(base_type, type->vector_elements);
      }
   }

   llvm::Type* llvm_type(const glsl_type* type)
   {
      llvm::Type* vec_type = llvm_vec_type(type);
      if (type->matrix_columns <= 1) {
         return vec_type;
      } else {
         return llvm::ArrayType::get(vec_type, type->matrix_columns);
      }
   }

   typedef std::map<ir_variable*, llvm::Value*> llvm_variables_t;
   //typedef std::unordered_map<ir_variable*, llvm::Value*> llvm_variables_t;
   llvm_variables_t llvm_variables;

   llvm::Value* llvm_variable(class ir_variable* var)
   {
      llvm_variables_t::iterator vari = llvm_variables.find(var);
      if (vari != llvm_variables.end()) {
         return vari->second;
      } else {
         llvm::Type* type = llvm_type(var->type);

         llvm::Value* v = NULL;
         if(fun) {
            if (ir_var_in == var->mode)
            {
               assert(var->location >= 0);
               v = bld.CreateConstGEP1_32(inputs, var->location);
               v = bld.CreateBitCast(v, llvm::PointerType::get(llvm_type(var->type), 0), var->name);
            }
            else if (ir_var_out == var->mode)
            {
               assert(var->location >= 0);
               v = bld.CreateConstGEP1_32(outputs, var->location);
               v = bld.CreateBitCast(v, llvm::PointerType::get(llvm_type(var->type), 0), var->name);
            }
            else if (ir_var_uniform == var->mode)
            {
               assert(var->location >= 0);
               v = bld.CreateConstGEP1_32(constants, var->location);
               v = bld.CreateBitCast(v, llvm::PointerType::get(llvm_type(var->type), 0), var->name);
            }
            else
            {
               if(bb == &fun->getEntryBlock())
                  v = bld.CreateAlloca(type, 0, var->name);
               else
                  v = new llvm::AllocaInst(type, 0, var->name, fun->getEntryBlock().getTerminator());
            }
         } else {
           // TODO: can anything global be non-constant in GLSL?; fix linkage
            //printf("var '%s' mode=%d location=%d \n", var->name, var->mode, var->location);
            switch(var->mode)
            {
               case ir_var_auto: // fall through
               case ir_var_temporary:
               {
                  llvm::Constant * init = llvm::UndefValue::get(llvm_type(var->type));
                  if(var->constant_value)
                     init = llvm_constant(var->constant_value);
                  v = new llvm::GlobalVariable(*mod, type, var->read_only, llvm::GlobalValue::InternalLinkage, init, var->name);
                  break;
               }
               case ir_var_in: // fall through
               case ir_var_out: // fall through
               case ir_var_uniform: // fall through
                  assert(var->location >= 0);
                  return NULL; // variable outside of function means declaration
               default:
                  assert(0);
            }

//            llvm::Function::LinkageTypes linkage;
//            if(var->mode == ir_var_auto || var->mode == ir_var_temporary)
//               linkage = llvm::GlobalValue::InternalLinkage;
//            else
//               linkage = llvm::GlobalValue::ExternalLinkage;
//            llvm::Constant* init = 0;
//            if(var->constant_value)
//            {
//               init = llvm_constant(var->constant_value);
//               // this constants need to be external (ie. written to output)
//               if (llvm::GlobalValue::ExternalLinkage == linkage)
//                  linkage = llvm::GlobalValue::AvailableExternallyLinkage;
//            }
//            else if(linkage == llvm::GlobalValue::InternalLinkage)
//               init = llvm::UndefValue::get(llvm_type(var->type));
//            v = new llvm::GlobalVariable(*mod, type, var->read_only, linkage, init, var->name);
         }
         assert(v);
         llvm_variables[var] = v;
         return v;
      }
   }

   //typedef std::map<ir_function_signature*, llvm::Function*> llvm_functions_t;
   //typedef std::unordered_map<ir_function_signature*, llvm::Function*> llvm_functions_t;
   //llvm_functions_t llvm_functions;

   llvm::Function* llvm_function(class ir_function_signature* sig)
   {
      const char* name = sig->function_name();
      char * functionName = (char *)malloc(strlen(name) + strlen(shaderSuffix) + 1);
      strcpy(functionName, name);
      strcat(functionName, shaderSuffix);
      llvm::Function * function = mod->getFunction(functionName);
      if (function)
      {
         free(functionName);
         return function;
      }
      else
      {
         llvm::Function::LinkageTypes linkage;
         std::vector<llvm::Type*> params;
         foreach_iter(exec_list_iterator, iter, sig->parameters) {
            ir_variable* arg = (ir_variable*)iter.get();
            params.push_back(llvm_type(arg->type));
         }

         if(!strcmp(name, "main") || !sig->is_defined)
         {
            linkage = llvm::Function::ExternalLinkage;
            llvm::PointerType * vecPtrTy = llvm::PointerType::get(llvm::VectorType::get(bld.getFloatTy(), 4), 0);
            assert(0 == params.size());
            params.push_back(vecPtrTy); // inputs
            params.push_back(vecPtrTy); // outputs
            params.push_back(vecPtrTy); // constants
         }
         else {
            linkage = llvm::Function::InternalLinkage;
         }
         llvm::FunctionType* ft = llvm::FunctionType::get(llvm_type(sig->return_type),
                                                          llvm::ArrayRef<llvm::Type*>(params),
                                                          false);
         function = llvm::Function::Create(ft, linkage, functionName, mod);
         free(functionName);
         return function;
      }
   }

   llvm::Value* llvm_value(class ir_instruction* ir)
   {
      result = 0;
      ir->accept(this);
      return result;
   }

   llvm::Constant* llvm_constant(class ir_instruction* ir)
   {
      return (llvm::Constant *)llvm_value(ir);
      //return &dynamic_cast<llvm::Constant&>(*llvm_value(ir));
   }

   llvm::Constant* llvm_int(unsigned v)
   {
      return llvm::ConstantInt::get(llvm::Type::getInt32Ty(ctx), v);
   }

   llvm::Value* llvm_pointer(class ir_rvalue* ir)
   {
      if(ir_dereference_variable* deref = ir->as_dereference_variable())
         return llvm_variable(deref->variable_referenced());
      else if(ir_dereference_array* deref = ir->as_dereference_array())
      {
         llvm::Value* gep[2] = {llvm_int(0), llvm_value(deref->array_index)};
         return bld.CreateInBoundsGEP(llvm_pointer(deref->array), gep);
         }
      else if(ir->as_dereference())
      {
         ir_dereference_record* deref = (ir_dereference_record*)ir;
         int idx = deref->record->type->field_index(deref->field);
         assert(idx >= 0);
         return bld.CreateConstInBoundsGEP2_32(llvm_pointer(deref->record), 0, idx);
      }
      else
      {
         assert(0);
         return 0;
      }
   }

//   llvm::Value* llvm_intrinsic(llvm::Intrinsic::ID id, llvm::Value* a)
//   {
//      llvm::Type* types[1] = {a->getType()};
//      return bld.CreateCall(llvm::Intrinsic::getDeclaration(mod, id, types, 1), a);
//   }
//
//   llvm::Value* llvm_intrinsic(llvm::Intrinsic::ID id, llvm::Value* a, llvm::Value* b)
//   {
//      llvm::Type* types[2] = {a->getType(), b->getType()};
//      /* only one type suffix is usually needed, so pass 1 here */
//      return bld.CreateCall2(llvm::Intrinsic::getDeclaration(mod, id, types, 1), a, b);
//   }

   llvm::Value* llvm_intrinsic_unop(ir_expression_operation op, llvm::Value * op0)
   {
      llvm::Type * floatType = llvm::Type::getFloatTy(ctx);
      const char * name = NULL;
      switch (op) {
      case ir_unop_sin:
         name = "sinf";
         break;
      case ir_unop_cos:
         name = "cosf";
         break;
      default:
         assert(0);
      }

      llvm::Function * function = mod->getFunction(name);
      if (!function) {
         // predeclare the intrinsic
         std::vector<llvm::Type*> args;
         args.push_back(floatType);
         llvm::FunctionType* type = llvm::FunctionType::get(floatType,
                                                            llvm::ArrayRef<llvm::Type*>(args),
                                                            false);
         function = llvm::Function::Create(type, llvm::Function::ExternalLinkage, name, mod);
         function->setCallingConv(llvm::CallingConv::C);
      }

      return bld.CreateCall(function, op0);
   }

   llvm::Value* llvm_intrinsic_binop(ir_expression_operation op, llvm::Value * op0, llvm::Value * op1)
   {
      llvm::Type * floatType = llvm::Type::getFloatTy(ctx);
      const char * name = NULL;
      switch (op) {
      case ir_binop_pow:
         name = "powf";
         break;
      default:
         assert(0);
      }

      llvm::Function * function = mod->getFunction(name);
      if (!function) {
         // predeclare the intrinsic
         std::vector<llvm::Type*> args;
         args.push_back(floatType);
         args.push_back(floatType);
         llvm::FunctionType* type = llvm::FunctionType::get(floatType,
                                                            llvm::ArrayRef<llvm::Type*>(args),
                                                            false);
         function = llvm::Function::Create(type, llvm::Function::ExternalLinkage, name, mod);
         function->setCallingConv(llvm::CallingConv::C);
      }

      return bld.CreateCall2(function, op0, op1);
   }

   llvm::Constant* llvm_imm(llvm::Type* type, double v)
   {
      if(type->isVectorTy())
      {
         std::vector<llvm::Constant*> values;
         values.push_back(llvm_imm(((llvm::VectorType*)type)->getElementType(), v));
         for(unsigned i = 1; i < ((llvm::VectorType*)type)->getNumElements(); ++i)
            values.push_back(values[0]);
         return llvm::ConstantVector::get(values);
      }
      else if(type->isIntegerTy())
         return llvm::ConstantInt::get(type, v);
      else if(type->isFloatingPointTy())
         return llvm::ConstantFP::get(type, v);
      else
      {
         assert(0);
         return 0;
      }
   }

   static llvm::Value* create_shuffle3(llvm::IRBuilder<>& bld, llvm::Value* v, unsigned a, unsigned b, unsigned c, const llvm::Twine& name = "")
   {
      llvm::Type* int_ty = llvm::Type::getInt32Ty(v->getContext());
      llvm::Constant* vals[3] = {llvm::ConstantInt::get(int_ty, a), llvm::ConstantInt::get(int_ty, b), llvm::ConstantInt::get(int_ty, c)};
      return bld.CreateShuffleVector(v, llvm::UndefValue::get(v->getType()), llvm::ConstantVector::get(pack(vals)), name);
   }

   llvm::Value* create_select(unsigned width, llvm::Value * cond, llvm::Value * tru, llvm::Value * fal, const char * name = "")
   {
      if (1 == width)
         return bld.CreateSelect(cond, tru, fal, name);

      llvm::Type * vectorType = tru->getType();
      llvm::Value * vector = llvm::Constant::getNullValue(vectorType);
      for (unsigned int i = 0; i < width; i++) {
         llvm::Value * c = bld.CreateExtractElement(cond, llvm_int(i));
         llvm::Value * t = bld.CreateExtractElement(tru, llvm_int(i));
         llvm::Value * f = bld.CreateExtractElement(fal, llvm_int(i));
         llvm::Value * v = bld.CreateSelect(c, t, f, name);
         vector = bld.CreateInsertElement(vector, v, llvm_int(i), "vslct");
      }
      return vector;
   }

   llvm::Value* create_dot_product(llvm::Value* ops0, llvm::Value* ops1, glsl_base_type type, unsigned width)
   {
      llvm::Value* prod;
      switch (type) {
      case GLSL_TYPE_UINT:
      case GLSL_TYPE_INT:
         prod = bld.CreateMul(ops0, ops1, "dot.mul");
         break;
      case GLSL_TYPE_FLOAT:
         prod = bld.CreateFMul(ops0, ops1, "dot.mul");
         break;
      default:
         assert(0);
      }

      if (width<= 1)
         return prod;

      llvm::Value* sum = 0;
      for (unsigned i = 0; i < width; ++i) {
         llvm::Value* elem = bld.CreateExtractElement(prod, llvm_int(i), "dot.elem");
         if (sum) {
            if (type == GLSL_TYPE_FLOAT)
               sum = bld.CreateFAdd(sum, elem, "dot.add");
            else
               sum = bld.CreateAdd(sum, elem, "dot.add");
         }
         else
            sum = elem;
      }
      return sum;
   }

   llvm::Value* llvm_expression(ir_expression* ir)
   {
      llvm::Value* ops[2];
      for(unsigned i = 0; i < ir->get_num_operands(); ++i)
         ops[i] = llvm_value(ir->operands[i]);

      if(ir->get_num_operands() == 2)
      {
         int vecidx = -1;
         int scaidx = -1;
         if(ir->operands[0]->type->vector_elements <= 1 && ir->operands[1]->type->vector_elements > 1)
         {
            scaidx = 0;
            vecidx = 1;
         }
         else if(ir->operands[0]->type->vector_elements > 1 && ir->operands[1]->type->vector_elements <= 1)
         {
            scaidx = 1;
            vecidx = 0;
         }
         else
            assert(ir->operands[0]->type->vector_elements == ir->operands[1]->type->vector_elements);

         if(scaidx >= 0)
         {
            llvm::Value* vec;
            vec = llvm::UndefValue::get(ops[vecidx]->getType());
            for(unsigned i = 0; i < ir->operands[vecidx]->type->vector_elements; ++i)
               vec = bld.CreateInsertElement(vec,  ops[scaidx], llvm_int(i), "sca2vec");
            ops[scaidx] = vec;
         }
      }

      switch (ir->operation) {
      case ir_unop_logic_not:
         return bld.CreateNot(ops[0]);
      case ir_unop_neg:
         switch (ir->operands[0]->type->base_type) {
         case GLSL_TYPE_UINT:
         case GLSL_TYPE_BOOL:
         case GLSL_TYPE_INT:
            return bld.CreateNeg(ops[0]);
         case GLSL_TYPE_FLOAT:
            return bld.CreateFNeg(ops[0]);
         default:
            assert(0);
         }
      case ir_unop_abs:
         switch (ir->operands[0]->type->base_type) {
         case GLSL_TYPE_UINT:
         case GLSL_TYPE_BOOL:
            return ops[0];
         case GLSL_TYPE_INT:
            return create_select(ir->operands[0]->type->vector_elements,
                                 bld.CreateICmpSGE(ops[0], llvm_imm(ops[0]->getType(), 0), "sabs.ge"),
                                 ops[0], bld.CreateNeg(ops[0], "sabs.neg"), "sabs.select");
         case GLSL_TYPE_FLOAT:
            return create_select(ir->operands[0]->type->vector_elements,
                                 bld.CreateFCmpUGE(ops[0], llvm_imm(ops[0]->getType(), 0), "fabs.ge"),
                                 ops[0], bld.CreateFNeg(ops[0], "fabs.neg"), "fabs.select");
         default:
            assert(0);
         }
      case ir_unop_sign:
         switch (ir->operands[0]->type->base_type) {
         case GLSL_TYPE_BOOL:
            return ops[0];
         case GLSL_TYPE_UINT:
            return bld.CreateZExt(bld.CreateICmpNE(ops[0], llvm_imm(ops[0]->getType(), 0), "usign.ne"), ops[0]->getType(), "usign.zext");
         case GLSL_TYPE_INT:
            return bld.CreateSelect(bld.CreateICmpNE(ops[0], llvm_imm(ops[0]->getType(), 0), "ssign.ne"),
                                    bld.CreateSelect(bld.CreateICmpSGE(ops[0], llvm_imm(ops[0]->getType(), 0), "ssign.ge"), llvm_imm(ops[0]->getType(), 1), llvm_imm(ops[0]->getType(), -1), "sabs.selects"),
                                    llvm_imm(ops[0]->getType(), 0), "sabs.select0");
         case GLSL_TYPE_FLOAT:
            return bld.CreateSelect(bld.CreateFCmpONE(ops[0], llvm_imm(ops[0]->getType(), 0), "fsign.ne"),
                                    bld.CreateSelect(bld.CreateFCmpUGE(ops[0], llvm_imm(ops[0]->getType(), 0), "fsign.ge"), llvm_imm(ops[0]->getType(), 1), llvm_imm(ops[0]->getType(), -1), "fabs.selects"),
                                    llvm_imm(ops[0]->getType(), 0), "fabs.select0");
         default:
            assert(0);
         }
      case ir_unop_rcp:
         assert(ir->operands[0]->type->base_type == GLSL_TYPE_FLOAT);
         return bld.CreateFDiv(llvm_imm(ops[0]->getType(), 1), ops[0]);
      case ir_unop_exp: // fall through
      case ir_unop_exp2: // fall through
      case ir_unop_log: // fall through
      case ir_unop_log2: // fall through
      case ir_unop_sin: // fall through
      case ir_unop_cos:
         assert(ir->operands[0]->type->base_type == GLSL_TYPE_FLOAT);
         return llvm_intrinsic_unop(ir->operation, ops[0]);
         // TODO: implement these somehow
      case ir_unop_dFdx:
         assert(0);
         //return llvm_intrinsic(llvm::Intrinsic::ddx, ops[0]);
      case ir_unop_dFdy:
         assert(0);
         //return llvm_intrinsic(llvm::Intrinsic::ddy, ops[0]);
      case ir_binop_add:
         switch(ir->operands[0]->type->base_type)
         {
         case GLSL_TYPE_BOOL:
         case GLSL_TYPE_UINT:
         case GLSL_TYPE_INT:
            return bld.CreateAdd(ops[0], ops[1]);
         case GLSL_TYPE_FLOAT:
            return bld.CreateFAdd(ops[0], ops[1]);
         default:
            assert(0);
         }
      case ir_binop_sub:
         switch(ir->operands[0]->type->base_type)
         {
         case GLSL_TYPE_BOOL:
         case GLSL_TYPE_UINT:
         case GLSL_TYPE_INT:
            return bld.CreateSub(ops[0], ops[1]);
         case GLSL_TYPE_FLOAT:
            return bld.CreateFSub(ops[0], ops[1]);
         default:
            assert(0);
         }
      case ir_binop_mul:
         if (ir->operands[0]->type->is_matrix() && ir->operands[1]->type->is_vector())
            assert(0);
         else if (ir->operands[0]->type->is_vector() && ir->operands[1]->type->is_matrix()) {
            assert(0); // matrix multiplication should have been lowered to vector ops
			llvm::VectorType * vectorType = llvm::VectorType::get(llvm_base_type(ir->operands[1]->type->base_type), ir->operands[1]->type->matrix_columns);
            llvm::Value * vector = llvm::Constant::getNullValue(vectorType);
            for (unsigned int i = 0; i < ir->operands[1]->type->matrix_columns; i++) {
               llvm::Value * value = bld.CreateExtractValue(ops[1], i, "vec*mat_col");
               value = create_dot_product(value, ops[0], ir->operands[1]->type->base_type, ir->operands[1]->type->vector_elements);
               vector = bld.CreateInsertElement(vector, value, llvm_int(i), "vec*mat_res");
            }
            return vector;
         }
         else if (ir->operands[0]->type->is_matrix() && ir->operands[1]->type->is_matrix())
            assert(0);

         switch (ir->operands[0]->type->base_type) {
         case GLSL_TYPE_BOOL:
            return bld.CreateAnd(ops[0], ops[1]);
         case GLSL_TYPE_UINT:
         case GLSL_TYPE_INT:
            return bld.CreateMul(ops[0], ops[1]);
         case GLSL_TYPE_FLOAT:
            return bld.CreateFMul(ops[0], ops[1]);
         default:
            assert(0);
         }
         case ir_binop_div:
         switch(ir->operands[0]->type->base_type)
         {
         case GLSL_TYPE_BOOL:
         case GLSL_TYPE_UINT:
            return bld.CreateUDiv(ops[0], ops[1]);
         case GLSL_TYPE_INT:
            return bld.CreateSDiv(ops[0], ops[1]);
         case GLSL_TYPE_FLOAT:
            return bld.CreateFDiv(ops[0], ops[1]);
         default:
            assert(0);
         }
      case ir_binop_mod:
         switch(ir->operands[0]->type->base_type)
         {
         case GLSL_TYPE_BOOL:
         case GLSL_TYPE_UINT:
            return bld.CreateURem(ops[0], ops[1]);
         case GLSL_TYPE_INT:
            return bld.CreateSRem(ops[0], ops[1]);
         case GLSL_TYPE_FLOAT:
            return bld.CreateFRem(ops[0], ops[1]);
         default:
            assert(0);
         }
      case ir_binop_less:
         switch(ir->operands[0]->type->base_type)
         {
         case GLSL_TYPE_BOOL:
         case GLSL_TYPE_UINT:
            return bld.CreateICmpULT(ops[0], ops[1]);
         case GLSL_TYPE_INT:
            return bld.CreateICmpSLT(ops[0], ops[1]);
         case GLSL_TYPE_FLOAT:
            return bld.CreateFCmpOLT(ops[0], ops[1]);
         default:
            assert(0);
         }
      case ir_binop_greater:
         switch(ir->operands[0]->type->base_type)
         {
         case GLSL_TYPE_BOOL:
         case GLSL_TYPE_UINT:
            return bld.CreateICmpUGT(ops[0], ops[1]);
         case GLSL_TYPE_INT:
            return bld.CreateICmpSGT(ops[0], ops[1]);
         case GLSL_TYPE_FLOAT:
            return bld.CreateFCmpOGT(ops[0], ops[1]);
         default:
            assert(0);
         }
      case ir_binop_lequal:
         switch(ir->operands[0]->type->base_type)
         {
         case GLSL_TYPE_BOOL:
         case GLSL_TYPE_UINT:
            return bld.CreateICmpULE(ops[0], ops[1]);
         case GLSL_TYPE_INT:
            return bld.CreateICmpSLE(ops[0], ops[1]);
         case GLSL_TYPE_FLOAT:
            return bld.CreateFCmpOLE(ops[0], ops[1]);
         default:
            assert(0);
         }
      case ir_binop_gequal:
         switch(ir->operands[0]->type->base_type)
         {
         case GLSL_TYPE_BOOL:
         case GLSL_TYPE_UINT:
            return bld.CreateICmpUGE(ops[0], ops[1]);
         case GLSL_TYPE_INT:
            return bld.CreateICmpSGE(ops[0], ops[1]);
         case GLSL_TYPE_FLOAT:
            return bld.CreateFCmpOGE(ops[0], ops[1]);
         default:
            assert(0);
         }
      case ir_binop_equal: // fall through
      case ir_binop_all_equal: // TODO: check op same as ir_binop_equal
         switch (ir->operands[0]->type->base_type) {
         case GLSL_TYPE_BOOL:
         case GLSL_TYPE_UINT:
         case GLSL_TYPE_INT:
            return bld.CreateICmpEQ(ops[0], ops[1]);
         case GLSL_TYPE_FLOAT:
            return bld.CreateFCmpOEQ(ops[0], ops[1]);
         default:
            assert(0);
         }
      case ir_binop_nequal:
         switch(ir->operands[0]->type->base_type)
         {
         case GLSL_TYPE_BOOL:
         case GLSL_TYPE_UINT:
         case GLSL_TYPE_INT:
            return bld.CreateICmpNE(ops[0], ops[1]);
         case GLSL_TYPE_FLOAT:
            return bld.CreateFCmpONE(ops[0], ops[1]);
         default:
            assert(0);
         }
      case ir_binop_logic_xor:
         assert(ir->operands[0]->type->base_type == GLSL_TYPE_BOOL);
         return bld.CreateICmpNE(ops[0], ops[1]);
      case ir_binop_logic_or:
         assert(ir->operands[0]->type->base_type == GLSL_TYPE_BOOL);
         return bld.CreateOr(ops[0], ops[1]);
      case ir_binop_logic_and:
         assert(ir->operands[0]->type->base_type == GLSL_TYPE_BOOL);
         return bld.CreateAnd(ops[0], ops[1]);
      case ir_binop_dot:
         return create_dot_product(ops[0], ops[1], ir->operands[0]->type->base_type, ir->operands[0]->type->vector_elements);
//      case ir_binop_cross: this op does not exist in ir.h
//         assert(ir->operands[0]->type->vector_elements == 3);
//         switch(ir->operands[0]->type->base_type)
//         {
//         case GLSL_TYPE_UINT:
//         case GLSL_TYPE_INT:
//            return bld.CreateSub(
//                  bld.CreateMul(create_shuffle3(bld, ops[0], 1, 2, 0, "cross.a120"), create_shuffle3(bld, ops[1], 2, 0, 1, "cross.a201"), "cross.ab"),
//                  bld.CreateMul(create_shuffle3(bld, ops[1], 1, 2, 0, "cross.b120"), create_shuffle3(bld, ops[0], 2, 0, 1, "cross.b201"), "cross.ba"),
//                  "cross.sub");
//         case GLSL_TYPE_FLOAT:
//            return bld.CreateFSub(
//                  bld.CreateFMul(create_shuffle3(bld, ops[0], 1, 2, 0, "cross.a120"), create_shuffle3(bld, ops[1], 2, 0, 1, "cross.a201"), "cross.ab"),
//                  bld.CreateFMul(create_shuffle3(bld, ops[1], 1, 2, 0, "cross.b120"), create_shuffle3(bld, ops[0], 2, 0, 1, "cross.b201"), "cross.ba"),
//                  "cross.sub");
//         default:
//            assert(0);
//         }
      case ir_unop_sqrt:
         assert(ir->operands[0]->type->base_type == GLSL_TYPE_FLOAT);
         return llvm_intrinsic_unop(ir->operation, ops[0]);
      case ir_unop_rsq:
         assert(ir->operands[0]->type->base_type == GLSL_TYPE_FLOAT);
         return bld.CreateFDiv(llvm_imm(ops[0]->getType(), 1), llvm_intrinsic_unop(ir_unop_sqrt, ops[0]), "rsqrt.rcp");
      case ir_unop_i2f:
         return bld.CreateSIToFP(ops[0], llvm_type(ir->type));
      case ir_unop_u2f:
      case ir_unop_b2f:
         return bld.CreateUIToFP(ops[0], llvm_type(ir->type));
      case ir_unop_b2i:
         return bld.CreateZExt(ops[0], llvm_type(ir->type));
      case ir_unop_f2i:
         return bld.CreateFPToSI(ops[0], llvm_type(ir->type));
      case ir_unop_f2b:
         return bld.CreateFCmpONE(ops[0], llvm_imm(ops[0]->getType(), 0));
      case ir_unop_i2b:
         return bld.CreateICmpNE(ops[0], llvm_imm(ops[0]->getType(), 0));
      case ir_unop_trunc:
      {
         if(ir->operands[0]->type->base_type != GLSL_TYPE_FLOAT)
            return ops[0];
         glsl_type int_type = *ir->operands[0]->type;
         int_type.base_type = GLSL_TYPE_INT;
         return bld.CreateSIToFP(bld.CreateFPToSI(ops[0], llvm_type(&int_type), "trunc.fptosi"),ops[0]->getType(), "trunc.sitofp");
      }
      case ir_unop_floor:
      {
         if(ir->operands[0]->type->base_type != GLSL_TYPE_FLOAT)
            return ops[0];
         llvm::Value* one = llvm_imm(ops[0]->getType(), 1);
         return bld.CreateFSub(ops[0], bld.CreateFRem(ops[0], one));
      }
      case ir_unop_ceil:
      {
         if(ir->operands[0]->type->base_type != GLSL_TYPE_FLOAT)
            return ops[0];
         llvm::Value* one = llvm_imm(ops[0]->getType(), 1);
         return bld.CreateFAdd(bld.CreateFSub(ops[0], bld.CreateFRem(ops[0], one)), one);
      }
      case ir_unop_fract:
      {
         if(ir->operands[0]->type->base_type != GLSL_TYPE_FLOAT)
            return llvm_imm(ops[0]->getType(), 0);
         llvm::Value* one = llvm_imm(ops[0]->getType(), 1);
         return bld.CreateFRem(ops[0], one);
      }
      // TODO: NaNs might be wrong in min/max, not sure how to fix it
      case ir_binop_min:
         switch(ir->operands[0]->type->base_type)
         {
         case GLSL_TYPE_BOOL:
            return bld.CreateAnd(ops[0], ops[1], "bmin");
         case GLSL_TYPE_UINT:
            return bld.CreateSelect(bld.CreateICmpULE(ops[0], ops[1], "umin.le"), ops[0], ops[1], "umin.select");
         case GLSL_TYPE_INT:
            return bld.CreateSelect(bld.CreateICmpSLE(ops[0], ops[1], "smin.le"), ops[0], ops[1], "smin.select");
         case GLSL_TYPE_FLOAT:
            return bld.CreateSelect(bld.CreateFCmpULE(ops[0], ops[1], "fmin.le"), ops[0], ops[1], "fmin.select");
         default:
            assert(0);
         }
      case ir_binop_max:
         switch(ir->operands[0]->type->base_type)
         {
         case GLSL_TYPE_BOOL:
            return bld.CreateOr(ops[0], ops[1], "bmax");
         case GLSL_TYPE_UINT:
            return bld.CreateSelect(bld.CreateICmpUGE(ops[0], ops[1], "umax.ge"), ops[0], ops[1], "umax.select");
         case GLSL_TYPE_INT:
            return bld.CreateSelect(bld.CreateICmpSGE(ops[0], ops[1], "smax.ge"), ops[0], ops[1], "smax.select");
         case GLSL_TYPE_FLOAT:
            return bld.CreateSelect(bld.CreateFCmpUGE(ops[0], ops[1], "fmax.ge"), ops[0], ops[1], "fmax.select");
         default:
            assert(0);
         }
      case ir_binop_pow:
         assert(GLSL_TYPE_FLOAT == ir->operands[0]->type->base_type);
         assert(GLSL_TYPE_FLOAT == ir->operands[1]->type->base_type);
         return llvm_intrinsic_binop(ir_binop_pow, ops[0], ops[1]);
      case ir_unop_bit_not:
         return bld.CreateNot(ops[0]);
      case ir_binop_bit_and:
         return bld.CreateAnd(ops[0], ops[1]);
      case ir_binop_bit_xor:
         return bld.CreateXor(ops[0], ops[1]);
      case ir_binop_bit_or:
         return bld.CreateOr(ops[0], ops[1]);
      case ir_binop_lshift:
         switch(ir->operands[0]->type->base_type)
         {
         case GLSL_TYPE_BOOL:
         case GLSL_TYPE_UINT:
         case GLSL_TYPE_INT:
            return bld.CreateLShr(ops[0], ops[1]);
         default:
            assert(0);
         }
      case ir_binop_rshift:
         switch(ir->operands[0]->type->base_type)
         {
         case GLSL_TYPE_BOOL:
         case GLSL_TYPE_UINT:
            return bld.CreateLShr(ops[0], ops[1]);
         case GLSL_TYPE_INT:
            return bld.CreateAShr(ops[0], ops[1]);
         default:
            assert(0);
            return 0;
         }
      default:
         printf("ir->operation=%d \n", ir->operation);
         assert(0);
         return 0;
      }
   }

   virtual void visit(class ir_expression * ir)
   {
      result = llvm_expression(ir);
   }

   virtual void visit(class ir_dereference_array *ir)
   {
      result = bld.CreateLoad(llvm_pointer(ir));
   }

   virtual void visit(class ir_dereference_record *ir)
   {
      result = bld.CreateLoad(llvm_pointer(ir));
   }

   virtual void visit(class ir_dereference_variable *ir)
   {
      result = bld.CreateLoad(llvm_pointer(ir), ir->variable_referenced()->name);
   }

   virtual void visit(class ir_texture * ir)
   {
      llvm::Value * coordinate = llvm_value(ir->coordinate);
      if (ir->projector)
      {
         llvm::Value * proj = llvm_value(ir->projector);
         unsigned width = ((llvm::VectorType*)coordinate->getType())->getNumElements();
         llvm::Value * div = llvm::Constant::getNullValue(coordinate->getType());
         for (unsigned i = 0; i < width; i++)
            div = bld.CreateInsertElement(div, proj, bld.getInt32(i), "texProjDup");
         coordinate = bld.CreateFDiv(coordinate, div, "texProj");
      }

      ir_variable * sampler = NULL;
      if(ir_dereference_variable* deref = ir->sampler->as_dereference_variable())
         sampler = deref->variable_referenced();
      else if(ir_dereference_array* deref = ir->sampler->as_dereference_array())
      {
         assert(0); // not implemented
         return;
         deref->array_index;
         deref->array;
      }
      else if(ir->sampler->as_dereference())
      {
         assert(0); // not implemented
         ir_dereference_record* deref = (ir_dereference_record*)ir->sampler;
         int idx = deref->record->type->field_index(deref->field);
         assert(idx >= 0);
      }
      else
         assert(0);

      assert(sampler->location >= 0 && sampler->location < 64); // TODO: proper limit

      // ESSL texture LOD is only for 2D texture in vert shader, and it's explicit
      // bias used only in frag shader, and added to computed LOD
      assert(ir_tex == ir->op);

      assert(GLSL_TYPE_FLOAT == sampler->type->sampler_type);
      printf("sampler '%s' location=%d dim=%d type=%d proj=%d lod=%d \n", sampler->name, sampler->location,
         sampler->type->sampler_dimensionality, sampler->type->sampler_type,
         ir->projector ? 1 : 0, ir->lod_info.lod ? 1 : 0);
      if (GLSL_SAMPLER_DIM_CUBE == sampler->type->sampler_dimensionality)
         result = texCube(bld, coordinate, sampler->location, gglCtx);
      else if (GLSL_SAMPLER_DIM_2D == sampler->type->sampler_dimensionality)
         result = tex2D(bld, coordinate, sampler->location, gglCtx);
      else
         assert(0);
   }

   virtual void visit(class ir_discard * ir)
   {
      llvm::BasicBlock* discard = llvm::BasicBlock::Create(ctx, "discard", fun);
      llvm::BasicBlock* after;
      if(ir->condition)
      {
         after = llvm::BasicBlock::Create(ctx, "discard.survived", fun);
         bld.CreateCondBr(llvm_value(ir->condition), discard, after);
      }
      else
      {
         after = llvm::BasicBlock::Create(ctx, "dead_code.discard", fun);
         bld.CreateBr(discard);
      }

      bld.SetInsertPoint(discard);

      // FIXME: According to the LLVM mailing list, UnwindInst should not
      // be used by the frontend since LLVM 3.0, and 'CreateUnwind'
      // method has been removed from the IRBuilder.  Here's the
      // temporary workaround.  But it would be better to remove
      // this in the future.
      //
      // A solution after LLVM 3.0: To add a global boolean in the shader to
      // store whether it was discarded or not and just continue on normally,
      // and handle the discard outside the shader, in the scanline function.
      // The discard instruction is not used frequently, so it should be okay
      // performance wise.
      //new llvm::UnwindInst(ctx, discard); /// Deprecated

      bb = after;
      bld.SetInsertPoint(bb);
   }

   virtual void visit(class ir_loop_jump *ir)
   {
      llvm::BasicBlock* target;
      if(ir->mode == ir_loop_jump::jump_continue)
         target = loop.first;
      else if(ir->mode == ir_loop_jump::jump_break)
         target = loop.second;
      assert(target);

      bld.CreateBr(target);

      bb = llvm::BasicBlock::Create(ctx, "dead_code.jump", fun);
      bld.SetInsertPoint(bb);
   }

   virtual void visit(class ir_loop * ir)
   {
      llvm::BasicBlock* body = llvm::BasicBlock::Create(ctx, "loop", fun);
      llvm::BasicBlock* header = body;
      llvm::BasicBlock* after = llvm::BasicBlock::Create(ctx, "loop.after", fun);
      llvm::Value* ctr;

      if(ir->counter)
      {
         ctr = llvm_variable(ir->counter);
         if(ir->from)
            bld.CreateStore(llvm_value(ir->from), ctr);
         if(ir->to)
            header = llvm::BasicBlock::Create(ctx, "loop.header", fun);
      }

      bld.CreateBr(header);

      if(ir->counter && ir->to)
      {
         bld.SetInsertPoint(header);
         llvm::Value* cond;
         llvm::Value* load = bld.CreateLoad(ctr);
         llvm::Value* to = llvm_value(ir->to);
         switch(ir->counter->type->base_type)
         {
         case GLSL_TYPE_BOOL:
         case GLSL_TYPE_UINT:
            cond = bld.CreateICmpULT(load, to);
            break;
         case GLSL_TYPE_INT:
            cond = bld.CreateICmpSLT(load, to);
            break;
         case GLSL_TYPE_FLOAT:
            cond = bld.CreateFCmpOLT(load, to);
            break;
         }
         bld.CreateCondBr(cond, body, after);
      }

      bld.SetInsertPoint(body);

      std::pair<llvm::BasicBlock*, llvm::BasicBlock*> saved_loop = loop;
      loop = std::make_pair(header, after);
      visit_exec_list(&ir->body_instructions, this);
      loop = saved_loop;

      if(ir->counter && ir->increment)
      {
         switch(ir->counter->type->base_type)
         {
         case GLSL_TYPE_BOOL:
         case GLSL_TYPE_UINT:
         case GLSL_TYPE_INT:
            bld.CreateStore(bld.CreateAdd(bld.CreateLoad(ctr), llvm_value(ir->increment)), ctr);
            break;
         case GLSL_TYPE_FLOAT:
            bld.CreateStore(bld.CreateFAdd(bld.CreateLoad(ctr), llvm_value(ir->increment)), ctr);
            break;
         }
      }
      bld.CreateBr(header);

      bb = after;
      bld.SetInsertPoint(bb);
   }

   virtual void visit(class ir_if *ir)
   {
      llvm::BasicBlock* bbt = llvm::BasicBlock::Create(ctx, "if", fun);
      llvm::BasicBlock* bbf = llvm::BasicBlock::Create(ctx, "else", fun);
      llvm::BasicBlock* bbe = llvm::BasicBlock::Create(ctx, "endif", fun);
      bld.CreateCondBr(llvm_value(ir->condition), bbt, bbf);

      bld.SetInsertPoint(bbt);
      visit_exec_list(&ir->then_instructions, this);
      bld.CreateBr(bbe);

      bld.SetInsertPoint(bbf);
      visit_exec_list(&ir->else_instructions, this);
      bld.CreateBr(bbe);

      bb = bbe;
      bld.SetInsertPoint(bb);
   }

   virtual void visit(class ir_return * ir)
   {
      if(!ir->value)
         bld.CreateRetVoid();
      else
         bld.CreateRet(llvm_value(ir->value));

      bb = llvm::BasicBlock::Create(ctx, "dead_code.return", fun);
      bld.SetInsertPoint(bb);
   }

   virtual void visit(class ir_call * ir)
   {
      std::vector<llvm::Value*> args;

      foreach_iter(exec_list_iterator, iter, *ir)
      {
         ir_rvalue *arg = (ir_constant *)iter.get();
         args.push_back(llvm_value(arg));
      }

      result = bld.CreateCall(llvm_function(ir->get_callee()), llvm::ArrayRef<llvm::Value*>(args));

      llvm::AttrListPtr attr;
      ((llvm::CallInst*)result)->setAttributes(attr);
   }

   virtual void visit(class ir_constant * ir)
   {
      if (ir->type->base_type == GLSL_TYPE_STRUCT) {
         std::vector<llvm::Constant*> fields;
         foreach_iter(exec_list_iterator, iter, ir->components) {
            ir_constant *field = (ir_constant *)iter.get();
            fields.push_back(llvm_constant(field));
         }
         result = llvm::ConstantStruct::get((llvm::StructType*)llvm_type(ir->type), fields);
      }
      else if (ir->type->base_type == GLSL_TYPE_ARRAY) {
         std::vector<llvm::Constant*> elems;
         for (unsigned i = 0; i < ir->type->length; i++)
            elems.push_back(llvm_constant(ir->array_elements[i]));
         result = llvm::ConstantArray::get((llvm::ArrayType*)llvm_type(ir->type), elems);
      }
      else
      {
         llvm::Type* base_type = llvm_base_type(ir->type->base_type);
         llvm::Type* vec_type = llvm_vec_type(ir->type);
         llvm::Type* type = llvm_type(ir->type);

         std::vector<llvm::Constant*> vecs;
         unsigned idx = 0;
         for (unsigned i = 0; i < ir->type->matrix_columns; ++i) {
            std::vector<llvm::Constant*> elems;
            for (unsigned j = 0; j < ir->type->vector_elements; ++j) {
               llvm::Constant* elem;
               switch(ir->type->base_type)
               {
               case GLSL_TYPE_FLOAT:
                  elem = llvm::ConstantFP::get(base_type, ir->value.f[idx]);
                  break;
               case GLSL_TYPE_UINT:
                  elem = llvm::ConstantInt::get(base_type, ir->value.u[idx]);
                  break;
               case GLSL_TYPE_INT:
                  elem = llvm::ConstantInt::get(base_type, ir->value.i[idx]);
                  break;
               case GLSL_TYPE_BOOL:
                  elem = llvm::ConstantInt::get(base_type, ir->value.b[idx]);
                  break;
               }
               elems.push_back(elem);
               ++idx;
            }

            llvm::Constant* vec;
            if(ir->type->vector_elements > 1) {
               llvm::ArrayRef<llvm::Constant*> ConstantArray(elems);
               vec = llvm::ConstantVector::get(ConstantArray);
            } else {
               vec = elems[0];
            }
            vecs.push_back(vec);
         }

         if(ir->type->matrix_columns > 1)
            result = llvm::ConstantArray::get((llvm::ArrayType*)type, vecs);
         else
            result = vecs[0];
      }
   }

   llvm::Value* llvm_shuffle(llvm::Value* val, int* shuffle_mask, unsigned res_width, const llvm::Twine &name = "")
   {
      llvm::Type* elem_type = val->getType();
      llvm::Type* res_type = elem_type;;
      unsigned val_width = 1;
      if(val->getType()->isVectorTy())
      {
         val_width = ((llvm::VectorType*)val->getType())->getNumElements();
         elem_type = ((llvm::VectorType*)val->getType())->getElementType();
      }
      if(res_width > 1)
         res_type = llvm::VectorType::get(elem_type, res_width);

      llvm::Constant* shuffle_mask_values[4];
      assert(res_width <= 4);
      bool any_def = false;
      for(unsigned i = 0; i < res_width; ++i)
      {
         if(shuffle_mask[i] < 0)
            shuffle_mask_values[i] = llvm::UndefValue::get(llvm::Type::getInt32Ty(ctx));
         else
         {
            any_def = true;
            shuffle_mask_values[i] = llvm_int(shuffle_mask[i]);
         }
      }

      llvm::Value* undef = llvm::UndefValue::get(res_type);
      if(!any_def)
         return undef;

      if(val_width > 1)
      {
         if(res_width > 1)
         {
            if(val_width == res_width)
            {
               bool nontrivial = false;
               for(unsigned i = 0; i < val_width; ++i)
               {
                  if(shuffle_mask[i] != (int)i)
                     nontrivial = true;
               }
               if(!nontrivial)
                  return val;
            }

            return bld.CreateShuffleVector(val, llvm::UndefValue::get(val->getType()), llvm::ConstantVector::get(pack(shuffle_mask_values, res_width)), name);
         }
         else
            return bld.CreateExtractElement(val, llvm_int(shuffle_mask[0]), name);
      }
      else
      {
         if(res_width > 1)
         {
            llvm::Value* tmp = undef;
            for(unsigned i = 0; i < res_width; ++i)
            {
               if(shuffle_mask[i] >= 0)
               tmp = bld.CreateInsertElement(tmp, val, llvm_int(i), name);
            }
            return tmp;
         }
         else if(shuffle_mask[0] >= 0)
            return val;
         else
            return undef;
      }
   }


   virtual void visit(class ir_swizzle * swz)
   {
      llvm::Value* val = llvm_value(swz->val);
      int mask[4] = {swz->mask.x, swz->mask.y, swz->mask.z, swz->mask.w};
      result = llvm_shuffle(val, mask, swz->mask.num_components, "swizzle");
   }

   virtual void visit(class ir_assignment * ir)
   {
      llvm::Value* lhs = llvm_pointer(ir->lhs);
      llvm::Value* rhs = llvm_value(ir->rhs);
      unsigned width = ir->lhs->type->vector_elements;
      unsigned mask = (1 << width) - 1;
      assert(rhs);

      // TODO: masking for matrix assignment
      if (ir->rhs->type->is_matrix()) {
         bld.CreateStore(rhs, lhs, "mat_str");
         return;
      }

      if (!(ir->write_mask & mask))
         return;

      if (ir->rhs->type->vector_elements < width) {
         int expand_mask[4] = {-1, -1, -1, -1};
         for (unsigned i = 0; i < ir->lhs->type->vector_elements; ++i)
            expand_mask[i] = i;
//         printf("ve: %u w %u issw: %i\n", ir->rhs->type->vector_elements, width, !!ir->rhs->as_swizzle());
         rhs = llvm_shuffle(rhs, expand_mask, width, "assign.expand");
      }

      if (width > 1 && (ir->write_mask & mask) != mask) {
         llvm::Constant* blend_mask[4];
         // refer to ir.h: ir_assignment::write_mask
         // A partially-set write mask means that each enabled channel gets
         // the value from a consecutive channel of the rhs.
         unsigned rhsChannel = 0;
         for (unsigned i = 0; i < width; ++i) {
            if (ir->write_mask & (1 << i))
               blend_mask[i] = llvm_int(width + rhsChannel++);
            else
               blend_mask[i] = llvm_int(i);
         }
         rhs = bld.CreateShuffleVector(bld.CreateLoad(lhs), rhs, llvm::ConstantVector::get(pack(blend_mask, width)), "assign.writemask");
      }

      if(ir->condition)
         rhs = bld.CreateSelect(llvm_value(ir->condition), rhs, bld.CreateLoad(lhs), "assign.conditional");

      bld.CreateStore(rhs, lhs);
   }

   virtual void visit(class ir_variable * var)
   {
      llvm_variable(var);
   }

   virtual void visit(ir_function_signature *sig)
   {
      if(!sig->is_defined)
         return;

      assert(!fun);
      fun = llvm_function(sig);

      bb = llvm::BasicBlock::Create(ctx, "entry", fun);
      bld.SetInsertPoint(bb);

      llvm::Function::arg_iterator ai = fun->arg_begin();
      if (!strcmp("main",sig->function_name()))
      {
         assert(3 == fun->arg_size());
         bld.CreateStore(ai, inputsPtr);
         inputs = ai;
         ai++;
         bld.CreateStore(ai, outputsPtr);
         outputs = ai;
         ai++;
         bld.CreateStore(ai, constantsPtr);
         constants = ai;
         ai++;
      }
      else
      {
         foreach_iter(exec_list_iterator, iter, sig->parameters) {
            ir_variable* arg = (ir_variable*)iter.get();
            ai->setName(arg->name);
            bld.CreateStore(ai, llvm_variable(arg));
            ++ai;
         }
         inputs = bld.CreateLoad(inputsPtr);
         outputs = bld.CreateLoad(outputsPtr);
         constants = bld.CreateLoad(constantsPtr);
      }
      inputs->setName("gl_inputs");
      outputs->setName("gl_outputs");
      constants->setName("gl_constants");



      foreach_iter(exec_list_iterator, iter, sig->body) {
         ir_instruction *ir = (ir_instruction *)iter.get();

         ir->accept(this);
      }

      if(fun->getReturnType()->isVoidTy())
         bld.CreateRetVoid();
      else
         bld.CreateRet(llvm::UndefValue::get(fun->getReturnType()));

      bb = NULL;
      fun = NULL;
   }

   virtual void visit(class ir_function * funs)
   {
      foreach_iter(exec_list_iterator, iter, *funs)
      {
         ir_function_signature* sig = (ir_function_signature*)iter.get();
         sig->accept(this);
      }
   }
};

struct llvm::Module *
glsl_ir_to_llvm_module(struct exec_list *ir, llvm::Module * mod,
                        const struct GGLState * gglCtx, const char * shaderSuffix)
{
   ir_to_llvm_visitor v(mod, gglCtx, shaderSuffix);

   visit_exec_list(ir, &v);

//   mod->dump();
   if(llvm::verifyModule(*mod, llvm::PrintMessageAction, 0))
   {
      puts("**\n module verification failed **\n");
      mod->dump();
      assert(0);
      return NULL;
   }

   return mod;
   //v.ir_to_llvm_emit_op1(NULL, OPCODE_END, ir_to_llvm_undef_dst, ir_to_llvm_undef);
}
