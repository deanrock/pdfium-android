package com.kirrupt.pdfiumandroid;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceView;
import android.view.WindowManager;

public class MyView1 extends SurfaceView {

	private static final int INVALID_POINTER_ID = -1;
	public Bitmap bitmap;
	
	private Bitmap fullPageBitmap;
	
	private int deviceDisplayWidth = 0;
	private int deviceDisplayHeight = 0;
	
	private int fullPageWidth = 0;
	private int fullPageHeight = 0;
	
	private float mPosX;
	private float mPosY;

	private float mLastTouchX;
	private float mLastTouchY;
	private int mActivePointerId = INVALID_POINTER_ID;

	private ScaleGestureDetector mScaleDetector;
	private float mScaleFactor = 1.f;

	private float focusX;
	private float focusY;

	private float lastFocusX = -1;
	private float lastFocusY = -1;


	static final int IMAGE_X_POS = 0;
	static final int IMAGE_Y_POS = 0;

	int displayWidth;
	int displayHeight;
	Matrix matrix;
	float sy;
	float sx;
	public static Context context;

	private PDFReader reader;
	private PDFDocument document;

	private float renderedForScaleFactor = 0.0f;
	private boolean renderingInProgress = false;

	class RenderPage extends AsyncTask<Integer, String, Bitmap> {

		private float scale;

		@Override
		protected void onPreExecute() {
			// TODO Auto-generated method stub
			super.onPreExecute();
			this.scale = mScaleFactor;
		}

		@Override
		protected Bitmap doInBackground(Integer... params) {
			long startTime = System.currentTimeMillis();

			displayWidth = document.width();
			displayHeight = document.height();
			//create bitmap with the required size
			Log.i(MyView.class.getSimpleName()," display: "+displayWidth+" "+displayHeight);
			Bitmap bitmap = Bitmap.createBitmap((int)displayWidth, (int)displayHeight,
					Config.ARGB_8888);
			
			//render bitmap, izraèunati scale na pravilno velikost glede screena.
			document.renderRectangle((int)displayWidth, (int)displayHeight, 1.0f, 0,0, bitmap);
			
			long renderTime = System.currentTimeMillis() - startTime;
			Log.d("TIME", "render=" + String.valueOf(renderTime));
			return bitmap;
		}

		@Override
		protected void onPostExecute(Bitmap result) {
			// TODO Auto-generated method stub
			super.onPostExecute(result);

			
			fullPageBitmap = result;
			bitmap = result;
			System.gc();

			renderedForScaleFactor = mScaleFactor;

			renderingInProgress = false;

			if (bitmap != null) {
				shouldRerender = false;
				
				Log.d("Render"," PageRender");
				
				setWillNotDraw(false);
				matrix = new Matrix();
				final int width = bitmap.getWidth();
				final int height = bitmap.getHeight();
				sx = document.width() / (float) width;
				sy = document.height() / (float) height;
				matrix.setScale(sx, sy);
								
				invalidate();
			}

			Rerender();
		}
	}
	
	class RenderFullPage extends AsyncTask<Integer, String, Bitmap> {
		@Override
		protected void onPreExecute() {
			// TODO Auto-generated method stub
			super.onPreExecute();
		}

		@Override
		protected Bitmap doInBackground(Integer... params) {
			long startTime = System.currentTimeMillis();
			
			float scaleFactor = 1;
			
			fullPageWidth = deviceDisplayWidth * (int)scaleFactor;
			float ratio = (float)document.width() / (float)deviceDisplayWidth * (int)scaleFactor;
			
			fullPageHeight = (int)(ratio * (float)document.height());
			
			Log.i(MyView1.class.getSimpleName()," render: "+fullPageWidth+" "+fullPageHeight+", ratio: "+ratio);
			
			Bitmap bitmap = Bitmap.createBitmap(fullPageWidth, fullPageHeight, Config.ARGB_8888);
			
			//document.renderRectangle(fullPageWidth, fullPageHeight, ratio, 0,0, bitmap);
			document.renderRectangle(fullPageWidth, fullPageHeight, 1.0f,100,100, bitmap);
			
			long renderTime = System.currentTimeMillis() - startTime;
			
			Log.i(MyView1.class.getSimpleName(), "(fullpage) render=" + String.valueOf(renderTime));
			
			return bitmap;
		}

		@Override
		protected void onPostExecute(Bitmap result) {
			// TODO Auto-generated method stub
			super.onPostExecute(result);

			fullPageBitmap = result;
			bitmap = result;

			if (fullPageBitmap != null) {		
				setWillNotDraw(false);
				matrix = new Matrix();
				final int width = bitmap.getWidth();
				final int height = bitmap.getHeight();
				sx = document.width() / (float) width;
				sy = document.height() / (float) height;
				matrix.setScale(sx, sy);
				invalidate();
			}
		}
	}

	private void Rerender() {
		//Log.d("R", "rerenderInProgress:"+renderingInProgress+" mScaleFactor:"+mScaleFactor+" renderedForScaleFactor:"+renderedForScaleFactor);
		//Log.d("R"," renderingInProgress: "+ !this.renderingInProgress);
		if (this.renderedForScaleFactor != this.mScaleFactor && !this.renderingInProgress) {
			RenderPage r = new RenderPage();
			this.renderingInProgress = true;
			Log.d("R", "in progress");

			int width = (int) ((this.getWidth() > 0) ? this.getWidth() : displayWidth);		
			
			r.execute(width, this.getHeight());
		}

	}

	private boolean shouldRerender = false;


	public MyView1(Context context) {
		this(context, null, 0);


	}

	public MyView1(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public MyView1(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

		mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
		long startTime = System.currentTimeMillis();

		//create PDF reader instance
		reader = new PDFReader();

		//load reader library (you only need to do this once)
		reader.loadLibrary();

		//create PDF document instance
		document = new PDFDocument();
			
		WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		Display display = wm.getDefaultDisplay(); 
		deviceDisplayWidth = display.getWidth();  // deprecated
		deviceDisplayHeight = display.getHeight();  // deprecated
		
		Log.i(MyView.class.getSimpleName()," path: "+Environment.getExternalStorageDirectory());
		boolean result = document.LoadDocument(Environment.getExternalStorageDirectory()+"/delo.pdf");
		
		//load first page
		boolean res = document.LoadPage(0);
		
		Log.d("TEST", "LOAD PAGE " + String.valueOf(res));
		long loadTime = System.currentTimeMillis() - startTime;
		Log.d("TIME", "load=" + String.valueOf(loadTime));
		//Rerender();
		
		RenderFullPage render = new RenderFullPage();
		render.execute(0);
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		// Let the ScaleGestureDetector inspect all events.
		mScaleDetector.onTouchEvent(ev);

		final int action = ev.getAction();
		switch (action & MotionEvent.ACTION_MASK) {
		case MotionEvent.ACTION_DOWN: {

			final float x = ev.getX() / mScaleFactor;
			final float y = ev.getY() / mScaleFactor;
			mLastTouchX = x;
			mLastTouchY = y;
					
			mActivePointerId = ev.getPointerId(0);

			break;
		}

		case MotionEvent.ACTION_MOVE: {
			final int pointerIndex = ev.findPointerIndex(mActivePointerId);
			final float x = ev.getX(pointerIndex) / mScaleFactor;
			final float y = ev.getY(pointerIndex) / mScaleFactor;

			
			// Only move if the ScaleGestureDetector isn't processing a gesture.
			if (!mScaleDetector.isInProgress()) {

				final float dx = x - mLastTouchX;
				final float dy = y - mLastTouchY;
				mPosX += dx;
				mPosY += dy;						

				invalidate();
			}
						
			//TODO: Potrebno je izraèunati položaj slike, èe je out of range, da se nastavi na privzeto vrednost 0		
					
			mLastTouchX = x;
			mLastTouchY = y;
			
			break;
		}

		case MotionEvent.ACTION_UP: {
			mActivePointerId = INVALID_POINTER_ID;
			break;
		}

		case MotionEvent.ACTION_CANCEL: {
			mActivePointerId = INVALID_POINTER_ID;
			break;
		}

		case MotionEvent.ACTION_POINTER_UP: {   

			final int pointerIndex = (ev.getAction() &    MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
			final int pointerId = ev.getPointerId(pointerIndex);
			if (pointerId == mActivePointerId) {
				// This was our active pointer going up. Choose a new
				// active pointer and adjust accordingly.
				final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
				mLastTouchX = ev.getX(newPointerIndex) / mScaleFactor;
				mLastTouchY = ev.getY(newPointerIndex) / mScaleFactor;
				
				mActivePointerId = ev.getPointerId(newPointerIndex);
			}
			break;
		}
		}

		return true;
	}

	@Override
	public void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		setBackgroundColor(Color.RED);
		canvas.save();
		canvas.scale(mScaleFactor, mScaleFactor, focusX, focusY);
		
		
		Log.i("Move","move: "+mPosX+" "+mPosY);
		canvas.translate(mPosX, mPosY);
		canvas.drawBitmap(this.bitmap, getMatrix(), null);
		canvas.restore();  
	}

	private class ScaleListener extends
	ScaleGestureDetector.SimpleOnScaleGestureListener {
		@Override
		public boolean onScaleBegin(ScaleGestureDetector detector) {

			 //float x = detector.getFocusX();
			// float y = detector.getFocusY();

			lastFocusX = -1;
			lastFocusY = -1;

			return true;
		}

		@Override
		public boolean onScale(ScaleGestureDetector detector) {
			mScaleFactor *= detector.getScaleFactor();

			focusX = detector.getFocusX();
			focusY = detector.getFocusY();

			if (lastFocusX == -1)
				lastFocusX = focusX;
			if (lastFocusY == -1)
				lastFocusY = focusY;

			mPosX += (focusX - lastFocusX);
			mPosY += (focusY - lastFocusY);
			Log.v("Zoom", "Factor:"  + mScaleFactor);
			// Don't let the object get too small or too large.
			mScaleFactor = Math.max(1f, Math.min(mScaleFactor, 5.0f));
			
			lastFocusX = focusX;
			lastFocusY = focusY;

			invalidate();
			
			Rerender();
			return true;
		}

	}
	
	
}

