package com.realsil.android.wristbanddemo;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.realsil.android.wristbanddemo.applicationlayer.ApplicationLayer;
import com.realsil.android.wristbanddemo.applicationlayer.ApplicationLayerAlarmPacket;
import com.realsil.android.wristbanddemo.applicationlayer.ApplicationLayerAlarmsPacket;
import com.realsil.android.wristbanddemo.backgroundscan.BackgroundScanAutoConnected;
import com.realsil.android.wristbanddemo.notifybroadcast.NotificationReceive;
import com.realsil.android.wristbanddemo.utility.AlarmInfoFragment;
import com.realsil.android.wristbanddemo.utility.SPWristbandConfigInfo;
import com.realsil.android.wristbanddemo.utility.WristbandManager;
import com.realsil.android.wristbanddemo.utility.WristbandManagerCallback;
import com.realsil.android.wristbanddemo.view.SwipeBackActivity;

import java.util.Calendar;
import java.util.List;

public class WristbandSettingAlarmActivity extends SwipeBackActivity implements AlarmInfoFragment.OnSaveListener {
    // Log
    private final static String TAG = "WristbandSettingAlarmActivity";
    private final static boolean D = true;

    RelativeLayout mrlAddAlarm;

    private ListView mlvWristbandAlarm;

    private CheckBox mcbCallRemind;
    private CheckBox mcbSmsRemind;
    private CheckBox mcbQQRemind;
    private CheckBox mcbWechatRemind;

    private ImageView mivAlarmBack;

    private WristbandManager mWristbandManager;

    private ProgressDialog mProgressDialog = null;

    private boolean mFirstInitialFlag;

    private static final String ENABLED_NOTIFICATION_LISTENERS = "enabled_notification_listeners";
    private static final String ACTION_NOTIFICATION_LISTENER_SETTINGS = "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS";

    TextView mtvClockDayFlagOne;
    TextView mtvClockTimeOne;
    RelativeLayout mrlClockOne;
    CheckBox mcbClockOne;

    TextView mtvClockDayFlagTwo;
    TextView mtvClockTimeTwo;
    RelativeLayout mrlClockTwo;
    CheckBox mcbClockTwo;

    TextView mtvClockDayFlagThree;
    TextView mtvClockTimeThree;
    RelativeLayout mrlClockThree;
    CheckBox mcbClockThree;

    private byte mDayFlagOne;
    private byte mDayFlagTwo;
    private byte mDayFlagThree;

    private Toast mToast;

    private boolean isQQEnable = false, isWechatEnable = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wristband_alarm);

        mFirstInitialFlag = true;

        mWristbandManager = WristbandManager.getInstance();
        mWristbandManager.registerCallback(mWristbandManagerCallback);
        // set UI
        setUI();

        initialStringFormat();

        initialUI();

        syncNotifyAndAlarmSetting();
    }

    private String mFormatMaxAlarms;

    private void initialStringFormat() {
        mFormatMaxAlarms = getResources().getString(R.string.settings_mydevice_reach_max_alarm);
    }

    private void initialUI() {
        mcbCallRemind.setChecked(SPWristbandConfigInfo.getNotifyCallFlag(this));
        mcbSmsRemind.setChecked(SPWristbandConfigInfo.getNotifyMessageFlag(this));
        mcbQQRemind.setChecked(SPWristbandConfigInfo.getNotifyQQFlag(this));
        mcbWechatRemind.setChecked(SPWristbandConfigInfo.getNotifyWechatFlag(this));

        String timeOne = SPWristbandConfigInfo.getAlarmTimeOne(WristbandSettingAlarmActivity.this);
        mtvClockTimeOne.setText((timeOne == null) ? "12:00" : timeOne);
        mDayFlagOne = SPWristbandConfigInfo.getAlarmFlagOne(WristbandSettingAlarmActivity.this);
        mtvClockDayFlagOne.setText(getDayFlagString(mDayFlagOne));

        String timeTwo = SPWristbandConfigInfo.getAlarmTimeTwo(WristbandSettingAlarmActivity.this);
        mtvClockTimeTwo.setText((timeTwo == null) ? "12:00" : timeTwo);
        mDayFlagTwo = SPWristbandConfigInfo.getAlarmFlagTwo(WristbandSettingAlarmActivity.this);
        mtvClockDayFlagTwo.setText(getDayFlagString(mDayFlagTwo));

        String timeThree = SPWristbandConfigInfo.getAlarmTimeThree(WristbandSettingAlarmActivity.this);
        mtvClockTimeThree.setText((timeThree == null) ? "12:00" : timeThree);
        mDayFlagThree = SPWristbandConfigInfo.getAlarmFlagThree(WristbandSettingAlarmActivity.this);
        mtvClockDayFlagThree.setText(getDayFlagString(mDayFlagThree));
    }

    private String getDayFlagString(byte flag) {
        String flagString = "";

        if((flag & 0xff) == ApplicationLayer.REPETITION_NULL
                && flagString.equals("")) {
            flagString = getString(R.string.settings_mydevice_setting_alarm_dayflag_onetime);
        }
        if((flag & 0xff) == ApplicationLayer.REPETITION_ALL
                && flagString.equals("")) {
            flagString = getString(R.string.settings_mydevice_setting_alarm_dayflag_every_day);
        }
        if(flagString.equals("")) {
            if ((flag & ApplicationLayer.REPETITION_MON) != 0) {
                flagString = flagString + (flagString.equals("") ? "" : ", ") + getString(R.string.monday_week);
            }
            if ((flag & ApplicationLayer.REPETITION_TUES) != 0) {
                flagString = flagString + (flagString.equals("") ? "" : ", ") + getString(R.string.tuesday_week);
            }
            if ((flag & ApplicationLayer.REPETITION_WED) != 0) {
                flagString = flagString + (flagString.equals("") ? "" : ", ") + getString(R.string.wednesday_week);
            }
            if ((flag & ApplicationLayer.REPETITION_THU) != 0) {
                flagString = flagString + (flagString.equals("") ? "" : ", ") + getString(R.string.thursday_week);
            }
            if ((flag & ApplicationLayer.REPETITION_FRI) != 0) {
                flagString = flagString + (flagString.equals("") ? "" : ", ") + getString(R.string.friday_week);
            }
            if ((flag & ApplicationLayer.REPETITION_SAT) != 0) {
                flagString = flagString + (flagString.equals("") ? "" : ", ") + getString(R.string.saturday_week);
            }
            if ((flag & ApplicationLayer.REPETITION_SUN) != 0) {
                flagString = flagString + (flagString.equals("") ? "" : ", ") + getString(R.string.sunday_week);
            }
        }
        return flagString;
    }

    private void setUI() {
        Typeface type = Typeface.createFromAsset(getAssets(), "fonts/american_typewriter.ttf");

        mivAlarmBack = (ImageView) findViewById(R.id.ivAlarmBack);
        mivAlarmBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        mcbCallRemind = (CheckBox) findViewById(R.id.cbCallRemind);
        mcbCallRemind.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // judge need to update
                if (mFirstInitialFlag
                        || isChecked == SPWristbandConfigInfo.getNotifyCallFlag(WristbandSettingAlarmActivity.this)) {
                    return;
                }
                if (mWristbandManager.isConnect()) {
                    SPWristbandConfigInfo.setNotifyCallFlag(WristbandSettingAlarmActivity.this, isChecked);
                    syncNotifySetting(isChecked ? ApplicationLayer.CALL_NOTIFY_MODE_ON : ApplicationLayer.CALL_NOTIFY_MODE_OFF);
                } else {
                    showToast(R.string.please_connect_band);
                }
            }
        });

        mcbSmsRemind = (CheckBox) findViewById(R.id.cbSmsRemind);
        mcbSmsRemind.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // judge need to update
                if (mFirstInitialFlag
                        || isChecked == SPWristbandConfigInfo.getNotifyMessageFlag(WristbandSettingAlarmActivity.this)) {
                    return;
                }
                if (mWristbandManager.isConnect()) {
                    SPWristbandConfigInfo.setNotifyMessageFlag(WristbandSettingAlarmActivity.this, isChecked);
                    syncNotifySetting(isChecked ? ApplicationLayer.CALL_NOTIFY_MODE_ENABLE_MESSAGE : ApplicationLayer.CALL_NOTIFY_MODE_DISABLE_MESSAGE);
                } else {
                    showToast(R.string.please_connect_band);
                }
            }
        });

        mcbQQRemind = (CheckBox) findViewById(R.id.cbQQRemind);
        mcbQQRemind.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // judge need to update
                if (mFirstInitialFlag
                        || isChecked == SPWristbandConfigInfo.getNotifyQQFlag(WristbandSettingAlarmActivity.this)) {
                    return;
                }
                if(isChecked) {
                    if(!isNotifyManageEnabled()) {
                        if(D) Log.d(TAG, "Notify manager didn't enable.");
                        showConfirmDialog(1);

                        mcbQQRemind.setChecked(false);
                        return;
                    }
                }
                if (mWristbandManager.isConnect()) {
                    SPWristbandConfigInfo.setNotifyQQFlag(WristbandSettingAlarmActivity.this, isChecked);
                    syncNotifySetting(isChecked ? ApplicationLayer.CALL_NOTIFY_MODE_ENABLE_QQ : ApplicationLayer.CALL_NOTIFY_MODE_DISABLE_QQ);
                } else {
                    showToast(R.string.please_connect_band);
                }
            }
        });

        mcbWechatRemind = (CheckBox) findViewById(R.id.cbWechatRemind);
        mcbWechatRemind.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // judge need to update
                if (mFirstInitialFlag
                        || isChecked == SPWristbandConfigInfo.getNotifyWechatFlag(WristbandSettingAlarmActivity.this)) {
                    return;
                }
                if (isChecked) {
                    if (!isNotifyManageEnabled()) {
                        if (D) Log.d(TAG, "Notify manager didn't enable.");
                        showConfirmDialog(2);

                        mcbWechatRemind.setChecked(false);
                        return;
                    }
                }
                if (mWristbandManager.isConnect()) {
                    SPWristbandConfigInfo.setNotifyWechatFlag(WristbandSettingAlarmActivity.this, isChecked);
                    syncNotifySetting(isChecked ? ApplicationLayer.CALL_NOTIFY_MODE_ENABLE_WECHAT : ApplicationLayer.CALL_NOTIFY_MODE_DISABLE_WECHAT);
                } else {
                    showToast(R.string.please_connect_band);
                }
            }
        });

        mtvClockDayFlagOne = (TextView)findViewById(R.id.tvClockDayFlagOne);
        mcbClockOne = (CheckBox)findViewById(R.id.cbClockOne);
        mcbClockOne.setOnCheckedChangeListener(checkChangeListener);

        mtvClockTimeOne = (TextView)findViewById(R.id.tvClockTimeOne);
        mtvClockTimeOne.setTypeface(type);
        mrlClockOne = (RelativeLayout)findViewById(R.id.rlClockOne);
        mrlClockOne.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String timeString[] = mtvClockTimeOne.getText().toString().split(":");
                int hour = Integer.valueOf(timeString[0]);
                int minute = Integer.valueOf(timeString[1]);

                showTimeInfoDialog(0, hour, minute, mDayFlagOne);
            }
        });

        mtvClockDayFlagTwo = (TextView)findViewById(R.id.tvClockDayFlagTwo);
        mcbClockTwo = (CheckBox)findViewById(R.id.cbClockTwo);
        mcbClockTwo.setOnCheckedChangeListener(checkChangeListener);

        mtvClockTimeTwo = (TextView)findViewById(R.id.tvClockTimeTwo);
        mtvClockTimeTwo.setTypeface(type);
        mrlClockTwo = (RelativeLayout)findViewById(R.id.rlClockTwo);
        mrlClockTwo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String timeString[] = mtvClockTimeTwo.getText().toString().split(":");
                int hour = Integer.valueOf(timeString[0]);
                int minute = Integer.valueOf(timeString[1]);

                showTimeInfoDialog(1, hour, minute, mDayFlagTwo);
            }
        });

        mtvClockDayFlagThree = (TextView)findViewById(R.id.tvClockDayFlagThree);
        mcbClockThree = (CheckBox)findViewById(R.id.cbClockThree);
        mcbClockThree.setOnCheckedChangeListener(checkChangeListener);

        mtvClockTimeThree = (TextView)findViewById(R.id.tvClockTimeThree);
        mtvClockTimeThree.setTypeface(type);
        mrlClockThree = (RelativeLayout)findViewById(R.id.rlClockThree);
        mrlClockThree.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String timeString[] = mtvClockTimeThree.getText().toString().split(":");
                int hour = Integer.valueOf(timeString[0]);
                int minute = Integer.valueOf(timeString[1]);

                showTimeInfoDialog(2, hour, minute, mDayFlagThree);
            }
        });
    }

    private void saveAlarm() {
        showProgressBar(R.string.settings_mydevice_setting_alarm);
        new Thread(new Runnable() {
            @Override
            public void run() {
                if(syncAlarmListToRemote()) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showToast(R.string.settings_mydevice_setting_alarm_success);
                        }
                    });
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showToast(R.string.settings_mydevice_setting_alarm_failed);
                        }
                    });
                }
                cancelProgressBar();
            }
        }).start();
    }

    CompoundButton.OnCheckedChangeListener checkChangeListener = new CompoundButton.OnCheckedChangeListener() {

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (mFirstInitialFlag) {
                return;
            }
            saveAlarm();
        }
    };

    private void checkNotifyManage() {
        if(D) Log.d(TAG, "checkNotifyManage");
        if (!isNotifyManageEnabled()) {
            if(D) Log.d(TAG, "Notify manager didn't enable.");
            showConfirmDialog(0);
        }
    }

    private boolean isNotifyManageEnabled() {
        if(D) Log.d(TAG, "isNotifyManageEnabled");
        String pkgName = getPackageName();
        final String flat = Settings.Secure.getString(getContentResolver(),
                ENABLED_NOTIFICATION_LISTENERS);
        if (!TextUtils.isEmpty(flat)) {
            final String[] names = flat.split(":");
            for (int i = 0; i < names.length; i++) {
                final ComponentName cn = ComponentName.unflattenFromString(names[i]);
                if (cn != null) {
                    if (TextUtils.equals(pkgName, cn.getPackageName())) {
                        if(isNLServiceCrashed()) {
                            // Try to restart it!
                            if(D) Log.e(TAG, "Try to restart it again!");
                            showToast(R.string.something_error_in_notification_service);
                            openNotificationAccess();
                        }
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isNLServiceCrashed() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> runningServiceInfos = manager.getRunningServices(Integer.MAX_VALUE);

        if (runningServiceInfos != null) {
            for (ActivityManager.RunningServiceInfo service : runningServiceInfos) {

                //NotificationReceive.class is the name of my class (the one that has to extend from NotificationListenerService)
                if (NotificationReceive.class.getName().equals(service.service.getClassName())) {

                    if (service.crashCount > 0) {
                        // in this situation we know that the notification listener service is not working for the app
                        if(D) Log.e(TAG, "!!!ERROR!!! Notification listener service didn't started!");
                        return true;
                    }
                    return false;
                }
            }
        }
        return false;
    }

    private void showConfirmDialog(final int i) {
        if(D) Log.d(TAG, "showConfirmDialog.");
        new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_LIGHT)
                .setMessage(R.string.settings_push_message_notify_access)
                .setTitle(R.string.settings_push_message_notify_access_title)
                .setCancelable(true)
                .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                if (i == 1) {
                                    isQQEnable = true;
                                    isWechatEnable = false;
                                } else if(i == 2) {
                                    isQQEnable = false;
                                    isWechatEnable = true;
                                }
                                openNotificationAccess();
                            }
                        })
                .setNegativeButton(android.R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // do nothing
                            }
                        })
                .create().show();
    }
    private void openNotificationAccess() {
        startActivity(new Intent(ACTION_NOTIFICATION_LISTENER_SETTINGS));
    }

    private void syncNotifySetting(final byte mode) {
        showProgressBar(R.string.settings_mydevice_syncing_remind_setting);
        new Thread(new Runnable() {
            @Override
            public void run() {
                if(!mWristbandManager.SetNotifyMode(mode)) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showToast(R.string.something_error);
                        }
                    });
                    //mWristbandManager.close();
                }

                cancelProgressBar();
            }
        }).start();
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
        if(mToast == null) {
            mToast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
        } else {
            mToast.setText(message);
        }
        mToast.show();
    }

    private void showProgressBar(final int message) {
        mProgressDialog = ProgressDialog.show(WristbandSettingAlarmActivity.this
                , null
                , getResources().getString(message)
                , true);
        mProgressDialog.setCancelable(false);

        mProgressBarSuperHandler.postDelayed(mProgressBarSuperTask, 30 * 1000);
    }

    private void showProgressBar(final String message) {
        mProgressDialog = ProgressDialog.show(WristbandSettingAlarmActivity.this
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

    private void showTimeInfoDialog(int position, int hour, int minutes, byte dayFlag){
        final FragmentManager fm = getFragmentManager();
        // start le scan, with no filter
        final AlarmInfoFragment dialog = AlarmInfoFragment.getInstance(this);

        Bundle bundle = new Bundle();
        bundle.putInt(AlarmInfoFragment.EXTRAS_VALUE_POSITION, position);
        bundle.putInt(AlarmInfoFragment.EXTRAS_DEFAULT_HOUR, hour);
        bundle.putInt(AlarmInfoFragment.EXTRAS_DEFAULT_MINUTE, minutes);
        bundle.putByte(AlarmInfoFragment.EXTRAS_DEFAULT_DAY_FLAG, dayFlag);
        dialog.setArguments(bundle);

        dialog.show(fm, "alarm_fragment");
    }

    private boolean syncAlarmListToRemote() {
        Calendar c1  = Calendar.getInstance();

        byte dayFlagOne = mDayFlagOne;
        int hourOne, minuteOne;
        boolean dayAddFlagOne = false;
        String timeStringOne[] = mtvClockTimeOne.getText().toString().split(":");
        hourOne = Integer.valueOf(timeStringOne[0]);
        minuteOne = Integer.valueOf(timeStringOne[1]);
        if(D) Log.d(TAG, "syncAlarmListToRemote, hourOne: " + hourOne + ", minuteOne: " + minuteOne);
        if(dayFlagOne == 0x00) {
            if(hourOne < c1.get(Calendar.HOUR_OF_DAY)) {
                dayAddFlagOne = true;
            } else if((hourOne == c1.get(Calendar.HOUR_OF_DAY))
                    && (minuteOne <= c1.get(Calendar.MINUTE))) {
                dayAddFlagOne = true;
            }
        }
        Calendar c2  = Calendar.getInstance();
        c2.add(Calendar.DATE, (dayAddFlagOne? 1: 0));

        ApplicationLayerAlarmPacket alarmOne =
                new ApplicationLayerAlarmPacket(c2.get(Calendar.YEAR),
                        c2.get(Calendar.MONTH) + 1,// here need add 1, because it origin range is 0 - 11;
                        c2.get(Calendar.DATE),
                        hourOne,
                        minuteOne,
                        0,
                        dayFlagOne);

        byte dayFlagTwo = mDayFlagTwo;
        int hourTwo, minuteTwo;
        boolean dayAddFlagTwo = false;
        String timeStringTwo[] = mtvClockTimeTwo.getText().toString().split(":");
        hourTwo = Integer.valueOf(timeStringTwo[0]);
        minuteTwo = Integer.valueOf(timeStringTwo[1]);
        if(D) Log.d(TAG, "syncAlarmListToRemote, hourTwo: " + hourTwo + ", minuteTwo: " + minuteTwo);
        if(dayFlagTwo == 0x00) {
            if(hourTwo < c1.get(Calendar.HOUR_OF_DAY)) {
                dayAddFlagTwo = true;
            } else if((hourTwo == c1.get(Calendar.HOUR_OF_DAY))
                    && (minuteTwo <= c1.get(Calendar.MINUTE))) {
                dayAddFlagTwo = true;
            }
        }
        c2  = Calendar.getInstance();
        c2.add(Calendar.DATE, (dayAddFlagTwo? 1: 0));
        ApplicationLayerAlarmPacket alarmTwo =
                new ApplicationLayerAlarmPacket(c2.get(Calendar.YEAR),
                        c2.get(Calendar.MONTH) + 1,// here need add 1, because it origin range is 0 - 11;
                        c2.get(Calendar.DATE),
                        hourTwo,
                        minuteTwo,
                        1,
                        dayFlagTwo);

        byte dayFlagThree = mDayFlagThree;
        int hourThree, minuteThree;
        boolean dayAddFlagThree = false;
        String timeStringThree[] = mtvClockTimeThree.getText().toString().split(":");
        hourThree = Integer.valueOf(timeStringThree[0]);
        minuteThree = Integer.valueOf(timeStringThree[1]);
        if(D) Log.d(TAG, "syncAlarmListToRemote, hourThree: " + hourThree + ", minuteThree: " + minuteThree);
        if(dayFlagThree == 0x00) {
            if(hourThree < c1.get(Calendar.HOUR_OF_DAY)) {
                dayAddFlagThree = true;
            } else if((hourThree == c1.get(Calendar.HOUR_OF_DAY))
                    && (minuteThree <= c1.get(Calendar.MINUTE))) {
                dayAddFlagThree = true;
            }
        }
        c2  = Calendar.getInstance();
        c2.add(Calendar.DATE, (dayAddFlagThree? 1: 0));
        ApplicationLayerAlarmPacket alarmThree =
                new ApplicationLayerAlarmPacket(c2.get(Calendar.YEAR),
                        c2.get(Calendar.MONTH) + 1,// here need add 1, because it origin range is 0 - 11;
                        c2.get(Calendar.DATE),
                        hourThree,
                        minuteThree,
                        2,
                        dayFlagThree);

        ApplicationLayerAlarmsPacket alarmsPacket = new ApplicationLayerAlarmsPacket();
        if(mcbClockOne.isChecked() == true) {
            alarmsPacket.add(alarmOne);
        }
        if(mcbClockTwo.isChecked() == true) {
            alarmsPacket.add(alarmTwo);
        }
        if(mcbClockThree.isChecked() == true) {
            alarmsPacket.add(alarmThree);
        }

        SPWristbandConfigInfo.setAlarmTimeOne(WristbandSettingAlarmActivity.this, mtvClockTimeOne.getText().toString());
        SPWristbandConfigInfo.setAlarmFlagOne(WristbandSettingAlarmActivity.this, dayFlagOne);
        SPWristbandConfigInfo.setAlarmTimeTwo(WristbandSettingAlarmActivity.this, mtvClockTimeTwo.getText().toString());
        SPWristbandConfigInfo.setAlarmFlagTwo(WristbandSettingAlarmActivity.this, dayFlagTwo);
        SPWristbandConfigInfo.setAlarmTimeThree(WristbandSettingAlarmActivity.this, mtvClockTimeThree.getText().toString());
        SPWristbandConfigInfo.setAlarmFlagThree(WristbandSettingAlarmActivity.this, dayFlagThree);

        if(mWristbandManager.isConnect()) {
            if(alarmsPacket.size() == 0) {
                if(!mWristbandManager.SetClocks(null)) {
                    return false;
                }
            } else {
                if(!mWristbandManager.SetClocks(alarmsPacket)){
                    return false;
                }
            }
            return true;
        } else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showToast(R.string.please_connect_band);
                }
            });
            return false;
        }


    }


    private void syncAlarmList() {
        showProgressBar(R.string.settings_mydevice_syncing_alarm);
        new Thread(new Runnable() {
            @Override
            public void run() {
                if(!mWristbandManager.SetClocksSyncRequest()) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showToast(R.string.something_error);
                        }
                    });
                    //mWristbandManager.close();
                }
                cancelProgressBar();
            }
        }).start();
        //showToast(R.string.settings_mydevice_syncing_alarm);
    }

    private void syncNotifyAndAlarmSetting() {
        if(D) Log.d(TAG, "syncNotifyAndAlarmSetting");
        showProgressBar(R.string.settings_mydevice_syncing_alarm_and_notify);
        new Thread(new Runnable() {
            @Override
            public void run() {
                if(!mWristbandManager.SendNotifyModeRequest()) {
                    cancelProgressBar();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showToast(R.string.something_error);
                            finish();
                        }
                    });
                    //mWristbandManager.close();
                }
            }
        }).start();
        //showToast(R.string.settings_mydevice_syncing_alarm);
    }

    // Application Layer callback
    WristbandManagerCallback mWristbandManagerCallback = new WristbandManagerCallback() {
        @Override
        public void onAlarmDataReceive(final ApplicationLayerAlarmPacket data) {
            if (D) Log.d(TAG, "onAlarmDataReceive");
            if(data != null) {
                if(!mFirstInitialFlag) {
                    mFirstInitialFlag = true;
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateCurrentAlarmList(data);
                    }
                });

                if (mFirstInitialFlag) {
                    mFirstInitialFlag = false;
                }
            }
            cancelProgressBar();
        }
        @Override
        public void onNotifyModeSettingReceive(byte data) {
            if (D) Log.d(TAG, "onNotifyModeSettingReceive");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mcbCallRemind.setChecked(SPWristbandConfigInfo.getNotifyCallFlag(WristbandSettingAlarmActivity.this));
                    mcbSmsRemind.setChecked(SPWristbandConfigInfo.getNotifyMessageFlag(WristbandSettingAlarmActivity.this));
                    mcbQQRemind.setChecked(SPWristbandConfigInfo.getNotifyQQFlag(WristbandSettingAlarmActivity.this));
                    mcbWechatRemind.setChecked(SPWristbandConfigInfo.getNotifyWechatFlag(WristbandSettingAlarmActivity.this));
                }
            });
            if (mFirstInitialFlag) {
                mFirstInitialFlag = false;
            }
            new Thread(new Runnable() {
                @Override
                public void run() {
                    if(!mWristbandManager.SetClocksSyncRequest()) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                showToast(R.string.something_error);
                            }
                        });
                        //mWristbandManager.close();
                    }
                    cancelProgressBar();
                }
            }).start();
        }
    };

    private void updateCurrentAlarmList(ApplicationLayerAlarmPacket data) {

        if(data.getId() == 0) {
            mDayFlagOne = data.getDayFlags();
            String hourOne = String.valueOf(data.getHour()).length() == 1
                    ? "0" + String.valueOf(data.getHour())
                    : String.valueOf(data.getHour());
            String minuteOne = String.valueOf(data.getMinute()).length() == 1
                    ? "0" + String.valueOf(data.getMinute())
                    : String.valueOf(data.getMinute());
            mtvClockTimeOne.setText(hourOne + ":" + minuteOne);
            mcbClockOne.setChecked(true);
            mtvClockDayFlagOne.setText(getDayFlagString(mDayFlagOne));
        } else if(data.getId() == 1) {
            mDayFlagTwo = data.getDayFlags();
            String hourTwo = String.valueOf(data.getHour()).length() == 1
                    ? "0" + String.valueOf(data.getHour())
                    : String.valueOf(data.getHour());
            String minuteTwo = String.valueOf(data.getMinute()).length() == 1
                    ? "0" + String.valueOf(data.getMinute())
                    : String.valueOf(data.getMinute());
            mtvClockTimeTwo.setText(hourTwo + ":" + minuteTwo);
            mcbClockTwo.setChecked(true);
            mtvClockDayFlagTwo.setText(getDayFlagString(mDayFlagTwo));
        } else if(data.getId() == 2) {
            mDayFlagThree = data.getDayFlags();
            String hourThree = String.valueOf(data.getHour()).length() == 1
                    ? "0" + String.valueOf(data.getHour())
                    : String.valueOf(data.getHour());
            String minuteThree = String.valueOf(data.getMinute()).length() == 1
                    ? "0" + String.valueOf(data.getMinute())
                    : String.valueOf(data.getMinute());
            mtvClockTimeThree.setText(hourThree + ":" + minuteThree);
            mcbClockThree.setChecked(true);
            mtvClockDayFlagThree.setText(getDayFlagString(mDayFlagThree));
        } else {
            if(D) Log.e(TAG, "updateCurrentAlarmList error, with: " + data.toString());
        }
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
        if(isNotifyManageEnabled()) {
            if(isQQEnable) {
                isQQEnable = false;
                mcbQQRemind.setChecked(true);
            } else if(isWechatEnable) {
                isWechatEnable = false;
                mcbWechatRemind.setChecked(true);
            }
        }

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
    }

    @Override
    public void onAlarmInfoSaved(final int position, int hour, int minute, final byte dayFlag) {
        if(D) Log.d(TAG, "onAlarmInfoSaved, position: " + position
                + ", hour: " + hour
                + ", minute: " + minute
                + ", dayFlag: " + dayFlag);

        String hourStr = String.valueOf(hour).length() == 1
                ? "0" + String.valueOf(hour)
                : String.valueOf(hour);
        String minuteStr = String.valueOf(minute).length() == 1
                ? "0" + String.valueOf(minute)
                : String.valueOf(minute);
        final String timeStr = hourStr + ":" + minuteStr;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(position == 0) {
                    mtvClockTimeOne.setText(timeStr);

                    mDayFlagOne = dayFlag;
                    mtvClockDayFlagOne.setText(getDayFlagString(mDayFlagOne));
                    if(mcbClockOne.isChecked()) {
                        saveAlarm();
                    } else {
                        mcbClockOne.setChecked(true);
                    }
                } else if(position == 1) {
                    mtvClockTimeTwo.setText(timeStr);

                    mDayFlagTwo = dayFlag;
                    mtvClockDayFlagTwo.setText(getDayFlagString(mDayFlagTwo));
                    if(mcbClockTwo.isChecked()) {
                        saveAlarm();
                    } else {
                        mcbClockTwo.setChecked(true);
                    }
                } else if(position == 2) {
                    mtvClockTimeThree.setText(timeStr);

                    mDayFlagThree = dayFlag;
                    mtvClockDayFlagThree.setText(getDayFlagString(mDayFlagThree));
                    if(mcbClockThree.isChecked()) {
                        saveAlarm();
                    } else {
                        mcbClockThree.setChecked(true);
                    }
                }
            }
        });

    }


    @Override
    public void onBackPressed() {
        mivAlarmBack.callOnClick();
        /*
        if(!isChangeAlarmSetting) {
            finish();
            return;
        }
        showProgressBar(R.string.settings_mydevice_setting_alarm);
        new Thread(new Runnable() {
            @Override
            public void run() {
                if(syncAlarmListToRemote()) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showToast(R.string.settings_mydevice_setting_alarm_success);
                        }
                    });
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showToast(R.string.settings_mydevice_setting_alarm_failed);
                        }
                    });
                }
                cancelProgressBar();
                finish();
            }
        }).start();
        */
    }
}
