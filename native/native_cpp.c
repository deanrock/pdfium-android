#include <jni.h>
#include <string.h>
#include <android/log.h>

void Java_com_kirrupt_pdfiumandroid_JNIWrapper_helloLog(JNIEnv * env, jobject this, jstring logThis)
{
	helloLog(env, this, logThis);
}
