package com.realsil.android.wristbanddemo.backgroundscan;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.realsil.android.wristbanddemo.R;
import com.realsil.android.wristbanddemo.utility.WristbandManager;

/**
 * Created by rain1_wen on 2016/8/8.
 */
public class BackgroundSync {
    // Log
    private final static String TAG = "BackgroundSync";
    private final static boolean D = true;
    private static Context mContext;

    // instance
    private static BackgroundSync mInstance;

    public static void initial(Context context) {
        mInstance = new BackgroundSync();
        mContext = context;

        mInstance.restartTimer();
    }

    private void restartTimer() {
        // stop timer
        stopSyncTimer();

        startSyncTimer();
    }

    // Rx super timer 30 * 60000
    private final int SYNC_PERIOD = 1000*10;
    final Handler mSyncHandler = new Handler();
    Runnable mSyncSuperTask = new Runnable(){
        @Override
        public void run() {
            // TODO Auto-generated method stub
            if(D) Log.w(TAG, "Sync timer fired");
            // sync
            syncData();

            restartTimer();
        }
    };
    private void startSyncTimer(){
        //if(D) Log.d(TAG, "startSyncTimer()");
        synchronized (mSyncHandler) {
            mSyncHandler.postDelayed(mSyncSuperTask, SYNC_PERIOD);
            //if(D) Log.d(TAG, "mSyncHandler.postDelayed");
        }
    }
    private void stopSyncTimer() {
        //if(D) Log.d(TAG, "stopSyncTimer()");
        synchronized (mSyncHandler) {
            mSyncHandler.removeCallbacks(mSyncSuperTask);
            //if(D) Log.d(TAG, "mSyncHandler.removeCallbacks");
        }
    }

    private void syncData() {
        final WristbandManager wristbandManager = WristbandManager.getInstance();
        if(wristbandManager == null) {
            return;
        }
        if(wristbandManager.isConnect()) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG,"TAGBackgroundSync");
                    wristbandManager.SendDataRequest();
                }
            }).start();
        }
    }
}
