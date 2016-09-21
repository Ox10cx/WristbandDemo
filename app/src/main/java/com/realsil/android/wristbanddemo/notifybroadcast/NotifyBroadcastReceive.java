package com.realsil.android.wristbanddemo.notifybroadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.realsil.android.wristbanddemo.backgroundscan.BackgroundScanAutoConnected;
import com.realsil.android.wristbanddemo.utility.WristbandManager;

public class NotifyBroadcastReceive extends BroadcastReceiver {
    // Log
    private final static String TAG = "NotifyBroadcastReceive";
    private final static boolean D = true;

    public final static int BROADCAST_CALL_WAIT = 0;
    public final static int BROADCAST_CALL_ACC = 1;
    public final static int BROADCAST_CALL_REJ = 2;
    public final static int BROADCAST_SMS = 3;
    public final static int BROADCAST_QQ = 4;
    public final static int BROADCAST_WECHAT = 5;

    public final static String ACTION_BROADCAST_CALL = "android.intent.action.PHONE_STATE";
    public final static String ACTION_BROADCAST_SMS = "android.provider.Telephony.SMS_RECEIVED";
    public final static String ACTION_BROADCAST_QQ_AND_WECHAT = NotificationReceive.BROADCAST_TYPE;
    //public final static String ACTION_BROADCAST_WECHAT = "android.intent.action.PHONE_STATE";

    OnBroadcastListener mCallback;
    public NotifyBroadcastReceive(OnBroadcastListener callback) {
        if(D) Log.d(TAG, "initial");
        mCallback = callback;
    }
    @Override
    public void onReceive(Context context, Intent intent) {
        if(D) Log.d(TAG, "onReceive, action: " + intent.getAction());
        // Need check current link again
        if(!(WristbandManager.getInstance().isConnect()
                && WristbandManager.getInstance().isReady()
                && !BackgroundScanAutoConnected.getInstance().isInLogin())) {
            if(D) Log.e(TAG, "Receive broadcast with state error, do nothing!");
            return;
        }
        if(intent.getAction().equals(ACTION_BROADCAST_CALL)){
            doReceivePhone(context, intent);
        } else if(intent.getAction().equals(ACTION_BROADCAST_SMS)) {
            if(mCallback != null) {
                mCallback.onBroadcastCome(BROADCAST_SMS);
            }
        } else if(intent.getAction().equals(ACTION_BROADCAST_QQ_AND_WECHAT)) {
            final int type = intent.getIntExtra(NotificationReceive.EXTRA_TYPE, 0);
            if(D) Log.i(TAG, "Receive NotificationReceive type : " + type);
            if(mCallback != null) {
                mCallback.onBroadcastCome(type);
            }
        }
    }
    public void close() {
        mCallback = null;
    }
    /**
     * operate the receive phone broadcast.
     * @param context
     * @param intent
     */
    public void doReceivePhone(Context context, Intent intent) {
        String phoneNumber = intent.getStringExtra(
                TelephonyManager.EXTRA_INCOMING_NUMBER);
        TelephonyManager telephony =
                (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
        int state = telephony.getCallState();
        switch(state){
            case TelephonyManager.CALL_STATE_RINGING:
                if(D) Log.i(TAG, "[Listener]wait:"+phoneNumber);
                if(mCallback != null) {
                    mCallback.onBroadcastCome(BROADCAST_CALL_WAIT);
                }
                break;
            case TelephonyManager.CALL_STATE_IDLE:
                if(D) Log.i(TAG, "[Listener]close:"+phoneNumber);
                if(mCallback != null) {
                    mCallback.onBroadcastCome(BROADCAST_CALL_REJ);
                }
                break;
            case TelephonyManager.CALL_STATE_OFFHOOK:
                if(D) Log.i(TAG, "[Broadcast]in call:"+phoneNumber);
                if(mCallback != null) {
                    mCallback.onBroadcastCome(BROADCAST_CALL_ACC);
                }
                break;
        }
    }

    /**
     * 电话状态监听.
     * @author stephen
     *
     */
    class OnePhoneStateListener extends PhoneStateListener{
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            Log.i(TAG, "[Listener] phone number: "+incomingNumber);
            switch(state){
                case TelephonyManager.CALL_STATE_RINGING:
                    Log.i(TAG, "[Listener]wait:"+incomingNumber);
                    //mCallback.onBroadcastCome(BROADCAST_CALL);
                    break;
                case TelephonyManager.CALL_STATE_IDLE:
                    Log.i(TAG, "[Listener]close:"+incomingNumber);
                    break;
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    Log.i(TAG, "[Listener]in call:"+incomingNumber);
                    break;
            }
            super.onCallStateChanged(state, incomingNumber);
        }
    }
    /**
     * Interface required to be implemented by activity
     */
    public static interface OnBroadcastListener {
        /**
         * Fired when Call/Message/QQ/Wechat notify come
         *
         * @param type      notify type
         */
        public void onBroadcastCome(int type);
    }
}
