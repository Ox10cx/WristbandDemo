package com.realsil.android.wristbanddemo.utility;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.LinearLayout;

/**
 * Use to disable the child slide event, when child need listen click event.
 */
public class DisableChildSlideLinearLayout extends LinearLayout {

    public DisableChildSlideLinearLayout(Context context) {
        this(context, null);
    }

    public DisableChildSlideLinearLayout(Context context, AttributeSet attrs,
                                 int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
    }

    public DisableChildSlideLinearLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    private void initView() {

    }

    private boolean mScrolling;
    private float touchDownY;
    private float touchDownX;

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        Log.d("Disable onInterceptTouchEvent", "Math: " + (Math.abs(touchDownY - event.getRawY())
                + Math.abs(touchDownX - event.getRawX())));
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touchDownY = event.getRawY();
                touchDownX = event.getRawX();
                mScrolling = false;
                break;
            case MotionEvent.ACTION_MOVE:
                if ((Math.abs(touchDownY - event.getRawY())
                        + Math.abs(touchDownX - event.getRawX())) >= 10) {
                    mScrolling = true;
                } else {
                    mScrolling = false;
                }
                break;
            case MotionEvent.ACTION_UP:
                mScrolling = false;
                break;
        }
        return mScrolling;
    }

    float x1 = 0;
    float x2 = 0;
    /*
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                return true;
            case MotionEvent.ACTION_MOVE:

                break;
            case MotionEvent.ACTION_UP:
                x2 = event.getX();
                if (touchDownX - x2 > DensityUtil.dip2px(getContext(), 40)) {
                    if(mSetOnSlideListener!=null){
                        mSetOnSlideListener.onRightToLeftSlide();
                    }
                }
                if (touchDownX - x2 < -DensityUtil.dip2px(getContext(), 40)) {
                    if(mSetOnSlideListener!=null){
                        mSetOnSlideListener.onLeftToRightSlide();
                    }
                }
                break;
        }

        return super.onTouchEvent(event);
    }

    private setOnSlideListener mSetOnSlideListener;

    public setOnSlideListener getmSetOnSlideListener() {
        return mSetOnSlideListener;
    }

    public void setmSetOnSlideListener(setOnSlideListener mSetOnSlideListener) {
        this.mSetOnSlideListener = mSetOnSlideListener;
    }

    public interface setOnSlideListener{
        void onRightToLeftSlide();
        void onLeftToRightSlide();
    }*/

}