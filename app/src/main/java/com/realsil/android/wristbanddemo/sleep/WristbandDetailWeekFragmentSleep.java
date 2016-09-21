package com.realsil.android.wristbanddemo.sleep;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.realsil.android.wristbanddemo.R;
import com.realsil.android.wristbanddemo.greendao.SleepData;
import com.realsil.android.wristbanddemo.greendao.SportData;
import com.realsil.android.wristbanddemo.sport.SportSubData;
import com.realsil.android.wristbanddemo.sport.SportWeekColumnUiManager;
import com.realsil.android.wristbanddemo.sport.SportWeekLineUiManager;
import com.realsil.android.wristbanddemo.utility.GlobalGreenDAO;
import com.realsil.android.wristbanddemo.utility.RefreshableLinearLayoutView;
import com.realsil.android.wristbanddemo.utility.WristbandManager;
import com.realsil.android.wristbanddemo.utility.WristbandManagerCallback;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import lecho.lib.hellocharts.gesture.ZoomType;
import lecho.lib.hellocharts.listener.ColumnChartOnValueSelectListener;
import lecho.lib.hellocharts.listener.LineChartOnValueSelectListener;
import lecho.lib.hellocharts.model.PointValue;
import lecho.lib.hellocharts.model.SubcolumnValue;
import lecho.lib.hellocharts.view.ColumnChartView;
import lecho.lib.hellocharts.view.LineChartView;

public class WristbandDetailWeekFragmentSleep extends Fragment {
    // Log
    private final static String TAG = "WristbandDetailWeekFragmentSleep";
    private final static boolean D = true;

    public static final String EXTRAS_DATE = "DATE";
    public static final String EXTRAS_DATE_YEAR = "DATE_YEAR";
    public static final String EXTRAS_DATE_MONTH = "DATE_MONTH";
    public static final String EXTRAS_DATE_DAY = "DATE_DAY";

    private static final int TYPE_LINE = 0;
    private static final int TYPE_COLUMN = 1;
    private int mType = TYPE_LINE;

    TextView mtvWeekDetailHeader;
    TextView mtvWeekTotalSleep;

    private ColumnChartView chart;
    private LineChartView lineChart;

    private ScrollView scrollView;

    private GlobalGreenDAO mGlobalGreenDAO;

    private int TOTAL_SLEEP = 8 * 60;

    private final double SLEEP_QUALITY_0 = 40;
    private final double SLEEP_QUALITY_1 = 60;
    private final double SLEEP_QUALITY_2 = 100;

    private Calendar mCalendar;
    private int mYear;
    private int mMonth;
    private int mDay;

    private Toast mToast;

    SleepWeekColumnUiManager sleepUi;
    private SleepWeekLineUiManager lineSleepUi;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = null;
        if(mType == TYPE_COLUMN) {
            rootView = inflater.inflate(R.layout.fragment_sleep_week, container, false);
        } else {
            rootView = inflater.inflate(R.layout.fragment_sleep_week_line, container, false);
        }
        // get global green dao instance
        mGlobalGreenDAO = GlobalGreenDAO.getInstance();

        mtvWeekDetailHeader = (TextView) rootView.findViewById(R.id.tvWeekDetailHeader);

        mtvWeekTotalSleep = (TextView) rootView.findViewById(R.id.tvDetailTotalSleep);
/*
        mCalendar = Calendar.getInstance();
        mCalendar.set(getArguments().getInt(EXTRAS_DATE_YEAR)
                ,getArguments().getInt(EXTRAS_DATE_MONTH)
                , getArguments().getInt(EXTRAS_DATE_DAY));
        if(D) Log.d(TAG, mCalendar.toString());
        */
        mYear = getArguments().getInt(EXTRAS_DATE_YEAR);
        mMonth = getArguments().getInt(EXTRAS_DATE_MONTH);
        mDay = getArguments().getInt(EXTRAS_DATE_DAY);

        scrollView = (ScrollView) rootView.findViewById(R.id.scrollView);

        if(mType == TYPE_COLUMN) {
            chart = (ColumnChartView) rootView.findViewById(R.id.lcvColumnChart);
        /*chart.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_UP){
                    scrollView.requestDisallowInterceptTouchEvent(false);
                }else{
                    scrollView.requestDisallowInterceptTouchEvent(true);
                }
                return false;
            }
        });*/
            chart.setOnValueTouchListener(new ValueTouchListener());
        } else {
            lineChart = (LineChartView) rootView.findViewById(R.id.lcvLineChart);
            lineChart.setOnValueTouchListener(new LineValueTouchListener());
            lineChart.setOnTouchListener(new View.OnTouchListener() {

                @Override
                public boolean onTouch(View v, MotionEvent event) {

                    if (lineChart.getZoomLevel() == 1) {
                        scrollView.requestDisallowInterceptTouchEvent(false);
                    } else {
                        scrollView.requestDisallowInterceptTouchEvent(true);
                    }
                    //if(D) Log.d(TAG, "chart.getZoomLevel(): " + chart.getZoomLevel());
                    return false;
                }
            });
        }

        initialStringFormat();

        return rootView;
    }
    //private String mSportCurrentStepFormat;
    private String mFormatSleepTotalSleep;
    private String mFormatDetailSleep;
    //private String mSportCurrentQuality;

    private void initialStringFormat() {
        mFormatSleepTotalSleep = getResources().getString(R.string.total_hour_min);
        mFormatDetailSleep = getResources().getString(R.string.detail_sleep_value);
    }

    private void initialUI() {
        //mtvWeekDetailHeader.setText(mYear + "/" + mMonth + "/" + mDay);
        mtvWeekDetailHeader.setText(String.valueOf(mYear));
        // Judge need of not to load two month
        Calendar c1 = Calendar.getInstance();
        c1.set(mYear, mMonth - 1, mDay);// here need decrease 1 of month
        int current = c1.get(Calendar.DAY_OF_WEEK);
        Log.d(TAG, "current: " + current + ", c1: " + c1.toString());

        // get the current month of sunday in the week.
        c1.add(Calendar.DATE, -1 * (current - Calendar.SUNDAY));
        // be careful, here need decrease one day, for calculator sleep data.
        c1.add(Calendar.DATE, -1);
        int firstMonth = c1.get(Calendar.MONTH) + 1;
        int firstYear = c1.get(Calendar.YEAR);
        int firstDay = c1.get(Calendar.DATE);
        Log.d(TAG, "firstYear: " + firstYear + ", firstMonth:" + firstMonth + ", c1: " + c1.toString());

        // get the current month of saturday in the week.
        c1.add(Calendar.DATE, Calendar.SATURDAY - Calendar.SUNDAY);
        int secondMonth = c1.get(Calendar.MONTH) + 1;
        int secondYear = c1.get(Calendar.YEAR);
        Log.d(TAG, "secondYear: " + secondYear + ", secondMonth:" + secondMonth + ", c1: " + c1.toString());

        List<SleepData> sleeps = new ArrayList<SleepData>();
        if(firstMonth == secondMonth) {
            sleeps = mGlobalGreenDAO.loadSleepDataByDate(mYear, mMonth);
        } else {
            List<SleepData> s1 = mGlobalGreenDAO.loadSleepDataByDate(firstYear, firstMonth);
            for(SleepData sp: s1) {
                sleeps.add(sp);
            }
            List<SleepData> s2 = mGlobalGreenDAO.loadSleepDataByDate(secondYear, secondMonth);
            for(SleepData sp: s2) {
                sleeps.add(sp);
            }
        }

        if(mType == TYPE_COLUMN) {
            sleepUi = new SleepWeekColumnUiManager(sleeps, mYear, mMonth, mDay);
            mtvWeekTotalSleep.setText(String.format(mFormatSleepTotalSleep
                    , sleepUi.getSumSleep() / 60
                    , sleepUi.getSumSleep() % 60 ));

            chart.setZoomEnabled(false);
            chart.setColumnChartData(sleepUi.getSleepTotalSleepColumnData());
        } else {
            lineSleepUi = new SleepWeekLineUiManager(sleeps, mYear, mMonth, mDay);
            mtvWeekTotalSleep.setText(String.format(mFormatSleepTotalSleep
                    , lineSleepUi.getSumSleep() / 60
                    , lineSleepUi.getSumSleep() % 60 ));

            if(lineSleepUi.getSumSleep() != 0) {
                lineChart.setZoomType(ZoomType.HORIZONTAL);
                lineChart.setZoomEnabled(true);
            } else {
                lineChart.setZoomEnabled(false);
            }
            lineChart.setLineChartData(lineSleepUi.getSleepTotalSleepLineData());
        }
    }
    private void showToast(final String message) {
        if(mToast == null) {
            mToast = Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT);
        } else {
            mToast.setText(message);
        }
        mToast.show();
    }
    private void showToast(final int message) {
        if(mToast == null) {
            mToast = Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT);
        } else {
            mToast.setText(message);
        }
        mToast.show();
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();

        initialUI();
    }
    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {

        } else {
            //相当于Fragment的onPause
        }
    }
    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    private class ValueTouchListener implements ColumnChartOnValueSelectListener {

        @Override
        public void onValueSelected(int columnIndex, int subcolumnIndex, SubcolumnValue value) {
            SleepSubData sleepSubData = sleepUi.getDetailData(columnIndex);
            int deep = 0, light = 0, awake = 0;
            if(sleepSubData != null) {
                deep = sleepSubData.getDeepSleepTime();
                light = sleepSubData.getLightSleepTime();
                awake = sleepSubData.getAwakeTimes();
            }
            String detail = String.format(mFormatDetailSleep
                    , deep / 60 , deep % 60
                    , light / 60 , light % 60
                    , awake);
            showToast(detail);
        }

        @Override
        public void onValueDeselected() {
            // TODO Auto-generated method stub

        }

    }

    private class LineValueTouchListener implements LineChartOnValueSelectListener {

        @Override
        public void onValueSelected(int pointIndex, int subpointIndex, PointValue value) {
            SleepSubData sleepSubData = lineSleepUi.getLinePointDetailData((int) (value.getX()));
            int deep = 0, light = 0, awake = 0;
            if(sleepSubData != null) {
                deep = sleepSubData.getDeepSleepTime();
                light = sleepSubData.getLightSleepTime();
                awake = sleepSubData.getAwakeTimes();
            }
            String detail = String.format(mFormatDetailSleep
                    , deep / 60 , deep % 60
                    , light / 60 , light % 60
                    , awake);
            showToast(detail);
        }

        @Override
        public void onValueDeselected() {
            // TODO Auto-generated method stub

        }

    }
}
