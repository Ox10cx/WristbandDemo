package com.realsil.android.wristbanddemo.sport;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.realsil.android.wristbanddemo.R;
import com.realsil.android.wristbanddemo.WristbandDetailActivity;
import com.realsil.android.wristbanddemo.backgroundscan.BackgroundScanAutoConnected;
import com.realsil.android.wristbanddemo.greendao.SportData;
import com.realsil.android.wristbanddemo.utility.CircleProcessBar;
import com.realsil.android.wristbanddemo.utility.GlobalGreenDAO;
import com.realsil.android.wristbanddemo.utility.HighLightView;
import com.realsil.android.wristbanddemo.utility.RefreshableLinearLayoutView;
import com.realsil.android.wristbanddemo.utility.SPWristbandConfigInfo;
import com.realsil.android.wristbanddemo.utility.WristbandCalculator;
import com.realsil.android.wristbanddemo.utility.WristbandManager;
import com.realsil.android.wristbanddemo.utility.WristbandManagerCallback;

import java.util.Calendar;
import java.util.List;

public class WristbandHomeFragmentSport extends Fragment {
    // Log
    private final static String TAG = "WristbandHomeFragmentSport";
    private final static boolean D = true;

    ImageView mivStepCircle;
    TextView mtvHomeCurrentStep;
    TextView mtvHomeGoal;

    TextView mtvHomeCurrentDistance;
    TextView mtvHomeCurrentCalorie;
    TextView mtvHomeCurrentQuality;

    CircleProcessBar mcpbStep;

    private GlobalGreenDAO mGlobalGreenDAO;

    private WristbandManager mWristbandManager;

    private HighLightView mHighLightView;

    private final double STEP_QUALITY_0 = 60;
    private final double STEP_QUALITY_1 = 70;
    private final double STEP_QUALITY_2 = 80;
    private final double STEP_QUALITY_3 = 100;

    private boolean isFirstInitial;

    RefreshableLinearLayoutView refreshableView;

    private ProgressRunable mProgressRunable;

    private Toast mToast;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_step, container, false);

        // get global green dao instance
        mGlobalGreenDAO = GlobalGreenDAO.getInstance();

        mWristbandManager = WristbandManager.getInstance();
        mWristbandManager.registerCallback(mWristbandManagerCallback);

        mHighLightView = new HighLightView(getActivity());

        mivStepCircle = (ImageView)rootView.findViewById(R.id.ivStepCircle);
        mivStepCircle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), WristbandDetailActivity.class);
                intent.putExtra(WristbandDetailActivity.EXTRAS_DETAIL_MODE, WristbandDetailActivity.DETAIL_MODE_SPORT_DAY);
                getActivity().startActivity(intent);
            }
        });

        mtvHomeCurrentStep = (TextView) rootView.findViewById(R.id.tvHomeCurrentStep);
        mtvHomeGoal = (TextView) rootView.findViewById(R.id.tvHomeGoal);

        mtvHomeCurrentDistance = (TextView) rootView.findViewById(R.id.tvHomeCurrentDistance);
        mtvHomeCurrentCalorie = (TextView) rootView.findViewById(R.id.tvHomeCurrentCalorie);
        mtvHomeCurrentQuality = (TextView) rootView.findViewById(R.id.tvHomeCurrentQuality);

        refreshableView = (RefreshableLinearLayoutView) rootView.findViewById(R.id.refreshable_view);
        refreshableView.setOnRefreshListener(new RefreshableLinearLayoutView.PullToRefreshListener() {
            @Override
            public void onRefresh() {
                syncData();
                //refreshableView.finishRefreshing();
            }
            @Override
            public void onMove() {
                initialProgressUi();
                if(mProgressRunable != null
                    && !mProgressRunable._run) {
                    mcpbStep.setProgress(mProgressRunable.totalProgress);
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

        isFirstInitial = true;

        mcpbStep = (CircleProcessBar) rootView.findViewById(R.id.cpbStep);
        mcpbStep.post(new Runnable() {
            @Override
            public void run() {
                initialProgressUi();
                initialUI();
                isFirstInitial = false;
            }
        });

        initialStringFormat();

        // Show Guide window
        showGuideSpec();

        registerSyncDataBroadcast();
        return rootView;
    }
    //private String mSportCurrentStepFormat;
    private String mFormatSportGoal;
    private String mFormatSportCurrentDistance;
    private String mFormatSportCurrentCalorie;
    //private String mSportCurrentQuality;

    private void initialStringFormat() {
        mFormatSportGoal = getResources().getString(R.string.step_goal);
        mFormatSportCurrentDistance = getResources().getString(R.string.distance_value);
        mFormatSportCurrentCalorie = getResources().getString(R.string.calorie_value);
    }

    private void initialUI() {
        stopProgressRunable();
        int total = SPWristbandConfigInfo.getTotalStep(getContext());
        mtvHomeGoal.setText(String.format(mFormatSportGoal, total));

        Calendar c1  = Calendar.getInstance();
        List<SportData> sports = mGlobalGreenDAO.loadSportDataByDate(c1.get(Calendar.YEAR),
                c1.get(Calendar.MONTH) + 1,// here need add 1, because it origin range is 0 - 11;
                c1.get(Calendar.DATE));

        List<SportData> sports1 = mGlobalGreenDAO.loadAllSportData();
        for(SportData sp: sports1) {
            if (D) Log.d(TAG, WristbandCalculator.toString(sp));
        }
        SportSubData subData = WristbandCalculator.sumOfSportDataByDate(c1.get(Calendar.YEAR),
                c1.get(Calendar.MONTH) + 1,// here need add 1, because it origin range is 0 - 11;
                c1.get(Calendar.DATE),
                sports);
        if(subData != null) {
            mtvHomeCurrentStep.setText(String.valueOf(subData.getStepCount()));

            mtvHomeCurrentDistance.setText(String.format(mFormatSportCurrentDistance, (float) subData.getDistance() / 1000));
            mtvHomeCurrentCalorie.setText(String.format(mFormatSportCurrentCalorie, (float) subData.getCalory() / 1000));

            float persent = (float) (subData.getStepCount() * 100) / total;
            if(persent > 100) {
                persent = 100;
            }
            if (persent < STEP_QUALITY_0) {
                mtvHomeCurrentQuality.setText(getResources().getString(R.string.step_quality_0));
            } else if (persent < STEP_QUALITY_1) {
                mtvHomeCurrentQuality.setText(getResources().getString(R.string.step_quality_1));
            } else if (persent < STEP_QUALITY_2) {
                mtvHomeCurrentQuality.setText(getResources().getString(R.string.step_quality_2));
            } else {
                mtvHomeCurrentQuality.setText(getResources().getString(R.string.step_quality_3));
            }
            startProgressRunable((int)persent);
        } else {
            mtvHomeCurrentStep.setText(String.valueOf(0));

            mtvHomeCurrentDistance.setText(String.format(mFormatSportCurrentDistance, 0.0));
            mtvHomeCurrentCalorie.setText(String.format(mFormatSportCurrentCalorie, 0.0));

            mtvHomeCurrentQuality.setText(getResources().getString(R.string.step_quality_0));

            startProgressRunable(0);
        }
        //new ProgressRunable(57).start();
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
            WristbandHomeFragmentSport.this.getActivity().runOnUiThread(new Runnable() {
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
                //请求运动数据
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

                    WristbandHomeFragmentSport.this.getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            initialUI();
                        }
                    });
                    cancelSync();
                    isInSync = false;
                    return;
                }
                WristbandHomeFragmentSport.this.getActivity().runOnUiThread(new Runnable() {
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
    private void initialProgressUi() {
        if(getActivity() == null) {
            return;
        }
        //if(D) Log.d(TAG, "initialProgressUi");
        float radius;// Circular radius
        if(mivStepCircle.getWidth() > mivStepCircle.getHeight()) {
            radius = (float)mivStepCircle.getHeight() / 2;
        } else {
            radius = (float)mivStepCircle.getWidth() / 2;
        }
        Bitmap bitmap = ((BitmapDrawable) mivStepCircle.getDrawable().getCurrent()).getBitmap();//Get Circular Image

        float proportion = ((float) bitmap.getWidth()) / (radius * 2);

        float paintRadius = (float)((bitmap.getWidth() * 0.05) / proportion);

        float insideCircleRadius = (float)((bitmap.getWidth() * (1 - 0.05*2))/2 / proportion);
        mcpbStep.setRingColor(getResources().getColor(R.color.step_circle_color));
        mcpbStep.setRadius(insideCircleRadius);
        mcpbStep.setStrokeWidth(paintRadius);
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
                mcpbStep.setProgress(currentProgress);
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
            _run = false;
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

    private void showGuideSpec() {
        String s = getResources().getString(R.string.guide_home_operate);
        if(!isFirstLoad()) {
            return;
        }
        final int defaultOrientation = getActivity().getRequestedOrientation();
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
        mHighLightView.showTipForView(mivStepCircle, s, HighLightView.HIGH_LIGHT_VIEW_TYPE_RECT, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCallback != null) {
                    mCallback.onGuideClicked();
                    getActivity().setRequestedOrientation(defaultOrientation);
                }
            }
        });
    }

    private boolean isFirstLoad() {
        return SPWristbandConfigInfo.getFirstAppStartFlag(getActivity());
    }
    OnGuideClickListener mCallback;
    public void registerGuideCallback(OnGuideClickListener callback) {
        mCallback = callback;
    }
    public static interface OnGuideClickListener {
        /**
         * Fired when user click the guide area
         *
         */
        public void onGuideClicked();
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

                    WristbandHomeFragmentSport.this.getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            initialUI();
                        }
                    });
                    cancelSync();
                    isInSync = false;
                    return;
                }
                WristbandHomeFragmentSport.this.getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        initialUI();
                    }
                });
            }
        }
    }
}
