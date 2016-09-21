package com.realsil.android.wristbanddemo.utility;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ComposeShader;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;


/**
 * reference: http://blog.csdn.net/jdsjlzx/article/details/42497135
 */
public class CircleProcessBar extends View {
	// the draw paint
	private Paint mRingPaint;
	// the draw circle paint
	private Paint mCirclePaint;
	// the drew color
	private int mRingColor;
	// Use the sweepGradient
	private SweepGradient mSweepGradient;
	// radius
	private float mRadius;
	// the ring radius, generate by radius and stroke width
	private float mRingRadius;
	// stroke width
	private float mStrokeWidth;
	// current x center
	private int mXCenter;
	// current y center
	private int mYCenter;
	// total progress
	private int mTotalProgress = 100;
	// current progress
	private int mProgress;

	public CircleProcessBar(Context context, AttributeSet attrs) {
		super(context, attrs);
		// initial the view
		initAttrs(50, 20, 0x003399ff);
		initVariable();
	}

	private void initAttrs(float radius, float width, int color) {
		mRadius = radius;
		mStrokeWidth = width;
		mRingColor = color;
		
		mRingRadius = mRadius + mStrokeWidth / 2;
		//Log.e("123", "initAttrs, mRadius: " + mRadius + ", mStrokeWidth: " + mStrokeWidth + ", mRingColor: " + mRingColor);
	}

	private void initVariable() {
		mRingPaint = new Paint();
		mRingPaint.setAntiAlias(true);
		mRingPaint.setDither(true);
		mRingPaint.setColor(mRingColor);//test
		mRingPaint.setStyle(Paint.Style.STROKE);
		mRingPaint.setStrokeCap(Paint.Cap.ROUND);
		mRingPaint.setStrokeWidth(mStrokeWidth);
	}

	@Override
	protected void onDraw(Canvas canvas) {

		mXCenter = getWidth() / 2;
		mYCenter = getHeight() / 2;
		
		if (mProgress > 0 ) {
			RectF oval = new RectF();
			oval.left = (mXCenter - mRingRadius);
			oval.top = (mYCenter - mRingRadius);
			oval.right = mRingRadius * 2 + (mXCenter - mRingRadius);
			oval.bottom = mRingRadius * 2 + (mYCenter - mRingRadius);

			/*
			//int[] colors = new int[]{Color.GREEN, Color.BLUE, Color.GREEN};
			int[] colors = new int[] {
					0xff1db6ff, 0xff0b58c2, 0xff002a6d, 0xff1db6ff
			};
			//实例化光束渲染
			RadialGradient radialGradient = new RadialGradient(mXCenter, mYCenter, mStrokeWidth,
					new int[] { Color.WHITE, Color.RED }, null, Shader.TileMode.REPEAT);
			//实例化梯度渲染
			SweepGradient sweepGradient = new SweepGradient(mXCenter ,mYCenter, colors, null);
			//实例化混合渲染
			ComposeShader composeShader = new ComposeShader(sweepGradient, radialGradient,
					PorterDuff.Mode.DARKEN);
			mRingPaint.setShader(composeShader);*/


			canvas.drawArc(oval, -90, ((float)mProgress / mTotalProgress) * 360, false, mRingPaint);
		}
	}

	public int getRingColor() {
		return mRingColor;
	}

	public void setRingColor(int mRingColor) {
		this.mRingColor = mRingColor;
		initVariable();
	}

	public float getRadius() {
		return mRadius;
	}

	public void setRadius(float mRadius) {
		this.mRadius = mRadius;
		initAttrs(mRadius, mStrokeWidth, mRingColor);
		initVariable();
	}

	public float getStrokeWidth() {
		return mStrokeWidth;
	}

	public void setStrokeWidth(float mStrokeWidth) {
		this.mStrokeWidth = mStrokeWidth;
		initAttrs(mRadius, mStrokeWidth, mRingColor);
		initVariable();
	}
	
	public void setProgress(int progress) {
		mProgress = progress;
//			invalidate();
		postInvalidate();
	}

}
