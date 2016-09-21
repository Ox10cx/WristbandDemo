package com.realsil.android.wristbanddemo.applicationlayer;

public class ApplicationLayerSumSportPacket {
	// Parameters
	private int mOffset;			// 1byte
	private int mTotalStep;			// 1byte
	private int mTotalCalory;		// 4byte
	private int mTotalDistance;		// 2byte
	private int mRecentlyStep;		// 1byte
	private int mRecentlyCalory;	// 4byte
	private int mRecentlyDistance;	// 2byte
	
	
	// Packet Length
	private final static int SUM_SPORT_HEADER_LENGTH = 16;
	
	public byte[] parseData() {
		byte[] data = new byte[SUM_SPORT_HEADER_LENGTH];
		mOffset = data[0] & 0xff;
		mTotalStep = (((data[1] & 0xff) << 16) | ((data[2] & 0xff) << 8) | (data[3] & 0xff)) & 0xffffff;
		mTotalCalory = (((data[4] & 0xff) << 16) | ((data[5] & 0xff) << 8) | (data[6] & 0xff)) & 0xffffff;
		mTotalDistance = (((data[7] & 0xff) << 16) | ((data[8] & 0xff) << 8) | (data[9] & 0xff)) & 0xffffff;
		mRecentlyStep = (((data[10] & 0xff) << 8) | (data[11] & 0xff)) & 0xffff;
		mRecentlyCalory = (((data[12] & 0xff) << 8) | (data[13] & 0xff)) & 0xffff;
		mRecentlyDistance = (((data[14] & 0xff) << 8) | (data[15] & 0xff)) & 0xffff;
        return data;
	}
}
