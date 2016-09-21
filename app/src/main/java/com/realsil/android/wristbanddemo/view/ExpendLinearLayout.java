package com.realsil.android.wristbanddemo.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

public class ExpendLinearLayout extends LinearLayout {
    private ExpendLinearLayout.OnSizeChangedListener listener;

    public ExpendLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ExpendLinearLayout(Context context) {
        super(context);
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int count = this.getChildCount();

        for(int i = 0; i < count; ++i) {
            this.getChildAt(i).measure(widthMeasureSpec, heightMeasureSpec);
        }

    }

    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if(this.listener != null) {
            this.listener.onSizeChanged(w, h, oldw, oldh);
        }

    }

    public void setOnSizeChangedListener(ExpendLinearLayout.OnSizeChangedListener listener) {
        this.listener = listener;
    }

    public interface OnSizeChangedListener {
        void onSizeChanged(int var1, int var2, int var3, int var4);
    }
}
