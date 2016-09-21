package com.realsil.android.wristbanddemo.applicationlayer;

import java.util.ArrayList;


public class ApplicationLayerAlarmsPacket {

	// Parameters
	private ArrayList<ApplicationLayerAlarmPacket> mAlarms = new ArrayList<ApplicationLayerAlarmPacket>();
	
	public byte[] getPacket() {
		if(mAlarms.size() == 0) {
    		return null;
    	}
    	// generate key value data
    	byte[] data = new byte [mAlarms.size() * ApplicationLayerAlarmPacket.ALARM_HEADER_LENGTH];
    	int i = 0;
    	for(ApplicationLayerAlarmPacket alarm : mAlarms) {
    		System.arraycopy(alarm.getPacket(), 0, 
    				data, i * ApplicationLayerAlarmPacket.ALARM_HEADER_LENGTH, ApplicationLayerAlarmPacket.ALARM_HEADER_LENGTH);
    		i ++;
    	}
        return data;
	}
	
	public void add(ApplicationLayerAlarmPacket e) {
		mAlarms.add(e);
	}
	public void add(ArrayList<ApplicationLayerAlarmPacket> e) {
		for(ApplicationLayerAlarmPacket p: e) {
			mAlarms.add(p);
		}
	}
	
	public int size() {
		return mAlarms.size();
	}
	
	/**
	 * Use to parse the Application Layer Alarms Packet.
	 * 
	 * @param data the receive packet
	 * @return success or false
	 * */
	public boolean parseData(byte[] data) {
		// check length
		if((data.length < ApplicationLayerAlarmPacket.ALARM_HEADER_LENGTH) 
				|| (data.length % ApplicationLayerAlarmPacket.ALARM_HEADER_LENGTH) != 0) {
			return false;
		}
		
		// get the alarm count
		byte[] subData = new byte[ApplicationLayerAlarmPacket.ALARM_HEADER_LENGTH];
		for(int i = 0; i < data.length / 5; i ++) {
			ApplicationLayerAlarmPacket alarm = new ApplicationLayerAlarmPacket();
			System.arraycopy(data, i * ApplicationLayerAlarmPacket.ALARM_HEADER_LENGTH,
					subData, 0, ApplicationLayerAlarmPacket.ALARM_HEADER_LENGTH);
			// parse the alarm packet
			alarm.parseData(subData);
			mAlarms.add(alarm);
		}
		return true;
	}

	public ArrayList<ApplicationLayerAlarmPacket> getAlarms() {
		return mAlarms;
	}
}
