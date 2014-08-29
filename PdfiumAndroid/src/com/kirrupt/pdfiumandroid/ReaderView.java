package com.kirrupt.pdfiumandroid;

import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.SparseArray;
import android.view.GestureDetector;
import android.view.GestureDetector.OnDoubleTapListener;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.ScaleAnimation;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.Scroller;

public class ReaderView
extends AdapterView<Adapter>
implements GestureDetector.OnGestureListener, ScaleGestureDetector.OnScaleGestureListener, Runnable, OnDoubleTapListener {
	private static final int  MOVING_DIAGONALLY = 0;
	private static final int  MOVING_LEFT       = 1;
	private static final int  MOVING_RIGHT      = 2;
	private static final int  MOVING_UP         = 3;
	private static final int  MOVING_DOWN       = 4;

	private static final int  FLING_MARGIN      = 100;
	private static final int  GAP               = 20;

	private static final float MIN_SCALE        = 1.0f;
	private static final float MAX_SCALE        = 50.0f;

	private Adapter           mAdapter;
	private int               mCurrent;    // Adapter's index for the current view
	private boolean           mResetLayout;
	private final SparseArray<View>
	mChildViews = new SparseArray<View>(3);
	// Shadows the children of the adapter view
	// but with more sensible indexing
	private final LinkedList<View>
	mViewCache = new LinkedList<View>();
	private boolean           mUserInteracting;  // Whether the user is interacting
	private boolean           mScaling;    // Whether the user is currently pinch zooming
	private float             mScale     = 1.0f;
	private int               mXScroll;    // Scroll amounts recorded from events.
	private int               mYScroll;    // and then accounted for in onLayout
	private final GestureDetector
	mGestureDetector;
	private final ScaleGestureDetector
	mScaleGestureDetector;
	private final Scroller    mScroller;
	private int               mScrollerLastX;
	private int               mScrollerLastY;
	private boolean           mScrollDisabled;

	private float mMeasuredWidth = 0.0f;
	private float mMeasuredHeight = 0.0f;

	private List<PDFPagerItem> mItems;
	private PointF mSize;

	private Fragment mParent;

	//Smart Zoom variables
	private boolean smartZoom = false;
	private boolean smartZoomReposition = false;
	
	private enum SmartZoomDirection {
		NONE,
		VERTICAL,
		HORIZONTAL
	}
	private SmartZoomDirection smartZoomDirection = SmartZoomDirection.NONE;

	static abstract class ViewMapper {
		abstract void applyToView(View view);
	}
	Context context;

	OnPdfChangeListener onPdfChangeListener;

	public ReaderView(Context context, OnPdfChangeListener onPdfChangeListener, List<PDFPagerItem> items, PointF size, PDFReaderView parent) {
		super(context);

		mGestureDetector = new GestureDetector(this);
		mScaleGestureDetector = new ScaleGestureDetector(context, this);
		mScroller        = new Scroller(context);
		this.context = context;
		this.onPdfChangeListener = onPdfChangeListener;

		mItems = items;
		mSize = size;
		mParent = parent;
	}

	public int getDisplayedViewIndex() {
		return mCurrent;
	}

	public void setDisplayedViewIndex(int i) {
		if (0 <= i && i < mAdapter.getCount()) {
			onMoveOffChild(mCurrent);
			mCurrent = i;
			onMoveToChild(i);
			mResetLayout = true;
			requestLayout();
		}
	}
	
	// When advancing down the page, we want to advance by about
	// 90% of a screenful. But we'd be happy to advance by between
	// 80% and 95% if it means we hit the bottom in a whole number
	// of steps.
	private int smartAdvanceAmount(int screenHeight, int max) {
		int advance = (int)(screenHeight * 0.9 + 0.5);
		int leftOver = max % advance;
		int steps = max / advance;
		if (leftOver == 0) {
			// We'll make it exactly. No adjustment
		} else if ((float)leftOver / steps <= screenHeight * 0.05) {
			// We can adjust up by less than 5% to make it exact.
			advance += (int)((float)leftOver/steps + 0.5);
		} else {
			int overshoot = advance - leftOver;
			if ((float)overshoot / steps <= screenHeight * 0.1) {
				// We can adjust down by less than 10% to make it exact.
				advance -= (int)((float)overshoot/steps + 0.5);
			}
		}
		if (advance > max)
			advance = max;
		return advance;
	}

	public void resetupChildren() {
		for (int i = 0; i < mChildViews.size(); i++)
			onChildSetup(mChildViews.keyAt(i), mChildViews.valueAt(i));
	}

	public void applyToChildren(ViewMapper mapper) {
		for (int i = 0; i < mChildViews.size(); i++)
			mapper.applyToView(mChildViews.valueAt(i));
	}

	public void refresh(boolean reflow) {
		mScale = 1.0f;
		mXScroll = mYScroll = 0;

		int numChildren = mChildViews.size();
		for (int i = 0; i < numChildren; i++) {
			View v = mChildViews.valueAt(i);
			onNotInUse(v);
			removeViewInLayout(v);
		}
		mChildViews.clear();
		mViewCache.clear();

		requestLayout();
	}

	protected void onChildSetup(int i, View v) {}

	protected void onMoveToChild(int i) {}

	protected void onMoveOffChild(int i) {}

	protected void onSettle(View v) {};

	protected void onUnsettle(View v) {};

	protected void onNotInUse(View v) {};

	protected void onScaleChild(View v, Float scale) {};

	public View getView(int i) {
		return mChildViews.get(i);
	}

	public View getDisplayedView() {
		return mChildViews.get(mCurrent);
	}

	@Override
	public void run() {
		if (!mScroller.isFinished()) {
			mScroller.computeScrollOffset();
			int x = mScroller.getCurrX();
			int y = mScroller.getCurrY();
			mXScroll += x - mScrollerLastX;
			mYScroll += y - mScrollerLastY;
			mScrollerLastX = x;
			mScrollerLastY = y;
			requestLayout();
			post(this);
		}
		else if (!mUserInteracting) {
			// End of an inertial scroll and the user is not interacting.
			// The layout is stable
			View v = mChildViews.get(mCurrent);
			if (v != null)
				postSettle(v);
		}
	}

	@Override
	public boolean onDown(MotionEvent arg0) {
		mScroller.forceFinished(true);
		return true;
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY) {
		if (mScrollDisabled)
			return true;

		View v = mChildViews.get(mCurrent);
		if (v != null) {
			Rect bounds = getScrollBounds(v);
			switch(directionOfTravel(velocityX, velocityY)) {
			case MOVING_LEFT:
				if (bounds.left >= 0) {
					// Fling off to the left bring next view onto screen
					View vl = mChildViews.get(mCurrent+1);

					if (vl != null) {
						slideViewOntoScreen(vl);
						return true;
					}
				}
				break;
			case MOVING_RIGHT:
				if (bounds.right <= 0) {
					// Fling off to the right bring previous view onto screen
					View vr = mChildViews.get(mCurrent-1);

					if (vr != null) {
						slideViewOntoScreen(vr);
						return true;
					}
				}
				break;
			}
			mScrollerLastX = mScrollerLastY = 0;
			// If the page has been dragged out of bounds then we want to spring back
			// nicely. fling jumps back into bounds instantly, so we don't want to use
			// fling in that case. On the other hand, we don't want to forgo a fling
			// just because of a slightly off-angle drag taking us out of bounds other
			// than in the direction of the drag, so we test for out of bounds only
			// in the direction of travel.
			//
			// Also don't fling if out of bounds in any direction by more than fling
			// margin
			Rect expandedBounds = new Rect(bounds);
			expandedBounds.inset(-FLING_MARGIN, -FLING_MARGIN);

			if(withinBoundsInDirectionOfTravel(bounds, velocityX, velocityY)
					&& expandedBounds.contains(0, 0)) {
				
				setSmartZoomDirection(velocityX, velocityY);
				
				if (smartZoomDirection == SmartZoomDirection.HORIZONTAL) {
					velocityY = 0;
				}else if (smartZoomDirection == SmartZoomDirection.VERTICAL) {
					velocityX = 0;
				}
				
				mScroller.fling(0, 0, (int)velocityX, (int)velocityY, bounds.left, bounds.right, bounds.top, bounds.bottom);
				post(this);
			}
		}

		return true;
	}

	@Override
	public void onLongPress(MotionEvent e) {
	}
	
	public void setSmartZoomDirection(float distanceX, float distanceY) {
		int diff = 10;
		
		if (smartZoomDirection == SmartZoomDirection.NONE) {
			if(distanceY >= diff || distanceY <= -diff){
				smartZoomDirection = SmartZoomDirection.VERTICAL;
				smartZoomReposition = true;
			}else if (distanceX >= diff || distanceX <= -diff) {
				smartZoomDirection = SmartZoomDirection.HORIZONTAL;
				smartZoomReposition = true;
			}
		}
	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
			float distanceY) {
		if (!mScrollDisabled) {
			if (smartZoom) {
				setSmartZoomDirection(distanceX, distanceY);
			}
			
			if (smartZoomDirection != SmartZoomDirection.NONE) {
				if (smartZoomDirection == SmartZoomDirection.HORIZONTAL) {
					mXScroll -= distanceX;
				}else if(smartZoomDirection == SmartZoomDirection.VERTICAL) {
					mYScroll -= distanceY;
				}
			}else{
				mXScroll -= distanceX;
				mYScroll -= distanceY;
			}
			
			requestLayout();
		}
		return true;
	}

	@Override
	public void onShowPress(MotionEvent e) {
	}

	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		return false;
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);

		applyToChildren(new ViewMapper() {
			@Override
			void applyToView(View view) {
				if (view instanceof PageView) {
					PageView v = (PageView)view;

					v.resetParentSize();
					v.reloadPage();
				}
			}
		});
	}

	@Override
	public boolean onScale(ScaleGestureDetector detector) {
		float previousScale = mScale;

		float min_scale = MIN_SCALE;
		float max_scale = MAX_SCALE;
		mScale = Math.min(Math.max(mScale * detector.getScaleFactor(), min_scale), max_scale);


		float factor = mScale/previousScale;

		View v = mChildViews.get(mCurrent);
		if (v != null) {
			// Work out the focus point relative to the view top left
			int viewFocusX = (int)detector.getFocusX() - (v.getLeft() + mXScroll);
			int viewFocusY = (int)detector.getFocusY() - (v.getTop() + mYScroll);
			// Scroll to maintain the focus point
			mXScroll += viewFocusX - viewFocusX * factor;
			mYScroll += viewFocusY - viewFocusY * factor;
			requestLayout();
		}

		return true;
	}

	@Override
	public boolean onScaleBegin(ScaleGestureDetector detector) {
		smartZoom = false;
		mScaling = true;
		// Ignore any scroll amounts yet to be accounted for: the
		// screen is not showing the effect of them, so they can
		// only confuse the user
		mXScroll = mYScroll = 0;
		// Avoid jump at end of scaling by disabling scrolling
		// until the next start of gesture
		mScrollDisabled = true;
		return true;
	}

	@Override
	public void onScaleEnd(ScaleGestureDetector detector) {
		mScaling = false;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		mScaleGestureDetector.onTouchEvent(event);

		if (!mScaling)
			mGestureDetector.onTouchEvent(event);

		if ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_DOWN) {
			mUserInteracting = true;
			if (BuildConfig.DEBUG) Log.i("ReaderView", "ACTION DOWN!");
		}
		if ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP) {
			if (BuildConfig.DEBUG) Log.i("ReaderView", "ACTION UP");
			mScrollDisabled = false;
			mUserInteracting = false;
			
			smartZoomDirection = SmartZoomDirection.NONE;

			View v = mChildViews.get(mCurrent);
			if (v != null) {
				if (mScroller.isFinished()) {
					// If, at the end of user interaction, there is no
					// current inertial scroll in operation then animate
					// the view onto screen if necessary
					slideViewOntoScreen(v);
				}

				if (mScroller.isFinished()) {
					// If still there is no inertial scroll in operation
					// then the layout is stable
					postSettle(v);
				}
			}
		}

		requestLayout();
		return true;
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);

		int n = getChildCount();
		for (int i = 0; i < n; i++)
			measureView(getChildAt(i));
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right,
			int bottom) {
		super.onLayout(changed, left, top, right, bottom);

		View cv = mChildViews.get(mCurrent);
		Point cvOffset;

		if (!mResetLayout) {
			// Move to next or previous if current is sufficiently off center
			if (cv != null) {
				cvOffset = subScreenSizeOffset(cv);
				// cv.getRight() may be out of date with the current scale
				// so add left to the measured width for the correct position
				if (cv.getLeft() + cv.getMeasuredWidth() + cvOffset.x + GAP/2 + mXScroll < getWidth()/2 && mCurrent + 1 < mAdapter.getCount()) {
					postUnsettle(cv);
					// post to invoke test for end of animation
					// where we must set hq area for the new current view
					post(this);

					onMoveOffChild(mCurrent);
					mCurrent++;
					onMoveToChild(mCurrent);
				}

				if (cv.getLeft() - cvOffset.x - GAP/2 + mXScroll >= getWidth()/2 && mCurrent > 0) {
					postUnsettle(cv);
					// post to invoke test for end of animation
					// where we must set hq area for the new current view
					post(this);

					onMoveOffChild(mCurrent);
					mCurrent--;
					onMoveToChild(mCurrent);
				}
			}

			// Remove not needed children and hold them for reuse
			int numChildren = mChildViews.size();
			int childIndices[] = new int[numChildren];
			for (int i = 0; i < numChildren; i++)
				childIndices[i] = mChildViews.keyAt(i);

			for (int i = 0; i < numChildren; i++) {
				int ai = childIndices[i];
				if (ai < mCurrent - 1 || ai > mCurrent + 1) {
					View v = mChildViews.get(ai);
					onNotInUse(v);
					mViewCache.add(v);
					removeViewInLayout(v);
					mChildViews.remove(ai);
				}
			}
		} else {
			mResetLayout = false;
			mXScroll = mYScroll = 0;

			// Remove all children and hold them for reuse
			int numChildren = mChildViews.size();
			for (int i = 0; i < numChildren; i++) {
				View v = mChildViews.valueAt(i);
				onNotInUse(v);
				mViewCache.add(v);
				removeViewInLayout(v);
			}
			mChildViews.clear();
			// post to ensure generation of hq area
			post(this);
		}

		// Ensure current view is present
		int cvLeft, cvRight, cvTop, cvBottom;
		boolean notPresent = (mChildViews.get(mCurrent) == null);
		cv = getOrCreateChild(mCurrent);
		// When the view is sub-screen-size in either dimension we
		// offset it to center within the screen area, and to keep
		// the views spaced out
		cvOffset = subScreenSizeOffset(cv);
		if (notPresent) {
			//Main item not already present. Just place it top left
			cvLeft = cvOffset.x;
			cvTop  = cvOffset.y;
		} else {
			// Main item already present. Adjust by scroll offsets
			cvLeft = cv.getLeft() + mXScroll;
			cvTop  = cv.getTop()  + mYScroll;
		}
		// Scroll values have been accounted for
		mXScroll = mYScroll = 0;
		cvRight  = cvLeft + cv.getMeasuredWidth();
		cvBottom = cvTop  + cv.getMeasuredHeight();

		if (!mUserInteracting && mScroller.isFinished()) {
			Point corr = getCorrection(getScrollBounds(cvLeft, cvTop, cvRight, cvBottom));
			cvRight  += corr.x;
			cvLeft   += corr.x;
			cvTop    += corr.y;
			cvBottom += corr.y;
		} else if (cv.getMeasuredHeight() <= getHeight()) {
			// When the current view is as small as the screen in height, clamp
			// it vertically
			Point corr = getCorrection(getScrollBounds(cvLeft, cvTop, cvRight, cvBottom));
			cvTop    += corr.y;
			cvBottom += corr.y;
		}

		cv.layout(cvLeft, cvTop, cvRight, cvBottom);

		if (mCurrent > 0) {
			View lv = getOrCreateChild(mCurrent - 1);
			Point leftOffset = subScreenSizeOffset(lv);
			int gap = leftOffset.x + GAP + cvOffset.x;
			lv.layout(cvLeft - lv.getMeasuredWidth() - gap,
					(cvBottom + cvTop - lv.getMeasuredHeight())/2,
					cvLeft - gap,
					(cvBottom + cvTop + lv.getMeasuredHeight())/2);
		}

		if (mCurrent + 1 < mAdapter.getCount()) {
			View rv = getOrCreateChild(mCurrent + 1);
			Point rightOffset = subScreenSizeOffset(rv);
			int gap = cvOffset.x + GAP + rightOffset.x;
			rv.layout(cvRight + gap,
					(cvBottom + cvTop - rv.getMeasuredHeight())/2,
					cvRight + rv.getMeasuredWidth() + gap,
					(cvBottom + cvTop + rv.getMeasuredHeight())/2);
		}

		invalidate();
	}

	@Override
	public Adapter getAdapter() {
		return mAdapter;
	}

	@Override
	public View getSelectedView() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setAdapter(Adapter adapter) {
		mAdapter = adapter;
		mChildViews.clear();
		removeAllViewsInLayout();
		requestLayout();
	}

	@Override
	public void setSelection(int arg0) {
		throw new UnsupportedOperationException();
	}

	private View getCached() {
		if (mViewCache.size() == 0)
			return null;
		else
			return mViewCache.removeFirst();
	}

	private View getOrCreateChild(int i) {
		View v = mChildViews.get(i);
		if (v == null) {
			v = mAdapter.getView(i, getCached(), this);
			addAndMeasureChild(i, v);
			onChildSetup(i, v);
			onScaleChild(v, mScale);
		}

		return v;
	}

	private void addAndMeasureChild(int i, View v) {
		LayoutParams params = v.getLayoutParams();
		if (params == null) {
			params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		}
		addViewInLayout(v, 0, params, true);
		mChildViews.append(i, v); // Record the view against it's adapter index
		measureView(v);
	}

	private void measureView(View v) {
		// See what size the view wants to be
		v.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);

		mMeasuredWidth = v.getMeasuredWidth();
		mMeasuredHeight = v.getMeasuredHeight();

		// Work out a scale that will fit it to this view
		float scale = Math.min((float)getWidth()/(float)v.getMeasuredWidth(),
				(float)getHeight()/(float)v.getMeasuredHeight());
		//Log.i("ReaderView", "measureView scale: "+scale+", mScale: "+mScale + ", gMW: "+v.getMeasuredWidth());
		// Use the fitting values scaled by our current scale factor
		v.measure(View.MeasureSpec.EXACTLY | (int)(v.getMeasuredWidth()*scale*mScale),
				View.MeasureSpec.EXACTLY | (int)(v.getMeasuredHeight()*scale*mScale));
	}

	private Rect getScrollBounds(int left, int top, int right, int bottom) {
		int xmin = getWidth() - right;
		int xmax = -left;
		int ymin = getHeight() - bottom;
		int ymax = -top;

		// In either dimension, if view smaller than screen then
		// constrain it to be central
		if (xmin > xmax) xmin = xmax = (xmin + xmax)/2;
		if (ymin > ymax) ymin = ymax = (ymin + ymax)/2;

		return new Rect(xmin, ymin, xmax, ymax);
	}

	private Rect getScrollBounds(View v) {
		// There can be scroll amounts not yet accounted for in
		// onLayout, so add mXScroll and mYScroll to the current
		// positions when calculating the bounds.
		return getScrollBounds(v.getLeft() + mXScroll,
				v.getTop() + mYScroll,
				v.getLeft() + v.getMeasuredWidth() + mXScroll,
				v.getTop() + v.getMeasuredHeight() + mYScroll);
	}

	private Point getCorrection(Rect bounds) {
		return new Point(Math.min(Math.max(0,bounds.left),bounds.right),
				Math.min(Math.max(0,bounds.top),bounds.bottom));
	}

	private void postSettle(final View v) {
		// onSettle and onUnsettle are posted so that the calls
		// wont be executed until after the system has performed
		// layout.
		post (new Runnable() {
			@Override
			public void run () {
				onSettle(v);
			}
		});
	}

	private void postUnsettle(final View v) {
		post (new Runnable() {
			@Override
			public void run () {
				onUnsettle(v);
			}
		});
	}

	private void slideViewOntoScreen(View v) {
		Point corr = getCorrection(getScrollBounds(v));
		if (corr.x != 0 || corr.y != 0) {
			mScrollerLastX = mScrollerLastY = 0;
			mScroller.startScroll(0, 0, corr.x, corr.y, 400);
			post(this);
		}
	}

	private Point subScreenSizeOffset(View v) {
		return new Point(Math.max((getWidth() - v.getMeasuredWidth())/2, 0),
				Math.max((getHeight() - v.getMeasuredHeight())/2, 0));
	}

	private static int directionOfTravel(float vx, float vy) {
		if (Math.abs(vx) > 2 * Math.abs(vy))
			return (vx > 0) ? MOVING_RIGHT : MOVING_LEFT;
		else if (Math.abs(vy) > 2 * Math.abs(vx))
			return (vy > 0) ? MOVING_DOWN : MOVING_UP;
		else
			return MOVING_DIAGONALLY;
	}

	private static boolean withinBoundsInDirectionOfTravel(Rect bounds, float vx, float vy) {
		switch (directionOfTravel(vx, vy)) {
		case MOVING_DIAGONALLY: return bounds.contains(0, 0);
		case MOVING_LEFT:       return bounds.left <= 0;
		case MOVING_RIGHT:      return bounds.right >= 0;
		case MOVING_UP:         return bounds.top <= 0;
		case MOVING_DOWN:       return bounds.bottom >= 0;
		default: throw new NoSuchElementException();
		}
	}

	/*
	 * (non-Javadoc)
	 * @see android.view.GestureDetector.OnDoubleTapListener#onDoubleTap(android.view.MotionEvent)
	 * 
	 * Dodano double tap zommiranje.
	 */
	@Override
	public boolean onDoubleTap(MotionEvent e) {
		//Log.i("ReaderView", "onDoubleTap "+mCurrent);

		int viewFocusX = 0;
		int viewFocusY = 0;

		View v = mChildViews.get(mCurrent);
		if (v != null) {
			// Work out the focus point relative to the view top left
			viewFocusX = (int)e.getX() - (v.getLeft() + mXScroll);
			viewFocusY = (int)e.getY() - (v.getTop() + mXScroll);
		}

		Rect selectedRect = null;

		float ratio = v.getWidth() / mSize.x;

		float click_x = viewFocusX / ratio;
		float click_y = viewFocusY / ratio;

		if (mItems != null && mItems.size() > mCurrent) {
			PDFPagerItem item = mItems.get(mCurrent);
			//Log.i("ReaderView", "vF x: "+viewFocusX+", y: "+viewFocusY+", fn: "+item.getFileName());

			int[] pageObjects = PDFReaderView.getMySerialExecutorSingleton().getNonBlockingPageObjectsForFileName(Environment.getExternalStorageDirectory()+"/"+item.getFileName());

			if (pageObjects != null) {
				//x: rect.Left
				//y: height - rect.Top
				//width: rect.Right - rect.Left
				//height: rect.Top - rect.Bottom

				if (BuildConfig.DEBUG) Log.i("ReaderView", "click x: "+click_x+", y: "+click_y);

				int count = pageObjects.length / 4;

				if (BuildConfig.DEBUG) Log.i("ReaderView", "pageObjects "+pageObjects.length);

				if (pageObjects.length % 4 == 0) {
					for(int i = 0; i < count*4; i+=4) {
						if (pageObjects[i] <= click_x &&//top
								pageObjects[i+1] <= click_y && //left
								pageObjects[i+2] > click_x && //right
								pageObjects[i+3] > click_y) { //left

							selectedRect = new Rect();
							selectedRect.left = pageObjects[i];
							selectedRect.top = pageObjects[i+1];
							selectedRect.right = pageObjects[i+2];
							selectedRect.bottom = pageObjects[i+3];
							break;
						}
					}

				}else{
					if (BuildConfig.DEBUG) Log.e("ReaderView", "pageObjects.length % 4 > 0!");
				}
			}

		}else{
			if (BuildConfig.DEBUG) Log.d("ReaderView", "mCurrent > size() or mItems==null, mCurrent "+mCurrent);
		}

		if (selectedRect != null) {
			if (BuildConfig.DEBUG) Log.i("ReaderView", "found rect!");

			int width = selectedRect.right - selectedRect.left;
			if (BuildConfig.DEBUG) Log.i("ReaderView", "width " +width);

			int padding = 20;

			int point_x = selectedRect.left - padding/2;
			int point_y = (int) click_y;//selectedRect.top;// (int)((float)selectedRect.top + (float)selectedRect.height()/(float)2 * (float)ratio);//fiX THIS!

			float width_padding = width + padding;

			float new_scale = mSize.x / width_padding * this.getWidth() / mMeasuredWidth;

			if (smartZoom && !smartZoomReposition && mScale == new_scale && e.getY() > mMeasuredHeight/3 && e.getY() < mMeasuredHeight/3*2) {
				unzoom(v);
			}else{
				mScale = new_scale;

				smartZoom = true;
				smartZoomReposition = false;

				if (v != null) {
					float ratiox = mMeasuredWidth / mSize.x;

					int moveX = (int)(point_x *ratiox *mScale);
					int moveY = (int)(point_y *ratiox *mScale) - (int)mMeasuredHeight/2;
					
					if (moveX < 0) {
						moveX = 0;
					}
					
					if (moveY < 0) {
						moveY = 0;
					}
					
					float limitY = mMeasuredHeight * mScale - mMeasuredHeight;
					
					if (moveY > limitY) {
						moveY = (int) (mMeasuredHeight * mScale - mMeasuredHeight) - 30;
					}
					
					mXScroll =-v.getLeft() - moveX;
					mYScroll =-v.getTop() - moveY;

					requestLayout();
				}
			}
		}else{
			unzoom(v);
		}

		return true;
	}
	
	private void unzoom(View v) {
		smartZoom = false;

		mScale = 1.0f;
		
		if (v != null) {
			mXScroll = -v.getLeft();//+= viewFocusX - viewFocusX * factor;
			mYScroll = -v.getTop();// viewFocusY - viewFocusY * factor;
			requestLayout();
		}
	}

	@Override
	public boolean onDoubleTapEvent(MotionEvent e) {
		return false;
	}

	@Override
	public boolean onSingleTapConfirmed(MotionEvent e) {
		if (onPdfChangeListener != null) {
			onPdfChangeListener.isSingleTap(true);
		}

		return false;
	}


}
