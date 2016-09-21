package com.realsil.android.wristbanddemo.applicationlayer;

public class ApplicationLayerSitPacket {
	// Parameters
	private byte mEnable;			// 1byte
	private int mThreshold;			// 2bytes
	private int mNotifyTime;		// 1byte
	private int mStartNotifyTime;	// 1byte
	private int mStopNotifyTime;	// 1byte
	private byte mDayFlags;			// 1byte
		
	// Day Flags
	public final static byte LONG_SIT_CONTROL_ENABLE 	= 0x01;
	public final static byte LONG_SIT_CONTROL_DISABLE 	= 0x00;
	
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
	private final static int SIT_HEADER_LENGTH = 8;
	
	public ApplicationLayerSitPacket(byte enable, int threshold, int notify, int start, int stop, byte dayflags) {
		mEnable = enable;
		mThreshold = threshold;
		mNotifyTime = notify;
		mStartNotifyTime = start;
		mStopNotifyTime = stop;
		mDayFlags = dayflags;
	}
	
	public byte[] getPacket() {
		byte[] data = new byte[SIT_HEADER_LENGTH];
		data[0] = 0x00;
		data[1] = mEnable;
		data[2] = (byte) ((mThreshold >> 8) & 0xff);
		data[3] = (byte)(mThreshold & 0xff);
		data[4] = (byte)(mNotifyTime & 0xff);
		data[5] = (byte)(mStartNotifyTime & 0xff);
		data[6] = (byte)(mStopNotifyTime & 0xff);
		data[7] = mDayFlags;
        return data;
	}
	
}
