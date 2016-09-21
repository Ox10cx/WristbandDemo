package com.realsil.android.wristbanddemo.applicationlayer;

public class ApplicationLayerRecentlySportPacket {
	// Parameters
	private byte mMode;					// 1byte
	private int mActiveTime;			// 1byte
	private long mCalory;				// 4byte
	private int mStepTarget;			// 2byte
	private int mDistance;				// 2byte
	
	
	// Packet Length
	private final static int RECENTLY_SPORT_HEADER_LENGTH = 10;
	
	public ApplicationLayerRecentlySportPacket(byte mode, int activeTime, long calory, int step, int distance) {
		mMode = mode;
		mActiveTime = activeTime;
		mCalory = calory;
		mStepTarget = step;
		mDistance = distance;
	}
	
	public byte[] getPacket() {
		byte[] data = new byte[RECENTLY_SPORT_HEADER_LENGTH];
		data[0] = mMode;
		data[1] = (byte)(mActiveTime & 0xff);
		data[2] = (byte) ((mCalory >> 24) & 0xff);
		data[3] = (byte) ((mCalory >> 16) & 0xff);
		data[4] = (byte) ((mCalory >> 8) & 0xff);
		data[5] = (byte)(mCalory & 0xff);
		data[6] = (byte) ((mStepTarget >> 8) & 0xff);
		data[7] = (byte)(mStepTarget & 0xff);
		data[8] = (byte) ((mDistance >> 8) & 0xff);
		data[9] = (byte)(mDistance & 0xff);
        return data;
	}
}
