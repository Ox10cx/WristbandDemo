package com.realsil.android.wristbanddemo.applicationlayer;

import android.util.Log;

public class ApplicationLayerAlarmPacket {
	// Parameters
	private int mYear;		// 6bits
	private int mMonth;		// 4bits
	private int mDay;		// 5bits
	private int mHour;		// 5bits
	private int mMinute;	// 6bits
	private int mId;		// 3bits
	private byte mDayFlags;	// 7bits
	
	// Day Flags
	public final static byte REPETITION_NULL 	= 0x00;
	public final static byte REPETITION_MON 	= 0x01;
	public final static byte REPETITION_TUES 	= 0x02;
	public final static byte REPETITION_WED 	= 0x04;
	public final static byte REPETITION_THU 	= 0x08;
	public final static byte REPETITION_FRI 	= 0x10;
	public final static byte REPETITION_SAT 	= 0x20;
	public final static byte REPETITION_SUN 	= 0x40;
	
	// Packet Length
	public final static int ALARM_HEADER_LENGTH = 5;
	
	/**
	 * Alarm object.
	 * 
	 * @param year		the alarm time of year.
	 * @param mon		the alarm time of mon.
	 * @param day		the alarm time of day.
	 * @param hour		the alarm time of hour.
	 * @param min		the alarm time of min.
	 * @param id		the alarm id.
	 * @param dayflags	the repetition flag
	 * */
	public ApplicationLayerAlarmPacket(int year, int mon, int day, int hour, int min, int id, byte dayflags) {
		mYear = year - 2000;// year start from 2000
		mMonth = mon;
		mDay = day;
		mHour = hour;
		mMinute = min;
		mId = id;
		mDayFlags = dayflags;
		Log.d("ApplicationLayerAlarmPacket", "mYear: " + mYear + ", mMonth: " + mMonth + ", mDay: " + mDay
				+ ", mHour: " + mHour + ", mMinute: " + mMinute);
	}
	public ApplicationLayerAlarmPacket() {
		
	}

	public void set(ApplicationLayerAlarmPacket alarm) {
		mYear = alarm.getYear();
		mMonth = alarm.getMonth();
		mDay = alarm.getDay();
		mHour = alarm.getHour();
		mMinute = alarm.getMinute();
		mId = alarm.getId();
		mDayFlags = alarm.getDayFlags();
	}
	
	public byte[] getPacket() {
		byte[] data = new byte[ALARM_HEADER_LENGTH];
		data[0] = (byte)((mYear<<2) | (mMonth>>2));
		data[1] = (byte)((mMonth<<6) | (mDay<<1) | (mHour>>4));
		data[2] = (byte)((mHour<<4) | (mMinute>>2));
		data[3] = (byte)((mMinute<<6) | (mId<<3) | (0x00));
		data[4] = (byte)(mDayFlags | 0x00);
		Log.e("getPacket", "mYear: " + mYear +
				", mMonth: " + mMonth +
				", mDay: " + mDay +
				", mHour: " + mHour +
				", mMinute: " + mMinute +
				", mId: " + mId +
				", mDayFlags: " + mDayFlags);
        return data;
	}
	
	public boolean parseData(byte[] data) {
		mYear = (data[0] >> 2) & 0x3f;
		mMonth = ((data[0] << 2) & 0x0f) | ((data[1] >> 6) & 0x03);
		mDay = (data[1] >> 1) & 0x1f;
		mHour = ((data[1] << 4) & 0x10) | ((data[2] >> 4) & 0x0f);
		mMinute = ((data[2] << 2) & 0x3f) | ((data[3] >> 6) & 0x03);
		mId = (data[3] >> 3) & 0x07;
		mDayFlags = (byte) (data[4] & 0x7f);
		Log.e("parseData", "mYear: " + mYear + 
				", mMonth: " + mMonth + 
				", mDay: " + mDay + 
				", mHour: " + mHour + 
				", mMinute: " + mMinute + 
				", mId: " + mId + 
				", mDayFlags: " + mDayFlags);
		return true;
	}

	public String toString() {
		String str = "mYear: " + mYear +
				", mMonth: " + mMonth +
				", mDay: " + mDay +
				", mHour: " + mHour +
				", mMinute: " + mMinute +
				", mId: " + mId +
				", mDayFlags: " + mDayFlags;
		return str;
	}

	public int getYear() {
		return mYear;
	}

	public int getMonth() {
		return mMonth;
	}

	public int getDay() {
		return mDay;
	}

	public int getHour() {
		return mHour;
	}

	public int getMinute() {
		return mMinute;
	}

	public int getId() {
		return mId;
	}

	public byte getDayFlags() {
		return mDayFlags;
	}

	public void setId(int mId) {
		this.mId = mId;
	}
}
