#include <stdlib.h>
#include <assert.h>

#include <GLES2/gl2.h>

#include "src/mesa/main/mtypes.h"
#include "src/talloc/hieralloc.h"

void _mesa_reference_shader(const void * ctx, struct gl_shader **ptr,
                                          struct gl_shader *sh)
{
   *ptr = sh;
}

struct gl_shader * _mesa_new_shader(const void * ctx, GLuint name, GLenum type)
{
   assert(type == GL_FRAGMENT_SHADER || type == GL_VERTEX_SHADER);
   struct gl_shader * shader = (struct gl_shader *)_hieralloc_zero(ctx, sizeof(struct gl_shader), "zr:gl_shader");
   if (shader) {
      shader->Type = type;
      shader->Name = name;
      shader->RefCount = 1;
   }
   return shader;
}

void _mesa_delete_shader(const void * ctx, struct gl_shader *shader)
{
   if (!shader)
      return;
   if (shader->RefCount > 1) {
      shader->DeletePending = GL_TRUE;
      return;
   }
   hieralloc_free(shader);
}
