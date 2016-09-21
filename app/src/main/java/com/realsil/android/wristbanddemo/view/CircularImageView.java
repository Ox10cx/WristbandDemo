package com.realsil.android.wristbanddemo.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;

import com.realsil.android.wristbanddemo.R;


/**
 * 使用方法：在需要圆形图片的地方在xml里把Imageview
 * 改为此CircularImageView
 *
 *
 */
public class CircularImageView extends ImageView {
	private final RectF roundRect = new RectF();
	private final Paint maskPaint = new Paint();
	private final Paint zonePaint = new Paint();
	private Paint borderPaint;

	public CircularImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.init();
	}

	public CircularImageView(Context context) {
		super(context);
		this.init();
	}

	private void init() {
		this.maskPaint.setAntiAlias(true);
		this.maskPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
		this.zonePaint.setAntiAlias(true);
		this.zonePaint.setColor(Color.BLACK);
	}

	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		super.onLayout(changed, left, top, right, bottom);
		int w = this.getWidth();
		int h = this.getHeight();
		if(w < h) {
			this.roundRect.set(0.0F, (float)((h - w) / 2), (float)w, (float)((h + w) / 2));
		} else {
			this.roundRect.set((float)((w - h) / 2), 0.0F, (float)((h + w) / 2), (float)h);
		}

	}

	public void draw(Canvas canvas) {
		canvas.saveLayer(this.roundRect, this.zonePaint, Canvas.ALL_SAVE_FLAG);
		canvas.drawOval(this.roundRect, this.zonePaint);
		canvas.saveLayer(this.roundRect, this.maskPaint, Canvas.ALL_SAVE_FLAG);
		super.draw(canvas);
		if(this.borderPaint != null) {
			canvas.drawOval(this.roundRect, this.borderPaint);
		}

		canvas.restore();
	}

	public void showBorder(int borderWidth, int color) {
		this.borderPaint = new Paint();
		this.borderPaint.setAntiAlias(true);
		this.borderPaint.setStrokeWidth((float)borderWidth);
		this.borderPaint.setColor(color);
		this.borderPaint.setStyle(Paint.Style.STROKE);
		this.borderPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));
	}
}