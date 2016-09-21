package com.realsil.android.wristbanddemo.utility;

import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.WindowManager;
import android.widget.ImageView;

import com.realsil.android.wristbanddemo.backgroundscan.BackgroundScanAutoConnected;
import com.realsil.android.wristbanddemo.bmob.BmobControlManager;
import com.realsil.android.wristbanddemo.bmob.bean.OTA;

import java.io.File;
import java.util.List;

import cn.bmob.v3.exception.BmobException;
import cn.bmob.v3.listener.FindListener;

/**
 * Created by rain1_wen on 2016/8/26.
 */
public class OtaAutoStartManager {
    // Log
    private final static String TAG = "OtaAutoStartManager";
    private final static boolean D = true;

    private static Context mContext;

    public boolean isInOtaAutoStartProcedure = false;

    public Integer mAppVersion = 0;
    public Integer mPatchVersion = 0;

    public String mAppFileUrl = "";
    public String mPatchFileUrl = "";

    public Integer mTargetAppVersion = 0;
    public Integer mTargetPatchVersion = 0;

    public boolean mNeedUpdateApp = false;
    public boolean mNeedUpdatePatch = false;

    // Use to manager command send transaction
    private volatile boolean isCommandSend;
    private volatile boolean isCommandSendOk;
    private final Object mCommandSendLock = new Object();
    private final int MAX_COMMAND_SEND_WAIT_TIME = 15000;

    private static  OtaAutoStartManager mInstance;

    public static void initial(Context context) {
        mInstance = new OtaAutoStartManager();
        mContext = context;

        // Register for callback
        BackgroundScanAutoConnected.getInstance().registerCallback(mInstance.mBackgroundScanCallback);
    }

    public static OtaAutoStartManager getInstance() {
        return mInstance;
    }

    private class OtaAutoStartThread extends Thread {
        public void run() {
            isInOtaAutoStartProcedure = true;
            if(D) Log.d(TAG, "OtaAutoStartThread, started.");


            isCommandSendOk = false;
            isCommandSend = false;

            // Register callback
            WristbandManager.getInstance().registerCallback(mWristbandManagerCallback);

            // Check and start ota process
            if (!WristbandManager.getInstance().readDfuVersion()) {
                if(D) Log.e(TAG, "readDfuVersion error, do nothing");

                isInOtaAutoStartProcedure = false;

                // un Register callback
                WristbandManager.getInstance().unRegisterCallback(mWristbandManagerCallback);
                return;
            }

            synchronized(mCommandSendLock) {
                if(isCommandSend != true) {
                    try {
                        // wait a while
                        if(D) Log.d(TAG, "wait the time set callback, wait for: " + MAX_COMMAND_SEND_WAIT_TIME + "ms");
                        mCommandSendLock.wait(MAX_COMMAND_SEND_WAIT_TIME);

                        if(D) Log.d(TAG, "waitCommandSend, isCommandSendOk: " + isCommandSendOk);
                    } catch (InterruptedException ee) {
                        // TODO Auto-generated catch block
                        ee.printStackTrace();
                    }
                }
            }

            // un Register callback
            WristbandManager.getInstance().unRegisterCallback(mWristbandManagerCallback);

            // Check the dfu info, need to update
            if(isCommandSendOk && isCommandSend) {
                boolean needUpdateApp = false;
                boolean needUpdatePatch = false;
                if(mTargetAppVersion < mAppVersion) {
                    needUpdateApp = true;
                }
                if(mTargetPatchVersion < mPatchVersion) {
                    needUpdatePatch = true;
                }

                if(D) Log.d(TAG, "mTargetAppVersion: " + mTargetAppVersion
                        + ", mAppVersion: " + mAppVersion
                        + ", needUpdateApp: " + needUpdateApp
                        + ", mTargetPatchVersion: " + mTargetPatchVersion
                        + ", mPatchVersion: " + mPatchVersion
                        + ", needUpdatePatch: " + needUpdatePatch);

                // For test
                /*
                needUpdateApp = true;
                needUpdatePatch = true;
                */
                if(needUpdateApp || needUpdatePatch) {
                    startOta(needUpdateApp, needUpdatePatch);
                } else {
                    if(D) Log.i(TAG, "Nothing need update, do nothing.");
                }
            } else {
                if(D) Log.e(TAG, "readDfuVersion error, isCommandSendOk: " + isCommandSendOk
                        + ", isCommandSend: " + isCommandSend + "do nothing");
            }

            isInOtaAutoStartProcedure = false;
        }

        // Step1: Try to get remote device info, normally the method called while user first connect the device
    }

    private void startOta(boolean needUpdateApp, boolean needUpdatePatch) {
        // Because of the app and patch is limit, we just download it.
        if(needUpdateApp) {
            if(!FileDownloadUtils.loadFile(mAppFileUrl)) {
                if(D) Log.w(TAG, "App load failed");
                needUpdateApp = false;
            }
        }

        if(needUpdatePatch) {
            if(!FileDownloadUtils.loadFile(mPatchFileUrl)) {
                if(D) Log.w(TAG, "Patch load failed");
                needUpdatePatch = false;
            }
        }

        if(D) Log.d(TAG, "startOta, needUpdateApp: " + needUpdateApp
                + ", needUpdatePatch: " + needUpdatePatch);

        if(needUpdateApp || needUpdatePatch) {
            mNeedUpdateApp = needUpdateApp;
            mNeedUpdatePatch = needUpdatePatch;

            // Current is in thread, need work in ui thread.
            runOnUiThreadWithOtaProcess();
        } else {
            if(D) Log.i(TAG, "startOta, Nothing need update, do nothing.");
        }
    }

    OtaDialog mOtaDialog;

    private void startOtaDialog() {
        if(!JudgeActivityFront.isAppOnForeground(mContext)) {
            if(D) Log.e(TAG, "showProgressBar, Is not in top.");
            return;
        }

        mOtaDialog  = OtaDialog.createDialog(mContext);
        //mProgressDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        mOtaDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_TOAST);// Some device can not show dialog
        mOtaDialog.setAppVersion(mAppVersion);
        mOtaDialog.setAppFileUrl(mAppFileUrl);
        mOtaDialog.setNeedUpdateApp(mNeedUpdateApp);
        mOtaDialog.setPatchVersion(mPatchVersion);
        mOtaDialog.setPatchFileUrl(mPatchFileUrl);
        mOtaDialog.setNeedUpdatePatch(mNeedUpdatePatch);

        mOtaDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                if(D) Log.d(TAG, "mOtaDialog, onDismiss");
                if(BackgroundScanAutoConnected.getInstance().isForceDisableAutoConnect()) {
                    BackgroundScanAutoConnected.getInstance().setIsForceDisableAutoConnect(false);

                    BackgroundScanAutoConnected.getInstance().startAutoConnect();
                }
            }
        });

        mOtaDialog.setCancelable(false);
        mOtaDialog.show();
    }

    private void runOnUiThreadWithOtaProcess() {
        Looper looper = Looper.getMainLooper(); //主线程的Looper对象
        //这里以主线程的Looper对象创建了handler，
        //所以，这个handler发送的Message会被传递给主线程的MessageQueue。
        UIHandler handler = new UIHandler(looper);

        handler.sendEmptyMessage(0);
    }

    class UIHandler extends Handler {
        public UIHandler(Looper looper) {
            super(looper);
        }
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            //Log.d("123", "handleMessage");

            startOtaDialog();
        }
    }


    // Application Layer callback
    WristbandManagerCallback mWristbandManagerCallback = new WristbandManagerCallback() {
        @Override
        public void onError(final int error) {
            if(D) Log.d(TAG, "onError, error: " + error);
            synchronized(mCommandSendLock) {
                isCommandSend = false;
                isCommandSendOk = false;
                mCommandSendLock.notifyAll();
            }
        }
        @Override
        public void onVersionRead(int appVersion, int patchVersion) {
            if (D) Log.d(TAG, "onVersionRead");
            mTargetAppVersion = appVersion;
            mTargetPatchVersion = patchVersion;

            // send msg to update ui
            synchronized(mCommandSendLock) {
                isCommandSend = true;
                isCommandSendOk = true;
                mCommandSendLock.notifyAll();
            }
        }
    };

    BackgroundScanAutoConnected.BackgroundScanCallback mBackgroundScanCallback
            = new BackgroundScanAutoConnected.BackgroundScanCallback() {
        public void onWristbandLoginStateChange(boolean connected) {
            if(D) Log.d(TAG, "onWristbandLoginStateChange, connected: " + connected);
            if(connected) {
                // Need delay?
                /*
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if(D) Log.d(TAG, "");
                    }
                }, 1000);*/
                if(!isInOtaAutoStartProcedure) {
                    isInOtaAutoStartProcedure = true;

                    // Step1: Try to get the bmob service the OTA file info
                    BmobControlManager.getInstance().getOTAInfo("", new FindListener<OTA>() {
                        @Override
                        public void done(List<OTA> list, BmobException e) {
                            if (e == null) {
                                if (D)
                                    Log.i(TAG, "get remote version success, list.size(): " + list.size());
                                for (OTA ota : list) {
                                    // Get the info
                                    if (ota.getType().equals(OTA.TYPE_OTA_APP)) {
                                        mAppVersion = Integer.parseInt(ota.getVersion());
                                        mAppFileUrl = ota.getFile().getFileUrl();
                                        if (D)
                                            Log.i(TAG, "get remote version success, mAppVersion: " + mAppVersion
                                                    + ", mAppFileUrl: " + mAppFileUrl);
                                    } else if (ota.getType().equals(OTA.TYPE_OTA_PATCH)) {
                                        mPatchVersion = Integer.parseInt(ota.getVersion());
                                        mPatchFileUrl = ota.getFile().getFileUrl();
                                        if (D)
                                            Log.i(TAG, "get remote version success, mPatchVersion: " + mPatchVersion
                                                    + ", mPatchFileUrl: " + mPatchFileUrl);
                                    }
                                }

                                OtaAutoStartThread otaAutoStartThread = new OtaAutoStartThread();
                                otaAutoStartThread.start();


                            } else {
                                Log.i("bmob", "失败："+e.getMessage()+","+e.getErrorCode());
                            }
                        }
                    });
                }
            }
        }
    };
}
