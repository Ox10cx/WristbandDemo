package com.realsil.android.wristbanddemo.applicationlayer;

public class ApplicationLayerTodaySumSportPacket {
	// Parameters
	private int mOffset;			// 1byte
	private long mTotalStep;			// 4byte
	private long mTotalCalory;		// 4byte
	private long mTotalDistance;		// 4byte

	public ApplicationLayerTodaySumSportPacket() {

	}

	public ApplicationLayerTodaySumSportPacket(int offset, long step, long calory, long dis) {
		mOffset = offset;
		mTotalStep = step;
		mTotalCalory = calory;
		mTotalDistance = dis;
	}
	
	// Packet Length
	private final static int SUM_SPORT_HEADER_LENGTH = 13;
	
	public boolean parseData(byte[] data) {
		// check header length
		if(data.length < SUM_SPORT_HEADER_LENGTH) {
			return false;
		}
		mOffset = data[0] & 0xff;
		mTotalStep = (((data[1] & 0xff) << 24) | ((data[2] & 0xff) << 16) | ((data[3] & 0xff) << 8) | (data[4] & 0xff)) & 0x00ffffffffL;
		mTotalCalory = (((data[5] & 0xff) << 24) | ((data[6] & 0xff) << 16) | ((data[7] & 0xff) << 8) | (data[8] & 0xff)) & 0x00ffffffffL;
		mTotalDistance = (((data[9] & 0xff) << 24) | ((data[10] & 0xff) << 16) | ((data[11] & 0xff) << 8) | (data[12] & 0xff)) & 0x00ffffffffL;

        return true;
	}



	public int getOffset() {
		return mOffset;
	}

	public long getTotalStep() {
		return mTotalStep;
	}

	public long getTotalCalory() {
		return mTotalCalory;
	}

	public long getTotalDistance() {
		return mTotalDistance;
	}
}
