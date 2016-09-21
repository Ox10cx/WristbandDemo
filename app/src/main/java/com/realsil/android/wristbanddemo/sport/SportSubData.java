package com.realsil.android.wristbanddemo.sport;

public class SportSubData {
	private int stepCount;
	private int calory;
    private int distance;
	public SportSubData() {
    	stepCount = 0;
    	calory = 0;
    	distance = 0;
    }
    public SportSubData(int s, int c, int d) {
    	stepCount = s;
    	calory = c;
    	distance = d;
    }
	public void add(SportSubData ssd) {
		stepCount += ssd.getStepCount();
		calory += ssd.getCalory();
		distance += ssd.getDistance();
	}
    public int getStepCount() {
		return stepCount;
	}

	public void setStepCount(int stepCount) {
		this.stepCount = stepCount;
	}

	public int getCalory() {
		return calory;
	}

	public void setCalory(int calory) {
		this.calory = calory;
	}

	public int getDistance() {
		return distance;
	}

	public void setDistance(int distance) {
		this.distance = distance;
	}
	
	public String toString() {
		return "stepCount: " + stepCount
				+ ", calory: " + calory
				+ ", distance: " + distance;
	}
}