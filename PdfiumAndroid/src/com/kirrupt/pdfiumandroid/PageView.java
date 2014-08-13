package com.kirrupt.pdfiumandroid;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;

import com.kirrupt.pdfiumandroid.MySerialExecutor.RenderRectangleParams;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;

class PatchInfo {
	public BitmapHolder bmh;
	public Bitmap bm;
	public Point patchViewSize;
	public Rect  patchArea;
	public boolean completeRedraw;

	public PatchInfo(Point aPatchViewSize, Rect aPatchArea, BitmapHolder aBmh, boolean aCompleteRedraw) {
		bmh = aBmh;
		bm = null;
		patchViewSize = aPatchViewSize;
		patchArea = aPatchArea;
		completeRedraw = aCompleteRedraw;
	}
}

// Make our ImageViews opaque to optimize redraw
class OpaqueImageView extends ImageView {

	public OpaqueImageView(Context context) {
		super(context);
	}

	@Override
	public boolean isOpaque() {
		return true;
	}
}

public class PageView extends ViewGroup {
	private static final int BACKGROUND_COLOR = 0xFFFFFFFF;
	private static final int PROGRESS_DIALOG_DELAY = 200;
	protected final Context   mContext;
	protected     int       mPageNumber;
	private       Point     mParentSize;
	protected     Point     mSize;   // Size of page at minimum zoom
	protected     PointF    mPDFPageSize;
	protected     float     mSourceScale;

	private       ImageView mEntire; // Image rendered at minimum zoom
	private       BitmapHolder mEntireBmh;
	private       AsyncTask<Void,Void,Bitmap> mDrawEntire;

	private       Point     mPatchViewSize; // View size on the basis of which the patch was created
	private       Rect      mPatchArea;
	private       ImageView mPatch;
	private       BitmapHolder mPatchBmh;
	private       AsyncTask<PatchInfo,Void,PatchInfo> mDrawPatch;
	protected     ArrayList<ArrayList<PointF>> mDrawing;
	private       View      mSearchView;
	
	public boolean forceAddHq = true;
	
	private PDFPagerItem mItem;

	private       ProgressBar mBusyIndicator;
	private final Handler   mHandler = new Handler();
	
	private ViewGroup mParent;

	public PageView(Context c, ViewGroup parent, PDFReader reader) {
		super(c);
		mContext = c;
		
		mParent = parent;
		resetParentSize();
		
		setBackgroundColor(BACKGROUND_COLOR);
		mEntireBmh = new BitmapHolder();
		mPatchBmh = new BitmapHolder();
	}
	
	public void resetParentSize() {
		mParentSize =  new Point(mParent.getWidth(), mParent.getHeight());
		
		if (mSize == null) {
			Log.i("PageView", "mSize == null");
			mSize = mParentSize;
		}
	}

	protected Bitmap drawPage(int sizeX, int sizeY,
			int patchX, int patchY, int patchWidth, int patchHeight) {
		
		if (mItem == null) {
			Log.e("PageView", "mItem is NULL");
			Bitmap b = Bitmap.createBitmap(10, 10, Config.ARGB_8888);
			return b;
		}
		
		resetSize();
		
		Log.i("PageView", "dP "+mParent.getWidth()+", "+mParent.getHeight());
		
		String mRealPath = Environment.getExternalStorageDirectory()+"/"+mItem.getFileName();
		File f = new File(mRealPath);
		if(!f.exists())
			blank(mPageNumber);
		
		RenderRectangleParams params = new RenderRectangleParams();
		params.setFileName(mRealPath);
		params.setSizeX(sizeX);
		params.setSizeY(sizeY);
		params.setPatchWidth(patchWidth);
		params.setPatchHeight(patchHeight);
		params.setPatchX(patchX);
		params.setPatchY(patchY);
		params.setCommand(MySerialExecutor.RENDER);
		
		Future<Bitmap> bitmap = PDFReaderView.getMySerialExecutorSingleton().Execute(this.getContext(), params);
		
		Bitmap b = null;
		
		try {
			b = bitmap.get();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if (b == null) {
			Log.e("PageView", "Bitmap is NULL!!!!");
			b = Bitmap.createBitmap(10, 10, Config.ARGB_8888);
		}else{
			
		}
		
		return b;
		//return mReader.drawPage(mPageNumber, sizeX, sizeY, patchX, patchY, patchWidth, patchHeight);
	}

	private void reinit() {
		// Cancel pending render task
		if (mDrawEntire != null) {
			mDrawEntire.cancel(true);
			mDrawEntire = null;
		}

		if (mDrawPatch != null) {
			mDrawPatch.cancel(true);
			mDrawPatch = null;
		}

		mPageNumber = 0;

		if (mSize == null)
			mSize = mParentSize;

		if (mEntire != null) {
			mEntire.setImageBitmap(null);
			mEntireBmh.setBm(null);
		}

		if (mPatch != null) {
			mPatch.setImageBitmap(null);
			mPatchBmh.setBm(null);
		}

		mPatchViewSize = null;
		mPatchArea = null;
	}

	public void releaseResources() {
		reinit();

		if (mBusyIndicator != null) {
			removeView(mBusyIndicator);
			mBusyIndicator = null;
		}
	}


	
	public void blank(int page) {
		reinit();
		mPageNumber = page;

		if (mBusyIndicator == null) {
			mBusyIndicator = new ProgressBar(mContext);
			mBusyIndicator.setIndeterminate(true);
			//mBusyIndicator.setBackgroundResource(R.drawable.busy);
			addView(mBusyIndicator);
		}
		
		setBackgroundColor(BACKGROUND_COLOR);
	}
	
	public void setPage(int page, PointF size, PDFPagerItem item) {
		mPageNumber = page;
		mPDFPageSize = size;
		
		if (mItem != null) {
			//close previous page
			
			String mRealPath = Environment.getExternalStorageDirectory()+"/"+mItem.getFileName();
			
			RenderRectangleParams params = new RenderRectangleParams();
			params.setFileName(mRealPath);
			params.setCommand(MySerialExecutor.CLOSE);
			
			PDFReaderView.getMySerialExecutorSingleton().Execute(this.getContext(), params);
		}
		
		mItem = item;
		
		reloadPage();
	}
	
	public void resetSize() {
		mParentSize =  new Point(mParent.getWidth(), mParent.getHeight());
		
		mSourceScale = Math.min(mParentSize.x/mPDFPageSize.x, mParentSize.y/mPDFPageSize.y);
		Point newSize = new Point((int)(mPDFPageSize.x*mSourceScale), (int)(mPDFPageSize.y*mSourceScale));
		mSize = newSize;
	}

	public void reloadPage() {
		// Cancel pending render task
		if (mDrawEntire != null) {
			mDrawEntire.cancel(true);
			mDrawEntire = null;
		}

		// Highlights may be missing because mIsBlank was true on last draw
		if (mSearchView != null)
			mSearchView.invalidate();
		
		if (mEntire == null) {
			mEntire = new OpaqueImageView(mContext);
			mEntire.setScaleType(ImageView.ScaleType.FIT_CENTER);
			addView(mEntire);
		}

		// Calculate scaled size that fits within the screen limits
		// This is the size at minimum zoom
		resetSize();

		mEntire.setImageBitmap(null);
		mEntireBmh.setBm(null);

		// Render the page in the background
		mDrawEntire = new AsyncTask<Void,Void,Bitmap>() {
			@Override
			protected Bitmap doInBackground(Void... v) {
				return drawPage(mSize.x, mSize.y, 0, 0, mSize.x, mSize.y);
			}

			@Override
			protected void onPreExecute() {
				setBackgroundColor(BACKGROUND_COLOR);
				mEntire.setImageBitmap(null);
				mEntireBmh.setBm(null);

				if (mBusyIndicator == null) {
					mBusyIndicator = new ProgressBar(mContext);
					mBusyIndicator.setIndeterminate(true);
					//mBusyIndicator.setBackgroundResource(R.drawable.busy);
					addView(mBusyIndicator);
					mBusyIndicator.setVisibility(INVISIBLE);
					mHandler.postDelayed(new Runnable() {
						@Override
						public void run() {
							if (mBusyIndicator != null)
								mBusyIndicator.setVisibility(VISIBLE);
						}
					}, PROGRESS_DIALOG_DELAY);
				}
			}

			@Override
			protected void onPostExecute(Bitmap bm) {
				removeView(mBusyIndicator);
				mBusyIndicator = null;
				mEntire.setImageBitmap(bm);
				mEntireBmh.setBm(bm);
				setBackgroundColor(Color.TRANSPARENT);
				
				if (forceAddHq) {
					forceAddHq = false;
					addHq(true);
				}
			}
		};

		mDrawEntire.execute();
		
		requestLayout();
		
		forceAddHq = true;
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int x, y;
		switch(View.MeasureSpec.getMode(widthMeasureSpec)) {
		case View.MeasureSpec.UNSPECIFIED:
			x = mSize.x;
			break;
		default:
			x = View.MeasureSpec.getSize(widthMeasureSpec);
		}
		switch(View.MeasureSpec.getMode(heightMeasureSpec)) {
		case View.MeasureSpec.UNSPECIFIED:
			y = mSize.y;
			break;
		default:
			y = View.MeasureSpec.getSize(heightMeasureSpec);
		}

		setMeasuredDimension(x, y);

		if (mBusyIndicator != null) {
			int limit = Math.min(mParentSize.x, mParentSize.y)/2;
			mBusyIndicator.measure(View.MeasureSpec.AT_MOST | limit, View.MeasureSpec.AT_MOST | limit);
		}
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		int w  = right-left;
		int h = bottom-top;

		if (mEntire != null) {
			mEntire.layout(0, 0, w, h);
		}

		if (mSearchView != null) {
			mSearchView.layout(0, 0, w, h);
		}

		if (mPatchViewSize != null) {
			if (mPatchViewSize.x != w || mPatchViewSize.y != h) {
				// Zoomed since patch was created
				mPatchViewSize = null;
				mPatchArea     = null;
				if (mPatch != null) {
					mPatch.setImageBitmap(null);
					mPatchBmh.setBm(null);
				}
			} else {
				mPatch.layout(mPatchArea.left, mPatchArea.top, mPatchArea.right, mPatchArea.bottom);
			}
		}

		if (mBusyIndicator != null) {
			int bw = mBusyIndicator.getMeasuredWidth();
			int bh = mBusyIndicator.getMeasuredHeight();

			mBusyIndicator.layout((w-bw)/2, (h-bh)/2, (w+bw)/2, (h+bh)/2);
		}
	}

	public void addHq(boolean update) {
		Log.d("PageView", "addHq "+update);
		resetSize();
		Rect viewArea = new Rect(getLeft(),getTop(),getRight(),getBottom());
		// If the viewArea's size matches the unzoomed size, there is no need for an hq patch
		if (viewArea.width() != mSize.x || viewArea.height() != mSize.y) {
			
			Point patchViewSize = new Point(viewArea.width(), viewArea.height());
			
			Log.d("PageView", mItem.getFileName()+" doesnt match "+mParentSize.x+", "+mParentSize.y+ " --- view: "+viewArea.width() +","+viewArea.height());
			
			Rect patchArea = new Rect(0, 0, mParentSize.x, mParentSize.y);

			// Intersect and test that there is an intersection
			if (!patchArea.intersect(viewArea)) {
				return;
			}
			
			// Offset patch area to be relative to the view top left
			patchArea.offset(-viewArea.left, -viewArea.top);

			boolean area_unchanged = patchArea.equals(mPatchArea) && patchViewSize.equals(mPatchViewSize);

			// If being asked for the same area as last time and not because of an update then nothing to do
			if (area_unchanged && !update)
				return;

			boolean completeRedraw = !(area_unchanged && update);

			// Stop the drawing of previous patch if still going
			if (mDrawPatch != null) {
				mDrawPatch.cancel(true);
				mDrawPatch = null;
			}

			mPatchBmh.drop();
			mPatchBmh = new BitmapHolder();

			// Create and add the image view if not already done
			if (mPatch == null) {
				mPatch = new OpaqueImageView(mContext);
				mPatch.setScaleType(ImageView.ScaleType.FIT_CENTER);
				addView(mPatch);
				//mSearchView.bringToFront();
			}

			mDrawPatch = new AsyncTask<PatchInfo,Void,PatchInfo>() {
				@Override
				protected PatchInfo doInBackground(PatchInfo... v) {
					v[0].bm = drawPage(v[0].patchViewSize.x, v[0].patchViewSize.y,
									v[0].patchArea.left, v[0].patchArea.top,
									v[0].patchArea.width(), v[0].patchArea.height());

					return v[0];
				}

				@Override
				protected void onPostExecute(PatchInfo v) {
					Log.i("PageView", "postExecute");
					if (mPatchBmh == v.bmh) {
						Log.i("PageView", "patch == bmh");
						mPatchViewSize = v.patchViewSize;
						mPatchArea     = v.patchArea;
						if (v.bm != null) {
							mPatch.setImageBitmap(v.bm);
							v.bmh.setBm(v.bm);
							v.bm = null;
						}
						//requestLayout();
						// Calling requestLayout here doesn't lead to a later call to layout. No idea
						// why, but apparently others have run into the problem.
						mPatch.layout(mPatchArea.left, mPatchArea.top, mPatchArea.right, mPatchArea.bottom);
						invalidate();
					}else{
						Log.i("PageView", "patch NOT bmp");
					}
				}
			};

			mDrawPatch.execute(new PatchInfo(patchViewSize, patchArea, mPatchBmh, completeRedraw));
		}
	}

	public void update() {
		// Cancel pending render task
		if (mDrawEntire != null) {
			mDrawEntire.cancel(true);
			mDrawEntire = null;
		}

		if (mDrawPatch != null) {
			mDrawPatch.cancel(true);
			mDrawPatch = null;
		}

		// Render the page in the background
		mDrawEntire = new AsyncTask<Void,Void,Bitmap>() {
			@Override
			protected Bitmap doInBackground(Void... v) {
				// Pass the current bitmap as a basis for the update, but use a bitmap
				// holder so that the held bitmap will be nulled and not hold on to
				// memory, should this view become redundant.
				return drawPage(mSize.x, mSize.y, 0, 0, mSize.x, mSize.y);
			}

			@Override
			protected void onPostExecute(Bitmap bm) {
				if (bm != null) {
					mEntire.setImageBitmap(bm);
					mEntireBmh.setBm(bm);
				}
				invalidate();
			}
		};

		mDrawEntire.execute();

		addHq(true);
	}

	public void removeHq() {
			// Stop the drawing of the patch if still going
			if (mDrawPatch != null) {
				mDrawPatch.cancel(true);
				mDrawPatch = null;
			}

			// And get rid of it
			mPatchViewSize = null;
			mPatchArea = null;
			if (mPatch != null) {
				mPatch.setImageBitmap(null);
				mPatchBmh.setBm(null);
			}
	}

	public int getPage() {
		return mPageNumber;
	}

	@Override
	public boolean isOpaque() {
		return true;
	}
	
}
