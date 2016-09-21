package com.realsil.android.wristbanddemo.sleep;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.AxisValue;
import lecho.lib.hellocharts.model.Column;
import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.PointValue;
import lecho.lib.hellocharts.model.SubcolumnValue;
import lecho.lib.hellocharts.model.ValueShape;
import lecho.lib.hellocharts.util.ChartUtils;

import android.util.Log;

import com.realsil.android.wristbanddemo.sport.SportSubData;
import com.realsil.android.wristbanddemo.utility.WristbandCalculator;
import com.realsil.android.wristbanddemo.utility.WristbandCalculator.SleepIncreaseComparator;
import com.realsil.android.wristbanddemo.greendao.SleepData;
import com.realsil.android.wristbanddemo.greendao.SportData;

public class SleepLineUiManager {
	private static final String TAG = "SleepLineUiManager";
    private static final boolean D = true;
    
	private ArrayList<SleepData> mSleeps;
	private HashMap<Integer, SportSubData> mHourSportDataMap;
	private boolean hasAxes = true;
    private boolean hasAxesNames = true;
    private boolean hasAxesLabelNames = false;
    private boolean hasLines = true;
    private boolean hasPoints = true;
    private ValueShape shape = ValueShape.CIRCLE;
    private boolean isFilled = false;
    private boolean hasLabels = false;
    private boolean isCubic = false;
    private boolean hasLabelForSelected = false;
    private boolean pointsHaveDifferentColor;
    private final static int SLEEP_MODE_DISPLAY_HIGHT_AWAKE = 4;
    private final static int SLEEP_MODE_DISPLAY_HIGHT_LIGHT = 7;
    private final static int SLEEP_MODE_DISPLAY_HIGHT_DEEP = 9;

	private int SLEEP_MODE_DISPLAY_COLOR_AWAKE = ChartUtils.COLOR_BLUE;
    private int SLEEP_MODE_DISPLAY_COLOR_LIGHT = ChartUtils.COLOR_ORANGE;
    private int SLEEP_MODE_DISPLAY_COLOR_DEEP = ChartUtils.COLOR_VIOLET;
    
	public SleepLineUiManager(List<SleepData> sleeps) {
		mSleeps = new ArrayList<SleepData>();

		for(SleepData sl: sleeps) {
			mSleeps.add(sl);
		}
		// sort the sleep data
		Collections.sort(mSleeps, new SleepIncreaseComparator());
	}
	
	public LineChartData getSleepLineData() {
		if(D) Log.d(TAG, "getSleepLineData");
		for(SleepData sl: mSleeps) {
			Log.d(TAG, "getSleepLineData, sort data. " 
					+ WristbandCalculator.toString(sl));
		}
		
		List<PointValue> lightSleepValues = new ArrayList<PointValue>();
		List<PointValue> deepSleepValues = new ArrayList<PointValue>();
		List<PointValue> awakeValues = new ArrayList<PointValue>();
		ArrayList<Line> lines = new ArrayList<Line>();
		
		
		int lastMode = -1;
		for(SleepData sl: mSleeps) {
			int mode = sl.getMode();
			int minutes = sl.getMinutes();
			if(D) Log.d(TAG, "lastMode: " + lastMode
					+ ", mode: " + mode
					+ ", minutes: " + minutes);
			if(lastMode == -1) {
				if(minutes == 1) {
					// if start with first minutes
					switch(mode) {
					case WristbandCalculator.SLEEP_MODE_START_DEEP_SLEEP:
						deepSleepValues.add(new PointValue(1, 0));
						deepSleepValues.add(new PointValue(1, SLEEP_MODE_DISPLAY_HIGHT_DEEP));
						break;
					case WristbandCalculator.SLEEP_MODE_START_LIGHT_SLEEP_MODE_1:
						lightSleepValues.add(new PointValue(1, 0));
						lightSleepValues.add(new PointValue(1, SLEEP_MODE_DISPLAY_HIGHT_LIGHT));
						break;
					case WristbandCalculator.SLEEP_MODE_START_LIGHT_SLEEP_MODE_2:
						lightSleepValues.add(new PointValue(1, 0));
						lightSleepValues.add(new PointValue(1, SLEEP_MODE_DISPLAY_HIGHT_LIGHT));
						break;
					case WristbandCalculator.SLEEP_MODE_START_ENTER_SLEEP:
						awakeValues.add(new PointValue(1, 0));
						awakeValues.add(new PointValue(1, SLEEP_MODE_DISPLAY_HIGHT_AWAKE));
						break;
					}
				} else {
					// initial first value
					deepSleepValues.add(new PointValue(1, 0));
					lightSleepValues.add(new PointValue(1, 0));
					awakeValues.add(new PointValue(1, 0));
					
					switch(mode) {
					
					case WristbandCalculator.SLEEP_MODE_START_DEEP_SLEEP:
						deepSleepValues.add(new PointValue(minutes, 0));
						deepSleepValues.add(new PointValue(minutes, SLEEP_MODE_DISPLAY_HIGHT_DEEP));
						break;
					case WristbandCalculator.SLEEP_MODE_START_LIGHT_SLEEP_MODE_1:
						lightSleepValues.add(new PointValue(minutes, 0));
						lightSleepValues.add(new PointValue(minutes, SLEEP_MODE_DISPLAY_HIGHT_LIGHT));
						break;
					case WristbandCalculator.SLEEP_MODE_START_LIGHT_SLEEP_MODE_2:
						lightSleepValues.add(new PointValue(minutes, 0));
						lightSleepValues.add(new PointValue(minutes, SLEEP_MODE_DISPLAY_HIGHT_LIGHT));
						break;
					case WristbandCalculator.SLEEP_MODE_START_ENTER_SLEEP:
						awakeValues.add(new PointValue(minutes, 0));
						awakeValues.add(new PointValue(minutes, SLEEP_MODE_DISPLAY_HIGHT_AWAKE));
						break;
					}
				}
				lastMode = mode;
				continue;
			}
			switch(lastMode) {
			case WristbandCalculator.SLEEP_MODE_START_DEEP_SLEEP:
				switch(mode) {
				case WristbandCalculator.SLEEP_MODE_START_LIGHT_SLEEP_MODE_2:
					deepSleepValues.add(new PointValue(minutes, SLEEP_MODE_DISPLAY_HIGHT_DEEP));
					deepSleepValues.add(new PointValue(minutes, 0));
					
					lightSleepValues.add(new PointValue(minutes, 0));
					lightSleepValues.add(new PointValue(minutes, SLEEP_MODE_DISPLAY_HIGHT_LIGHT));
					break;
				default:
					if(D) Log.e(TAG, "The input data may be is error" 
							+ ", lastMode: " + lastMode
							+ ", mode: " + mode);
					return null;
				}
				break;
				
			case WristbandCalculator.SLEEP_MODE_START_LIGHT_SLEEP_MODE_1:
				switch(mode) {
				case WristbandCalculator.SLEEP_MODE_START_DEEP_SLEEP:
					lightSleepValues.add(new PointValue(minutes, SLEEP_MODE_DISPLAY_HIGHT_LIGHT));
					lightSleepValues.add(new PointValue(minutes, 0));
					
					deepSleepValues.add(new PointValue(minutes, 0));
					deepSleepValues.add(new PointValue(minutes, SLEEP_MODE_DISPLAY_HIGHT_DEEP));
					break;
				case WristbandCalculator.SLEEP_MODE_EXIT_SLEEP:
					lightSleepValues.add(new PointValue(minutes, SLEEP_MODE_DISPLAY_HIGHT_LIGHT));
					lightSleepValues.add(new PointValue(minutes, 0));
					break;
				default:
					if(D) Log.e(TAG, "The input data may be is error" 
							+ ", lastMode: " + lastMode
							+ ", mode: " + mode);
					return null;
				}
				break;
			case WristbandCalculator.SLEEP_MODE_START_LIGHT_SLEEP_MODE_2:
				switch(mode) {
				case WristbandCalculator.SLEEP_MODE_START_LIGHT_SLEEP_MODE_1:
					break;
				case WristbandCalculator.SLEEP_MODE_EXIT_SLEEP:
					lightSleepValues.add(new PointValue(minutes, SLEEP_MODE_DISPLAY_HIGHT_LIGHT));
					lightSleepValues.add(new PointValue(minutes, 0));
					break;
				default:
					if(D) Log.e(TAG, "The input data may be is error" 
							+ ", lastMode: " + lastMode
							+ ", mode: " + mode);
					return null;
				}
				break;
			case WristbandCalculator.SLEEP_MODE_START_ENTER_SLEEP:
				switch(mode) {
				case WristbandCalculator.SLEEP_MODE_START_LIGHT_SLEEP_MODE_1:
					awakeValues.add(new PointValue(minutes, SLEEP_MODE_DISPLAY_HIGHT_AWAKE));
					awakeValues.add(new PointValue(minutes, 0));
					
					lightSleepValues.add(new PointValue(minutes, 0));
					lightSleepValues.add(new PointValue(minutes, SLEEP_MODE_DISPLAY_HIGHT_LIGHT));
					break;
				default:
					if(D) Log.e(TAG, "The input data may be is error" 
							+ ", lastMode: " + lastMode
							+ ", mode: " + mode);
					return null;
				}
				break;
			case WristbandCalculator.SLEEP_MODE_EXIT_SLEEP:
				switch(mode) {
				case WristbandCalculator.SLEEP_MODE_START_ENTER_SLEEP:
					awakeValues.add(new PointValue(minutes, 0));
					awakeValues.add(new PointValue(minutes, SLEEP_MODE_DISPLAY_HIGHT_AWAKE));
					break;
				default:
					if(D) Log.e(TAG, "The input data may be is error" 
							+ ", lastMode: " + lastMode
							+ ", mode: " + mode);
					return null;
				}
				break;
			}
			lastMode = mode;
		}

		deepSleepValues.add(new PointValue(1440, 0));
		lightSleepValues.add(new PointValue(1440, 0));
		awakeValues.add(new PointValue(1440, 0));
		
		// Awake
		Line line1 = new Line(awakeValues);
		line1.setColor(SLEEP_MODE_DISPLAY_COLOR_AWAKE);
		line1.setCubic(false);
		line1.setFilled(true);
		line1.setHasLabels(false);
		line1.setHasLabelsOnlyForSelected(false);
		line1.setHasLines(true);
		line1.setHasPoints(false);
		
		Line line2 = new Line(lightSleepValues);
		line2.setColor(SLEEP_MODE_DISPLAY_COLOR_LIGHT);
		line2.setCubic(false);
		line2.setFilled(true);
		line2.setHasLabels(false);
		line2.setHasLabelsOnlyForSelected(false);
		line2.setHasLines(true);
		line2.setHasPoints(false);
		
		Line line3 = new Line(deepSleepValues);
		line3.setColor(SLEEP_MODE_DISPLAY_COLOR_DEEP);
		line3.setCubic(false);
		line3.setFilled(true);
		line3.setHasLabels(false);
		line3.setHasLabelsOnlyForSelected(false);
		line3.setHasLines(true);
		line3.setHasPoints(false);

        lines.add(line1);
        lines.add(line2);
        lines.add(line3);
		
        LineChartData data = new LineChartData(lines);
        if (hasAxes) {
            Axis axisX = new Axis();
            //Axis axisY = new Axis();
            if (hasAxesNames) {
                List<AxisValue> axisValues = new ArrayList<AxisValue>();
                // set display x label.
                for (int i = 1; i <= 1440 ; i++) {
                    axisValues.add(new AxisValue(i).setLabel(i/60 + ":" + i%60));
                }
                
                axisX.setValues(axisValues);
            }
            data.setAxisXBottom(axisX);
            //data.setAxisYLeft(axisY);
        } else {
            data.setAxisXBottom(null);
            data.setAxisYLeft(null);
        }

        data.setBaseValue(Float.NEGATIVE_INFINITY);
		return data;
	}
	
	public LineChartData getSpecialSleepLineData() {
		if(D) Log.d(TAG, "getSpecialSleepLineData");
		for(SleepData sl: mSleeps) {
			Log.d(TAG, "getSpecialSleepLineData, sort data. " 
					+ WristbandCalculator.toString(sl));
		}
		
		List<PointValue> lightSleepValues = new ArrayList<PointValue>();
		List<PointValue> deepSleepValues = new ArrayList<PointValue>();
		List<PointValue> awakeValues = new ArrayList<PointValue>();
		ArrayList<Line> lines = new ArrayList<Line>();
		
		
		int lastMode = -1;
		int lastMinutes = -1;
		for(SleepData sl: mSleeps) {
			int mode = sl.getMode();
			int minutes = sl.getMinutes();
			if(D) Log.d(TAG, "lastMode: " + lastMode
					+ ", mode: " + mode
					+ ", minutes: " + minutes);
			if(lastMode == -1) {
				if(minutes == 1) {
					// if start with first minutes
					switch(mode) {
					case WristbandCalculator.SLEEP_MODE_START_DEEP_SLEEP:
						deepSleepValues.add(new PointValue(1, 0));
						deepSleepValues.add(new PointValue(1, SLEEP_MODE_DISPLAY_HIGHT_DEEP));
						break;
					case WristbandCalculator.SLEEP_MODE_START_LIGHT_SLEEP_MODE_1:
						lightSleepValues.add(new PointValue(1, 0));
						lightSleepValues.add(new PointValue(1, SLEEP_MODE_DISPLAY_HIGHT_LIGHT));
						break;
					case WristbandCalculator.SLEEP_MODE_START_LIGHT_SLEEP_MODE_2:
						lightSleepValues.add(new PointValue(1, 0));
						lightSleepValues.add(new PointValue(1, SLEEP_MODE_DISPLAY_HIGHT_LIGHT));
						break;
					case WristbandCalculator.SLEEP_MODE_START_ENTER_SLEEP:
						awakeValues.add(new PointValue(1, 0));
						awakeValues.add(new PointValue(1, SLEEP_MODE_DISPLAY_HIGHT_AWAKE));
						break;
					}
				} else {
					// initial first value
					deepSleepValues.add(new PointValue(1, 0));
					lightSleepValues.add(new PointValue(1, 0));
					awakeValues.add(new PointValue(1, 0));
					
					switch(mode) {
					
					case WristbandCalculator.SLEEP_MODE_START_DEEP_SLEEP:
						deepSleepValues.add(new PointValue(minutes, 0));
						deepSleepValues.add(new PointValue(minutes, SLEEP_MODE_DISPLAY_HIGHT_DEEP));
						break;
					case WristbandCalculator.SLEEP_MODE_START_LIGHT_SLEEP_MODE_1:
						lightSleepValues.add(new PointValue(minutes, 0));
						lightSleepValues.add(new PointValue(minutes, SLEEP_MODE_DISPLAY_HIGHT_LIGHT));
						break;
					case WristbandCalculator.SLEEP_MODE_START_LIGHT_SLEEP_MODE_2:
						lightSleepValues.add(new PointValue(minutes, 0));
						lightSleepValues.add(new PointValue(minutes, SLEEP_MODE_DISPLAY_HIGHT_LIGHT));
						break;
					case WristbandCalculator.SLEEP_MODE_START_ENTER_SLEEP:
						awakeValues.add(new PointValue(minutes, 0));
						awakeValues.add(new PointValue(minutes, SLEEP_MODE_DISPLAY_HIGHT_AWAKE));
						break;
					}
				}
				lastMode = mode;
				continue;
			}
			switch(lastMode) {
			case WristbandCalculator.SLEEP_MODE_START_DEEP_SLEEP:
				switch(mode) {
				case WristbandCalculator.SLEEP_MODE_START_LIGHT_SLEEP_MODE_2:
					deepSleepValues.add(new PointValue(minutes, SLEEP_MODE_DISPLAY_HIGHT_DEEP));
					deepSleepValues.add(new PointValue(minutes, 0));
					
					lightSleepValues.add(new PointValue(minutes, 0));
					lightSleepValues.add(new PointValue(minutes, SLEEP_MODE_DISPLAY_HIGHT_LIGHT));
					break;
				default:
					if(D) Log.e(TAG, "The input data may be is error" 
							+ ", lastMode: " + lastMode
							+ ", mode: " + mode);
					return null;
				}
				break;
				
			case WristbandCalculator.SLEEP_MODE_START_LIGHT_SLEEP_MODE_1:
				switch(mode) {
				case WristbandCalculator.SLEEP_MODE_START_DEEP_SLEEP:
					lightSleepValues.add(new PointValue(minutes, SLEEP_MODE_DISPLAY_HIGHT_LIGHT));
					lightSleepValues.add(new PointValue(minutes, 0));
					
					deepSleepValues.add(new PointValue(minutes, 0));
					deepSleepValues.add(new PointValue(minutes, SLEEP_MODE_DISPLAY_HIGHT_DEEP));
					break;
				case WristbandCalculator.SLEEP_MODE_EXIT_SLEEP:
					lightSleepValues.add(new PointValue(minutes, SLEEP_MODE_DISPLAY_HIGHT_LIGHT));
					lightSleepValues.add(new PointValue(minutes, 0));
					break;
				default:
					if(D) Log.e(TAG, "The input data may be is error" 
							+ ", lastMode: " + lastMode
							+ ", mode: " + mode);
					return null;
				}
				break;
			case WristbandCalculator.SLEEP_MODE_START_LIGHT_SLEEP_MODE_2:
				switch(mode) {
				case WristbandCalculator.SLEEP_MODE_START_LIGHT_SLEEP_MODE_1:
					break;
				case WristbandCalculator.SLEEP_MODE_EXIT_SLEEP:
					lightSleepValues.add(new PointValue(minutes, SLEEP_MODE_DISPLAY_HIGHT_LIGHT));
					lightSleepValues.add(new PointValue(minutes, 0));
					break;
				default:
					if(D) Log.e(TAG, "The input data may be is error" 
							+ ", lastMode: " + lastMode
							+ ", mode: " + mode);
					return null;
				}
				break;
			case WristbandCalculator.SLEEP_MODE_START_ENTER_SLEEP:
				switch(mode) {
				case WristbandCalculator.SLEEP_MODE_START_LIGHT_SLEEP_MODE_1:
					awakeValues.add(new PointValue(minutes, SLEEP_MODE_DISPLAY_HIGHT_AWAKE));
					awakeValues.add(new PointValue(minutes, 0));
					
					lightSleepValues.add(new PointValue(minutes, 0));
					lightSleepValues.add(new PointValue(minutes, SLEEP_MODE_DISPLAY_HIGHT_LIGHT));
					break;
				default:
					if(D) Log.e(TAG, "The input data may be is error" 
							+ ", lastMode: " + lastMode
							+ ", mode: " + mode);
					return null;
				}
				break;
			case WristbandCalculator.SLEEP_MODE_EXIT_SLEEP:
				switch(mode) {
				case WristbandCalculator.SLEEP_MODE_START_ENTER_SLEEP:
					awakeValues.add(new PointValue(minutes, 0));
					awakeValues.add(new PointValue(minutes, SLEEP_MODE_DISPLAY_HIGHT_AWAKE));
					break;
				default:
					if(D) Log.e(TAG, "The input data may be is error" 
							+ ", lastMode: " + lastMode
							+ ", mode: " + mode);
					return null;
				}
				break;
			}
			lastMode = mode;
			lastMinutes = minutes;
		}

		deepSleepValues.add(new PointValue(lastMinutes + 10, 0));
		lightSleepValues.add(new PointValue(lastMinutes + 10, 0));
		awakeValues.add(new PointValue(lastMinutes + 10, 0));
		
		// Awake
		Line line1 = new Line(awakeValues);
		line1.setColor(SLEEP_MODE_DISPLAY_COLOR_AWAKE);
		line1.setCubic(false);
		line1.setFilled(true);
		line1.setHasLabels(false);
		line1.setHasLabelsOnlyForSelected(false);
		line1.setHasLines(true);
		line1.setHasPoints(false);
		
		Line line2 = new Line(lightSleepValues);
		line2.setColor(SLEEP_MODE_DISPLAY_COLOR_LIGHT);
		line2.setCubic(false);
		line2.setFilled(true);
		line2.setHasLabels(false);
		line2.setHasLabelsOnlyForSelected(false);
		line2.setHasLines(true);
		line2.setHasPoints(false);
		
		Line line3 = new Line(deepSleepValues);
		line3.setColor(SLEEP_MODE_DISPLAY_COLOR_DEEP);
		line3.setCubic(false);
		line3.setFilled(true);
		line3.setHasLabels(false);
		line3.setHasLabelsOnlyForSelected(false);
		line3.setHasLines(true);
		line3.setHasPoints(false);

        lines.add(line1);
        lines.add(line2);
        lines.add(line3);
		
        LineChartData data = new LineChartData(lines);
        if (hasAxes) {
            Axis axisX = new Axis();
            //Axis axisY = new Axis();
            if (hasAxesNames) {
                List<AxisValue> axisValues = new ArrayList<AxisValue>();
                // set display x label.
                for (int i = 1; i <= lastMinutes + 10; i++) {
                    axisValues.add(new AxisValue(i).setLabel(i/60 + ":" + i%60));
                }
                
                axisX.setValues(axisValues);
            }
            data.setAxisXBottom(axisX);
            //data.setAxisYLeft(axisY);
        } else {
            data.setAxisXBottom(null);
            data.setAxisYLeft(null);
        }

        data.setBaseValue(Float.NEGATIVE_INFINITY);
		return data;
	}


	public void setSleepModeDisplayColorAwake(int sleepModeDisplayColorAwake) {
		SLEEP_MODE_DISPLAY_COLOR_AWAKE = sleepModeDisplayColorAwake;
	}

	public void setSleepModeDisplayColorLight(int sleepModeDisplayColorLight) {
		SLEEP_MODE_DISPLAY_COLOR_LIGHT = sleepModeDisplayColorLight;
	}

	public void setSleepModeDisplayColorDeep(int sleepModeDisplayColorDeep) {
		SLEEP_MODE_DISPLAY_COLOR_DEEP = sleepModeDisplayColorDeep;
	}

}
