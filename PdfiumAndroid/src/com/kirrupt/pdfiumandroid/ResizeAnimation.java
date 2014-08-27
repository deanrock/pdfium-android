package com.kirrupt.pdfiumandroid;

import android.annotation.TargetApi;
import android.os.Build;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;

public class ResizeAnimation extends Animation
{
    private int mWidth;
    private int mStartWidth;
    private int mHeight;
    private int mStartHeight;
    private int mLeft;
    private int mTop;
    private View mView;
    private int mDuration;

    public ResizeAnimation(View view, int width, int height, int top, int left, int duration)
    {
        mView = view;
        mWidth = width;
        mHeight = height;
        mLeft = left;
        mTop = top;
        mStartHeight = view.getHeight();
        mStartWidth = view.getWidth();
        mDuration = duration;
    }

    @Override
    protected void applyTransformation(float interpolatedTime, Transformation t)
    {
        int newWidth = mStartWidth + (int) ((mWidth - mStartWidth) * interpolatedTime);
        int newHeight = mStartHeight + (int) ((mHeight - mStartHeight) * interpolatedTime);

        mView.getLayoutParams().width = newWidth;
        mView.getLayoutParams().height = newHeight;
        
        mView.requestLayout();
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
	@Override
    public void initialize(int width, int height, int parentWidth, int parentHeight)
    {
        super.initialize(width, height, parentWidth, parentHeight);
        this.setDuration(mDuration);
        mView.animate().setDuration(mDuration);
        mView.animate().x(mLeft).y(mTop);
    }

    @Override
    public boolean willChangeBounds()
    {
        return true;
    }
}
