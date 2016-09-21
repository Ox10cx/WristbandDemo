package com.realsil.android.wristbanddemo.sport;

import android.util.Log;

import com.realsil.android.wristbanddemo.greendao.SportData;
import com.realsil.android.wristbanddemo.utility.WristbandCalculator;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.AxisValue;
import lecho.lib.hellocharts.model.Column;
import lecho.lib.hellocharts.model.ColumnChartData;
import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.PointValue;
import lecho.lib.hellocharts.model.SubcolumnValue;
import lecho.lib.hellocharts.model.ValueShape;
import lecho.lib.hellocharts.util.ChartUtils;

public class SportWeekColumnUiManager {
	private ArrayList<SportData> mSports;
	private HashMap<Integer, SportSubData> mWeekSportDataMap;// Start with sunday
	private boolean hasAxes = true;
    private boolean hasAxesNames = true;
    private boolean hasAxesLabelNames = false;
    private boolean hasLines = true;
    private boolean hasPoints = true;
    private ValueShape shape = ValueShape.CIRCLE;
    private boolean isFilled = false;
    private boolean hasLabels = true;
    private boolean isCubic = false;
    private boolean hasLabelForSelected = false;
    private boolean pointsHaveDifferentColor;

    private int mSundayYear;
    private int mSundayMonth;
    private int mSundayDate;


    public SportWeekColumnUiManager(List<SportData> sports, int year, int month, int day) {
		Calendar c1 = Calendar.getInstance();
        c1.set(year, month - 1, day);// here need decrease 1 of month
        mWeekSportDataMap = new HashMap<Integer, SportSubData>();
        // get the current day of week.
        int current = c1.get(Calendar.DAY_OF_WEEK);
        // get the current month of sunday in the week.
        c1.add(Calendar.DATE, -1 * (current - Calendar.SUNDAY));
        mSundayMonth = c1.get(Calendar.MONTH) + 1;
        mSundayYear = c1.get(Calendar.YEAR);
        mSundayDate = c1.get(Calendar.DATE);

        Calendar sunday = Calendar.getInstance();
        sunday.set(mSundayYear, mSundayMonth - 1, mSundayDate);// here need decrease 1 of month
        for(int i = 1; i <= 7; ++i) {
            Log.i("123", "SportWeekColumnUiManager, i: " + i
                    + ", sunday: " + sunday.toString()
                    + ", year: " + year
                    + ", month: " + month
                    + ", day: " + day);
            SportSubData subData =  WristbandCalculator.sumOfSportDataByDate(sunday.get(Calendar.YEAR)
                    , sunday.get(Calendar.MONTH) + 1
                    , sunday.get(Calendar.DATE)
                    , sports);
            if(subData != null) {
                Log.i("234", "SportWeekColumnUiManager, i: " + i
                        + ", subData: " + subData.toString()
                        + ", sunday: " + sunday.toString());
                mWeekSportDataMap.put(i, subData);
            }
            sunday.add(Calendar.DATE, 1);
        }
    }
    public SportSubData getDetailData(int dayOfWeek) {
        return mWeekSportDataMap.get(dayOfWeek + 1);
    }
    public int getSumStep() {
        int sum = 0;
        for (int i = 1; i <= 7; ++i) {
            if(mWeekSportDataMap.get(i) != null) {
                sum += mWeekSportDataMap.get(i).getStepCount();
            }
        }
        return sum;
    }

    public ColumnChartData getSportStepColumnData() {
        List<SubcolumnValue> values;
        List<Column> columns = new ArrayList<Column>();

        for (int i = 1; i <= 7; ++i) {
            values = new ArrayList<SubcolumnValue>();
            SubcolumnValue subcolumnValue;
            if(mWeekSportDataMap.get(i) == null) {
                subcolumnValue = new SubcolumnValue(0, ChartUtils.COLOR_BLUE);
            } else {
                subcolumnValue = new SubcolumnValue(mWeekSportDataMap.get(i).getStepCount(), ChartUtils.COLOR_BLUE);
            }
            values.add(subcolumnValue);


            Column column = new Column(values);
            column.setHasLabels(hasLabels);
            column.setHasLabelsOnlyForSelected(hasLabelForSelected);

            columns.add(column);
        }

        ColumnChartData data = new ColumnChartData(columns);

        if (hasAxes) {
            Axis axisX = new Axis();
            Axis axisY = new Axis().setHasLines(true);
            if (hasAxesNames) {
                if(hasAxesLabelNames) {
                    axisX.setName("Time");
                    axisY.setName("Step Count");
                }

                List<AxisValue> axisValues = new ArrayList<AxisValue>();

                Calendar sunday = Calendar.getInstance();
                sunday.set(mSundayYear, mSundayMonth - 1, mSundayDate);// here need decrease 1 of month
                // set display x label.
                for (int i = 0; i < 7 ; i++) {
                    axisValues.add(new AxisValue(i).setLabel((sunday.get(Calendar.MONTH) + 1)
                            + "/"
                            + sunday.get(Calendar.DATE)));
                    sunday.add(Calendar.DATE, 1);
                }

                axisX.setValues(axisValues);

            }

            data.setAxisXBottom(axisX);
            data.setAxisYLeft(axisY);
        } else {
            data.setAxisXBottom(null);
            data.setAxisYLeft(null);
        }

        return data;
    }

}
