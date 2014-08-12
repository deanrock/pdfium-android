#include "PDFReader.h"
#include <android/bitmap.h>
#include <android/log.h>

#define  LOG_TAG    "DEBUG"
#define  LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

void PDFReader::loadLibrary() {
    FPDF_InitLibrary(0);
}

void PDFReader::destroyLibrary() {
  FPDF_DestroyLibrary();
}


/*
 * Class:     com_kirrupt_pdfiumandroid_PDFReader
 * Method:    destroyLibrary
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_kirrupt_pdfiumandroid_PDFReader_destroyLibrary
  (JNIEnv *env, jobject obj)
  {
    PDFReader *reader = getHandle<PDFReader>(env, obj);
    
    if (!reader) {
        return;
    }

    reader->destroyLibrary();
  }

JNIEXPORT void JNICALL Java_com_kirrupt_pdfiumandroid_PDFReader_loadLibrary
  (JNIEnv *env, jobject obj)
  {
    PDFReader *reader = getHandle<PDFReader>(env, obj);
    
    if (!reader) {
        return;
    }

    reader->loadLibrary();
  }


JNIEXPORT void JNICALL Java_com_kirrupt_pdfiumandroid_PDFReader_initialise
  (JNIEnv *env, jobject obj)
  {
    PDFReader *reader = new PDFReader();
    setHandle<PDFReader>(env, obj, reader);
  }
