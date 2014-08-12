#include "PDFDocument.h"
#include <android/bitmap.h>
#include <android/log.h>

#define  LOG_TAG    "DEBUG"
#define  LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)

bool PDFDocument::LoadDocument(const char * path) { 
  this->document = FPDF_LoadDocument(path, NULL);
    
	return (this->document) ? 1 : 0;
}

bool PDFDocument::LoadPage(int pageNum) {
    this->page = FPDF_LoadPage(this->document, pageNum);
    
	return (this->page) ? 1 : 0;
}

int PDFDocument::width() {
    return FPDF_GetPageWidth(this->page);
}

int PDFDocument::height() {
    return FPDF_GetPageHeight(this->page);
}

int PDFDocument::getBufferHeight() {
    return this->bitmapHeight;
}

int PDFDocument::getBufferWidth() {
    return this->bitmapWidth;
}

int PDFDocument::getBufferStride() {
    return FPDFBitmap_GetStride(this->bitmap);
}

int PDFDocument::getBufferSize() {
    if (this->bitmap) {
        return FPDFBitmap_GetStride(this->bitmap) * this->bitmapHeight;
    }
}

void PDFDocument::closePage() {
  if (this->page) {
    FPDF_ClosePage(this->page);
    this->page = NULL;
    LOGD("closing page");
   }
}

void PDFDocument::closeDocument() {
  if (this->document) {
    FPDF_CloseDocument(this->document);
    this->document = NULL;
    LOGD("closing document");
  }
}

bool PDFDocument::RenderPage(int start_x, int start_y, int size_x, int size_y) {
    this->bitmap = (CFX_DIBitmap *)FPDFBitmap_CreateEx(size_x, size_y, FPDFBitmap_BGRA, NULL, 0);
    
    FPDFBitmap_FillRect(this->bitmap, 0, 0, size_x, size_y, 0xFFFFFFFF);
    
    FPDF_RenderPageBitmap(this->bitmap, this->page, start_x, start_y, size_x, size_y, 0, 0);
    
    this->bitmapHeight = size_y;
    this->bitmapWidth = size_x;
    
	return true;
}

inline jbyte fromInt(unsigned char pixel) {
    if(pixel < 0)
        pixel = 0;
    
    if(pixel > 255)
        pixel = 255;
    
    return static_cast<jbyte>(static_cast<unsigned char>(pixel));
}

void PDFDocument::getBuffer(char *buffer)
{
    //char *buf = reinterpret_cast<const char*>(
    //       FPDFBitmap_GetBuffer(this->bitmap));
    
    int pos = 0;
    for (int i = this->bitmapHeight-1;i>=0;i--) {
        for (int j = 0;j<this->bitmapWidth;j++) {
            unsigned int pixel = this->bitmap->GetPixel(j, i);
            
            //std::cout << "px "<<x;
            //print_bytes((void*)&pixel, 4);
            for (int x=0;x<3;x++) {
                
                unsigned char byte = ((unsigned char*)((void*)&pixel))[x];
                //print_bytes((void*)&byte, 1);
                //std::cout << (int)byte << "\n";
                //putc( (int)byte,fptr);
                buffer[pos++] = fromInt(byte);
            }
        }
    }
}

void PDFDocument::getAndroidBitmap(uint8_t* bitmap)
{
    /*int pos = 0;
    for (int i = 0;i<this->bitmapHeight;i++) {
        for (int j = 0;j<this->bitmapWidth;j++) {
            unsigned int pixel = this->bitmap->GetPixel(j, i);
            
            //std::cout << "px "<<x;
            //print_bytes((void*)&pixel, 4)
              unsigned char byte = ((unsigned char*)((void*)&pixel))[2];
              bitmap[pos++] = fromInt(byte);

              byte = ((unsigned char*)((void*)&pixel))[1];
              bitmap[pos++] = fromInt(byte);

              byte = ((unsigned char*)((void*)&pixel))[0];
              bitmap[pos++] = fromInt(byte);

            bitmap[pos++] = 0xFF;//alpha channel
        }
    }*/

    int size = this->bitmapHeight * this->bitmapWidth * 4;

    LOGD("getAndroidBitmap, length: %d", size);

    memcpy(bitmap, this->bitmap->GetBuffer(), size);
}

void PDFDocument::destroyBitmap()
{
  FPDFBitmap_Destroy(this->bitmap);
  this->bitmap = NULL;
}

struct timeval getTime()
{
    struct timeval tv;
    gettimeofday(&tv, NULL);

    return tv;
}

void LogElapsed(struct timeval start_tv, const char* text)
{
    double elapsed = 0.0;

    struct timeval tv = getTime();
    gettimeofday(&tv, NULL);
    elapsed = (tv.tv_sec - start_tv.tv_sec) +
        (tv.tv_usec - start_tv.tv_usec) / 1000000.0;
  
    LOGI("[time] %s elapsed %f", text, elapsed);
}

bool PDFDocument::renderRectangleWithScale(int width, int height, float scale, int start_x, int start_y, uint8_t *bitmap)
{
    if (scale <= 0) {
        LOGE("renderRectangleWithScale wrong scale");
        return false;
    }

    int renderWidth = (float)this->width() * (float)scale;
    int renderHeight = (float)this->height() * (float)scale;

    return this->renderRectangle(width, height, renderWidth, renderHeight,
        start_x, start_y, bitmap);
}

bool PDFDocument::renderRectangle(int width, int height, int renderWidth, int renderHeight, int start_x, int start_y, uint8_t *bitmap)
{
    if (width <= 0 || height <= 0 || start_x < 0 || start_y < 0) {
        LOGE("renderRectangle wrong width,height,start_x or start_y");
        return false;
    }

    if (!this->page) {
        LOGE("page is NULL");
        return false;
    }

    if (!this->document) {
        LOGE("document is NULL");
        return false;
    }

    if (height > renderHeight) {
        LOGE("height %d is bigger than renderheight %d", height, renderHeight);
        height = renderHeight;
    }

    if (width > renderWidth) {
        LOGE("width %d is bigger than renderwidth %d", width, renderWidth);
        width = renderWidth;
    }

    int endX = start_x + width;
    int endY = start_y + height;

    LOGI("bitmap size: %d * %d * 4 = %d", width, height, width*height*4);
    LOGI("renderBitmap size: %d x %d", renderWidth, renderHeight);
    LOGI("start_x=%d, start_y=%d, end_x=%d, end_y=%d",start_x, start_y, endX, endY);

    if (endX > renderWidth) {
        LOGE("end_x %d too big", endX);
        return false;
    }

    if (endY > renderHeight) {
        LOGE("end_y %d too big", endY);
        return false;
    }

    if (start_y > renderHeight) {
        LOGE("start_y %d too big", start_y);
        return false;
    }

    if (start_x > renderWidth) {
        LOGE("start_x %d too big", start_x);
        return false;
    }

    //render
    struct timeval start_tv = getTime();

    CFX_DIBitmap *renderedBitmap = (CFX_DIBitmap *)FPDFBitmap_CreateEx(width, height, FPDFBitmap_BGRA, NULL, 0);
    
    LogElapsed(start_tv, "CreateEx");

    struct timeval tv2 = getTime();
    FPDFBitmap_FillRect(renderedBitmap, 0, 0, width, height, 0xFFFFFFFF);
    LogElapsed(tv2, "FillRect");

    struct timeval tv3 = getTime();
    FPDF_RenderPageBitmap(renderedBitmap, this->page, -start_x, -start_y, renderWidth, renderHeight, 0, 0);
    LogElapsed(tv3, "RenderPage");

    int size = 4 * width * height;

    struct timeval tv4 = getTime();

    int pos = 0;
    for (int i = 0;i<height;i++) {
        for (int j = 0;j<width;j++) {
            unsigned int pixel = renderedBitmap->GetPixel(j, i);
            unsigned char byte = ((unsigned char*)((void*)&pixel))[2];
            bitmap[pos++] = fromInt(byte);

            byte = ((unsigned char*)((void*)&pixel))[1];
            bitmap[pos++] = fromInt(byte);

            byte = ((unsigned char*)((void*)&pixel))[0];
            bitmap[pos++] = fromInt(byte);

            bitmap[pos++] = 0xFF;//alpha channel
        }
    }

    LogElapsed(tv4, "covert bitmap");

    FPDFBitmap_Destroy(renderedBitmap);
    renderedBitmap = NULL;

    return true;
}

JNIEXPORT void JNICALL Java_com_kirrupt_pdfiumandroid_PDFDocument_initialise
(JNIEnv *env, jobject obj)
{
  	PDFDocument *reader = new PDFDocument();
  	setHandle(env, obj, reader);
}

JNIEXPORT jboolean JNICALL Java_com_kirrupt_pdfiumandroid_PDFDocument_LoadDocument
(JNIEnv *env, jobject obj, jstring path)
{
  	PDFDocument *reader = getHandle<PDFDocument>(env, obj);
    
  	const char *pathstr = env->GetStringUTFChars(path, 0);
    
  	bool r = reader->LoadDocument(pathstr);
    
  	return (jboolean)r;
}

JNIEXPORT jboolean JNICALL Java_com_kirrupt_pdfiumandroid_PDFDocument_LoadPage
(JNIEnv *env, jobject obj, jint pageNum)
{
    PDFDocument *reader = getHandle<PDFDocument>(env, obj);
    
    bool r = reader->LoadPage((int)pageNum);
    
    return (jboolean)r;
}

JNIEXPORT jint JNICALL Java_com_kirrupt_pdfiumandroid_PDFDocument_width
(JNIEnv *env, jobject obj)
{
    PDFDocument *reader = getHandle<PDFDocument>(env, obj);
    
    if (reader) {
        return reader->width();
    }
    
    return -1;
}

JNIEXPORT jint JNICALL Java_com_kirrupt_pdfiumandroid_PDFDocument_height
(JNIEnv *env, jobject obj)
{
    PDFDocument *reader = getHandle<PDFDocument>(env, obj);
    
    if (reader) {
        return reader->height();
    }
    
    return -1;
}

JNIEXPORT jobject JNICALL Java_com_kirrupt_pdfiumandroid_PDFDocument_RenderPage
(JNIEnv *env, jobject obj, jint start_x, jint start_y, jint size_x, jint size_y)
{
  	PDFDocument *reader = getHandle<PDFDocument>(env, obj);
    
    reader->RenderPage(start_x, start_y, size_x, size_y);
    
  	return (jboolean)0;
}

JNIEXPORT jint JNICALL Java_com_kirrupt_pdfiumandroid_PDFDocument_getBufferSize
(JNIEnv *env, jobject obj)
{
    PDFDocument *reader = getHandle<PDFDocument>(env, obj);
    
    return reader->getBufferSize();
}

JNIEXPORT void JNICALL Java_com_kirrupt_pdfiumandroid_PDFDocument_getBuffer
(JNIEnv *env, jobject obj, jobject buffer)
{
    PDFDocument *reader = getHandle<PDFDocument>(env, obj);
    
    //jobject output = env->NewDirectByteBuffer((void*)buf, reader->getBufferSize());
    //env->SetObjectArrayElement(buffer, 0, output);
    
    jclass cls = env->GetObjectClass(buffer);
    jmethodID mid = env->GetMethodID(cls, "limit", "(I)Ljava/nio/Buffer;");
    char *buf = (char*)env->GetDirectBufferAddress(buffer);
    //jlong capacity = env->GetDirectBufferCapacity(buffer);
    
    reader->getBuffer(buf);
}

JNIEXPORT jint JNICALL Java_com_kirrupt_pdfiumandroid_PDFDocument_getBufferWidth
(JNIEnv *env, jobject obj)
{
    PDFDocument *reader = getHandle<PDFDocument>(env, obj);
    
    return reader->getBufferWidth();
}

JNIEXPORT jint JNICALL Java_com_kirrupt_pdfiumandroid_PDFDocument_getBufferHeight
(JNIEnv *env, jobject obj)
{
    PDFDocument *reader = getHandle<PDFDocument>(env, obj);
    
    return reader->getBufferHeight();
}

JNIEXPORT jint JNICALL Java_com_kirrupt_pdfiumandroid_PDFDocument_getBufferStride
(JNIEnv *env, jobject obj)
{
    PDFDocument *reader = getHandle<PDFDocument>(env, obj);
    
    return reader->getBufferStride();
}

JNIEXPORT jboolean JNICALL Java_com_kirrupt_pdfiumandroid_PDFDocument_getJavaBitmap
(JNIEnv *env, jobject obj, jobject bitmap)
{
    PDFDocument *reader = getHandle<PDFDocument>(env, obj);
    
    if (!reader) {
        return false;
    }
    
    AndroidBitmapInfo bitmapInfo;
    //uint32_t* storedBitmapPixels = NULL;
    //LOGD("reading bitmap info...");
    int ret;
    if ((ret = AndroidBitmap_getInfo(env, bitmap, &bitmapInfo)) < 0)
    {
        LOGE("AndroidBitmap_getInfo() failed ! error=%d", ret);
        return false;
    }
    LOGD("width:%d height:%d stride:%d", bitmapInfo.width, bitmapInfo.height, bitmapInfo.stride);
    if (bitmapInfo.format != ANDROID_BITMAP_FORMAT_RGBA_8888)
    {
        LOGE("Bitmap format is not RGBA_8888!");
        return false;
    }
    
    void* bitmapPixels;
    if ((ret = AndroidBitmap_lockPixels(env, bitmap, &bitmapPixels)) < 0)
    {
        LOGE("AndroidBitmap_lockPixels() failed ! error=%d", ret);
        return false;
    }
    
    //uint8_t* src = (uint8_t*) bitmapPixels;
    //storedBitmapPixels = new uint32_t[bitmapInfo.height * bitmapInfo.width];
    int pixelsCount = bitmapInfo.height * bitmapInfo.width;
    //memcpy(storedBitmapPixels, src, sizeof(uint32_t) * pixelsCount);
    LOGE("pixelsCount = %d", pixelsCount);

    reader->getAndroidBitmap((uint8_t*)bitmapPixels);

    AndroidBitmap_unlockPixels(env, bitmap);
    
    return true;
}

JNIEXPORT void JNICALL Java_com_kirrupt_pdfiumandroid_PDFDocument_destroyBitmap
  (JNIEnv *env, jobject obj)
  {
    PDFDocument *reader = getHandle<PDFDocument>(env, obj);
    
    if (!reader) {
        return;
    }

    reader->destroyBitmap();
  }

JNIEXPORT void JNICALL Java_com_kirrupt_pdfiumandroid_PDFDocument_closePage
  (JNIEnv *env, jobject obj)
  {
    PDFDocument *reader = getHandle<PDFDocument>(env, obj);
    
    if (!reader) {
        return;
    }

    reader->closePage();
  }

/*
 * Class:     com_kirrupt_pdfiumandroid_PDFDocument
 * Method:    closeDocument
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_kirrupt_pdfiumandroid_PDFDocument_closeDocument
  (JNIEnv *env, jobject obj)
  {
    PDFDocument *reader = getHandle<PDFDocument>(env, obj);
    
    if (!reader) {
        return;
    }

    reader->closeDocument();
  }


  JNIEXPORT jint JNICALL Java_com_kirrupt_pdfiumandroid_PDFDocument_getRectangleBitmapSize
  (JNIEnv *env, jobject obj, jint width, jint height)
  {
    return 4 * (int)width * (int)height;
  }


/*
 * Class:     com_kirrupt_pdfiumandroid_PDFDocument
 * Method:    renderRectangle
 * Signature: (IIIIIILandroid/graphics/Bitmap;)Z
 */
JNIEXPORT jboolean JNICALL Java_com_kirrupt_pdfiumandroid_PDFDocument_renderRectangle
  (JNIEnv *env, jobject obj, jint width, jint height, jint renderWidth, jint renderHeight, jint start_x, jint start_y, jobject bitmap)
  {
    PDFDocument *reader = getHandle<PDFDocument>(env, obj);
    
    if (!reader) {
        return false;
    }

    AndroidBitmapInfo bitmapInfo;
    //uint32_t* storedBitmapPixels = NULL;
    //LOGD("reading bitmap info...");
    int ret;
    if ((ret = AndroidBitmap_getInfo(env, bitmap, &bitmapInfo)) < 0)
    {
        LOGE("AndroidBitmap_getInfo() failed ! error=%d", ret);
        return false;
    }
    LOGI("width:%d height:%d stride:%d", bitmapInfo.width, bitmapInfo.height, bitmapInfo.stride);
    if (bitmapInfo.format != ANDROID_BITMAP_FORMAT_RGBA_8888)
    {
        LOGE("Bitmap format is not RGBA_8888!");
        return false;
    }
    
    void* bitmapPixels;
    if ((ret = AndroidBitmap_lockPixels(env, bitmap, &bitmapPixels)) < 0)
    {
        LOGE("AndroidBitmap_lockPixels() failed ! error=%d", ret);
        return false;
    }

    bool result = false;

    result = reader->renderRectangle(width, height, renderWidth, renderHeight, start_x, start_y, (uint8_t *)bitmapPixels);

    AndroidBitmap_unlockPixels(env, bitmap);

    return result;
  }


/*
 * Class:     com_kirrupt_pdfiumandroid_PDFDocument
 * Method:    renderRectangle
 * Signature: (IIIIILandroid/graphics/Bitmap;)V
 */
JNIEXPORT jboolean JNICALL Java_com_kirrupt_pdfiumandroid_PDFDocument_renderRectangleWithScale
  (JNIEnv *env, jobject obj, jint width, jint height, jfloat scale, jint start_x, jint start_y, jobject bitmap)
  {
    PDFDocument *reader = getHandle<PDFDocument>(env, obj);
    
    if (!reader) {
        return false;
    }

    AndroidBitmapInfo bitmapInfo;
    //uint32_t* storedBitmapPixels = NULL;
    //LOGD("reading bitmap info...");
    int ret;
    if ((ret = AndroidBitmap_getInfo(env, bitmap, &bitmapInfo)) < 0)
    {
        LOGE("AndroidBitmap_getInfo() failed ! error=%d", ret);
        return false;
    }
    LOGI("width:%d height:%d stride:%d", bitmapInfo.width, bitmapInfo.height, bitmapInfo.stride);
    if (bitmapInfo.format != ANDROID_BITMAP_FORMAT_RGBA_8888)
    {
        LOGE("Bitmap format is not RGBA_8888!");
        return false;
    }
    
    void* bitmapPixels;
    if ((ret = AndroidBitmap_lockPixels(env, bitmap, &bitmapPixels)) < 0)
    {
        LOGE("AndroidBitmap_lockPixels() failed ! error=%d", ret);
        return false;
    }

    bool result = false;

    result = reader->renderRectangleWithScale(width, height, scale, start_x, start_y, (uint8_t *)bitmapPixels);

    AndroidBitmap_unlockPixels(env, bitmap);

    return result;
  }

