package com.realsil.android.wristbanddemo;

import android.app.Application;

import com.realsil.android.wristbanddemo.backgroundscan.BackgroundScanAutoConnected;
import com.realsil.android.wristbanddemo.backgroundscan.BackgroundSync;
import com.realsil.android.wristbanddemo.constant.ConstantParam;
import com.realsil.android.wristbanddemo.bmob.BmobControlManager;
import com.realsil.android.wristbanddemo.utility.GlobalGatt;
import com.realsil.android.wristbanddemo.utility.GlobalGreenDAO;
import com.realsil.android.wristbanddemo.utility.OtaAutoStartManager;
import com.realsil.android.wristbanddemo.utility.WristbandManager;

import java.io.File;

import cn.bmob.v3.Bmob;
import cn.sharesdk.framework.ShareSDK;


public class WristbandDemoApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // initial Global Gatt
        GlobalGatt.initial(this);
        // initial Green DAO
        GlobalGreenDAO.initial(this);
        // initial Wristband manager
        WristbandManager.initial(this);
        // initial BackgroundScanAutoConnected
        BackgroundScanAutoConnected.initial(this);

        BackgroundSync.initial(this);

        ShareSDK.initSDK(this);

        // Only the network have auto ota function
        if(BmobControlManager.checkAPKWorkType()) {
            OtaAutoStartManager.initial(this);
        }

        // initial Bmob Manager
        BmobControlManager.initial(this);
        // initial Bmob
        Bmob.initialize(this, ConstantParam.BMOB_APPLICATION_ID);

        // initial the cash path
        File file = new File(ConstantParam.IMAGE_SAVE_CACHE);
        if (!file.exists()) {
            file.mkdirs();
        }
        file = new File(ConstantParam.LOG_SAVE_CACHE);
        if (!file.exists()) {
            file.mkdirs();
        }

        file = new File(ConstantParam.FILE_SAVE_CACHE);
        if (!file.exists()) {
            file.mkdirs();
        }
    }
}
