package com.realsil.android.wristbanddemo;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.realsil.android.wristbanddemo.backgroundscan.BackgroundScanAutoConnected;
import com.realsil.android.wristbanddemo.bmob.BmobControlManager;
import com.realsil.android.wristbanddemo.bmob.BmobDataSyncManager;
import com.realsil.android.wristbanddemo.constant.ConstantParam;
import com.realsil.android.wristbanddemo.sleep.WristbandDetailDayFragmentSleep;
import com.realsil.android.wristbanddemo.sleep.WristbandDetailMonthFragmentSleep;
import com.realsil.android.wristbanddemo.sleep.WristbandDetailWeekFragmentSleep;
import com.realsil.android.wristbanddemo.sport.WristbandDetailDayFragmentSport;
import com.realsil.android.wristbanddemo.sport.WristbandDetailMonthFragmentSport;
import com.realsil.android.wristbanddemo.sport.WristbandDetailWeekFragmentSport;
import com.realsil.android.wristbanddemo.utility.GlobalGreenDAO;
import com.realsil.android.wristbanddemo.utility.MyDateUtils;
import com.realsil.android.wristbanddemo.utility.RealsilFragmentPagerAdapter;
import com.realsil.android.wristbanddemo.utility.RealsilLeftViewPager;
import com.realsil.android.wristbanddemo.utility.SPWristbandConfigInfo;
import com.realsil.android.wristbanddemo.utility.WristbandCalculator;

import java.util.ArrayList;
import java.util.Calendar;

import cn.bmob.v3.exception.BmobException;

public class WristbandDetailActivity extends FragmentActivity {
    // Log
    private final static String TAG = "WristbandDetailActivity";
    private final static boolean D = true;

    private GlobalGreenDAO mGlobalGreenDAO;

    private TextView mtvDetailTitle;
    private TextView mtvDayDetail;
    private TextView mtvWeekDetail;
    private TextView mtvMonthDetail;

    private LinearLayout mllDetail;

    private ImageView mivDetailBack;
    private RealsilLeftViewPager mvpMain;

    public static final String EXTRAS_DETAIL_MODE = "DETAIL_MODE";
    public static final int DETAIL_MODE_MASK = 0x0f00;
    public static final int DETAIL_MODE_SPORT_MASK = 0x0100;
    public static final int DETAIL_MODE_SLEEP_MASK = 0x0200;
    public static final int DETAIL_MODE_DAY_MASK = 0x0001;
    public static final int DETAIL_MODE_WEEK_MASK = 0x0002;
    public static final int DETAIL_MODE_MONTH_MASK = 0x0004;
    public static final int DETAIL_MODE_SPORT_DAY = DETAIL_MODE_SPORT_MASK | DETAIL_MODE_DAY_MASK;
    public static final int DETAIL_MODE_SPORT_WEEK = DETAIL_MODE_SPORT_MASK | DETAIL_MODE_WEEK_MASK;
    public static final int DETAIL_MODE_SPORT_MONTH = DETAIL_MODE_SPORT_MASK | DETAIL_MODE_MONTH_MASK;
    public static final int DETAIL_MODE_SLEEP_DAY = DETAIL_MODE_SLEEP_MASK | DETAIL_MODE_DAY_MASK;
    public static final int DETAIL_MODE_SLEEP_WEEK = DETAIL_MODE_SLEEP_MASK | DETAIL_MODE_WEEK_MASK;
    public static final int DETAIL_MODE_SLEEP_MONTH = DETAIL_MODE_SLEEP_MASK | DETAIL_MODE_MONTH_MASK;

    private int mCurrentDetailMode;

    private Calendar mCurrentCalendar;
    private ArrayList<Fragment> mFragmentList;

    private Toast mToast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wristband_detail);
        // Fragment中包含surfaceView出现闪屏问题解决方法, reference: http://blog.csdn.net/qiang_csd/article/details/43529651
        getWindow().setFormat(PixelFormat.TRANSLUCENT);
        // get the select devices info
        final Intent intent = getIntent();
        mCurrentDetailMode = intent.getIntExtra(EXTRAS_DETAIL_MODE, DETAIL_MODE_SPORT_DAY);
        if(D) Log.d(TAG, "mCurrentDetailMode: " + mCurrentDetailMode);

        // get global green dao instance
        mGlobalGreenDAO = GlobalGreenDAO.getInstance();

        mCurrentCalendar = Calendar.getInstance();

        // set UI
        setUI();

        if((mCurrentDetailMode & DETAIL_MODE_SPORT_MASK) != 0) {
            mllDetail.setBackgroundResource(R.mipmap.bg_day);
        } else {
            mllDetail.setBackgroundResource(R.mipmap.bg_night);
        }
        // initial view page
        initViewPage();

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
            //window.setNavigationBarColor(Color.TRANSPARENT);
        }
    }

    private void setUI() {
        mtvDetailTitle = (TextView) findViewById(R.id.tvDetailTitle);
        mllDetail = (LinearLayout) findViewById(R.id.llDetail);
        mivDetailBack = (ImageView) findViewById(R.id.ivDetailBack);
        mivDetailBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //mWristbandManager.SendDataRequest();
                finish();
            }
        });

        mtvDayDetail = (TextView) findViewById(R.id.tvDayDetail);
        mtvDayDetail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mtvDayDetail.setTextColor(getResources().getColor(R.color.white));
                mtvWeekDetail.setTextColor(getResources().getColor(R.color.black));
                mtvMonthDetail.setTextColor(getResources().getColor(R.color.black));
                mCurrentDetailMode = (mCurrentDetailMode & DETAIL_MODE_MASK) | DETAIL_MODE_DAY_MASK;
                if(D) Log.d(TAG, "mtvDayDetail, mCurrentDetailMode: " + mCurrentDetailMode);
                initViewPage();
            }
        });

        mtvWeekDetail = (TextView) findViewById(R.id.tvWeekDetail);
        mtvWeekDetail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mtvDayDetail.setTextColor(getResources().getColor(R.color.black));
                mtvWeekDetail.setTextColor(getResources().getColor(R.color.white));
                mtvMonthDetail.setTextColor(getResources().getColor(R.color.black));
                mCurrentDetailMode = (mCurrentDetailMode & DETAIL_MODE_MASK) | DETAIL_MODE_WEEK_MASK;
                if(D) Log.d(TAG, "mtvWeekDetail, mCurrentDetailMode: " + mCurrentDetailMode);

                // Sync data
                syncData();

                initViewPage();
            }
        });

        mtvMonthDetail = (TextView) findViewById(R.id.tvMonthDetail);
        mtvMonthDetail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mtvDayDetail.setTextColor(getResources().getColor(R.color.black));
                mtvWeekDetail.setTextColor(getResources().getColor(R.color.black));
                mtvMonthDetail.setTextColor(getResources().getColor(R.color.white));
                mCurrentDetailMode = (mCurrentDetailMode & DETAIL_MODE_MASK) | DETAIL_MODE_MONTH_MASK;
                if(D) Log.d(TAG, "mtvMonthDetail, mCurrentDetailMode: " + mCurrentDetailMode);

                // Sync data
                syncData();

                initViewPage();
            }
        });

        mvpMain = (RealsilLeftViewPager) findViewById(R.id.vpMain);
        //ViewPager page change listener
        mvpMain.setOnPageChangeListener(new mOnPageChangeListener());
    }

    RealsilFragmentPagerAdapter fragmentPagerAdapter;
    private void initViewPage() {
        if(D) Log.d(TAG, "initViewPage");
        if (mFragmentList != null) {
            if(D) Log.d(TAG, "initViewPage, mFragmentList != null");
            fragmentPagerAdapter.clear();
            mFragmentList.clear();
        }
        mFragmentList = new ArrayList<Fragment>();
        if (mCurrentDetailMode == DETAIL_MODE_SPORT_DAY) {
            Calendar c1 = Calendar.getInstance();
            mFragmentList.add(generateSpecialDateSportDayFragment(c1));
            c1.add(Calendar.DATE, -1);
            mFragmentList.add(generateSpecialDateSportDayFragment(c1));
        } else if (mCurrentDetailMode == DETAIL_MODE_SPORT_WEEK) {
            Calendar c1 = Calendar.getInstance();
            mFragmentList.add(generateSpecialDateSportWeekFragment(c1));
            c1.add(Calendar.DATE, -7); // decrease 1 week
            mFragmentList.add(generateSpecialDateSportWeekFragment(c1));
        } else if (mCurrentDetailMode == DETAIL_MODE_SPORT_MONTH) {
            Calendar c1 = Calendar.getInstance();
            mFragmentList.add(generateSpecialDateSportMonthFragment(c1));
            c1.add(Calendar.DATE, -1
                    * (WristbandCalculator.getMonthMaxDays(c1.get(Calendar.YEAR), c1.get(Calendar.MONTH) + 1))); // decrease 1 month
            mFragmentList.add(generateSpecialDateSportMonthFragment(c1));
        } else if (mCurrentDetailMode == DETAIL_MODE_SLEEP_DAY) {
            Calendar c1 = Calendar.getInstance();
            mFragmentList.add(generateSpecialDateSleepDayFragment(c1));
            c1.add(Calendar.DATE, -1);
            mFragmentList.add(generateSpecialDateSleepDayFragment(c1));
        } else if (mCurrentDetailMode == DETAIL_MODE_SLEEP_WEEK) {
            Calendar c1 = Calendar.getInstance();
            mFragmentList.add(generateSpecialDateSleepWeekFragment(c1));
            c1.add(Calendar.DATE, -7); // decrease 1 week
            mFragmentList.add(generateSpecialDateSleepWeekFragment(c1));
        } else if (mCurrentDetailMode == DETAIL_MODE_SLEEP_MONTH) {
            Calendar c1 = Calendar.getInstance();
            mFragmentList.add(generateSpecialDateSleepMonthFragment(c1));
            c1.add(Calendar.DATE, -1
                    * (WristbandCalculator.getMonthMaxDays(c1.get(Calendar.YEAR), c1.get(Calendar.MONTH) + 1))); // decrease 1 month
            mFragmentList.add(generateSpecialDateSleepMonthFragment(c1));
        }
        fragmentPagerAdapter = new RealsilFragmentPagerAdapter(getSupportFragmentManager(), mFragmentList);

        //ViewPager set adapter
        mvpMain.setAdapter(fragmentPagerAdapter);

        //ViewPager show first fragment
        mvpMain.setCurrentItem(0);

    }

    private WristbandDetailDayFragmentSport generateSpecialDateSportDayFragment(Calendar c1) {
        int year = c1.get(Calendar.YEAR);
        int month = c1.get(Calendar.MONTH) + 1;
        int day = c1.get(Calendar.DATE);
        WristbandDetailDayFragmentSport fragment = new WristbandDetailDayFragmentSport();
        Bundle bundle = new Bundle();
        bundle.putInt(WristbandDetailDayFragmentSport.EXTRAS_DATE_YEAR, year);
        bundle.putInt(WristbandDetailDayFragmentSport.EXTRAS_DATE_MONTH, month);
        bundle.putInt(WristbandDetailDayFragmentSport.EXTRAS_DATE_DAY, day);
        fragment.setArguments(bundle);
        return fragment;
    }

    private WristbandDetailWeekFragmentSport generateSpecialDateSportWeekFragment(Calendar c1) {
        int year = c1.get(Calendar.YEAR);
        int month = c1.get(Calendar.MONTH) + 1;
        int day = c1.get(Calendar.DATE);
        WristbandDetailWeekFragmentSport fragment = new WristbandDetailWeekFragmentSport();
        Bundle bundle = new Bundle();
        bundle.putInt(WristbandDetailWeekFragmentSport.EXTRAS_DATE_YEAR, year);
        bundle.putInt(WristbandDetailWeekFragmentSport.EXTRAS_DATE_MONTH, month);
        bundle.putInt(WristbandDetailWeekFragmentSport.EXTRAS_DATE_DAY, day);
        fragment.setArguments(bundle);
        return fragment;
    }
    private WristbandDetailMonthFragmentSport generateSpecialDateSportMonthFragment(Calendar c1) {
        int year = c1.get(Calendar.YEAR);
        int month = c1.get(Calendar.MONTH) + 1;
        int day = c1.get(Calendar.DATE);
        WristbandDetailMonthFragmentSport fragment = new WristbandDetailMonthFragmentSport();
        Bundle bundle = new Bundle();
        bundle.putInt(WristbandDetailMonthFragmentSport.EXTRAS_DATE_YEAR, year);
        bundle.putInt(WristbandDetailMonthFragmentSport.EXTRAS_DATE_MONTH, month);
        bundle.putInt(WristbandDetailMonthFragmentSport.EXTRAS_DATE_DAY, day);
        fragment.setArguments(bundle);
        return fragment;
    }

    private WristbandDetailDayFragmentSleep generateSpecialDateSleepDayFragment(Calendar c1) {
        int year = c1.get(Calendar.YEAR);
        int month = c1.get(Calendar.MONTH) + 1;
        int day = c1.get(Calendar.DATE);
        WristbandDetailDayFragmentSleep fragment = new WristbandDetailDayFragmentSleep();
        Bundle bundle = new Bundle();
        bundle.putInt(WristbandDetailDayFragmentSleep.EXTRAS_DATE_YEAR, year);
        bundle.putInt(WristbandDetailDayFragmentSleep.EXTRAS_DATE_MONTH, month);
        bundle.putInt(WristbandDetailDayFragmentSleep.EXTRAS_DATE_DAY, day);
        fragment.setArguments(bundle);
        return fragment;
    }

    private WristbandDetailWeekFragmentSleep generateSpecialDateSleepWeekFragment(Calendar c1) {
        int year = c1.get(Calendar.YEAR);
        int month = c1.get(Calendar.MONTH) + 1;
        int day = c1.get(Calendar.DATE);
        WristbandDetailWeekFragmentSleep fragment = new WristbandDetailWeekFragmentSleep();
        Bundle bundle = new Bundle();
        bundle.putInt(WristbandDetailWeekFragmentSleep.EXTRAS_DATE_YEAR, year);
        bundle.putInt(WristbandDetailWeekFragmentSleep.EXTRAS_DATE_MONTH, month);
        bundle.putInt(WristbandDetailWeekFragmentSleep.EXTRAS_DATE_DAY, day);
        fragment.setArguments(bundle);
        return fragment;
    }

    private WristbandDetailMonthFragmentSleep generateSpecialDateSleepMonthFragment(Calendar c1) {
        int year = c1.get(Calendar.YEAR);
        int month = c1.get(Calendar.MONTH) + 1;
        int day = c1.get(Calendar.DATE);
        WristbandDetailMonthFragmentSleep fragment = new WristbandDetailMonthFragmentSleep();
        Bundle bundle = new Bundle();
        bundle.putInt(WristbandDetailMonthFragmentSport.EXTRAS_DATE_YEAR, year);
        bundle.putInt(WristbandDetailMonthFragmentSport.EXTRAS_DATE_MONTH, month);
        bundle.putInt(WristbandDetailMonthFragmentSport.EXTRAS_DATE_DAY, day);
        fragment.setArguments(bundle);
        return fragment;
    }
    // Judge reach the bottom
    private boolean isScrolling;
    private boolean isBottom;
    /**
     * ViewPager change Fragment, Text Color change
     */
    private class mOnPageChangeListener implements RealsilLeftViewPager.OnPageChangeListener{
        @Override
        public void onPageScrollStateChanged(int state) {
            if(D) Log.d(TAG, "onPageScrollStateChanged, state: " + state + ", isScrolling: " + isScrolling
                    + ", isBottom: " + isBottom + ", mvpMain.getCurrentItem(): " + mvpMain.getCurrentItem());
            if (state == 1) {
                isScrolling = true;
            } else {
                isScrolling = false;
            }
            if(state == 0) {
                if(mvpMain.getCurrentItem() == 0) {
                    if(isBottom) {
                        if((mCurrentDetailMode & DETAIL_MODE_DAY_MASK) != 0) {
                            showToast(R.string.tomorrow_has_not_start_yet);
                        } else {
                            showToast(R.string.future_has_not_start_yet);
                        }
                        isBottom = false;
                    }
                }
            }
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            /*
            if(D) Log.d(TAG, "onPageScrolled, position: " + position + ", positionOffset: " + positionOffset
                    + ", positionOffsetPixels: " + positionOffsetPixels
                    + ", isScrolling: " + isScrolling);*/
            if(position == 0) {
                if (isScrolling) {
                    if (positionOffsetPixels == 0
                            && positionOffset == 0) {
                        isBottom = true;
                    } else {
                        isBottom = false;
                    }
                }
            }
            //if(D) Log.d(TAG, "onPageScrolled, isBottom: " + isBottom);
        }

        @Override
        public void onPageSelected(int position) {
            if(D) Log.d(TAG, "onPageSelected. position: " + position +
                    ", mCurrentDetailMode: " + mCurrentDetailMode +
                    ", mFragmentList.size(): " + mFragmentList.size());

            // Sync data
            syncData(position);

            if(mCurrentDetailMode == DETAIL_MODE_SPORT_DAY) {
                if(position == mFragmentList.size() - 1) {
                    Calendar c1 = Calendar.getInstance();
                    c1.add(Calendar.DATE, -1 * mFragmentList.size());

                    if(D) Log.d(TAG, "create fragment. c1: " + c1.toString() +
                            ", mFragmentSportDayList.size(): " + mFragmentList.size());
                    // create a fragment
                    //mFragmentSportDayList.add(generateSpecialDateSportFragment(c1));
                    mFragmentList.add(generateSpecialDateSportDayFragment(c1));
                    fragmentPagerAdapter.notifyDataSetChanged();
                }
            } else if(mCurrentDetailMode == DETAIL_MODE_SPORT_WEEK) {
                if(position == mFragmentList.size() - 1) {
                    Calendar c1 = Calendar.getInstance();
                    c1.add(Calendar.DATE, -7 * mFragmentList.size());

                    if(D) Log.d(TAG, "create fragment. c1: " + c1.toString() +
                            ", mFragmentSportDayList.size(): " + mFragmentList.size());
                    // create a fragment
                    //mFragmentSportDayList.add(generateSpecialDateSportFragment(c1));
                    mFragmentList.add(generateSpecialDateSportWeekFragment(c1));
                    fragmentPagerAdapter.notifyDataSetChanged();
                }
            } else if(mCurrentDetailMode == DETAIL_MODE_SPORT_MONTH) {
                if(position == mFragmentList.size() - 1) {
                    Calendar c1 = Calendar.getInstance();
                    for(int i = 0; i < mFragmentList.size(); ++i) {
                        c1.add(Calendar.DATE, -1
                                * (WristbandCalculator.getMonthMaxDays(c1.get(Calendar.YEAR), c1.get(Calendar.MONTH) + 1)));
                    }

                    if(D) Log.d(TAG, "create fragment. c1: " + c1.toString() +
                            ", mFragmentSportDayList.size(): " + mFragmentList.size());
                    // create a fragment
                    //mFragmentSportDayList.add(generateSpecialDateSportFragment(c1));
                    mFragmentList.add(generateSpecialDateSportMonthFragment(c1));
                    fragmentPagerAdapter.notifyDataSetChanged();
                }
            } else if(mCurrentDetailMode == DETAIL_MODE_SLEEP_DAY) {
                if(position == mFragmentList.size() - 1) {
                    Calendar c1 = Calendar.getInstance();
                    c1.add(Calendar.DATE, -1 * mFragmentList.size());

                    if(D) Log.d(TAG, "create fragment. c1: " + c1.toString() +
                            ", mFragmentSportDayList.size(): " + mFragmentList.size());
                    // create a fragment
                    //mFragmentSportDayList.add(generateSpecialDateSportFragment(c1));
                    mFragmentList.add(generateSpecialDateSleepDayFragment(c1));
                    fragmentPagerAdapter.notifyDataSetChanged();
                }
            } else if(mCurrentDetailMode == DETAIL_MODE_SLEEP_WEEK) {
                if(position == mFragmentList.size() - 1) {
                    Calendar c1 = Calendar.getInstance();
                    c1.add(Calendar.DATE, -7 * mFragmentList.size());

                    if(D) Log.d(TAG, "create fragment. c1: " + c1.toString() +
                            ", mFragmentSleepDayList.size(): " + mFragmentList.size());
                    // create a fragment
                    //mFragmentSleepDayList.add(generateSpecialDateSleepFragment(c1));
                    mFragmentList.add(generateSpecialDateSleepWeekFragment(c1));
                    fragmentPagerAdapter.notifyDataSetChanged();
                }
            } else if(mCurrentDetailMode == DETAIL_MODE_SLEEP_MONTH) {
                if(position == mFragmentList.size() - 1) {
                    Calendar c1 = Calendar.getInstance();
                    for(int i = 0; i < mFragmentList.size(); ++i) {
                        c1.add(Calendar.DATE, -1
                                * (WristbandCalculator.getMonthMaxDays(c1.get(Calendar.YEAR), c1.get(Calendar.MONTH) + 1)));
                    }

                    if(D) Log.d(TAG, "create fragment. c1: " + c1.toString() +
                            ", mFragmentSleepDayList.size(): " + mFragmentList.size());
                    // create a fragment
                    //mFragmentSportDayList.add(generateSpecialDateSportFragment(c1));
                    mFragmentList.add(generateSpecialDateSleepMonthFragment(c1));
                    fragmentPagerAdapter.notifyDataSetChanged();
                }
            }


        }
    }

    private void syncData(int position) {
        Calendar c1 = Calendar.getInstance();
        // Sync more date
        if((mCurrentDetailMode & DETAIL_MODE_SLEEP_DAY) != 0) {
            if(position == mFragmentList.size() - 1) {
                c1.add(Calendar.DATE, -1 * (mFragmentList.size() - 1));// need less one day
            }
        } else if((mCurrentDetailMode & DETAIL_MODE_SLEEP_WEEK) != 0) {
            if(position == mFragmentList.size() - 1) {
                c1.add(Calendar.DATE, -7 * (mFragmentList.size() - 1));// need less one week
            }
        }else if((mCurrentDetailMode & DETAIL_MODE_SLEEP_MONTH) != 0) {
            if(position == mFragmentList.size() - 1) {
                for(int i = 0; i <= mFragmentList.size(); ++i) {// need less one month
                    c1.add(Calendar.DATE, -1
                            * (WristbandCalculator.getMonthMaxDays(c1.get(Calendar.YEAR), c1.get(Calendar.MONTH) + 1)));
                }
            }
        }

        syncData(c1);
    }

    private void syncData() {
        Calendar c1 = Calendar.getInstance();
        // Sync more date
        if((mCurrentDetailMode & DETAIL_MODE_SLEEP_DAY) != 0) {
            c1.add(Calendar.DATE, -1);// need less one day
        } else if((mCurrentDetailMode & DETAIL_MODE_SLEEP_WEEK) != 0) {
            c1.add(Calendar.DATE, -7);// need less one week
        }else if((mCurrentDetailMode & DETAIL_MODE_SLEEP_MONTH) != 0) {
            c1.add(Calendar.DATE, -1
                    * (WristbandCalculator.getMonthMaxDays(c1.get(Calendar.YEAR), c1.get(Calendar.MONTH) + 1)));// need less one month
        }

        syncData(c1);
    }

    private void syncData(Calendar c1) {
        if(!(BmobControlManager.getInstance().checkAPKWorkType() && BmobControlManager.getInstance().isNetworkConnected())) {
            if(D) Log.w(TAG, "syncData check failed");
            return;
        }

        Context context = getApplication();

        String format = ConstantParam.DEFAULT_DATE_FORMAT;
        // We only sync some date from server
        String temp = SPWristbandConfigInfo.getBmobLastHistorySyncDate(context);
        if(temp == null) {
            temp = MyDateUtils.getCurDate(format);
        }
        final String lastSyncDate = temp;
        Calendar lastSyncDateCalendar = MyDateUtils.getCalendarFromDate(lastSyncDate, format);

        if(D) Log.w(TAG, "syncData, mCurrentDetailMode: " + mCurrentDetailMode
                + ", c1: " + MyDateUtils.getDateFromCalendar(c1, ConstantParam.DEFAULT_DATE_FORMAT)
                + ", lastSyncDateCalendar: " + MyDateUtils.getDateFromCalendar(lastSyncDateCalendar, ConstantParam.DEFAULT_DATE_FORMAT));

        // Sync more date
        if((mCurrentDetailMode & DETAIL_MODE_SLEEP_DAY) != 0) {

            if(lastSyncDateCalendar.compareTo(c1) >= 0) {
                showProgressBar(R.string.syncing_data);
                BmobDataSyncManager.syncDataFromServer(context, syncListen);
            }
        } else if((mCurrentDetailMode & DETAIL_MODE_SLEEP_WEEK) != 0) {

            if(lastSyncDateCalendar.compareTo(c1) >= 0) {
                showProgressBar(R.string.syncing_data);
                BmobDataSyncManager.syncDataFromServer(7 * ConstantParam.DEFAULT_HISTORY_SYNC_MAX_SIZE, context, syncListen);
            }
        }else if((mCurrentDetailMode & DETAIL_MODE_SLEEP_MONTH) != 0) {

            if(lastSyncDateCalendar.compareTo(c1) >= 0) {
                showProgressBar(R.string.syncing_data);
                BmobDataSyncManager.syncDataFromServer(31 * ConstantParam.DEFAULT_HISTORY_SYNC_MAX_SIZE, context, syncListen);
            }
        }
    }

    BmobDataSyncManager.SyncListen syncListen = new BmobDataSyncManager.SyncListen() {
        @Override
        public void onSyncDone(BmobException e) {
            if (e == null) {
                if (D) Log.d(TAG, "Sync success");
                showToast(R.string.sync_data_success);
                int cur = mvpMain.getCurrentItem();
                for(int i = cur; i < mFragmentList.size(); i++) {
                    // Refresh the ui.
                    mFragmentList.get(i).onResume();
                }
            } else {
                if (D) Log.e(TAG, "Sync error: " + e.getMessage());
                showToast(R.string.syncing_data_fail);
            }

            cancelProgressBar();
        }
    };


    private ProgressDialog mProgressDialog = null;
    private void showProgressBar(final int message) {
        mProgressDialog = ProgressDialog.show(WristbandDetailActivity.this
                , null
                , getResources().getString(message)
                , true);
        mProgressDialog.setCancelable(false);

        mProgressBarSuperHandler.postDelayed(mProgressBarSuperTask, 30 * 1000);
    }

    private void showProgressBar(final String message) {
        mProgressDialog = ProgressDialog.show(WristbandDetailActivity.this
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

    @Override
    protected void onStop() {
        if(D) Log.d(TAG, "onStop()");
        super.onStop();
    }

    @Override
    protected void onResume() {
        if(D) Log.d(TAG, "onResume()");
        super.onResume();

        BackgroundScanAutoConnected.getInstance().startAutoConnect();

        // initial view page
        //initViewPage();
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
    }

}
