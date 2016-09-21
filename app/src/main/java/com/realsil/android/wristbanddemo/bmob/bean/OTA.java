package com.realsil.android.wristbanddemo.bmob.bean;

import cn.bmob.v3.BmobObject;
import cn.bmob.v3.datatype.BmobFile;

/**
 * Created by rain1_wen on 2016/8/19.
 */
public class OTA extends BmobObject {
    private String version;
    private String type;
    private String fileName;
    private BmobFile file;

    public final static String TYPE_OTA_APP = "app";
    public final static String TYPE_OTA_PATCH = "patch";

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public BmobFile getFile() {
        return file;
    }

    public void setFile(BmobFile file) {
        this.file = file;
    }
}
