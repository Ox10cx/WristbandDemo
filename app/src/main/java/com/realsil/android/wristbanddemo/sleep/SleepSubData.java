package com.realsil.android.wristbanddemo.sleep;

public class SleepSubData {
	private int deepSleepTime;
	private int lightSleepTime;
    private int awakeTimes;
	public SleepSubData() {
    	deepSleepTime = 0;
    	lightSleepTime = 0;
    	awakeTimes = 0;
    }
	public void add(SleepSubData ssd) {
		deepSleepTime += ssd.getDeepSleepTime();
		lightSleepTime += ssd.getLightSleepTime();
		awakeTimes += ssd.getAwakeTimes();
	}
    public int getDeepSleepTime() {
		return deepSleepTime;
	}


	public void setDeepSleepTime(int deepSleepTime) {
		this.deepSleepTime = deepSleepTime;
	}


	public int getLightSleepTime() {
		return lightSleepTime;
	}


	public void setLightSleepTime(int lightSleepTime) {
		this.lightSleepTime = lightSleepTime;
	}


	public int getAwakeTimes() {
		return awakeTimes;
	}


	public void setAwakeTimes(int awakeTimes) {
		this.awakeTimes = awakeTimes;
	}
	
	public int getTotalSleepTime() {
		return lightSleepTime + deepSleepTime;
	}
	
	public String toString() {
		return "deepSleepTime: " + deepSleepTime
				+ ", lightSleepTime: " + lightSleepTime
				+ ", awakeTimes: " + awakeTimes;
	}
}