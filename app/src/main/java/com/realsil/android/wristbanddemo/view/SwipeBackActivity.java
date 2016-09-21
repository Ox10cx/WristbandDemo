package com.realsil.android.wristbanddemo.view;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.v4.widget.SlidingPaneLayout;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;


import com.realsil.android.wristbanddemo.R;

import java.lang.reflect.Field;

/**
 * Created by Administrator on 2016/7/4.
 */
public abstract class SwipeBackActivity extends Activity implements SlidingPaneLayout.PanelSlideListener {
    // Log
    private final static String TAG = "SwipeBackActivity";
    private final static boolean D = true;

    PagerEnabledSlidingPaneLayout mSlidingPaneLayout;

    Context context;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.setContentView(R.layout.activity_base);

        initSwipeBackFinish();

        context = getApplication();
    }

    /**
     * Sub Activity must use the
     * */
    public void setContentView(@LayoutRes int layoutResID) {
        if(D) Log.d(TAG, "setContentView");

        View view = View.inflate(context, layoutResID, null);
        LinearLayout parentView = (LinearLayout) this.findViewById(R.id.ll_base_container);

        parentView.addView(view);
    }

    /**
     * 初始化滑动返回
     */
    private void initSwipeBackFinish() {
        //Log.d("123", "initSwipeBackFinish:" + isSupportSwipeBack());
        if (isSupportSwipeBack()) {
            mSlidingPaneLayout = new PagerEnabledSlidingPaneLayout(this);
            //通过反射改变mOverhangSize的值为0，这个mOverhangSize值为菜单到右边屏幕的最短距离，默认
            //是32dp，现在给它改成0
            try {
                //属性
                Field f_overHang = SlidingPaneLayout.class.getDeclaredField("mOverhangSize");
                f_overHang.setAccessible(true);
                f_overHang.set(mSlidingPaneLayout, 0);
            } catch (Exception e) {
                e.printStackTrace();
            }
            mSlidingPaneLayout.setPanelSlideListener(this);
            mSlidingPaneLayout.setSliderFadeColor(getResources().getColor(android.R.color.transparent));

            View leftView = new View(this);
            leftView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            mSlidingPaneLayout.addView(leftView, 0);

            ViewGroup decor = (ViewGroup) getWindow().getDecorView();
            ViewGroup decorChild = (ViewGroup) decor.getChildAt(0);
//            decorChild.setBackgroundColor(getResources().getColor(android.R.color.white));
//            decor.removeView(decorChild);
            View containerView = decor.findViewById(R.id.ll_base_parent);
            containerView.setBackgroundColor(getResources().getColor(android.R.color.white));
            ViewGroup parent = (ViewGroup) containerView.getParent();
            parent.removeView(containerView);
            mSlidingPaneLayout.addView(containerView);
            parent.addView(mSlidingPaneLayout);
//            slidingPaneLayout.addView(decorChild, 1);
        }
    }

    /**
     * 是否支持滑动返回
     *
     * @return
     */
    protected boolean isSupportSwipeBack() {
        return true;
    }

    protected void onPanelChanged() {

    }

    @Override
    public void onPanelClosed(View view) {

    }

    @Override
    public void onPanelOpened(View view) {
        onPanelChanged();
        finish();
        this.overridePendingTransition(0, R.anim.slide_out_right);
    }

    @Override
    public void onPanelSlide(View view, float v) {
    }

    // In some case, canScroll is not useful
    public void allowDrag(boolean enable) {
        mSlidingPaneLayout.isAllowDrag = enable;
    }
}
