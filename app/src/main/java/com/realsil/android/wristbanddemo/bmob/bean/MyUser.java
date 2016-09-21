package com.realsil.android.wristbanddemo.bmob.bean;

import cn.bmob.v3.BmobUser;
import cn.bmob.v3.datatype.BmobFile;

/**
 * Created by rain1_wen on 2016/8/19.
 */
public class MyUser extends BmobUser {
    private Boolean gender;
    private Integer age;
    private String nickName;
    private BmobFile image;
    private Integer height;
    private Integer weight;
    private Integer stepTarget;

    public String getNickName() {
        return this.nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public BmobFile getImage() {
        return image;
    }

    public void setImage(BmobFile image) {
        this.image = image;
    }

    public Boolean getGender() {
        return gender;
    }

    public void setGender(Boolean gender) {
        this.gender = gender;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public Integer getWeight() {
        return weight;
    }

    public void setWeight(Integer weight) {
        this.weight = weight;
    }

    public Integer getStepTarget() {
        return stepTarget;
    }

    public void setStepTarget(Integer stepTarget) {
        this.stepTarget = stepTarget;
    }
}
