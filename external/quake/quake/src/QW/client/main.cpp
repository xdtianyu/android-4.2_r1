/* //device/apps/Quake/quake/src/QW/client/main.c
**
** Copyright 2007, The Android Open Source Project
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

#include <nativehelper/jni.h>
#include <stdio.h>
#include <assert.h>

#include <GLES/gl.h>
#include <GLES/glext.h>
#include <GLES/gl.h>

// Delegate to "C" routines to do the rest of the processing.
// (We do this because it's difficult to mix the C++ headers
// for JNI and gl_context with the C headers for quake.)

extern "C"
{
void AndroidInit(int width, int height);
int AndroidEvent(int type, int value);
void AndroidStep();
int AndroidQuiting();
}

void
qinit(JNIEnv *env, jobject thiz, /* jobjectArray config, */
       jint width, jint height) {
	AndroidInit(width, height);
}

jboolean
qevent(JNIEnv *env, jobject thiz, jint type, jint value) {
	return AndroidEvent(type, value) ? JNI_TRUE : JNI_FALSE;
}

void
qstep(JNIEnv *env, jobject thiz) {
	AndroidStep();
}

jboolean
qquitting(JNIEnv *env, jobject thiz) {
  return AndroidQuiting() ? JNI_TRUE : JNI_FALSE;
}

static const char *classPathName = "com/android/quake/QuakeApplication";

static JNINativeMethod methods[] = {
  {"init", "(II)V", (void*)qinit },
  {"event", "(II)Z", (void*)qevent },
  {"step", "()V", (void*)qstep },
  {"quitting", "()Z", (void*)qquitting },
};

/*
 * Register several native methods for one class.
 */
static int registerNativeMethods(JNIEnv* env, const char* className,
    JNINativeMethod* gMethods, int numMethods)
{
    jclass clazz;

    clazz = env->FindClass(className);
    if (clazz == NULL) {
        fprintf(stderr,
            "Native registration unable to find class '%s'\n", className);
        return JNI_FALSE;
    }
    if (env->RegisterNatives(clazz, gMethods, numMethods) < 0) {
        fprintf(stderr, "RegisterNatives failed for '%s'\n", className);
        return JNI_FALSE;
    }

    return JNI_TRUE;
}

/*
 * Register native methods for all classes we know about.
 */
static int registerNatives(JNIEnv* env)
{
  if (!registerNativeMethods(env, classPathName,
			     methods, sizeof(methods) / sizeof(methods[0]))) {
    return JNI_FALSE;
  }

  return JNI_TRUE;
}

/*
 * Set some test stuff up.
 *
 * Returns the JNI version on success, -1 on failure.
 */
jint JNI_OnLoad(JavaVM* vm, void* reserved)
{
    JNIEnv* env = NULL;
    jint result = -1;

    if (vm->GetEnv((void**) &env, JNI_VERSION_1_4) != JNI_OK) {
        fprintf(stderr, "ERROR: GetEnv failed\n");
        goto bail;
    }
    assert(env != NULL);

    printf("In mgmain JNI_OnLoad\n");

    if (!registerNatives(env)) {
        fprintf(stderr, "ERROR: miniglobe native registration failed\n");
        goto bail;
    }

    /* success -- return valid version number */
    result = JNI_VERSION_1_4;

bail:
    return result;
}
