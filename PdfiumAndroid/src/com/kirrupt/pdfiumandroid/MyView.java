package com.kirrupt.pdfiumandroid;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Bitmap.Config;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.WindowManager;

public class MyView extends View {
	
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

			this.scale = scaleFactor;
		}

		@Override
		protected Bitmap doInBackground(Integer... params) {
			long startTime = System.currentTimeMillis();

			//create bitmap with the required size
			Bitmap bitmap = Bitmap.createBitmap((int)displayWidth, (int)displayHeight,
					Config.ARGB_8888);
			
			//render bitmap
			document.renderRectangleWithScale((int)displayWidth, (int)displayHeight, scale*1.0f, (int)(scale*1), (int)(scale*1), bitmap);

			long renderTime = System.currentTimeMillis() - startTime;
			Log.d("TIME", "render=" + String.valueOf(renderTime));
			return bitmap;
		}

		@Override
		protected void onPostExecute(Bitmap result) {
			// TODO Auto-generated method stub
			super.onPostExecute(result);

			bitmap = result;
			System.gc();

			renderedForScaleFactor = scaleFactor;

			renderingInProgress = false;

			if (bitmap != null) {
				shouldRerender = false;
				invalidate();
			}

			Rerender();
		}
	}

	private void Rerender() {
		Log.d("R", "rerender");
		if (this.renderedForScaleFactor != this.scaleFactor
				&& !this.renderingInProgress) {
			RenderPage r = new RenderPage();
			this.renderingInProgress = true;
			Log.d("R", "in progress");
			
			int width = (int) ((this.getWidth() > 0) ? this.getWidth() : displayWidth);
			r.execute(width, this.getHeight());
		}

	}

	private boolean shouldRerender = false;

	public Bitmap bitmap = null;

	public MyView(Context context) {
		super(context);

		detector = new ScaleGestureDetector(getContext(), new ScaleListener());
	}

	public MyView(Context context, AttributeSet attrs) {
		super(context, attrs);

		detector = new ScaleGestureDetector(getContext(), new ScaleListener());
 
		long startTime = System.currentTimeMillis();
		
		//create PDF reader instance
		reader = new PDFReader();
		
		//load reader library (you only need to do this once)
		reader.loadLibrary();
		
		//create PDF document instance
		document = new PDFDocument();
		
		//load document from path
		boolean result = document.LoadDocument("/sdcard/delo.pdf");
		
		//load first page
		boolean res = document.LoadPage(0);
		
		Log.d("TEST", "LOAD PAGE " + String.valueOf(res));
		long loadTime = System.currentTimeMillis() - startTime;
		Log.d("TIME", "load=" + String.valueOf(loadTime));
		
		
		WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		Display display = wm.getDefaultDisplay(); 

		displayWidth = display.getWidth();
		displayHeight = display.getHeight();
		Log.d("SCREEN", String.valueOf(displayWidth)+" "+String.valueOf(displayHeight));
		Rerender();
			
		
		//close page
		//document.closePage();
		
		//close document
		//document.closeDocument();
		
		//destroy library
		//reader.destroyLibrary();
}

	@Override
	protected void onDraw(Canvas canvas) {
		// TODO Auto-generated method stub

		/*super.onDraw(canvas);

		Log.d("DRAW", "draw");
		canvas.save();

		canvas.translate(mPosX, mPosY);
		canvas.scale(scaleFactor, scaleFactor);

		if (this.bitmap != null) {
			canvas.drawBitmap(this.bitmap, 0, 0, null);
		}

		canvas.restore();*/
		
		super.onDraw(canvas);

        canvas.save();

        //We're going to scale the X and Y coordinates by the same amount
        //canvas.scale(scaleFactor, scaleFactor);
        canvas.scale(this.scaleFactor, this.scaleFactor, this.detector.getFocusX(), this.detector.getFocusY());

        //If translateX times -1 is lesser than zero, let's set it to zero. This takes care of the left bound
        if((translateX * -1) < 0) {
           translateX = 0;
        }

        //This is where we take care of the right bound. We compare translateX times -1 to (scaleFactor - 1) * displayWidth.
        //If translateX is greater than that value, then we know that we've gone over the bound. So we set the value of 
        //translateX to (1 - scaleFactor) times the display width. Notice that the terms are interchanged; it's the same
        //as doing -1 * (scaleFactor - 1) * displayWidth
        else if((translateX * -1) > (scaleFactor - 1) * displayWidth) {
           translateX = (1 - scaleFactor) * displayWidth;
        }

        if(translateY * -1 < 0) {
           translateY = 0;
        }

        //We do the exact same thing for the bottom bound, except in this case we use the height of the display
        else if((translateY * -1) > (scaleFactor - 1) * displayHeight) {
           translateY = (1 - scaleFactor) * displayHeight;
        }

        //We need to divide by the scale factor here, otherwise we end up with excessive panning based on our zoom level
        //because the translation amount also gets scaled according to how much we've zoomed into the canvas.
        canvas.translate(translateX / scaleFactor, translateY / scaleFactor);
        
        
    	
		if (this.bitmap != null) {
			canvas.drawBitmap(this.bitmap, 0, 0, null);
		}

        /* The rest of your canvas-drawing code */
        canvas.restore();
		
		
		
		
		
		
	

	}

	// These two constants specify the minimum and maximum zoom
	private static float MIN_ZOOM = 1f;
	private static float MAX_ZOOM = 5f;

	private float scaleFactor = 1.f;
	private ScaleGestureDetector detector;

	// These constants specify the mode that we're in
	private static int NONE = 0;
	private static int DRAG = 1;
	private static int ZOOM = 2;

	private int mode;

	// These two variables keep track of the X and Y coordinate of the finger
	// when it first
	// touches the screen
	private float startX = 0f;
	private float startY = 0f;

	// These two variables keep track of the amount we need to translate the
	// canvas along the X
	// and the Y coordinate
	private float translateX = 0f;
	private float translateY = 0f;

	// These two variables keep track of the amount we translated the X and Y
	// coordinates, the last time we
	// panned.
	private float previousTranslateX = 0f;
	private float previousTranslateY = 0f;

	private float mPosX;
	private float mPosY;

	
	private boolean dragged = false;
	private float displayWidth;
	private float displayHeight;
	
	private float mLastTouchX;
	private float mLastTouchY;
	private int mActivePointerId = INVALID_POINTER_ID;

	private static final int INVALID_POINTER_ID = -1;

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		/*
		 * detector.onTouchEvent(ev);
		 * 
		 * Rerender();
		 * 
		 * final int action = ev.getAction(); switch (action &
		 * MotionEvent.ACTION_MASK) { case MotionEvent.ACTION_DOWN: { final
		 * float x = ev.getX(); final float y = ev.getY();
		 * 
		 * mLastTouchX = x; mLastTouchY = y; mActivePointerId =
		 * ev.getPointerId(0); break; }
		 * 
		 * case MotionEvent.ACTION_MOVE: { final int pointerIndex =
		 * ev.findPointerIndex(mActivePointerId); final float x =
		 * ev.getX(pointerIndex); final float y = ev.getY(pointerIndex);
		 * 
		 * // Only move if the ScaleGestureDetector isn't processing a gesture.
		 * if (!detector.isInProgress()) { final float dx = x - mLastTouchX;
		 * final float dy = y - mLastTouchY;
		 * 
		 * mPosX += dx; mPosY += dy;
		 * 
		 * invalidate(); }
		 * 
		 * mLastTouchX = x; mLastTouchY = y;
		 * 
		 * break; }
		 * 
		 * case MotionEvent.ACTION_UP: { mActivePointerId = INVALID_POINTER_ID;
		 * break; }
		 * 
		 * case MotionEvent.ACTION_CANCEL: { mActivePointerId =
		 * INVALID_POINTER_ID; break; }
		 * 
		 * case MotionEvent.ACTION_POINTER_UP: { final int pointerIndex =
		 * (ev.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >>
		 * MotionEvent.ACTION_POINTER_INDEX_SHIFT; final int pointerId =
		 * ev.getPointerId(pointerIndex); if (pointerId == mActivePointerId) {
		 * // This was our active pointer going up. Choose a new // active
		 * pointer and adjust accordingly. final int newPointerIndex =
		 * pointerIndex == 0 ? 1 : 0; mLastTouchX = ev.getX(newPointerIndex);
		 * mLastTouchY = ev.getY(newPointerIndex); mActivePointerId =
		 * ev.getPointerId(newPointerIndex); } break; } }
		 * 
		 * return true;
		 */
		
		Rerender();

		switch (event.getAction() & MotionEvent.ACTION_MASK) {

		case MotionEvent.ACTION_DOWN:
			mode = DRAG;

			// We assign the current X and Y coordinate of the finger to startX
			// and startY minus the previously translated
			// amount for each coordinates This works even when we are
			// translating the first time because the initial
			// values for these two variables is zero.
			startX = event.getX() - previousTranslateX;
			startY = event.getY() - previousTranslateY;
			break;

		case MotionEvent.ACTION_MOVE:
			translateX = event.getX() - startX;
			translateY = event.getY() - startY;

			// We cannot use startX and startY directly because we have adjusted
			// their values using the previous translation values.
			// This is why we need to add those values to startX and startY so
			// that we can get the actual coordinates of the finger.
			double distance = Math
					.sqrt(Math.pow(
							event.getX() - (startX + previousTranslateX), 2)
							+ Math.pow(event.getY()
									- (startY + previousTranslateY), 2));

			if (distance > 0) {
				dragged = true;
			}

			break;

		case MotionEvent.ACTION_POINTER_DOWN:
			mode = ZOOM;
			break;

		case MotionEvent.ACTION_UP:
			mode = NONE;
			dragged = false;

			// All fingers went up, so let's save the value of translateX and
			// translateY into previousTranslateX and
			// previousTranslate
			previousTranslateX = translateX;
			previousTranslateY = translateY;
			break;

		case MotionEvent.ACTION_POINTER_UP:
			mode = DRAG;

			// This is not strictly necessary; we save the value of translateX
			// and translateY into previousTranslateX
			// and previousTranslateY when the second finger goes up
			previousTranslateX = translateX;
			previousTranslateY = translateY;
			break;
		}

		detector.onTouchEvent(event);

		// We redraw the canvas only in the following cases:
		//
		// o The mode is ZOOM
		// OR
		// o The mode is DRAG and the scale factor is not equal to 1 (meaning we
		// have zoomed) and dragged is
		// set to true (meaning the finger has actually moved)
		if ((mode == DRAG && scaleFactor != 1f && dragged) || mode == ZOOM) {
			invalidate();
		}

		return true;
	}


	private class ScaleListener extends
			ScaleGestureDetector.SimpleOnScaleGestureListener {
		@Override
		public boolean onScale(ScaleGestureDetector detector) {
			scaleFactor *= detector.getScaleFactor();
			scaleFactor = Math.max(MIN_ZOOM, Math.min(scaleFactor, MAX_ZOOM));
			invalidate();
			return true;
		}
	}
}
