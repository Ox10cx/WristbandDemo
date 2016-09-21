package com.realsil.android.wristbanddemo.utility;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import android.util.Log;

import com.realsil.android.wristbanddemo.applicationlayer.ApplicationLayer;
import com.realsil.android.wristbanddemo.applicationlayer.ApplicationLayerTodaySumSportPacket;
import com.realsil.android.wristbanddemo.greendao.SleepData;
import com.realsil.android.wristbanddemo.greendao.SportData;
import com.realsil.android.wristbanddemo.sleep.SleepSubData;
import com.realsil.android.wristbanddemo.sport.SportSubData;


public class WristbandCalculator {
	private static final String TAG = "WristbandCalculator";
    private static final boolean D = true;

	public static final int MAX_MINUTE = 24 * 60;
	public static final int START_SLEEP_TIME_HOUR = 18;
	public static final int START_SLEEP_TIME_MINUTE = START_SLEEP_TIME_HOUR * 60;
	public static final int END_SLEEP_TIME_HOUR = 18;// Change to 17:59
	public static final int END_SLEEP_TIME_MINUTE = END_SLEEP_TIME_HOUR * 60 - 1;
	

    /**
	 * Get the hour-sportData key-value of the special date. The input data must
	 * make sure every offset only have a valid data.
	 *
	 * @param sports the input data
	 * 
	 * @return the total hour-sportData key-value of the date.
	 */
	public static HashMap<Integer, SportSubData> getAllHourDataWithSameDate(List<SportData> sports) {
		// get all the hour
		/*
		for(int i = 1; i <= 24; i ++) {
			for(int j = (4 * (i - 1)) + 1; j <= (4 * i); i ++) {
				//for()
			}
		}*/
		HashMap<Integer, SportSubData> hourData = new HashMap<Integer, SportSubData>();
		
		int hour;
		SportSubData subData;
		for(SportData sp: sports) {
			hour = ((sp.getOffset()) / 4) + 1;// offset start from 0
			if(hourData.get(hour) == null) {
				subData = new SportSubData(sp.getStepCount(),
						sp.getCalory(),
						sp.getDistance());
			} else {
				subData = hourData.get(hour);
				subData.setStepCount(sp.getStepCount() + subData.getStepCount());
				subData.setCalory(sp.getCalory() + subData.getCalory());
				subData.setDistance(sp.getDistance() + subData.getDistance());
			}
			// update the data
			hourData.put(hour, subData);
		}
		// display all the hour-SportSubData key-value
		Iterator iter = hourData.entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry entry = (Map.Entry) iter.next();
			Integer key = (Integer)entry.getKey();
			SportSubData val = (SportSubData)entry.getValue();

			if(D) Log.d(TAG, "Hour: " + key + ", "
				+ val.toString());
		}
		return hourData;
	}
			
    
    /**
	 * Get the hour-sportData key-value of the special date. The input can
	 * be the origin data from database, it will select by date, and unique 
	 * data by the offset.
	 * 
	 * @param y the special year
	 * @param m the special month
	 * @param d the special day
	 * @param sports the input data
	 * 
	 * @return the total hour-sportData key-value of the date.
	 */
	public static HashMap<Integer, SportSubData> getAllHourDataByDate(
			int y, int m, int d,
			List<SportData> sports) {
		// get the special date sport data
		List<SportData> sps = getSubSportDataByDate(y, m, d, sports);
		SportSubData subData = new SportSubData();
		if(sps == null) {
			if(D) Log.e(TAG, "sumOfSportDataByDate, didn't find the data in list by date.");
			return null;
		}
		// get every offset data, and sort by offset
		List<SportData> offsetDataMap = getAllUniqueOffsetDataWithSameDate(sps);
		
		return getAllHourDataWithSameDate(offsetDataMap);
	}
    
    /**
	 * Get sum of the sport data of the special date.
	 * 
	 * @param y the special year
	 * @param m the special month
	 * @param d the special day
	 * @param sports the input data
	 * 
	 * @return the total sport data of the date.
	 */
	public static SportSubData sumOfSportDataByDate(
			int y, int m, int d,
			List<SportData> sports) {
		// get the special date sport data
		List<SportData> sps = getSubSportDataByDate(y, m, d, sports);
		SportSubData subData = new SportSubData();
		if(sps == null) {
			if(D) Log.e(TAG, "sumOfSportDataByDate, didn't find the data in list by date.");
			return null;
		}
		// get every offset data, and sort by offset
		HashMap<Integer, SportData> offsetDataMap = getAllOffsetDataWithSameDate(sps);
		
		// iterator all the offset-SportData key-value
		Iterator iter = offsetDataMap.entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry entry = (Map.Entry) iter.next();
			//Object key = entry.getKey();
			SportData val = (SportData)entry.getValue();
			subData.setStepCount(subData.getStepCount() + val.getStepCount());
			subData.setCalory(subData.getCalory() + val.getCalory());
			subData.setDistance(subData.getDistance() + val.getDistance());
		}
		if(D) Log.i(TAG, "sumOfSportDataByDate, special date with, year: " + y
				+ ", month: " + m
				+ ", day: " + d
				+ ", sub sport data: " + subData.toString());
		return subData;
	}
	
	/**
	 * Get all the Unique SportData(One Offset have only one data), The input data must be the same date,
	 * if not, it will return error result.
	 * 
	 * @param sports the input data
	 * @return All the offset-SportData key-value.
	 */
	public static List<SportData> getAllUniqueOffsetDataWithSameDate(List<SportData> sports) {
		ArrayList<SportData> offsetDataMap = new ArrayList<SportData>();
		HashMap<Integer, ArrayList<SportData>> map = getAllOffsetDataListWithSameDate(sports);
		if(map == null) {
			if(D) Log.e(TAG, "getAllOffsetDataWithSameDate, map empty.");
			return null;
		}
		
		// iterator all the offset-SportData key-value
		Iterator iter = map.entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry entry = (Map.Entry) iter.next();
			Integer key = (Integer)entry.getKey();
			ArrayList<SportData> val = (ArrayList<SportData>)entry.getValue();
			
			SportData offsetVal = findLastDataInOffsetByDate(val);
			offsetDataMap.add(offsetVal);
		}
		
		if(offsetDataMap == null) {
			if(D) Log.e(TAG, "getAllOffsetDataWithSameDate, offsetDataMap empty.");
			return null;
		}
		
		return offsetDataMap;
	}
	
	
	/**
	 * Get all the offset-SportData key-value, The input data must be the same date,
	 * if not, it will return error result.
	 * 
	 * @param sports the input data
	 * @return All the offset-SportData key-value.
	 */
	public static HashMap<Integer, SportData> getAllOffsetDataWithSameDate(List<SportData> sports) {
		HashMap<Integer, SportData> offsetDataMap = new HashMap<Integer, SportData>();
		HashMap<Integer, ArrayList<SportData>> map = getAllOffsetDataListWithSameDate(sports);
		if(map == null) {
			if(D) Log.e(TAG, "getAllOffsetDataWithSameDate, map empty.");
			return null;
		}
		
		// iterator all the offset-SportData key-value
		Iterator iter = map.entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry entry = (Map.Entry) iter.next();
			Integer key = (Integer)entry.getKey();
			ArrayList<SportData> val = (ArrayList<SportData>)entry.getValue();
			SportData offsetVal = findLastDataInOffsetByDate(val);
			offsetDataMap.put(key, offsetVal);
		}
		
		if(offsetDataMap == null) {
			if(D) Log.e(TAG, "getAllOffsetDataWithSameDate, offsetDataMap empty.");
			return null;
		}
		
		return offsetDataMap;
	}
	
	/**
	 * Get all the offset-SportDataList key-value, The input data must be the same date,
	 * if not, it will return error result.
	 * 
	 * @param sports the input data
	 * @return All the offset-SportDataList key-value.
	 */
	public static HashMap<Integer, ArrayList<SportData>> getAllOffsetDataListWithSameDate(List<SportData> sports) {
		ArrayList<Integer> listOffset = new ArrayList<Integer>();
		HashMap<Integer, ArrayList<SportData>> offsetDataMap = new HashMap<Integer, ArrayList<SportData>>();
		for(SportData sp: sports) {
			ArrayList<SportData> sds;
			if(offsetDataMap.get(sp.getOffset()) == null) {
				sds = new ArrayList<SportData>();
			} else {
				sds = offsetDataMap.get(sp.getOffset());
			}

			sds.add(sp);
			offsetDataMap.put(sp.getOffset(), sds);
		}
		if(offsetDataMap.size() == 0) {
			if(D) Log.e(TAG, "getAllOffsetDataListWithSameDate, map empty.");
			return null;
		}
		return offsetDataMap;
	}
	
	
	/**
	 * Get the sub SportData list by the special date, if didn't find, it will return
	 * null.
	 * 
	 * @param sports the input data
	 * @return Sub data list of the special date. 
	 */
	public static List<SportData> getSubSportDataByDate(
			int y, int m, int d,
			List<SportData> sports) {
		ArrayList<SportData> sps = new ArrayList<SportData>();
		for(SportData sp: sports) {
			if(sp.getYear() == y
					&& sp.getMonth() == m 
					&& sp.getDay() == d) {
				sps.add(sp);
			}
		}
		if(sps.size() == 0) {
			if(D) Log.e(TAG, "getSubSportDataByDate, didn't find the data in list by date.");
			return null;
		}
		return sps;
	}
	
	/**
	 * Find the last valid data in the offset, the input data must be with the
	 * same date(year, month, day) and same offset. If input with the various
	 * date, the return result may be not exact. If input with the various offset,
	 * the it will return null.
	 * 
	 * @param sports the input data
	 * @return The last valid data in the offset.
	 */
	public static SportData findLastDataInOffsetByDate(List<SportData> sports) {
		SportData sp;
		// Check the input data
		if(sports == null
				|| sports.size() == 0) {
			if(D) Log.d(TAG, "The input sport data error.");
			return null;
		}
		// Check offset
		if(findValidOffset(sports) == -1) {
			if(D) Log.d(TAG, "The input sport data error with error offset.");
			return null;
		}
		sp = findLastDataInOffsetByDateWithoutCheck(sports);
		
		return sp;
	}
	
	/**
	 * Find the last valid data in the offset, do not do data check, you must
	 * make sure the input data list with same offset.
	 * 
	 * @param sports the input data
	 * @return The last valid data in the offset.
	 */
	public static SportData findLastDataInOffsetByDateWithoutCheck(List<SportData> sports) {
		Date maxDate = sports.get(0).getDate();
		SportData maxSportData = sports.get(0);
		for(SportData sp: sports) {
			if(sp.getDate().compareTo(maxDate) > 0) {
				maxDate = sp.getDate();
				maxSportData = sp;
			}
		}

		if(D) Log.d(TAG, "The last data, " + toString(maxSportData));
		return maxSportData;
	}
	
	public static String toString(SportData sp) {
		return "year: " + sp.getYear()
				+ ", month: " + sp.getMonth()
				+ ", day: " + sp.getDay()
				+ ", offset: " + sp.getOffset()
				+ ", sport mode: " + sp.getMode()
				+ ", stepCount: " + sp.getStepCount()
				+ ", activeTime: " + sp.getActiveTime()
				+ ", calory: " + sp.getCalory()
				+ ", distance: " + sp.getDistance()
				+ ", date: " + sp.getDate();
	}

	public static String toString(List<SportData> sps) {
		String str = new String();
		for(SportData sp: sps) {
			str += toString(sp);
		}
		return str;
	}
	
	/**
	 * Find the valid offset in the list, if the input list has various offset,
	 * it will return -1.
	 * 
	 * @param sports the input data
	 * @return The valid offset.
	 */
	public static int findValidOffset(List<SportData> sports) {
		// Check the input data
		if(sports == null
				|| sports.size() == 0) {
			if(D) Log.e(TAG, "The input sport data error.");
			return -1;
		}
		int offset = sports.get(0).getOffset();
		for(SportData sp: sports) {
			if(offset != sp.getOffset()) {
				if(D) Log.e(TAG, "The input sport data error, have too many offset.");
				return -1;
			}
		}
		if(D) Log.d(TAG, "findValidOffset, offset: " + offset);
		return offset;
	}

	/**
	 * Get nearly offset step data, use to sync to remote
	 *
	 * @param sports the input data, you must make sure the input data sort by
	 *               time, order by ascend.
	 *
	 * @return the total sport data of the date.
	 */
	public static SportData getNearlyOffsetStepData(List<SportData> sports) {
		Calendar c1 = Calendar.getInstance();
		int Year = c1.get(Calendar.YEAR);
		int Month = c1.get(Calendar.MONTH) + 1;
		int Day = c1.get(Calendar.DATE);
		int Hour = c1.get(Calendar.HOUR_OF_DAY);
		int Minutes = c1.get(Calendar.MINUTE);
		int Offset = (Hour * 60 + Minutes) / 15;
		// we should get the last one, in current time.
		for(int i = sports.size() - 1; i >= 0; i --) {
			SportData sportData = sports.get(i);
			if(sportData.getYear() == Year
					&& sportData.getMonth() == Month
					&& sportData.getDay() == Day
					&& sportData.getOffset() == Offset) {
				Log.d(TAG, "getNearlyOffsetStepData: " + toString(sportData));
				return sportData;
			}
		}
		return null;
	}


	public static void adjustTodayTotalStepData(ApplicationLayerTodaySumSportPacket data) {
		Calendar c1  = Calendar.getInstance();
		List<SportData> sports = GlobalGreenDAO.getInstance().loadSportDataByDate(c1.get(Calendar.YEAR),
				c1.get(Calendar.MONTH) + 1,// here need add 1, because it origin range is 0 - 11;
				c1.get(Calendar.DATE));

		SportSubData subData = sumOfSportDataByDate(c1.get(Calendar.YEAR),
				c1.get(Calendar.MONTH) + 1,// here need add 1, because it origin range is 0 - 11;
				c1.get(Calendar.DATE),
				sports);
		if(subData == null) {
			if(D) Log.e(TAG, "adjustTodayTotalStepData with no subData");
			return;
		}
		int offset = data.getOffset();
		long diffStep = data.getTotalStep() - subData.getStepCount();
		long diffCalory = data.getTotalCalory() - subData.getCalory();
		long diffDistance = data.getTotalDistance() - subData.getDistance();
		if(D) Log.d(TAG, "offset: " + offset
				+ ", diffStep: " + diffStep
				+ ", diffCalory: " + diffCalory
				+ ", diffDistance: " + diffDistance);
		// get every offset data, and sort by offset
		HashMap<Integer, SportData> offsetDataMap = getAllOffsetDataWithSameDate(sports);

		if(offsetDataMap == null) {
			if(D) Log.e(TAG, "adjustTodayTotalStepData with no offsetDataMap");
			return;
		}
		// get the sort key
		Object[] keyArray =  offsetDataMap.keySet().toArray();
		Arrays.sort(keyArray);

		for(int i = keyArray.length - 1; i >= 0; i--) {
			int offsetValue = (Integer)keyArray[i];
			SportData sportData = offsetDataMap.get(keyArray[i]);
			if(D) Log.d(TAG, "Key: " + offsetValue + ", data: " + toString(sportData));
			if(D) Log.d(TAG, "diffStep: " + diffStep
					+ ", diffCalory: " + diffCalory
					+ ", diffDistance: " + diffDistance);
			if(offsetValue < offset) {
				if(diffStep == 0
						&& diffCalory == 0
						&& diffDistance == 0) {
					if(D) Log.i(TAG, "adjustTodayTotalStepData OK!");
					break;
				}

				long tempStep;
				long tempCalory;
				long tempDistance;
				// Update step diff
				if(diffStep >= 0) {
					tempStep = sportData.getStepCount() + diffStep;
				} else {
					if(sportData.getStepCount() + diffStep >= 0) {
						tempStep = sportData.getStepCount() + diffStep;
					} else {
						tempStep = 0L;
					}
				}
				// update diff value
				diffStep = (sportData.getStepCount() + diffStep) - tempStep;


				// Update Calory diff
				if(diffCalory >= 0) {
					tempCalory = sportData.getCalory() + diffCalory;
				} else {
					if(sportData.getCalory() + diffCalory >= 0) {
						tempCalory = sportData.getCalory() + diffCalory;
					} else {
						tempCalory = 0L;
					}
				}
				// update diff value
				diffCalory = (sportData.getCalory() + diffCalory) - tempCalory;


				// Update Distance diff
				if(diffDistance >= 0) {
					tempDistance = sportData.getDistance() + diffDistance;
				} else {
					if(sportData.getDistance() + diffDistance >= 0) {
						tempDistance = sportData.getDistance() + diffDistance;
					} else {
						tempDistance = 0L;
					}
				}
				// update diff value
				diffDistance = (sportData.getDistance() + diffDistance) - tempDistance;


				// save the data
				SportData temp = new SportData(null
						, sportData.getYear(), sportData.getMonth(), sportData.getDay()
						, sportData.getOffset()
						, -1// mode
						, (int)tempStep
						, -1// active time
						, (int)tempCalory, (int)tempDistance
						, new Date());
				if(D) Log.d(TAG, "Adjust data: " + toString(temp));
				GlobalGreenDAO.getInstance().saveSportData(temp);
			}
		}


/*
		// iterator all the offset-SportData key-value
		Iterator iter = offsetDataMap.entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry entry = (Map.Entry) iter.next();
			//Object key = entry.getKey();
			SportData val = (SportData)entry.getValue();
			subData.setStepCount(subData.getStepCount() + val.getStepCount());
			subData.setCalory(subData.getCalory() + val.getCalory());
			subData.setDistance(subData.getDistance() + val.getDistance());
		}
		*/
	}
	
	// Sleep Data
	/**
	 * Remove some minute data.
	 * You must make sure the data have be sort.
	 * */
	public static void removeSameMinuteSleepData(List<SleepData> sleeps) {
		int lastMinute = -1;
		SleepData sl;
		if(sleeps != null) {
			for (int i = 0; i < sleeps.size(); i ++) {
				sl = sleeps.get(i);
				if (lastMinute != -1) {
					if (sl.getMinutes() == lastMinute) {
						sleeps.remove(sl);
						if (D) Log.d(TAG, "removeSameMinuteSleepData, lastMinute: " + lastMinute
								+ ", same date: " + toString(sl));
						i = i - 1;
						continue;
					}
				}
				lastMinute = sl.getMinutes();
			}
		}
	}


	/**
	 * Get sum of the sleep data. No Error check
	 * This method is use to calculate the 18:00 PM - 10:00 AM
	 *
	 * @param sleeps the input data
	 *
	 * @return the total sport data of the date.
	 */
	public static SleepSubData sumOfSleepDataByMinutesSpecNoErrorCheck(
			List<SleepData> sleeps) {
		SleepSubData subData = new SleepSubData();

		ArrayList<SleepData> sls = new ArrayList<SleepData>();
		for(SleepData sl: sleeps) {
			sls.add(sl);
		}
		List<SleepData> slss;
		// increase sort the sleep data by minutes
		Collections.sort(sls, new SleepIncreaseComparator());

		// Remove the error minute data
		removeSameMinuteSleepData(sls);

		for(SleepData sl: sls) {
			Log.d(TAG, "sumOfSleepDataByMinutesSpecNoErrorCheck, sort data. "
					+ toString(sl));
		}
		SleepData lastSleepData = null;
		for(SleepData sl: sls) {
			if(lastSleepData == null) {// get the last mode.
				lastSleepData = sl;
			} else {
				boolean needUpdateLast = true;
				switch(lastSleepData.getMode()) {
					case ApplicationLayer.SLEEP_MODE_START_WAKE:
						switch(sl.getMode()) {
							case ApplicationLayer.SLEEP_MODE_START_SLEEP:
								//
								break;

							default:
								if(D) Log.e(TAG, "The input data may be is error"
										+ ", lastSleepData.getMode(): " + lastSleepData.getMode()
										+ ", sl.getMode(): " + sl.getMode());
								needUpdateLast = false;
								break;
						}
						break;

					case ApplicationLayer.SLEEP_MODE_START_SLEEP:
						switch(sl.getMode()) {
							case ApplicationLayer.SLEEP_MODE_START_DEEP_SLEEP:
								subData.setLightSleepTime(subData.getLightSleepTime()
										+ sl.getMinutes() - lastSleepData.getMinutes());
								break;
							case ApplicationLayer.SLEEP_MODE_START_WAKE:
								subData.setAwakeTimes(subData.getAwakeTimes() + 1);
								subData.setLightSleepTime(subData.getLightSleepTime()
										+ sl.getMinutes() - lastSleepData.getMinutes());
								break;
							default:
								if(D) Log.e(TAG, "The input data may be is error"
										+ ", lastSleepData.getMode(): " + lastSleepData.getMode()
										+ ", sl.getMode(): " + sl.getMode());
								needUpdateLast = false;
								break;
						}
						break;
					case ApplicationLayer.SLEEP_MODE_START_DEEP_SLEEP:
						switch(sl.getMode()) {
							case ApplicationLayer.SLEEP_MODE_START_SLEEP:
								subData.setDeepSleepTime(subData.getDeepSleepTime()
										+ sl.getMinutes() - lastSleepData.getMinutes());
								break;
							case ApplicationLayer.SLEEP_MODE_START_WAKE:
								subData.setAwakeTimes(subData.getAwakeTimes() + 1);
								subData.setDeepSleepTime(subData.getDeepSleepTime()
										+ sl.getMinutes() - lastSleepData.getMinutes());
								break;
							default:
								if(D) Log.e(TAG, "The input data may be is error"
										+ ", lastSleepData.getMode(): " + lastSleepData.getMode()
										+ ", sl.getMode(): " + sl.getMode());
								needUpdateLast = false;
								break;
						}
						break;
				}

				if(D) Log.i(TAG, "sumOfSleepDataByMinutesSpecNoErrorCheck"
						+ ", lastSleepData.getMode(): " + lastSleepData.getMode()
						+ ", sl.getMode(): " + sl.getMode()
						+ ", needUpdateLast: " + needUpdateLast
						+ ", sub sleep data: " + subData.toString());
				if(needUpdateLast) {
					// update last sleep data
					lastSleepData = sl;
				}
			}
		}

		if(D) Log.i(TAG, "sumOfSleepDataByMinutesSpecNoErrorCheck"
				+ ", sub sleep data: " + subData.toString());
		return subData;
	}

	/**
	 * Get sum of the sleep data.
	 * This method is use to calculate the 18:00 PM - 10:00 AM
	 *
	 * @param sleeps the input data
	 *
	 * @return the total sport data of the date.
	 */
	public static SleepSubData sumOfSleepDataByMinutesSpec(
			List<SleepData> sleeps) {
		SleepSubData subData = new SleepSubData();

		ArrayList<SleepData> sls = new ArrayList<SleepData>();
		for(SleepData sl: sleeps) {
			sls.add(sl);
		}
		List<SleepData> slss;
		// increase sort the sleep data by minutes
		Collections.sort(sls, new SleepIncreaseComparator());

		// Remove the error minute data
		removeSameMinuteSleepData(sls);

		for(SleepData sl: sls) {
			Log.d(TAG, "sumOfSleepDataByMinutesSpec, sort data. "
					+ toString(sl));
		}
		SleepData lastSleepData = null;
		for(SleepData sl: sls) {
			if(lastSleepData == null) {// get the last mode.
				lastSleepData = sl;
			} else {
				switch(lastSleepData.getMode()) {
					case ApplicationLayer.SLEEP_MODE_START_WAKE:
						switch(sl.getMode()) {
							case ApplicationLayer.SLEEP_MODE_START_SLEEP:
								//
								break;
							default:
								if(D) Log.e(TAG, "The input data may be is error"
										+ ", lastSleepData.getMode(): " + lastSleepData.getMode()
										+ ", sl.getMode(): " + sl.getMode());
								return null;
						}
						break;

					case ApplicationLayer.SLEEP_MODE_START_SLEEP:
						switch(sl.getMode()) {
							case ApplicationLayer.SLEEP_MODE_START_DEEP_SLEEP:
								subData.setLightSleepTime(subData.getLightSleepTime()
										+ sl.getMinutes() - lastSleepData.getMinutes());
								break;
							case ApplicationLayer.SLEEP_MODE_START_WAKE:
								subData.setAwakeTimes(subData.getAwakeTimes() + 1);
								subData.setLightSleepTime(subData.getLightSleepTime()
										+ sl.getMinutes() - lastSleepData.getMinutes());
								break;
							default:
								if(D) Log.e(TAG, "The input data may be is error"
										+ ", lastSleepData.getMode(): " + lastSleepData.getMode()
										+ ", sl.getMode(): " + sl.getMode());
								return null;
						}
						break;
					case ApplicationLayer.SLEEP_MODE_START_DEEP_SLEEP:
						switch(sl.getMode()) {
							case ApplicationLayer.SLEEP_MODE_START_SLEEP:
								subData.setDeepSleepTime(subData.getDeepSleepTime()
										+ sl.getMinutes() - lastSleepData.getMinutes());
								break;
							case ApplicationLayer.SLEEP_MODE_START_WAKE:
								subData.setAwakeTimes(subData.getAwakeTimes() + 1);
								subData.setDeepSleepTime(subData.getDeepSleepTime()
										+ sl.getMinutes() - lastSleepData.getMinutes());
								break;
							default:
								if(D) Log.e(TAG, "The input data may be is error"
										+ ", lastSleepData.getMode(): " + lastSleepData.getMode()
										+ ", sl.getMode(): " + sl.getMode());
								return null;
						}
						break;
				}

				if(D) Log.i(TAG, "sumOfSleepDataByMinutesSpec"
						+ ", lastSleepData.getMode(): " + lastSleepData.getMode()
						+ ", sl.getMode(): " + sl.getMode()
						+ ", sub sleep data: " + subData.toString());

				// update last sleep data
				lastSleepData = sl;
			}
		}

		if(D) Log.i(TAG, "sumOfSleepDataByMinutesSpec"
				+ ", sub sleep data: " + subData.toString());
		return subData;
	}

	/**
	 * Get sum of the sleep data.
	 * 
	 * @param sleeps the input data
	 * 
	 * @return the total sport data of the date.
	 */
	public static SleepSubData sumOfSleepDataByMinutes(
			List<SleepData> sleeps) {
		SleepSubData subData = new SleepSubData();
		
		ArrayList<SleepData> sls = new ArrayList<SleepData>();
		for(SleepData sl: sleeps) {
			sls.add(sl);
		}
		List<SleepData> slss;
		// increase sort the sleep data by minutes
		Collections.sort(sls, new SleepIncreaseComparator());
		
		for(SleepData sl: sls) {
			Log.d(TAG, "sumOfSleepDataByDate, sort data. " 
					+ toString(sl));
		}
		SleepData lastSleepData = null;
		for(SleepData sl: sls) {
			if(lastSleepData == null) {// get the last mode.
				lastSleepData = sl;
			} else {
				switch(lastSleepData.getMode()) {
				case SLEEP_MODE_START_DEEP_SLEEP:
					switch(sl.getMode()) {
					case SLEEP_MODE_START_LIGHT_SLEEP_MODE_2:
						subData.setDeepSleepTime(subData.getDeepSleepTime() 
								+ sl.getMinutes() - lastSleepData.getMinutes());
						break;
					default:
						if(D) Log.e(TAG, "The input data may be is error" 
								+ ", lastSleepData.getMode(): " + lastSleepData.getMode()
								+ ", sl.getMode(): " + sl.getMode());
						return null;
					}
					break;
					
				case SLEEP_MODE_START_LIGHT_SLEEP_MODE_1:
					switch(sl.getMode()) {
					case SLEEP_MODE_START_DEEP_SLEEP:
						subData.setLightSleepTime(subData.getLightSleepTime() 
								+ sl.getMinutes() - lastSleepData.getMinutes());
						break;
					case SLEEP_MODE_EXIT_SLEEP:
						subData.setAwakeTimes(subData.getAwakeTimes() + 1);
						subData.setLightSleepTime(subData.getLightSleepTime() 
								+ sl.getMinutes() - lastSleepData.getMinutes());
						break;
					default:
						if(D) Log.e(TAG, "The input data may be is error" 
								+ ", lastSleepData.getMode(): " + lastSleepData.getMode()
								+ ", sl.getMode(): " + sl.getMode());
						return null;
					}
					break;
				case SLEEP_MODE_START_LIGHT_SLEEP_MODE_2:
					switch(sl.getMode()) {
					case SLEEP_MODE_START_LIGHT_SLEEP_MODE_1:
						subData.setLightSleepTime(subData.getLightSleepTime() 
								+ sl.getMinutes() - lastSleepData.getMinutes());
						break;
					case SLEEP_MODE_EXIT_SLEEP:
						subData.setAwakeTimes(subData.getAwakeTimes() + 1);
						subData.setLightSleepTime(subData.getLightSleepTime() 
								+ sl.getMinutes() - lastSleepData.getMinutes());
						break;
					default:
						if(D) Log.e(TAG, "The input data may be is error" 
								+ ", lastSleepData.getMode(): " + lastSleepData.getMode()
								+ ", sl.getMode(): " + sl.getMode());
						return null;
					}
					break;
				case SLEEP_MODE_START_ENTER_SLEEP:
					switch(sl.getMode()) {
					case SLEEP_MODE_START_LIGHT_SLEEP_MODE_1:
						// do nothing
						break;
					default:
						if(D) Log.e(TAG, "The input data may be is error" 
								+ ", lastSleepData.getMode(): " + lastSleepData.getMode()
								+ ", sl.getMode(): " + sl.getMode());
						return null;
					}
					break;
				case SLEEP_MODE_EXIT_SLEEP:
					switch(sl.getMode()) {
					case SLEEP_MODE_START_ENTER_SLEEP:
						// do nothing
						break;
					default:
						if(D) Log.e(TAG, "The input data may be is error" 
								+ ", lastSleepData.getMode(): " + lastSleepData.getMode()
								+ ", sl.getMode(): " + sl.getMode());
						return null;
					}
					break;
				}
				
				if(D) Log.i(TAG, "sumOfSleepDataByMinutes"
						+ ", lastSleepData.getMode(): " + lastSleepData.getMode()
						+ ", sl.getMode(): " + sl.getMode()
						+ ", sub sleep data: " + subData.toString());
				
				// update last sleep data
				lastSleepData = sl;
			}
		}

		if(D) Log.i(TAG, "sumOfSleepDataByMinutes"
				+ ", sub sleep data: " + subData.toString());
		return subData;
	}
	/**
	 * Get sum of the sleep data of the special date.
	 * This method is use to calculate the 18:00 PM - 10:00 AM
	 *
	 * @param y the special year
	 * @param m the special month
	 * @param d the special day
	 * @param sleeps the input data
	 *
	 * @return the total sport data of the date.
	 */
	public static SleepSubData sumOfSleepDataByDateSpec(
			int y, int m, int d,
			List<SleepData> sleeps) {
		Calendar c1 = Calendar.getInstance();
		c1.set(y, m - 1, d);// here need decrease 1 of month
		c1.add(Calendar.DATE, -1);
		int yesterdayYear = c1.get(Calendar.YEAR);
		int yesterdayMonth = c1.get(Calendar.MONTH) + 1;
		int yesterdayDay = c1.get(Calendar.DATE);
		if(D) Log.d(TAG, "sumOfSleepDataByDateSpec, y: " + y + ", m: " + m + ", d: " + d
				+ ", yesterdayYear: " + yesterdayYear + ", yesterdayMonth: " + yesterdayMonth
				+ ", yesterdayDay: " + yesterdayDay);

		// get the special date sleep data
		ArrayList<SleepData> sls = new ArrayList<>();

		List<SleepData> d1 = getSubSleepDataByDate(yesterdayYear, yesterdayMonth, yesterdayDay, sleeps);
		if(d1 != null) {
			for(SleepData sl: d1) {
				if(sl.getMinutes() >= START_SLEEP_TIME_MINUTE
						&& sl.getMinutes() <= MAX_MINUTE) {
					sls.add(sl);
				}
			}
		}
		List<SleepData> d2 = getSubSleepDataByDate(y, m, d, sleeps);
		if(d2 != null) {
			for(SleepData sl: d2) {
				if(sl.getMinutes() >= 0
						&& sl.getMinutes() <= END_SLEEP_TIME_MINUTE) {
					// Use Deep Copy
					SleepData tmp = new SleepData(sl.getId(), sl.getYear(), sl.getMonth(), sl.getDay(),
							sl.getMinutes(), sl.getMode(), sl.getDate());
					tmp.setMinutes(tmp.getMinutes() + MAX_MINUTE);
					sls.add(tmp);
				}
			}
		}
		if(sls.size() == 0
				|| sls.size() == 1) {
			if(D) Log.e(TAG, "sumOfSleepDataByDate, didn't find the data in list by date, or size equal to 1. sls.size(): " + sls.size());
			return null;
		}

		return sumOfSleepDataByMinutesSpec(sls);
	}

	/**
	 * Get sum of the sleep data of the special date. No Error Check(
	 * This method is use to calculate the 18:00 PM - 10:00 AM
	 *
	 * @param y the special year
	 * @param m the special month
	 * @param d the special day
	 * @param sleeps the input data
	 *
	 * @return the total sport data of the date.
	 */
	public static SleepSubData sumOfSleepDataByDateSpecNoErrorCheck(
			int y, int m, int d,
			List<SleepData> sleeps) {
		Calendar c1 = Calendar.getInstance();
		c1.set(y, m - 1, d);// here need decrease 1 of month
		c1.add(Calendar.DATE, -1);
		int yesterdayYear = c1.get(Calendar.YEAR);
		int yesterdayMonth = c1.get(Calendar.MONTH) + 1;
		int yesterdayDay = c1.get(Calendar.DATE);
		if(D) Log.d(TAG, "sumOfSleepDataByDateSpecNoErrorCheck, y: " + y + ", m: " + m + ", d: " + d
				+ ", yesterdayYear: " + yesterdayYear + ", yesterdayMonth: " + yesterdayMonth
				+ ", yesterdayDay: " + yesterdayDay);

		// get the special date sleep data
		ArrayList<SleepData> sls = new ArrayList<>();

		List<SleepData> d1 = getSubSleepDataByDate(yesterdayYear, yesterdayMonth, yesterdayDay, sleeps);
		if(d1 != null) {
			for(SleepData sl: d1) {
				if(sl.getMinutes() >= START_SLEEP_TIME_MINUTE
						&& sl.getMinutes() <= MAX_MINUTE) {
					sls.add(sl);
				}
			}
		}
		List<SleepData> d2 = getSubSleepDataByDate(y, m, d, sleeps);
		if(d2 != null) {
			for(SleepData sl: d2) {
				if(sl.getMinutes() >= 0
						&& sl.getMinutes() <= END_SLEEP_TIME_MINUTE) {
					// Use Deep Copy
					SleepData tmp = new SleepData(sl.getId(), sl.getYear(), sl.getMonth(), sl.getDay(),
							sl.getMinutes(), sl.getMode(), sl.getDate());
					tmp.setMinutes(tmp.getMinutes() + MAX_MINUTE);
					sls.add(tmp);
				}
			}
		}
		if(sls.size() == 0
				|| sls.size() == 1) {
			if(D) Log.e(TAG, "sumOfSleepDataByDateSpecNoErrorCheck, didn't find the data in list by date, or size equal to 1. sls.size(): " + sls.size());
			return null;
		}

		return sumOfSleepDataByMinutesSpecNoErrorCheck(sls);
	}

	/**
	 * Get sum of the sleep data of the special date.
	 * 
	 * @param y the special year
	 * @param m the special month
	 * @param d the special day
	 * @param sleeps the input data
	 * 
	 * @return the total sport data of the date.
	 */
	public static SleepSubData sumOfSleepDataByDate(
			int y, int m, int d,
			List<SleepData> sleeps) {
		// get the special date sleep data
		List<SleepData> sls = getSubSleepDataByDate(y, m, d, sleeps);
		if(sls == null) {
			if(D) Log.e(TAG, "sumOfSleepDataByDate, didn't find the data in list by date.");
			return null;
		}
		
		return sumOfSleepDataByMinutes(sls);
	}
	
	private static long getDiffTimeWithMinute(Date d1, Date d2) {
		long nd = 1000 * 24 * 60 * 60;// The number of milliseconds in a day
		long nh = 1000 * 60 * 60;// The number of milliseconds in a hour
		long nm = 1000 * 60;// The number of milliseconds in a minute
		long ns = 1000;// The number of milliseconds in a second
		
		long diff = d1.getTime() - d2.getTime();
		long day = diff / nd;// Calculate how many days in diff
		long hour = diff % nd / nh + day * 24;// Calculate how many hours in diff
		long min = diff % nd % nh / nm + day * 24 * 60;// Calculate how many minutes in diff
		//long sec = diff % nd % nh % nm / ns;// Calculate how many seconds in diff
		/*
		if(D) Log.d(TAG, "d1.getTime(): " + d1.getTime()
				+ ", d2.getTime(): " + d2.getTime()
				+ ", diff: " + diff
				+ ", min: " + min);
				*/
		return min;
	}
	
	private long getDiffTimeWithHour(Date d1, Date d2) {
		long nd = 1000 * 24 * 60 * 60;// The number of milliseconds in a day
		long nh = 1000 * 60 * 60;// The number of milliseconds in a hour
		long nm = 1000 * 60;// The number of milliseconds in a minute
		long ns = 1000;// The number of milliseconds in a second
		
		long diff = d1.getTime() - d2.getTime();
		long day = diff / nd;// Calculate how many days in diff
		long hour = diff % nd / nh + day * 24;// Calculate how many hours in diff
		//long min = diff % nd % nh / nm + day * 24 * 60;// Calculate how many minutes in diff
		//long sec = diff % nd % nh % nm / ns;// Calculate how many seconds in diff

		return hour;
	}
	
	public static final int SLEEP_MODE_START_DEEP_SLEEP = 1;
	public static final int SLEEP_MODE_START_LIGHT_SLEEP_MODE_1 = 2;
	public static final int SLEEP_MODE_START_LIGHT_SLEEP_MODE_2 = 3;
	public static final int SLEEP_MODE_START_ENTER_SLEEP = 4;
	public static final int SLEEP_MODE_EXIT_SLEEP = 5;
	
	
	
	public static String toString(SleepData sl) {
		return "year: " + sl.getYear()
				+ ", month: " + sl.getMonth()
				+ ", day: " + sl.getDay()
				+ ", minutes: " + sl.getMinutes()
				+ ", sleep mode: " + sl.getMode()
				+ ", date: " + sl.getDate();
	}
	
	/**
	 * Sleep data list Decrease Comparator class, sort by the minutes.
	 * 
	 */
	public static class SleepDecreaseComparator implements Comparator {

		public int compare(Object arg0, Object arg1) {
			// TODO Auto-generated method stub
			
			return compareSleep((SleepData) arg0, (SleepData) arg1);
		}
		public int compareSleep(SleepData o1, SleepData o2){
			if(o1.getMinutes() > o2.getMinutes()) {
				return -1;
			} else if(o1.getMinutes() < o2.getMinutes()) {
				return 1;
			} else {
				return 0;
			}
		}
	}
	/**
	 * Sleep data list Increase Comparator class, sort by the minutes.
	 * 
	 */
	public static class SleepIncreaseComparator implements Comparator {

		public int compare(Object arg0, Object arg1) {
			// TODO Auto-generated method stub
			
			return compareSleep((SleepData) arg0, (SleepData) arg1);
		}
		public int compareSleep(SleepData o1, SleepData o2){
			if(o1.getMinutes() > o2.getMinutes()) {
				return 1;
			} else if(o1.getMinutes() < o2.getMinutes()) {
				return -1;
			} else {
				return 0;
			}
		}
	}
	
	
	/**
	 * Get the sub SleepData list by the special date, if didn't find, it will return
	 * null.
	 * 
	 * @param sleeps the input data
	 * @return Sub data list of the special date. 
	 */
	public static List<SleepData> getSubSleepDataByDate(
			int y, int m, int d,
			List<SleepData> sleeps) {
		ArrayList<SleepData> sls = new ArrayList<SleepData>();
		for(SleepData sl: sleeps) {
			if(sl.getYear() == y
					&& sl.getMonth() == m 
					&& sl.getDay() == d) {
				sls.add(sl);
			}
		}
		if(sls.size() == 0) {
			if(D) Log.e(TAG, "getSubSleepDataByDate, didn't find the data in list by date.");
			return null;
		}
		return sls;
	}
	
	
	
	
	
	
	
	
	
	
	
	
	/**
	 * Get the max days in the special month.
	 * 
	 * @param year the special year
	 * @param month the special month
	 * @return The max days in the special month.
	 */
	public static int getMonthMaxDays(int year, int month) {
		int maxDays;
		Calendar rightNow = Calendar.getInstance();

		SimpleDateFormat simpleDate = new SimpleDateFormat("yyyy/MM"); 

		try {
			rightNow.setTime(simpleDate.parse(year + "/" + month)); 
		} catch (ParseException e) {
			e.printStackTrace();
		}

		maxDays = rightNow.getActualMaximum(Calendar.DAY_OF_MONTH);
		if(D) Log.d(TAG, "getMonthMaxDays, year: " + year 
				+ ", month: " + month
				+ ", maxDays: " + maxDays);
		
		return maxDays;
	}
	
	/**
	 * Get the week of the special day.
	 * 
	 * @param year the special year
	 * @param month the special month
	 * @param day the special day
	 * 
	 * @return The week of the special day.
	 */
	public static int getWeekOfDay(int year, int month, int day) {
		int week;
		Calendar rightNow = Calendar.getInstance();

		SimpleDateFormat simpleDate = new SimpleDateFormat("yyyy/MM/dd"); 

		try {
			rightNow.setTime(simpleDate.parse(year + "/" + month + "/" + day)); 
		} catch (ParseException e) {
			e.printStackTrace();
		}

		week = rightNow.get(Calendar.DAY_OF_WEEK);
		if(D) Log.d(TAG, "getMonthMaxDays, year: " + year 
				+ ", month: " + month
				+ ", day: " + day
				+ ", week: " + week);
		
		return week;
	}
	
	
	
	
	
}
