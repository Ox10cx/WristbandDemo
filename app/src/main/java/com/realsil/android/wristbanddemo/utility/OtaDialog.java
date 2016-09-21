package com.realsil.android.wristbanddemo.utility;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.realsil.android.blehub.dfu.RealsilDfu;
import com.realsil.android.blehub.dfu.RealsilDfuCallback;
import com.realsil.android.wristbanddemo.R;
import com.realsil.android.wristbanddemo.backgroundscan.BackgroundScanAutoConnected;

public class OtaDialog extends Dialog {
    // Log
    private final static String TAG = "OtaDialog";
    private final static boolean D = true;

    private static Context mContext;

    // Handle msg
    private static final int OTA_CALLBACK_STATE_CHANGE = 0;
    private static final int OTA_CALLBACK_PROCESS_CHANGE = 1;
    private static final int OTA_CALLBACK_SUCCESS = 2;
    private static final int OTA_CALLBACK_ERROR = 3;

    // Type
    public static final String EXTRAS_VALUE_NEED_UPDATE_APP = "VALUE_NEED_UPDATE_APP";
    public static final String EXTRAS_VALUE_NEED_UPDATE_PATCH = "VALUE_NEED_UPDATE_PATCH";
    public static final String EXTRAS_VALUE_APP_VERSION = "VALUE_APP_VERSION";
    public static final String EXTRAS_VALUE_PATCH_VERSION = "VALUE_PATCH_VERSION";
    public static final String EXTRAS_VALUE_APP_FILE_URL = "VALUE_APP_FILE_URL";
    public static final String EXTRAS_VALUE_PATCH_FILE_URL = "VALUE_PATCH_FILE_URL";
    public static final String EXTRAS_VALUE_TARGET_APP_VERSION = "VALUE_TARGET_APP_VERSION";
    public static final String EXTRAS_VALUE_TARGET_PATCH_VERSION = "VALUE_TARGET_PATCH_VERSION";

    // dfu object
    private RealsilDfu dfu = null;

    public Integer mAppVersion = 0;
    public Integer mPatchVersion = 0;

    public String mAppFileUrl = "";
    public String mPatchFileUrl = "";

    public Integer mTargetAppVersion = 0;
    public Integer mTargetPatchVersion = 0;

    public boolean mNeedUpdateApp = false;
    public boolean mNeedUpdatePatch = false;

    private LinearLayout mllTitleBack;
    private RelativeLayout mrlSure;
    private RelativeLayout mrlCancel;

    private LinearLayout mllUploadingBack;
    private TextView mtvUploadingStatus;
    private ProgressBar mpbUploadProgress;
    private TextView mtvProgress;

    private boolean isInOta;

    private Toast mToast;

    private static final int UPDATE_TYPE_APP = 0;
    private static final int UPDATE_TYPE_PATCH = 1;

    private int mCurrentUpdateType;

    private static OtaDialog mOtaProgressDialog = null;

    private boolean isForceStart = false;

    public OtaDialog(Context context){
        super(context);
        this.mContext = context;
    }

    public OtaDialog(Context context, int theme) {
        super(context, theme);
    }

    public static OtaDialog createDialog(Context context){
        mOtaProgressDialog = new OtaDialog(context);
        mOtaProgressDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        mOtaProgressDialog.setContentView(R.layout.fragment_ota);
        mOtaProgressDialog.getWindow().getAttributes().gravity = Gravity.CENTER;

        return mOtaProgressDialog;
    }

    public void onWindowFocusChanged(boolean hasFocus){
        if (mOtaProgressDialog == null){
            return;
        }

        initialUI();

        if(isForceStart) {
            mllTitleBack.setVisibility(View.GONE);
            mllUploadingBack.setVisibility(View.VISIBLE);
        }
    }

    private void initialUI() {
        // get the Realsil Dfu proxy
        RealsilDfu.getDfuProxy(mContext, cb);
/*
        mAppVersion = getArguments().getInt(EXTRAS_VALUE_APP_VERSION);
        mPatchVersion = getArguments().getInt(EXTRAS_VALUE_PATCH_VERSION);
        mTargetAppVersion = getArguments().getInt(EXTRAS_VALUE_TARGET_APP_VERSION);
        mTargetPatchVersion = getArguments().getInt(EXTRAS_VALUE_TARGET_PATCH_VERSION);
        mAppFileUrl = getArguments().getString(EXTRAS_VALUE_APP_FILE_URL);
        mPatchFileUrl = getArguments().getString(EXTRAS_VALUE_PATCH_FILE_URL);

        mNeedUpdateApp = getArguments().getBoolean(EXTRAS_VALUE_NEED_UPDATE_APP);
        mNeedUpdatePatch = getArguments().getBoolean(EXTRAS_VALUE_NEED_UPDATE_PATCH);
        */
        if(D) Log.i(TAG, "mAppVersion: " + mAppVersion
                + ", mPatchVersion: " + mPatchVersion
                + ", mTargetAppVersion: " + mTargetAppVersion
                + ", mTargetPatchVersion: " + mTargetPatchVersion
                + ", mAppFileUrl: " + mAppFileUrl
                + ", mPatchFileUrl: " + mPatchFileUrl
                + ", mNeedUpdateApp: " + mNeedUpdateApp
                + ", mNeedUpdatePatch: " + mNeedUpdatePatch);

        // initial UI
        mllTitleBack = (LinearLayout) mOtaProgressDialog.findViewById(R.id.llTitleBack);

        mrlSure = (RelativeLayout) mOtaProgressDialog.findViewById(R.id.rlSure);
        mrlSure.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                mllTitleBack.setVisibility(View.GONE);
                mllUploadingBack.setVisibility(View.VISIBLE);

                // Must Set One
                // initial param
                isInOta = true;
                if((mNeedUpdateApp && mNeedUpdatePatch)
                        || (mNeedUpdatePatch)) {
                    mCurrentUpdateType = UPDATE_TYPE_PATCH;
                } else {
                    mCurrentUpdateType = UPDATE_TYPE_APP;
                }

                startOtaProcess();
            }
        });

        mrlCancel = (RelativeLayout) mOtaProgressDialog.findViewById(R.id.rlCancel);
        mrlCancel.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // cancel
                mOtaProgressDialog.cancel();
            }
        });

        mllUploadingBack = (LinearLayout) mOtaProgressDialog.findViewById(R.id.llUploadingBack);
        mtvUploadingStatus = (TextView) mOtaProgressDialog.findViewById(R.id.tvUploadingStatus);
        mpbUploadProgress = (ProgressBar) mOtaProgressDialog.findViewById(R.id.pbUploadProgress);
        mtvProgress = (TextView) mOtaProgressDialog.findViewById(R.id.tvProgress);
    }

    private void startOtaProcess() {
        if(dfu == null) {
            showToast(R.string.dfu_not_ready);
            if(D) Log.e(TAG, "the realsil dfu didn't ready");
            return;
        }

        String address = WristbandManager.getInstance().getBluetoothAddress();
        String filePath = "";

        // Outside have download the file
        if(mCurrentUpdateType == UPDATE_TYPE_APP) {
            filePath = FileDownloadUtils.getUniquePath(mAppFileUrl);
        } else {
            filePath = FileDownloadUtils.getUniquePath(mPatchFileUrl);
        }

        // set the total speed for android 4.4, to escape the internal error
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            dfu.setSpeedControl(true, 1000);
        }
        // Use GlobalGatt do not need to disconnect, just unregister the callback
        GlobalGatt.getInstance().unRegisterAllCallback(address);
        /*
        // disconnect the gatt
        disconnect(mBtGatt);// be care here
        // wait a while for close gatt.
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }*/
        if(D) Log.e(TAG, "Start OTA, address is: " + address + ", filePath: " + filePath);
        if(dfu.start(address, filePath)){
            showToast(R.string.dfu_start_ota_success_msg);
            if(D) Log.d(TAG, "true");
        } else {
            showToast(R.string.dfu_start_ota_fail_msg);
            if(D) Log.e(TAG, "something error in device info or the file, false");

            isInOta = false;
            OtaDialog.this.dismiss();
        }
    }

    public void onBackPressed() {
        if(isInOta) {
            showToast(R.string.exit_ota_text_force);
        } else {
            this.dismiss();
        }
    }

    RealsilDfuCallback cb = new RealsilDfuCallback() {
        public void onServiceConnectionStateChange(boolean status, RealsilDfu d) {
            if(D) Log.e(TAG, "status: " + status);
            if(status == true) {
                //Toast.makeText(getApplicationContext(), "DFU Service connected", Toast.LENGTH_SHORT).show();
                dfu = d;

                if(isForceStart) {
                    mrlSure.callOnClick();
                }
            } else {
                //Toast.makeText(getApplicationContext(), "DFU Service disconnected", Toast.LENGTH_SHORT).show();
                dfu = null;
            }
        }

        public void onError(int e) {
            if(D) Log.e(TAG, "onError: " + e);

            // send msg to update ui
            Message msg = mHandle.obtainMessage(OTA_CALLBACK_ERROR);
            msg.arg1 = e;
            mHandle.sendMessage(msg);
        }

        public void onSucess(int s) {
            if(D) Log.e(TAG, "onSucess: " + s);

            // send msg to update ui
            Message msg = mHandle.obtainMessage(OTA_CALLBACK_SUCCESS);
            msg.arg1 = s;
            mHandle.sendMessage(msg);

        }

        public void onProcessStateChanged(int state) {
            if(D) Log.e(TAG, "onProcessStateChanged: " + state);

            // send msg to update ui
            Message msg = mHandle.obtainMessage(OTA_CALLBACK_STATE_CHANGE);
            msg.arg1 = state;
            mHandle.sendMessage(msg);
        }

        public void onProgressChanged(int progress) {
            if(D) Log.e(TAG, "onProgressChanged: " + progress);

            // send msg to update ui
            Message msg = mHandle.obtainMessage(OTA_CALLBACK_PROCESS_CHANGE);
            msg.arg1 = progress;
            mHandle.sendMessage(msg);
        }
    };

    private Handler mHandle = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            // TODO Auto-generated method stub
            if(D) Log.d(TAG, "MSG No " + msg.what);
            switch (msg.what) {
                case OTA_CALLBACK_PROCESS_CHANGE:
                    mpbUploadProgress.setProgress(msg.arg1);
                    mtvProgress.setText(msg.arg1 + "%");
                    break;
                case OTA_CALLBACK_STATE_CHANGE:
                    switch (msg.arg1) {
                        case RealsilDfu.STA_ORIGIN:
                        case RealsilDfu.STA_REMOTE_ENTER_OTA:
                        case RealsilDfu.STA_FIND_OTA_REMOTE:
                        case RealsilDfu.STA_CONNECT_OTA_REMOTE:
                            if(mCurrentUpdateType == UPDATE_TYPE_APP) {
                                mtvUploadingStatus.setText(R.string.dfu_status_initialing_app_msg);
                            } else {
                                mtvUploadingStatus.setText(R.string.dfu_status_initialing_patch_msg);
                            }
                            break;
                        case RealsilDfu.STA_START_OTA_PROCESS:
                            if(mCurrentUpdateType == UPDATE_TYPE_APP) {
                                mtvUploadingStatus.setText(R.string.dfu_status_starting_app_msg);
                            } else {
                                mtvUploadingStatus.setText(R.string.dfu_status_starting_patch_msg);
                            }
                            break;
                        case RealsilDfu.STA_OTA_UPGRADE_SUCCESS:
                            if(mCurrentUpdateType == UPDATE_TYPE_APP) {
                                mtvUploadingStatus.setText(R.string.dfu_status_completed_msg);
                            }
                            break;
                        default:
                            break;
                    }
                    break;
                case OTA_CALLBACK_SUCCESS:
                    if(mCurrentUpdateType == UPDATE_TYPE_APP) {
                        mtvUploadingStatus.setText(R.string.dfu_status_starting_app_msg);

                        isInOta = false;

                        showToast(R.string.dfu_status_completed_msg);

                        OtaDialog.this.dismiss();
                    } else {
                        // Check Need update app
                        if(mNeedUpdateApp) {
                            // Update state
                            mCurrentUpdateType = UPDATE_TYPE_APP;

                            mpbUploadProgress.setProgress(0);
                            mtvProgress.setText(0 + "%");

                            // Disable auto connect
                            BackgroundScanAutoConnected.getInstance().setIsForceDisableAutoConnect(true);

                            startOtaProcess();
                        } else {
                            mtvUploadingStatus.setText(R.string.dfu_status_starting_app_msg);

                            isInOta = false;

                            showToast(R.string.dfu_status_completed_msg);

                            OtaDialog.this.dismiss();
                        }
                    }
                    WristbandManager.getInstance().close();
                    break;
                case OTA_CALLBACK_ERROR:
                    isInOta = false;
                    showToast(mContext.getString(R.string.dfu_status_error_msg, msg.arg1));
                    WristbandManager.getInstance().close();

                    OtaDialog.this.dismiss();
                    break;
                default:
                    break;
            }

            super.handleMessage(msg);
        }

    };


    /**
     * send message
     * @param msgType Type message type
     * @param obj object sent with the message set to null if not used
     * @param arg1 parameter sent with the message, set to -1 if not used
     * @param arg2 parameter sent with the message, set to -1 if not used
     **/
    private void SendMessage(int msgType, Object obj, int arg1, int arg2) {
        if(mHandle != null) {
            //	Message msg = new Message();
            Message msg = Message.obtain();
            msg.what = msgType;
            if(arg1 != -1) {
                msg.arg1 = arg1;
            }
            if(arg2 != -1) {
                msg.arg2 = arg2;
            }
            if(null != obj) {
                msg.obj = obj;
            }
            mHandle.sendMessage(msg);
        }
        else {
            if(D) Log.e(TAG,"handler is null, can't send message");
        }
    }

    private void showToast(final String message) {
        if(mToast == null) {
            mToast = Toast.makeText(mContext, message, Toast.LENGTH_SHORT);
        } else {
            mToast.setText(message);
        }
        mToast.show();
    }
    private void showToast(final int message) {
        if(mToast == null) {
            mToast = Toast.makeText(mContext, message, Toast.LENGTH_SHORT);
        } else {
            mToast.setText(message);
        }
        mToast.show();
    }

    public Integer getAppVersion() {
        return mAppVersion;
    }

    public void setAppVersion(Integer mAppVersion) {
        this.mAppVersion = mAppVersion;
    }

    public Integer getPatchVersion() {
        return mPatchVersion;
    }

    public void setPatchVersion(Integer mPatchVersion) {
        this.mPatchVersion = mPatchVersion;
    }

    public String getAppFileUrl() {
        return mAppFileUrl;
    }

    public void setAppFileUrl(String mAppFileUrl) {
        this.mAppFileUrl = mAppFileUrl;
    }

    public String getPatchFileUrl() {
        return mPatchFileUrl;
    }

    public void setPatchFileUrl(String mPatchFileUrl) {
        this.mPatchFileUrl = mPatchFileUrl;
    }

    public Integer getTargetAppVersion() {
        return mTargetAppVersion;
    }

    public void setTargetAppVersion(Integer mTargetAppVersion) {
        this.mTargetAppVersion = mTargetAppVersion;
    }

    public Integer getTargetPatchVersion() {
        return mTargetPatchVersion;
    }

    public void setTargetPatchVersion(Integer mTargetPatchVersion) {
        this.mTargetPatchVersion = mTargetPatchVersion;
    }

    public boolean isNeedUpdateApp() {
        return mNeedUpdateApp;
    }

    public void setNeedUpdateApp(boolean mNeedUpdateApp) {
        this.mNeedUpdateApp = mNeedUpdateApp;
    }

    public boolean isNeedUpdatePatch() {
        return mNeedUpdatePatch;
    }

    public void setNeedUpdatePatch(boolean mNeedUpdatePatch) {
        this.mNeedUpdatePatch = mNeedUpdatePatch;
    }

    public boolean isForceStart() {
        return isForceStart;
    }

    public void setIsForceStart(boolean isForceStart) {
        this.isForceStart = isForceStart;
    }
}
