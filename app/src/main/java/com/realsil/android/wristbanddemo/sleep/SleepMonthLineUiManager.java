package com.realsil.android.wristbanddemo.sleep;

import android.graphics.Color;
import android.util.Log;

import com.realsil.android.wristbanddemo.greendao.SleepData;
import com.realsil.android.wristbanddemo.greendao.SportData;
import com.realsil.android.wristbanddemo.sport.SportSubData;
import com.realsil.android.wristbanddemo.utility.WristbandCalculator;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.AxisValue;
import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.PointValue;
import lecho.lib.hellocharts.model.ValueShape;
import lecho.lib.hellocharts.util.ChartUtils;

public class SleepMonthLineUiManager {
	private ArrayList<SleepData> mSleeps;
	private HashMap<Integer, SleepSubData> mMonthSleepDataMap;// Start with first week in the month, it have 6 month
	private boolean hasAxes = true;
    private boolean hasAxesNames = true;
    private boolean hasAxesLabelNames = false;
    private boolean hasLines = true;
    private boolean hasPoints = true;
    private ValueShape shape = ValueShape.CIRCLE;
    private boolean isFilled = true;
    private boolean hasLabels = false;
    private boolean isCubic = false;
    private boolean hasLabelForSelected = false;
    private boolean pointsHaveDifferentColor;

    private int mFirstSundayYear;
    private int mFirstSundayMonth;
    private int mFirstSundayDate;

    private int mSelectYear;
    private int mSelectMonth;

    private int mMaxDayInMonth;

    public SleepMonthLineUiManager(List<SleepData> sleeps, int year, int month, int day) {
		Calendar c1 = Calendar.getInstance();
        // get the first day of the month.
        c1.set(year, month - 1, 1);// here need decrease 1 of month
        mMonthSleepDataMap = new HashMap<Integer, SleepSubData>();

        mSelectYear = year;
        mSelectMonth = month;

        mMaxDayInMonth = WristbandCalculator.getMonthMaxDays(year, month);

        for(int i = 1; i <= mMaxDayInMonth; ++i) {
            SleepSubData sumOfDay = new SleepSubData();
            // sum every day in the month
            SleepSubData subData = WristbandCalculator.sumOfSleepDataByDateSpecNoErrorCheck(year
                    , month
                    , i
                    , sleeps);
            if (subData != null) {
                Log.i("234", "SleepMonthLineUiManager, i: " + i
                        + ", subData: " + subData.toString());
                sumOfDay.add(subData);
            }

            mMonthSleepDataMap.put(i, sumOfDay);
        }
    }
    public SleepSubData getLinePointDetailData(int day) {
        return mMonthSleepDataMap.get(day);
    }
    public int getSumSleep() {
        int sum = 0;
        for (int i = 1; i <= mMaxDayInMonth; ++i) {
            if(mMonthSleepDataMap.get(i) != null) {
                sum += mMonthSleepDataMap.get(i).getTotalSleepTime();
            }
        }
        return sum;
    }

    public LineChartData getSleepTotalSleepLineData() {
        List<PointValue> values = new ArrayList<PointValue>();

        for (int i = 1; i <= mMaxDayInMonth; ++i) {
            if(mMonthSleepDataMap.get(i) == null) {
                values.add(new PointValue(i, 0));
            } else {
                values.add(new PointValue(i, mMonthSleepDataMap.get(i).getTotalSleepTime()));
            }
        }
        Line line = new Line(values);
        line.setColor(ChartUtils.COLOR_VIOLET);
        //line.setColor(Color.parseColor("#ff36bcf6"));
        //line.setColor(R.color.step_line_color);
        line.setShape(shape);
        line.setCubic(isCubic);
        line.setFilled(isFilled);
        line.setHasLabels(hasLabels);
        line.setHasLabelsOnlyForSelected(hasLabelForSelected);
        line.setHasLines(hasLines);
        line.setHasPoints(hasPoints);
        line.setPointRadius(4);

        ArrayList<Line> lines = new ArrayList<Line>();
        lines.add(line);

        LineChartData data = new LineChartData(lines);
        if (hasAxes) {
            Axis axisX = new Axis().setHasLines(true);
            Axis axisY = new Axis();
            if (hasAxesNames) {
                if(hasAxesLabelNames) {
                    axisX.setName("Time");
                    axisY.setName("Step Count");
                }
                List<AxisValue> axisValues = new ArrayList<AxisValue>();

                // set display x label.
                for (int i = 1; i <= mMaxDayInMonth ; i++) {
                    axisValues.add(new AxisValue(i).setLabel(mSelectMonth
                            + "/"
                            + i));
                }

                axisX.setValues(axisValues);
            }
            data.setAxisXBottom(axisX);
            data.setAxisYLeft(axisY);
        } else {
            data.setAxisXBottom(null);
            data.setAxisYLeft(null);
        }

        data.setBaseValue(Float.NEGATIVE_INFINITY);
        return data;
    }

}
