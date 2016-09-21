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
import com.realsil.android.wristbanddemo.utility.SPWristbandConfigInfo;
import com.realsil.android.wristbanddemo.utility.WristbandCalculator;

import java.util.Calendar;
import java.util.List;

import lecho.lib.hellocharts.gesture.ZoomType;
import lecho.lib.hellocharts.listener.LineChartOnValueSelectListener;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.PointValue;
import lecho.lib.hellocharts.view.LineChartView;

public class WristbandDetailDayFragmentSport extends Fragment {
    // Log
    private final static String TAG = "WristbandDetailDayFragmentSport";
    private final static boolean D = true;

    public static final String EXTRAS_DATE = "DATE";
    public static final String EXTRAS_DATE_YEAR = "DATE_YEAR";
    public static final String EXTRAS_DATE_MONTH = "DATE_MONTH";
    public static final String EXTRAS_DATE_DAY = "DATE_DAY";

    TextView mtvDayDetailHeader;
    TextView mtvDetailTotalStep;
    TextView mtvDetailGoal;

    TextView mtvDetailTotalDistance;
    TextView mtvDetailTotalCalorie;
    TextView mtvDetailQuality;

    private LineChartView chart;

    private ScrollView scrollView;

    private GlobalGreenDAO mGlobalGreenDAO;

    private final double STEP_QUALITY_0 = 60;
    private final double STEP_QUALITY_1 = 70;
    private final double STEP_QUALITY_2 = 80;
    private final double STEP_QUALITY_3 = 100;

    private int mYear;
    private int mMonth;
    private int mDay;

    private Toast mToast;

    SportLineUiManager sportUi;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_step_day, container, false);

        // get global green dao instance
        mGlobalGreenDAO = GlobalGreenDAO.getInstance();

        mtvDayDetailHeader = (TextView) rootView.findViewById(R.id.tvDayDetailHeader);

        mtvDetailTotalStep = (TextView) rootView.findViewById(R.id.tvDetailTotalStep);
        mtvDetailGoal = (TextView) rootView.findViewById(R.id.tvDetailGoal);

        mtvDetailTotalDistance = (TextView) rootView.findViewById(R.id.tvDetailTotalDistance);
        mtvDetailTotalCalorie = (TextView) rootView.findViewById(R.id.tvDetailTotalCalorie);
        mtvDetailQuality = (TextView) rootView.findViewById(R.id.tvDetailQuality);

        mYear = getArguments().getInt(EXTRAS_DATE_YEAR);
        mMonth = getArguments().getInt(EXTRAS_DATE_MONTH);
        mDay = getArguments().getInt(EXTRAS_DATE_DAY);

        scrollView = (ScrollView) rootView.findViewById(R.id.scrollView);
        chart = (LineChartView) rootView.findViewById(R.id.lcvLineChart);
        chart.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if(chart.getZoomLevel() == 1){
                    scrollView.requestDisallowInterceptTouchEvent(false);
                }else{
                    scrollView.requestDisallowInterceptTouchEvent(true);
                }
                //if(D) Log.d(TAG, "chart.getZoomLevel(): " + chart.getZoomLevel());
                return false;
            }
        });
        chart.setOnValueTouchListener(new ValueTouchListener());
        initialStringFormat();

        return rootView;
    }
    //private String mSportCurrentStepFormat;
    private String mFormatSportTotalStep;
    private String mFormatSportGoal;
    private String mFormatSportCurrentDistance;
    private String mFormatSportCurrentCalorie;
    private String mFormatDate;
    private String mFormatDetailSport;
    //private String mSportCurrentQuality;

    private void initialStringFormat() {
        mFormatSportTotalStep = getResources().getString(R.string.total_step_value);
        mFormatSportGoal = getResources().getString(R.string.goal_percent);
        mFormatSportCurrentDistance = getResources().getString(R.string.distance_value);
        mFormatSportCurrentCalorie = getResources().getString(R.string.calorie_value);
        mFormatDate = getResources().getString(R.string.date_value);
        mFormatDetailSport = getResources().getString(R.string.detail_sport_value);
    }

    private void initialUI() {
        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DATE, -1);
        if(D) Log.e(TAG, "year: " + mYear
                + ", month: " + mMonth
                + ", day: " + mDay
                + ", yesterday: " + yesterday.toString());
        if(Calendar.getInstance().get(Calendar.YEAR) == mYear
                && Calendar.getInstance().get(Calendar.MONTH) == (mMonth - 1)
                && Calendar.getInstance().get(Calendar.DATE) == mDay) {
            mtvDayDetailHeader.setText(getResources().getString(R.string.today));
        } else if(yesterday.get(Calendar.YEAR) == mYear
                && yesterday.get(Calendar.MONTH) == (mMonth - 1)
                && yesterday.get(Calendar.DATE) == mDay){
            mtvDayDetailHeader.setText(getResources().getString(R.string.yesterday));
        } else {
            mtvDayDetailHeader.setText(String.format(mFormatDate,
                    mYear,
                    mMonth,// here need add 1, because it origin range is 0 - 11;
                    mDay));
        }
        List<SportData> sports = mGlobalGreenDAO.loadSportDataByDate(mYear, mMonth, mDay);
        /*
        List<SportData> sports1 = mGlobalGreenDAO.loadAllSportData();
        for(SportData sp: sports1) {
            if (D) Log.d(TAG, WristbandCalculator.toString(sp));
        }*/
        SportSubData subData = WristbandCalculator.sumOfSportDataByDate(mYear, mMonth, mDay, sports);

        if(subData != null) {
            mtvDetailTotalStep.setText(String.format(mFormatSportTotalStep, subData.getStepCount()));

            mtvDetailTotalDistance.setText(String.format(mFormatSportCurrentDistance, (float) subData.getDistance() / 1000));
            mtvDetailTotalCalorie.setText(String.format(mFormatSportCurrentCalorie, (float) subData.getCalory() / 1000));

            int total = SPWristbandConfigInfo.getTotalStep(getContext());
            float persent = (float) (subData.getStepCount() * 100) / total;
            mtvDetailGoal.setText(String.format(mFormatSportGoal, persent));
            if(persent > 100) {
                persent = 100;
            }
            if (persent < STEP_QUALITY_0) {
                mtvDetailQuality.setText(getResources().getString(R.string.step_quality_0));
            } else if (persent < STEP_QUALITY_1) {
                mtvDetailQuality.setText(getResources().getString(R.string.step_quality_1));
            } else if (persent < STEP_QUALITY_2) {
                mtvDetailQuality.setText(getResources().getString(R.string.step_quality_2));
            } else {
                mtvDetailQuality.setText(getResources().getString(R.string.step_quality_3));
            }

            sportUi = new SportLineUiManager(sports);
            chart.setZoomType(ZoomType.HORIZONTAL);
            chart.setZoomEnabled(true);
            chart.setLineChartData(sportUi.getSportStepLineData());
            //chart.setLineChartData(sportUi.getSportStepGainLineData(7000));
        } else {
            mtvDetailTotalStep.setText(String.format(mFormatSportTotalStep, 0));

            mtvDetailTotalDistance.setText(String.format(mFormatSportCurrentDistance, 0.0));
            mtvDetailTotalCalorie.setText(String.format(mFormatSportCurrentCalorie, 0.0));

            mtvDetailGoal.setText(String.format(mFormatSportGoal, 0.0));
            mtvDetailQuality.setText(getResources().getString(R.string.step_quality_0));
            chart.setZoomEnabled(false);
            chart.setLineChartData(SportLineUiManager.getEmptySportStepLineData());
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

    private class ValueTouchListener implements LineChartOnValueSelectListener {

        @Override
        public void onValueSelected(int pointIndex, int subpointIndex, PointValue value) {
            //showToast(String.valueOf(value.getY()));
            SportSubData sportSubData = sportUi.getLinePointDetailData((int) (value.getX()));
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
