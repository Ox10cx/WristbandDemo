package com.realsil.android.wristbanddemo.applicationlayer;

import android.util.Log;

public class ApplicationLayerSportItemPacket {
	// Parameters
	private int mOffset;			// 11bits
	private int mMode;				// 2bits
	private int mStepCount;			// 12bits
	private int mActiveTime;		// 4bits
	private int mCalory;			// 19bits
	private int mDistance;			// 16bits
	
	// Packet Length
	public final static int SPORT_ITEM_LENGTH = 8;
	
	public boolean parseData(byte[] data) {
		// check header length
		if(data.length < SPORT_ITEM_LENGTH) {
			return false;
		}
		mOffset = ((data[0] & 0xff) << 3) | ((data[1] >> 5) & 0x07);// here must be care shift operation of negative
		mMode = (data[1] >> 3) & 0x03;// here must be care shift operation of negative
		mStepCount = ((data[1] & 0x07) << 9) | (data[2] << 1) & 0x1fe | ((data[3] >> 7) & 0x01);// here must be care shift operation of negative
		mActiveTime = (data[3] >> 3) & 0x0f;// here must be care shift operation of negative
		mCalory = ((data[3] & 0x07) << 16) | (data[4] << 8) & 0xff00 | (data[5] & 0xff);// here must be care shift operation of negative
		mDistance = ((data[6] << 8) | (data[7] & 0xff)) & 0xffff;// here must be care shift operation of negative
		Log.e("123", "mOffset: " + mOffset +
				", mMode:" + mMode +
				", mStepCount:" + mStepCount +
				", mActiveTime:" + mActiveTime +
				", mCalory:" + mCalory +
				", mDistance:" + mDistance);
		return true;
	}

	public int getOffset() {
		return mOffset;
	}

	public int getMode() {
		return mMode;
	}

	public int getStepCount() {
		return mStepCount;
	}

	public int getActiveTime() {
		return mActiveTime;
	}

	public int getCalory() {
		return mCalory;
	}

	public int getDistance() {
		return mDistance;
	}
}
