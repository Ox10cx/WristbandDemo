package com.realsil.android.wristbanddemo.sleep;

import android.util.Log;

import com.realsil.android.wristbanddemo.greendao.SleepData;
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

public class SleepMonthColumnUiManager {
	private ArrayList<SleepData> mSleeps;
	private HashMap<Integer, SleepSubData> mMonthSleepDataMap;// Start with first week in the month, it have 6 month
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


    public SleepMonthColumnUiManager(List<SleepData> sleeps, int year, int month, int day) {
		Calendar c1 = Calendar.getInstance();
        // get the first day of the month.
        c1.set(year, month - 1, 1);// here need decrease 1 of month
        mMonthSleepDataMap = new HashMap<Integer, SleepSubData>();
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
            SleepSubData sumOfWeek = new SleepSubData();
            // sum every week in the month
            for(int j = 1; j <= 7; ++j) {
                SleepSubData subData = WristbandCalculator.sumOfSleepDataByDateSpecNoErrorCheck(sunday.get(Calendar.YEAR)
                        , sunday.get(Calendar.MONTH) + 1
                        , sunday.get(Calendar.DATE)
                        , sleeps);
                if (subData != null) {
                    Log.i("234", "SleepWeekColumnUiManager, i: " + i
                            + ", subData: " + subData.toString()
                            + ", sunday: " + sunday.toString());
                    sumOfWeek.add(subData);
                }
                sunday.add(Calendar.DATE, 1);
            }

            mMonthSleepDataMap.put(i, sumOfWeek);
        }
    }
    public SleepSubData getDetailData(int week) {
        return mMonthSleepDataMap.get(week + 1);
    }
    public int getSumSleep() {
        int sum = 0;
        for (int i = 1; i <= 6; ++i) {
            if(mMonthSleepDataMap.get(i) != null) {
                sum += mMonthSleepDataMap.get(i).getTotalSleepTime();
            }
        }
        return sum;
    }

    public ColumnChartData getSleepTotalSleepColumnData() {
        List<SubcolumnValue> values;
        List<Column> columns = new ArrayList<Column>();

        for (int i = 1; i <= 6; ++i) {
            values = new ArrayList<SubcolumnValue>();
            SubcolumnValue subcolumnValue;
            if(mMonthSleepDataMap.get(i) == null) {
                subcolumnValue = new SubcolumnValue(0, ChartUtils.COLOR_VIOLET);
            } else {
                subcolumnValue = new SubcolumnValue(mMonthSleepDataMap.get(i).getTotalSleepTime(), ChartUtils.COLOR_VIOLET);
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
