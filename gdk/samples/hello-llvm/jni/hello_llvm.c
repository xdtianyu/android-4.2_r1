#if 0
#include <jni.h>

jstring getStr(JNIEnv*);

jstring
Java_com_example_hellollvm_HelloJni_stringFromJNI( JNIEnv* env,
                                                  jobject thiz )
{
    return getStr(env);
}
#endif

int test_func2();

void test_func() {
  test_func2();
}
