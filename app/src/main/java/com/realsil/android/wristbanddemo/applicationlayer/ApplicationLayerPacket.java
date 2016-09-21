package com.realsil.android.wristbanddemo.applicationlayer;

import android.util.Log;

import java.util.ArrayList;


public class ApplicationLayerPacket {
	// Log
	private final static String TAG = "ApplicationLayerPacket";
	private final static boolean D = true;
	
	// Header
	private byte mCommandId;	// 1Byte, bit(0-7)
	private int mVersion;		// 4bit
	
	// Parameter
	private ArrayList<ApplicationLayerKeyPacket> mKeyPacketArrays = new ArrayList<ApplicationLayerKeyPacket>();
	

	// Version
	private final static int VERSION_CODE = 0;
	
	// Header Length
	private final static int HEADER_LENGTH = 2;
	
	
	public static int getVersion() {
		return VERSION_CODE;
	}
	
	/**
	 * prepare the Application Layer Packet to send
	 * 
	 * @param cmd the comand id
	 * @param keyData the send key data
	 * 
	 * @return the integrated Application Layer Packet
	 * */
	public static byte[] preparePacket(byte cmd, byte[] keyData) {
		byte[] data = new byte[HEADER_LENGTH + keyData.length];
		data[0] = cmd;
		data[1] = (byte) ((getVersion() << 4) & 0xff);
		System.arraycopy(keyData, 0, data, HEADER_LENGTH, keyData.length);
		return data;
	}
	
	/**
	 * Use to parse the Application Layer Packet.
	 * 
	 * @param data the receive packet
	 * @return success or false
	 * */
	public boolean parseData(byte[] data) {
		// check length
		if(data.length < HEADER_LENGTH) {
			return false;
		}
		
		mCommandId = data[0];
		mVersion = (data[1] >> 4) & 0x0f;
		if(D) Log.d(TAG, "mCommandId: " + mCommandId + ", mVersion: " + mVersion);
		int remainLength = data.length - HEADER_LENGTH;
		do {
			// Save key data
			byte[] keyData = new byte[remainLength];
			System.arraycopy(data, data.length - remainLength, keyData, 0, remainLength);
			
			// get the key data
			ApplicationLayerKeyPacket keyPacket = new ApplicationLayerKeyPacket();
			if(keyPacket.parseData(keyData) != true) {
				return false;
			}
			// add to key data array
			mKeyPacketArrays.add(keyPacket);
			// update remain data length
			remainLength = remainLength - (keyPacket.getPayloadLength() + ApplicationLayerKeyPacket.HEADER_LENGTH);
			if(D) Log.d(TAG, "remainLength: " + remainLength);
		} while(remainLength > 0);
		
		return true;
	}
	
	public ArrayList<ApplicationLayerKeyPacket> getKeyPacketArrays() {
		return mKeyPacketArrays;
	}

	public void setKeyPacketArrays(
			ArrayList<ApplicationLayerKeyPacket> keyPacketArrays) {
		mKeyPacketArrays = keyPacketArrays;
	}
	
	public byte getCommandId() {
		return mCommandId;
	}

	public void setCommandId(byte commandId) {
		mCommandId = commandId;
	}
}
