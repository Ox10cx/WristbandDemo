package com.realsil.android.wristbanddemo;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.realsil.android.wristbanddemo.ShareSdk.LoginApi;
import com.realsil.android.wristbanddemo.ShareSdk.OnLoginListener;
import com.realsil.android.wristbanddemo.ShareSdk.UserInfo;
import com.realsil.android.wristbanddemo.bmob.bean.MyUser;
import com.realsil.android.wristbanddemo.constant.ConstantParam;
import com.realsil.android.wristbanddemo.bmob.BmobControlManager;
import com.realsil.android.wristbanddemo.bmob.BmobDataSyncManager;
import com.realsil.android.wristbanddemo.utility.SPWristbandConfigInfo;

import java.util.HashMap;

import cn.bmob.v3.BmobUser;
import cn.bmob.v3.exception.BmobException;
import cn.sharesdk.framework.Platform;
import cn.sharesdk.framework.ShareSDK;
import rx.Subscriber;

/**
 * Created by rain1_wen on 2016/8/7.
 */
public class WristbandLoginActivity extends Activity implements View.OnClickListener{
    // Log
    private final static String TAG = "WristbandLoginActivity";
    private final static boolean D = true;

    private ImageView mivClearLoginUserName;
    private ImageView mivClearLoginPsw;
    private TextView mtvLogin;
    private EditText metLoginUserName;
    private EditText metLoginPsw;

    private ImageView mivLoginWechat;
    private ImageView mivLoginQQ;
    private ImageView mivLoginWeibo;

    private final static int THIRD_PARTY_LOGIN_TYPE_WEIXIN = 0;
    private final static int THIRD_PARTY_LOGIN_TYPE_QQ = 1;
    private final static int THIRD_PARTY_LOGIN_TYPE_WEIBO = 2;

    private ProgressDialog mProgressDialog = null;

    private Toast mToast;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wristband_login);

        // set UI
        setUI();
    }

    private void setUI() {
        mivClearLoginUserName = (ImageView) findViewById(R.id.ivClearLoginUserName);
        mivClearLoginUserName.setOnClickListener(this);

        mivClearLoginPsw = (ImageView) findViewById(R.id.ivClearLoginPsw);
        mivClearLoginPsw.setOnClickListener(this);

        mtvLogin = (TextView) findViewById(R.id.tvLogin);
        mtvLogin.setOnClickListener(this);

        mivLoginWechat = (ImageView)findViewById(R.id.ivLoginWechat);
        mivLoginWechat.setOnClickListener(this);

        mivLoginQQ = (ImageView)findViewById(R.id.ivLoginQQ);
        mivLoginQQ.setOnClickListener(this);

        mivLoginWeibo = (ImageView)findViewById(R.id.ivLoginWeibo);
        mivLoginWeibo.setOnClickListener(this);

        metLoginUserName = (EditText) findViewById(R.id.etLoginUserName);
        metLoginUserName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                String ss = s.toString().trim();
                if (ss == null
                        || ss.length() == 0) {
                    mivClearLoginUserName.setVisibility(View.INVISIBLE);
                } else {
                    mivClearLoginUserName.setVisibility(View.VISIBLE);
                }
            }
        });

        metLoginPsw = (EditText) findViewById(R.id.etLoginPsw);
        metLoginPsw.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                String ss = s.toString().trim();
                if (ss == null
                        || ss.length() == 0) {
                    mivClearLoginPsw.setVisibility(View.INVISIBLE);
                } else {
                    mivClearLoginPsw.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    /**
     * 点击事件
     *
     * @param v
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tvLogin://登录
                loginUserInfo();
                break;
            case R.id.ivLoginWechat:
                loginThirdPart(THIRD_PARTY_LOGIN_TYPE_WEIXIN);
                break;
            case R.id.ivLoginQQ:
                loginThirdPart(THIRD_PARTY_LOGIN_TYPE_QQ);
                break;
            case R.id.ivLoginWeibo:
                loginThirdPart(THIRD_PARTY_LOGIN_TYPE_WEIBO);
                break;
            case R.id.ivClearLoginUserName:
                metLoginUserName.setText("");
                mivClearLoginUserName.setVisibility(View.INVISIBLE);
                break;
            case R.id.ivClearLoginPsw:
                metLoginPsw.setText("");
                mivClearLoginPsw.setVisibility(View.INVISIBLE);
                break;
            default:
                break;
        }
    }


    /**
     * 第三方登录
     */
    private void loginThirdPart(int type) {
        String platformName;
        final String platformCodeForLink;
        switch (type) {
            case THIRD_PARTY_LOGIN_TYPE_WEIXIN:
                platformName = "Wechat";
                platformCodeForLink = "wechat";
                break;
            case THIRD_PARTY_LOGIN_TYPE_QQ:
                platformName = "QQ";
                platformCodeForLink = "qq";
                break;
            case THIRD_PARTY_LOGIN_TYPE_WEIBO:
                platformName = "SinaWeibo";
                platformCodeForLink = "weibo";
                break;
            default:
                platformName = "QQ";
                platformCodeForLink = "qq";
                break;
        }

        final String platformPrefix = platformName;
        LoginApi api = new LoginApi();
        //设置登陆的平台后执行登陆的方法
        api.setPlatform(platformName);
        api.setOnLoginListener(new OnLoginListener() {
            public boolean onLogin(final String platform, HashMap<String, Object> res) {
                // 在这个方法填写尝试的代码，返回true表示还不能登录，需要注册
                // 此处全部给回需要注册
                Platform plat = ShareSDK.getPlatform(platform);
                final String open_id = plat.getDb().getUserId();
                final String avater = plat.getDb().getUserIcon();
                final String userName = plat.getDb().getUserName();

                final BmobUser bu2 = new BmobUser();
                bu2.setUsername(userName);
                bu2.setPassword("");
                showProgressBar(R.string.login_logining);
                //新增加的Observable
                bu2.loginObservable(BmobUser.class).subscribe(new Subscriber<BmobUser>() {
                    @Override
                    public void onCompleted() {
                    }

                    @Override
                    public void onError(Throwable e) {
                        cancelProgressBar();
                        //showToast(String.format(getString(R.string.login_error_info), new BmobException(e).getMessage()));
                        // Not register, register now

                        // Save the info
                        SPWristbandConfigInfo.setUserName(WristbandLoginActivity.this, platformPrefix + "_" + open_id);
                        SPWristbandConfigInfo.setUserPsw(WristbandLoginActivity.this, ConstantParam.DEFAULT_USER_PSW);

                        SPWristbandConfigInfo.setUserId(WristbandLoginActivity.this, open_id);
                        SPWristbandConfigInfo.setAvatarPath(WristbandLoginActivity.this, avater);
                        SPWristbandConfigInfo.setName(WristbandLoginActivity.this, userName);

                        Intent intent = new Intent(WristbandLoginActivity.this, WristbandPersonalRegisterInfoAllActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    }

                    @Override
                    public void onNext(BmobUser bmobUser) {
                        cancelProgressBar();
                        showToast(R.string.login_success);

                        loginSuccessProcedure((MyUser) bmobUser);
                    }
                });
                return true;
            }

            public boolean onRegister(UserInfo info) {
                // 填写处理注册信息的代码，返回true表示数据合法，注册页面可以关闭
                return true;
            }
        });
        api.login(this);
    }

    private void loginUserInfo() {
        final String userName = metLoginUserName.getText().toString().trim();
        final String psw = metLoginPsw.getText().toString().trim();
        // Judge the input
        if (TextUtils.isEmpty(userName)) {
            showToast(R.string.login_user_name_should_not_null);
            return;
        }
        // Judge the input
        if (TextUtils.isEmpty(psw)) {
            showToast(R.string.login_psw_should_not_null);
            return;
        }

        // user name to create the id
        login(userName, psw);
    }

    // Temp method
    private void login(final String userName, final String psw) {
        showProgressBar(R.string.login_logining);
        if(!BmobControlManager.getInstance().login(userName, psw, new Subscriber<MyUser>() {
            @Override
            public void onCompleted() {
            }

            @Override
            public void onError(Throwable e) {
                cancelProgressBar();
                showToast(String.format(getString(R.string.login_error_info), new BmobException(e).getMessage()));
            }

            @Override
            public void onNext(MyUser bmobUser) {
                cancelProgressBar();
                showToast(R.string.login_success);

                // Save these info for feature use
                SPWristbandConfigInfo.setUserName(WristbandLoginActivity.this, userName);
                SPWristbandConfigInfo.setUserPsw(WristbandLoginActivity.this, psw);

                loginSuccessProcedure(bmobUser);
            }
        })) {
            cancelProgressBar();
        }


    }

    private void loginSuccessProcedure(MyUser myUser) {
        String userId = myUser.getObjectId();
        if(D) Log.d(TAG, "Login success: " + userId);
        SPWristbandConfigInfo.setUserId(WristbandLoginActivity.this, userId);
        SPWristbandConfigInfo.setAvatarPath(WristbandLoginActivity.this, myUser.getImage().getFileUrl());
        SPWristbandConfigInfo.setGendar(WristbandLoginActivity.this, myUser.getGender());
        SPWristbandConfigInfo.setName(WristbandLoginActivity.this, myUser.getNickName());
        SPWristbandConfigInfo.setWeight(WristbandLoginActivity.this, myUser.getWeight());
        SPWristbandConfigInfo.setHeight(WristbandLoginActivity.this, myUser.getHeight());
        SPWristbandConfigInfo.setAge(WristbandLoginActivity.this, myUser.getAge());
        SPWristbandConfigInfo.setTotalStep(WristbandLoginActivity.this, myUser.getStepTarget());

        showProgressBar(R.string.syncing_data);
        // need to sync the data from the server
        BmobDataSyncManager.syncDataFromServer(getApplication(), new BmobDataSyncManager.SyncListen() {

            @Override
            public void onSyncDone(BmobException e) {
                if (e == null) {
                    if (D) Log.d(TAG, "Sync success");
                    showToast(R.string.sync_data_success);
                    Intent intent = new Intent(WristbandLoginActivity.this, WristbandHomeActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    if (D) Log.e(TAG, "Sync error: " + e.getMessage());
                    showToast(R.string.syncing_data_fail);
                }
                cancelProgressBar();
            }
        });
    }

    // For Test
    private String getWristbandIdFromString(String userName) {
        StringBuilder id = new StringBuilder(10);
        byte[] data = userName.getBytes();

        String generateId = byte2intString(data);
        if(data.length < 10) {
            id.append(generateId);
            for(int i = 0; i < 10 - data.length; i++) {
                id.append("0");
            }
        } else {
            id.append(generateId.substring(0, 10));
        }

        return id.toString();
    }

    // use to create a special user id
    public static String byte2intString(byte[] res) {
        StringBuilder str = new StringBuilder(res.length);
        for (int i = 0; i < res.length; i++) {
            int target = res[0] & 0xff;
            str.append(String.valueOf(target).substring(0, 1));
        }
        return str.toString();
    }

    private void showProgressBar(final int message) {
        mProgressDialog = ProgressDialog.show(WristbandLoginActivity.this
                , null
                , getResources().getString(message)
                , true);
        mProgressDialog.setCancelable(false);

        mProgressBarSuperHandler.postDelayed(mProgressBarSuperTask, 30 * 1000);
    }

    private void showProgressBar(final String message) {
        mProgressDialog = ProgressDialog.show(WristbandLoginActivity.this
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
}
