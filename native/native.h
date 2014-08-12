#ifndef _NATIVE_H
#define _NATIVE_H

#include <jni.h>
#include <string.h>
#include <android/log.h>
 
#ifdef __cplusplus
        extern "C" {
#endif
        void helloLog(JNIEnv * env, jobject obj, jstring logThis);
#ifdef __cplusplus
        }
#endif
 
#endif