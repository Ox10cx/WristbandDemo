package com.realsil.android.wristbanddemo.sleep;

import android.graphics.Color;
import android.util.Log;

import com.realsil.android.wristbanddemo.applicationlayer.ApplicationLayer;
import com.realsil.android.wristbanddemo.greendao.SleepData;
import com.realsil.android.wristbanddemo.sport.SportSubData;
import com.realsil.android.wristbanddemo.utility.WristbandCalculator;
import com.realsil.android.wristbanddemo.utility.WristbandCalculator.SleepIncreaseComparator;
import com.realsil.android.wristbanddemo.view.SleepChartView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.AxisValue;
import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.PointValue;
import lecho.lib.hellocharts.model.ValueShape;
import lecho.lib.hellocharts.util.ChartUtils;

public class SleepLineUiManagerSpec {
	private static final String TAG = "SleepLineUiManagerSpec";
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
	public final static int SLEEP_MODE_DISPLAY_HIGHT_NULL = 0;
	public final static int SLEEP_MODE_DISPLAY_HIGHT_AWAKE = 4;
	public final static int SLEEP_MODE_DISPLAY_HIGHT_LIGHT = 7;
	public final static int SLEEP_MODE_DISPLAY_HIGHT_DEEP = 9;
	public final static int SLEEP_MODE_DISPLAY_HIGHT_MAX = 11;

	private int SLEEP_MODE_DISPLAY_COLOR_AWAKE = ChartUtils.COLOR_BLUE;
    private int SLEEP_MODE_DISPLAY_COLOR_LIGHT = ChartUtils.COLOR_ORANGE;
    private int SLEEP_MODE_DISPLAY_COLOR_DEEP = ChartUtils.COLOR_VIOLET;

	public SleepLineUiManagerSpec(int y, int m, int d,
								  List<SleepData> sleeps) {
		Calendar c1 = Calendar.getInstance();
		c1.set(y, m - 1, d);// here need decrease 1 of month
		c1.add(Calendar.DATE, -1);
		int yesterdayYear = c1.get(Calendar.YEAR);
		int yesterdayMonth = c1.get(Calendar.MONTH) + 1;
		int yesterdayDay = c1.get(Calendar.DATE);
		if(D) Log.d(TAG, "SleepLineUiManagerSpec, y: " + y + ", m: " + m + ", d: " + d
				+ ", yesterdayYear: " + yesterdayYear + ", yesterdayMonth: " + yesterdayMonth
				+ ", yesterdayDay: " + yesterdayDay);
		// get the special date sleep data
		mSleeps = new ArrayList<SleepData>();
		for(SleepData sl: sleeps) {
			Log.d(TAG, "SleepLineUiManagerSpec, sort data. "
					+ WristbandCalculator.toString(sl));
		}
		List<SleepData> d1 = WristbandCalculator.getSubSleepDataByDate(yesterdayYear, yesterdayMonth, yesterdayDay, sleeps);
		if(d1 != null) {
			for(SleepData sl: d1) {
				if(sl.getMinutes() >= WristbandCalculator.START_SLEEP_TIME_MINUTE
						&& sl.getMinutes() <= WristbandCalculator.MAX_MINUTE) {
					// Use Deep Copy
					SleepData tmp = new SleepData(sl.getId(), sl.getYear(), sl.getMonth(), sl.getDay(),
							sl.getMinutes(), sl.getMode(), sl.getDate());
					tmp.setMinutes(tmp.getMinutes() - WristbandCalculator.START_SLEEP_TIME_MINUTE);
					mSleeps.add(tmp);
				}
			}
		}
		List<SleepData> d2 = WristbandCalculator.getSubSleepDataByDate(y, m, d, sleeps);
		if(d2 != null) {
			for(SleepData sl: d2) {
				if(sl.getMinutes() >= 0
						&& sl.getMinutes() <= WristbandCalculator.END_SLEEP_TIME_MINUTE) {
					// Use Deep Copy
					SleepData tmp = new SleepData(sl.getId(), sl.getYear(), sl.getMonth(), sl.getDay(),
							sl.getMinutes(), sl.getMode(), sl.getDate());
					tmp.setMinutes(tmp.getMinutes() + WristbandCalculator.MAX_MINUTE - WristbandCalculator.START_SLEEP_TIME_MINUTE);
					mSleeps.add(tmp);

				}
			}
		}

		// sort the sleep data
		Collections.sort(mSleeps, new SleepIncreaseComparator());

		// Remove the error minute data
		WristbandCalculator.removeSameMinuteSleepData(mSleeps);
	}
	
	public LineChartData getSleepLineData() {
		if(D) Log.d(TAG, "getSleepLineData");
		for(SleepData sl: mSleeps) {
			Log.d(TAG, "getSleepLineData, sort data. " 
					+ WristbandCalculator.toString(sl));
		}

		ArrayList<Integer> Minutes = new ArrayList<>();
		
		List<PointValue> lightSleepValues = new ArrayList<PointValue>();
		List<PointValue> deepSleepValues = new ArrayList<PointValue>();
		List<PointValue> awakeValues = new ArrayList<PointValue>();
		ArrayList<Line> lines = new ArrayList<Line>();
		
		
		int lastMode = -1;
		for(int i = 0; i < mSleeps.size(); i ++) {
			SleepData sl = mSleeps.get(i);
			int mode = sl.getMode();
			int minutes = sl.getMinutes();
			if(D) Log.d(TAG, "lastMode: " + lastMode
					+ ", mode: " + mode
					+ ", minutes: " + minutes);
			if(lastMode == -1) {
				if(minutes == 0) {
					Minutes.add(minutes);
					// if start with first minutes
					switch(mode) {
					case ApplicationLayer.SLEEP_MODE_START_WAKE:
						awakeValues.add(new PointValue(0, 0));
						awakeValues.add(new PointValue(0, SLEEP_MODE_DISPLAY_HIGHT_AWAKE));
						break;
					case ApplicationLayer.SLEEP_MODE_START_SLEEP:
						lightSleepValues.add(new PointValue(0, 0));
						lightSleepValues.add(new PointValue(0, SLEEP_MODE_DISPLAY_HIGHT_LIGHT));
						break;
					case ApplicationLayer.SLEEP_MODE_START_DEEP_SLEEP:
						deepSleepValues.add(new PointValue(0, 0));
						deepSleepValues.add(new PointValue(0, SLEEP_MODE_DISPLAY_HIGHT_DEEP));
						break;
					}
				} else {
					Minutes.add(0);
					// initial first value
					deepSleepValues.add(new PointValue(0, 0));
					lightSleepValues.add(new PointValue(0, 0));
					awakeValues.add(new PointValue(0, 0));

					Minutes.add(minutes);
					switch(mode) {
					
					case ApplicationLayer.SLEEP_MODE_START_WAKE:
						awakeValues.add(new PointValue(minutes, 0));
						awakeValues.add(new PointValue(minutes, SLEEP_MODE_DISPLAY_HIGHT_AWAKE));
						break;
					case ApplicationLayer.SLEEP_MODE_START_SLEEP:
						lightSleepValues.add(new PointValue(minutes, 0));
						lightSleepValues.add(new PointValue(minutes, SLEEP_MODE_DISPLAY_HIGHT_LIGHT));
						break;
					case ApplicationLayer.SLEEP_MODE_START_DEEP_SLEEP:
						deepSleepValues.add(new PointValue(minutes, 0));
						deepSleepValues.add(new PointValue(minutes, SLEEP_MODE_DISPLAY_HIGHT_DEEP));
						break;
					}
				}
				lastMode = mode;
				continue;
			}
			Minutes.add(minutes);
			switch(lastMode) {
			case ApplicationLayer.SLEEP_MODE_START_WAKE:
				switch(mode) {
				case ApplicationLayer.SLEEP_MODE_START_SLEEP:
					lightSleepValues.add(new PointValue(minutes, 0));
					lightSleepValues.add(new PointValue(minutes, SLEEP_MODE_DISPLAY_HIGHT_LIGHT));

					awakeValues.add(new PointValue(minutes, SLEEP_MODE_DISPLAY_HIGHT_AWAKE));
					awakeValues.add(new PointValue(minutes, 0));
					break;
				default:
					if(D) Log.e(TAG, "The input data may be is error" 
							+ ", lastMode: " + lastMode
							+ ", mode: " + mode);
					return null;
				}
				break;
				
			case ApplicationLayer.SLEEP_MODE_START_SLEEP:
				switch(mode) {
				case ApplicationLayer.SLEEP_MODE_START_DEEP_SLEEP:
					deepSleepValues.add(new PointValue(minutes, 0));
					deepSleepValues.add(new PointValue(minutes, SLEEP_MODE_DISPLAY_HIGHT_DEEP));

					lightSleepValues.add(new PointValue(minutes, SLEEP_MODE_DISPLAY_HIGHT_LIGHT));
					lightSleepValues.add(new PointValue(minutes, 0));
					break;
				case ApplicationLayer.SLEEP_MODE_START_WAKE:
					if(i != (mSleeps.size() - 1)) {
						awakeValues.add(new PointValue(minutes, 0));
						awakeValues.add(new PointValue(minutes, SLEEP_MODE_DISPLAY_HIGHT_AWAKE));
					}

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
			case ApplicationLayer.SLEEP_MODE_START_DEEP_SLEEP:
				switch(mode) {
				case ApplicationLayer.SLEEP_MODE_START_SLEEP:
					lightSleepValues.add(new PointValue(minutes, 0));
					lightSleepValues.add(new PointValue(minutes, SLEEP_MODE_DISPLAY_HIGHT_LIGHT));

					deepSleepValues.add(new PointValue(minutes, SLEEP_MODE_DISPLAY_HIGHT_DEEP));
					deepSleepValues.add(new PointValue(minutes, 0));
					break;
				case ApplicationLayer.SLEEP_MODE_START_WAKE:
					if(i != (mSleeps.size() - 1)) {
						awakeValues.add(new PointValue(minutes, 0));
						awakeValues.add(new PointValue(minutes, SLEEP_MODE_DISPLAY_HIGHT_AWAKE));
					}

					deepSleepValues.add(new PointValue(minutes, SLEEP_MODE_DISPLAY_HIGHT_DEEP));
					deepSleepValues.add(new PointValue(minutes, 0));
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
		int totalTime = WristbandCalculator.MAX_MINUTE - WristbandCalculator.START_SLEEP_TIME_MINUTE + WristbandCalculator.END_SLEEP_TIME_MINUTE;
		Minutes.add(totalTime);
		deepSleepValues.add(new PointValue(totalTime, 0));
		lightSleepValues.add(new PointValue(totalTime, 0));
		awakeValues.add(new PointValue(totalTime, 0));
		
		// Awake
		Line line1 = new Line(awakeValues);
		line1.setColor(SLEEP_MODE_DISPLAY_COLOR_AWAKE);
		line1.setCubic(false);
		line1.setFilled(true);
		line1.setHasLabels(false);
		line1.setHasLabelsOnlyForSelected(false);
		line1.setHasLines(true);
		line1.setHasPoints(false);
		//line1.setSquare(true);
		
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
				for(int i: Minutes) {
					int hour;
					if(i > (WristbandCalculator.MAX_MINUTE -WristbandCalculator.START_SLEEP_TIME_MINUTE)) {
						hour = (i - (WristbandCalculator.MAX_MINUTE -WristbandCalculator.START_SLEEP_TIME_MINUTE)) / 60;
					} else {
						hour = (i + WristbandCalculator.START_SLEEP_TIME_MINUTE) / 60;
					}
					int minute = i % 60;
					String hourStr = String.valueOf(hour).length() == 1
							? "0" + String.valueOf(hour)
							: String.valueOf(hour);
					String minuteStr = String.valueOf(minute).length() == 1
							? "0" + String.valueOf(minute)
							: String.valueOf(minute);
					axisValues.add(new AxisValue(i).setLabel(hourStr + ":" + minuteStr));
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

	public SleepChartView.SleepChartData getSpecialSleepNewUIDataNoErrorCheck() {
		if(D) Log.d(TAG, "getSpecialSleepNewUIDataNoErrorCheck");
		for(SleepData sl: mSleeps) {
			Log.d(TAG, "getSpecialSleepNewUIDataNoErrorCheck, sort data. "
					+ WristbandCalculator.toString(sl));
		}

		ArrayList<Integer> Minutes = new ArrayList<>();

		ArrayList<SleepChartView.ChartData> chartDatas = new ArrayList<>();
		ArrayList<SleepChartView.AxisValue> axisDatas = new ArrayList<>();


		int lastMode = -1;
		int lastMinutes = -1;
		for(int i = 0; i < mSleeps.size(); i ++) {
			SleepData sl = mSleeps.get(i);
			int mode = sl.getMode();
			int minutes = sl.getMinutes();
			if (D) Log.d(TAG, "lastMode: " + lastMode
					+ ", mode: " + mode
					+ ", minutes: " + minutes);
			Minutes.add(minutes);
			if(lastMode == -1) {
				// initial first value
				//deepSleepValues.add(new PointValue(0, 0));
				//lightSleepValues.add(new PointValue(0, 0));
				//awakeValues.add(new PointValue(0, 0));
				if(mSleeps.size() != 1) {
					// if start with first minutes
					switch(mode) {
						case ApplicationLayer.SLEEP_MODE_START_WAKE:
							chartDatas.add(new SleepChartView.ChartData(minutes, SLEEP_MODE_DISPLAY_HIGHT_AWAKE, SLEEP_MODE_DISPLAY_COLOR_AWAKE));
							break;
						case ApplicationLayer.SLEEP_MODE_START_SLEEP:
							chartDatas.add(new SleepChartView.ChartData(minutes, SLEEP_MODE_DISPLAY_HIGHT_LIGHT, SLEEP_MODE_DISPLAY_COLOR_LIGHT));
							break;
						case ApplicationLayer.SLEEP_MODE_START_DEEP_SLEEP:
							chartDatas.add(new SleepChartView.ChartData(minutes, SLEEP_MODE_DISPLAY_HIGHT_DEEP, SLEEP_MODE_DISPLAY_COLOR_DEEP));
							break;
					}
				}

				lastMode = mode;
				lastMinutes = minutes;
				continue;
			}
			switch(lastMode) {
				case ApplicationLayer.SLEEP_MODE_START_WAKE:
					switch(mode) {
						case ApplicationLayer.SLEEP_MODE_START_SLEEP:
							chartDatas.add(new SleepChartView.ChartData(minutes, SLEEP_MODE_DISPLAY_HIGHT_LIGHT, SLEEP_MODE_DISPLAY_COLOR_LIGHT));
							break;
						case ApplicationLayer.SLEEP_MODE_START_WAKE:
							if(D) Log.e(TAG, "The input data may be is error, Same mode, Do nothing"
									+ ", lastMode: " + lastMode
									+ ", mode: " + mode);
							break;
						case ApplicationLayer.SLEEP_MODE_START_DEEP_SLEEP:
							if(D) Log.e(TAG, "The input data may be is error, Error mode, Just show"
									+ ", lastMode: " + lastMode
									+ ", mode: " + mode);
							chartDatas.add(new SleepChartView.ChartData(minutes, SLEEP_MODE_DISPLAY_HIGHT_DEEP, SLEEP_MODE_DISPLAY_COLOR_DEEP));
							break;
						default:
							if(D) Log.e(TAG, "The input data may be is error, Mode not special. Do nothing"
									+ ", lastMode: " + lastMode
									+ ", mode: " + mode);
							break;
					}
					break;

				case ApplicationLayer.SLEEP_MODE_START_SLEEP:
					switch(mode) {
						case ApplicationLayer.SLEEP_MODE_START_DEEP_SLEEP:
							chartDatas.add(new SleepChartView.ChartData(minutes, SLEEP_MODE_DISPLAY_HIGHT_DEEP, SLEEP_MODE_DISPLAY_COLOR_DEEP));
							break;
						case ApplicationLayer.SLEEP_MODE_START_WAKE:
							chartDatas.add(new SleepChartView.ChartData(minutes, SLEEP_MODE_DISPLAY_HIGHT_AWAKE, SLEEP_MODE_DISPLAY_COLOR_AWAKE));
							break;
						case ApplicationLayer.SLEEP_MODE_START_SLEEP:
							if(D) Log.e(TAG, "The input data may be is error, Same mode, Do nothing"
									+ ", lastMode: " + lastMode
									+ ", mode: " + mode);
							break;
						default:
							if(D) Log.e(TAG, "The input data may be is error, Mode not special. Do nothing"
									+ ", lastMode: " + lastMode
									+ ", mode: " + mode);
							break;
					}
					break;
				case ApplicationLayer.SLEEP_MODE_START_DEEP_SLEEP:
					switch(mode) {
						case ApplicationLayer.SLEEP_MODE_START_SLEEP:
							chartDatas.add(new SleepChartView.ChartData(minutes, SLEEP_MODE_DISPLAY_HIGHT_LIGHT, SLEEP_MODE_DISPLAY_COLOR_LIGHT));
							break;
						case ApplicationLayer.SLEEP_MODE_START_WAKE:
							chartDatas.add(new SleepChartView.ChartData(minutes, SLEEP_MODE_DISPLAY_HIGHT_AWAKE, SLEEP_MODE_DISPLAY_COLOR_AWAKE));
							break;
						case ApplicationLayer.SLEEP_MODE_START_DEEP_SLEEP:
							if(D) Log.e(TAG, "The input data may be is error, Same mode, Do nothing"
									+ ", lastMode: " + lastMode
									+ ", mode: " + mode);
							break;
						default:
							if(D) Log.e(TAG, "The input data may be is error, Mode not special. Do nothing"
									+ ", lastMode: " + lastMode
									+ ", mode: " + mode);
							break;
					}
					break;
			}
			lastMode = mode;
			lastMinutes = minutes;
		}
		int totalTime = WristbandCalculator.MAX_MINUTE - WristbandCalculator.START_SLEEP_TIME_MINUTE + WristbandCalculator.END_SLEEP_TIME_MINUTE;

		//Axis axisY = new Axis();
		if (hasAxesNames) {
			int minMinute = (int)chartDatas.get(0).X;
			int maxMinute = (int)chartDatas.get(chartDatas.size() - 1).X;
			int j = 0;
			for(int i = 0; i < 5; i++) {

				if(i == 0) {
					j = minMinute;
				} else if(i == 4) {
					j = maxMinute;
				} else {
					j = minMinute + ((maxMinute - minMinute) / 4) * i;
				}
				int hour;
				if(j > (WristbandCalculator.MAX_MINUTE -WristbandCalculator.START_SLEEP_TIME_MINUTE)) {
					hour = (j - (WristbandCalculator.MAX_MINUTE -WristbandCalculator.START_SLEEP_TIME_MINUTE)) / 60;
				} else {
					hour = (j + WristbandCalculator.START_SLEEP_TIME_MINUTE) / 60;
				}
				int minute = j % 60;

				Log.d(TAG, "j: " + j
						+ ", minMinute: " + minMinute
						+ ", maxMinute: " + maxMinute
						+ ", hour: " + hour
						+ ", minute: " + minute);
				String hourStr = String.valueOf(hour).length() == 1
						? "0" + String.valueOf(hour)
						: String.valueOf(hour);
				String minuteStr = String.valueOf(minute).length() == 1
						? "0" + String.valueOf(minute)
						: String.valueOf(minute);
				axisDatas.add(new SleepChartView.AxisValue(j, hourStr + ":" + minuteStr));
			}
		}

		SleepChartView.SleepChartData data = new SleepChartView.SleepChartData(chartDatas, axisDatas);

		return data;
	}


	public SleepChartView.SleepChartData getSpecialSleepNewUIData() {
		if(D) Log.d(TAG, "getSpecialSleepNewUIData");
		for(SleepData sl: mSleeps) {
			Log.d(TAG, "getSpecialSleepNewUIData, sort data. "
					+ WristbandCalculator.toString(sl));
		}

		ArrayList<Integer> Minutes = new ArrayList<>();

		ArrayList<SleepChartView.ChartData> chartDatas = new ArrayList<>();
		ArrayList<SleepChartView.AxisValue> axisDatas = new ArrayList<>();


		int lastMode = -1;
		int lastMinutes = -1;
		for(int i = 0; i < mSleeps.size(); i ++) {
			SleepData sl = mSleeps.get(i);
			int mode = sl.getMode();
			int minutes = sl.getMinutes();
			if (D) Log.d(TAG, "lastMode: " + lastMode
					+ ", mode: " + mode
					+ ", minutes: " + minutes);
			Minutes.add(minutes);
			if(lastMode == -1) {
				// initial first value
				//deepSleepValues.add(new PointValue(0, 0));
				//lightSleepValues.add(new PointValue(0, 0));
				//awakeValues.add(new PointValue(0, 0));
				if(mSleeps.size() != 1) {
					// if start with first minutes
					switch(mode) {
						case ApplicationLayer.SLEEP_MODE_START_WAKE:
							chartDatas.add(new SleepChartView.ChartData(minutes, SLEEP_MODE_DISPLAY_HIGHT_AWAKE, SLEEP_MODE_DISPLAY_COLOR_AWAKE));
							break;
						case ApplicationLayer.SLEEP_MODE_START_SLEEP:
							chartDatas.add(new SleepChartView.ChartData(minutes, SLEEP_MODE_DISPLAY_HIGHT_LIGHT, SLEEP_MODE_DISPLAY_COLOR_LIGHT));
							break;
						case ApplicationLayer.SLEEP_MODE_START_DEEP_SLEEP:
							chartDatas.add(new SleepChartView.ChartData(minutes, SLEEP_MODE_DISPLAY_HIGHT_DEEP, SLEEP_MODE_DISPLAY_COLOR_DEEP));
							break;
					}
				}

				lastMode = mode;
				lastMinutes = minutes;
				continue;
			}
			switch(lastMode) {
				case ApplicationLayer.SLEEP_MODE_START_WAKE:
					switch(mode) {
						case ApplicationLayer.SLEEP_MODE_START_SLEEP:
							chartDatas.add(new SleepChartView.ChartData(minutes, SLEEP_MODE_DISPLAY_HIGHT_LIGHT, SLEEP_MODE_DISPLAY_COLOR_LIGHT));
							break;

						default:
							if(D) Log.e(TAG, "The input data may be is error"
									+ ", lastMode: " + lastMode
									+ ", mode: " + mode);
							return null;
					}
					break;

				case ApplicationLayer.SLEEP_MODE_START_SLEEP:
					switch(mode) {
						case ApplicationLayer.SLEEP_MODE_START_DEEP_SLEEP:
							chartDatas.add(new SleepChartView.ChartData(minutes, SLEEP_MODE_DISPLAY_HIGHT_DEEP, SLEEP_MODE_DISPLAY_COLOR_DEEP));
							break;
						case ApplicationLayer.SLEEP_MODE_START_WAKE:
							chartDatas.add(new SleepChartView.ChartData(minutes, SLEEP_MODE_DISPLAY_HIGHT_AWAKE, SLEEP_MODE_DISPLAY_COLOR_AWAKE));
							break;
						default:
							if(D) Log.e(TAG, "The input data may be is error"
									+ ", lastMode: " + lastMode
									+ ", mode: " + mode);
							return null;
					}
					break;
				case ApplicationLayer.SLEEP_MODE_START_DEEP_SLEEP:
					switch(mode) {
						case ApplicationLayer.SLEEP_MODE_START_SLEEP:
							chartDatas.add(new SleepChartView.ChartData(minutes, SLEEP_MODE_DISPLAY_HIGHT_LIGHT, SLEEP_MODE_DISPLAY_COLOR_LIGHT));
							break;
						case ApplicationLayer.SLEEP_MODE_START_WAKE:
							chartDatas.add(new SleepChartView.ChartData(minutes, SLEEP_MODE_DISPLAY_HIGHT_AWAKE, SLEEP_MODE_DISPLAY_COLOR_AWAKE));
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
		int totalTime = WristbandCalculator.MAX_MINUTE - WristbandCalculator.START_SLEEP_TIME_MINUTE + WristbandCalculator.END_SLEEP_TIME_MINUTE;

		//Axis axisY = new Axis();
		if (hasAxesNames) {
			int minMinute = (int)chartDatas.get(0).X;
			int maxMinute = (int)chartDatas.get(chartDatas.size() - 1).X;
			int j = 0;
			for(int i = 0; i < 5; i++) {

				if(i == 0) {
					j = minMinute;
				} else if(i == 4) {
					j = maxMinute;
				} else {
					j = minMinute + ((maxMinute - minMinute) / 4) * i;
				}
				int hour;
				if(j > (WristbandCalculator.MAX_MINUTE -WristbandCalculator.START_SLEEP_TIME_MINUTE)) {
					hour = (j - (WristbandCalculator.MAX_MINUTE -WristbandCalculator.START_SLEEP_TIME_MINUTE)) / 60;
				} else {
					hour = (j + WristbandCalculator.START_SLEEP_TIME_MINUTE) / 60;
				}
				int minute = j % 60;

				Log.d(TAG, "j: " + j
						+ ", minMinute: " + minMinute
						+ ", maxMinute: " + maxMinute
						+ ", hour: " + hour
						+ ", minute: " + minute);
				String hourStr = String.valueOf(hour).length() == 1
						? "0" + String.valueOf(hour)
						: String.valueOf(hour);
				String minuteStr = String.valueOf(minute).length() == 1
						? "0" + String.valueOf(minute)
						: String.valueOf(minute);
				axisDatas.add(new SleepChartView.AxisValue(j, hourStr + ":" + minuteStr));
			}
		}

		SleepChartView.SleepChartData data = new SleepChartView.SleepChartData(chartDatas, axisDatas);

		return data;
	}

	public LineChartData getSpecialSleepLineData() {
		if(D) Log.d(TAG, "getSpecialSleepLineData");
		for(SleepData sl: mSleeps) {
			Log.d(TAG, "getSpecialSleepLineData, sort data. "
					+ WristbandCalculator.toString(sl));
		}

		ArrayList<Integer> Minutes = new ArrayList<>();

		List<PointValue> lightSleepValues = new ArrayList<PointValue>();
		List<PointValue> deepSleepValues = new ArrayList<PointValue>();
		List<PointValue> awakeValues = new ArrayList<PointValue>();
		ArrayList<Line> lines = new ArrayList<Line>();


		int lastMode = -1;
		int lastMinutes = -1;
		for(int i = 0; i < mSleeps.size(); i ++) {
			SleepData sl = mSleeps.get(i);
			int mode = sl.getMode();
			int minutes = sl.getMinutes();
			if (D) Log.d(TAG, "lastMode: " + lastMode
					+ ", mode: " + mode
					+ ", minutes: " + minutes);
			Minutes.add(minutes);
			if(lastMode == -1) {
				// initial first value
				//deepSleepValues.add(new PointValue(0, 0));
				//lightSleepValues.add(new PointValue(0, 0));
				//awakeValues.add(new PointValue(0, 0));
				if(mSleeps.size() != 1) {
					// if start with first minutes
					switch(mode) {
						case ApplicationLayer.SLEEP_MODE_START_WAKE:
							awakeValues.add(new PointValue(minutes, 0));
							awakeValues.add(new PointValue(minutes, SLEEP_MODE_DISPLAY_HIGHT_AWAKE));
							break;
						case ApplicationLayer.SLEEP_MODE_START_SLEEP:
							lightSleepValues.add(new PointValue(minutes, 0));
							lightSleepValues.add(new PointValue(minutes, SLEEP_MODE_DISPLAY_HIGHT_LIGHT));
							break;
						case ApplicationLayer.SLEEP_MODE_START_DEEP_SLEEP:
							deepSleepValues.add(new PointValue(minutes, 0));
							deepSleepValues.add(new PointValue(minutes, SLEEP_MODE_DISPLAY_HIGHT_DEEP));
							break;
					}
				}

				lastMode = mode;
				lastMinutes = minutes;
				continue;
			}
			switch(lastMode) {
				case ApplicationLayer.SLEEP_MODE_START_WAKE:
					switch(mode) {
						case ApplicationLayer.SLEEP_MODE_START_SLEEP:
							if(i != (mSleeps.size() - 1)) {
								lightSleepValues.add(new PointValue(minutes, 0));
								lightSleepValues.add(new PointValue(minutes, SLEEP_MODE_DISPLAY_HIGHT_LIGHT));
							} else {
								if(D) Log.e(TAG, "May be a error sleep data.");
							}
							awakeValues.add(new PointValue(minutes, SLEEP_MODE_DISPLAY_HIGHT_AWAKE));
							awakeValues.add(new PointValue(minutes, 0));
							break;
						default:
							if(D) Log.e(TAG, "The input data may be is error"
									+ ", lastMode: " + lastMode
									+ ", mode: " + mode);
							return null;
					}
					break;

				case ApplicationLayer.SLEEP_MODE_START_SLEEP:
					switch(mode) {
						case ApplicationLayer.SLEEP_MODE_START_DEEP_SLEEP:
							if(i != (mSleeps.size() - 1)) {
								deepSleepValues.add(new PointValue(minutes, 0));
								deepSleepValues.add(new PointValue(minutes, SLEEP_MODE_DISPLAY_HIGHT_DEEP));
							} else {
								if(D) Log.e(TAG, "May be a error sleep data.");
							}
							lightSleepValues.add(new PointValue(minutes, SLEEP_MODE_DISPLAY_HIGHT_LIGHT));
							lightSleepValues.add(new PointValue(minutes, 0));
							break;
						case ApplicationLayer.SLEEP_MODE_START_WAKE:
							if(i != (mSleeps.size() - 1)) {
								awakeValues.add(new PointValue(minutes, 0));
								awakeValues.add(new PointValue(minutes, SLEEP_MODE_DISPLAY_HIGHT_AWAKE));
							}

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
				case ApplicationLayer.SLEEP_MODE_START_DEEP_SLEEP:
					switch(mode) {
						case ApplicationLayer.SLEEP_MODE_START_SLEEP:
							if(i != (mSleeps.size() - 1)) {
								lightSleepValues.add(new PointValue(minutes, 0));
								lightSleepValues.add(new PointValue(minutes, SLEEP_MODE_DISPLAY_HIGHT_LIGHT));
							}  else {
								if(D) Log.e(TAG, "May be a error sleep data.");
							}
							deepSleepValues.add(new PointValue(minutes, SLEEP_MODE_DISPLAY_HIGHT_DEEP));
							deepSleepValues.add(new PointValue(minutes, 0));
							break;
						case ApplicationLayer.SLEEP_MODE_START_WAKE:
							if(i != (mSleeps.size() - 1)) {
								awakeValues.add(new PointValue(minutes, 0));
								awakeValues.add(new PointValue(minutes, SLEEP_MODE_DISPLAY_HIGHT_AWAKE));
							}

							deepSleepValues.add(new PointValue(minutes, SLEEP_MODE_DISPLAY_HIGHT_DEEP));
							deepSleepValues.add(new PointValue(minutes, 0));
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
		int totalTime = WristbandCalculator.MAX_MINUTE - WristbandCalculator.START_SLEEP_TIME_MINUTE + WristbandCalculator.END_SLEEP_TIME_MINUTE;
		/*
		if(lastMinutes < totalTime) {
			deepSleepValues.add(new PointValue(lastMinutes, 0));
			lightSleepValues.add(new PointValue(lastMinutes, 0));
			awakeValues.add(new PointValue(lastMinutes, 0));
		} else {
			deepSleepValues.add(new PointValue(totalTime, 0));
			lightSleepValues.add(new PointValue(totalTime, 0));
			awakeValues.add(new PointValue(totalTime, 0));
		}*/

		// Awake
		Line line1 = new Line(awakeValues);
		line1.setColor(SLEEP_MODE_DISPLAY_COLOR_AWAKE);
		line1.setCubic(false);
		line1.setFilled(true);
		line1.setHasLabels(false);
		line1.setHasLabelsOnlyForSelected(false);
		line1.setHasLines(true);
		line1.setHasPoints(false);
		line1.setAreaTransparency(255);

		Line line2 = new Line(lightSleepValues);
		line2.setColor(SLEEP_MODE_DISPLAY_COLOR_LIGHT);
		line2.setCubic(false);
		line2.setFilled(true);
		line2.setHasLabels(false);
		line2.setHasLabelsOnlyForSelected(false);
		line2.setHasLines(true);
		line2.setHasPoints(false);
		line2.setAreaTransparency(255);

		Line line3 = new Line(deepSleepValues);
		line3.setColor(SLEEP_MODE_DISPLAY_COLOR_DEEP);
		line3.setCubic(false);
		line3.setFilled(true);
		line3.setHasLabels(false);
		line3.setHasLabelsOnlyForSelected(false);
		line3.setHasLines(true);
		line3.setHasPoints(false);
		//line1.setSquare(true);
		line3.setAreaTransparency(255);

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
				for(int i: Minutes) {
					int hour;
					if(i > (WristbandCalculator.MAX_MINUTE -WristbandCalculator.START_SLEEP_TIME_MINUTE)) {
						hour = (i - (WristbandCalculator.MAX_MINUTE -WristbandCalculator.START_SLEEP_TIME_MINUTE)) / 60;
					} else {
						hour = (i + WristbandCalculator.START_SLEEP_TIME_MINUTE) / 60;
					}
					int minute = i % 60;
					String hourStr = String.valueOf(hour).length() == 1
							? "0" + String.valueOf(hour)
							: String.valueOf(hour);
					String minuteStr = String.valueOf(minute).length() == 1
							? "0" + String.valueOf(minute)
							: String.valueOf(minute);
					axisValues.add(new AxisValue(i).setLabel(hourStr + ":" + minuteStr));
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


	public static LineChartData getSpecialEmptySleepLineData() {
		if(D) Log.d(TAG, "getSpecialEmptySleepLineData");
		List<PointValue> values = new ArrayList<PointValue>();
		List<AxisValue> axisValues = new ArrayList<AxisValue>();
		for(int i = 0;
			i <= ((WristbandCalculator.MAX_MINUTE - WristbandCalculator.START_SLEEP_TIME_MINUTE) + WristbandCalculator.END_SLEEP_TIME_MINUTE);
			i++) {
			if(i % 60 == 0
					|| i == (WristbandCalculator.MAX_MINUTE - WristbandCalculator.START_SLEEP_TIME_MINUTE) + WristbandCalculator.END_SLEEP_TIME_MINUTE) {
				int hour;
				if(i > (WristbandCalculator.MAX_MINUTE - WristbandCalculator.START_SLEEP_TIME_MINUTE)) {
					hour = (i - (WristbandCalculator.MAX_MINUTE - WristbandCalculator.START_SLEEP_TIME_MINUTE)) / 60;
				} else {
					hour = (i + WristbandCalculator.START_SLEEP_TIME_MINUTE) / 60;
				}
				int minute = i % 60;
				if(D) Log.d(TAG, "hour: " + hour + "minute: " + minute);
				String hourStr = String.valueOf(hour).length() == 1
						? "0" + String.valueOf(hour)
						: String.valueOf(hour);
				String minuteStr = String.valueOf(minute).length() == 1
						? "0" + String.valueOf(minute)
						: String.valueOf(minute);
				axisValues.add(new AxisValue(i).setLabel(hourStr + ":" + minuteStr));
				values.add(new PointValue(i, 0));
			}
		}


		Line line = new Line(values);
		ArrayList<Line> lines = new ArrayList<Line>();
		lines.add(line);
		LineChartData data = new LineChartData(lines);
		if (true) {
			Axis axisX = new Axis();
			//Axis axisY = new Axis();
			if (true) {
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
