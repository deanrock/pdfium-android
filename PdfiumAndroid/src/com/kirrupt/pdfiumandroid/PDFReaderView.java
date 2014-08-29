package com.kirrupt.pdfiumandroid;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.kirrupt.pdfiumandroid.MySerialExecutor.RenderRectangleParams;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.content.Context;
import android.graphics.PointF;
import android.os.Bundle;
import android.os.Debug;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.RelativeLayout;

@SuppressLint("ValidFragment")
public class PDFReaderView extends Fragment {
	private OnPdfChangeListener onPdfChangeListener;
	private PDFReader reader;
	private List<PDFPagerItem>mItems;
	private PointF mSize;
	private static MySerialExecutor mMySerialExecutor;
	private Context mContext;
	Adapter mAdapter;
	ReaderView mDocView;
	int currentPage = 0;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		mContext = getActivity().getApplicationContext();

		createUI(savedInstanceState);
		
		Timer myTimer = new Timer();
	    myTimer.scheduleAtFixedRate(new TimerTask() {
	        @Override
	        public void run() {
	        	MemoryInfo mi = new MemoryInfo();
	    		ActivityManager activityManager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
	    		activityManager.getMemoryInfo(mi);
	    		long availableMegs = mi.availMem / 1048576L;
	    		long used = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
	    		used /= 1048576L;
	    		
	    		long x = Debug.getNativeHeapAllocatedSize() / 1048576L;
	    		
	    		if (BuildConfig.DEBUG) Log.i("memory", "used: "+String.valueOf(used)+", native: "+String.valueOf(x)+", available: "+String.valueOf(availableMegs));
	    		//System.gc();
	        }

	    }, 0, 5000);
	}
	
	public PDFReaderView(List<PDFPagerItem> items, PointF size, OnPdfChangeListener onPdfChangeListener) {
		mItems = items;
		mSize = size;
		
		this.onPdfChangeListener = onPdfChangeListener;
	}
	
	public static MySerialExecutor getMySerialExecutorSingleton() {
		if (mMySerialExecutor == null) {
			mMySerialExecutor = new MySerialExecutor();
		}
		
		return mMySerialExecutor;
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return createUI(savedInstanceState);
	}
	
	private View createUI(Bundle savedInstanceState) {
		RelativeLayout layout = new RelativeLayout(this.getActivity());
		
		RenderRectangleParams params = new RenderRectangleParams();
		params.setCommand(MySerialExecutor.CLOSE_ALL);
		
		PDFReaderView.getMySerialExecutorSingleton().Execute(getActivity(), params);
		
		mDocView = new ReaderView(this.getActivity(), onPdfChangeListener, mItems, mSize, this) {
			private boolean showButtonsDisabled;
			
			@Override
			public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
				return super.onScroll(e1, e2, distanceX, distanceY);
			}

			@Override
			public boolean onScaleBegin(ScaleGestureDetector d) {
				showButtonsDisabled = true;
				return super.onScaleBegin(d);
			}

			@Override
			public boolean onTouchEvent(MotionEvent event) {
				if (event.getActionMasked() == MotionEvent.ACTION_DOWN)
					showButtonsDisabled = false;

				return super.onTouchEvent(event);
			}

			@Override
			protected void onChildSetup(int i, View v) {
			}

			@Override
			protected void onMoveToChild(int i) {
				currentPage = i+1;
				onPdfChangeListener.setCurrentPage(currentPage);			
			}

			@Override
			protected void onSettle(View v) {
				// When the layout has settled ask the page to render
				// in HQ
				((PageView)v).addHq(false);
			}

			@Override
			protected void onUnsettle(View v) {
				// When something changes making the previous settled view
				// no longer appropriate, tell the page to remove HQ
				((PageView)v).removeHq();
			}

			@Override
			protected void onNotInUse(View v) {
				((PageView)v).releaseResources();
			}
		};

		mAdapter = new PageAdapter(this.getActivity(), reader, mItems, mSize);	
		mDocView.setAdapter(mAdapter);

		layout.addView(mDocView);
		layout.setBackgroundResource(R.drawable.background);
		
		return layout;
	}
	
	//Osve�imo trenutni page, ko ga prenesemo (PdfIzdajaActivity - broadcaste)
	public void refreshPage(int page){
		
		if(currentPage==page)
			mDocView.refresh(false);
	}
	
	public void setPageByUser(int page){
		if (BuildConfig.DEBUG) Log.d(PDFReaderView.class.getSimpleName()," page: "+page);
		mDocView.setDisplayedViewIndex(page);

	}
	
	public PDFReaderView() {
		
	}
	
	public PDFReaderView(OnPdfChangeListener onPdfChangeListener){
		this.onPdfChangeListener = onPdfChangeListener;
	}

	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		
		RenderRectangleParams params = new RenderRectangleParams();
		params.setCommand(MySerialExecutor.CLOSE_READER);
		
		PDFReaderView.getMySerialExecutorSingleton().Execute(getActivity(), params);
	}
	
	
}
