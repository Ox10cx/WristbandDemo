package com.realsil.android.wristbanddemo.view;

import android.content.Context;
import android.graphics.*;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.hardware.SensorManager;
import android.os.Handler;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.*;
import android.view.GestureDetector.OnGestureListener;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * @author chenll 自定义chart显示控件 使用示例 chartView = (ChartView)
 *         findViewById(R.id.chartView);
 *         chartView.setBackColor(Color.parseColor(
 *         getString(R.color.backColor)));
 *         chartView.setAxesLineColor(Color.parseColor
 *         (getString(R.color.axesLineColor)));
 *         chartView.setGridLineColor(Color.
 *         parseColor(getString(R.color.axesLineColor)));
 *         chartView.setPointColor
 *         (Color.parseColor(getString(R.color.ponitineColor)));
 *         chartView.setShapeColor(Color.YELLOW); chartView.setYMaxValue(150);
 *         chartView.setYMinValue(0); chartView.setData(dataList);
 *         chartView.setOnTapPointListener(new OnTapPointListener() {
 * @Override public void onTap(ChartAxes axes) {
 * <p/>
 * Toast.makeText(MainActivity.this, axes.Y + "yy",
 * Toast.LENGTH_SHORT).show(); } });
 * chartView.startChart();全部设置完成以后，通过这个方法进行启动chart
 */
public class SleepChartView extends SurfaceView {
    // Log
    private final static String TAG = "SleepChartView";
    private final static boolean D = true;

    private Context context;
    private SurfaceHolder holder;
    private GestureDetector detector;

    /**
     * 设置数据
     */
    private List<ChartData> valueData = new ArrayList<>();
    /**
     * x轴坐标
     */
    private List<AxisValue> xAxisList = new ArrayList<>();
    /**
     * 画布宽度
     */
    private int width = 0;
    /**
     * 画布高度
     */
    /**/
    private int height = 0;

    /**
     * X轴刻度行距
     */
    private int xdistnance = 40;
    /**
     *X轴刻度距离X轴的 
     */
    private int toxdistnance = 40;
    private int yTextpaddingleft=20;

    public int getyTextpaddingleft() {
        return yTextpaddingleft;
    }

    public void setyTextpaddingleft(int yTextpaddingleft) {
        this.yTextpaddingleft = yTextpaddingleft;
    }

    public int getToxdistnance() {
        return this.toxdistnance;
    }

    public void setToxdistnance(int toxdistnance) {
        this.toxdistnance = toxdistnance;
    }

    public int getXdistnance() {
        return this.xdistnance;
    }

    public void setXdistnance(int xdistnance) {
        this.xdistnance = xdistnance;
    }

    private final static int INVALID_DATA = 0x7fffffff;

    /**
     * Y轴最大值
     */
    private float yMaxValue = INVALID_DATA;
    /**
     * Y轴最小值
     */
    private float yMinValue = 0;
    /**
     * X轴最大值
     */
    private float xMaxValue = INVALID_DATA;
    /**
     * X轴最小值
     */
    private float xMinValue = INVALID_DATA;
    /**
     * 左边距
     */
    private int leftPadding = 0;
    /**
     * 顶部边距
     */
    private int topPadding = 10;
    /**
     * 右侧边距
     */
    private int rightPadding = 12;
    /**
     * 底部边距
     */
    private int bottomPadding = 25;
    /**
     * X轴刻度文字显示区域高度
     */
    private int xTextHeight = 50;

    /**
     * 顶部文字显示区域高度
     */
    private int topTextHeight = 0;
    /**
     * 顶部文字显示区域高度
     */
    private int topPointTextColor = Color.parseColor("#000000");
    /**
     * 背景色
     */
    private int backColor = Color.TRANSPARENT;//Color.parseColor("#AAB470");//
    /**
     * 坐标Y轴和边框的颜色
     */
    private int axesLineColor = Color.parseColor("#d3d3d3");
    /**
     * 坐标Y轴和边框的颜色
     */
    private int axesLineYColor = Color.parseColor("#d3d3d3");

    /**
     * 坐标Y轴和边框的颜色
     */
    private int axesLineXColor = Color.parseColor("#d3d3d3");
    /**
     * 坐标轴X和边框的颜色
     */
    private int axesXLineColor = Color.parseColor("#d3d3d3");
    /**
     * 内部网格线的颜色
     */
    private int gridLineColor = Color.parseColor("#d3d3d3");
    /**
     * chart图线的颜色
     */
    private int chartLineColor = Color.GRAY;
    /**
     * 阴影的颜色
     */
    private int shapeColor = Color.parseColor("#000000");
    /**
     * 数据点的点击事件监听
     */
    private OnTapPointListener tapPointListener;
    /**
     * X轴刻度文字字体大小
     */
    private int bottomTextSize = 25;
    /**
     * 边框线宽度
     */
    private int chartFrameLineSize = 1;
    /**
     * 网格线宽度
     */
    private int gridLineSize = 1;

    /**
     * 当前选中的点
     */
    private int currentPressedPoint = INVALID_DATA;
    /**
     * 第一个点与最后一个点距离左边界和右边界的距离
     */
    private static final int POINT_PADDING = 0;

    /**
     * X轴画笔
     */
    private Paint Xpaint;

    /**
     * 最顶上一条线离封顶20px
     */
    int chartInnerTopPadding = 20;

    public SleepChartView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.context = context;
        holder = getHolder();
        holder.addCallback(new ChartCallBack());
    }

    public SleepChartView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
        holder = getHolder();
        holder.addCallback(new ChartCallBack());
    }

    public SleepChartView(Context context) {
        this(context, null);
        holder = getHolder();
        holder.addCallback(new ChartCallBack());
    }

    /**
     * 全部设置完成后，开始chart的绘制
     */
    public void startChart() {
        width = getWidth();
        height = getHeight();

        refreshChart();

        //detector = new GestureDetector(SleepChartView.this.getContext(), new ChartGestureListener());
        setOnTouchListener(new ChartTouchListener());
    }

    /**
     * 设置显示的chart数据
     *
     * @param data
     */
    public void setData(List<ChartData> data) {
        this.valueData = data;

        updateMaxMinXYValue();
    }

    public void setData(SleepChartData data) {
        this.valueData = data.value;
        this.xAxisList = data.axis;

        updateMaxMinXYValue();
    }

    public List<ChartData> getData() {
        return valueData;
    }

    /**
     * 触摸事件
     */
    class ChartTouchListener implements OnTouchListener {

        private float startX;
        private float downX;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            //detector.onTouchEvent(event);
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:

                    break;
                case MotionEvent.ACTION_UP:

                    checkAndStartPressAnimation(event.getX(), event.getY());

                    //v.performClick();
                    break;
                case MotionEvent.ACTION_MOVE:

                    break;

                default:
                    break;
            }
            return true;
        }
    }

    private void checkAndStartPressAnimation(float x, float y) {
        SleepChartAxes axes = isAvailableTap(x, y);
        if(axes != null) {
            startPressAnimation();
            if(tapPointListener != null) {
                tapPointListener.onTap(axes);
            }
        }
    }

    /**
     * 手势事件
     */
    class ChartGestureListener implements OnGestureListener {

        @Override
        public boolean onDown(MotionEvent e) {
            Log.d(TAG, "onDown: " + e.toString());
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            Log.d(TAG, "onFling: " + e1.toString());
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {

        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            return false;
        }

        @Override
        public void onShowPress(MotionEvent e) {
            Log.d(TAG, "onShowPress: " + e.toString());
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            Log.d(TAG, "onSingleTapUp: " + e.toString());
            return true;
        }
    }

    class ChartCallBack implements SurfaceHolder.Callback {

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            if(D) Log.d(TAG, "surfaceChanged");
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            if(D) Log.w(TAG, "surfaceCreated");
            startChart();
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            if(D) Log.e(TAG, "surfaceDestroyed");
            clear();
        }
    }

    /**
     * 设置纵坐标的最大值和最小值
     *
     * @param yMax 最大值
     * @param yMin 最小值
     */
    public void setLimitYValues(int yMax, int yMin) {

        this.yMaxValue = yMax;
        this.yMinValue = yMin;
    }

    /**
     * 设置Y轴最大值
     *
     * @param yMax 最大值
     */
    public void setYMaxValue(int yMax) {
        this.yMaxValue = yMax;
    }

    /**
     * 设置Y轴最小值
     *
     * @param yMin 最小值
     */
    public void setYMinValue(int yMin) {
        this.yMinValue = yMin;
    }

    /**
     * 设置横坐标的最大值和最小值
     *
     * @param xMax 最大值
     * @param xMin 最小值
     */
    public void setLimitXValues(int xMax, int xMin) {

        this.xMaxValue = xMax;
        this.xMinValue = xMin;
    }

    /**
     * 设置X轴最大值
     *
     * @param xMax 最大值
     */
    public void setXMaxValue(int xMax) {
        this.xMaxValue = xMax;
    }

    /**
     * 设置X轴最小值
     *
     * @param xMin 最小值
     */
    public void setXMinValue(int xMin) {
        this.xMinValue = xMin;
    }

    /**
     * chart图显示的边距
     *
     * @param left   左边距
     * @param top    顶部边距
     * @param right  右侧边距
     * @param bottom 底部边距
     */
    public void setChartPadding(int left, int top, int right, int bottom) {
        this.leftPadding = left;
        this.topPadding = top;
        this.rightPadding = right;
        this.bottomPadding = bottom;
    }

    /**
     * 设置chart图显示的左边距
     *
     * @param left 左边距
     */
    public void setChartLeftPadding(int left) {
        this.leftPadding = left;
    }

    /**
     * 设置chart图显示的顶部边距
     *
     * @param top 顶部边距
     */
    public void setChartTopPadding(int top) {
        this.topPadding = top;
    }
    /**
     * 设置chart图显示的右边距
     *
     * @param right 右侧边距
     */
    public void setChartRightPadding(int right) {
        this.rightPadding = right;
    }

    /**
     * 设置chart图显示的底部边距
     *
     * @param bottom 底部边距
     */
    public void setChartBottomPadding(int bottom) {
        this.bottomPadding = bottom;
    }

    /**
     * 设置X轴刻度文本显示区域的高度
     *
     * @param xTextWidth 区域高度值
     */
    public void setxTextWidth(int xTextWidth) {
        this.xTextHeight = xTextWidth;
    }
    /**
     * X轴刻度文字字体大小
     *
     * @return 字体大小 PX单位
     */
    public int getBottomTextSize() {
        return bottomTextSize;
    }

    /**
     * 设置X轴刻度文字字体大小
     *
     * @param bottomTextSize 字体大小 PX单位
     */
    public void setBottomTextSize(int bottomTextSize) {
        this.bottomTextSize = bottomTextSize;
    }

    /**
     * 设置chart图边框线宽度
     *
     * @param chartFrameLineSize 宽度值 PX单位
     */
    public void setChartFrameLineSize(int chartFrameLineSize) {
        this.chartFrameLineSize = chartFrameLineSize;
    }

    /**
     * 设置网格线宽度
     *
     * @param gridLineSize 网格线宽度值 PX单位
     */
    public void setGridLineSize(int gridLineSize) {
        this.gridLineSize = gridLineSize;
    }
    /**
     * 设置背景色
     *
     * @param backColor 背景色值
     */
    public void setBackColor(int backColor) {
        this.backColor = backColor;
    }

    /**
     * 设置坐标轴的和边框的颜色
     *
     * @param axesLineColor 颜色值
     */
    public void setAxesLineColor(int axesLineColor) {
        this.axesLineColor = axesLineColor;
    }

    /**
     * 设置网格线的颜色
     *
     * @param gridLineColor 颜色值
     */
    public void setGridLineColor(int gridLineColor) {
        this.gridLineColor = gridLineColor;
    }

    /**
     * 设置Chart图线的颜色
     *
     * @param chartLineColor 颜色值
     */
    public void setChartLineColor(int chartLineColor) {
        this.chartLineColor = chartLineColor;
    }

    /**
     * 画整体chart图
     *
     */
    public void refreshChart() {
        synchronized (holder) {
            if (holder != null) {
                Canvas canvas = holder.lockCanvas();
                if (canvas != null) {
                    canvas.drawColor(backColor, PorterDuff.Mode.CLEAR);
                    drawChartLine(canvas);
                    drawAxesLine(canvas);

                    holder.unlockCanvasAndPost(canvas);
                }
            }
        }
    }

    /**
     * 画X，Y轴和边框
     *
     * @param canvas
     */
    private void drawAxesLine(Canvas canvas) {
        Xpaint = new Paint();
        Xpaint.setColor(axesLineXColor);
        Xpaint.setAntiAlias(true);
        Xpaint.setStyle(Style.FILL_AND_STROKE);
        Xpaint.setStrokeWidth(chartFrameLineSize);

        // 下方X轴
        canvas.drawLine(leftPadding , height - bottomPadding - xTextHeight , width-rightPadding , height - bottomPadding - xTextHeight,
                Xpaint);
    }
    /**
     * 画折线图，网格竖线，X坐标值
     *
     * @param canvas
     */
    private void drawChartLine(Canvas canvas) {
        if(D) Log.d(TAG, "drawChartLine, currentPressedPoint: " + currentPressedPoint
                + ", currentPercent: " + currentPercent);
        Paint paintChart = new Paint();
        paintChart.setAntiAlias(true);
        paintChart.setStyle(Style.FILL);

        Paint paintGrid = new Paint();
        paintGrid.setColor(gridLineColor);
        paintGrid.setAntiAlias(true);
        paintGrid.setStyle(Style.STROKE);
        paintGrid.setStrokeWidth(gridLineSize);

        Paint paintBottomText = new Paint();
        paintBottomText.setColor(axesXLineColor);
        paintBottomText.setAntiAlias(true);
        paintBottomText.setSubpixelText(true);
        paintBottomText.setTypeface(Typeface.MONOSPACE);
        paintBottomText.setTextSize(bottomTextSize);
        paintBottomText.setTextAlign(Align.CENTER);

        if(valueData.size() > 0) {
            for (int i = 0; i < valueData.size() - 1; i++) {
                paintChart.setColor(valueData.get(i).color);
                if (currentPressedPoint != INVALID_DATA
                        && currentPressedPoint == i) {
                    canvas.drawRect(getChartRealXByDataX(valueData.get(i).X)
                            , getChartRealYByDataY(valueData.get(i).Y * (currentPercent/MAX_PERCENT))
                            , getChartRealXByDataX(valueData.get(i + 1).X)
                            , getChartRealYByDataY(0)
                            , paintChart);
                    continue;
                }
                canvas.drawRect(getChartRealXByDataX(valueData.get(i).X)
                        , getChartRealYByDataY(valueData.get(i).Y)
                        , getChartRealXByDataX(valueData.get(i + 1).X)
                        , getChartRealYByDataY(0)
                        , paintChart);
            }
        }

        if (xAxisList.size() > 0) {
            for (int i = 0; i < xAxisList.size(); i++) {
                //Log.d(TAG, "xAxisList.get(i).value: " + xAxisList.get(i).value + ", xAxisList.get(i).label: " + xAxisList.get(i).label);
                // 写X轴坐标的刻度值
                if (!TextUtils.isEmpty(xAxisList.get(i).label)) {
                    int diffX = 0;
                    if(i == 0) {
                        diffX = 30;
                    } else if(i == xAxisList.size() - 1) {
                        diffX = -30;
                    }
                    canvas.drawText(xAxisList.get(i).label, getChartRealXByDataX(xAxisList.get(i).value) + diffX , getChartRealYByDataY(0) + 40, paintBottomText);
                }
            }
        }


        /*
        if (axesData.size() > 0) {
            for (int i = 0; i < axesData.size() - 1; i++) {
                // 画块状图
                canvas.drawRect(axesData.get(i).X, axesData.get(i).Y, axesData.get(i + 1).X, height - bottomPadding - xTextHeight, paintChart);
            }
        }*/
    }

    final Handler pressAnimationTimer = new Handler();
    Runnable pressAnimationTask;
    private final static float MAX_PERCENT = 20;
    private final static int PRESS_ANIMATION_DURATION = 10;
    private float currentPercent;
    public void startPressAnimation() {
        currentPercent = 0;
        if(D) Log.d(TAG, "startPressAnimation");
        pressAnimationTask = new Runnable(){
            @Override
            public void run() {
                // TODO Auto-generated method stub
                //if(D) Log.d(TAG, "task, currentPercent: " + currentPercent);
                if(currentPercent > MAX_PERCENT) {
                    stopPressAnimation();
                    return;
                }
                refreshChart();

                currentPercent ++;

                pressAnimationTimer.postDelayed(pressAnimationTask, PRESS_ANIMATION_DURATION);
            }
        };

        // 0.1s
        pressAnimationTimer.postDelayed(pressAnimationTask, PRESS_ANIMATION_DURATION);
    }

    public void stopPressAnimation() {
        if(D) Log.d(TAG, "stopPressAnimation");
        clearPressed();
        pressAnimationTimer.removeCallbacks(pressAnimationTask);
    }

    /**
     * 通过x轴间距,计算出字体大小,自动适应宽度
     *
     * @param paint        the Paint to set the text size for
     * @param desiredWidth the desired width
     * @param text         the text that should be that width
     * @return
     */
    private void setTextSizeForWidth(Paint paint, float desiredWidth, String text) {

        // Pick a reasonably large value for the test. Larger values produce
        // more accurate results, but may cause problems with hardware
        // acceleration. But there are workarounds for that, too; refer to
        // http://stackoverflow.com/questions/6253528/font-size-too-large-to-fit-in-cache
        final float testTextSize = 48f;

        // Get the bounds of the text, using our testTextSize.
        paint.setTextSize(testTextSize);
        Rect bounds = new Rect();
        paint.getTextBounds(text, 0, text.length(), bounds);

        // Calculate the desired size as a proportion of our testTextSize.
        float desiredTextSize = testTextSize * desiredWidth / bounds.width();

        //如果计算出的字体高度小于预设值
        if (bottomTextSize > desiredTextSize) {
            // Set the paint for that size.
            paint.setTextSize(desiredTextSize);
        } else {
            paint.setTextSize(bottomTextSize);
        }
    }

    /**
     * 要画的数据
     */
    public static class ChartData {
        public float Y;
        public float X;
        public int color;

        public ChartData(float x, float y, int color) {
            Y = y;
            X = x;
            this.color = color;
        }

        @Override
        public String toString() {
            return "ChartData [Y=" + Y + ", X=" + X + "]";
        }
    }

    public static class AxisValue {
        public float value;
        public String label;

        public AxisValue(float value, String label) {
            this.value = value;
            this.label = label;
        }
    }

    public static class SleepChartData {
        public ArrayList<ChartData> value;
        public ArrayList<AxisValue> axis;

        public SleepChartData(ArrayList<ChartData> value, ArrayList<AxisValue> axis) {
            this.value = value;
            this.axis = axis;
        }
    }

    /**
     * 转化为X，Y的坐标值对象
     */
    public static class SleepChartAxes {

        public float startX;
        public float endX;
        public float Y;

        public SleepChartAxes(float startX, float endX, float Y) {
            this.startX = startX;
            this.endX = endX;
            this.Y = Y;
        }

        @Override
        public String toString() {
            return "ChartAxes [startX=" + startX +", endX=" + endX +  ", Y=" + Y + "]";
        }
    }

    private float getChartRealYByDataY(float y) {
        int yArea = getYTotalLength();
        float yValue = topPadding + topTextHeight + chartInnerTopPadding
                + (yArea * (1 - ((y - yMinValue) / (yMaxValue - yMinValue))));

        return yValue;
    }

    private float getChartRealXByDataX(float x) {
        int xArea = getXTotalLength();
        float xValue = leftPadding  + POINT_PADDING
                + (xArea * ((x - xMinValue) / (xMaxValue - xMinValue)));

        return xValue;
    }

    private float getDataYByChartRealY(float yValue) {
        int yArea = getYTotalLength();

        float y = (1 - ((yValue - (topPadding + topTextHeight + chartInnerTopPadding))/yArea)) * (yMaxValue - yMinValue) + yMinValue;

        //if(D) Log.d(TAG, "getDataYByChartRealY, yValue: " + yValue + ", y: " + y);
        return y;
    }

    private float getDataXByChartRealX(float xValue) {
        int xArea = getXTotalLength();

        float x = ((xValue - leftPadding  + POINT_PADDING)/xArea) * (xMaxValue - xMinValue) + xMinValue;

        //if(D) Log.d(TAG, "getDataXByChartRealX, xValue: " + xValue + ", x: " + x);
        return x;
    }

    private int getXTotalLength() {
        int xArea = width - leftPadding - rightPadding;
        return xArea;
    }

    private int getYTotalLength() {
        int yArea = height - topPadding - bottomPadding - xTextHeight - topTextHeight - chartInnerTopPadding;
        return yArea;
    }

    private void updateMaxMinXYValue() {
        float minY = 0x7fffffff;
        float minX = 0x7fffffff;
        float maxY = 0;
        float maxX = 0;
        for(ChartData data: valueData) {
            if(data.X > maxX) {
                maxX = data.X;
            }
            if(data.Y > maxY) {
                maxY = data.Y;
            }
            if(data.X < minX) {
                minX = data.X;
            }
            if(data.Y < minY) {
                minY = data.Y;
            }
        }

        if(xMinValue == INVALID_DATA) {
            xMinValue = minX;
        }
        if(xMaxValue == INVALID_DATA) {
            xMaxValue = maxX;
        }
        // Min y only set to zero.
        /*
        if(yMinValue == INVALID_DATA) {
            yMinValue = minY;
        }*/
        if(yMaxValue == INVALID_DATA) {
            yMaxValue = maxY;
        }

        if(D) Log.d(TAG, "updateMaxMinXYValue, xMinValue: " + xMinValue
                + ", xMaxValue: " + xMaxValue
                + ", yMinValue: " + yMinValue
                + ", yMaxValue: " + yMaxValue
                + ", minX: " + minX
                + ", maxX: " + maxX
                + ", minY: " + minY
                + ", maxY: " + maxY);
    }
    /**
     * 判断当前点击是否点击在有效区域
     *
     * @param x 点击的X坐标值
     * @param y 点击的Y坐标值
     * @return 返回当前的坐标的对应区域
     */
    private SleepChartAxes isAvailableTap(float x, float y) {
        SleepChartAxes axes = null;

        for (int i = 0; i < valueData.size() - 1; i++) {
            if(D) Log.d(TAG, "isAvailableTap, x: " + x + ", y: " + y
                    + ", getChartRealXByDataX(valueData.get(i).X): " + getChartRealXByDataX(valueData.get(i).X)
                    + ", getChartRealXByDataX(valueData.get(i + 1).X: " + getChartRealXByDataX(valueData.get(i + 1).X)
                    + ", getChartRealYByDataY(0): " + getChartRealYByDataY(0)
                    + ", getChartRealYByDataY(valueData.get(i).Y): " + getChartRealYByDataY(valueData.get(i).Y));
            if ((x > getChartRealXByDataX(valueData.get(i).X) && x < getChartRealXByDataX(valueData.get(i + 1).X))
                    && (y < getChartRealYByDataY(0) && y > getChartRealYByDataY(valueData.get(i).Y))) {
                currentPressedPoint = i;
                axes = new SleepChartAxes(valueData.get(i).X, valueData.get(i + 1).X, valueData.get(i).Y);
                break;
            }
        }
        return axes;
    }

    /**
     * 数据点的点击事件监听
     */
    public interface OnTapPointListener {
        void onTap(SleepChartAxes axes);
    }

    public void setOnTapPointListener(OnTapPointListener tapPointListener) {
        this.tapPointListener = tapPointListener;
    }

    public void clearPressed() {
        currentPressedPoint = INVALID_DATA;
        refreshChart();
    }

    public void clear() {
        valueData.clear();
    }
}
