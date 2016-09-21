package com.realsil.android.wristbanddemo.bmob.bean;

import cn.bmob.v3.BmobObject;

/**
 * Created by rain1_wen on 2016/8/19.
 */
public class Sports extends BmobObject {
    private String uid;
    private Integer steps;
    private Integer offset;
    private Integer distance;
    private Integer date;
    private Integer calory;

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public Integer getSteps() {
        return steps;
    }

    public void setSteps(Integer steps) {
        this.steps = steps;
    }

    public Integer getOffset() {
        return offset;
    }

    public void setOffset(Integer offset) {
        this.offset = offset;
    }

    public Integer getDistance() {
        return distance;
    }

    public void setDistance(Integer distance) {
        this.distance = distance;
    }

    public Integer getDate() {
        return date;
    }

    public void setDate(Integer date) {
        this.date = date;
    }

    public Integer getCalory() {
        return calory;
    }

    public void setCalory(Integer calory) {
        this.calory = calory;
    }
}
