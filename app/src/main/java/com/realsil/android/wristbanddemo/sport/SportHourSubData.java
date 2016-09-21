package com.realsil.android.wristbanddemo.sport;
import com.realsil.android.wristbanddemo.greendao.SportData;

public class SportHourSubData {
	private int hour;
	private SportSubData subData;
    SportHourSubData() {
    	hour = 0;
    	subData = new SportSubData();
    }
    SportHourSubData(int h, SportSubData s) {
    	hour = h;
    	subData = s;
    }
    

	public int getHour() {
		return hour;
	}
	public void setHour(int hour) {
		this.hour = hour;
	}
	public SportSubData getSubData() {
		return subData;
	}
	public void setSubData(SportSubData subData) {
		this.subData = subData;
	}
    
	
	public String toString() {
		return "stepCount: " + hour
				+ subData.toString();
	}
}