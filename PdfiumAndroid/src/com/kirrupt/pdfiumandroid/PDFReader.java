package com.kirrupt.pdfiumandroid;

import java.nio.ByteBuffer;

import android.graphics.Bitmap;

public class PDFReader {
	private long nativeHandle;
	
	static {
        System.loadLibrary("_native_gyp");
    }
	
	public PDFReader() {
		initialise();
	}
	
	public native void initialise();
	public native void loadLibrary();
	public native void destroyLibrary();
}

