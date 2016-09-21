package com.realsil.android.wristbanddemo.sport;

import android.graphics.Color;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.AxisValue;
import lecho.lib.hellocharts.model.ColumnChartData;
import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.PointValue;
import lecho.lib.hellocharts.model.SubcolumnValue;
import lecho.lib.hellocharts.model.ValueShape;
import lecho.lib.hellocharts.util.ChartUtils;

import com.realsil.android.wristbanddemo.R;
import com.realsil.android.wristbanddemo.greendao.SportData;
import com.realsil.android.wristbanddemo.utility.WristbandCalculator;

public class SportLineUiManager {
	private ArrayList<SportData> mSports;
	private HashMap<Integer, SportSubData> mHourSportDataMap;
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
    
	public SportLineUiManager(List<SportData> sports) {
		mSports = new ArrayList<SportData>();
		// Make sure every offset only have one valid value.
		List<SportData> sps =
                WristbandCalculator.getAllUniqueOffsetDataWithSameDate(sports);
		for(SportData sp: sps) {
			mSports.add(sp);
		}
		mHourSportDataMap =
                WristbandCalculator.getAllHourDataWithSameDate(mSports);
	}
    public SportSubData getLinePointDetailData(int hour) {
        return mHourSportDataMap.get(hour);
    }
	public LineChartData getSportStepLineData() {
		List<PointValue> values = new ArrayList<PointValue>();
		
        for (int i = 1; i <= 24; ++i) {
        	if(mHourSportDataMap.get(i) == null) {
        		values.add(new PointValue(i, 0));
        	} else {
        		values.add(new PointValue(i, mHourSportDataMap.get(i).getStepCount()));
        	}
        }
    	Line line = new Line(values);
        //line.setColor(ChartUtils.COLOR_BLUE);
        line.setColor(Color.parseColor("#ff36bcf6"));
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
                for (int i = 1; i <= 24 ; i++) {
                    axisValues.add(new AxisValue(i).setLabel(i + ":00"));
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

    public static LineChartData getEmptySportStepLineData() {
        List<PointValue> values = new ArrayList<PointValue>();

        for (int i = 1; i <= 24; ++i) {
            values.add(new PointValue(i, 0));
        }
        Line line = new Line(values);
        line.setColor(ChartUtils.COLOR_BLUE);
        line.setShape(ValueShape.CIRCLE);
        line.setCubic(false);
        line.setFilled(false);
        line.setHasLabels(false);
        line.setHasLabelsOnlyForSelected(false);
        line.setHasLines(false);
        line.setHasPoints(false);

        ArrayList<Line> lines = new ArrayList<Line>();
        lines.add(line);

        LineChartData data = new LineChartData(lines);
        if (true) {
            Axis axisX = new Axis();
            Axis axisY = new Axis();
            if (true) {
                if(false) {
                    axisX.setName("Time");
                    axisY.setName("Step Count");
                }
                List<AxisValue> axisValues = new ArrayList<AxisValue>();

                // set display x label.
                for (int i = 1; i <= 24 ; i++) {
                    Log.d("Sport", i + ":00");
                    axisValues.add(new AxisValue(i).setLabel(i + ":00"));
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
	
	public LineChartData getSportCaloryLineData() {
		List<PointValue> values = new ArrayList<PointValue>();
		
        for (int i = 1; i <= 24; ++i) {
        	if(mHourSportDataMap.get(i) == null) {
        		values.add(new PointValue(i, 0));
        	} else {
        		values.add(new PointValue(i, mHourSportDataMap.get(i).getCalory()));
        	}
        }
    	Line line = new Line(values);
        line.setColor(ChartUtils.COLOR_GREEN);
        line.setShape(shape);
        line.setCubic(isCubic);
        line.setFilled(isFilled);
        line.setHasLabels(hasLabels);
        line.setHasLabelsOnlyForSelected(hasLabelForSelected);
        line.setHasLines(hasLines);
        line.setHasPoints(hasPoints);
        
        ArrayList<Line> lines = new ArrayList<Line>();
        lines.add(line);
        
        LineChartData data = new LineChartData(lines);
        if (hasAxes) {
            Axis axisX = new Axis().setHasLines(true);
            Axis axisY = new Axis();
            if (hasAxesNames) {
            	if(hasAxesLabelNames) {
            		axisX.setName("Time");
                	axisY.setName("Calory(kcal)");
            	}
                List<AxisValue> axisValues = new ArrayList<AxisValue>();
                // set display x label.
                for (int i = 1; i <= 24 ; i++) {
                    axisValues.add(new AxisValue(i).setLabel(i + ":00"));
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
	
	public LineChartData getSportDistanceLineData() {
		List<PointValue> values = new ArrayList<PointValue>();
		
        for (int i = 1; i <= 24; ++i) {
        	if(mHourSportDataMap.get(i) == null) {
        		values.add(new PointValue(i, 0));
        	} else {
        		values.add(new PointValue(i, mHourSportDataMap.get(i).getDistance()));
        	}
        }
    	Line line = new Line(values);
        line.setColor(ChartUtils.COLOR_ORANGE);
        line.setShape(shape);
        line.setCubic(isCubic);
        line.setFilled(isFilled);
        line.setHasLabels(hasLabels);
        line.setHasLabelsOnlyForSelected(hasLabelForSelected);
        line.setHasLines(hasLines);
        line.setHasPoints(hasPoints);
        
        ArrayList<Line> lines = new ArrayList<Line>();
        lines.add(line);
        
        LineChartData data = new LineChartData(lines);
        if (hasAxes) {
            Axis axisX = new Axis().setHasLines(true);
            Axis axisY = new Axis();
            if (hasAxesNames) {
            	if(hasAxesLabelNames) {
            		axisX.setName("Time");
                	axisY.setName("Distance(m)");
            	}
                List<AxisValue> axisValues = new ArrayList<AxisValue>();
                // set display x label.
                for (int i = 1; i <= 24 ; i++) {
                    axisValues.add(new AxisValue(i).setLabel(i + ":00"));
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
	
	
	
	public LineChartData getSportStepGainLineData(int target) {
		List<PointValue> values = new ArrayList<PointValue>();
		List<PointValue> targetValues = new ArrayList<PointValue>();
		int lastValue = 0;
        for (int i = 1; i <= 24; ++i) {
        	int value;
        	if(mHourSportDataMap.get(i) == null) {
        		value = 0;
        	} else {
        		value = mHourSportDataMap.get(i).getStepCount();
        	}
        	lastValue += value;
        	values.add(new PointValue(i, lastValue));
        	targetValues.add(new PointValue(i, target));
        }
        // generate gain line
    	Line line = new Line(values);
        line.setColor(ChartUtils.COLOR_BLUE);
        line.setShape(ValueShape.CIRCLE);
        line.setCubic(false);
        line.setFilled(true);
        line.setHasLabels(false);
        line.setHasLabelsOnlyForSelected(false);
        line.setHasLines(true);
        line.setHasPoints(true);
        
        // generate target line
        Line targetLine = new Line(targetValues);
        targetLine.setColor(ChartUtils.COLOR_RED);
        targetLine.setShape(ValueShape.CIRCLE);
        targetLine.setCubic(false);
        targetLine.setFilled(false);
        targetLine.setHasLabels(false);
        targetLine.setHasLabelsOnlyForSelected(false);
        targetLine.setHasLines(true);
        targetLine.setHasPoints(false);
        
        ArrayList<Line> lines = new ArrayList<Line>();
        lines.add(line);
        lines.add(targetLine);
        
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
                for (int i = 1; i <= 24 ; i++) {
                    axisValues.add(new AxisValue(i).setLabel(i + ":00"));
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
	
	public LineChartData getSportCaloryGainLineData() {
		List<PointValue> values = new ArrayList<PointValue>();
		int lastValue = 0;
        for (int i = 1; i <= 24; ++i) {
        	int value;
        	if(mHourSportDataMap.get(i) == null) {
        		value = 0;
        	} else {
        		value = mHourSportDataMap.get(i).getCalory();
        	}
        	lastValue += value;
        	values.add(new PointValue(i, lastValue));
        }
        
    	Line line = new Line(values);
        line.setColor(ChartUtils.COLOR_GREEN);
        
        line.setShape(ValueShape.CIRCLE);
        line.setCubic(false);
        line.setFilled(true);
        line.setHasLabels(false);
        line.setHasLabelsOnlyForSelected(false);
        line.setHasLines(true);
        line.setHasPoints(true);
        
        ArrayList<Line> lines = new ArrayList<Line>();
        lines.add(line);
        
        LineChartData data = new LineChartData(lines);
        if (hasAxes) {
            Axis axisX = new Axis().setHasLines(true);
            Axis axisY = new Axis();
            if (hasAxesNames) {
            	if(hasAxesLabelNames) {
            		axisX.setName("Time");
                	axisY.setName("Calory(cal)");
            	}
                List<AxisValue> axisValues = new ArrayList<AxisValue>();
                // set display x label.
                for (int i = 1; i <= 24 ; i++) {
                    axisValues.add(new AxisValue(i).setLabel(i + ":00"));
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
	
	public LineChartData getSportDistanceGainLineData() {
		List<PointValue> values = new ArrayList<PointValue>();
		int lastValue = 0;
        for (int i = 1; i <= 24; ++i) {
        	int value;
        	if(mHourSportDataMap.get(i) == null) {
        		value = 0;
        	} else {
        		value = mHourSportDataMap.get(i).getDistance();
        	}
        	lastValue += value;
        	values.add(new PointValue(i, lastValue));
        }
        
    	Line line = new Line(values);
        line.setColor(ChartUtils.COLOR_ORANGE);
        line.setShape(ValueShape.CIRCLE);
        line.setCubic(false);
        line.setFilled(true);
        line.setHasLabels(false);
        line.setHasLabelsOnlyForSelected(false);
        line.setHasLines(true);
        line.setHasPoints(true);
        
        ArrayList<Line> lines = new ArrayList<Line>();
        lines.add(line);
        
        LineChartData data = new LineChartData(lines);
        if (hasAxes) {
            Axis axisX = new Axis().setHasLines(true);
            Axis axisY = new Axis();
            if (hasAxesNames) {
            	if(hasAxesLabelNames) {
            		axisX.setName("Time");
                	axisY.setName("Distance(m)");
            	}
                List<AxisValue> axisValues = new ArrayList<AxisValue>();
                // set display x label.
                for (int i = 1; i <= 24 ; i++) {
                    axisValues.add(new AxisValue(i).setLabel(i + ":00"));
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
