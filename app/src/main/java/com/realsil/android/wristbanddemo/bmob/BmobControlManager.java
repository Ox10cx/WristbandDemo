package com.realsil.android.wristbanddemo.bmob;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.text.TextUtils;
import android.util.Log;

import com.realsil.android.wristbanddemo.BuildConfig;
import com.realsil.android.wristbanddemo.bmob.bean.MyUser;
import com.realsil.android.wristbanddemo.bmob.bean.OTA;
import com.realsil.android.wristbanddemo.constant.ConstantParam;
import com.realsil.android.wristbanddemo.utility.ImageLoadingUtils;
import com.realsil.android.wristbanddemo.utility.MyDateUtils;
import com.realsil.android.wristbanddemo.utility.SPWristbandConfigInfo;

import org.json.JSONObject;

import java.io.File;
import java.util.List;

import cn.bmob.v3.AsyncCustomEndpoints;
import cn.bmob.v3.BmobQuery;
import cn.bmob.v3.BmobUser;
import cn.bmob.v3.datatype.BmobFile;
import cn.bmob.v3.exception.BmobException;
import cn.bmob.v3.listener.CloudCodeListener;
import cn.bmob.v3.listener.FindListener;
import cn.bmob.v3.listener.UpdateListener;
import cn.bmob.v3.listener.UploadFileListener;
import rx.Subscriber;
import rx.Subscription;
import rx.subscriptions.CompositeSubscription;

/**
 * Created by rain1_wen on 2016/8/23.
 */
public class BmobControlManager {
    // Log
    private final static String TAG = "BmobControlManager";
    private final static boolean D = true;

    private static BmobControlManager mInstance;
    private static Context mContext;

    public static void initial(Context context) {
        if (D) Log.d(TAG, "initial()");
        mInstance = new BmobControlManager();
        mContext = context;
    }

    public static BmobControlManager getInstance() {
        return mInstance;
    }

    /**
     * 检测网络是否可用
     * @return
     */
    public boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        boolean isConnect = ni != null && ni.isConnectedOrConnecting();
        if(D) Log.d(TAG, "isNetworkConnected, isConnect: " + isConnect);
        return isConnect;
    }

    /**
     * 获取当前网络类型
     * @return 0：没有网络   1：WIFI网络   2：WAP网络    3：NET网络
     */

    public static final int NETTYPE_WIFI = 0x01;
    public static final int NETTYPE_CMWAP = 0x02;
    public static final int NETTYPE_CMNET = 0x03;
    public int getNetworkType() {
        int netType = 0;
        ConnectivityManager connectivityManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo == null) {
            return netType;
        }
        int nType = networkInfo.getType();
        if (nType == ConnectivityManager.TYPE_MOBILE) {
            String extraInfo = networkInfo.getExtraInfo();
            if(!TextUtils.isEmpty(extraInfo)){
                if (extraInfo.toLowerCase().equals("cmnet")) {
                    netType = NETTYPE_CMNET;
                } else {
                    netType = NETTYPE_CMWAP;
                }
            }
        } else if (nType == ConnectivityManager.TYPE_WIFI) {
            netType = NETTYPE_WIFI;
        }
        return netType;
    }

    public static boolean checkAPKWorkType() {
        if(BuildConfig.APP_WORK_TYPE == ConstantParam.WORK_TYPE_INTERNET) {
            if(D) Log.d(TAG, "checkAPKWorkType: true, BuildConfig.APP_WORK_TYPE: " + BuildConfig.APP_WORK_TYPE);
            return true;
        }
        if(D) Log.d(TAG, "checkAPKWorkType: true, BuildConfig.APP_WORK_TYPE: " + BuildConfig.APP_WORK_TYPE);
        return false;
    }

    public void logOff() {
        if(checkAPKWorkType() && isNetworkConnected()) {
            final MyUser bmobUser = BmobUser.getCurrentUser(MyUser.class);
            if (bmobUser != null) {
                BmobUser.logOut();
            }
        }
    }

    public boolean login(final String userName, final String psw, Subscriber<MyUser> listen) {
        //String userId = getWristbandIdFromString(userName);
        if(checkAPKWorkType() && isNetworkConnected()) {
            final MyUser bu2 = new MyUser();
            bu2.setUsername(userName);
            bu2.setPassword(psw);

            if(SPWristbandConfigInfo.getBmobLastSyncDate(mContext) == null) {
                String format = ConstantParam.DEFAULT_DETAIL_DATE_FORMAT;
                SPWristbandConfigInfo.setBmobLastSyncDate(mContext, MyDateUtils.getCurDate(format));
            }
            //新增加的Observable
            bu2.loginObservable(MyUser.class).subscribe(listen);
        } else {
            if(D) Log.w(TAG, "login Wrong, checkAPKWorkType(): " + checkAPKWorkType()
                    + ", isNetworkConnected(): " + isNetworkConnected());
            return false;
        }

        return true;
    }


    public boolean syncUserInfoWithImage(final UpdateListener listen) {
        if(checkAPKWorkType() && isNetworkConnected()) {
            final MyUser bmobUser = BmobUser.getCurrentUser(MyUser.class);
            if (bmobUser != null) {
                final MyUser myUser = new MyUser();
                myUser.setUsername(SPWristbandConfigInfo.getUserName(mContext));
                myUser.setPassword(SPWristbandConfigInfo.getUserPsw(mContext));

                myUser.setAge(SPWristbandConfigInfo.getAge(mContext));
                myUser.setGender(SPWristbandConfigInfo.getGendar(mContext));
                myUser.setHeight(SPWristbandConfigInfo.getHeight((mContext)));
                myUser.setWeight(SPWristbandConfigInfo.getWeight(mContext));
                myUser.setStepTarget(SPWristbandConfigInfo.getTotalStep(mContext));
                BmobFile bmobFile;
                if(ImageLoadingUtils.checkIsTheHttpString(SPWristbandConfigInfo.getAvatarPath(mContext))) {
                    bmobFile = new BmobFile(new File(
                            ImageLoadingUtils.getUniqueImagePath(SPWristbandConfigInfo.getAvatarPath(mContext))));
                } else {
                    bmobFile = new BmobFile(new File(
                            SPWristbandConfigInfo.getAvatarPath(mContext)));
                }

                myUser.setImage(bmobFile);
                myUser.setNickName(SPWristbandConfigInfo.getName(mContext));

                bmobFile.uploadblock(new UploadFileListener() {

                    @Override
                    public void done(BmobException e) {
                        if (e == null) {
                            Log.d(TAG, "done success");
                            addSubscription(myUser.update(bmobUser.getObjectId(), listen));
                        } else {
                            listen.done(e);
                        }

                    }

                    @Override
                    public void onProgress(Integer value) {
                        // 返回的上传进度（百分比）
                        Log.d(TAG, "onProgress: " + value);
                    }
                });
            } else {
                if(D) Log.w(TAG, "syncUserInfo Wrong, Local user is not support");
                return false;
            }
        } else {
            if(D) Log.w(TAG, "syncUserInfoWithImage Wrong, checkAPKWorkType(): " + checkAPKWorkType()
                    + ", isNetworkConnected(): " + isNetworkConnected());
            return false;
        }

        return true;
    }

    public boolean syncUserInfo(final UpdateListener listen) {
        if(checkAPKWorkType() && isNetworkConnected()) {
            final MyUser bmobUser = BmobUser.getCurrentUser(MyUser.class);
            if (bmobUser != null) {
                final MyUser myUser = new MyUser();
                myUser.setUsername(SPWristbandConfigInfo.getUserName(mContext));
                myUser.setPassword(SPWristbandConfigInfo.getUserPsw(mContext));

                myUser.setAge(SPWristbandConfigInfo.getAge(mContext));
                myUser.setGender(SPWristbandConfigInfo.getGendar(mContext));
                myUser.setHeight(SPWristbandConfigInfo.getHeight((mContext)));
                myUser.setWeight(SPWristbandConfigInfo.getWeight(mContext));
                myUser.setStepTarget(SPWristbandConfigInfo.getTotalStep(mContext));
                myUser.setNickName(SPWristbandConfigInfo.getName(mContext));

                addSubscription(myUser.update(bmobUser.getObjectId(), listen));
            } else {
                if(D) Log.w(TAG, "syncUserInfo Wrong, Local user is not support");
                return false;
            }
        } else {
            if(D) Log.w(TAG, "syncUserInfo Wrong, checkAPKWorkType(): " + checkAPKWorkType()
                    + ", isNetworkConnected(): " + isNetworkConnected());
            return false;
        }
        return true;
    }


    public boolean getAppFileVersion(final GetInfoListen listen) {
        if(checkAPKWorkType() && isNetworkConnected()) {
            // 创建云端代码对象
            AsyncCustomEndpoints cloudCode = new AsyncCustomEndpoints();
            String cloudCodeName = "getAppFileVersion";
            JSONObject params = new JSONObject();
            // 异步调用云端代码
            cloudCode.callEndpoint(cloudCodeName, params,
                    new CloudCodeListener() {
                        @Override
                        public void done(Object o, BmobException e) {
                            if (e == null) {
                                listen.onGetInfoDone(o, null);
                            } else {
                                if (D)
                                    Log.w(TAG, "getAppFileVersion, error:" + e.getMessage() + ", " + e.getErrorCode());

                                listen.onGetInfoDone(null, e);
                            }
                        }
                    });
        } else {
            if(D) Log.w(TAG, "getAppFileVersion Wrong, checkAPKWorkType(): " + checkAPKWorkType()
                    + ", isNetworkConnected(): " + isNetworkConnected());
            return false;
        }
        return true;
    }

    public boolean getPatchFileVersion(final GetInfoListen listen) {
        if(checkAPKWorkType() && isNetworkConnected()) {
            // 创建云端代码对象
            AsyncCustomEndpoints cloudCode = new AsyncCustomEndpoints();
            String cloudCodeName = "getPatchFileVersion";
            JSONObject params = new JSONObject();
            // 异步调用云端代码
            cloudCode.callEndpoint(cloudCodeName, params,
                    new CloudCodeListener() {
                        @Override
                        public void done(Object o, BmobException e) {
                            if (e == null) {
                                listen.onGetInfoDone(o, null);
                            } else {
                                if (D)
                                    Log.w(TAG, "getPatchFileVersion, error:" + e.getMessage() + ", " + e.getErrorCode());

                                listen.onGetInfoDone(null, e);
                            }
                        }
                    });
        } else {
            if(D) Log.w(TAG, "getPatchFileVersion Wrong, checkAPKWorkType(): " + checkAPKWorkType()
                    + ", isNetworkConnected(): " + isNetworkConnected());
            return false;
        }
        return true;
    }

    public boolean getOTAInfo(String type, final FindListener listen) {
        if(D) Log.w(TAG, "getOTAInfo type: " + type);
        if(checkAPKWorkType() && isNetworkConnected()) {
            BmobQuery<OTA> query = new BmobQuery<OTA>();
            if(type != null
                    && type.length() != 0) {
                query.addWhereEqualTo("OTA", type);
            }
            //执行查询方法
            query.findObjects(listen);
        } else {
            if(D) Log.w(TAG, "getOTAInfo Wrong, checkAPKWorkType(): " + checkAPKWorkType()
                    + ", isNetworkConnected(): " + isNetworkConnected());
            return false;
        }
        return true;
    }

    public boolean getOTAAppInfo(final FindListener listen) {
        return getOTAInfo(OTA.TYPE_OTA_APP, listen);
    }

    public boolean getOTAPatchInfo(final FindListener listen) {
        return getOTAInfo(OTA.TYPE_OTA_PATCH, listen);
    }




    private CompositeSubscription mCompositeSubscription;

    /**
     * 解决Subscription内存泄露问题
     * @param s
     */
    protected void addSubscription(Subscription s) {
        if (this.mCompositeSubscription == null) {
            this.mCompositeSubscription = new CompositeSubscription();
        }
        this.mCompositeSubscription.add(s);
    }

    public static abstract class GetInfoListen {
        public abstract void onGetInfoDone(Object o, BmobException e);
    }

}
