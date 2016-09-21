package com.realsil.android.wristbanddemo.utility;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.realsil.android.wristbanddemo.R;

/**
 * 高亮的View。
 *
 * @author Geek_Soledad (msdx.android@qq.com)
 * @version 2015-12-07 3.1
 * @since 2015-12-07 3.1
 * reference: http://blog.csdn.net/maosidiaoxian/article/details/50248145
 */
public class HighLightView extends View {
    private static final float RADIUS_RATIO = 1f / 3;
    private static final PorterDuffXfermode X_FER_MODE = new PorterDuffXfermode(PorterDuff.Mode.DST_OUT);
    public final static int HIGH_LIGHT_VIEW_TYPE_CIRCLE = 0;
    public final static int HIGH_LIGHT_VIEW_TYPE_RECT = 1;
    public final static int HIGH_LIGHT_VIEW_TYPE_RECT_SPEC = 2;
    private final Paint mPaint;

    private int mOverlayColor;//遮罩层颜色
    private int mRadius; // 圆半径
    private int mCenterX; // 圆心横坐标
    private int mCenterY; // 圆心纵坐标

    private String mTip; // 提示文字
    private float mTipX; // 文字横坐标
    private float mTipY; // 文字纵坐标

    private View mHighLightView; // 高亮的View
    private Bitmap mBitmap;

    private OnClickListener mClickListener; // 点击回调
    /**
     * 可点击的区域
     */
    private Rect mClickRect = new Rect();
    /**
     * 是否在点击区域按下
     */
    private boolean mDownInClickRect;

    public HighLightView(Context context) {
        super(context);
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setDither(true);
        mPaint.setStyle(Paint.Style.FILL);
        int textSize = getResources().getDimensionPixelSize(R.dimen.text_25dp);
        mPaint.setTextSize(textSize);
        mOverlayColor = getResources().getColor(R.color.guide_overlay);

        setFilterTouchesWhenObscured(false);
    }

    /**
     * @param view     要高亮的view
     * @param tip      文字提示
     * @param type      高亮图像形式
     * @param listener 点击回调
     */
    public void showTipForView(final View view, final String tip, final int type, OnClickListener listener) {
        mHighLightView = view;
        mTip = tip;
        mClickListener = listener;

        view.post(new Runnable() {
            @Override
            public void run() {
                final View anchorView = view.getRootView();
                if (type == HIGH_LIGHT_VIEW_TYPE_CIRCLE) {
                    prepareCircle(anchorView, view);
                } else if (type == HIGH_LIGHT_VIEW_TYPE_RECT) {
                    prepareRect(anchorView, view);
                } else {
                    prepareRectSpec(anchorView, view);
                }
                if (anchorView instanceof ViewGroup) {
                    ((ViewGroup) anchorView).addView(HighLightView.this, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                }
            }
        });
    }

    private void prepareCircle(View anchorView, View view) {
        final int[] baseLocation = new int[2];
        anchorView.getLocationInWindow(baseLocation);
        final int[] viewLocation = new int[2];
        view.getLocationInWindow(viewLocation);//获取当前view在窗口中的位置
        final int viewHeight = view.getHeight();
        final int viewWidth = view.getWidth();
        final int halfHeight = viewHeight / 2;
        mRadius = (int) (viewWidth * RADIUS_RATIO);
        mCenterX = viewLocation[0] - baseLocation[0] + viewWidth / 2;// 获取圆心x坐标
        mCenterY = viewLocation[1] - baseLocation[1] + halfHeight; // 获取圆心y坐标

        // 可点击区域为圆心按钮相交的近似矩形
        mClickRect.set(mCenterX - mRadius, mCenterY - halfHeight, mCenterX + mRadius, mCenterY + halfHeight);
        mTipX = (anchorView.getWidth() - mPaint.measureText(mTip)) / 2; //提示文字的横坐标，居中即可
        mTipY = viewLocation[1] + mRadius * 2; // 提示文字的纵坐标，只需要在圆的下方，这里设为view的纵坐标加上直径
        mBitmap = Bitmap.createBitmap(anchorView.getWidth(), anchorView.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(mBitmap);
        canvas.drawColor(mOverlayColor);
        mPaint.setXfermode(X_FER_MODE);
        canvas.drawCircle(mCenterX, mCenterY, mRadius, mPaint);
        mPaint.setXfermode(null);
        mPaint.setColor(Color.WHITE);
        canvas.drawText(mTip, mTipX, mTipY, mPaint);
    }

    private void prepareRect(View anchorView, View view) {
        final int[] baseLocation = new int[2];
        anchorView.getLocationInWindow(baseLocation);
        final int[] viewLocation = new int[2];
        view.getLocationInWindow(viewLocation);//获取当前view在窗口中的位置
        final int viewHeight = view.getHeight();
        final int viewWidth = view.getWidth();
        final int halfHeight = viewHeight / 2;
        final int halfWidth = viewWidth / 2;
        mRadius = (int) (viewWidth * RADIUS_RATIO);
        mCenterX = viewLocation[0] - baseLocation[0] + halfWidth;// 获取圆心x坐标
        mCenterY = viewLocation[1] - baseLocation[1] + halfHeight; // 获取圆心y坐标

        // 可点击区域为绘制矩形
        mClickRect.set(mCenterX - halfWidth, mCenterY - halfHeight
                , mCenterX + halfWidth, mCenterY + halfHeight);
        mTipX = (anchorView.getWidth() - mPaint.measureText(mTip)) / 2; //提示文字的横坐标，居中即可
        mTipY = viewLocation[1] + viewHeight + mPaint.getTextSize()*5/3; // 提示文字的纵坐标，只需要在矩形的下方，这里设为view的纵坐标加上直径
        mBitmap = Bitmap.createBitmap(anchorView.getWidth(), anchorView.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(mBitmap);
        canvas.drawColor(mOverlayColor);
        mPaint.setXfermode(X_FER_MODE);
        canvas.drawRect(mCenterX - halfWidth, mCenterY - halfHeight
                , mCenterX + halfWidth, mCenterY + halfHeight, mPaint);
        mPaint.setXfermode(null);
        mPaint.setColor(Color.WHITE);
        canvas.drawText(mTip, mTipX, mTipY, mPaint);
    }

    private void prepareRectSpec(View anchorView, View view) {
        final int[] baseLocation = new int[2];
        anchorView.getLocationInWindow(baseLocation);
        final int[] viewLocation = new int[2];
        view.getLocationInWindow(viewLocation);//获取当前view在窗口中的位置
        final int viewHeight = view.getHeight();
        final int viewWidth = view.getWidth();
        final int halfHeight = viewHeight / 2;
        final int halfWidth = viewWidth / 2;
        mRadius = (int) (viewWidth * RADIUS_RATIO);
        mCenterX = viewLocation[0] - baseLocation[0] + halfWidth;// 获取圆心x坐标
        mCenterY = viewLocation[1] - baseLocation[1] + halfHeight; // 获取圆心y坐标
        int Offset = getResources().getDimensionPixelSize(R.dimen.length_80dp);
        // 可点击区域为绘制矩形
        mClickRect.set(mCenterX - halfWidth, mCenterY - halfHeight
                , mCenterX + halfWidth, mCenterY + halfHeight + halfHeight + Offset);
        mTipX = (anchorView.getWidth() - mPaint.measureText(mTip)) / 2; //提示文字的横坐标，居中即可
        mTipY = viewLocation[1] + viewHeight + mPaint.getTextSize()*5/3  + halfHeight + Offset; // 提示文字的纵坐标，只需要在矩形的下方，这里设为view的纵坐标加上直径
        mBitmap = Bitmap.createBitmap(anchorView.getWidth(), anchorView.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(mBitmap);
        canvas.drawColor(mOverlayColor);
        mPaint.setXfermode(X_FER_MODE);
        canvas.drawRect(mCenterX - halfWidth, mCenterY - halfHeight
                , mCenterX + halfWidth, mCenterY + halfHeight + Offset, mPaint);// Show more
        mPaint.setXfermode(null);
        mPaint.setColor(Color.WHITE);
        canvas.drawText(mTip, mTipX, mTipY, mPaint);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawBitmap(mBitmap, 0, 0, null);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                final PointF down = new PointF(event.getX(), event.getY());
                if (isInClickRect(down)) {
                    mDownInClickRect = true;
                }
                break;

            case MotionEvent.ACTION_UP:
                if (!mDownInClickRect) {
                    break;
                }
                final PointF up = new PointF(event.getX(), event.getY());
                if (isInClickRect(up) && mClickListener != null) {
                    mClickListener.onClick(mHighLightView);
                    ((ViewGroup)getParent()).removeView(HighLightView.this);
                }
                mDownInClickRect = false;
                return true;
        }
        return true;
    }

    private boolean isInClickRect(PointF point) {
        return point.x > mClickRect.left && point.x < mClickRect.right
                && point.y > mClickRect.top && point.y < mClickRect.bottom;
    }
}
