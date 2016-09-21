package com.realsil.android.wristbanddemo;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.LoaderManager;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.realsil.android.blehub.dfu.BinInputStream;
import com.realsil.android.blehub.dfu.RealsilDfu;
import com.realsil.android.blehub.dfu.RealsilDfuCallback;
import com.realsil.android.wristbanddemo.backgroundscan.BackgroundScanAutoConnected;
import com.realsil.android.wristbanddemo.bmob.BmobControlManager;
import com.realsil.android.wristbanddemo.bmob.bean.OTA;
import com.realsil.android.wristbanddemo.utility.FileDownloadUtils;
import com.realsil.android.wristbanddemo.utility.GlobalGatt;
import com.realsil.android.wristbanddemo.utility.HighLightView;
import com.realsil.android.wristbanddemo.utility.JudgeActivityFront;
import com.realsil.android.wristbanddemo.utility.OtaDialog;
import com.realsil.android.wristbanddemo.utility.SPWristbandConfigInfo;
import com.realsil.android.wristbanddemo.utility.WristbandManager;
import com.realsil.android.wristbanddemo.utility.WristbandManagerCallback;
import com.realsil.android.wristbanddemo.view.SwipeBackActivity;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import cn.bmob.v3.exception.BmobException;
import cn.bmob.v3.listener.FindListener;

public class WristbandDeviceInfoActivity extends SwipeBackActivity {
    // Log
    private final static String TAG = "WristbandDeviceInfoActivity";
    private final static boolean D = true;

    private RelativeLayout mrlUpload;

    private ImageView mivDeviceInfoBack;

    private TextView mtvUpload;

    private TextView mtvDeviceName;
    private TextView mtvBdAddress;
    private TextView mtvCurrentAppVersion;
    private TextView mtvCurrentPatchVersion;
    private TextView mtvNewAppVersion;
    private TextView mtvNewPatchVersion;

    public Integer mAppVersion = 0;
    public Integer mPatchVersion = 0;

    public String mAppFileUrl = "";
    public String mPatchFileUrl = "";

    public Integer mTargetAppVersion = 0;
    public Integer mTargetPatchVersion = 0;

    public boolean mNeedUpdateApp = false;
    public boolean mNeedUpdatePatch = false;

    private WristbandManager mWristbandManager;
    private GlobalGatt mGlobalGatt;

    private String mDeviceName;

    private Toast mToast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wristband_device_info);

        mWristbandManager = WristbandManager.getInstance();
        mWristbandManager.registerCallback(mWristbandManagerCallback);

        mGlobalGatt = GlobalGatt.getInstance();

        // set UI
        setUI();

        initialStringFormat();

        initialUI();
    }

    private String mFormatConnectDevice;
    private void initialStringFormat() {
        mFormatConnectDevice = getResources().getString(R.string.connect_with_device_name);
    }

    private void initialUI() {
        if(!BackgroundScanAutoConnected.getInstance().isInLogin()) {
            showInfo();
        }
    }

    private void showInfo() {
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
                            mtvNewAppVersion.setText(mAppVersion.toString());
                            if (D)
                                Log.i(TAG, "get remote version success, mAppVersion: " + mAppVersion
                                        + ", mAppFileUrl: " + mAppFileUrl);
                        } else if (ota.getType().equals(OTA.TYPE_OTA_PATCH)) {
                            mPatchVersion = Integer.parseInt(ota.getVersion());
                            mtvNewPatchVersion.setText(mPatchVersion.toString());
                            mPatchFileUrl = ota.getFile().getFileUrl();
                            if (D)
                                Log.i(TAG, "get remote version success, mPatchVersion: " + mPatchVersion
                                        + ", mPatchFileUrl: " + mPatchFileUrl);
                        }
                    }
                    new Thread(new Runnable() {
                        @Override
                        public void run() {

                            // here we just use to read dfu info, do not enable notification
                            if (!mWristbandManager.readDfuVersion()) {
                                // send msg to update ui
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        showToast(R.string.dfu_start_ota_fail_msg);
                                    }
                                });
                            }
                        }
                    }).start();

                } else {
                    Log.i("bmob", "失败："+e.getMessage()+","+e.getErrorCode());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showToast(R.string.dfu_start_ota_fail_msg);
                        }
                    });
                }
            }
        });
        mtvBdAddress.setText(mWristbandManager.getBluetoothAddress());
        mtvDeviceName.setText(SPWristbandConfigInfo.getInfoKeyValue(this, mWristbandManager.getBluetoothAddress()));
    }

    private void setUI() {
        mivDeviceInfoBack = (ImageView) findViewById(R.id.ivDeviceInfoBack);
        mivDeviceInfoBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WristbandDeviceInfoActivity.this.finish();
            }
        });

        mrlUpload = (RelativeLayout) findViewById(R.id.rlUpload);
        mrlUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mWristbandManager.isConnect()) {
                    onUploadClicked(v);
                } else {
                    showToast(R.string.please_connect_band);
                }
            }
        });
        mrlUpload.setEnabled(false);

        mtvUpload = (TextView) findViewById(R.id.tvUpload);
        mtvDeviceName = (TextView) findViewById(R.id.tvDeviceName);
        mtvBdAddress = (TextView) findViewById(R.id.tvBdAddress);
        mtvCurrentAppVersion = (TextView) findViewById(R.id.tvCurrentAppVersion);
        mtvCurrentPatchVersion = (TextView) findViewById(R.id.tvCurrentPatchVersion);
        mtvNewAppVersion = (TextView) findViewById(R.id.tvNewAppVersion);
        mtvNewPatchVersion = (TextView) findViewById(R.id.tvNewPatchVersion);
    }



    /**
     * Callback of UPDATE/CANCEL button on FeaturesActivity
     * Button.onClick convert to onUploadClicked
     */
    public void onUploadClicked(final View view) {
        int batteryLevel = mWristbandManager.getBatteryLevel();
        if(batteryLevel >= 0
                && batteryLevel < 60) {
            showToast(String.format(getString(R.string.dfu_battery_not_enough), batteryLevel));
            if(D) Log.e(TAG, "the battery level is too low. batteryLevel: " + batteryLevel);
            return;
        }

        startOtaDialog();
    }

    private OtaDialog mOtaDialog;
    private void startOtaDialog() {
        mOtaDialog  = OtaDialog.createDialog(WristbandDeviceInfoActivity.this);
        //mProgressDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        mOtaDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_TOAST);// Some device can not show dialog
        mOtaDialog.setAppVersion(mAppVersion);
        mOtaDialog.setAppFileUrl(mAppFileUrl);
        mOtaDialog.setNeedUpdateApp(mNeedUpdateApp);
        mOtaDialog.setPatchVersion(mPatchVersion);
        mOtaDialog.setPatchFileUrl(mPatchFileUrl);
        mOtaDialog.setNeedUpdatePatch(mNeedUpdatePatch);

        mOtaDialog.setIsForceStart(true);

        mOtaDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                if (D) Log.d(TAG, "mOtaDialog, onDismiss");
                if (BackgroundScanAutoConnected.getInstance().isForceDisableAutoConnect()) {
                    BackgroundScanAutoConnected.getInstance().setIsForceDisableAutoConnect(false);

                    BackgroundScanAutoConnected.getInstance().startAutoConnect();

                    // new method is return to the home page to reconnect the device.
                    Intent intent = new Intent(WristbandDeviceInfoActivity.this, WristbandHomeActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    WristbandDeviceInfoActivity.this.startActivity(intent);
                    WristbandDeviceInfoActivity.this.finish();
                }
            }
        });

        mOtaDialog.setCancelable(false);
        mOtaDialog.show();
    }

    private void showToast(final String message) {
        if(mToast == null) {
            mToast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
        } else {
            mToast.setText(message);
        }
        mToast.show();
    }
    private void showToast(final int message) {
        if (mToast == null) {
            mToast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
        } else {
            mToast.setText(message);
        }
        mToast.show();
    }

    @Override
    protected void onStop() {
        if(D) Log.d(TAG, "onStop()");
        super.onStop();
    }

    @Override
    protected void onResume() {
        if(D) Log.d(TAG, "onResume()");
        super.onResume();

        BackgroundScanAutoConnected.getInstance().startAutoConnect();
    }

    @Override
    protected void onPause() {
        if(D) Log.d(TAG, "onPause()");
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (D) Log.d(TAG, "onDestroy()");
        super.onDestroy();

        mWristbandManager.unRegisterCallback(mWristbandManagerCallback);
        /*
        //unregiste the broadcast Receiver
        if(mBondStateReceiver != null){
            this.unregisterReceiver(mBondStateReceiver);
            if(D) Log.i(TAG, "unregisterReceiver");
        }
        */
    }

    // Application Layer callback
    WristbandManagerCallback mWristbandManagerCallback = new WristbandManagerCallback() {
        @Override
        public void onError(final int error) {
            if(D) Log.d(TAG, "onError, error: " + error);
            showToast(R.string.something_error);

            mWristbandManager.close();
        }
        @Override
        public void onVersionRead(final int appVersion, final int patchVersion) {
            if (D) Log.d(TAG, "onVersionRead");
            //
            mTargetAppVersion = appVersion;
            mTargetPatchVersion = patchVersion;

            mNeedUpdateApp = false;
            mNeedUpdatePatch = false;
            if(mTargetAppVersion < mAppVersion) {
                mNeedUpdateApp = true;
            }
            if(mTargetPatchVersion < mPatchVersion) {
                mNeedUpdatePatch = true;
            }

            new Thread(new Runnable() {
                @Override
                public void run() {

                    if(mNeedUpdateApp) {
                        if(!FileDownloadUtils.loadFile(mAppFileUrl)) {
                            if(D) Log.w(TAG, "App load failed");
                            mNeedUpdateApp = false;
                        }
                    }

                    if(mNeedUpdatePatch) {
                        if(!FileDownloadUtils.loadFile(mPatchFileUrl)) {
                            if(D) Log.w(TAG, "Patch load failed");
                            mNeedUpdatePatch = false;
                        }
                    }

                    // send msg to update ui
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mtvCurrentAppVersion.setText(mTargetAppVersion.toString());
                            mtvCurrentPatchVersion.setText(mTargetPatchVersion.toString());
                            if(mNeedUpdateApp || mNeedUpdatePatch) {
                                mtvUpload.setText(R.string.device_info_have_new_version);
                                mrlUpload.setEnabled(true);
                            } else {
                                if(D) Log.i(TAG, "Nothing need update, do nothing.");
                            }
                        }
                    });
                }
            }).start();

        }
        @Override
        public void onNameRead(final String data) {
            if(D) Log.d(TAG, "onNameRead, name: " + data);
            mDeviceName = data;
        }
    };
}
