package com.realsil.android.wristbanddemo.applicationlayer;

public class ApplicationLayerStepPacket {
	// Parameters
	private long mStepTarget;			// 4byte
	
	// Packet Length
	private final static int STEP_HEADER_LENGTH = 4;
	
	public ApplicationLayerStepPacket(long step) {
		mStepTarget = step;
	}
	
	public byte[] getPacket() {
		byte[] data = new byte[STEP_HEADER_LENGTH];
		data[0] = (byte) ((mStepTarget >> 24) & 0xff);
		data[1] = (byte) ((mStepTarget >> 16) & 0xff);
		data[2] = (byte) ((mStepTarget >> 8) & 0xff);
		data[3] = (byte)(mStepTarget & 0xff);
        return data;
	}
}
