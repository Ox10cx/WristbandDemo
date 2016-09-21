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
import lecho.lib.hellocharts.model.SubcolumnValue;
import lecho.lib.hellocharts.model.ValueShape;
import lecho.lib.hellocharts.util.ChartUtils;

public class SportMonthColumnUiManager {
	private ArrayList<SportData> mSports;
	private HashMap<Integer, SportSubData> mMonthSportDataMap;// Start with first week in the month, it have 6 month
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

    private int mFirstSundayYear;
    private int mFirstSundayMonth;
    private int mFirstSundayDate;


    public SportMonthColumnUiManager(List<SportData> sports, int year, int month, int day) {
		Calendar c1 = Calendar.getInstance();
        // get the first day of the month.
        c1.set(year, month - 1, 1);// here need decrease 1 of month
        mMonthSportDataMap = new HashMap<Integer, SportSubData>();
        // get the first day of the week.
        int current = c1.get(Calendar.DAY_OF_WEEK);
        // get the current sunday in the week.
        c1.add(Calendar.DATE, -1 * (current - Calendar.SUNDAY));
        mFirstSundayMonth = c1.get(Calendar.MONTH) + 1;
        mFirstSundayYear = c1.get(Calendar.YEAR);
        mFirstSundayDate = c1.get(Calendar.DATE);

        Calendar sunday = Calendar.getInstance();
        sunday.set(mFirstSundayYear, mFirstSundayMonth - 1, mFirstSundayDate);// here need decrease 1 of month
        for(int i = 1; i <= 6; ++i) {
            SportSubData sumOfWeek = new SportSubData();
            // sum every week in the month
            for(int j = 1; j <= 7; ++j) {
                SportSubData subData = WristbandCalculator.sumOfSportDataByDate(sunday.get(Calendar.YEAR)
                        , sunday.get(Calendar.MONTH) + 1
                        , sunday.get(Calendar.DATE)
                        , sports);
                if (subData != null) {
                    Log.i("234", "SportWeekColumnUiManager, i: " + i
                            + ", subData: " + subData.toString()
                            + ", sunday: " + sunday.toString());
                    sumOfWeek.add(subData);
                }
                sunday.add(Calendar.DATE, 1);
            }

            mMonthSportDataMap.put(i, sumOfWeek);
        }
    }
    public SportSubData getDetailData(int week) {
        return mMonthSportDataMap.get(week + 1);
    }
    public int getSumStep() {
        int sum = 0;
        for (int i = 1; i <= 6; ++i) {
            if(mMonthSportDataMap.get(i) != null) {
                sum += mMonthSportDataMap.get(i).getStepCount();
            }
        }
        return sum;
    }

    public ColumnChartData getSportStepColumnData() {
        List<SubcolumnValue> values;
        List<Column> columns = new ArrayList<Column>();

        for (int i = 1; i <= 6; ++i) {
            values = new ArrayList<SubcolumnValue>();
            SubcolumnValue subcolumnValue;
            if(mMonthSportDataMap.get(i) == null) {
                subcolumnValue = new SubcolumnValue(0, ChartUtils.COLOR_BLUE);
            } else {
                subcolumnValue = new SubcolumnValue(mMonthSportDataMap.get(i).getStepCount(), ChartUtils.COLOR_BLUE);
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
                sunday.set(mFirstSundayYear, mFirstSundayMonth - 1, mFirstSundayDate);// here need decrease 1 of month
                // set display x label.
                for (int i = 0; i < 6 ; i++) {
                    int firstDay = sunday.get(Calendar.DATE);
                    // find the end of the week
                    sunday.add(Calendar.DATE, 6);
                    int secondDay = sunday.get(Calendar.DATE);
                    axisValues.add(new AxisValue(i).setLabel(firstDay
                            + "-"
                            + secondDay));
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
