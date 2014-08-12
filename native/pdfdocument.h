#include "../fpdfsdk/include/fpdfview.h"
#include "../core/include/fxge/fx_dib.h"
#include "handle.h"
#include "com_kirrupt_pdfiumandroid_PDFDocument.h"

class PDFDocument
{
	FPDF_DOCUMENT document;
	FPDF_PAGE page;
	CFX_DIBitmap *bitmap;

	int bitmapHeight;
	int bitmapWidth;

public:
	bool LoadDocument(const char * path);
	bool LoadPage(int pageNum);
	bool RenderPage(int start_x, int start_y, int size_x, int size_y);

	void getBuffer(char *buffer);
	void getAndroidBitmap(uint8_t* bitmap);

	void destroyBitmap();

	int width();
	int height();
	int getBufferSize();
	int getBufferWidth();
	int getBufferHeight();
	int getBufferStride();

	bool renderRectangleWithScale(int width, int height, float scale, int start_x, int start_y, uint8_t *bitmap);
	bool renderRectangle(int width, int height, int renderWidth, int renderHeight, int start_x, int start_y, uint8_t *bitmap);

	void closePage();
	void closeDocument();
};
