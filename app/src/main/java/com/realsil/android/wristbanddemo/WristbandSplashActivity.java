package com.realsil.android.wristbanddemo;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.realsil.android.wristbanddemo.constant.ConstantParam;
import com.realsil.android.wristbanddemo.sleep.WristbandHomeFragmentSleep;
import com.realsil.android.wristbanddemo.sport.WristbandHomeFragmentSport;
import com.realsil.android.wristbanddemo.utility.GlobalGatt;
import com.realsil.android.wristbanddemo.utility.GlobalGreenDAO;
import com.realsil.android.wristbanddemo.utility.HighLightView;
import com.realsil.android.wristbanddemo.utility.RealsilFragmentPagerAdapter;
import com.realsil.android.wristbanddemo.utility.SPWristbandConfigInfo;
import com.realsil.android.wristbanddemo.utility.WristbandManager;
import com.realsil.android.wristbanddemo.utility.WristbandSplashFragment;
import com.realsil.android.wristbanddemo.utility.WriteLog;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Created by Administrator on 2016/4/13.
 */
public class WristbandSplashActivity extends FragmentActivity {
    private ImageView mivIndicatorFirst;
    private ImageView mivIndicatorSecond;
    private ImageView mivIndicatorThird;

    private boolean isAllowLogin = ConstantParam.APP_WORK_TYPE;

    private Button mbtnStart;

    private ViewPager mvpMain;
    private ArrayList<Fragment> mFragmentList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wristband_splash);
        if(!isFirstLoad()) {
            if(SPWristbandConfigInfo.getUserId(this) != null
                    || !isAllowLogin) {
                Intent intent = new Intent(WristbandSplashActivity.this, WristbandHomeActivity.class);
                WristbandSplashActivity.this.startActivity(intent);
                finish();
                return;
            } else {
                Intent intent = new Intent(WristbandSplashActivity.this, WristbandLoginActivity.class);
                WristbandSplashActivity.this.startActivity(intent);
                finish();
                return;
            }
        }

        if(SPWristbandConfigInfo.getUserId(this) != null
                || isAllowLogin) {
            Intent intent = new Intent(WristbandSplashActivity.this, WristbandLoginActivity.class);
            WristbandSplashActivity.this.startActivity(intent);
            finish();
        }

        // set UI
        setUI();

        // initial view page
        initViewPage();
    }

    private void setUI() {
        mbtnStart = (Button) findViewById(R.id.btnStart);
        mbtnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(WristbandSplashActivity.this, WristbandHomeActivity.class);
                WristbandSplashActivity.this.startActivity(intent);
                finish();
            }
        });
        mivIndicatorFirst = (ImageView) findViewById(R.id.ivIndicatorFirst);
        mivIndicatorFirst.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeFragment(0);
            }
        });

        mivIndicatorSecond = (ImageView) findViewById(R.id.ivIndicatorSecond);
        mivIndicatorSecond.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeFragment(1);
            }
        });

        mivIndicatorThird= (ImageView) findViewById(R.id.ivIndicatorThird);
        mivIndicatorThird.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeFragment(2);
            }
        });

        mvpMain = (ViewPager) findViewById(R.id.vpMain);
    }

    private void initViewPage() {
        WristbandSplashFragment one = new WristbandSplashFragment();
        WristbandSplashFragment two = new WristbandSplashFragment();
        WristbandSplashFragment three = new WristbandSplashFragment();
        if(isZh()) {
            one.initial(R.mipmap.splash_one_chinese);
            two.initial(R.mipmap.splash_two_chinese);
            three.initial(R.mipmap.splash_three_chinese);
        } else {
            one.initial(R.mipmap.splash_one_english);
            two.initial(R.mipmap.splash_two_english);
            three.initial(R.mipmap.splash_three_english);
        }
        mFragmentList=new ArrayList<Fragment>();
        mFragmentList.add(one);
        mFragmentList.add(two);
        mFragmentList.add(three);

        //ViewPager set adapter
        mvpMain.setAdapter(new RealsilFragmentPagerAdapter(getSupportFragmentManager(), mFragmentList));
        //ViewPager page change listener
        mvpMain.setOnPageChangeListener(new mOnPageChangeListener());
        //ViewPager show first fragment
        changeFragment(0);
    }

    private void changeFragment(int item) {
        mvpMain.setCurrentItem(item);
    }

    /**
     * ViewPager change Fragment, Text Color change
     */
    private class mOnPageChangeListener implements ViewPager.OnPageChangeListener{

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int position) {
            switch (position) {
                case 0:
                    mivIndicatorFirst.setImageResource(R.mipmap.page_indicator_focused);
                    mivIndicatorSecond.setImageResource(R.mipmap.page_indicator);
                    mivIndicatorThird.setImageResource(R.mipmap.page_indicator);
                    mbtnStart.setVisibility(View.GONE);
                    break;
                case 1:
                    mivIndicatorFirst.setImageResource(R.mipmap.page_indicator);
                    mivIndicatorSecond.setImageResource(R.mipmap.page_indicator_focused);
                    mivIndicatorThird.setImageResource(R.mipmap.page_indicator);
                    mbtnStart.setVisibility(View.GONE);
                    break;
                case 2:
                    mivIndicatorFirst.setImageResource(R.mipmap.page_indicator);
                    mivIndicatorSecond.setImageResource(R.mipmap.page_indicator);
                    mivIndicatorThird.setImageResource(R.mipmap.page_indicator_focused);
                    mbtnStart.setVisibility(View.VISIBLE);
                    break;
            }
        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }
    }

    private boolean isZh() {
        Locale locale = getResources().getConfiguration().locale;
        String language = locale.getLanguage();
        if (language.endsWith("zh"))
            return true;
        else
            return false;
    }

    private boolean isFirstLoad() {
        return SPWristbandConfigInfo.getFirstAppStartFlag(this);
    }

    @Override
    public void onBackPressed() {
        // Disable back.
    }
}
