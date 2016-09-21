package com.realsil.android.wristbanddemo.sleep;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.realsil.android.wristbanddemo.R;
import com.realsil.android.wristbanddemo.WristbandDetailActivity;
import com.realsil.android.wristbanddemo.backgroundscan.BackgroundScanAutoConnected;
import com.realsil.android.wristbanddemo.greendao.SleepData;
import com.realsil.android.wristbanddemo.greendao.SportData;
import com.realsil.android.wristbanddemo.sport.SportSubData;
import com.realsil.android.wristbanddemo.utility.CircleProcessBar;
import com.realsil.android.wristbanddemo.utility.GlobalGreenDAO;
import com.realsil.android.wristbanddemo.utility.RefreshableLinearLayoutView;
import com.realsil.android.wristbanddemo.utility.SPWristbandConfigInfo;
import com.realsil.android.wristbanddemo.utility.WristbandCalculator;
import com.realsil.android.wristbanddemo.utility.WristbandManager;
import com.realsil.android.wristbanddemo.utility.WristbandManagerCallback;

import java.util.Calendar;
import java.util.List;

public class WristbandHomeFragmentSleep extends Fragment {
    // Log
    private final static String TAG = "WristbandHomeFragmentSleep";
    private final static boolean D = true;

    ImageView mivSleepCircle;
    TextView mtvHomeCurrentSleep;

    TextView mtvHomeCurrentDeep;
    TextView mtvHomeCurrentLight;
    TextView mtvHomeCurrentAwakeTimes;

    CircleProcessBar mcpbSleep;

    private GlobalGreenDAO mGlobalGreenDAO;

    private WristbandManager mWristbandManager;

    private boolean isFirstInitial;

    RefreshableLinearLayoutView refreshableView;
    private ProgressRunable mProgressRunable;

    private Toast mToast;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_sleep, container, false);

        // get global green dao instance
        mGlobalGreenDAO = GlobalGreenDAO.getInstance();

        mWristbandManager = WristbandManager.getInstance();
        mWristbandManager.registerCallback(mWristbandManagerCallback);

        mivSleepCircle = (ImageView)rootView.findViewById(R.id.ivSleepCircle);
        mivSleepCircle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), WristbandDetailActivity.class);
                intent.putExtra(WristbandDetailActivity.EXTRAS_DETAIL_MODE, WristbandDetailActivity.DETAIL_MODE_SLEEP_DAY);
                getActivity().startActivity(intent);
            }
        });

        mtvHomeCurrentSleep = (TextView) rootView.findViewById(R.id.tvHomeCurrentSleep);

        mtvHomeCurrentDeep = (TextView) rootView.findViewById(R.id.tvHomeCurrentDeep);
        mtvHomeCurrentLight = (TextView) rootView.findViewById(R.id.tvHomeCurrentLight);
        mtvHomeCurrentAwakeTimes = (TextView) rootView.findViewById(R.id.tvHomeCurrentAwakeTimes);

        refreshableView = (RefreshableLinearLayoutView) rootView.findViewById(R.id.refreshable_view);
        refreshableView.setOnRefreshListener(new RefreshableLinearLayoutView.PullToRefreshListener() {
            @Override
            public void onRefresh() {
                syncData();
            }
            @Override
            public void onMove() {
                initialProgressUi();
                if(mProgressRunable != null
                        && !mProgressRunable._run) {
                    mcpbSleep.setProgress(mProgressRunable.totalProgress);
                }

                //syncData();
                //refreshableView.finishRefreshing();
            }
            @Override
            public void onNotRefresh() {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        initialUI();
                    }
                });
            }
        }, 2);

        initialStringFormat();

        isFirstInitial = true;

        mcpbSleep = (CircleProcessBar) rootView.findViewById(R.id.cpbSleep);
        mcpbSleep.post(new Runnable() {
            @Override
            public void run() {
                initialProgressUi();
                initialUI();
                isFirstInitial = false;
            }
        });

        registerSyncDataBroadcast();
        return rootView;
    }
    //private String mSportCurrentStepFormat;
    private String mFormatDeep;
    private String mFormatLight;
    private String mFormatAwake;
    //private String mFormatSportCurrentCalorie;
    //private String mSportCurrentQuality;

    private void initialStringFormat() {
        mFormatDeep = getResources().getString(R.string.hour_min);
        mFormatLight = getResources().getString(R.string.hour_min);
        mFormatAwake = getResources().getString(R.string.times_value);
        //mFormatSportCurrentCalorie = getResources().getString(R.string.calorie_value);
    }

    private void initialUI() {
        stopProgressRunable();
        Calendar c1  = Calendar.getInstance();
        List<SleepData> sleeps = mGlobalGreenDAO.loadSleepDataByDateSpec(c1.get(Calendar.YEAR),
                c1.get(Calendar.MONTH) + 1,// here need add 1, because it origin range is 0 - 11;
                c1.get(Calendar.DATE));

        SleepSubData subData = WristbandCalculator.sumOfSleepDataByDateSpecNoErrorCheck(c1.get(Calendar.YEAR),
                c1.get(Calendar.MONTH) + 1,// here need add 1, because it origin range is 0 - 11;
                c1.get(Calendar.DATE),
                sleeps);
        if(subData != null) {
            float persent = (float) (subData.getTotalSleepTime() * 100)
                    / (getTotalSleep() * 60);
            if(persent > 100) {
                persent = 100;
            }
            mtvHomeCurrentSleep.setText(String.valueOf(subData.getTotalSleepTime() / 60));

            mtvHomeCurrentDeep.setText(String.format(mFormatDeep
                    , subData.getDeepSleepTime() / 60, subData.getDeepSleepTime() % 60));
            mtvHomeCurrentLight.setText(String.format(mFormatLight
                    , subData.getLightSleepTime() / 60, subData.getLightSleepTime() % 60));
            mtvHomeCurrentAwakeTimes.setText(String.format(mFormatAwake
                    , subData.getAwakeTimes()));

            startProgressRunable((int) persent);
        } else {
            mtvHomeCurrentSleep.setText(String.valueOf(0));

            mtvHomeCurrentDeep.setText(String.format(mFormatDeep
                    , 0, 0));
            mtvHomeCurrentLight.setText(String.format(mFormatLight
                    , 0, 0));
            mtvHomeCurrentAwakeTimes.setText(String.format(mFormatAwake
                    , 0));

            startProgressRunable(0);
        }

    }

    private int getTotalSleep() {
        int age = SPWristbandConfigInfo.getAge(getContext());
        int totalSleep;
        if(age < 10) {
            totalSleep = 13;
        } else if(age < 18) {
            totalSleep = 12;
        } else if(age < 59) {
            totalSleep = 10;
        } else {
            totalSleep = 8;
        }
        if(D) Log.d(TAG, "getTotalSleep, age: " + age + ", totalSleep: " + totalSleep);
        return totalSleep;
    }
    private boolean isInSync = false;
    private void syncData() {
        isInSync = true;
        if(mWristbandManager.isConnect()) {
            stopProgressRunable();
            if(!mWristbandManager.isCallbackRegisted(mWristbandManagerCallback)) {
                mWristbandManager.registerCallback(mWristbandManagerCallback);
            }
            startSync();
        } else {
            cancelSync();
            BackgroundScanAutoConnected.getInstance().startAutoConnect();
            WristbandHomeFragmentSleep.this.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showToast(R.string.please_connect_band);
                }
            });
        }
    }
    private void sleep(int time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void startSync() {
        mSyncSuperHandler.postDelayed(mSyncSuperTask, 30 * 1000);
        new Thread(new Runnable() {
            @Override
            public void run() {
                mWristbandManager.SendDataRequest();
            }
        }).start();
    }

    private void cancelSync() {
        refreshableView.finishRefreshing();
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {

                initialUI();
            }
        });
        mSyncSuperHandler.removeCallbacks(mSyncSuperTask);
    }

    // Alarm timer
    Handler mSyncSuperHandler = new Handler();
    Runnable mSyncSuperTask = new Runnable(){
        @Override
        public void run() {
            // TODO Auto-generated method stub
            if(D) Log.w(TAG, "Wait Progress Timeout");
            showToast(R.string.syncing_data_fail);
            // stop timer
            cancelSync();
        }
    };

    // Application Layer callback
    WristbandManagerCallback mWristbandManagerCallback = new WristbandManagerCallback() {
        @Override
        public void onConnectionStateChange(final boolean status) {
            if(D) Log.d(TAG, "onConnectionStateChange, status: " + status);
            // if already connect to the remote device, we can do more things here.
            if(status) {
                //
            } else {
                cancelSync();
            }
        }
/*
        @Override
        public void onLoginStateChange(final int state) {
            if(D) Log.d(TAG, "onLoginStateChange, state: " + state);
            if(state == WristbandManager.STATE_WRIST_SYNC_DATA) {
                if(D) Log.d(TAG, "data sync ok");
                if(isInSync) {
                    showToast(R.string.sync_data_success);

                    WristbandHomeFragmentSleep.this.getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            initialUI();
                        }
                    });

                    cancelSync();
                    isInSync = false;
                    return;
                }


                WristbandHomeFragmentSleep.this.getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        initialUI();
                    }
                });
            }
        }
*/
        @Override
        public void onError(final int error) {
            if(D) Log.d(TAG, "onError, error: " + error);
            if(isInSync) {
                showToast(R.string.syncing_data_fail);
                isInSync = false;
            }
            cancelSync();
        }
    };
    private void showToast(final String message) {
        if(mToast == null) {
            mToast = Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT);
        } else {
            mToast.setText(message);
        }
        mToast.show();
    }
    private void showToast(final int message) {
        if(mToast == null) {
            mToast = Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT);
        } else {
            mToast.setText(message);
        }
        mToast.show();
    }

    private void initialProgressUi() {
        if(getActivity() == null) {
            return;
        }
        //if(D) Log.d(TAG, "initialProgressUi");
        float radius;// Circular radius
        if(mivSleepCircle.getWidth() > mivSleepCircle.getHeight()) {
            radius = (float)mivSleepCircle.getHeight() / 2;
        } else {
            radius = (float)mivSleepCircle.getWidth() / 2;
        }
        Bitmap bitmap = ((BitmapDrawable) mivSleepCircle.getDrawable().getCurrent()).getBitmap();//Get Circular Image

        float proportion = ((float) bitmap.getWidth()) / (radius * 2);

        float paintRadius = (float)((bitmap.getWidth() * 0.05) / proportion);

        float insideCircleRadius = (float)((bitmap.getWidth() * (1 - 0.05*2))/2 / proportion);
        mcpbSleep.setRingColor(getResources().getColor(R.color.sleep_circle_color));
        mcpbSleep.setRadius(insideCircleRadius);
        mcpbSleep.setStrokeWidth(paintRadius);
    }
    private void startProgressRunable(int persent) {
        if(D) Log.d(TAG, "startProgressRunable, persent: " + persent);
        mProgressRunable = new ProgressRunable(persent);
        mProgressRunable.start();
    }
    private void stopProgressRunable() {
        if(D) Log.d(TAG, "stopProgressRunable");
        if(mProgressRunable != null) {
            mProgressRunable.stopThread();
            mProgressRunable.interrupt();
            startProgressRunable(0);
        }
    }
    class ProgressRunable extends Thread {
        private int totalProgress;
        private int currentProgress;
        ProgressRunable(int progress) {
            if(progress < 100) {
                totalProgress = progress;
            } else {
                totalProgress = 100;
            }
        }

        private boolean  _run  = true;
        public void stopThread() {
            this ._run = false;
        }

        @Override
        public void run() {
            currentProgress = 0;
            while (currentProgress <= totalProgress) {
                initialProgressUi();
                mcpbSleep.setProgress(currentProgress);
                currentProgress += 1;
                try {
                    Thread.sleep(50);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if(!this._run) {
                    if(D) Log.w(TAG, "ProgressRunable interrupted.");
                    break;
                }
            }
        }

    }
    @Override
    public void onDestroy() {
        super.onDestroy();

        unregisterSyncDataBroadcast();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        if(!isFirstInitial) {
            initialUI();
        }
        // make sure callback registed
        mWristbandManager.registerCallback(mWristbandManagerCallback);
        super.onResume();
    }
    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            //相当于Fragment的onResume
            //initialProgressUi();

            //initialUI();
        } else {
            //相当于Fragment的onPause
        }
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if(D) Log.d(TAG, "onConfigurationChanged");
        initialUI();
    }

    private SyncDataReceiver mSyncDataReceiver;
    public void registerSyncDataBroadcast() {
        if(D) Log.d(TAG, "registerSyncDataBroadcast");
        mSyncDataReceiver = new SyncDataReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(WristbandManager.ACTION_SYNC_DATA_OK);
        getActivity().registerReceiver(mSyncDataReceiver, filter);
    }

    public void unregisterSyncDataBroadcast() {
        if(D) Log.d(TAG, "unregisteSyncDataBroadcast");
        if(mSyncDataReceiver != null) {
            getActivity().unregisterReceiver(mSyncDataReceiver);
        }
    }
    // Broadcast to receive BT on/off broadcast
    public class SyncDataReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO Auto-generated method stub
            String action = intent.getAction();
            if (WristbandManager.ACTION_SYNC_DATA_OK.equals(action)) {
                if(D) Log.d(TAG, "data sync ok");
                if(isInSync) {
                    showToast(R.string.sync_data_success);

                    WristbandHomeFragmentSleep.this.getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            initialUI();
                        }
                    });
                    cancelSync();
                    isInSync = false;
                    return;
                }
                WristbandHomeFragmentSleep.this.getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        initialUI();
                    }
                });
            }
        }
    }
}
