package com.kirrupt.pdfiumandroid;

public class JNIWrapper {
	//private native static Boolean LoadDocument(String path);
	
	public native static void helloLog(String message);
	
	static {
	    System.loadLibrary("_native_gyp");
	}
	
	public String Hi() {
		return null;
	}
}
