package com.realsil.android.wristbanddemo;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.realsil.android.wristbanddemo.backgroundscan.BackgroundScanAutoConnected;
import com.realsil.android.wristbanddemo.constant.ConstantParam;
import com.realsil.android.wristbanddemo.bmob.BmobControlManager;
import com.realsil.android.wristbanddemo.utility.GlobalGreenDAO;
import com.realsil.android.wristbanddemo.utility.ImageLoadingUtils;
import com.realsil.android.wristbanddemo.utility.SPWristbandConfigInfo;
import com.realsil.android.wristbanddemo.utility.ValuePickerFragment;
import com.realsil.android.wristbanddemo.utility.WristbandManager;
import com.realsil.android.wristbanddemo.view.SwipeBackActivity;

import java.io.File;

import cn.bmob.v3.exception.BmobException;
import cn.bmob.v3.listener.UpdateListener;

public class WristbandSettingPersonalActivity extends SwipeBackActivity implements ValuePickerFragment.OnSaveListener {
    // Log
    private final static String TAG = "WristbandSettingPersonalActivity";
    private final static boolean D = true;

    private WristbandManager mWristbandManager;
    private final int REQUEST_CODE_CAPTURE_CAMEIA = 1;
    private final int REQUEST_CODE_PICK_IMAGE = 2;
    private final int REQUEST_CODE_CROP = 3;

    private RelativeLayout mrlHeadPortrait;
    private RelativeLayout mrlPersonageGender;
    private RelativeLayout mrlPersonageAge;
    private RelativeLayout mrlPersonageHeight;
    private RelativeLayout mrlPersonageWeight;
    private RelativeLayout mrlPersonageGoal;
    private RelativeLayout mrlSettingsLogOff;

    private TextView mtvPersonalName;

    private TextView mtvPersonalGenderValue;
    private TextView mtvPersonalAgeValue;
    private TextView mtvPersonalHeightValue;
    private TextView mtvPersonalWeightValue;
    private TextView mtvPersonalGoalValue;

    private ImageView mivPersonageHeadPortrait;
    private ImageView mivPersonageBack;

    private ProgressDialog mProgressDialog = null;

    private Toast mToast;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wristband_personage);

        // get wristband instance
        mWristbandManager = WristbandManager.getInstance();

        // set UI
        setUI();

        initialStringFormat();
    }

    private String mFormatAgeValue;
    private String mFormatHeightValue;
    private String mFormatWeightValue;
    private String mFormatGoalValue;


    private void initialStringFormat() {
        mFormatAgeValue = getResources().getString(R.string.age_value);
        mFormatHeightValue = getResources().getString(R.string.cm_value);
        mFormatWeightValue = getResources().getString(R.string.kilogram_value);
        mFormatGoalValue = getResources().getString(R.string.step_value);
    }

    private void initialUI() {
        if(SPWristbandConfigInfo.getGendar(this)) {
            mtvPersonalGenderValue.setText(getResources().getString(R.string.settings_personage_gender_male));
        } else {
            mtvPersonalGenderValue.setText(getResources().getString(R.string.settings_personage_gender_female));
        }

        String avatarPath = SPWristbandConfigInfo.getAvatarPath(this);

        if(avatarPath == null) {
            if(SPWristbandConfigInfo.getGendar(this)) {
                mivPersonageHeadPortrait.setImageResource(R.mipmap.head_portrait_default_man);
            } else {
                mivPersonageHeadPortrait.setImageResource(R.mipmap.head_portrait_default_woman);
            }

        } else {
            if(D) Log.d(TAG, "avatarPath: " + avatarPath);
            //Uri uri = Uri.fromFile(new File(avatarPath));
            //mivPersonageHeadPortrait.setImageURI(uri);
            if(SPWristbandConfigInfo.getGendar(this)) {
                ImageLoadingUtils.getImage(mivPersonageHeadPortrait, avatarPath, R.mipmap.head_portrait_default_man);
            } else {
                ImageLoadingUtils.getImage(mivPersonageHeadPortrait, avatarPath, R.mipmap.head_portrait_default_woman);
            }
        }

        String name = SPWristbandConfigInfo.getName(this);
        if(name == null) {
            mtvPersonalName.setText(R.string.settings_personage_name);
        } else {
            if(D) Log.d(TAG, "name: " + name);
            mtvPersonalName.setText(name);
        }

        mtvPersonalAgeValue.setText(String.format(mFormatAgeValue, SPWristbandConfigInfo.getAge(this)));
        mtvPersonalHeightValue.setText(String.format(mFormatHeightValue, SPWristbandConfigInfo.getHeight(this)));
        mtvPersonalWeightValue.setText(String.format(mFormatWeightValue, SPWristbandConfigInfo.getWeight(this)));
        mtvPersonalGoalValue.setText(String.format(mFormatGoalValue, SPWristbandConfigInfo.getTotalStep(this)));
    }

    private void setUI() {
        mtvPersonalName = (TextView) findViewById(R.id.tvPersonalName);
        mtvPersonalName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final EditText et = new EditText(WristbandSettingPersonalActivity.this);
                et.setSingleLine(true);
                new AlertDialog.Builder(WristbandSettingPersonalActivity.this, AlertDialog.THEME_HOLO_LIGHT)
                        .setTitle(R.string.settings_personage_rename)
                        .setView(et)
                        .setPositiveButton(R.string.sure, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                String input = et.getText().toString();
                                if (input.equals("")) {
                                    showToast(R.string.name_should_not_null);
                                }
                                else {
                                    if(D) Log.d(TAG, "set the personage name, name: " + input);
                                    //SPWristbandConfigInfo.setName(WristbandSettingPersonalActivity.this, input);
                                    updateNickName(input);
                                    initialUI();
                                }
                            }
                        })
                        .setNegativeButton(R.string.cancel, null)
                        .show();
            }
        });
        mtvPersonalGenderValue = (TextView) findViewById(R.id.tvPersonalGenderValue);
        mtvPersonalAgeValue = (TextView) findViewById(R.id.tvPersonalAgeValue);
        mtvPersonalHeightValue = (TextView) findViewById(R.id.tvPersonalHeightValue);
        mtvPersonalWeightValue = (TextView) findViewById(R.id.tvPersonalWeightValue);
        mtvPersonalGoalValue = (TextView) findViewById(R.id.tvPersonalGoalValue);

        mivPersonageHeadPortrait = (ImageView) findViewById(R.id.ivPersonageHeadPortrait);
        mivPersonageHeadPortrait.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String[] array = new String[] { getString(R.string.settings_personage_set_avatar_album)
                        , getString(R.string.settings_personage_set_avatar_photograph)};
                Dialog dialog = new AlertDialog.Builder(WristbandSettingPersonalActivity.this, AlertDialog.THEME_HOLO_LIGHT)
                        .setTitle(R.string.settings_personage_set_avatar)
                        .setItems(array, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if(which == 0) {// get picture
                                    getImageFromGallery();
                                } else {
                                    getImageFromCamera();
                                }
                            }
                        })
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface dialog, int which) {
                                // TODO Auto-generated method stub
                                dialog.dismiss();
                            }
                        }).create();
                dialog.show();
            }
        });

        mivPersonageBack = (ImageView) findViewById(R.id.ivPersonageBack);
        mivPersonageBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //mWristbandManager.SendDataRequest();
                finish();
            }
        });

        mrlPersonageGender = (RelativeLayout) findViewById(R.id.rlPersonageGender);
        mrlPersonageGender.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean currentGendar = SPWristbandConfigInfo.getGendar(WristbandSettingPersonalActivity.this);
                SPWristbandConfigInfo.setGendar(WristbandSettingPersonalActivity.this, !currentGendar);

                syncUserProfile();
                initialUI();
            }
        });

        mrlPersonageAge = (RelativeLayout) findViewById(R.id.rlPersonageAge);
        mrlPersonageAge.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSetInfoDialog(ValuePickerFragment.TYPE_AGE, SPWristbandConfigInfo.getAge(WristbandSettingPersonalActivity.this));
            }
        });
        mrlPersonageHeight = (RelativeLayout) findViewById(R.id.rlPersonageHeight);
        mrlPersonageHeight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSetInfoDialog(ValuePickerFragment.TYPE_HEIGHT, SPWristbandConfigInfo.getHeight(WristbandSettingPersonalActivity.this));
            }
        });
        mrlPersonageWeight = (RelativeLayout) findViewById(R.id.rlPersonageWeight);
        mrlPersonageWeight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSetInfoDialog(ValuePickerFragment.TYPE_WEIGHT, SPWristbandConfigInfo.getWeight(WristbandSettingPersonalActivity.this));
            }
        });
        mrlPersonageGoal = (RelativeLayout) findViewById(R.id.rlPersonageGoal);
        mrlPersonageGoal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSetInfoDialog(ValuePickerFragment.TYPE_TOTAL_STEP, SPWristbandConfigInfo.getTotalStep(WristbandSettingPersonalActivity.this));
            }
        });
        mrlSettingsLogOff= (RelativeLayout) findViewById(R.id.rlSettingsLogOff);
        mrlSettingsLogOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder aa = new AlertDialog.Builder(WristbandSettingPersonalActivity.this, AlertDialog.THEME_HOLO_LIGHT);
                aa.setTitle(R.string.settings_personage_log_off_title);
                aa.setMessage(R.string.settings_personage_log_off_text);
                aa.setPositiveButton(R.string.sure, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // TODO Auto-generated method stub
                        // need close?
                        mWristbandManager.close();

                        SPWristbandConfigInfo.setUserId(WristbandSettingPersonalActivity.this, null);

                        // need clear all user info?
                        SPWristbandConfigInfo.deleteAll(WristbandSettingPersonalActivity.this);
                        /*
                        SPWristbandConfigInfo.setAge(WristbandSettingPersonalActivity.this, 0);
                        SPWristbandConfigInfo.setAvatarPath(WristbandSettingPersonalActivity.this, null);
                        SPWristbandConfigInfo.setName(WristbandSettingPersonalActivity.this, null);
                        SPWristbandConfigInfo.setTotalStep(WristbandSettingPersonalActivity.this, 0);
                        SPWristbandConfigInfo.setWeight(WristbandSettingPersonalActivity.this, 0);
                        SPWristbandConfigInfo.setGendar(WristbandSettingPersonalActivity.this, false);
                        SPWristbandConfigInfo.setHeight(WristbandSettingPersonalActivity.this, 0);*/

                        // need clear all data?
                        GlobalGreenDAO.getInstance().deleteAllSportData();
                        GlobalGreenDAO.getInstance().deleteAllSleepData();

                        // Log off
                        BmobControlManager.getInstance().logOff();

                        Intent intent = new Intent(WristbandSettingPersonalActivity.this, WristbandLoginActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        WristbandSettingPersonalActivity.this.startActivity(intent);
                        finish();
                    }
                });
                aa.setNegativeButton(R.string.cancel, null);
                aa.create();
                aa.show();


            }
        });
        if(ConstantParam.APP_WORK_TYPE != ConstantParam.WORK_TYPE_INTERNET) {
            mrlSettingsLogOff.setVisibility(View.GONE);
        }
    }



    @Override
    public void onValueInfoSaved(int type, int value) {
        if(D) Log.i(TAG, "onValueInfoSaved, type: " + type + ", value: " + value);

        switch (type) {
            case ValuePickerFragment.TYPE_AGE:
                SPWristbandConfigInfo.setAge(this, value);
                syncUserProfile();
                break;
            case ValuePickerFragment.TYPE_HEIGHT:
                SPWristbandConfigInfo.setHeight(this, value);
                syncUserProfile();
                break;
            case ValuePickerFragment.TYPE_WEIGHT:
                SPWristbandConfigInfo.setWeight(this, value);
                syncUserProfile();
                break;
            case ValuePickerFragment.TYPE_TOTAL_STEP:
                SPWristbandConfigInfo.setTotalStep(this, value);
                syncTotalStep();
                break;
        }

        initialUI();
    }

    private void syncUserProfile() {
        if(!mWristbandManager.isConnect()) {
            if(D) Log.w(TAG, "syncUserProfile failed, not connect. check update other internet info.");
            showProgressBar(R.string.settings_mydevice_syncing_user_profile);
            if(BmobControlManager.getInstance().syncUserInfo(new UpdateListener() {
                @Override
                public void done(final BmobException e) {
                    if(e == null) {

                    } else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                showToast(e.getMessage());
                            }
                        });
                    }
                    cancelProgressBar();
                }
            })) {

            } else {
                cancelProgressBar();
            }
            return;
        }
        showProgressBar(R.string.settings_mydevice_syncing_user_profile);
        new Thread(new Runnable() {
            @Override
            public void run() {
                if(!mWristbandManager.SetUserProfile()) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showToast(R.string.something_error);
                        }
                    });
                    //mWristbandManager.close();

                    cancelProgressBar();
                } else {
                    if(BmobControlManager.getInstance().syncUserInfo(new UpdateListener() {
                        @Override
                        public void done(final BmobException e) {
                            if(e == null) {

                            } else {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        showToast(e.getMessage());
                                    }
                                });
                            }
                            cancelProgressBar();
                        }
                    })) {

                    } else {
                        cancelProgressBar();
                    }
                }
            }
        }).start();
    }

    private void syncTotalStep() {
        if(!mWristbandManager.isConnect()) {
            if(D) Log.w(TAG, "syncTotalStep failed, not connect. check update other internet info.");
            showProgressBar(R.string.settings_mydevice_syncing_user_profile);
            if(BmobControlManager.getInstance().syncUserInfo(new UpdateListener() {
                @Override
                public void done(final BmobException e) {
                    if(e == null) {

                    } else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                showToast(e.getMessage());
                            }
                        });
                    }
                    cancelProgressBar();
                }
            })) {

            } else {
                cancelProgressBar();
            }
            return;
        }
        showProgressBar(R.string.settings_mydevice_syncing_user_profile);
        new Thread(new Runnable() {
            @Override
            public void run() {
                if(!mWristbandManager.SetTargetStep()) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showToast(R.string.something_error);
                        }
                    });
                    //mWristbandManager.close();
                } else {
                    if(BmobControlManager.getInstance().syncUserInfo(new UpdateListener() {
                        @Override
                        public void done(final BmobException e) {
                            if(e == null) {

                            } else {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        showToast(e.getMessage());
                                    }
                                });
                            }
                            cancelProgressBar();
                        }
                    })) {

                    } else {
                        cancelProgressBar();
                    }
                }
            }
        }).start();
    }
    private void showSetInfoDialog(int type, int def){
        final FragmentManager fm = getFragmentManager();
        // start le scan, with no filter
        final ValuePickerFragment dialog = ValuePickerFragment.getInstance(this);
        Bundle bundle = new Bundle();
        bundle.putInt(ValuePickerFragment.EXTRAS_VALUE_TYPE, type);
        bundle.putInt(ValuePickerFragment.EXTRAS_VALUE_DEFAULT, def);
        dialog.setArguments(bundle);

        dialog.show(fm, "alarm_fragment");
    }

    /**
     * Called when Select File was pressed
     */
    public void getImageFromGallery() {
        final Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");// select image file
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            // file browser has been found on the device
            startActivityForResult(intent, REQUEST_CODE_PICK_IMAGE);
        } else {
            // no file browser, please download one.
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showToast(R.string.file_browser_not_support);
                }
            });
        }
    }

    private final String SAVED_IMAGE_DIR_PATH = Environment.getExternalStorageDirectory() + "/WrisbandDemo/";
    private String capturePath;
    protected void getImageFromCamera() {
        String state = Environment.getExternalStorageState();
        if (state.equals(Environment.MEDIA_MOUNTED)) {
            Intent getImageByCamera = new Intent("android.media.action.IMAGE_CAPTURE");
            capturePath = ImageLoadingUtils.getUniqueImagePath(String.valueOf(System.currentTimeMillis()));
            /*
            String out_file_path = SAVED_IMAGE_DIR_PATH;
            File dir = new File(out_file_path);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            capturePath = SAVED_IMAGE_DIR_PATH + System.currentTimeMillis() + ".jpg";*/
            getImageByCamera.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(capturePath)));
            getImageByCamera.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);
            startActivityForResult(getImageByCamera, REQUEST_CODE_CAPTURE_CAMEIA);
        }
        else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showToast(R.string.settings_personage_make_sure_sdcard);
                }
            });
        }
    }

    private String cropPath;
    private void crop(Uri uri) {
        if(D) Log.d(TAG, "crop, uri.toString(): " + uri.toString());
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setDataAndType(uri, "image/*");
        intent.putExtra("crop", "true");
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        intent.putExtra("outputX", 250);
        intent.putExtra("outputY", 250);

        intent.putExtra("outputFormat", "JPEG");
        intent.putExtra("noFaceDetection", true);
        intent.putExtra("return-data", false);// we only need the uri

        //cropPath = SAVED_IMAGE_DIR_PATH + System.currentTimeMillis() + ".jpg";
        cropPath = ImageLoadingUtils.getUniqueImagePath(String.valueOf(System.currentTimeMillis()));
        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(cropPath)));

        startActivityForResult(intent, REQUEST_CODE_CROP);
    }

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

    private void showProgressBar(final int message) {
        mProgressDialog = ProgressDialog.show(WristbandSettingPersonalActivity.this
                , null
                , getResources().getString(message)
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
            mWristbandManager.close();
            // stop timer
            cancelProgressBar();
        }
    };

    @Override
    protected void onStop() {
        if(D) Log.d(TAG, "onStop()");
        super.onStop();
    }

    @Override
    protected void onResume() {
        if(D) Log.d(TAG, "onResume()");
        super.onResume();

        initialUI();

        BackgroundScanAutoConnected.getInstance().startAutoConnect();
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(D) Log.d(TAG, "onActivityResult, requestCode: " + requestCode + ", resultCode: " + resultCode);
        if(resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CODE_PICK_IMAGE) {
                if(D) Log.d(TAG, "REQUEST_CODE_PICK_IMAGE");
                if (data != null) {
                    Uri uri = data.getData();
                    crop(uri);
                }
            } else if (requestCode == REQUEST_CODE_CAPTURE_CAMEIA) {
                crop(Uri.fromFile(new File(capturePath)));
                //SPWristbandConfigInfo.setAvatarPath(this, capturePath);

            } else if (requestCode == REQUEST_CODE_CROP) {
                updateAvatar(cropPath);
                initialUI();
            }
        }
    }

    private void updateAvatar(String path) {

        SPWristbandConfigInfo.setAvatarPath(this, path);

        showProgressBar(R.string.settings_mydevice_syncing_user_profile);
        if(BmobControlManager.getInstance().syncUserInfoWithImage(new UpdateListener() {
            @Override
            public void done(BmobException e) {
                if(e == null) {

                } else {
                    showToast(e.getMessage());
                    Log.w(TAG, "updateAvatar, e.getMessage(): " + e.getMessage());
                }
                cancelProgressBar();
            }
        })) {

        } else {
            cancelProgressBar();
        }
    }

    private void updateNickName(String name) {

        SPWristbandConfigInfo.setName(this, name);
        showProgressBar(R.string.settings_mydevice_syncing_user_profile);
        if(BmobControlManager.getInstance().syncUserInfo(new UpdateListener() {
            @Override
            public void done(BmobException e) {
                if (e == null) {

                } else {
                    showToast(e.getMessage());
                    Log.w(TAG, "updateNickName, e.getMessage(): " + e.getMessage());
                }
                cancelProgressBar();
            }
        })) {

        } else {
            cancelProgressBar();
        }
    }
}
