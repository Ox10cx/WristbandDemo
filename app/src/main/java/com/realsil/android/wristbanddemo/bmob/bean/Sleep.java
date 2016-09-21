package com.realsil.android.wristbanddemo.bmob.bean;

import cn.bmob.v3.BmobObject;

/**
 * Created by rain1_wen on 2016/8/19.
 */
public class Sleep extends BmobObject {
    private String uid;
    private Integer mode;
    private Integer minute;
    private Integer date;

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public Integer getMode() {
        return mode;
    }

    public void setMode(Integer mode) {
        this.mode = mode;
    }

    public Integer getMinute() {
        return minute;
    }

    public void setMinute(Integer minute) {
        this.minute = minute;
    }

    public Integer getDate() {
        return date;
    }

    public void setDate(Integer date) {
        this.date = date;
    }
}
