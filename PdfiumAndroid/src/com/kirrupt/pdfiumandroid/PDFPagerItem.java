package com.kirrupt.pdfiumandroid;

public class PDFPagerItem {

	private int id;
	private int page;
	private String fileName;
	private OnPdfChangeListener onPdfChangeListener;

	public PDFPagerItem(int id, int page, String fileName, OnPdfChangeListener onPdfChangeListener){
		this.setId(id);
		this.setPage(page);
		this.setFileName(fileName);
		this.setMyTouchListener(onPdfChangeListener);
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getPage() {
		return page;
	}

	public void setPage(int page) {
		this.page = page;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public OnPdfChangeListener getMyTouchListener() {
		return onPdfChangeListener;
	}

	public void setMyTouchListener(OnPdfChangeListener onPdfChangeListener) {
		this.onPdfChangeListener = onPdfChangeListener;
	}

}
