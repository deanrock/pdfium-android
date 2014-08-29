package com.kirrupt.pdfiumandroid;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.util.Log;

public class MySerialExecutor extends SerialExecutor {
	private Bitmap mBitmap;
	
	public static int RENDER = 1;
	public static int CLOSE = 2;
	public static int CLOSE_ALL = 3;
	public static int CLOSE_READER = 4;
	
	public final ReentrantLock nativeCallLock = new ReentrantLock();
	
	private static boolean isReaderInitialized = false;
	private static PDFReader reader;
	
	private ConcurrentHashMap<String, int[]> mPageObjects;
	
	private HashMap<String, PDFDocument> mDocuments;
	
	public Future<Bitmap> Execute(Context context, RenderRectangleParams params) {
		Future<Bitmap> bitmap;
		
		PDFReaderView.getMySerialExecutorSingleton().nativeCallLock.lock();
		
		try {
			bitmap = PDFReaderView.getMySerialExecutorSingleton().queue(context, params);
		}finally {
			PDFReaderView.getMySerialExecutorSingleton().nativeCallLock.unlock();
		}
		
		return bitmap;
	}
	
	public int[] getNonBlockingPageObjectsForFileName(String fileName) {
		return mPageObjects.get(fileName);
	}
	
	public MySerialExecutor() {
		super();
		
		mDocuments = new HashMap<String, PDFDocument>();
		mPageObjects = new ConcurrentHashMap<String, int[]>();
	}
	
	@Override
	public Future<Bitmap> queue(Context context, TaskParams params) {
		Future<Bitmap> bitmap = mExecutorService.submit(new SerialTask(context, params));
		
		return bitmap;
	}
	
	public void closeAllDocuments() {
		Iterator<Entry<String, PDFDocument>>it  = mDocuments.entrySet().iterator();
		
		while(it.hasNext()) {
			Entry<String, PDFDocument> entry = it.next();
			
			PDFDocument document = entry.getValue();
			
			document.closePage();
			document.closeDocument();
			it.remove();
		}
		
		mPageObjects.clear();
	}

	@Override
	public Bitmap execute(TaskParams p) {
		if (!isReaderInitialized) {
			if (reader == null) {
				reader = new PDFReader();
			}
			
			reader.loadLibrary();
			isReaderInitialized = true;
			if (BuildConfig.DEBUG) Log.i("MySerialExecutor", "initializing reader");
		}
		
		RenderRectangleParams params = (RenderRectangleParams)p;
		
		if (BuildConfig.DEBUG) Log.i("MySerialExecutor", "called with command "+String.valueOf(params.getCommand()));

		if (params.getCommand() == MySerialExecutor.RENDER) {

			long startFullTime = System.currentTimeMillis();

			long startTime = System.currentTimeMillis();

			PDFDocument mDocument;

			if (!mDocuments.containsKey(params.getFileName())) {
				mDocument = new PDFDocument();

				if (BuildConfig.DEBUG) Log.i("MySerialExecutor"," opening: "+params.getFileName());
				boolean result = mDocument.LoadDocument(params.getFileName());

				if (!result) {
					if (BuildConfig.DEBUG) Log.e("MySerialExecutor", "cannot load "+params.getFileName());
					return null;
				}

				//load first page
				boolean res = mDocument.LoadPage(0);

				if (!res) {
					if (BuildConfig.DEBUG) Log.e("MySerialExecutor", "cannot load page 0 from "+params.getFileName());
					return null;
				}

				mDocuments.put(params.getFileName(), mDocument);
				
				//load page objects
				int[] array = mDocument.getPageObjectsSmart();
				
				if (array != null) {
					//mDocument.pageObjects = array;
					mPageObjects.put(params.getFileName(), array);
				}
				
				if (BuildConfig.DEBUG) Log.i("MySerialExecutor", "success: "+array.length);
			}

			mDocument = mDocuments.get(params.getFileName());
			
			int width = mDocument.width();
			int height = mDocument.height();

			float scaleFactor = (float)params.getSizeX() / (float)width;

			long loadTime = System.currentTimeMillis() - startTime;

			if (BuildConfig.DEBUG) Log.i("MySerialExecutor", "document: [w] "+width+", [h] "+height+", render: sizeX: "+
					params.getSizeX()+" sizeY:"+params.getSizeY()+", patchX:"+params.getPatchX()+", patchY: "+params.getPatchY()+", patchWidth: "+
					params.getPatchWidth()+", patchHeight: "+params.getPatchHeight()+", ratio: "+scaleFactor);

			mBitmap = Bitmap.createBitmap(params.getPatchWidth(), params.getPatchHeight(), Config.ARGB_8888);

			boolean result = mDocument.renderRectangle(params.getPatchWidth(), params.getPatchHeight(), params.getSizeX(), params.getSizeY(), params.getPatchX(), params.getPatchY(), mBitmap);

			long fullTime = System.currentTimeMillis() - startFullTime;

			if (BuildConfig.DEBUG) Log.i(MySerialExecutor.class.getSimpleName(), "full=" + String.valueOf(fullTime) + 
					" (load="+String.valueOf(loadTime)+
					")");

			if (!result) {
				return null;
			}
			
			if (BuildConfig.DEBUG) Log.i("MySerialExecutor", "Ending taskk "+params.getCommand());

			return mBitmap;
		}else if (params.command == CLOSE) {
			if (mDocuments.containsKey(params.getFileName())) {
				PDFDocument document = mDocuments.get(params.getFileName());
				
				if (document != null) {
					document.closePage();
					document.closeDocument();
					mDocuments.remove(params.getFileName());
					
					if (mPageObjects.containsKey(params.getFileName())) {
						mPageObjects.remove(params.getFileName());
					}
				}
				
				if (BuildConfig.DEBUG) Log.i("MySerialExecutor", "document closed "+params.getFileName());
			}else{
				if (BuildConfig.DEBUG) Log.e("MySerialExecutor", "couldn't find document to close with url "+params.getFileName());
			}
		}else if(params.command == CLOSE_ALL) {
			closeAllDocuments();
			
			if (BuildConfig.DEBUG) Log.i("MySerialExecutor", "closing all documents...");
		}else if(params.command == CLOSE_READER) {
			closeAllDocuments();
			
			isReaderInitialized = false;
			reader.destroyLibrary();
			
			if (BuildConfig.DEBUG) Log.i("MySerialExecutor", "closing all documents & reader...");
		}
		
		return null;
	}
	
	public static class RenderRectangleParams extends TaskParams {
		public int getPatchWidth() {
			return patchWidth;
		}
		public void setPatchWidth(int patchWidth) {
			this.patchWidth = patchWidth;
		}
		public int getPatchHeight() {
			return patchHeight;
		}
		public void setPatchHeight(int patchHeight) {
			this.patchHeight = patchHeight;
		}
		public int getSizeX() {
			return sizeX;
		}
		public void setSizeX(int sizeX) {
			this.sizeX = sizeX;
		}
		public int getSizeY() {
			return sizeY;
		}
		public void setSizeY(int sizeY) {
			this.sizeY = sizeY;
		}
		public int getPatchX() {
			return patchX;
		}
		public void setPatchX(int patchX) {
			this.patchX = patchX;
		}
		public int getPatchY() {
			return patchY;
		}
		public void setPatchY(int patchY) {
			this.patchY = patchY;
		}
		public Bitmap getBitmap() {
			return bitmap;
		}
		public void setBitmap(Bitmap bitmap) {
			this.bitmap = bitmap;
		}
		public String getFileName() {
			return fileName;
		}
		public void setFileName(String fileName) {
			this.fileName = fileName;
		}
		int patchWidth, patchHeight, sizeX, sizeY, patchX, patchY;
		Bitmap bitmap;
		String fileName;
		int command;
		public int getCommand() {
			return command;
		}
		public void setCommand(int command) {
			this.command = command;
		}
	}
}
