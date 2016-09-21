package com.realsil.android.wristbanddemo.utility;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.util.Log;

import java.util.List;

/**
 * Created by Administrator on 2016/5/24.
 */
public class JudgeActivityFront {
    // Log
    private final static String TAG = "JudgeActivityFront";
    private final static boolean D = true;
    /**
     * 判断当前应用程序处于前台还是后台
     * 后台返回为true，前台为false
     */
    public static boolean isApplicationBroughtToBackground(final Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

        List<ActivityManager.RunningTaskInfo> tasks = am.getRunningTasks(1);
        if (!tasks.isEmpty()) {
            ComponentName topActivity = tasks.get(0).topActivity;
            if(D) Log.d(TAG, "topActivity.getPackageName(): " + topActivity.getPackageName()
                    + ", topActivity.getClassName(): " + topActivity.getClassName()
                    + ", context.getPackageName(): " + context.getPackageName());
            if (!topActivity.getPackageName().equals(context.getPackageName())) {
                return true;
            }
        } else {
            return true;
        }
        return false;
    }


    public static boolean isAppOnForeground(final Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        /*
        List<ActivityManager.RunningTaskInfo> tasks = am.getRunningTasks(1);
        if (!tasks.isEmpty()) {
            ComponentName topActivity = tasks.get(0).topActivity;
            if(D) Log.d(TAG, "topActivity.getPackageName(): " + topActivity.getPackageName()
                    + ", topActivity.getClassName(): " + topActivity.getClassName()
                    + ", context.getPackageName(): " + context.getPackageName());
            if (!topActivity.getPackageName().equals(context.getPackageName())) {
                return true;
            }
        }*/
        List<ActivityManager.RunningAppProcessInfo>appProcesses = am.getRunningAppProcesses();
        if (appProcesses == null) {
            if(D) Log.d(TAG, "appProcesses == null");
            return false;
        }
        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            if(D) Log.d(TAG, "appProcess.processName = " + appProcess.processName
                    + ", context.getPackageName(): " + context.getPackageName()
                    + ", appProcess.importance: " + appProcess.importance);
            if (appProcess.processName.equals(context.getPackageName())
                    && appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                return true;
            }
        }
        return false;
    }
}
