
package com.realsil.android.wristbanddemo.notifybroadcast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import android.app.Notification;
import android.app.Notification.Action;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;

public class NotificationReceive extends NotificationListenerService {
    // Log
    private final static String TAG = "NotificationReceive";
    private final static boolean D = true;

    public static final String BROADCAST_TYPE = "com.realsil.android.wristbanddemo.notifybroadcast.BROADCAST_TYPE";
    public static final String EXTRA_TYPE = "com.realsil.android.wristbanddemo.notifybroadcast.EXTRA_TYPE";


    private static final String PAKAGE_QQ = "com.tencent.mobileqq";
    private static final String TITLE_QQ = "QQ";
    private static final String PAKAGE_MM = "com.tencent.mm";

    public static final String ACTION_NLS_CONTROL = "com.seven.notificationlistenerdemo.NLSCONTROL";
    public static List<StatusBarNotification[]> mCurrentNotifications = new ArrayList<StatusBarNotification[]>();
    public static int mCurrentNotificationsCounts = 0;
    public static StatusBarNotification mPostedNotification;
    public static StatusBarNotification mRemovedNotification;

    @Override
    public void onCreate() {
        super.onCreate();
        if(D) Log.d(TAG, "onCreate");
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_NLS_CONTROL);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        if(D) Log.d(TAG, "onBind");
        return super.onBind(intent);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        String packageName = sbn.getPackageName();
        String title = sbn.getNotification().extras.getString(Notification.EXTRA_TITLE);
        /*
        if(D) Log.d(TAG, "add, sbn.getPackageName(): " + sbn.getPackageName()
                + ", title: " + sbn.getNotification().extras.getString(Notification.EXTRA_TITLE));
                */
        if(packageName.equals(PAKAGE_MM)) {
            sendNotifyBroadcast(NotifyBroadcastReceive.BROADCAST_WECHAT);
        } else if(packageName.equals(PAKAGE_QQ)) {
            sendNotifyBroadcast(NotifyBroadcastReceive.BROADCAST_QQ);
        }
    }

    private void sendNotifyBroadcast(int type) {
        if(D) Log.d(TAG, "sendNotifyBroadcast, type: " + type);
        final Intent broadcast = new Intent();
        broadcast.setAction(BROADCAST_TYPE);
        broadcast.putExtra(EXTRA_TYPE, type);
        sendBroadcast(broadcast);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        /*
        if(D) Log.d(TAG, "removed, sbn.getPackageName(): " + sbn.getPackageName()
                + "" + sbn.getNotification().extras.getString(Notification.EXTRA_TITLE));
                */
    }

}
