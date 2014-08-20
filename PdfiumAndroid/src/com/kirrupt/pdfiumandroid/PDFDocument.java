package com.kirrupt.pdfiumandroid;

import java.nio.ByteBuffer;

import android.graphics.Bitmap;

public class PDFDocument {
	private long nativeHandle;
	
	public PDFDocument() {
		initialise();
	}
	
	public int[] pageObjects;
	
	public native void initialise();
	public native boolean LoadDocument(String path);
	public native boolean LoadPage(int pageNum);
	public native Boolean RenderPage(int start_x, int start_y, int size_x, int size_y);
	
	public native int getBufferSize();
	public native void getBuffer(ByteBuffer buffer);
	
	public native int getBufferWidth();
	public native int getBufferHeight();
	public native int getBufferStride();
	
	public native boolean getJavaBitmap(Bitmap bitmap);
	
	public native int getRectangleBitmapSize(int width, int height);
	
	/**
	 * Renders part of the PDF page that is inside of the following rectangle:
	 * (start_x, start_y, width + start_x, height + start_y)
	 * 
	 * @param  width    the width of resulting rectangle
	 * @param  height   the height of resulting rectangle
	 * @param  scale    the factor with which to scale PDF page (e.g. 1.0f is normal size of PDF page, the same size returned by width() and height() methods)
	 * @param  start_x  the start coordinate from x axis from where to start the rectangle (parameter is in scaled coordinate system)
	 * @param  start_y  the start coordinate from y axis from where to start the rectangle (parameter is in scaled coordinate system)
	 * @param  bitmap   the bitmap reference which has to be initializes with the size of width * height
	 * @return          the {@link Boolean} variable which indicates if method was successful
	 */
	public native boolean renderRectangleWithScale(int width, int height, float scale, int start_x, int start_y, Bitmap bitmap);
	
	/**
	 * Renders part of the PDF page that is inside of the following rectangle:
	 * (start_x, start_y, width + start_x, height + start_y)
	 * 
	 * renderWidth/renderHeight ratio needs to be proportional to width/height ratio!
	 * 
	 * @param  width    the width of resulting rectangle
	 * @param  height   the height of resulting rectangle
	 * @param  renderWidth   the width of rendered surface
	 * @param  renderHeight  the height of rendered surface
	 * @param  start_x  the start coordinate from x axis from where to start the rectangle (parameter is in scaled coordinate system)
	 * @param  start_y  the start coordinate from y axis from where to start the rectangle (parameter is in scaled coordinate system)
	 * @param  bitmap   the bitmap reference which has to be initializes with the size of width * height
	 * @return          the {@link Boolean} variable which indicates if method was successful
	 */
	public native boolean renderRectangle(int width, int height, int renderWidth, int renderHeight, int start_x, int start_y, Bitmap bitmap);
	
	public native void destroyBitmap();
	
	public native int width();
	public native int height();
	
	public native void closePage();
	public native void closeDocument();
	
	public native int[] getPageObjects();
}
