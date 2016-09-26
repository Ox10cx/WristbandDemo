package com.realsil.android.wristbanddemo;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.LoaderManager;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelUuid;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.realsil.android.blehub.dfu.BinInputStream;
import com.realsil.android.blehub.dfu.RealsilDfu;
import com.realsil.android.blehub.dfu.RealsilDfuCallback;
import com.realsil.android.wristbanddemo.applicationlayer.ApplicationLayerAlarmPacket;
import com.realsil.android.wristbanddemo.backgroundscan.BackgroundScanAutoConnected;
import com.realsil.android.wristbanddemo.utility.AppHelpFragment;
import com.realsil.android.wristbanddemo.utility.ExtendedBluetoothDevice;
import com.realsil.android.wristbanddemo.utility.GlobalGatt;
import com.realsil.android.wristbanddemo.utility.GlobalGreenDAO;
import com.realsil.android.wristbanddemo.utility.HighLightView;
import com.realsil.android.wristbanddemo.utility.SPWristbandConfigInfo;
import com.realsil.android.wristbanddemo.utility.SpecScanRecord;
import com.realsil.android.wristbanddemo.utility.WristbandManager;
import com.realsil.android.wristbanddemo.utility.WristbandManagerCallback;
import com.realsil.android.wristbanddemo.view.SwipeBackActivity;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

public class WristbandOtaActivity extends SwipeBackActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    // Log
    private final static String TAG = "WristbandOtaActivity";
    private final static boolean D = true;

    private static final String DATA_FILE_PATH = "file_path";
    private static final String DATA_FILE_STREAM = "file_stream";
    private static final String DATA_STATUS = "status";
    private static final String EXTRA_URI = "uri";

    private static final int SELECT_FILE_REQ = 1;
    private static final int REQUEST_ENABLE_BT = 2;

    // Handle msg
    private static final int OTA_GET_TARGET_VERSION_INFO_SUCCESS = 0;
    private static final int OTA_GET_TARGET_VERSION_INFO_FAIL = 1;
    private static final int OTA_GET_FILE_INFO_SUCCESS = 2;
    private static final int OTA_GET_FILE_INFO_FAIL = 3;
    private static final int OTA_CALLBACK_STATE_CHANGE = 4;
    private static final int OTA_CALLBACK_PROCESS_CHANGE = 5;
    private static final int OTA_CALLBACK_SUCCESS = 6;
    private static final int OTA_CALLBACK_ERROR = 7;

    public static final int MSG_ERROR = 20;

    private ProgressDialog mProgressDialog = null;

    private RelativeLayout mrlSelectFile;
    private RelativeLayout mrlUpload;

    private ImageView mivOTABack;

    private TextView mtvSelectFile;
    private TextView mtvUpload;

    private TextView mtvFileName;
    private TextView mtvFileSize;
    private TextView mtvFileVersion;
    private TextView mtvTargetAppVersion;
    private TextView mtvTargetPatchVersion;
    private TextView mtvFileStatus;
    private TextView mtvProgress;
    private TextView mtvUploadingStatus;
    private ProgressBar mProgressBar;

    private WristbandManager mWristbandManager;
    private GlobalGatt mGlobalGatt;

    // dfu object
    private RealsilDfu dfu = null;

    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;

    private String mFilePath;
    private BinInputStream mBinInputStream;
    private Uri mFileStreamUri;
    private int newFwVersion;
    private int oldFwVersion;
    private int newPatchVersion;
    private int oldPatchVersion;

    private String mDeviceName;

    // Read info Lock
    private Object mLock = new Object();
    private final int LOCK_WAIT_TIME = 15000;

    private boolean isInOta;

    private Toast mToast;

    private HighLightView mHighLightView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wristband_ota);

        mWristbandManager = WristbandManager.getInstance();
        mWristbandManager.registerCallback(mWristbandManagerCallback);

        mGlobalGatt = GlobalGatt.getInstance();
        mBluetoothAdapter = mGlobalGatt.getBluetoothAdapter();

        // get the Realsil Dfu proxy
        RealsilDfu.getDfuProxy(this, cb);

        mHighLightView = new HighLightView(this);

        // set UI
        setUI();

        initialStringFormat();

        initialUI();

        isInOta = false;
        // Disallow swipe
        allowDrag(!isInOta);

        // Show Guide window
        showGuide(mrlSelectFile, R.string.dfu_action_select_file);
    }

    private String mFormatConnectDevice;
    private void initialStringFormat() {
        mFormatConnectDevice = getResources().getString(R.string.connect_with_device_name);
    }

    private void initialUI() {
        if(!mWristbandManager.isConnect()) {
            mrlSelectFile.setEnabled(false);
            mrlUpload.setEnabled(false);
        } else {
            mrlSelectFile.setEnabled(true);
            mrlUpload.setEnabled(false);
            if(!BackgroundScanAutoConnected.getInstance().isInLogin()) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        // here we just use to read dfu info, do not enable notification
                        if (!mWristbandManager.readDfuVersion()) {
                            // send msg to update ui
                            Message msg = mHandle.obtainMessage(OTA_GET_TARGET_VERSION_INFO_FAIL);
                            mHandle.sendMessage(msg);
                        }
                    }
                }).start();
            }
        }

        mProgressBar.setVisibility(View.INVISIBLE);
        mtvProgress.setVisibility(View.INVISIBLE);
        mtvUploadingStatus.setVisibility(View.INVISIBLE);
        mtvFileName.setText(null);
        mtvFileSize.setText(null);
        mtvFileVersion.setText(null);
        mtvTargetAppVersion.setText(null);
        mtvTargetPatchVersion.setText(null);
        mtvFileStatus.setText(R.string.dfu_file_status_no_file);
    }

    private void setUI() {
        mivOTABack = (ImageView) findViewById(R.id.ivOTABack);
        mivOTABack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isInOta) {
                    AlertDialog.Builder aa = new AlertDialog.Builder(WristbandOtaActivity.this, AlertDialog.THEME_HOLO_LIGHT);
                    aa.setTitle(R.string.exit_app_title);
                    aa.setMessage(R.string.exit_ota_text);
                    /*aa.setPositiveButton(R.string.sure, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // TODO Auto-generated method stub
                            WristbandOtaActivity.this.finish();
                        }
                    });*/
                    aa.setNegativeButton(R.string.cancel, null);
                    aa.create();
                    aa.show();
                } else {
                    WristbandOtaActivity.this.finish();
                }
            }
        });

        mrlSelectFile = (RelativeLayout) findViewById(R.id.rlSelectFile);
        mrlSelectFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mWristbandManager.isConnect()) {
                    onSelectFileClicked(v);
                } else {
                    showToast(R.string.please_connect_band);
                }
            }
        });

        mrlUpload = (RelativeLayout) findViewById(R.id.rlUpload);
        mrlUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mWristbandManager.isConnect()) {
                    onUploadClicked(v);
                } else {
                    showToast(R.string.please_connect_band);
                }
            }
        });
        mtvSelectFile = (TextView) findViewById(R.id.tvSelectFile);
        mtvUpload = (TextView) findViewById(R.id.tvUpload);
        mtvFileName = (TextView) findViewById(R.id.tvFileName);
        mtvFileSize = (TextView) findViewById(R.id.tvFileSize);
        mtvFileVersion = (TextView) findViewById(R.id.tvFileVersion);
        mtvTargetAppVersion = (TextView) findViewById(R.id.tvTargetAppVersion);
        mtvTargetPatchVersion = (TextView) findViewById(R.id.tvTargetPatchVersion);
        mtvFileStatus = (TextView) findViewById(R.id.tvFileStatus);
        mtvProgress = (TextView) findViewById(R.id.tvProgress);
        mtvUploadingStatus = (TextView) findViewById(R.id.tvUploadingStatus);

        mProgressBar = (ProgressBar) findViewById(R.id.pbUploadProgress);
    }
    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        switch (requestCode) {
            case SELECT_FILE_REQ:
                if (resultCode != RESULT_OK)
                    return;

                // clear previous data
                mFilePath = null;
                mFileStreamUri = null;


                // and read new one
                final Uri uri = data.getData();
            /*
             * The URI returned from application may be in 'file' or 'content' schema.
             * 'File' schema allows us to create a File object and read details from if directly.
             *
             * Data from 'Content' schema must be read by Content Provider. To do that we are using a Loader.
             */
                if (uri.getScheme().equals("file")) {
                    // the direct path to the file has been returned
                    final String path = uri.getPath();
                    // load the file
                    if(LoadFileInfo(path) == true) {
                        // send msg
                        mHandle.sendMessage(mHandle.obtainMessage(OTA_GET_FILE_INFO_SUCCESS));
                    } else {
                        showToast(R.string.dfu_file_status_invalid);
                        // send msg
                        mHandle.sendMessage(mHandle.obtainMessage(OTA_GET_FILE_INFO_FAIL));
                    }

                } else if (uri.getScheme().equals("content")) {
                    // an Uri has been returned
                    mFileStreamUri = uri;
                    // if application returned Uri for streaming, let's us it. Does it works?
                    // FIXME both Uris works with Google Drive app. Why both? What's the difference? How about other apps like DropBox?
                    final Bundle extras = data.getExtras();
                    if (extras != null && extras.containsKey(Intent.EXTRA_STREAM))
                        mFileStreamUri = extras.getParcelable(Intent.EXTRA_STREAM);

                    // file name and size must be obtained from Content Provider
                    final Bundle bundle = new Bundle();
                    bundle.putParcelable(EXTRA_URI, uri);
                    getLoaderManager().restartLoader(0, bundle, this);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    //do nothing
                    Toast.makeText(this, "Bt is enabled!", Toast.LENGTH_LONG).show();
                } else {
                    // User did not enable Bluetooth or an error occured
                    if(D) Log.d(TAG, "BT not enabled");
                    Toast.makeText(this, "Bt is not enabled!", Toast.LENGTH_LONG).show();
                    finish();
                }
                break;
            default:
                break;
        }
    }

    public boolean LoadFileInfo(String path) {
        // check the file path
        if (TextUtils.isEmpty(path) == true) {
            if(D) Log.e("TAG", "the file path string is null");
            return false;
        }

        // check the file type
        if( MimeTypeMap.getFileExtensionFromUrl(path).equalsIgnoreCase("BIN") != true) {
            if(D) Log.e("TAG", "the file type is not right");
            return false;
        }
        //get the new firmware version
        try {
            mBinInputStream = openInputStream(path);
        }catch (final IOException e){
            if(D) Log.e(TAG, "An exception occurred while opening file", e);
            return false;
        }
        newFwVersion = mBinInputStream.binFileVersion();
        if(D) Log.d(TAG, "newFwVersion = " + newFwVersion);
        // close the file
        if(mBinInputStream != null){
            try {
                mBinInputStream.close();
                mBinInputStream = null;
            }catch (IOException e){
                if(D) Log.e(TAG, "error in close file", e);
                return false;
            }
        }
        mFilePath = path;
        return true;
    }

    @Override
    public Loader<Cursor> onCreateLoader(final int id, final Bundle args) {
        final Uri uri = args.getParcelable(EXTRA_URI);
        final String[] projection = new String[]{MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns.SIZE, MediaStore.MediaColumns.DATA};
        return new CursorLoader(this, uri, projection, null, null, null);
    }

    @Override
    public void onLoaderReset(final Loader<Cursor> loader) {
        mtvFileName.setText(null);
        mtvFileSize.setText(null);
        mFilePath = null;
        mFileStreamUri = null;
    }

    @Override
    public void onLoadFinished(final Loader<Cursor> loader, final Cursor data) {
        if (data.moveToNext()) {
            final String fileName = data.getString(0 /* DISPLAY_NAME */);
            final int fileSize = data.getInt(1 /* SIZE */);
            final String filePath = data.getString(2 /* DATA */);
            // load the file
            if(LoadFileInfo(filePath) == true) {
                // send msg
                mHandle.sendMessage(mHandle.obtainMessage(OTA_GET_FILE_INFO_SUCCESS));
            } else {
                showToast(R.string.dfu_file_status_invalid);
                // send msg
                mHandle.sendMessage(mHandle.obtainMessage(OTA_GET_FILE_INFO_FAIL));
            }

        }
    }

    /**
     * Opens the binary input stream from a BIN file. A Path to the BIN file is given.
     *
     * @param filePath the path to the BIN file
     * @return the binary input stream with BIN data
     * @throws IOException
     */
    private BinInputStream openInputStream(final String filePath) throws IOException {
        final InputStream is = new FileInputStream(filePath);
        return new BinInputStream(is);
    }

    /**
     * Called when Select File was pressed
     *
     * @param view a button that was pressed
     */
    public void onSelectFileClicked(final View view) {
        // Clear file info.
        mtvFileName.setText(null);
        mtvFileSize.setText(null);
        mtvFileVersion.setText(null);
        mtvFileStatus.setText(R.string.dfu_file_status_no_file);
        mrlUpload.setEnabled(false);

        final Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("file/*.bin");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            // file browser has been found on the device
            startActivityForResult(intent, SELECT_FILE_REQ);
        } else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showToast(R.string.dfu_alert_no_filebrowser_title);
                }
            });
        }
    }

    /**
     * Callback of UPDATE/CANCEL button on FeaturesActivity
     * Button.onClick convert to onUploadClicked
     */
    public void onUploadClicked(final View view) {
        int batteryLevel = mWristbandManager.getBatteryLevel();
        if(batteryLevel >= 0
                && batteryLevel < 60) {
            showToast(String.format(getString(R.string.dfu_battery_not_enough), batteryLevel));
            if(D) Log.e(TAG, "the battery level is too low. batteryLevel: " + batteryLevel);
            return;
        }
        if(dfu == null) {
            showToast(R.string.dfu_not_ready);
            if(D) Log.e(TAG, "the realsil dfu didn't ready");
            return;
        }

        // set the total speed for android 4.4, to escape the internal error
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            dfu.setSpeedControl(true, 1000);
        }
        // Use GlobalGatt do not need to disconnect, just unregister the callback
        mGlobalGatt.unRegisterAllCallback(mWristbandManager.getBluetoothAddress());
        /*
        // disconnect the gatt
        disconnect(mBtGatt);// be care here
        // wait a while for close gatt.
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }*/
        if(D) Log.e(TAG, "Start OTA, address is: " + mWristbandManager.getBluetoothAddress());
        if(mtvFileVersion.getText().toString().trim().equals(mtvTargetAppVersion.getText().toString().trim())
                ||mtvFileVersion.getText().toString().trim().equals(mtvTargetPatchVersion.getText().toString().trim())){
            showToast("当前固件已经处于最新版");
            return;
        }
        if(dfu.start(mWristbandManager.getBluetoothAddress(), mFilePath)){
            showToast(R.string.dfu_start_ota_success_msg);
            if(D) Log.d(TAG, "true");

            isInOta = true;
            // Disallow swipe
            allowDrag(!isInOta);

            showProgressBar();
            //来回移动
            mProgressBar.setIndeterminate(true);
            mrlUpload.setEnabled(false);
            mrlSelectFile.setEnabled(false);
        } else {
            showToast(R.string.dfu_start_ota_fail_msg);
            if(D) Log.e(TAG, "something error in device info or the file, false");
        }


    }

    // escape fast click
    private static long lastClickTime;
    public synchronized static boolean isFastClick() {
        long time = System.currentTimeMillis();
        if ( time - lastClickTime < 500) {
            return true;
        }
        lastClickTime = time;
        return false;
    }

    private void showProgressBar() {
        mProgressBar.setVisibility(View.VISIBLE);
        mtvProgress.setVisibility(View.VISIBLE);
        mtvUploadingStatus.setVisibility(View.VISIBLE);
        mrlSelectFile.setEnabled(false);
        mrlUpload.setEnabled(false);
        mtvProgress.setText(null);
        mtvUploadingStatus.setText(null);
    }

    private void showProgressBar(final int message) {
        mProgressDialog = ProgressDialog.show(WristbandOtaActivity.this
                , null
                , getResources().getString(message)
                , true);
        mProgressDialog.setCancelable(false);

        mProgressBarSuperHandler.postDelayed(mProgressBarSuperTask, 30 * 1000);
    }

    private void showProgressBar(final String message) {
        mProgressDialog = ProgressDialog.show(WristbandOtaActivity.this
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
            mWristbandManager.close();
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

        if(dfu != null) {
            dfu.close();
        }
        mWristbandManager.unRegisterCallback(mWristbandManagerCallback);
        /*
        //unregiste the broadcast Receiver
        if(mBondStateReceiver != null){
            this.unregisterReceiver(mBondStateReceiver);
            if(D) Log.i(TAG, "unregisterReceiver");
        }
        */
    }



    RealsilDfuCallback cb = new RealsilDfuCallback() {
        public void onServiceConnectionStateChange(boolean status, RealsilDfu d) {
            if(D) Log.e(TAG, "status: " + status);
            if(status == true) {
                //Toast.makeText(getApplicationContext(), "DFU Service connected", Toast.LENGTH_SHORT).show();
                dfu = d;
            } else {
                //Toast.makeText(getApplicationContext(), "DFU Service disconnected", Toast.LENGTH_SHORT).show();
                dfu = null;
            }
        }

        public void onError(int e) {
            if(D) Log.e(TAG, "onError: " + e);

            // send msg to update ui
            Message msg = mHandle.obtainMessage(OTA_CALLBACK_ERROR);
            msg.arg1 = e;
            mHandle.sendMessage(msg);
        }

        public void onSucess(int s) {
            if(D) Log.e(TAG, "onSucess: " + s);

            // send msg to update ui
            Message msg = mHandle.obtainMessage(OTA_CALLBACK_SUCCESS);
            msg.arg1 = s;
            mHandle.sendMessage(msg);

        }

        public void onProcessStateChanged(int state) {
            if(D) Log.e(TAG, "onProcessStateChanged: " + state);

            // send msg to update ui
            Message msg = mHandle.obtainMessage(OTA_CALLBACK_STATE_CHANGE);
            msg.arg1 = state;
            mHandle.sendMessage(msg);
        }

        public void onProgressChanged(int progress) {
            if(D) Log.e(TAG, "onProgressChanged: " + progress);

            // send msg to update ui
            Message msg = mHandle.obtainMessage(OTA_CALLBACK_PROCESS_CHANGE);
            msg.arg1 = progress;
            mHandle.sendMessage(msg);
        }
    };
    private void showGuide(View v, int id) {
        String s = getResources().getString(id);
        showGuide(v, s);
    }
    private void showGuide(View v, String s) {
        if(!isFirstLoad()) {
            return;
        }
        final int defaultOrientation = getRequestedOrientation();
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
        mHighLightView.showTipForView(v, s, HighLightView.HIGH_LIGHT_VIEW_TYPE_RECT, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setRequestedOrientation(defaultOrientation);
                v.callOnClick();
            }
        });
    }
    private boolean isFirstLoad() {
        return SPWristbandConfigInfo.getFirstOTAStartFlag(this);
    }

    private Handler mHandle = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            // TODO Auto-generated method stub
            if(D) Log.d(TAG, "MSG No " + msg.what);
            switch (msg.what) {
                //对端APP版本号 对端Patch版本号
                case OTA_GET_TARGET_VERSION_INFO_SUCCESS:
                    mtvTargetAppVersion.setText(String.valueOf(msg.arg1));
                    mtvTargetPatchVersion.setText(String.valueOf(msg.arg2));
                    mWristbandManager.getDeviceName();
                    break;
                case OTA_GET_TARGET_VERSION_INFO_FAIL:
                    //mPatchVersionView.setText(String.valueOf(msg.arg1));
                    showToast(R.string.dfu_start_ota_fail_msg);
                    finish();
                    break;
                case OTA_GET_FILE_INFO_SUCCESS:
                    final File file = new File(mFilePath);
                    mtvFileVersion.setText(String.valueOf(newFwVersion));
                    String fileName = file.getName();
                    if(fileName.length() > 20) {
                        fileName = fileName.substring(0, 20) + "...";
                    }
                    mtvFileName.setText(fileName);
                    mtvFileSize.setText(getString(R.string.dfu_file_size_text, file.length()));
                    mtvFileStatus.setText(R.string.dfu_file_status_ok);
                    mrlUpload.setEnabled(true);
                    mrlUpload.requestFocus();
                    // Show Guide window
                    showGuide(mrlUpload, R.string.dfu_action_upload);
                    SPWristbandConfigInfo.setFirstOtaStartFlag(WristbandOtaActivity.this, false);
                    break;
                case OTA_GET_FILE_INFO_FAIL:
                    mtvFileName.setText("");
                    mtvFileVersion.setText("");
                    mtvFileSize.setText("");
                    mtvFileStatus.setText(R.string.dfu_file_status_invalid);
                    mrlUpload.setEnabled(false);
                    break;
                case OTA_CALLBACK_PROCESS_CHANGE:
                    mProgressBar.setProgress(msg.arg1);
                    mtvProgress.setText(msg.arg1 + "%");
                    break;
                case OTA_CALLBACK_STATE_CHANGE:
                    switch (msg.arg1) {
                        case RealsilDfu.STA_ORIGIN:
                            mtvUploadingStatus.setText(getString(R.string.dfu_status_starting_msg));
                            break;
                        case RealsilDfu.STA_REMOTE_ENTER_OTA:
                            mtvUploadingStatus.setText(getString(R.string.dfu_status_starting_msg));
                            break;
                        case RealsilDfu.STA_FIND_OTA_REMOTE:
                            mtvUploadingStatus.setText(getString(R.string.dfu_status_starting_msg));
                            break;
                        case RealsilDfu.STA_CONNECT_OTA_REMOTE:
                            mtvUploadingStatus.setText(getString(R.string.dfu_status_starting_msg));
                            break;
                        case RealsilDfu.STA_START_OTA_PROCESS:
                            if(mDeviceName == null) {
                                mtvUploadingStatus.setText(getString(R.string.dfu_status_uploading_msg, mWristbandManager.getBluetoothAddress()));
                            } else {
                                mtvUploadingStatus.setText(getString(R.string.dfu_status_uploading_msg, mDeviceName));
                            }
                            break;
                        case RealsilDfu.STA_OTA_UPGRADE_SUCCESS:
                            mtvUploadingStatus.setText(getString(R.string.dfu_status_completed_msg));
                            break;
                        default:
                            break;
                    }
                    break;
                case OTA_CALLBACK_SUCCESS:
                    isInOta = false;
                    // Disallow swipe
                    allowDrag(!isInOta);
                    showToast(R.string.dfu_status_completed_msg);
                    mWristbandManager.close();
                    initialUI();

                    // new method is return to the home page to reconnect the device.
                    Intent intent = new Intent(WristbandOtaActivity.this, WristbandHomeActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    WristbandOtaActivity.this.startActivity(intent);
                    WristbandOtaActivity.this.finish();

                    /*
                    if(!mWristbandManager.isConnect()
                            && SPWristbandConfigInfo.getBondedDevice(WristbandOtaActivity.this) != null) {
                        /*
                        if(mBondStateReceiver == null) {
                            // Broadcast to receive Hid connect message
                            mBondStateReceiver = new BondStateReceiver();
                            IntentFilter filter = new IntentFilter();
                            filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
                            filter.addAction(BluetoothDevice.ACTION_UUID);
                            WristbandOtaActivity.this.registerReceiver(mBondStateReceiver, filter);
                        }
                        */
                    /*
                        scanLeDevice(true);
                    }*/
                    break;
                case OTA_CALLBACK_ERROR:
                    isInOta = false;
                    // Disallow swipe
                    allowDrag(!isInOta);
                    showToast(getString(R.string.dfu_status_error_msg, msg.arg1));
                    mWristbandManager.close();
                    initialUI();

                    Intent intent2 = new Intent(WristbandOtaActivity.this, WristbandHomeActivity.class);
                    WristbandOtaActivity.this.startActivity(intent2);
                    WristbandOtaActivity.this.finish();
                    /*
                    if(!mWristbandManager.isConnect()
                            && SPWristbandConfigInfo.getBondedDevice(WristbandOtaActivity.this) != null) {
                        /*
                        if(mBondStateReceiver == null) {
                            // Broadcast to receive Hid connect message
                            mBondStateReceiver = new BondStateReceiver();
                            IntentFilter filter = new IntentFilter();
                            filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
                            filter.addAction(BluetoothDevice.ACTION_UUID);
                            WristbandOtaActivity.this.registerReceiver(mBondStateReceiver, filter);
                        }
                        */
                    /*
                        scanLeDevice(true);
                    }*/
                    break;
                case MSG_ERROR:
                    showToast(R.string.something_error);

                    cancelProgressBar();

                    mWristbandManager.close();
                    break;
                default:
                    break;
            }

            super.handleMessage(msg);
        }

    };

    // Application Layer callback
    WristbandManagerCallback mWristbandManagerCallback = new WristbandManagerCallback() {
        @Override
        public void onError(final int error) {
            if(D) Log.d(TAG, "onError, error: " + error);
            SendMessage(MSG_ERROR, null, error, -1);
        }
        //读取手环的版本号
        @Override
        public void onVersionRead(int appVersion, int patchVersion) {
            if (D) Log.d(TAG, "onVersionRead");
            // send msg to update ui
            Message msg = mHandle.obtainMessage(OTA_GET_TARGET_VERSION_INFO_SUCCESS);
            msg.arg1 = appVersion;
            msg.arg2 = patchVersion;
            mHandle.sendMessage(msg);
        }
        @Override
        public void onNameRead(final String data) {
            if(D) Log.d(TAG, "onNameRead, name: " + data);
            mDeviceName = data;
        }
    };

    /**
     * send message
     * @param msgType Type message type
     * @param obj object sent with the message set to null if not used
     * @param arg1 parameter sent with the message, set to -1 if not used
     * @param arg2 parameter sent with the message, set to -1 if not used
     **/
    private void SendMessage(int msgType, Object obj, int arg1, int arg2) {
        if(mHandle != null) {
            //	Message msg = new Message();
            Message msg = Message.obtain();
            msg.what = msgType;
            if(arg1 != -1) {
                msg.arg1 = arg1;
            }
            if(arg2 != -1) {
                msg.arg2 = arg2;
            }
            if(null != obj) {
                msg.obj = obj;
            }
            mHandle.sendMessage(msg);
        }
        else {
            if(D) Log.e(TAG,"handler is null, can't send message");
        }
    }

    @Override
    public void onBackPressed() {
        if(isInOta) {
            AlertDialog.Builder aa = new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_LIGHT);
            aa.setTitle(R.string.exit_app_title);
            aa.setMessage(R.string.exit_ota_text);
            /*aa.setPositiveButton(R.string.sure, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // TODO Auto-generated method stub
                    WristbandOtaActivity.this.finish();
                }
            });*/
            aa.setNegativeButton(R.string.cancel, null);
            aa.create();
            aa.show();
        } else {
            WristbandOtaActivity.this.finish();
        }
    }
}
