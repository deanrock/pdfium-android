package com.kirrupt.pdfiumandroid;

import java.util.List;

import android.content.Context;
import android.graphics.Point;
import android.graphics.PointF;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

public class PageAdapter extends BaseAdapter {
	private final Context mContext;
	private final PDFReader mReader;
	private List<PDFPagerItem> mPages;
	private PointF mSize;
	

	public PageAdapter(Context c, PDFReader reader, List<PDFPagerItem> pages, PointF size) {
		mContext = c;
		mReader = reader;
		mPages = pages;
		mSize = size;
	}

	@Override
	public int getCount() {
		return mPages.size();
	}

	@Override
	public Object getItem(int position) {
		return null;
	}

	@Override
	public long getItemId(int position) {
		return 0;
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		final PageView pageView;
		if (convertView == null) {
			pageView = new PageView(mContext, parent, mReader);	
		} else {
			pageView = (PageView) convertView;
		}

		pageView.resetParentSize();
		pageView.setPage(position, mSize, mPages.get(position));

		return pageView;
	}
}
