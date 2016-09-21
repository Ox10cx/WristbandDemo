package com.realsil.android.wristbanddemo.sleep;

import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.realsil.android.wristbanddemo.R;
import com.realsil.android.wristbanddemo.constant.ConstantParam;
import com.realsil.android.wristbanddemo.greendao.SleepData;
import com.realsil.android.wristbanddemo.greendao.SportData;
import com.realsil.android.wristbanddemo.sport.SportLineUiManager;
import com.realsil.android.wristbanddemo.sport.SportSubData;
import com.realsil.android.wristbanddemo.utility.DensityUtils;
import com.realsil.android.wristbanddemo.utility.GlobalGreenDAO;
import com.realsil.android.wristbanddemo.utility.RefreshableLinearLayoutView;
import com.realsil.android.wristbanddemo.utility.SPWristbandConfigInfo;
import com.realsil.android.wristbanddemo.utility.WristbandCalculator;
import com.realsil.android.wristbanddemo.utility.WristbandManager;
import com.realsil.android.wristbanddemo.utility.WristbandManagerCallback;
import com.realsil.android.wristbanddemo.view.SleepChartView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import lecho.lib.hellocharts.gesture.ZoomType;
import lecho.lib.hellocharts.listener.LineChartOnValueSelectListener;
import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.PointValue;
import lecho.lib.hellocharts.view.LineChartView;

public class WristbandDetailDayFragmentSleep extends Fragment {
    // Log
    private final static String TAG = "WristbandDetailDayFragmentSleep";
    private final static boolean D = true;

    public static final String EXTRAS_DATE = "DATE";
    public static final String EXTRAS_DATE_YEAR = "DATE_YEAR";
    public static final String EXTRAS_DATE_MONTH = "DATE_MONTH";
    public static final String EXTRAS_DATE_DAY = "DATE_DAY";

    TextView mtvDayDetailHeader;
    TextView mtvDetailTotalSleep;
    TextView mtvDetailTotalDeep;

    TextView mtvDetailTotalLight;
    TextView mtvDetailAwake;
    TextView mtvDetailQuality;

    private SleepChartView chart;

    private ImageView mivDetailQuality;

    private LinearLayout mllDetailQuality;

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
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_sleep_day, container, false);

        // get global green dao instance
        mGlobalGreenDAO = GlobalGreenDAO.getInstance();

        mtvDayDetailHeader = (TextView) rootView.findViewById(R.id.tvDayDetailHeader);

        mtvDetailTotalSleep = (TextView) rootView.findViewById(R.id.tvDetailTotalSleep);
        mtvDetailTotalDeep = (TextView) rootView.findViewById(R.id.tvDetailTotalDeep);

        mtvDetailTotalLight = (TextView) rootView.findViewById(R.id.tvDetailTotalLight);
        mtvDetailAwake = (TextView) rootView.findViewById(R.id.tvDetailAwake);
        mtvDetailQuality = (TextView) rootView.findViewById(R.id.tvDetailQuality);

        mivDetailQuality = (ImageView) rootView.findViewById(R.id.ivDetailQuality);

        mllDetailQuality = (LinearLayout) rootView.findViewById(R.id.llDetailQuality);

        mYear = getArguments().getInt(EXTRAS_DATE_YEAR);
        mMonth = getArguments().getInt(EXTRAS_DATE_MONTH);
        mDay = getArguments().getInt(EXTRAS_DATE_DAY);

        scrollView = (ScrollView) rootView.findViewById(R.id.scrollView);
        chart = (SleepChartView) rootView.findViewById(R.id.lcvLineChart);
        /*
        mscvSleep.setChartLineColor(Color.parseColor("#5d740d"));//舒张压线的颜色值
        mscvSleep.setChartLeftPadding(DensityUtils.dip2px(context, 16));//设置Y轴文字宽度
        mscvSleep.setChartRightPadding(DensityUtils.dip2px(context, 16));//设置Y轴文字宽度
        mscvSleep.setyTextpaddingleft(DensityUtils.dip2px(context, 18));
        mscvSleep.setZOrderOnTop(true);
        mscvSleep.getHolder().setFormat(PixelFormat.TRANSPARENT);
        mscvSleep.setYMaxValue(5);
        mscvSleep.setYMinValue(0);*/

        chart.setChartLeftPadding(DensityUtils.dip2px(getContext(), 10));//设置左边界
        chart.setChartRightPadding(DensityUtils.dip2px(getContext(), 10));//设置右边界
        chart.setZOrderOnTop(true);
        chart.getHolder().setFormat(PixelFormat.TRANSPARENT);
        chart.setYMaxValue(SleepLineUiManagerSpec.SLEEP_MODE_DISPLAY_HIGHT_MAX);
        chart.setYMinValue(SleepLineUiManagerSpec.SLEEP_MODE_DISPLAY_HIGHT_NULL);
        chart.setBottomTextSize(DensityUtils.sp2px(getActivity(), 12));
        chart.setOnTapPointListener(new SleepChartView.OnTapPointListener() {
            @Override
            public void onTap(SleepChartView.SleepChartAxes axes) {
                Log.d(TAG, "axes: " + axes.toString());
                int sleepTime = (int)(axes.endX - axes.startX);
                String mode = "";
                if(axes.Y == SleepLineUiManagerSpec.SLEEP_MODE_DISPLAY_HIGHT_AWAKE) {
                    mode = getString(R.string.sleep_latency);
                } else if(axes.Y == SleepLineUiManagerSpec.SLEEP_MODE_DISPLAY_HIGHT_LIGHT) {
                    mode = getString(R.string.light_sleep);
                } else if(axes.Y == SleepLineUiManagerSpec.SLEEP_MODE_DISPLAY_HIGHT_DEEP) {
                    mode = getString(R.string.deep_sleep);
                }
                int startHour;
                if((int)axes.startX > (WristbandCalculator.MAX_MINUTE -WristbandCalculator.START_SLEEP_TIME_MINUTE)) {
                    startHour = ((int)axes.startX - (WristbandCalculator.MAX_MINUTE -WristbandCalculator.START_SLEEP_TIME_MINUTE)) / 60;
                } else {
                    startHour = ((int)axes.startX + WristbandCalculator.START_SLEEP_TIME_MINUTE) / 60;
                }
                int startMinute = (int)axes.startX % 60;
                String startHourStr = String.valueOf(startHour).length() == 1
                        ? "0" + String.valueOf(startHour)
                        : String.valueOf(startHour);
                String startMinuteStr = String.valueOf(startMinute).length() == 1
                        ? "0" + String.valueOf(startMinute)
                        : String.valueOf(startMinute);
                int endHour;
                if((int)axes.endX > (WristbandCalculator.MAX_MINUTE -WristbandCalculator.START_SLEEP_TIME_MINUTE)) {
                    endHour = ((int)axes.endX - (WristbandCalculator.MAX_MINUTE -WristbandCalculator.START_SLEEP_TIME_MINUTE)) / 60;
                } else {
                    endHour = ((int)axes.endX + WristbandCalculator.START_SLEEP_TIME_MINUTE) / 60;
                }
                int endMinute = (int)axes.endX % 60;
                String endHourStr = String.valueOf(endHour).length() == 1
                        ? "0" + String.valueOf(endHour)
                        : String.valueOf(endHour);
                String endMinuteStr = String.valueOf(endMinute).length() == 1
                        ? "0" + String.valueOf(endMinute)
                        : String.valueOf(endMinute);

                showToast(String.format(mFormatSelectSleep
                        , mode
                        , startHourStr, startMinuteStr
                        , endHourStr, endMinuteStr));
                //mFormatSelectSleep
            }
        });
        initialStringFormat();

        return rootView;
    }
    //private String mSportCurrentStepFormat;
    private String mFormatSleep;
    private String mFormatDeep;
    private String mFormatLight;
    private String mFormatAwake;
    private String mFormatDate;
    private String mFormatSelectSleep;

    private void initialStringFormat() {
        mFormatSleep = getResources().getString(R.string.total_hour_min);
        mFormatDeep = getResources().getString(R.string.hour_min);
        mFormatLight = getResources().getString(R.string.hour_min);
        mFormatAwake = getResources().getString(R.string.times_value);
        mFormatDate = getResources().getString(R.string.date_value);
        mFormatSelectSleep = getResources().getString(R.string.detail_sleep_select_value);
    }

    private void initialUI() {
        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DATE, -1);
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
        List<SleepData> sleeps = mGlobalGreenDAO.loadSleepDataByDateSpec(mYear, mMonth, mDay);

        SleepSubData subData = WristbandCalculator.sumOfSleepDataByDateSpecNoErrorCheck(mYear, mMonth, mDay, sleeps);

        if(subData != null
                && subData.getTotalSleepTime() > 0) {
            mtvDetailTotalSleep.setText(String.format(mFormatSleep
                    , subData.getTotalSleepTime() / 60, subData.getTotalSleepTime() % 60));

            mtvDetailTotalDeep.setText(String.format(mFormatDeep
                    , subData.getDeepSleepTime() / 60, subData.getDeepSleepTime() % 60));
            mtvDetailTotalLight.setText(String.format(mFormatLight
                    , subData.getLightSleepTime() / 60, subData.getLightSleepTime() % 60));
            mtvDetailAwake.setText(String.format(mFormatAwake
                    , subData.getAwakeTimes()));
            float persent = (float) (subData.getTotalSleepTime() * 100)
                    / (getTotalSleep() * 60);
            if(persent > 100) {
                persent = 100;
            }
            if (persent < SLEEP_QUALITY_0) {
                mtvDetailQuality.setText(getResources().getString(R.string.sleep_quality_0));
                mivDetailQuality.setImageResource(R.mipmap.day_icon_sleep_grade_bad);
            } else if (persent < SLEEP_QUALITY_1) {
                mtvDetailQuality.setText(getResources().getString(R.string.sleep_quality_1));
                mivDetailQuality.setImageResource(R.mipmap.day_icon_sleep_grade_normal);
            } else {
                mtvDetailQuality.setText(getResources().getString(R.string.sleep_quality_2));
                mivDetailQuality.setImageResource(R.mipmap.day_icon_sleep_grade_good);
            }

            SleepLineUiManagerSpec sleepUi = new SleepLineUiManagerSpec(mYear, mMonth, mDay, sleeps);
            sleepUi.setSleepModeDisplayColorDeep(getResources().getColor(R.color.sleep_deep));
            sleepUi.setSleepModeDisplayColorLight(getResources().getColor(R.color.sleep_light));
            sleepUi.setSleepModeDisplayColorAwake(getResources().getColor(R.color.sleep_active));

            chart.setData(sleepUi.getSpecialSleepNewUIDataNoErrorCheck());
            chart.startChart();
        } else {
            mtvDetailTotalSleep.setText(String.format(mFormatSleep, 0, 0));
            mtvDetailTotalDeep.setText(String.format(mFormatDeep, 0, 0));

            mtvDetailTotalLight.setText(String.format(mFormatLight, 0, 0));
            mtvDetailAwake.setText(String.format(mFormatAwake, 0));

            mtvDetailQuality.setText(getResources().getString(R.string.no_sleep_data));
            mivDetailQuality.setImageResource(R.mipmap.day_icon_no_data);
        }


        // Use to show some case, sleep may have error data, just for debug
        if(ConstantParam.isInDebugMode()) {
            if(subData != null) {
                // Use error check method
                if(WristbandCalculator.sumOfSleepDataByDateSpec(mYear, mMonth, mDay, sleeps) == null) {
                    mtvDetailQuality.setText("有异常");
                    mtvDetailQuality.setTextColor(Color.RED);

                    mivDetailQuality.setImageResource(R.mipmap.day_icon_no_data);
                    mllDetailQuality.setVisibility(View.VISIBLE);
                }
            }
        }
    }
    private int getTotalSleep() {
        int age = SPWristbandConfigInfo.getAge(getContext());
        int totalSleep;
        if(age < 10) {
            totalSleep = 13;
        } else if(age < 18) {
            totalSleep = 12;
        } else if(age < 59) {
            totalSleep = 10;
        } else {
            totalSleep = 8;
        }
        if(D) Log.d(TAG, "getTotalSleep, age: " + age + ", totalSleep: " + totalSleep);
        return totalSleep;
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
            showToast(String.valueOf(value.getY()));
        }

        @Override
        public void onValueDeselected() {
            // TODO Auto-generated method stub

        }

    }
}
