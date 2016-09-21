package com.realsil.android.wristbanddemo.applicationlayer;

import android.util.Log;

public class ApplicationLayerKeyPacket {
	// Log
	private final static String TAG = "ApplicationLayerKeyPacket";
	private final static boolean D = true;
		
	// Header
	private byte mKey;	// 1Byte, bit(0-7)
	

	private int mPayloadLength;		// 9bit
	
	// Parameters
	private byte[] mKeyData;	// the key data;
	
	// Header Length
	public final static int HEADER_LENGTH = 3;
	
	
    
    /**
	 * Use to parse the Application Layer key Packet.
	 * 
	 * @param data the key data
	 * @return success or false
	 * */
    public boolean parseData(byte[] data) {
    	// check length
		if(data.length < HEADER_LENGTH) {
			return false;
		}
    	mKey = data[0];
    	mPayloadLength = ((data[1] << 8) & 0x100) | (data[2] & 0xff);
    	if(D) Log.d(TAG, "mKey: " + mKey + ", mPayloadLength: " + mPayloadLength);
    	// check length
    	if(data.length < HEADER_LENGTH + mPayloadLength) {
    		return false;
    	}
    	// Save the key data
    	mKeyData = new byte[mPayloadLength];
    	System.arraycopy(data, HEADER_LENGTH, mKeyData, 0, mPayloadLength);
    	
    	return true;
    }
    
    /**
	 * prepare the Transport Layer Packet to send
	 * 
	 * @param data the send key value
	 * @param key the key number
	 * 
	 * @return the integrated Application Layer Key Packet
	 * */
	public static byte[] preparePacket(byte key, byte[] data) {
		byte[] keyData;
		if(data != null) {
			keyData = new byte[data.length + HEADER_LENGTH];
			System.arraycopy(data, 0, keyData, HEADER_LENGTH, data.length);
			// prepare header
			keyData[0] = key;
			keyData[1] = (byte) ((data.length >> 8) & 0x01);
			keyData[2] = (byte) (data.length & 0xff);
		} else {
			keyData = new byte[HEADER_LENGTH];
			// prepare header
			keyData[0] = key;
			keyData[1] = 0;
			keyData[2] = 0;
		}
		
		return keyData;
	}
		
    
    public byte getKey() {
		return mKey;
	}

	public void setKey(byte mKey) {
		this.mKey = mKey;
	}

	public int getPayloadLength() {
		return mPayloadLength;
	}

	public void setPayloadLength(int mPayloadLength) {
		this.mPayloadLength = mPayloadLength;
	}

	public byte[] getKeyData() {
		return mKeyData;
	}

	public void setKeyData(byte[] mKeyData) {
		this.mKeyData = mKeyData;
	}
}
