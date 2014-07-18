package com.kirrupt.pdfiumandroid;

import com.kirrupt.pdfiumandroid.MyView1.RenderFullPage;
import com.kirrupt.pdfiumandroid.MyView1.RenderPage;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Bitmap.Config;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;
import android.view.Display;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnTouchListener;

public class MyView2 extends ImageView implements OnTouchListener {


	private Matrix matrix = new Matrix();
	private Matrix savedMatrix = new Matrix();

	static final int NONE = 0;
	static final int DRAG = 1;
	static final int ZOOM = 2;

	private int mode = NONE;

	private PointF mStartPoint = new PointF();
	private PointF mMiddlePoint = new PointF();
	private Point mBitmapMiddlePoint = new Point();

	private float oldDist = 1f;
	private float matrixValues[] = {0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f};
	private float scale;
	private float oldEventX = 0;
	private float oldEventY = 0;
	private float oldStartPointX = 0;
	private float oldStartPointY = 0;
	private int mViewWidth = -1;
	private int mViewHeight = -1;
	private int mBitmapWidth = -1;
	private int mBitmapHeight = -1;
	private boolean mDraggable = false;

	float displayWidth;
	float displayHeight;

	float deviceDisplayWidth;
	float deviceDisplayHeight; 
	
	private PDFReader reader;
	private PDFDocument document;
	
	private float renderedForScaleFactor = 0.0f;
	private boolean renderingInProgress = false;
	
	private boolean shouldRerender = false;
	
	class RenderPage extends AsyncTask<Integer, String, Bitmap> {


		@Override
		protected void onPreExecute() {
			// TODO Auto-generated method stub
			super.onPreExecute();

		}

		@Override
		protected Bitmap doInBackground(Integer... params) {
			long startTime = System.currentTimeMillis();

			//create bitmap with the required size
			Bitmap bitmap = Bitmap.createBitmap((int)displayWidth, (int)displayHeight,
					Config.ARGB_8888);

			//render bitmap
			document.renderRectangle((int)displayWidth, (int)displayHeight, scale*1.0f, (int)(scale*200), (int)(scale*200), bitmap);

			long renderTime = System.currentTimeMillis() - startTime;
			Log.d("TIME", "render=" + String.valueOf(renderTime));
			return bitmap;
		}

		@Override
		protected void onPostExecute(Bitmap result) {
			// TODO Auto-generated method stub
			super.onPostExecute(result);

			
			System.gc();

			renderedForScaleFactor = scale;

			renderingInProgress = false;

			if (result != null) {
				shouldRerender = false;
				setBitmap(result);
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
			
			Log.i("Render","RenderFullPage");
			float scaleFactor = 1;
			
			int fullPageWidth = (int) (document.width() * (int)scaleFactor);
			float ratio = (float)document.width() / (float)document.width() * (int)scaleFactor;
			
			int fullPageHeight = (int)(ratio * (float)document.height());
			
			Log.i(MyView1.class.getSimpleName()," render: "+fullPageWidth+" "+fullPageHeight+", ratio: "+ratio);
			
			Bitmap bitmap = Bitmap.createBitmap(fullPageWidth, fullPageHeight, Config.ARGB_8888);
			
			document.renderRectangle(fullPageWidth, fullPageHeight, ratio, 0,0, bitmap);
			
			long renderTime = System.currentTimeMillis() - startTime;
			
			Log.i(MyView1.class.getSimpleName(), "(fullpage) render=" + String.valueOf(renderTime));
			
			return bitmap;
		}

		@Override
		protected void onPostExecute(Bitmap result) {
			// TODO Auto-generated method stub
			super.onPostExecute(result);


			if (result != null) {	
				setBitmap(result);
				invalidate();
			}
		}
	}
	
	private void Rerender() {
		Log.d("R", "rerenderInProgress:"+renderingInProgress+" mScaleFactor:"+scale+" renderedForScaleFactor:"+renderedForScaleFactor);
		
		if (this.renderedForScaleFactor != this.scale && !this.renderingInProgress) {
			RenderPage r = new RenderPage();
			this.renderingInProgress = true;
			Log.d("R", "in progress");

			int width = (int) ((this.getWidth() > 0) ? this.getWidth() : displayWidth);		
			
			r.execute(width, this.getHeight());
		}

	}
	
	public MyView2(Context context) {
		this(context, null, 0);
		
		this.setOnTouchListener(this);

	}

	public MyView2(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
		
		this.setOnTouchListener(this);

	}

	public MyView2(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		
		this.setOnTouchListener(this);
		
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
	public void onSizeChanged (int w, int h, int oldw, int oldh){
		super.onSizeChanged(w, h, oldw, oldh);
		mViewWidth = w;
		mViewHeight = h;
	}

	public void setBitmap(Bitmap bitmap){
		if(bitmap != null){
			setImageBitmap(bitmap);

			mBitmapWidth = bitmap.getWidth();
			mBitmapHeight = bitmap.getHeight();
			mBitmapMiddlePoint.x = (mViewWidth / 2) - (mBitmapWidth /  2);
			mBitmapMiddlePoint.y = (mViewHeight / 2) - (mBitmapHeight / 2);

			matrix.postTranslate(mBitmapMiddlePoint.x, mBitmapMiddlePoint.y);
			this.setImageMatrix(matrix);
		}
	}

	@Override
	public boolean onTouch(View v, MotionEvent event){
		
		Log.i("touch", "touch");
		switch (event.getAction() & MotionEvent.ACTION_MASK) {
		case MotionEvent.ACTION_DOWN:
			savedMatrix.set(matrix);
			mStartPoint.set(event.getX(), event.getY());
			mode = DRAG;
			break;
		case MotionEvent.ACTION_POINTER_DOWN:
			oldDist = spacing(event);
			if(oldDist > 10f){
				savedMatrix.set(matrix);
				midPoint(mMiddlePoint, event);
				mode = ZOOM;
			}
			break;
		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_POINTER_UP:
			mode = NONE;
			break;
		case MotionEvent.ACTION_MOVE:
			if(mode == DRAG){
				drag(event);
			} else if(mode == ZOOM){
				zoom(event);
			} 
			break;
		}

		return true;
	}



	public void drag(MotionEvent event){
		matrix.getValues(matrixValues);

		Log.i("drag","drag");
		float left = matrixValues[2];
		float top = matrixValues[5];
		float bottom = (top + (matrixValues[0] * mBitmapHeight)) - mViewHeight;
		float right = (left + (matrixValues[0] * mBitmapWidth)) -mViewWidth;

		float eventX = event.getX();
		float eventY = event.getY();
		float spacingX = eventX - mStartPoint.x;
		float spacingY = eventY - mStartPoint.y;
		float newPositionLeft = (left  < 0 ? spacingX : spacingX * -1) + left;
		float newPositionRight = (spacingX) + right;
		float newPositionTop = (top  < 0 ? spacingY : spacingY * -1) + top;
		float newPositionBottom = (spacingY) + bottom;
		boolean x = true;
		boolean y = true;

		if(newPositionRight < 0.0f || newPositionLeft > 0.0f){
			if(newPositionRight < 0.0f && newPositionLeft > 0.0f){
				x = false;
			} else{
				eventX = oldEventX;
				mStartPoint.x = oldStartPointX;
			}
		}
		if(newPositionBottom < 0.0f || newPositionTop > 0.0f){
			if(newPositionBottom < 0.0f && newPositionTop > 0.0f){
				y = false;
			} else{
				eventY = oldEventY;
				mStartPoint.y = oldStartPointY;
			}
		}

		if(mDraggable){
			matrix.set(savedMatrix);
			matrix.postTranslate(x? eventX - mStartPoint.x : 0, y? eventY - mStartPoint.y : 0);
			this.setImageMatrix(matrix);
			if(x)oldEventX = eventX;
			if(y)oldEventY = eventY;
			if(x)oldStartPointX = mStartPoint.x;
			if(y)oldStartPointY = mStartPoint.y;
		}

	}

	public void zoom(MotionEvent event){
		matrix.getValues(matrixValues);

		float newDist = spacing(event);
		float bitmapWidth = matrixValues[0] * mBitmapWidth;
		float bimtapHeight = matrixValues[0] * mBitmapHeight;
		boolean in = newDist > oldDist;

		if(!in && matrixValues[0] < 1){
			return;
		}
		if(bitmapWidth > mViewWidth || bimtapHeight > mViewHeight){
			mDraggable = true;
		} else{
			mDraggable = false;
		}

		float midX = (mViewWidth / 2);
		float midY = (mViewHeight / 2);

		matrix.set(savedMatrix);
		scale = newDist / oldDist;
		matrix.postScale(scale, scale, bitmapWidth > mViewWidth ? mMiddlePoint.x : midX, bimtapHeight > mViewHeight ? mMiddlePoint.y : midY); 

		Log.i("zoom","zoom");
		this.setImageMatrix(matrix);


	}

	/** Determine the space between the first two fingers */
	private float spacing(MotionEvent event) {
		float x = event.getX(0) - event.getX(1);
		float y = event.getY(0) - event.getY(1);

		return (float)Math.sqrt(x * x + y * y);
	}

	/** Calculate the mid point of the first two fingers */
	private void midPoint(PointF point, MotionEvent event) {
		float x = event.getX(0) + event.getX(1);
		float y = event.getY(0) + event.getY(1);
		point.set(x / 2, y / 2);
	}
}

