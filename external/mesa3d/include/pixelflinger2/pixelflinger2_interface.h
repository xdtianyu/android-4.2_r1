/**
 **
 ** Copyright 2010, The Android Open Source Project
 **
 ** Licensed under the Apache License, Version 2.0 (the "License");
 ** you may not use this file except in compliance with the License.
 ** You may obtain a copy of the License at
 **
 **     http://www.apache.org/licenses/LICENSE-2.0
 **
 ** Unless required by applicable law or agreed to in writing, software
 ** distributed under the License is distributed on an "AS IS" BASIS,
 ** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ** See the License for the specific language governing permissions and
 ** limitations under the License.
 */

#ifndef _PIXELFLINGER2_INTERFACE_H_
#define _PIXELFLINGER2_INTERFACE_H_

#include "GLES2/gl2.h"
#include "pixelflinger2/pixelflinger2_format.h"
#include "pixelflinger2/pixelflinger2_constants.h"
#include "pixelflinger2/pixelflinger2_vector4.h"

typedef struct gl_shader gl_shader_t;
typedef struct gl_shader_program gl_shader_program_t;

typedef struct VertexInput {
   Vector4 attributes[GGL_MAXVERTEXATTRIBS]; // vert input
}
#ifndef __arm__
__attribute__ ((aligned (16))) // LLVM generates movaps on X86, needs 16 bytes align
#endif
VertexInput_t;

// the layout must NOT change, and must match the #defines in constants.h
typedef struct VertexOutput {
   Vector4 pointSize; // vert output
   Vector4 position; // vert output and frag input gl_FragCoord
   Vector4 varyings[GGL_MAXVARYINGVECTORS];
   Vector4 frontFacingPointCoord; // frag input, gl_FrontFacing gl_PointCoord yzw
   Vector4 fragColor[GGL_MAXDRAWBUFFERS]; // frag output, gl_FragData
}
#ifndef __arm__
__attribute__ ((aligned (16)))
#endif
VertexOutput_t ;

typedef struct GGLSurface {
   unsigned width, height;
   enum GGLPixelFormat format;
   void * data;
   unsigned stride, version;
} GGLSurface_t;

typedef struct GGLTexture {
   unsigned type; // GL_TEXTURE_2D, or GL_TEXTURE_CUBE_MAP

   // currently only support RGBA_8888, RGBX_8888 and RGB_565
   // storage uses either int or short
   enum GGLPixelFormat format; // affects vs/fs jit

   unsigned width, height; // base level dimension
   unsigned levelCount; // mipmapped texture requires power-of-2 width and height

   // data layout is level 0 of first surface (cubemap +x), level 0 of second surface (for cube map, -x),
   // level 0 of 3rd surface (cubemap +y), cubemap level 0 -y, cubemap level 0 +z,
   // cubemap level 0 -z, level 1 of first surface,
   // then level 1 of 1st surface, level 1 of 2nd surface ....
   void * levels;

   // the following affects vs/fs jit; must fit in byte; size used in GetShaderKey
   enum GGLTextureWrap {
      GGL_REPEAT = 0, GGL_CLAMP_TO_EDGE = 1, GGL_MIRRORED_REPEAT = 2
} wrapS :
2, wrapT :
   2;

   enum GGLTextureMinFilter {
      GGL_NEAREST = 0, GGL_LINEAR, /*GGL_NEAREST_MIPMAP_NEAREST = 2,
      GGL_LINEAR_MIPMAP_NEAREST, GGL_NEAREST_MIPMAP_LINEAR, GGL_LINEAR_MIPMAP_LINEAR = 5*/
} minFilter :
1, magFilter :
   1; // only GGL_NEAREST and GGL_LINEAR
} GGLTexture_t;

typedef struct GGLStencilState {
   unsigned char ref, mask; // ref is masked during StencilFuncSeparate

   // GL_NEVER = 0, GL_LESS, GL_EQUAL, GL_LEQUAL, GL_GREATER, GL_NOTEQUAL, GL_GEQUAL,
   // GL_ALWAYS; value = GLenum  & 0x7 (GLenum is 0x200-0x207)
   unsigned char func; // compare function

   // GL_ZERO = 0, GL_KEEP = 1, GL_REPLACE, GL_INCR, GL_DECR, GL_INVERT, GL_INCR_WRAP,
   // GL_DECR_WRAP = 7; value = 0 | GLenum - GL_KEEP | GL_INVERT | GLenum - GL_INCR_WRAP
   unsigned char sFail, dFail, dPass; // operations
}  GGLStencilState_t;

typedef struct GGLActiveStencil { // do not change layout, used in GenerateScanLine
   unsigned char face; // FRONT = 0, BACK = 1
   unsigned char ref, mask;
} GGLActiveStencil_t;

typedef struct GGLBufferState { // all affect scanline jit
   enum GGLPixelFormat colorFormat, depthFormat, stencilFormat;
unsigned stencilTest :
   1;
unsigned depthTest :
   1;
   // same as sf/bFunc; GL_NEVER = 0, GL_LESS, GL_EQUAL, GL_LEQUAL, GL_GREATER, GL_NOTEQUAL,
   // GL_GEQUAL, GL_ALWAYS = 7; value = GLenum  & 0x7 (GLenum is 0x200-0x207)
unsigned depthFunc :
   3;
} GGLBufferState_t;

typedef struct GGLBlendState { // all values affect scanline jit
   unsigned char color[4]; // rgba[0,255]

   // value = 0,1 | GLenum - GL_SRC_COLOR + 2 | GLenum - GL_CONSTANT_COLOR + 11
   enum GGLBlendFactor {
      GGL_ZERO = 0, GGL_ONE, GGL_SRC_COLOR = 2, GGL_ONE_MINUS_SRC_COLOR, GGL_SRC_ALPHA, GGL_ONE_MINUS_SRC_ALPHA,
      GGL_DST_ALPHA, GGL_ONE_MINUS_DST_ALPHA, GGL_DST_COLOR, GGL_ONE_MINUS_DST_COLOR,
      GGL_SRC_ALPHA_SATURATE, GGL_CONSTANT_COLOR = 11, GGL_ONE_MINUS_CONSTANT_COLOR,
      GGL_CONSTANT_ALPHA, GGL_ONE_MINUS_CONSTANT_ALPHA
} scf :
4, saf :
4, dcf :
4, daf :
   4;

   //value = GLenum - GL_FUNC_ADD
   enum GGLBlendFunc {
      GGL_FUNC_ADD = 0, GGL_FUNC_SUBTRACT = 4,
      GGL_FUNC_REVERSE_SUBTRACT = 5
} ce :
3, ae :
   3;

unsigned enable :
   1;
} GGLBlendState_t;

typedef struct GGLTextureState {
   // format affects vs and fs jit
   GGLTexture_t textures[GGL_MAXCOMBINEDTEXTUREIMAGEUNITS]; // the active samplers
   // array of pointers to texture surface data synced to textures; used by LLVM generated texture sampler
   void * textureData[GGL_MAXCOMBINEDTEXTUREIMAGEUNITS];
   // array of texture dimensions synced to textures; by LLVM generated texture sampler
   unsigned textureDimensions[GGL_MAXCOMBINEDTEXTUREIMAGEUNITS * 2];
} GGLTextureState_t;

typedef struct GGLState {
   GGLStencilState_t frontStencil, backStencil; // all affect scanline jit

   GGLBufferState_t bufferState; // all affect scanline jit

   GGLBlendState_t blendState; // all affect scanline jit

   GGLTextureState_t textureState; // most affect vs/fs jit

} GGLState_t;

// most functions are according to GL ES 2.0 spec and uses GLenum values
// there is some error checking for invalid GLenum
typedef struct GGLInterface GGLInterface_t;
struct GGLInterface {
   // these 5 should be moved into libAgl2
   void (* CullFace)(GGLInterface_t * iface, GLenum mode);
   void (* FrontFace)(GGLInterface_t * iface, GLenum mode);
   void (* DepthRangef)(GGLInterface_t * iface, GLclampf zNear, GLclampf zFar);
   void (* Viewport)(GGLInterface_t * iface, GLint x, GLint y, GLsizei width, GLsizei height);
   void (* ViewportTransform)(const GGLInterface_t * iface, Vector4 * v);


   void (* BlendColor)(GGLInterface_t * iface, GLclampf red, GLclampf green,
                       GLclampf blue, GLclampf alpha);
   void (* BlendEquationSeparate)(GGLInterface_t * iface, GLenum modeRGB, GLenum modeAlpha);
   void (* BlendFuncSeparate)(GGLInterface_t * iface, GLenum srcRGB, GLenum dstRGB,
                              GLenum srcAlpha, GLenum dstAlpha);
   void (* EnableDisable)(GGLInterface_t * iface, GLenum cap, GLboolean enable);

   void (* DepthFunc)(GGLInterface_t * iface, GLenum func);
   void (* StencilFuncSeparate)(GGLInterface_t * iface, GLenum face, GLenum func,
                                GLint ref, GLuint mask);
   void (* StencilOpSeparate)(GGLInterface_t * iface, GLenum face, GLenum sfail,
                              GLenum dpfail, GLenum dppass);
   // select GL_FRONT or GL_BACK stencil state before raster/scanline
   void (* StencilSelect)(const GGLInterface_t * iface, GLenum face);
   void (* ClearStencil)(GGLInterface_t * iface, GLint s);
   void (* ClearColor)(GGLInterface_t * iface, GLclampf r, GLclampf g, GLclampf b, GLclampf a);
   void (* ClearDepthf)(GGLInterface_t * iface, GLclampf d);
   void (* Clear)(const GGLInterface_t * iface, GLbitfield buf);

   // shallow copy, surface data pointed to must be valid until texture is set to another texture
   // libAgl2 needs to check ret of ShaderUniform to detect assigning to sampler unit
   void (* SetSampler)(GGLInterface_t * iface, const unsigned sampler, GGLTexture_t * texture);

   // shallow copy, surface data must remain valid; use GL_COLOR_BUFFER_BIT,
   // GL_DEPTH_BUFFER_BIT, GL_STENCIL_BUFFER_BIT; format must be RGBA_8888, Z_32 or S_8
   void (* SetBuffer)(GGLInterface_t * iface, const GLenum type, GGLSurface_t * surface);


   // runs active vertex shader using currently set program; no error checking
   void (* ProcessVertex)(const GGLInterface_t * iface, const VertexInput_t * input,
                          VertexOutput_t * output);
   // draws a triangle given 3 unprocessed vertices; should be moved into libAgl2
   void (* DrawTriangle)(const GGLInterface_t * iface, const VertexInput_t * v0,
                         const VertexInput_t * v1, const VertexInput_t * v2);
   // rasters a vertex processed triangle using active program; scizors to frame surface
   void (* RasterTriangle)(const GGLInterface_t * iface, const VertexOutput_t * v1,
                           const VertexOutput_t * v2, const VertexOutput_t * v3);
   // rasters a vertex processed trapezoid using active program; scizors to frame surface
   void (* RasterTrapezoid)(const GGLInterface_t * iface, const VertexOutput_t * tl,
                            const VertexOutput_t * tr, const VertexOutput_t * bl,
                            const VertexOutput_t * br);

   // scan line given left and right processed and scizored vertices
   void (* ScanLine)(const GGLInterface_t * iface, const VertexOutput_t * v1,
                     const VertexOutput_t * v2);

   // creates empty shader
   gl_shader_t * (* ShaderCreate)(const GGLInterface_t * iface, GLenum type);

   void (* ShaderSource)(gl_shader_t * shader, GLsizei count, const char ** string, const int * length);

   // compiles a shader given glsl; returns GL_TRUE on success; glsl only used during call
   GLboolean (* ShaderCompile)(const GGLInterface_t * iface, gl_shader_t * shader,
                               const char * glsl, const char ** infoLog);

   void (* ShaderDelete)(const GGLInterface_t * iface, gl_shader_t * shader);

   // creates empty program
   gl_shader_program_t * (* ShaderProgramCreate)(const GGLInterface_t * iface);

   // attaches a shader to program
   void (* ShaderAttach)(const GGLInterface_t * iface, gl_shader_program_t * program,
                         gl_shader_t * shader);

   // detaches a shader from program
   void (* ShaderDetach)(const GGLInterface_t * iface, gl_shader_program_t * program,
                         gl_shader_t * shader);

   // duplicates shaders to program, and links varyings / attributes
   GLboolean (* ShaderProgramLink)(gl_shader_program_t * program, const char ** infoLog);
   // frees program
   void (* ShaderProgramDelete)(GGLInterface_t * iface, gl_shader_program_t * program);

   // LLVM JIT and set as active program
   void (* ShaderUse)(GGLInterface_t * iface, gl_shader_program_t * program);

   void (* ShaderGetiv)(const gl_shader_t * shader, const GLenum pname, GLint * params);

   void (* ShaderGetInfoLog)(const gl_shader_t * shader, GLsizei bufsize, GLsizei* length, GLchar* infolog);

   void (* ShaderProgramGetiv)(const gl_shader_program_t * program, const GLenum pname, GLint * params);

   void (* ShaderProgramGetInfoLog)(const gl_shader_program_t * program, GLsizei bufsize, GLsizei* length, GLchar* infolog);

   // bind attribute location before linking
   void (* ShaderAttributeBind)(const gl_shader_program_t * program,
                                GLuint index, const GLchar * name);
   GLint (* ShaderAttributeLocation)(const gl_shader_program_t * program,
                                     const char * name);
   // returns fragment input location and vertex output location for varying of linked program
   GLint (* ShaderVaryingLocation)(const gl_shader_program_t * program,
                                   const char * name, GLint * vertexOutputLocation);
   // gets uniform location for linked program
   GLint (* ShaderUniformLocation)(const gl_shader_program_t * program,
                                   const char * name);
   void (* ShaderUniformGetfv)(gl_shader_program_t * program,
                               GLint location, GLfloat * params);
   void (* ShaderUniformGetiv)(gl_shader_program_t * program,
                               GLint location, GLint * params);

   // retrieves the tmu each sampler is set to, sampler2tmu[sampler] == -1 means not used
   void (* ShaderUniformGetSamplers)(const gl_shader_program_t * program,
                                     int sampler2tmu[GGL_MAXCOMBINEDTEXTUREIMAGEUNITS]);

   // updates linked program uniform value by location; return >= 0 indicates sampler assigned
   GLint (* ShaderUniform)(gl_shader_program_t * program,
                           GLint location, GLsizei count, const GLvoid *values, GLenum type);

   // updates linked program uniform matrix value by location
   void (* ShaderUniformMatrix)(gl_shader_program_t * program, GLint cols,
                                GLint rows, GLint location, GLsizei count,
                                GLboolean transpose, const GLfloat *values);
};

#ifdef __cplusplus
extern "C"
{
#endif

   GGLInterface_t * CreateGGLInterface();

   void DestroyGGLInterface(GGLInterface_t * interface);

   // creates empty shader
   gl_shader_t * GGLShaderCreate(GLenum type);

   // compiles a shader given glsl; returns GL_TRUE on success; glsl only used during call; use infoLog to retrieve status
   GLboolean GGLShaderCompile(gl_shader_t * shader, const char * glsl, const char ** infoLog);

   void GGLShaderDelete(gl_shader_t * shader);

   // creates empty program
   gl_shader_program_t * GGLShaderProgramCreate();

   // attaches a shader to program
   unsigned GGLShaderAttach(gl_shader_program_t * program, gl_shader_t * shader);

   // detaches a shader from program
   unsigned GGLShaderDetach(gl_shader_program_t * program, gl_shader_t * shader);

   // duplicates shaders to program, and links varyings / attributes;
   GLboolean GGLShaderProgramLink(gl_shader_program_t * program, const char ** infoLog);

   // frees program
   void GGLShaderProgramDelete(gl_shader_program_t * program);

   // LLVM JIT and set as active program, also call after gglState change to re-JIT
   void GGLShaderUse(void * llvmCtx, const GGLState_t * gglState, gl_shader_program_t * program);

   void GGLShaderGetiv(const gl_shader_t * shader, const GLenum pname, GLint * params);

   void GGLShaderGetInfoLog(const gl_shader_t * shader, GLsizei bufsize, GLsizei* length, GLchar* infolog);
   
   void GGLShaderProgramGetiv(const gl_shader_program_t * program, const GLenum pname, GLint * params);

   void GGLShaderProgramGetInfoLog(const gl_shader_program_t * program, GLsizei bufsize, GLsizei* length, GLchar* infolog);

   // bind attribute location before linking
   void GGLShaderAttributeBind(const gl_shader_program_t * program,
                               GLuint index, const GLchar * name);
   GLint GGLShaderAttributeLocation(const gl_shader_program_t * program,
                                    const char * name);
   // returns fragment input location and vertex output location for varying of linked program
   GLint GGLShaderVaryingLocation(const gl_shader_program_t * program,
                                  const char * name, GLint * vertexOutputLocation);
   // gets uniform location for linked program
   GLint GGLShaderUniformLocation(const gl_shader_program_t * program,
                                  const char * name);

   void GGLShaderUniformMatrix(gl_shader_program_t * program, GLint cols, GLint rows,
                            GLint location, GLsizei count, GLboolean transpose, const GLfloat *values);

   // retrieves the tmu each sampler is set to, sampler2tmu[sampler] == -1 means not used
   void GGLShaderUniformGetSamplers(const gl_shader_program_t * program,
                                    int sampler2tmu[GGL_MAXCOMBINEDTEXTUREIMAGEUNITS]);

   void GGLProcessVertex(const gl_shader_program_t * program, const VertexInput_t * input,
                         VertexOutput_t * output, const float (*constants)[4]);

   // scan line given left and right processed and scizored vertices
   // depth value bitcast float->int, if negative then ^= 0x7fffffff
   void GGLScanLine(const gl_shader_program_t * program, const enum GGLPixelFormat colorFormat,
                    void * frameBuffer, int * depthBuffer, unsigned char * stencilBuffer,
                    unsigned bufferWidth, unsigned bufferHeight, GGLActiveStencil_t * activeStencil,
                    const VertexOutput_t * start, const VertexOutput_t * end, const float (*constants)[4]);

//   void GGLProcessFragment(const VertexOutput_t * inputs, VertexOutput_t * outputs,
//                           const float (*constants[4]));

#ifdef __cplusplus
}
#endif

#endif // #ifndef _PIXELFLINGER2_INTERFACE_H_
