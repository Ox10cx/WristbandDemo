package com.realsil.android.wristbanddemo.sport;

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
import com.realsil.android.wristbanddemo.greendao.SportData;
import com.realsil.android.wristbanddemo.utility.GlobalGreenDAO;
import com.realsil.android.wristbanddemo.utility.RefreshableLinearLayoutView;
import com.realsil.android.wristbanddemo.utility.WristbandManager;
import com.realsil.android.wristbanddemo.utility.WristbandManagerCallback;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import lecho.lib.hellocharts.gesture.ZoomType;
import lecho.lib.hellocharts.listener.ColumnChartOnValueSelectListener;
import lecho.lib.hellocharts.listener.LineChartOnValueSelectListener;
import lecho.lib.hellocharts.model.PointValue;
import lecho.lib.hellocharts.model.SubcolumnValue;
import lecho.lib.hellocharts.view.ColumnChartView;
import lecho.lib.hellocharts.view.LineChartView;

public class WristbandDetailMonthFragmentSport extends Fragment {
    // Log
    private final static String TAG = "WristbandDetailMonthFragmentSport";
    private final static boolean D = true;

    public static final String EXTRAS_DATE = "DATE";
    public static final String EXTRAS_DATE_YEAR = "DATE_YEAR";
    public static final String EXTRAS_DATE_MONTH = "DATE_MONTH";
    public static final String EXTRAS_DATE_DAY = "DATE_DAY";

    private static final int TYPE_LINE = 0;
    private static final int TYPE_COLUMN = 1;
    private int mType = TYPE_LINE;

    TextView mtvMonthDetailHeader;
    TextView mtvMonthTotalStep;

    private ColumnChartView chart;
    private LineChartView lineChart;

    private ScrollView scrollView;

    private GlobalGreenDAO mGlobalGreenDAO;

    private final double STEP_QUALITY_0 = 20;
    private final double STEP_QUALITY_1 = 50;
    private final double STEP_QUALITY_2 = 80;
    private final double STEP_QUALITY_3 = 100;

    RefreshableLinearLayoutView refreshableView;

    private Calendar mCalendar;
    private int mYear;
    private int mMonth;
    private int mDay;

    private Toast mToast;
    private SportMonthColumnUiManager sportUi;
    private SportMonthLineUiManager lineSportUi;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = null;
        if(mType == TYPE_COLUMN) {
            rootView = inflater.inflate(R.layout.fragment_step_month, container, false);
        } else {
            rootView = inflater.inflate(R.layout.fragment_step_month_line, container, false);
        }

        // get global green dao instance
        mGlobalGreenDAO = GlobalGreenDAO.getInstance();

        mtvMonthDetailHeader = (TextView) rootView.findViewById(R.id.tvMonthDetailHeader);

        mtvMonthTotalStep = (TextView) rootView.findViewById(R.id.tvDetailTotalStep);
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
    private String mFormatSportTotalStep;
    private String mFormatDetailSport;
    //private String mSportCurrentQuality;

    private void initialStringFormat() {
        mFormatSportTotalStep = getResources().getString(R.string.total_step_value);
        mFormatDetailSport = getResources().getString(R.string.detail_sport_value);
    }

    private void initialUI() {
        mtvMonthDetailHeader.setText(mYear + "/" + mMonth);
        //mtvMonthDetailHeader.setText(String.valueOf(mYear));
        Calendar c1 = Calendar.getInstance();
        // get the first day of the week.
        c1.set(mYear, mMonth - 1, 1);// here need decrease 1 of month
        int firstCurrentMonth = c1.get(Calendar.MONTH) + 1;
        int firstCurrentYear = c1.get(Calendar.YEAR);
        Log.d(TAG, "firstCurrentYear: " + firstCurrentYear + ", firstCurrentMonth:" + firstCurrentMonth + ", c1: " + c1.toString());

        // get the first day of the week.
        int current = c1.get(Calendar.DAY_OF_WEEK);
        // get the first sunday in the month.
        c1.add(Calendar.DATE, -1 * (current - Calendar.SUNDAY));
        int secondSundayMonth = c1.get(Calendar.MONTH) + 1;
        int secondSundayYear = c1.get(Calendar.YEAR);
        Log.d(TAG, "secondSundayYear: " + secondSundayYear + ", secondSundayMonth:" + secondSundayMonth + ", c1: " + c1.toString());

        // get the last saturday in the month. here we will load six month day.
        c1.add(Calendar.DATE, (7 * 6) - 1);
        int thirdSaturdayMonth = c1.get(Calendar.MONTH) + 1;
        int thirdSaturdayYear = c1.get(Calendar.YEAR);
        // get the current month of saturday in the Month.
        c1.add(Calendar.DATE, Calendar.SATURDAY - Calendar.SUNDAY);
        Log.d(TAG, "thirdSaturdayYear: " + thirdSaturdayYear + ", thirdSaturdayMonth:" + thirdSaturdayMonth + ", c1: " + c1.toString());

        List<SportData> sports = new ArrayList<SportData>();
        if(firstCurrentMonth == secondSundayMonth
                && firstCurrentMonth == thirdSaturdayYear) {
            sports = mGlobalGreenDAO.loadSportDataByDate(mYear, mMonth);
        } else if(firstCurrentMonth != secondSundayMonth) { // need load three month data
            List<SportData> s1 = mGlobalGreenDAO.loadSportDataByDate(firstCurrentYear, firstCurrentMonth);
            for(SportData sp: s1) {
                sports.add(sp);
            }
            List<SportData> s2 = mGlobalGreenDAO.loadSportDataByDate(secondSundayYear, secondSundayMonth);
            for(SportData sp: s2) {
                sports.add(sp);
            }
            List<SportData> s3 = mGlobalGreenDAO.loadSportDataByDate(thirdSaturdayYear, thirdSaturdayMonth);
            for(SportData sp: s3) {
                sports.add(sp);
            }
        } else { // need load two month data
            List<SportData> s1 = mGlobalGreenDAO.loadSportDataByDate(firstCurrentYear, firstCurrentMonth);
            for(SportData sp: s1) {
                sports.add(sp);
            }
            List<SportData> s2 = mGlobalGreenDAO.loadSportDataByDate(thirdSaturdayYear, thirdSaturdayMonth);
            for(SportData sp: s2) {
                sports.add(sp);
            }
        }


        if(mType == TYPE_COLUMN) {
            sportUi = new SportMonthColumnUiManager(sports, mYear, mMonth, mDay);
            mtvMonthTotalStep.setText(String.format(mFormatSportTotalStep, sportUi.getSumStep()));

            chart.setZoomEnabled(false);
            chart.setColumnChartData(sportUi.getSportStepColumnData());
        } else {
            lineSportUi = new SportMonthLineUiManager(sports, mYear, mMonth, mDay);
            mtvMonthTotalStep.setText(String.format(mFormatSportTotalStep, lineSportUi.getSumStep()));

            if(lineSportUi.getSumStep() != 0) {
                lineChart.setZoomType(ZoomType.HORIZONTAL);
                lineChart.setZoomEnabled(true);
            } else {
                lineChart.setZoomEnabled(false);
            }
            lineChart.setLineChartData(lineSportUi.getSportStepLineData());
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
            SportSubData sportSubData = sportUi.getDetailData(columnIndex);
            int distance = 0, calorie = 0, step = 0;
            if(sportSubData != null) {
                distance = sportSubData.getDistance();
                calorie = sportSubData.getCalory();
                step = sportSubData.getStepCount();
            }
            String detail = String.format(mFormatDetailSport
                    , (float) distance / 1000, (float) calorie / 1000, step);
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
            SportSubData sportSubData = lineSportUi.getLinePointDetailData((int) (value.getX()));
            int distance = 0, calorie = 0, step = 0;
            if(sportSubData != null) {
                distance = sportSubData.getDistance();
                calorie = sportSubData.getCalory();
                step = sportSubData.getStepCount();
            }
            String detail = String.format(mFormatDetailSport
                    , (float) distance / 1000, (float) calorie / 1000, step);
            showToast(detail);
        }

        @Override
        public void onValueDeselected() {
            // TODO Auto-generated method stub

        }

    }
}
