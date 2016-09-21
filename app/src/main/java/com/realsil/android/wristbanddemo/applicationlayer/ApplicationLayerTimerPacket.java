package com.realsil.android.wristbanddemo.applicationlayer;

public class ApplicationLayerTimerPacket {
	
	// Parameters
	private int mYear;		// 6bits
	private int mMonth;		// 4bits
	private int mDay;		// 5bits
	private int mHour;		// 5bits
	private int mMinute;	// 6bits
	private int mSecond;	// 6bits
	
	// Packet Length
	private final static int TIMER_HEADER_LENGTH = 4;
	
	public ApplicationLayerTimerPacket(int year, int mon, int day, int hour, int min, int sec) {
		mYear = year - 2000;// year start from 2000
		mMonth = mon;
		mDay = day;
		mHour = hour;
		mMinute = min;
		mSecond = sec;
	}
	
	public byte[] getPacket() {
		byte[] time_data = new byte[TIMER_HEADER_LENGTH];
		time_data[0] = (byte)((mYear<<2) | (mMonth>>2));
        time_data[1] = (byte)((mMonth<<6) | (mDay<<1) | (mHour>>4));
        time_data[2] = (byte)((mHour<<4) | (mMinute>>2));
        time_data[3] = (byte)((mMinute<<6) | (mSecond));
        return time_data;
	}
		
}
