#include "native.h"

#define DEBUG_TAG "NDK_PdfiumAndroid"

void helloLog(JNIEnv *env, jobject obj, jstring logThis)
{
    jboolean isCopy;
    const char * szLogThis = env->GetStringUTFChars(logThis, &isCopy);

    __android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG, "NDK:LC: [%s]", szLogThis);

    env->ReleaseStringUTFChars(logThis, szLogThis);

}
