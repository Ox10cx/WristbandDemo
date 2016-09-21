package com.realsil.android.wristbanddemo.applicationlayer;

import android.util.Log;

public class ApplicationLayerSleepItemPacket {

	// Parameters
	private int mMinutes;			// 16bits
	private int mMode;				// 4bits
	
	// Packet Length
	public final static int SLEEP_ITEM_LENGTH = 4;
	
	public boolean parseData(byte[] data) {
		// check header length
		if(data.length < SLEEP_ITEM_LENGTH) {
			return false;
		}
		mMinutes = ((data[0] << 8) | (data[1] & 0xff)) & 0xffff;// here must be care shift operation of negative
		mMode = data[3] & 0x0f;// here must be care shift operation of negative
		return true;
	}

	public int getMinutes() {
		return mMinutes;
	}

	public int getMode() {
		return mMode;
	}
}
