/*
 * Mesa 3-D graphics library
 * Version:  7.3
 *
 * Copyright (C) 1999-2008  Brian Paul   All Rights Reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
 * BRIAN PAUL BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN
 * AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

/**
 * \file prog_parameter.c
 * Program parameter lists and functions.
 * \author Brian Paul
 */

#ifndef PROG_PARAMETER_H
#define PROG_PARAMETER_H

#include "main/mtypes.h"
#include "prog_statevars.h"


/**
 * Program parameter flags
 */
/*@{*/
#define PROG_PARAM_BIT_CENTROID   0x1  /**< for varying vars (GLSL 1.20) */
#define PROG_PARAM_BIT_INVARIANT  0x2  /**< for varying vars (GLSL 1.20) */
#define PROG_PARAM_BIT_FLAT       0x4  /**< for varying vars (GLSL 1.30) */
#define PROG_PARAM_BIT_LINEAR     0x8  /**< for varying vars (GLSL 1.30) */
#define PROG_PARAM_BIT_CYL_WRAP  0x10  /**< XXX gallium debug */
/*@}*/



/**
 * Program parameter.
 * Used by shaders/programs for uniforms, constants, varying vars, etc.
 */
struct gl_program_parameter
{
   const char *Name;        /**< Null-terminated string */
   //gl_register_file Type;   /**< PROGRAM_NAMED_PARAM, CONSTANT or STATE_VAR */
   //GLenum DataType;         /**< GL_FLOAT, GL_FLOAT_VEC2, etc */
   /**
    * Number of components (1..4), or more.
    * If the number of components is greater than 4,
    * this parameter is part of a larger uniform like a GLSL matrix or array.
    * The next program parameter's Size will be Size-4 of this parameter.
    */
   //GLuint Size;
   GLuint Slots;            /**< how many float[4] slots occupied */
   // location starts from 0, such that VERT_ATTRIB_GENERIC0 = 0
   // since there are no predefined vertex attribs in es20
   // for varyings BindLocation is vertex output location
   // and Loctation is fragment input location
   GLint BindLocation;  /**< requested by BindAttribLocation for attributes */
   GLint Location;          /**< actual location assigned after linking */
   //GLboolean Initialized;   /**< debug: Has the ParameterValue[] been set? */
   //GLbitfield Flags;        /**< Bitmask of PROG_PARAM_*_BIT */
   /**
    * A sequence of STATE_* tokens and integers to identify GL state.
    */
   //gl_state_index StateIndexes[STATE_LENGTH];
};


/**
 * List of gl_program_parameter instances.
 */
struct gl_program_parameter_list
{
   GLuint Size;           /**< allocated size of Parameters, ParameterValues */
   GLuint NumParameters;  /**< number of parameters in arrays */
   struct gl_program_parameter *Parameters; /**< Array [Size] */
   //GLfloat (*ParameterValues)[4];        /**< Array [Size] of GLfloat[4] */
   //GLbitfield StateFlags; /**< _NEW_* flags indicating which state changes
   //                            might invalidate ParameterValues[] */
};

#ifdef __cplusplus
extern "C" {
class ir_variable;
#else
typedef struct ir_variable ir_variable;
#endif

// returns index in paramList or -1
GLint _mesa_add_parameter(struct gl_program_parameter_list * paramList,
                           const char * name);
                                                  
GLint _mesa_get_parameter(const struct gl_program_parameter_list * paramList,
                           const char * name);
#ifdef __cplusplus
}
#endif

#endif /* PROG_PARAMETER_H */
