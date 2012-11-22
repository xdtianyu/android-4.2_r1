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
#include <dlfcn.h>

#define LOG_TAG "QuakeMaster"
#include <utils/Log.h>

int AndroidInit();
int AndroidEvent2(int type, int value);
int AndroidMotionEvent(unsigned long long eventTime, int action,
        float x, float y, float pressure, float size, int deviceId);
int AndroidTrackballEvent(unsigned long long eventTime, int action,
        float x, float y);
int AndroidStep(int width, int height);
void AndroidQuit();

jboolean
qinit(JNIEnv *env, jobject thiz) {
    ALOGI("qinit");
    return AndroidInit() ? JNI_TRUE : JNI_FALSE;
 }

jboolean
qevent(JNIEnv *env, jobject thiz, jint type, jint value) {
    return AndroidEvent2(type, value) ? JNI_TRUE : JNI_FALSE;
}

jboolean
qmotionevent(JNIEnv *env, jobject thiz, jlong eventTime, jint action,
        jfloat x, jfloat y, jfloat pressure, jfloat size, jint deviceId) {
    return AndroidMotionEvent((unsigned long long) eventTime,
            action, x, y, pressure, size,
            deviceId) ? JNI_TRUE : JNI_FALSE;
}

jboolean
qtrackballevent(JNIEnv *env, jobject thiz, jlong eventTime, jint action,
        jfloat x, jfloat y) {
    return AndroidTrackballEvent((unsigned long long) eventTime,
            action, x, y) ? JNI_TRUE : JNI_FALSE;
}

jboolean
qstep(JNIEnv *env, jobject thiz, jint width, jint height) {
    return AndroidStep(width, height)  ? JNI_TRUE : JNI_FALSE;
}

void
qquit(JNIEnv *env, jobject thiz) {
    ALOGI("qquit");
    AndroidQuit();
 }

static const char *classPathName = "com/android/quake/QuakeLib";

static JNINativeMethod methods[] = {
  {"init", "()Z", (void*)qinit },
  {"event", "(II)Z", (void*)qevent },
  {"motionEvent", "(JIFFFFI)Z", (void*) qmotionevent },
  {"trackballEvent", "(JIFF)Z", (void*) qtrackballevent },
  {"step", "(II)Z", (void*)qstep },
  {"quit", "()V", (void*)qquit },
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

typedef union {
    JNIEnv* env;
    void* venv;
} UnionJNIEnvToVoid;

jint JNI_OnLoad(JavaVM* vm, void* reserved)
{
    UnionJNIEnvToVoid uenv;
    uenv.venv = NULL;
    jint result = -1;
    JNIEnv* env = NULL;

    if (vm->GetEnv(&uenv.venv, JNI_VERSION_1_4) != JNI_OK) {
        fprintf(stderr, "ERROR: GetEnv failed\n");
        goto bail;
    }
    env = uenv.env;

    assert(env != NULL);

    printf("In mgmain JNI_OnLoad\n");

    if (!registerNatives(env)) {
        fprintf(stderr, "ERROR: quakemaster native registration failed\n");
        goto bail;
    }

    /* success -- return valid version number */
    result = JNI_VERSION_1_4;

bail:
    return result;
}
