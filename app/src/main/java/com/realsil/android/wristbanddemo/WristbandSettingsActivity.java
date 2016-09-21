package com.realsil.android.wristbanddemo;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.realsil.android.wristbanddemo.backgroundscan.BackgroundScanAutoConnected;
import com.realsil.android.wristbanddemo.bmob.BmobControlManager;
import com.realsil.android.wristbanddemo.greendao.SleepData;
import com.realsil.android.wristbanddemo.greendao.SportData;
import com.realsil.android.wristbanddemo.sleep.WristbandHomeFragmentSleep;
import com.realsil.android.wristbanddemo.sport.WristbandHomeFragmentSport;
import com.realsil.android.wristbanddemo.utility.AppHelpFragment;
import com.realsil.android.wristbanddemo.utility.GlobalGatt;
import com.realsil.android.wristbanddemo.utility.GlobalGreenDAO;
import com.realsil.android.wristbanddemo.utility.HighLightView;
import com.realsil.android.wristbanddemo.utility.JudgeActivityFront;
import com.realsil.android.wristbanddemo.utility.RealsilFragmentPagerAdapter;
import com.realsil.android.wristbanddemo.utility.SPWristbandConfigInfo;
import com.realsil.android.wristbanddemo.utility.WristbandManager;
import com.realsil.android.wristbanddemo.utility.WristbandManagerCallback;
import com.realsil.android.wristbanddemo.view.SwipeBackActivity;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import cn.bmob.v3.Bmob;

public class WristbandSettingsActivity extends SwipeBackActivity {
    // Log
    private final static String TAG = "WristbandSettingsActivity";
    private final static boolean D = true;

    private WristbandManager mWristbandManager;

    private GlobalGatt mGlobalGatt;

    private RelativeLayout mrlPersonage;
    private RelativeLayout mrlMyDevice;
    private RelativeLayout mrlSmartLost;
    private RelativeLayout mrlLongSit;
    private RelativeLayout mrlFindBand;
    private RelativeLayout mrlSmartAlarm;
    private RelativeLayout mrlRenameBand;
    private RelativeLayout mrlUpdateBle;
    private RelativeLayout mrlReset;
    private RelativeLayout mrlAbout;
    private RelativeLayout mrlSettingsQuit;

    //private ImageView mivSettingsBleBattery;
    private ImageView mivSettingBack;

    private ImageView mivMyDeviceIcon;
    private ImageView mivMyDeviceBack;

    private CheckBox mcbSmartLostSwitch;
    private CheckBox mcbLongSitSwitch;
    private GlobalGreenDAO mGlobalGreenDAO;

    private boolean mFirstInitialFlag;

    private ProgressDialog mProgressDialog = null;

    private Toast mToast;

    private HighLightView mHighLightView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wristband_settings);

        mFirstInitialFlag = true;

        mHighLightView = new HighLightView(this);

        // get wristband instance
        mWristbandManager = WristbandManager.getInstance();
        //mWristbandManager.registerCallback(mWristbandManagerCallback);
        mGlobalGatt = GlobalGatt.getInstance();

        mGlobalGreenDAO = GlobalGreenDAO.getInstance();

        // set UI
        setUI();

        initialStringFormat();

        // Show Guide window
        showGuide(mrlMyDevice, R.string.guide_setting_my_device);

        BackgroundScanAutoConnected.getInstance().registerCallback(mBackgroundScanCallback);
    }

    private String mFormatMinutesValue;
    private String mFormatHourValue;

    private void initialStringFormat() {
        mFormatMinutesValue = getResources().getString(R.string.minutes_value);
        mFormatHourValue = getResources().getString(R.string.hour_value);
    }

    private void initialUI() {
        if(!mWristbandManager.isConnect()) {
            //mivSettingsBleBattery.setVisibility(View.GONE);
            mcbSmartLostSwitch.setEnabled(false);
            mcbLongSitSwitch.setEnabled(false);
        } else {
            mcbSmartLostSwitch.setEnabled(true);
            mcbLongSitSwitch.setEnabled(true);

            mcbSmartLostSwitch.setChecked(SPWristbandConfigInfo.getControlSwitchLost(this));
            mcbLongSitSwitch.setChecked(SPWristbandConfigInfo.getControlSwitchLongSit(this));

            //int battery = mWristbandManager.getBatteryLevel();// need do this
            // here we just use to read battery info, do not enable notification
            //mivSettingsBleBattery.setImageLevel(battery > 100 ? 100 : battery);
            //mivSettingsBleBattery.setVisibility(View.VISIBLE);
        }

        if(BmobControlManager.checkAPKWorkType()) {
            mrlUpdateBle.setVisibility(View.GONE);
        } else {
            mrlUpdateBle.setVisibility(View.VISIBLE);
        }
    }

    private void setUI() {
        //mivSettingsBleBattery = (ImageView) findViewById(R.id.ivSettingsBleBattery);
        mivSettingBack = (ImageView) findViewById(R.id.ivSettingBack);
        mivSettingBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //mWristbandManager.SendDataRequest();
                finish();
            }
        });

        mrlPersonage = (RelativeLayout) findViewById(R.id.rlPersonage);
        mrlPersonage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(WristbandSettingsActivity.this, WristbandSettingPersonalActivity.class);
                WristbandSettingsActivity.this.startActivity(intent);
            }
        });

        mrlMyDevice = (RelativeLayout) findViewById(R.id.rlMyDevice);
        mrlMyDevice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(WristbandSettingsActivity.this, WristbandSettingScanDeviceActivity.class);
                WristbandSettingsActivity.this.startActivity(intent);
            }
        });

        mrlSmartLost = (RelativeLayout) findViewById(R.id.rlSmartLost);
        /*
        mrlSmartLost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(WristbandSettingsActivity.this, WristbandSettingLostActivity.class);
                WristbandSettingsActivity.this.startActivity(intent);
            }
        });*/

        mrlLongSit = (RelativeLayout) findViewById(R.id.rlLongSit);
        /*mrlLongSit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAlarmTimeDialog();
            }
        });*/


        mrlFindBand = (RelativeLayout) findViewById(R.id.rlFindBand);
        mrlFindBand.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mWristbandManager.isConnect()) {
                    mWristbandManager.enableImmediateAlert(true);
                } else {
                    showToast(R.string.please_connect_band);
                }
            }
        });

        mrlSmartAlarm = (RelativeLayout) findViewById(R.id.rlSmartAlarm);
        mrlSmartAlarm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mWristbandManager.isConnect()) {
                    Intent intent = new Intent(WristbandSettingsActivity.this, WristbandSettingAlarmActivity.class);
                    WristbandSettingsActivity.this.startActivity(intent);
                } else {
                    showToast(R.string.please_connect_band);
                }
            }
        });

        mrlRenameBand = (RelativeLayout) findViewById(R.id.rlRenameBand);
        mrlRenameBand.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //mWristbandManager.SendDataRequest();
                if(mWristbandManager.isConnect()) {
                    final EditText et = new EditText(WristbandSettingsActivity.this);
                    et.setSingleLine(true);
                    String deviceName;
                    String name = SPWristbandConfigInfo.getInfoKeyValue(WristbandSettingsActivity.this, mWristbandManager.getBluetoothAddress());
                    if(name == null) {
                        BluetoothDevice device = mGlobalGatt.getBluetoothAdapter().getRemoteDevice(mWristbandManager.getBluetoothAddress());
                        deviceName = device.getName();
                    } else {
                        deviceName = name;
                    }
                    //et.setInputType(SINGLE);
                    new AlertDialog.Builder(WristbandSettingsActivity.this, AlertDialog.THEME_HOLO_LIGHT)
                            .setTitle(R.string.settings_mydevice_rename_bracelet)
                            .setMessage(getResources().getString(R.string.current_device_name) + deviceName)
                            .setView(et)
                            .setPositiveButton(R.string.sure, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    String input = et.getText().toString().trim();
                                    if (input.equals("")) {
                                        showToast(R.string.name_should_not_null);
                                    } else if(input.getBytes().length > 20) {
                                        if(D) Log.d(TAG, "input.getBytes().length: " + input.getBytes().length);
                                        showToast(R.string.name_too_long);
                                    }
                                    else {
                                        if(D) Log.d(TAG, "set the device name, name: " + input);
                                        mWristbandManager.setDeviceName(input);
                                    }
                                }
                            })
                            .setNegativeButton(R.string.cancel, null)
                            .show();
                } else {
                    showToast(R.string.please_connect_band);
                }
            }
        });
        mrlUpdateBle = (RelativeLayout) findViewById(R.id.rlUpdateBle);
        mrlUpdateBle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mWristbandManager.isConnect()) {
                    int batteryLevel = mWristbandManager.getBatteryLevel();
                    if(batteryLevel >= 0
                            && batteryLevel < 60) {
                        showToast(String.format(getString(R.string.dfu_battery_not_enough), batteryLevel));
                        if(D) Log.e(TAG, "the battery level is too low. batteryLevel: " + batteryLevel);
                        return;
                    }
                    Intent intent = new Intent(WristbandSettingsActivity.this, WristbandOtaActivity.class);
                    WristbandSettingsActivity.this.startActivity(intent);
                } else {
                    showToast(R.string.please_connect_band);
                }
            }
        });
        mrlReset = (RelativeLayout) findViewById(R.id.rlReset);
        mrlReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!mWristbandManager.isConnect()) {
                    showToast(R.string.please_connect_band);
                    return;
                }
                new AlertDialog.Builder(WristbandSettingsActivity.this, AlertDialog.THEME_HOLO_LIGHT)
                        .setTitle(R.string.settings_about_factory_data_reset)
                        .setMessage(R.string.settings_about_factory_data_reset_tip)
                        .setPositiveButton(R.string.sure, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                if (D) Log.d(TAG, "reset");
                                if(mWristbandManager.isConnect()) {
                                    mWristbandManager.SendRemoveBondCommand();
                                    // remote will disconnect
                                    //mWristbandManager.close();
                                }
                                mGlobalGreenDAO.deleteAllSportData();
                                mGlobalGreenDAO.deleteAllSleepData();
                                //mWristbandManager.close();
                                SPWristbandConfigInfo.deleteAll(WristbandSettingsActivity.this);
								
								// Resume first app start bit.
								SPWristbandConfigInfo.setFirstAppStartFlag(WristbandSettingsActivity.this, false);

                                initialUI();
                            }
                        })
                        .setNegativeButton(R.string.cancel, null)
                        .show();
            }
        });
        mrlAbout = (RelativeLayout) findViewById(R.id.rlAbout);
        mrlAbout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final AppHelpFragment fragment = AppHelpFragment.getInstance(R.string.about_text, true);
                fragment.show(getFragmentManager(), "help_fragment");
            }
        });

        mrlSettingsQuit = (RelativeLayout) findViewById(R.id.rlSettingsQuit);
        mrlSettingsQuit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder aa = new AlertDialog.Builder(WristbandSettingsActivity.this, AlertDialog.THEME_HOLO_LIGHT);
                aa.setTitle(R.string.exit_app_title);
                aa.setMessage(R.string.exit_app_text);
                aa.setPositiveButton(R.string.sure, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // TODO Auto-generated method stub
                        dialog.dismiss();
                        close();
                    }
                });
                aa.setNegativeButton(R.string.cancel, null);
                aa.create();
                aa.show();
            }
        });

        mcbSmartLostSwitch = (CheckBox) findViewById(R.id.cbSmartLostSwitch);
        mcbSmartLostSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, final boolean isChecked) {
                // judge need to update
                if(mFirstInitialFlag
                        || isChecked == SPWristbandConfigInfo.getControlSwitchLost(WristbandSettingsActivity.this)) {
                    return;
                }
                if(mWristbandManager.isConnect()) {
                    SPWristbandConfigInfo.setControlSwitchLost(WristbandSettingsActivity.this, isChecked);
                    showProgressBar(R.string.settings_mydevice_syncing_user_profile);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            mWristbandManager.enableLinkLossAlert(isChecked);
                            cancelProgressBar();
                        }
                    }).start();
                } else {
                    showToast(R.string.please_connect_band);
                }
            }
        });

        mcbLongSitSwitch = (CheckBox) findViewById(R.id.cbLongSitSwitch);
        mcbLongSitSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, final boolean isChecked) {
                // judge need to update
                if(mFirstInitialFlag
                        || isChecked == SPWristbandConfigInfo.getControlSwitchLongSit(WristbandSettingsActivity.this)) {
                    if(D) Log.d(TAG, "mFirstInitialFlag: " + mFirstInitialFlag
                            + ", isChecked: " + isChecked
                            + ", SPWristbandConfigInfo.getControlSwitchLongSit(WristbandSettingsActivity.this): " + SPWristbandConfigInfo.getControlSwitchLongSit(WristbandSettingsActivity.this));
                    return;
                }
                if(mWristbandManager.isConnect()) {
                    SPWristbandConfigInfo.setControlSwitchLongSit(WristbandSettingsActivity.this, isChecked);
                    showProgressBar(R.string.settings_mydevice_syncing_user_profile);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            mWristbandManager.SetLongSit(isChecked);
                            cancelProgressBar();
                        }
                    }).start();
                    if(isChecked) {
                        showToast(R.string.settings_mydevice_long_sit_alarm_time_toast);
                    }
                } else {
                    showToast(R.string.please_connect_band);
                }
            }
        });
    }
    private int selectedIndex = 0;
    private void showAlarmTimeDialog() {

        final int[] arrayAlarmTimeValue = new int[] { 15, 30, 60, 180};
        final String[] arrayAlarmTime = new String[] { String.format(mFormatMinutesValue, arrayAlarmTimeValue[0])
                , String.format(mFormatMinutesValue, arrayAlarmTimeValue[1])
                , String.format(mFormatHourValue, arrayAlarmTimeValue[2]/60)
                , String.format(mFormatHourValue, arrayAlarmTimeValue[3]/60)};

        int defSelect = 0;
        for(int i = 0; i < arrayAlarmTimeValue.length; i ++) {
            if(arrayAlarmTimeValue[i] == SPWristbandConfigInfo.getLongSitAlarmTime(WristbandSettingsActivity.this)) {
                defSelect = i;
            }
        }
        Dialog dialog = new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_LIGHT)
                .setTitle(R.string.settings_mydevice_long_sit_alarm_time)
                .setPositiveButton(R.string.sure, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        // TODO Auto-generated method stub
                        SPWristbandConfigInfo.setLongSitAlarmTime(WristbandSettingsActivity.this, arrayAlarmTimeValue[selectedIndex]);
                        if(mcbLongSitSwitch.isChecked()) {
                            if(mWristbandManager.isConnect()) {
                                showProgressBar(R.string.settings_mydevice_syncing_user_profile);
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        mWristbandManager.SetLongSit(true);
                                        cancelProgressBar();
                                    }
                                }).start();
                            }
                        }
                    }
                })
                .setSingleChoiceItems(arrayAlarmTime
                        , defSelect
                        , new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,
                                        int which) {
                        // TODO Auto-generated method stub
                        selectedIndex = which;
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        // TODO Auto-generated method stub
                        dialog.dismiss();
                    }
                }).create();
        dialog.show();
    }

    // Use for close all activity
    public void close() {
        //mWristbandManager.close();

        Intent intent = new Intent();
        intent.setAction("WRISTBAND_CLOSE_BROADCAST");
        sendBroadcast(intent);
        finish();
    }

    private void showProgressBar(final int message) {
        mProgressDialog = ProgressDialog.show(WristbandSettingsActivity.this
                , null
                , getResources().getString(message)
                , true);
        mProgressDialog.setCancelable(false);

        mProgressBarSuperHandler.postDelayed(mProgressBarSuperTask, 30 * 1000);
    }

    private void showProgressBar(final String message) {
        mProgressDialog = ProgressDialog.show(WristbandSettingsActivity.this
                , null
                , message
                , true);
        mProgressDialog.setCancelable(false);

        mProgressBarSuperHandler.postDelayed(mProgressBarSuperTask, 30 * 1000);
    }

    private void cancelProgressBar() {
        if(mProgressDialog != null) {
            if(mProgressDialog.isShowing()) {
                mProgressDialog.cancel();
            }
        }

        mProgressBarSuperHandler.removeCallbacks(mProgressBarSuperTask);
    }

    // Alarm timer
    Handler mProgressBarSuperHandler = new Handler();
    Runnable mProgressBarSuperTask = new Runnable(){
        @Override
        public void run() {
            // TODO Auto-generated method stub
            if(D) Log.w(TAG, "Wait Progress Timeout");
            showToast(R.string.progress_bar_timeout);
            mWristbandManager.close();
            // stop timer
            cancelProgressBar();
        }
    };
    private void showToast(final String message) {
        if(mToast == null) {
            mToast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
        } else {
            mToast.setText(message);
        }
        mToast.show();
    }
    private void showToast(final int message) {
        if(mToast == null) {
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
        // initial UI
        initialUI();

        syncLongSitAndBattery();

        BackgroundScanAutoConnected.getInstance().startAutoConnect();
    }

    @Override
    protected void onPause() {
        if(D) Log.d(TAG, "onPause()");
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if(D) Log.d(TAG, "onDestroy()");
        super.onDestroy();

        mWristbandManager.unRegisterCallback(mWristbandManagerCallback);
        BackgroundScanAutoConnected.getInstance().unregisterCallback(mBackgroundScanCallback);
    }

    public void syncLongSitAndBattery() {
        mWristbandManager.registerCallback(mWristbandManagerCallback);

        if(mWristbandManager.isConnect()) {
            if (!mWristbandManager.isReady()
                    || mWristbandManager.isInSendCommand()) {
                if(D) Log.w(TAG, "Not login or is in sending command, maybe something wrong!");
                /*
                mWristbandManager.close();
                */
                return;
            }
            // here we need read
            new Thread(new Runnable() {
                @Override
                public void run() {
                    // Check again
                    if (!mWristbandManager.isReady()
                            || mWristbandManager.isInSendCommand()) {
                        if(D) Log.w(TAG, "Not login or is in sending command, maybe something wrong!");
                        return;
                    }
                    // Add first initial flag
                    if(SPWristbandConfigInfo.getFirstInitialFlag(WristbandSettingsActivity.this)) {
                        if (!mWristbandManager.SendLongSitRequest()) {
                            if(mFirstInitialFlag) {
                                mFirstInitialFlag = false;
                            }
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    showToast(R.string.something_error);
                                }
                            });
                            //mWristbandManager.close();
                        }
                    } else {
                        if(mFirstInitialFlag) {
                            mFirstInitialFlag = false;
                        }
                        /*
                        if(D) Log.d(TAG, "Is initialed, just update battery level.");
                        if (!mWristbandManager.readBatteryLevel()) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    showToast(R.string.something_error);
                                }
                            });
                            //mWristbandManager.close();
                        }*/
                    }

                }
            }).start();
        }
    }

    // Application Layer callback
    WristbandManagerCallback mWristbandManagerCallback = new WristbandManagerCallback() {
        /*
        @Override
        public void onBatteryRead(int value) {
            if(D) Log.d(TAG, "onBatteryRead, value: " + value);
            if(mFirstInitialFlag) {
                mFirstInitialFlag = false;
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    initialUI();
                }
            });
        }*/

        @Override
        public void onLongSitSettingReceive(byte data) {
            if(D) Log.d(TAG, "onLongSitSettingReceive, data: " + data);
            if(mFirstInitialFlag) {
                mFirstInitialFlag = false;
            }
            SPWristbandConfigInfo.setFirstInitialFlag(WristbandSettingsActivity.this, false);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    initialUI();
                }
            });
            /*
            new Thread(new Runnable() {
                @Override
                public void run() {
                    // Check again
                    if (!mWristbandManager.isReady()
                            || mWristbandManager.isInSendCommand()) {
                        if(D) Log.w(TAG, "onLongSitSettingReceive, is in sending command!");
                        return;
                    }
                    mWristbandManager.readBatteryLevel();
                }
            }).start();*/
        }
    };

    BackgroundScanAutoConnected.BackgroundScanCallback mBackgroundScanCallback
            = new BackgroundScanAutoConnected.BackgroundScanCallback() {
        public void onWristbandLoginStateChange(boolean connected) {
            if(D) Log.d(TAG, "onWristbandLoginStateChange, connected: " + connected);
            if(connected) {
                syncLongSitAndBattery();
            } else {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        initialUI();
                    }
                });
            }
        }
    };

    private void showGuide(View v, int id) {
        if(D) Log.w(TAG, "showGuide");
        String s = getResources().getString(id);
        showGuide(v, s);
    }
    private void showGuide(View v, String s) {
        if(!isFirstLoad()) {
            return;
        }
        final int defaultOrientation = getRequestedOrientation();
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
        if(D) Log.w(TAG, "showGuide, s: " + s);
        mHighLightView.showTipForView(v, s, HighLightView.HIGH_LIGHT_VIEW_TYPE_RECT, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setRequestedOrientation(defaultOrientation);
                v.callOnClick();
            }
        });
    }
    private boolean isFirstLoad() {
        return SPWristbandConfigInfo.getFirstAppStartFlag(this);
    }
}
