package com.realsil.android.wristbanddemo.applicationlayer;

import android.util.Log;

public class ApplicationLayerTodaySportPacket {

	// Parameters
	private long mStepTarget;			// 4byte
	private long mDistance;				// 4byte
	private long mCalory;				// 4byte
	
	// Packet Length
	private final static int TODAY_SPORT_HEADER_LENGTH = 12;
	
	public ApplicationLayerTodaySportPacket(long step, long distance, long calory) {
		mStepTarget = step;
		mDistance = distance;
		mCalory = calory;
	}
	
	public byte[] getPacket() {
		Log.d("ApplicationLayerTodaySportPacket", "mStepTarget: " + mStepTarget
				+ ", mDistance: " + mDistance
				+ ", mCalory: " + mCalory);
		byte[] data = new byte[TODAY_SPORT_HEADER_LENGTH];
		data[0] = (byte) ((mStepTarget >> 24) & 0xff);
		data[1] = (byte) ((mStepTarget >> 16) & 0xff);
		data[2] = (byte) ((mStepTarget >> 8) & 0xff);
		data[3] = (byte)(mStepTarget & 0xff);
		data[4] = (byte) ((mDistance >> 24) & 0xff);
		data[5] = (byte) ((mDistance >> 16) & 0xff);
		data[6] = (byte) ((mDistance >> 8) & 0xff);
		data[7] = (byte)(mDistance & 0xff);
		data[8] = (byte) ((mCalory >> 24) & 0xff);
		data[9] = (byte) ((mCalory >> 16) & 0xff);
		data[10] = (byte) ((mCalory >> 8) & 0xff);
		data[11] = (byte)(mCalory & 0xff);
        return data;
	}
}
