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
import com.realsil.android.wristbanddemo.utility.WristbandCalculator;
import com.realsil.android.wristbanddemo.utility.WristbandManager;
import com.realsil.android.wristbanddemo.utility.WristbandManagerCallback;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import lecho.lib.hellocharts.gesture.ZoomType;
import lecho.lib.hellocharts.listener.ColumnChartOnValueSelectListener;
import lecho.lib.hellocharts.listener.LineChartOnValueSelectListener;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.PointValue;
import lecho.lib.hellocharts.model.SubcolumnValue;
import lecho.lib.hellocharts.view.ColumnChartView;
import lecho.lib.hellocharts.view.LineChartView;

public class WristbandDetailWeekFragmentSport extends Fragment {
    // Log
    private final static String TAG = "WristbandDetailWeekFragmentSport";
    private final static boolean D = true;

    public static final String EXTRAS_DATE = "DATE";
    public static final String EXTRAS_DATE_YEAR = "DATE_YEAR";
    public static final String EXTRAS_DATE_MONTH = "DATE_MONTH";
    public static final String EXTRAS_DATE_DAY = "DATE_DAY";

    private static final int TYPE_LINE = 0;
    private static final int TYPE_COLUMN = 1;
    private int mType = TYPE_LINE;

    TextView mtvWeekDetailHeader;
    TextView mtvWeekTotalStep;

    private ColumnChartView chart;
    private LineChartView lineChart;

    private ScrollView scrollView;

    private GlobalGreenDAO mGlobalGreenDAO;

    private final double STEP_QUALITY_0 = 20;
    private final double STEP_QUALITY_1 = 50;
    private final double STEP_QUALITY_2 = 80;
    private final double STEP_QUALITY_3 = 100;

    private Calendar mCalendar;
    private int mYear;
    private int mMonth;
    private int mDay;

    private Toast mToast;

    private SportWeekColumnUiManager sportUi;
    private SportWeekLineUiManager lineSportUi;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = null;
        if(mType == TYPE_COLUMN) {
            rootView = inflater.inflate(R.layout.fragment_step_week, container, false);
        } else {
            rootView = inflater.inflate(R.layout.fragment_step_week_line, container, false);
        }

        // get global green dao instance
        mGlobalGreenDAO = GlobalGreenDAO.getInstance();

        mtvWeekDetailHeader = (TextView) rootView.findViewById(R.id.tvWeekDetailHeader);

        mtvWeekTotalStep = (TextView) rootView.findViewById(R.id.tvDetailTotalStep);
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
        //mtvWeekDetailHeader.setText(mYear + "/" + mMonth + "/" + mDay);
        mtvWeekDetailHeader.setText(String.valueOf(mYear));
        // Judge need of not to load two month
        Calendar c1 = Calendar.getInstance();
        c1.set(mYear, mMonth - 1, mDay);// here need decrease 1 of month
        int current = c1.get(Calendar.DAY_OF_WEEK);
        Log.d(TAG, "current: " + current + ", c1: " + c1.toString());

        // get the current month of sunday in the week.
        c1.add(Calendar.DATE, -1 * (current - Calendar.SUNDAY));
        int firstMonth = c1.get(Calendar.MONTH) + 1;
        int firstYear = c1.get(Calendar.YEAR);
        int firstDay = c1.get(Calendar.DATE);
        Log.d(TAG, "firstYear: " + firstYear + ", firstMonth:" + firstMonth + ", c1: " + c1.toString());

        // get the current month of saturday in the week.
        c1.add(Calendar.DATE, Calendar.SATURDAY - Calendar.SUNDAY);
        int secondMonth = c1.get(Calendar.MONTH) + 1;
        int secondYear = c1.get(Calendar.YEAR);
        Log.d(TAG, "secondYear: " + secondYear + ", secondMonth:" + secondMonth + ", c1: " + c1.toString());

        List<SportData> sports = new ArrayList<SportData>();
        if(firstMonth == secondMonth) {
            sports = mGlobalGreenDAO.loadSportDataByDate(mYear, mMonth);
        } else {
            List<SportData> s1 = mGlobalGreenDAO.loadSportDataByDate(firstYear, firstMonth);
            for(SportData sp: s1) {
                sports.add(sp);
            }
            List<SportData> s2 = mGlobalGreenDAO.loadSportDataByDate(secondYear, secondMonth);
            for(SportData sp: s2) {
                sports.add(sp);
            }
        }
        //Log.i(TAG, "display data: " + WristbandCalculator.toString(sports));
        /*
        SportSubData subWeekData = new SportSubData();
        Calendar sunday = Calendar.getInstance();
        sunday.set(firstYear, firstMonth - 1, firstDay);// here need decrease 1 of month

        for(int i = 0; i < 7; i ++) {
            //Calendar calendar = sunday;

            SportSubData subData =  WristbandCalculator.sumOfSportDataByDate(sunday.get(Calendar.YEAR)
                    , sunday.get(Calendar.MONTH) + 1
                    , sunday.get(Calendar.DATE)
                    , sports);
            if(subData != null) {
                Log.i("initialUI", "SportWeekColumnUiManager, i: " + i
                        + ", subData: " + subData.toString()
                        + ", sunday: " + sunday.toString()
                        + ", c1: " + c1.toString());
                subWeekData.setStepCount(subWeekData.getStepCount() + subData.getStepCount());
            }
            sunday.add(Calendar.DATE, 1);
        }*/


        if(mType == TYPE_COLUMN) {
            sportUi = new SportWeekColumnUiManager(sports, mYear, mMonth, mDay);
            mtvWeekTotalStep.setText(String.format(mFormatSportTotalStep, sportUi.getSumStep()));

            chart.setZoomEnabled(false);
            chart.setColumnChartData(sportUi.getSportStepColumnData());
        } else {
            lineSportUi = new SportWeekLineUiManager(sports, mYear, mMonth, mDay);
            mtvWeekTotalStep.setText(String.format(mFormatSportTotalStep, lineSportUi.getSumStep()));

            if(lineSportUi.getSumStep() != 0) {
                lineChart.setZoomType(ZoomType.HORIZONTAL);
                lineChart.setZoomEnabled(true);
            } else {
                lineChart.setZoomEnabled(false);
            }
            lineChart.setLineChartData(lineSportUi.getSportStepLineData());
        }
    }

    // Application Layer callback
    WristbandManagerCallback mWristbandManagerCallback = new WristbandManagerCallback() {

    };
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
            if(D) Log.e(TAG, "columnIndex: " + columnIndex + ", subcolumnIndex: " + subcolumnIndex);
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
