/*
 * Similar core function to LGPL licensed talloc from Samba
 */

#ifndef _HIERALLOC_H_
#define _HIERALLOC_H_

#include <stdio.h>
#include <stdarg.h>

// allow __LINE__ to be stringified
#ifndef __location__
#define __HIERALLOC_STRING_0__(s)   #s
#define __HIERALLOC_STRING_1__(s)   __HIERALLOC_STRING_0__(s)
#define __HIERALLOC_STRING_2__      __HIERALLOC_STRING_1__(__LINE__)
#define __location__                __FILE__ ":" __HIERALLOC_STRING_2__
#endif

#define hieralloc(ctx, type) (type *)hieralloc_allocate(ctx, sizeof(type), #type)
#define hieralloc_size(ctx, size) hieralloc_allocate(ctx, size, "sz:"__location__)
#define hieralloc_new(ctx) hieralloc_allocate(ctx, 0, "nw:" __location__)
#define hieralloc_zero(ctx, type) (type *)_hieralloc_zero(ctx, sizeof(type), "zr:"#type)
#define hieralloc_zero_size(ctx, size) _hieralloc_zero(ctx, size, "zrsz:"__location__)
#define hieralloc_array(ctx, type, count) (type *)hieralloc_allocate(ctx, sizeof(type) * count, "ar:"#type)
#define hieralloc_realloc(ctx, p, type, count) (type *)hieralloc_reallocate(ctx, p, sizeof(type) * count, "re:"#type)

#ifdef __cplusplus
extern "C" {
#endif
// allocate memory and attach to parent context and siblings
void * hieralloc_allocate(const void * context, unsigned size, const char * name);

// (re)allocate memory and attach to parent context and siblings
void * hieralloc_reallocate(const void * context, void * ptr, unsigned size, const char * name);

// calls destructor if set, and frees children.
// if destructor returns -1, then do nothing and return -1.
int hieralloc_free(void * ptr);

// creates 0 allocation to be used as parent context
void * hieralloc_init(const char * name);

// returns global context
void * hieralloc_autofree_context();

// sets destructor to be called before freeing; dctor return -1 aborts free
void hieralloc_set_destructor(const void * ptr, int (* destructor)(void *));

// gets parent context of allocated memory
void * hieralloc_parent(const void * ptr);

// moves allocation to new parent context; maintain children but update siblings
// returns ptr on success
void * hieralloc_steal(const void * new_ctx, const void * ptr);

// not implemented from talloc_reference
void * hieralloc_reference(const void * ref_ctx, const void * ptr);

// not implemented from talloc_unlink
int hieralloc_unlink(const void * ctx, void * ptr);

// allocate and zero memory
void * _hieralloc_zero(const void * ctx, unsigned size, const char * name);

// allocate and copy 
char * hieralloc_strdup(const void * ctx, const char * str);

// allocate and copy
char * hieralloc_strndup(const void * ctx, const char * str, unsigned len);

// reallocate and append
char * hieralloc_strdup_append(char * str, const char * append);

// reallocate and append
char * hieralloc_strndup_append(char * str, const char * append, unsigned len);

// allocate and vsprintf
char * hieralloc_vasprintf(const void * ctx, const char * fmt, va_list va);

// allocate and sprintf
char * hieralloc_asprintf(const void * ctx, const char * fmt, ...);

// reallocate and append vsprintf
char * hieralloc_vasprintf_append(char * str, const char * fmt, va_list va);

// reallocate and append sprintf
char * hieralloc_asprintf_append(char * str, const char * fmt, ...);

// report self and child allocations
void hieralloc_report(const void * ptr, FILE * file);

void hieralloc_report_brief(const void * ptr, FILE * file);

void hieralloc_report_lineage(const void * ptr, FILE * file, int tab);

int hieralloc_find(const void * top, const void * ptr, FILE * file, int tab);

#ifdef __cplusplus
}
#endif

#endif
