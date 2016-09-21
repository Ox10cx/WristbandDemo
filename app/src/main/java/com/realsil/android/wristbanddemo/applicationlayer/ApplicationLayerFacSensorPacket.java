package com.realsil.android.wristbanddemo.applicationlayer;


public class ApplicationLayerFacSensorPacket {


	// Header
	private int mX;			// 16bits
	private int mY;			// 16bits
	private int mZ;			// 16bits
	
	// Packet Length
	private final static int SENSOR_HEADER_LENGTH = 6;
	
	public boolean parseData(byte[] data) {
		// check header length
		if(data.length < SENSOR_HEADER_LENGTH) {
			return false;
		}
		mX = ((data[0] << 8) & 0xFF00) | (data[1] & 0xff);// here must be care shift operation of negative
		mY = ((data[2] << 8) & 0xFF00) | (data[3] & 0xff);// here must be care shift operation of negative
		mZ = ((data[4] << 8) & 0xFF00) | (data[5] & 0xff);// here must be care shift operation of negative
		return true;
	}

	public int getX() {
		return mX;
	}

	public int getY() {
		return mY;
	}

	public int getZ() {
		return mZ;
	}
}
