package com.realsil.android.wristbanddemo;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.LoaderManager;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.realsil.android.blehub.dfu.BinInputStream;
import com.realsil.android.blehub.dfu.RealsilDfu;
import com.realsil.android.blehub.dfu.RealsilDfuCallback;
import com.realsil.android.wristbanddemo.ShareSdk.MyShareSdk;
import com.realsil.android.wristbanddemo.backgroundscan.BackgroundScanAutoConnected;
import com.realsil.android.wristbanddemo.constant.ConstantParam;
import com.realsil.android.wristbanddemo.greendao.SportData;
import com.realsil.android.wristbanddemo.sport.SportLineUiManager;
import com.realsil.android.wristbanddemo.sport.SportSubData;
import com.realsil.android.wristbanddemo.utility.GlobalGatt;
import com.realsil.android.wristbanddemo.utility.GlobalGreenDAO;
import com.realsil.android.wristbanddemo.utility.HighLightView;
import com.realsil.android.wristbanddemo.utility.ImageLoadingUtils;
import com.realsil.android.wristbanddemo.utility.SPWristbandConfigInfo;
import com.realsil.android.wristbanddemo.utility.WristbandCalculator;
import com.realsil.android.wristbanddemo.utility.WristbandManager;
import com.realsil.android.wristbanddemo.utility.WristbandManagerCallback;
import com.realsil.android.wristbanddemo.view.CircularImageView;
import com.realsil.android.wristbanddemo.view.SwipeBackActivity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import lecho.lib.hellocharts.gesture.ZoomType;

public class WristbandShareActivity extends SwipeBackActivity implements View.OnClickListener {
    // Log
    private final static String TAG = "WristbandShareActivity";
    private final static boolean D = true;

    private ImageView mivShareBack;
    private ImageView mivShare;
    private CircularImageView mivShareHeadPortrait;
    private TextView mtvShareName;
    private TextView mtvTotalStep;
    private TextView mtvTotalDistance;
    private TextView mtvTotalCalory;

    String mShareImagePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wristband_share_sport);
        initialStringFormat();

        setUI();

        initialUI();
    }

    private String mFormatSportTotalStep;
    private String mFormatSportCurrentDistance;
    private String mFormatSportCurrentCalorie;
    //private String mSportCurrentQuality;

    private void initialStringFormat() {
        mFormatSportTotalStep = getResources().getString(R.string.total_step_value);
        mFormatSportCurrentDistance = getResources().getString(R.string.distance_value);
        mFormatSportCurrentCalorie = getResources().getString(R.string.calorie_value);
    }

    private void initialUI() {
        String avatarPath = SPWristbandConfigInfo.getAvatarPath(this);


        if(avatarPath == null) {
            if(SPWristbandConfigInfo.getGendar(this)) {
                mivShareHeadPortrait.setImageResource(R.mipmap.head_portrait_default_man);
            } else {
                mivShareHeadPortrait.setImageResource(R.mipmap.head_portrait_default_woman);
            }

        } else {
            if(D) Log.d(TAG, "avatarPath: " + avatarPath);
            //Uri uri = Uri.fromFile(new File(avatarPath));
            //mivPersonageHeadPortrait.setImageURI(uri);
            if(SPWristbandConfigInfo.getGendar(this)) {
                ImageLoadingUtils.getImage(mivShareHeadPortrait, avatarPath, R.mipmap.head_portrait_default_man);
            } else {
                ImageLoadingUtils.getImage(mivShareHeadPortrait, avatarPath, R.mipmap.head_portrait_default_woman);
            }
        }

        String name = SPWristbandConfigInfo.getName(this);
        if(name == null) {
            mtvShareName.setText(R.string.settings_personage_name);
        } else {
            if(D) Log.d(TAG, "name: " + name);
            mtvShareName.setText(name);
        }

        Calendar today = Calendar.getInstance();

        List<SportData> sports = GlobalGreenDAO.getInstance().loadSportDataByDate(today.get(Calendar.YEAR)
                , today.get(Calendar.MONTH) + 1// here need add 1, because it origin range is 0 - 11;
                , today.get(Calendar.DATE));

        SportSubData subData = WristbandCalculator.sumOfSportDataByDate(today.get(Calendar.YEAR)
                , today.get(Calendar.MONTH) + 1// here need add 1, because it origin range is 0 - 11;
                , today.get(Calendar.DATE), sports);

        if(subData != null) {
            mtvTotalStep.setText(String.format(mFormatSportTotalStep, subData.getStepCount()));

            mtvTotalDistance.setText(String.format(mFormatSportCurrentDistance, (float) subData.getDistance() / 1000));
            mtvTotalCalory.setText(String.format(mFormatSportCurrentCalorie, (float) subData.getCalory() / 1000));

        } else {
            mtvTotalStep.setText(String.format(mFormatSportTotalStep, 0));

            mtvTotalDistance.setText(String.format(mFormatSportCurrentDistance, 0.0));
            mtvTotalCalory.setText(String.format(mFormatSportCurrentCalorie, 0.0));
        }
    }

    private void setUI() {
        Typeface type = Typeface.createFromAsset(getAssets(), "fonts/american_typewriter.ttf");

        mivShareBack = (ImageView) findViewById(R.id.ivShareBack);
        mivShareBack.setOnClickListener(this);

        mivShare = (ImageView) findViewById(R.id.ivShare);
        mivShare.setOnClickListener(this);
        mivShare.post(new Runnable() {
            @Override
            public void run() {
                mivShare.callOnClick();
            }
        });
        mivShare.setVisibility(View.INVISIBLE);
        mivShareBack.setVisibility(View.INVISIBLE);

        mivShareHeadPortrait = (CircularImageView) findViewById(R.id.ivShareHeadPortrait);

        mtvShareName = (TextView) findViewById(R.id.tvShareName);
        mtvTotalStep = (TextView) findViewById(R.id.tvTotalStep);
        mtvTotalDistance = (TextView) findViewById(R.id.tvTotalDistance);
        mtvTotalCalory = (TextView) findViewById(R.id.tvTotalCalory);

        mtvShareName.setTypeface(type);
        mtvTotalStep.setTypeface(type);
        mtvTotalDistance.setTypeface(type);
        mtvTotalCalory.setTypeface(type);
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.ivShareBack://登录
                finish();
                break;
            case R.id.ivShare:
                createShareImage();
                Context context = getApplication();
                MyShareSdk shareSdk = new MyShareSdk();
                //shareSdk.setTitle(getString(R.string.share_title));
                //shareSdk.setText(context.getString(R.string.share_content));
                //shareSdk.setUrl("http://www.realsil.com.cn/");
                //shareSdk.setSiteUrl("http://www.realsil.com.cn/");
                //shareSdk.setTitleUrl("http://www.realsil.com.cn/");
                shareSdk.setImagePath(mShareImagePath);
                //shareSdk.setViewToShare(getWindow().getDecorView());
                shareSdk.show(context);
                break;
            default:
                break;
        }
    }

    private String createShareImage() {
        if(mShareImagePath != null) {
            return mShareImagePath;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss",
                Locale.US);

        final String fname = ConstantParam.IMAGE_SAVE_CACHE + sdf.format(new Date())+ ".png";
        if(D) Log.e(TAG, "createShareImage, fname: " + fname);

        ViewGroup view = (ViewGroup) getWindow().getDecorView();
        if(D) Log.e(TAG, "view == NULL? " + (view==null));

        view.setDrawingCacheEnabled(true);

        view.buildDrawingCache();

        final Bitmap bitmap = view.getDrawingCache();

        mivShare.setVisibility(View.VISIBLE);
        mivShareBack.setVisibility(View.VISIBLE);
        new Thread(new Runnable() {
            @Override
            public void run() {
                if(bitmap != null) {
                    if(D) Log.d(TAG, "bitmap got!");
                    try {
                        FileOutputStream out = new FileOutputStream(fname);
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                        if(D) Log.d(TAG, "file " + fname + "output done.");
                    }catch(Exception e) {
                        e.printStackTrace();
                    }
                }else{
                    if(D) Log.d(TAG, "bitmap is NULL!");
                }
            }
        }).start();

        mShareImagePath = fname;

        return fname;
    }
}
