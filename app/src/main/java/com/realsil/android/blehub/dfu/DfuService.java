/*
 * Copyright (C) 2015 Realsil Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.realsil.android.blehub.dfu;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.UUID;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Log;

import com.realsil.android.wristbanddemo.utility.GlobalGatt;


public class DfuService extends Service {
    private static final String TAG = DfuService.class.getSimpleName();
    private static final boolean DBG = true;
    // OTA Work Mode
    public static final int OTA_MODE_FULL_FUNCTION = 0;
    public static final int OTA_MODE_LIMIT_FUNCTION = 1;
    // normal mode is full function, include enter OTA and scan OTA devices
    private int mOtaWorkMode = OTA_MODE_FULL_FUNCTION;
    
    // speed control
    private SpeedControl mSpeedControl;
    private static final int SPEED_CONTROL_TOTAL_TX_SPEED = 3000;//3KB/s
    private static final int SPEED_CONTROL_MTU_PAYLOAD_SIZE_LIMIT = 20;
    
    // binder object
    private IBinder mBinder;
    
    // use to tell other is in ota process or not
    private boolean isInOtaProcess = false;
    
    // use to signed the callback
    private String mPackageName = ""; 
    // service callback manager
    private RemoteCallbackList<IRealsilDfuCallback> mCallbacks = new RemoteCallbackList<IRealsilDfuCallback>();
    private HashMap<String, IRealsilDfuCallback> mCallbacksMap = new HashMap<String, IRealsilDfuCallback>(); 
    
    /**
     * The number of the last error that has occurred or 0 if there was no error
     */
    private volatile int mErrorState;
    
    // special error this error may stop OTA process, and may send a RESET command to the remote if the remote in OTA model
    public static final int ERROR_MASK = 0x0100;
    // for cann't connect to DFU device
    public static final int ERROR_DEVICE_DISCONNECTED = ERROR_MASK | 0x00;
    // for file io exception
    public static final int ERROR_FILE_IO_EXCEPTION = ERROR_MASK | 0x01;
    // for start gatt discovery failed
    public static final int ERROR_SERVICE_DISCOVERY_NOT_STARTED = ERROR_MASK | 0x02;
    // for object.wait error 
    public static final int ERROR_LOCK_WAIT_ERROR = ERROR_MASK | 0x03;
    // for cannot connect with no callback error
    public static final int ERROR_CANNOT_CONNECT_WITH_NO_CALLBACK_ERROR = ERROR_MASK | 0x04;
    // for send command but no callback, maybe a GKI_getBuffer error, or something else
    public static final int ERROR_CANNOT_SEND_COMMAND_WITH_NO_CALLBACK_ERROR = ERROR_MASK | 0x05;
    // for cannot find the special service, maybe the device is not right
    public static final int ERROR_CANNOT_FIND_SERVICE_ERROR = ERROR_MASK | 0x06;
    // for cannot find the special characteristic, maybe the device is not right
    public static final int ERROR_CANNOT_FIND_CHARAC_ERROR = ERROR_MASK | 0x07;
    // for connect with null return
    public static final int ERROR_CONNECT_ERROR = ERROR_MASK | 0x08;
    // for cannot find the device enter ota model
    public static final int ERROR_CANNOT_FIND_DEVICE_ERROR = ERROR_MASK | 0x09;
    // for cannot enable the remote notification
    public static final int ERROR_WRITE_CHARAC_NOTIFY_ERROR = ERROR_MASK | 0x0A;
    // for write characteristic failed
    public static final int ERROR_WRITE_CHARAC_ERROR = ERROR_MASK | 0x0B;
    // for send command reach the max try time
    public static final int ERROR_SEND_COMMAND_WITH_MAX_TRY_TIME_ERROR = ERROR_MASK | 0x0C;
    // for battery check
    public static final int ERROR_LOW_POWER_ERROR = ERROR_MASK | 0x0D;

    
    // remote error this error may stop OTA process, and will send a RESET command to the remote
    // the low byte is the error reason in the notification
    public static final int ERROR_REMOTE_MASK = 0x0200;
    // for no notification come
    public static final int ERROR_NO_NOTIFICATION_COME_ERROR = ERROR_REMOTE_MASK | 0xFF;
    
    // bluedroid error this error may stop OTA process, and will send a RESET command to the remote
    // the low byte is the error reason back from bluedroid
    public static final int ERROR_BLUEDROID_MASK = 0x0400;
    
    // connection error this error may try to reconnect two times when disconnect in OTA model
    // the low byte is the error reason back from bluedroid
    public static final int ERROR_CONNECTION_MASK = 0x0800;
    

    // le scan flag and time
    private volatile boolean mScanning;
	private static final long SCAN_PERIOD = 10000;
	private static final String EXTRA_DEVICE_NAME = "BeeTgt";  // the name of the device enter OTA model
    
	/**
     * The OTA process state
     */
    private volatile int mProcessState;
    // Dfu process state
    public static final int PROCESS_STATE_MASK = 0x0100;
    // process state
 	public static final int STA_ORIGIN = PROCESS_STATE_MASK | 0x01; // initial state
 	public static final int STA_REMOTE_ENTER_OTA = PROCESS_STATE_MASK | 0x02;
 	public static final int STA_FIND_OTA_REMOTE = PROCESS_STATE_MASK | 0x03;
 	public static final int STA_CONNECT_OTA_REMOTE = PROCESS_STATE_MASK | 0x04;
 	public static final int STA_START_OTA_PROCESS = PROCESS_STATE_MASK | 0x05;
 	public static final int STA_OTA_UPGRADE_SUCCESS = PROCESS_STATE_MASK | 0x06;
    
    /* Handle Message */
    private static final int START_OTA_PROCESS = 1;
    private static final int PROCESS_STATE_BROADCAST = 2;
    private static final int ERROR_BROADCAST = 3;
    private static final int SUCCESS_BROADCAST = 4;
    private static final int PROGRESS_BROADCAST = 5;
    
  	// for no notification come flag
    public volatile boolean isNotificationCome = false;
   	// for scan the ota device flag
    public volatile boolean isScanTheDevice = false;
    // for cannot disconnect error flag
    public volatile boolean isDisconnectOK = false;
    // for connect with no callback error flag
    public volatile boolean isConnectedCallbackCome = false;
    // for notification set flag
    private volatile boolean isNotificationsSet = false;
    
    // for write Characteristic
    private volatile boolean mWriteCharacteristicSuccess; // write gatt success or not
    private volatile boolean mOnCharacteristicWriteCalled;// onCharacteristicWrite() funtion called or not

    private volatile boolean mOnCharacteristicReadCalled;// onCharacteristicRead() funtion called or not
    private volatile boolean lastPacketTransferred;        // whether the last packet of the image is transferred
    private static final int MAX_RESEND_TIME = 3;
    
    // for battery check
    private final static int MIN_POWER_LEVER = 60;
    private final static int MAX_POWER_LEVER = 110;
    private final static int MIN_POWER_LEVER_FOR_HUAWEI = 140;//140*2=280, 2.8V
    
    // storage the receive read infomation
    private volatile byte[] mReceivedReadData = null;
    
    /*
     * Bluetooth manager
     * */
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mBluetoothGatt;
    
    /*
     * The device info in the normal model
     * */
    private String mDeviceAddress;
    private String mDeviceName;
    
    // The device info in the OTA model
    private String mOtaDeviceName;   
	private String mOtaDeviceAddress;
	private String mOtaFilePath;
	
    /**
     * Lock used in synchronization purposes
     */
    private final Object mLock        = new Object();
    private final Object mLeScanLock  = new Object(); //used only for le scan
    private final Object mCharacteristicWriteCalledLock = new Object(); //used only for CharacteristicWriteCalled funtion called or not lock
    private final Object mCharacteristicReadCalledLock = new Object(); //used only for CharacteristicWriteCalled funtion called or not lock
    private static final int MAX_CONNECTION_LOCK_WAIT_TIME = 10000;
    private static final int MAX_NOTIFICATION_LOCK_WAIT_TIME = 3000;
    private static final int MAX_CALLBACK_LOCK_WAIT_TIME = 5000;
    /**
     * The current connection state. If its value is > 0 than an error has occurred. Error number is a negative value of mConnectionState
     */
    private int mConnectionState;
    private final static int STATE_DISCONNECTED = 0;
    private final static int STATE_CONNECTING = -1;
    private final static int STATE_CONNECTED = -2;
    private final static int STATE_CONNECTED_AND_READY = -3; // indicates that services were discovered
    private final static int STATE_DISCONNECTING = -4;
    private final static int STATE_CLOSED = -5;

    /**
     * Use for bin file up to 100K
     */
    private final static int BIG_IMAGE_LIMIT_IMAGE_SIZE = 100 * 1024;
    private final static int BIG_IMAGE_START_OFFSET_OF_BIG_IMAGE = 140 * 1024;
    private final static int BIG_IMAGE_SPECIAL_POINT = 104000;
    private final static int BIG_IMAGE_HEADER_LENGTH = 12;
    
    /**
     * The maximum number of bytes in one packet.
     */
    private final static int MAX_PACKET_SIZE = 20;

    private byte[] mBuffer = new byte[MAX_PACKET_SIZE];
    private BinInputStream mBinInputStream;
    private int mImageSizeInBytes;
    private int mImageSizeInPackets;
    private int mImageVersion;
    private int mBytesSent;
    private int mBytesConfirmed;
    
    // Version Check control
    private boolean haveVersionCheck = false;
    
    private short mOriginalVersion;
    private int mImageUpdateOffset;

    // storage the receive notification infomation
    private volatile byte[] mReceivedData = null;
    
    /*
     * OTA command control
     * */
    private static final byte OPCODE_DFU_START_DFU = 0x01;
    private static final byte OPCODE_DFU_RECEIVE_FW_IMAGE = 0x02;
    private static final byte OPCODE_DFU_VALIDATE_FW_IMAGE = 0x03;
    private static final byte OPCODE_DFU_ACTIVE_IMAGE_RESET = 0x04;
    private static final byte OPCODE_DFU_RESET = 0x05;
    private static final byte OPCODE_DFU_REPORT_RECEIVED_IMAGE_INFO = 0x06;
    private static final byte OPCODE_DFU_CONNECTION_PARAMETER_UPDATE = 0x07;

    private static byte[] OPCODE_DFU_START_DFU_STR = new byte[17];
    private static byte[] OPCODE_DFU_RECEIVE_FW_IMAGE_STR = new byte[7];
    private static byte[] OPCODE_DFU_VALIDATE_FW_IMAGE_STR = new byte[3];
    private static byte[] OPCODE_DFU_ACTIVE_IMAGE_RESET_STR = new byte[1];
    private static byte[] OPCODE_DFU_RESET_STR = new byte[1];
    private static byte[] OPCODE_DFU_REPORT_RECEIVED_IMAGE_INFO_STR = new byte[3];
    private static byte[] OPCODE_DFU_CONNECTION_PARAMETER_UPDATE_STR = new byte[9];
    
    // OTA RESET CMD
    private static final byte[] OPCODE_OTA_ENTER_OTA_MODEL = new byte[] {0x01};
    
    private static int CONNECTION_PARAMETER_CONN_INTERVAL_MIN = 0x0006;
    private static int CONNECTION_PARAMETER_CONN_INTERVAL_MAX = 0x0011;
    private static int CONNECTION_PARAMETER_SLAVE_LATENCY = 0x0000;
    private static int CONNECTION_PARAMETER_SUPPERVISION_TIMEOUT = 500;
    /*
     * Notification reason type
     * */
    public static final byte DFU_STATUS_SUCCESS = 0x01;
    public static final byte DFU_STATUS_NOT_SUPPORTED = 0x02;
    public static final byte DFU_STATUS_INVALID_PARAM = 0x03;
    public static final byte DFU_STATUS_OPERATION_FAILED = 0x04;
    public static final byte DFU_STATUS_DATA_SIZE_EXCEEDS_LIMIT = 0x05;
    public static final byte DFU_STATUS_CRC_ERROR = 0x06;
    
    /*
     * UUID control
     */
    public static final UUID DFU_SERVICE_UUID = UUID.fromString("00006287-3c17-d293-8e48-14fe2e4da212");
    public static final UUID DFU_DATA_UUID = UUID.fromString("00006387-3c17-d293-8e48-14fe2e4da212");
    public static final UUID DFU_CONTROL_POINT_UUID = UUID.fromString("00006487-3c17-d293-8e48-14fe2e4da212");
    public static final UUID CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private static final UUID[] serviceUuids = {DFU_SERVICE_UUID};
    
    private BluetoothGattCharacteristic mControlPointCharacteristic;
    private BluetoothGattCharacteristic mDataCharacteristic;
    private BluetoothGattDescriptor mNotifyDescriptor;
    
    private static final UUID OTA_SERVICE= UUID.fromString("0000ffd0-0000-1000-8000-00805f9b34fb");
    private static final UUID OTA_CHARA = UUID.fromString("0000ffd1-0000-1000-8000-00805f9b34fb");
    // modify for new spec.
    private static final UUID NEW_OTA_SERVICE= UUID.fromString("0000d0ff-3c17-d293-8e48-14fe2e4da212");
    
    private static final UUID BATTERY_SERVICE= UUID.fromString("0000180F-0000-1000-8000-00805f9b34fb");
    private static final UUID BATTERY_READ_CHARA = UUID.fromString("00002A19-0000-1000-8000-00805f9b34fb");
    
    
    private BluetoothGattCharacteristic mOtaResetCharacteristic;
    private BluetoothGattCharacteristic mBluetoothBatteryReadCharacteristic;
    
    // Thread
    private ThreadOta mThreadOta;

    // Global Gatt
    private GlobalGatt mGlobalGatt;

    // AES control
    private boolean haveAES = true;
    //AES JNI native interface
    static {
        System.loadLibrary("AesJni");
    }
    public native boolean aesInit( int aes_mode, byte[] key  );
    public native void aes_encrypt( byte[] input, byte[] output );
    private byte[] mSecretkey = new byte[] {
        (byte)0x4E, (byte)0x46, (byte)0xF8, (byte)0xC5, (byte)0x09, (byte)0x2B, (byte)0x29, (byte)0xE2,
        (byte)0x9A, (byte)0x97, (byte)0x1A, (byte)0x0C, (byte)0xD1, (byte)0xF6, (byte)0x10, (byte)0xFB,
        (byte)0x1F, (byte)0x67, (byte)0x63, (byte)0xDF, (byte)0x80, (byte)0x7A, (byte)0x7E, (byte)0x70,
        (byte)0x96, (byte)0x0D, (byte)0x4C, (byte)0xD3, (byte)0x11, (byte)0x8E, (byte)0x60, (byte)0x1A
    };
    
    @Override
    public void onCreate() {
        super.onCreate();
        if(DBG) Log.d(TAG, "onCreate");
        // Speed Control initial.
        mSpeedControl = new SpeedControl(SPEED_CONTROL_MTU_PAYLOAD_SIZE_LIMIT, SPEED_CONTROL_TOTAL_TX_SPEED, false);
        mBinder = new RealsilDfuBinder(this);
        // Use Global Gatt
        mGlobalGatt = GlobalGatt.getInstance();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(DBG) Log.d(TAG, "onDestroy()");
        
        try {
            if (mBinInputStream != null) {
                mBinInputStream.close();
            }
        } catch (IOException e1) {
        	if(DBG) Log.i(TAG, "close mBinInputStream fail");
        }
    }
    
    @Override
	public IBinder onBind(Intent intent) {
        if(DBG) Log.d(TAG, "onBind");
        return mBinder;
    }
    
    @Override
    public boolean onUnbind(Intent intent) {
        if(DBG) Log.d(TAG, "onUnbind");
        return super.onUnbind(intent);
    }
    
    //Binder object: Must be static class or memory leak may occur 
    private class RealsilDfuBinder extends IRealsilDfu.Stub implements IBinder {
        private DfuService mService;

        private DfuService getService() {
            
            if (mService != null) {
                return mService;
            }
            return null;
        }

        RealsilDfuBinder(DfuService svc) {
            mService = svc;
        }

        public boolean cleanup()  {
            mService = null;
            return true;
        }

		@Override
		public boolean start(String packageName, String addr, String path) {
			DfuService service = getService();
            if (service == null) return false;
            return service.start(packageName, addr, path);
		}

		@Override
		public boolean isWorking() {
			DfuService service = getService();
            if (service == null) return false;
            return service.isWorking();
		}
		
		@Override
		public int getWorkMode() {
            return mOtaWorkMode;
		}
		
		@Override
		public boolean setWorkMode(int mode) {
			if(isWorking() == true) {
				if(DBG) Log.e(TAG, "set work mode error, is in OTA right now, return");
				return false;
			}
			if(mode != OTA_MODE_FULL_FUNCTION && mode != OTA_MODE_LIMIT_FUNCTION) {
				if(DBG) Log.e(TAG, "set work mode error, unknown work mode type, return");
				return false;
			}
			mOtaWorkMode = mode;
			return true;
		}

		@Override
		public void registerCallback(String packageName, IRealsilDfuCallback cb)
				throws RemoteException {
			// TODO Auto-generated method stub
			if (cb != null) {   
                mCallbacks.register(cb);
                mCallbacksMap.put(packageName, cb); 
			}
		}

		@Override
		public void unregisterCallback(String packageName, IRealsilDfuCallback cb)
				throws RemoteException {
			// TODO Auto-generated method stub
			if (cb != null) {
                mCallbacks.unregister(cb);
                mCallbacksMap.remove(packageName); 
			}
		}

		@Override
		public int getCurrentOtaState() throws RemoteException {
			// TODO Auto-generated method stub
			return mProcessState;
		}

		@Override
		public boolean setSecretKey(byte[] key) throws RemoteException {
			if(isWorking() == true) {
				if(DBG) Log.e(TAG, "set secret key error, is in OTA right now, return");
				return false;
			}
			if(key.length != 32) {
				if(DBG) Log.e(TAG, "set secret key error, the secret key length is not right, current length is: " + key.length + "want length is: 32");
				return false;
			}
			System.arraycopy(key, 0, mSecretkey, 0, mSecretkey.length);
			if(DBG) Log.i(TAG, "set secret success, the new key is: " + Arrays.toString(mSecretkey));
			return true;
		}

		@Override
		public boolean setVersionCheck(boolean enable) throws RemoteException {
			// TODO Auto-generated method stub
			if(isWorking() == true) {
				if(DBG) Log.e(TAG, "set version check error, is in OTA right now, return");
				return false;
			}
			
			haveVersionCheck = enable;
			return true;
		}

		@Override
		public boolean setSpeedControl(boolean en, int speed)
				throws RemoteException {
			if(isWorking() == true) {
				if(DBG) Log.e(TAG, "set speed control error, is in OTA right now, return");
				return false;
			}
			mSpeedControl.SetSpeedControlMode(en);
			mSpeedControl.SetTotalSpeed(speed);
			return true;
		}

    };
    
    
    public boolean start(String packageName, String addr, String path) {
    	if(DBG) Log.d(TAG, "start");
    	if(isInOtaProcess == true) {
    		if(DBG) Log.e(TAG, "is in OTA right now, return");
    		return false;
    	}
    	
    	if(initialize() != true) {
    		if(DBG) Log.e(TAG, "initial failed");
    		return false;
    	}
    	
    	if(packageName == null) {
    		if(DBG) Log.e(TAG, "the packageName is null");
    		return false;
    	} else {
    		mPackageName = packageName;
    		IRealsilDfuCallback cb = mCallbacksMap.get(mPackageName);
    		if(cb == null) {
    			if(DBG) Log.e(TAG, "didn't find the special callback in the service");
    			return false;
    		}
    	}
    	
    	// check the addr
    	if(BluetoothAdapter.checkBluetoothAddress(addr) != true) {
    		if(DBG) Log.e(TAG, "the address format isn't right, address: " + addr);
    		return false;
    	}
    	// check the path
    	if(fileIsExists(path) != true) {
    		if(DBG) Log.e(TAG, "the ota file didn't find, path: " + path);
    		return false;
    	}
    	
    	// check the file.
        if(checkAndPrepareTheFile(path) != true) {
        	if(DBG) Log.e(TAG, "the ota file didn't right");
    		return false;
        }
    	
        if(!aesInit(3, mSecretkey)) {//AES-256
    		if(DBG) Log.e(TAG, "encrpt initial error, the encrypted key is: " + Arrays.toString(mSecretkey) + "length is: "+ mSecretkey.length);
    		return false;
        } else {
        	if(DBG) Log.d(TAG, "aes init success with key: " + Arrays.toString(mSecretkey) + "length is: "+ mSecretkey.length);
        }
        //aesInit(3);//AES-256
        
        
        if(DBG) Log.d(TAG,"enterOtaModel()-> begin to enter ota model...");
    	// save the normal device info and the ota path file
    	mDeviceAddress = addr;
    	mOtaFilePath = path;
    	
    	// update the process state
        sendProcessStateBroadcast(STA_ORIGIN);
        
    	// send msg to start OTA
    	mHandle.sendMessage(mHandle.obtainMessage(START_OTA_PROCESS));
    	
    	isInOtaProcess = true;
    	return true;
    }
    
    public boolean isWorking() {
    	if(DBG) Log.d(TAG, "isDfuWorking");
    	return isInOtaProcess;
    }
    
    // check the file exist or not
    private boolean fileIsExists(String file){
        try{
            File f=new File(file);
            if(!f.exists()){
            	return false;
            }
        }catch (Exception e) {
            // TODO: handle exception
            return false;
        }
        return true;
    }
    
    private void prepareOTAProcess() throws DfuException {
    	if(DBG) Log.d(TAG, "prepareOTAProcess");
    	
    	// TODO STEP 1:
        // let the remote device enter OTA model
        // update the process state
        sendProcessStateBroadcast(STA_REMOTE_ENTER_OTA);
		
        //connect the remote device
        if(DBG) Log.d(TAG, "start to connect the RCU which going to upgrade");
        connect(mDeviceAddress);
        if(DBG) Log.d(TAG, "connected the RCU which going to upgrade");
        /*
        // Need Power check
        if(DBG) Log.d(TAG, "start to read remote power.");
        readCharac(mBluetoothGatt, mBluetoothBatteryReadCharacteristic);
        if(DBG) Log.d(TAG, "remote power check ok, current power is.");
        if(DBG) Log.d(TAG, "Reading battery power");
     	// wait for RCU send notification
        int battery = mReceivedReadData[0] & 0xff;
        if(mReceivedReadData == null) {
        	if(DBG) Log.e(TAG, "Get battery info failed, do nothing.");
        } else if(battery < MIN_POWER_LEVER) {
        	if(DBG) Log.e(TAG, "Remote battery error, battery: " + battery);
            throw new DfuException("Remote battery error", ERROR_LOW_POWER_ERROR);
        } else if(battery > MAX_POWER_LEVER && battery < MIN_POWER_LEVER_FOR_HUAWEI) {// Only for huawei.
        	if(DBG) Log.e(TAG, "Remote battery error(huawei), battery: " + battery);
            throw new DfuException("Remote battery error(huawei)", ERROR_LOW_POWER_ERROR);
        } else {
        	if(DBG) Log.i(TAG, "Current battery: " + battery);
        }*/
        
        // write RESET command to remote
        if(DBG) Log.d(TAG, "start to write RESET command to the RCU which going to upgrade");
        try {
            writeCharac(mBluetoothGatt, mOtaResetCharacteristic, OPCODE_OTA_ENTER_OTA_MODEL);
		} catch (final DfuException ee) {
        	if(DBG) Log.e(TAG, "Send the enter OTA mode command have some error, ignore it, error code is: " + ee.getErrorNumber());
        }
        if(DBG) Log.d(TAG, "write RESET command success");
        
        // delay 1s make sure RCU enter the OTA mode, then start le scan
        try {
        	if(DBG) Log.d(TAG, "delay 1s make sure RCU enter the OTA mode, then start le scan");
			Thread.sleep(1000);
			if(DBG) Log.d(TAG, "delay 1s reached");
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        // delay for remote connection super timeout reach.
        if(mConnectionState != STATE_DISCONNECTED) {
            if (DBG) Log.d(TAG, "delay for remote connection super timeout reach.");
            waitUntilDisconnected();
            if (DBG) Log.d(TAG, "delay for remote disconnect time reached");
        }

        // close the gatt
        closeGatt(mBluetoothGatt);
        
        // TODO STEP 2:
        // find the remote device which enter OTA model
        // update the process state
        sendProcessStateBroadcast(STA_FIND_OTA_REMOTE);
        // find the device enter the ota model
        if(DBG) Log.d(TAG, "start to find the device enter the ota model");
        scanTheOtaDevice();
        if(DBG) Log.d(TAG, "find the device success");
        
        
    }
    
    private void connectOTAProcess() throws DfuException {
    	if(DBG) Log.d(TAG, "connectOTAProcess");
    	
    	// TODO STEP 3:
        // connect to the device in OTA model
        // update the process state
        sendProcessStateBroadcast(STA_CONNECT_OTA_REMOTE);
        
        // sometimes connect will failed at first, so we try 3 times
        int tryConnectTime = 0;
        // initial the connection state
        mConnectionState = STATE_DISCONNECTED;
        if(DBG) Log.d(TAG, "start OTA upgrade: deviceAddress = " + mOtaDeviceAddress +", deviceName = " + mOtaDeviceName
                +", filePath = " + mOtaFilePath);
        
        // Let's connect to the device
        if(DBG) Log.d(TAG, "Connecting to DFU target...");
        
        boolean con = false;
        // try to connect five time
        do{
        	try {
        		if(DBG) Log.d(TAG, "start to connect the device in OTA model");
    	        connect(mOtaDeviceAddress);
    	        con = true;
    	        if(DBG) Log.d(TAG, "connected the device in OTA model");
        	} catch (DfuException e1) {
        		// if connect fail with GATT_ERROR, do no need disconnect
	            if((mErrorState & ~ERROR_CONNECTION_MASK) != 0x85) {
	            	disconnect(mBluetoothGatt);
	            } else {
	            	if(DBG) Log.d(TAG, "connect fail with GATT_ERROR, do not need disconnect");
	            }
	            // Close the gatt
	            closeGatt(mBluetoothGatt);
	            try {
	                Thread.sleep(2000);
	            } catch (InterruptedException e) {
	                e.printStackTrace();
	            }
	            tryConnectTime++;
        	}
        } while(tryConnectTime < 3 && con == false);
        
        if (con == false) { // error occurred
        	if(DBG) Log.e(TAG, "An error occurred while connecting to the device, report error!!!");
            throw new DfuException("Unable to connect the device", ERROR_DEVICE_DISCONNECTED);
        }
    }
    
    private void startOTAProcess() throws DfuException {
        if(DBG) Log.d(TAG, "startOTAProcess");
        
        // TODO STEP 4:
        // start OTA process
        // update the process state
        sendProcessStateBroadcast(STA_START_OTA_PROCESS);
        
        // TODO STEP 4.1: enable notifications
        if(DBG) Log.d(TAG, "start enable notification");
        setCharacteristicNotification(mBluetoothGatt, mControlPointCharacteristic, true);
        if(DBG) Log.d(TAG, "notification enabled");
        
        // set up the temporary variable that will hold the responses
        byte[] response;
        byte status;
        
        // TODO STEP 4.1: try to update connection parameter
        if(DBG) Log.d(TAG, "Sending OPCODE_DFU_CONNECTION_PARAMETER_UPDATE command (OpCode = 0x07)");
        OPCODE_DFU_CONNECTION_PARAMETER_UPDATE_STR[0] = OPCODE_DFU_CONNECTION_PARAMETER_UPDATE;
        OPCODE_DFU_CONNECTION_PARAMETER_UPDATE_STR[1] = (byte)(CONNECTION_PARAMETER_CONN_INTERVAL_MIN & 0xff);
        OPCODE_DFU_CONNECTION_PARAMETER_UPDATE_STR[2] = (byte)((CONNECTION_PARAMETER_CONN_INTERVAL_MIN >> 8) & 0xff);
        OPCODE_DFU_CONNECTION_PARAMETER_UPDATE_STR[3] = (byte)(CONNECTION_PARAMETER_CONN_INTERVAL_MAX & 0xff);
        OPCODE_DFU_CONNECTION_PARAMETER_UPDATE_STR[4] = (byte)((CONNECTION_PARAMETER_CONN_INTERVAL_MAX >> 8) & 0xff);
        OPCODE_DFU_CONNECTION_PARAMETER_UPDATE_STR[5] = (byte)(CONNECTION_PARAMETER_SLAVE_LATENCY & 0xff);
        OPCODE_DFU_CONNECTION_PARAMETER_UPDATE_STR[6] = (byte)((CONNECTION_PARAMETER_SLAVE_LATENCY >> 8) & 0xff);
        OPCODE_DFU_CONNECTION_PARAMETER_UPDATE_STR[7] = (byte)(CONNECTION_PARAMETER_SUPPERVISION_TIMEOUT & 0xff);
        OPCODE_DFU_CONNECTION_PARAMETER_UPDATE_STR[8] = (byte)((CONNECTION_PARAMETER_SUPPERVISION_TIMEOUT >> 8) & 0xff);
        writeCharac(mBluetoothGatt, mControlPointCharacteristic, OPCODE_DFU_CONNECTION_PARAMETER_UPDATE_STR);
        
        // TODO STEP 4.2: get target image info
        // send Start OPCODE_DFU_REPORT_RECEIVED_IMAGE_INFO command to Control Point
        if(DBG) Log.d(TAG, "Sending OPCODE_DFU_REPORT_RECEIVED_IMAGE_INFO command (OpCode = 0x06)");
        OPCODE_DFU_REPORT_RECEIVED_IMAGE_INFO_STR[0] = OPCODE_DFU_REPORT_RECEIVED_IMAGE_INFO;
        OPCODE_DFU_REPORT_RECEIVED_IMAGE_INFO_STR[1] = (byte)(mBinInputStream.binFileSignature() & 0xff);
        OPCODE_DFU_REPORT_RECEIVED_IMAGE_INFO_STR[2] = (byte)((mBinInputStream.binFileSignature() >> 8) & 0xff);
        writeCharac(mBluetoothGatt, mControlPointCharacteristic, OPCODE_DFU_REPORT_RECEIVED_IMAGE_INFO_STR);
        if(DBG) Log.d(TAG, "Reading OPCODE_DFU_REPORT_RECEIVED_IMAGE_INFO notification");
     	// wait for RCU send notification
        response = readNotificationResponse();
        status = response[2];
        if (status == DFU_STATUS_SUCCESS) {
            ByteBuffer wrapped = ByteBuffer.wrap(response);
            wrapped.order(ByteOrder.LITTLE_ENDIAN);
            mOriginalVersion = wrapped.getShort(3);
            mImageUpdateOffset = wrapped.getInt(5);
        } else {
        	if(DBG) Log.e(TAG, "Get target image info failed, status: " + status);
            throw new DfuException("Get target image info failed", ERROR_REMOTE_MASK | status);
        }
        
        // TODO STEP 4.3: option version check
        if((mOriginalVersion >= mImageVersion) && (haveVersionCheck == true)) {
        	if(DBG) Log.e(TAG, "the remote image version is big then image file, didn't need to update, mImageVersion: "
        	 				+ mImageVersion + " mOriginalVersion: " + mOriginalVersion);
        	if(DBG) Log.e(TAG, "RemoteDfuException -> Sending Reset command (Op Code = 0x05)");
			OPCODE_DFU_RESET_STR[0] = OPCODE_DFU_RESET;
			try {
                writeCharac(mBluetoothGatt, mControlPointCharacteristic, OPCODE_DFU_RESET_STR);
			} catch (final DfuException ee) {
	        	if(DBG) Log.e(TAG, "Send the reset command have some error, ignore it, error code is: " + ee.getErrorNumber());
	        }
			waitUntilDisconnected();
			
            sendSuccessBroadcast(0);
            return;
        }
        
        // TODO STEP 4.4: 
        // send Start OPCODE_DFU_START_DFU command to Control Point
        // opcode, signature
        if(DBG) Log.d(TAG, "Sending OPCODE_DFU_START_DFU command (OpCode = 0x01)");
        if(DBG) Log.d(TAG, "mImageUpdateOffset = " + mImageUpdateOffset);
        if (mImageUpdateOffset == 0) {
            OPCODE_DFU_START_DFU_STR[0] = OPCODE_DFU_START_DFU; // uint8 opCode
            OPCODE_DFU_START_DFU_STR[1] = (byte)(mBinInputStream.binFileOffset() & 0xff); // uint16 offset low byte
            OPCODE_DFU_START_DFU_STR[2] = (byte)((mBinInputStream.binFileOffset() >> 8) & 0xff); // uint16 offset high byte
            OPCODE_DFU_START_DFU_STR[3] = (byte)(mBinInputStream.binFileSignature() & 0xff); // uint16 signature low byte
            OPCODE_DFU_START_DFU_STR[4] = (byte)((mBinInputStream.binFileSignature() >> 8) & 0xff); // uint16 signature high byte
            OPCODE_DFU_START_DFU_STR[5] = (byte)(mBinInputStream.binFileVersion() &  0xff); // uint16 version low byte
            OPCODE_DFU_START_DFU_STR[6] = (byte)((mBinInputStream.binFileVersion() >> 8) & 0xff); // uint16 version high byte
            OPCODE_DFU_START_DFU_STR[7] = (byte)(mBinInputStream.binFileChecksum() & 0xff); // uint16 checksum low byte
            OPCODE_DFU_START_DFU_STR[8] = (byte)((mBinInputStream.binFileChecksum() >> 8) & 0xff); // uint16 checksum high byte
            OPCODE_DFU_START_DFU_STR[9] = (byte)(mBinInputStream.binFileLength() & 0xff); // uint16 length low byte
            OPCODE_DFU_START_DFU_STR[10] = (byte)((mBinInputStream.binFileLength() >> 8) & 0xff); // uint16 length high byte
            OPCODE_DFU_START_DFU_STR[11] = mBinInputStream.binFileOtaFlag(); // uint8 ota_flag
            OPCODE_DFU_START_DFU_STR[12] = mBinInputStream.binFileReserved8(); // uint8 reserved_8
            OPCODE_DFU_START_DFU_STR[13] = (byte)0x00;//add 4 bytes 0x00 for aes
            OPCODE_DFU_START_DFU_STR[14] = (byte)0x00;
            OPCODE_DFU_START_DFU_STR[15] = (byte)0x00;
            OPCODE_DFU_START_DFU_STR[16] = (byte)0x00;

            if(haveAES) {
                //AES Encryption
                byte[] beforeEncryptBytes = new byte[16];
                byte[] afterEncryptBytes = new byte[16];
                int i = 0;
                //System.arraycopy();
                for (i = 0; i < 16; i++) {
                    beforeEncryptBytes[i] = OPCODE_DFU_START_DFU_STR[i + 1];
                }
                if(DBG) Log.i(TAG, "The original data is: " + Arrays.toString(beforeEncryptBytes));
                
                aes_encrypt(beforeEncryptBytes, afterEncryptBytes);
                if(DBG) Log.i(TAG, "The encrypted data is: " + Arrays.toString(afterEncryptBytes));
                
                for (i = 0; i < 16; i++) {
                    OPCODE_DFU_START_DFU_STR[i + 1] = afterEncryptBytes[i];
                }
            }

            writeCharac(mBluetoothGatt, mControlPointCharacteristic, OPCODE_DFU_START_DFU_STR);
            if(DBG) Log.d(TAG, "Reading OPCODE_DFU_START_DFU notification");
            response = readNotificationResponse();
            status = response[2];
            if (status != DFU_STATUS_SUCCESS) {
                throw new DfuException("Starting DFU failed", ERROR_REMOTE_MASK | status);
            }
        }
        
        // TODO STEP 4.5: DFUPushImageToTarget
        // send Start OPCODE_DFU_RECEIVE_FW_IMAGE command to Control Point
        // opcode, signature, offset
        // writeImageSize
        if(DBG) Log.d(TAG, "Sending OPCODE_DFU_RECEIVE_FW_IMAGE command (OpCode = 0x02)");
        OPCODE_DFU_RECEIVE_FW_IMAGE_STR[0] = OPCODE_DFU_RECEIVE_FW_IMAGE;
        OPCODE_DFU_RECEIVE_FW_IMAGE_STR[1] = (byte)(mBinInputStream.binFileSignature() & 0xff);
        OPCODE_DFU_RECEIVE_FW_IMAGE_STR[2] = (byte)((mBinInputStream.binFileSignature() >> 8) & 0xff);
        if (mImageUpdateOffset != 0) {
            OPCODE_DFU_RECEIVE_FW_IMAGE_STR[3] = (byte)(mImageUpdateOffset & 0xff);
            OPCODE_DFU_RECEIVE_FW_IMAGE_STR[4] = (byte)((mImageUpdateOffset >> 8) & 0xff);
            OPCODE_DFU_RECEIVE_FW_IMAGE_STR[5] = (byte)((mImageUpdateOffset >> 16) & 0xff);
            OPCODE_DFU_RECEIVE_FW_IMAGE_STR[6] = (byte)((mImageUpdateOffset >> 24) & 0xff);
        } else {
            OPCODE_DFU_RECEIVE_FW_IMAGE_STR[3] = 12; // Bin header length
            OPCODE_DFU_RECEIVE_FW_IMAGE_STR[4] = 0x00;
            OPCODE_DFU_RECEIVE_FW_IMAGE_STR[5] = 0x00;
            OPCODE_DFU_RECEIVE_FW_IMAGE_STR[6] = 0x00;
        }
        writeCharac(mBluetoothGatt, mControlPointCharacteristic, OPCODE_DFU_RECEIVE_FW_IMAGE_STR);
        
        // the last sent to remote may not alignment, reinstall input stream
        if (mBytesSent != mImageUpdateOffset) {
            try {
            	// close the input stream
            	closeInputStream(mBinInputStream);
            	// reinstall the input stream, here didn't need check
            	checkAndPrepareTheFile(mOtaFilePath);
            	if(mImageUpdateOffset != 0) {
	            	// initial byte sent
	            	mBytesSent = mImageUpdateOffset - 12;
	                mBinInputStream.skip(mImageUpdateOffset - 12);
            	} else {
            		// initial byte sent
            		mBytesSent = 0;
            	}
                if(DBG) Log.i(TAG, "mBytesSent " + mBytesSent + " mImageUpdateOffset: " + mImageUpdateOffset);
            } catch (IOException e) {
                // do nothing
            	if(DBG) Log.i(TAG, "TODO STEP 4.4 IOException do nothing");
            }
        }
        
        final long startTime = System.currentTimeMillis();
        if(DBG) Log.i(TAG, "time test of download fw start time" + startTime);
        // send the image
        uploadFirmwareImage(mBluetoothGatt, mDataCharacteristic, mBinInputStream);
        if(DBG) Log.i(TAG, "Transfer of " + mBytesSent + " bytes has taken " + (System.currentTimeMillis() -
                			startTime) + " ms");
        
        // TODO STEP 4.6: validate FW
        // send Start OPCODE_DFU_VALIDATE_FW_IMAGE command to Control Point
        // opcode, signature
        if(DBG) Log.d(TAG, "Sending OPCODE_DFU_VALIDATE_FW_IMAGE command (OpCode = 0x03)");
        OPCODE_DFU_VALIDATE_FW_IMAGE_STR[0] = OPCODE_DFU_VALIDATE_FW_IMAGE;
        OPCODE_DFU_VALIDATE_FW_IMAGE_STR[1] = (byte)(mBinInputStream.binFileSignature() & 0xff); // uint16 signature low byte
        OPCODE_DFU_VALIDATE_FW_IMAGE_STR[2] = (byte)((mBinInputStream.binFileSignature() >> 8) & 0xff); // uint16 signature high byte
        writeCharac(mBluetoothGatt, mControlPointCharacteristic, OPCODE_DFU_VALIDATE_FW_IMAGE_STR);
        if(DBG) Log.d(TAG, "Reading OPCODE_DFU_VALIDATE_FW_IMAGE notification");
        response = readNotificationResponse();
        status = response[2];
        if (status != DFU_STATUS_SUCCESS) {
            if(DBG) Log.e(TAG, "Validate FW failed with status: " + String.valueOf(status));
            throw new DfuException("Validate FW failed", ERROR_REMOTE_MASK | status);
        }
        
        // TODO STEP 5:
        // completed broadcast must send after the OPCODE_DFU_ACTIVE_IMAGE_RESET response come
        // update the process state
        sendProcessStateBroadcast(STA_OTA_UPGRADE_SUCCESS);
        
        // send Start OPCODE_DFU_ACTIVE_IMAGE_RESET command to Control Point
        // opcode, signature
        if(DBG) Log.d(TAG, "Sending OPCODE_DFU_ACTIVE_IMAGE_RESET command (OpCode = 0x04)");
        //We assume it complete because after OPCODE_DFU_ACTIVE_IMAGE_RESET command sent, the bee target will reboot immdiately and can't reply a write resonse
        OPCODE_DFU_ACTIVE_IMAGE_RESET_STR[0] = OPCODE_DFU_ACTIVE_IMAGE_RESET;
        // if the last command, we didn't care the response come or not
        try{
        	writeCharac(mBluetoothGatt, mControlPointCharacteristic, OPCODE_DFU_ACTIVE_IMAGE_RESET_STR);
        } catch (final DfuException e) {
        	if(DBG) Log.e(TAG, "Send the last command have some error, ignore it, error code is: " + e.getErrorNumber());
        }
        
        // if send the OPCODE_DFU_ACTIVE_IMAGE_RESET command, the disconnect will soon come, so here need to check the connect state
        if(mConnectionState == STATE_CONNECTED_AND_READY) {
        	if(DBG) Log.d(TAG, "wait the remote reset and disconnect");
        	waitUntilDisconnected();
        } else {
        	if(DBG) Log.d(TAG, "the remote is already disconnected");
        }
        

        // TODO STEP 6:
        // if SUCCESS_MODE is SUCCESS_WITH_RECONNECT
        // the OTA process is success
        sendSuccessBroadcast(1);
    }
    
    
    private Handler mHandle = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            if(DBG) Log.d(TAG, "MSG No " + msg.what);
            
            switch (msg.what) {
	            case START_OTA_PROCESS:  // 0
	            	// run the OTA process thread
                    mThreadOta = new ThreadOta();
                    mThreadOta.start();
	                break;
	            case ERROR_BROADCAST:
	            	dispatchProcessingCompletedCallback(ERROR_BROADCAST, msg.arg1);
	            	break;
	            case SUCCESS_BROADCAST:
	            	dispatchProcessingCompletedCallback(SUCCESS_BROADCAST, msg.arg1);
	            	break;
	            case PROGRESS_BROADCAST:
	            	dispatchProcessingCompletedCallback(PROGRESS_BROADCAST, msg.arg1);
	            	break;
	            case PROCESS_STATE_BROADCAST:
	            	dispatchProcessingCompletedCallback(PROCESS_STATE_BROADCAST, msg.arg1);
	            	break;
	            default:
	                break;
            }
            super.handleMessage(msg);
        }
        
    };
    
    private void dispatchProcessingCompletedCallback(int type, int code) {  
    	IRealsilDfuCallback cb = mCallbacksMap.get(mPackageName);    
            if (cb == null){
                return;
            }    
            mCallbacks.beginBroadcast();    
            try {
            	switch (type) {
	            case ERROR_BROADCAST:
	            	cb.onError(code);
	            	break;
	            case SUCCESS_BROADCAST:
	            	cb.onSucess(code);
	            	break;
	            case PROGRESS_BROADCAST:
	            	cb.onProgressChanged(code);
	            	break;
	            case PROCESS_STATE_BROADCAST:
	            	cb.onProcessStateChanged(code);
	            	break;
	            default:
	                break;
            	}
            }catch (RemoteException e) {    
            }    
            mCallbacks.finishBroadcast();    
    }  
    
    // OTA process Thread
    private class ThreadOta extends Thread {
    	public void run() {
    		// reconnect counter
    		int reconnectCounter = 0;
            // time test
            final long startOTATime = System.currentTimeMillis();
    		if(DBG) Log.i(TAG, "ota thread is run, time test of OTA start time " + startOTATime);
    		
    		// prepare the OTA process
    		try {
    			// if device is in normal mode, we must do some prepare work
    			if(mOtaWorkMode == OTA_MODE_FULL_FUNCTION) {
    				// let the device enter OTA mode, and find the device by LE scan
    				prepareOTAProcess();
    			} else {
    				// set the ota address
    				mOtaDeviceAddress = mDeviceAddress;
    			}
				int error;
				// if a error disconnect occur, and connect count less 2
	    		do {
	    			connectOTAProcess();
	    			// initial error flag
	    			error = 0;
		    		// start OTA process
		    		try {
						startOTAProcess();
					} catch (DfuException e) {
						// TODO Auto-generated catch block
						if(DBG) Log.e(TAG, "Something error in OTA process, e: " + e);
						error = e.getErrorNumber();
						reconnectCounter++;
						
						// if is not a connection error and reach the max reconnect count, just return
						if(((error & ERROR_CONNECTION_MASK) == 0) || reconnectCounter == 2) {
							// any error must try to send reset command
							if(DBG) Log.e(TAG, "RemoteDfuException -> Sending Reset command (Op Code = 0x05)");
							OPCODE_DFU_RESET_STR[0] = OPCODE_DFU_RESET;
							try {
			                    writeCharac(mBluetoothGatt, mControlPointCharacteristic, OPCODE_DFU_RESET_STR);
							} catch (final DfuException ee) {
					        	if(DBG) Log.e(TAG, "Send the reset command have some error, ignore it, error code is: " + ee.getErrorNumber());
					        }
							waitUntilDisconnected();
							
							// send error msg to handle
							sendErrorBroadcast(error);
							break;
						}
						
						terminateConnection(mBluetoothGatt, error);
					} 
	    	    } while (reconnectCounter < 2 && (error & ERROR_CONNECTION_MASK) != 0);
			} catch (DfuException e) {
				// TODO Auto-generated catch block
				if(DBG) Log.e(TAG, "Something error in OTA process, e: " + e);
				// send error msg to handle
				sendErrorBroadcast(e.getErrorNumber());
			}
    		
			isInOtaProcess = false;
    		if(DBG) Log.i(TAG, "ota thread is stop, time test of OTA time " + (System.currentTimeMillis() -
                            startOTATime) + " ms");
    	}
    }
    
    private Handler mLeScanHandler = new Handler();   
    private void scanLeDevice(boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
        	mLeScanHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
	                if(true == mScanning) {
	                    mScanning = false;
	                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                	}
                }
            }, SCAN_PERIOD);

            mScanning = true;
            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
            	// Android4.4 don't support 128bit UUID filter.
	            mBluetoothAdapter.startLeScan(serviceUuids, mLeScanCallback);
            } else {
            	mBluetoothAdapter.startLeScan(mLeScanCallback);
            }
        } 
        else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
    }
    
    
    //class DfuGattCallback extends BluetoothGattCallback {
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt, final int status, final int newState) {
        	if(DBG) Log.d(TAG, "onConnectionStateChange: status = " + status + ",newState = " + newState);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothGatt.STATE_CONNECTED) {
                    if(DBG) Log.i(TAG, "onConnectionStateChange: Connected to GATT server");
                    mConnectionState = STATE_CONNECTED;

                    // Attempts to discover services after successful connection.
                    final boolean success = gatt.discoverServices();
                    if(DBG) Log.d(TAG, "onConnectionStateChange: Attempting to start service discovery..." + (success ? "succeed" : "failed"));
                    if (!success) {
                        mErrorState = ERROR_SERVICE_DISCOVERY_NOT_STARTED;
                    } else {
                        // just return here, lock will be notified when service discovery finishes
                        return;
                    }
                } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                	if(DBG) Log.i(TAG, "onConnectionStateChange: Disconnected from GATT server");
                	if(mProcessState == STA_START_OTA_PROCESS) {
                		// maybe remote disconnect
        	            mErrorState = ERROR_CONNECTION_MASK | status;
        	            if(DBG) Log.i(TAG, "disconnect in OTA process, mErrorState: " + mErrorState);
                	}
                    mConnectionState = STATE_DISCONNECTED;
                    // disconnect ok
                    isDisconnectOK = true;
                }
            } else {
            	if(DBG) Log.i(TAG, "onConnectionStateChange error: status " + status + " newState: " +
                        	newState);
                mConnectionState = STATE_DISCONNECTED;
                // maybe something error
	            mErrorState = ERROR_CONNECTION_MASK | status;
            }
            // notify waiting thread
            synchronized (mLock) {
            	// for long time no connection state change callback
            	// if connect status with GATT_ERROR, just forget it
            	isConnectedCallbackCome = true;
                mLock.notifyAll();
            }
        }
     	// find server
        // warning: 1. here not judge the service
        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, final int status) {
        	if(DBG) Log.d(TAG, "onServicesDiscovered: status = " + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
            	// if in the normal model
            	if(mProcessState == STA_REMOTE_ENTER_OTA) {
	            	// find the special service UUID
	                BluetoothGattService mService = gatt.getService(OTA_SERVICE);
	                if(mService == null) {
	                	// modify for new spec
	                	mService = gatt.getService(NEW_OTA_SERVICE);
	                	if(mService == null) {
		                    if(DBG) Log.e(TAG, "OTA service not found");
		                    mErrorState = ERROR_CANNOT_FIND_SERVICE_ERROR;
		                    return;
	                	}
	                }
	             	// find the special characteristic UUID
	                mOtaResetCharacteristic = mService.getCharacteristic(OTA_CHARA);
	                if(mOtaResetCharacteristic == null) {
	                	if(DBG) Log.e(TAG, "OTA characteristic not found");
	                    mErrorState = ERROR_CANNOT_FIND_CHARAC_ERROR;
	                    return;
	                }
	                // find the special battery service
	                mService = gatt.getService(BATTERY_SERVICE);
	                if(mService == null) {
	                	if(mService == null) {
		                    if(DBG) Log.e(TAG, "Battery service not found");
		                    mErrorState = ERROR_CANNOT_FIND_SERVICE_ERROR;
		                    return;
	                	}
	                }
	             	// find the special characteristic UUID
	                mBluetoothBatteryReadCharacteristic = mService.getCharacteristic(BATTERY_READ_CHARA);
	                if(mBluetoothBatteryReadCharacteristic == null) {
	                	if(DBG) Log.e(TAG, "Battery characteristic not found");
	                    mErrorState = ERROR_CANNOT_FIND_CHARAC_ERROR;
	                    return;
	                }
	                
	                // command type
	                mOtaResetCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
	            // if in the OTA model
            	} else {
            		// find the special service UUID
	                BluetoothGattService mService = gatt.getService(DFU_SERVICE_UUID);
	                if(mService == null) {
	                    if(DBG) Log.e(TAG, "OTA service not found");
	                    mErrorState = ERROR_CANNOT_FIND_SERVICE_ERROR;
	                    return;
	                }
	             	// find the special characteristic UUID
	                mControlPointCharacteristic = mService.getCharacteristic(DFU_CONTROL_POINT_UUID);
	                if(mControlPointCharacteristic == null) {
	                	if(DBG) Log.e(TAG, "OTA characteristic not found with: " + DFU_CONTROL_POINT_UUID.toString());
	                    mErrorState = ERROR_CANNOT_FIND_CHARAC_ERROR;
	                    return;
	                }
	                // request type
	                mControlPointCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
	                // find the special characteristic UUID
	                mDataCharacteristic = mService.getCharacteristic(DFU_DATA_UUID);
	                if(mDataCharacteristic == null) {
	                	if(DBG) Log.e(TAG, "OTA characteristic not found with: " + DFU_DATA_UUID.toString());
	                    mErrorState = ERROR_CANNOT_FIND_CHARAC_ERROR;
	                    return;
	                }
	                // command type
	                mDataCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
            	}
            	if(DBG) Log.d(TAG, "onServicesDiscovered: Services discovered");
                mConnectionState = STATE_CONNECTED_AND_READY;
            } else {
            	if(DBG) Log.e(TAG, "onServicesDiscovered: error status = " + status);
                mErrorState = ERROR_CONNECTION_MASK | status;
            }

            // notify waiting thread
            synchronized (mLock) {
            	// for long time no connection state change callback
            	isConnectedCallbackCome = true;
                mLock.notifyAll();
                return;
            }
        }

        @Override
        public void onDescriptorWrite(final BluetoothGatt gatt, final BluetoothGattDescriptor descriptor, final int status) {
            if(DBG) Log.d(TAG, "onDescriptorWrite(): status = " + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (CLIENT_CHARACTERISTIC_CONFIG.equals(descriptor.getUuid())) {
                    // we have enabled or disabled characteristic
                	isNotificationsSet = true;
                }
            } else {
            	if(DBG) Log.e(TAG, "onDescriptorWrite(): Descriptor write error: " + status);
                mErrorState = ERROR_BLUEDROID_MASK | status;
            }

            // notify waiting thread
            synchronized (mLock) {
                mLock.notifyAll();
            }
        }

        @Override
        public void onCharacteristicWrite(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, final int status) {
            //Log.d(TAG, "onCharacteristicWrite(): status = " + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                /*
                 * This method is called when either a CONTROL POINT or PACKET characteristic has been written.
				 * If it is the CONTROL POINT characteristic, just set the flag to true.
				 * If the PACKET characteristic was written we must:
				 *  - if the image size was written in DFU Start procedure, just set flag to true
				 *  - else 
				 *      - send the next packet, if notification is not required at that moment
				 *      - do nothing, because we have to wait for the notification to confirm the data received  
				 */
            	mWriteCharacteristicSuccess = true;
                if (DFU_DATA_UUID.equals(characteristic.getUuid())) {
                    // if the PACKET characteristic was written with image data, update counters
                    mBytesSent += characteristic.getValue().length;
                    if(DBG) Log.i(TAG, "onCharacteristicWrite(): mBytesSent = " + mBytesSent + "; Total mImageSizeInBytes = " + mImageSizeInBytes);
                    
                    lastPacketTransferred = (mBytesSent == mImageSizeInBytes);
                    // send the progress information
                    updateProgressNotification();
                }
            
            } else if (status == BluetoothGatt.GATT_FAILURE){
            	if(DBG) Log.d(TAG,"Characteristic write error: " + status);
                
                if(DFU_DATA_UUID.equals(characteristic.getUuid())){
                    mWriteCharacteristicSuccess = false;
                    if(DBG) Log.d(TAG, "write image packet error:" + status + " please retry.");
                }
            } else {
            	mErrorState = ERROR_BLUEDROID_MASK | status;
            	if(DBG) Log.e(TAG,"Characteristic write error: " + mErrorState);
            }

            // notify waiting thread
            synchronized (mCharacteristicWriteCalledLock){
            	mOnCharacteristicWriteCalled = true;
                mCharacteristicWriteCalledLock.notifyAll();
            }
        }
        @Override
        public void onCharacteristicRead(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, final int status) {
        	if(DBG) Log.d(TAG, "onCharacteristicWrite(): status = " + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (BATTERY_READ_CHARA.equals(characteristic.getUuid())) {
                	mReceivedReadData = characteristic.getValue();
                    if(DBG) Log.i(TAG, "onCharacteristicRead(): mReceivedReadData = " + Arrays.toString(mReceivedReadData));
                }
            
            } else {
            	mErrorState = ERROR_BLUEDROID_MASK | status;
            	if(DBG) Log.e(TAG,"Characteristic read error: " + mErrorState);
            }

            // notify waiting thread
            synchronized (mCharacteristicReadCalledLock){
            	mOnCharacteristicReadCalled = true;
            	mCharacteristicReadCalledLock.notifyAll();
            }
        }
        
        @Override
        public void onCharacteristicChanged(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
            final int responseType = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
            final int requestOpCode = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 1);
            if(DBG) Log.d(TAG, "onCharacteristicChanged: responseType = " + responseType + ", requestOpCode = " + requestOpCode);
            if(responseType == 16) {// check the right notification
                if(requestOpCode == OPCODE_DFU_CONNECTION_PARAMETER_UPDATE) {
                    // we do not wait for connection parameters notification
                    if(DBG) Log.w(TAG, "we do not wait for connection parameters notification, value: " +
                            Arrays.toString(characteristic.getValue()));
                    return;
                }
                mReceivedData = characteristic.getValue();

                // notify waiting thread
                synchronized (mLock) {
                    // for long time no notification come
                    isNotificationCome = true;

                    mLock.notifyAll();
                }
            }
        }
    };

    private void closeInputStream(InputStream inputStream) {
    	if(DBG) Log.i(TAG, "closeInputStream...");
        try {
            if (inputStream != null) {
                inputStream.close();
            }
        } catch (IOException e1) {
        	if(DBG) Log.i(TAG, "closeInputStream fail");
        }
    }
    
    /**
     * Sets number of data packets that will be send before the notification will be received
     *
     * @param data  control point data packet
     * @param value number of packets before receiving notification. If this value is 0, then the notification of packet receipt will be disabled by the DFU target.
     */
    private void setNumberOfPackets(final byte[] data, final int value) {
    	if(DBG) Log.d(TAG, "setNumberOfPackets()");
        data[1] = (byte) (value & 0xFF);
        data[2] = (byte) ((value >> 8) & 0xFF);
    }

    /**
     * Opens the binary input stream from a BIN file. A Path to the BIN file is given.
     *
     * @param filePath the path to the BIN file
     * @return the binary input stream with BIN data
     * @throws IOException
     */
    private BinInputStream openInputStream(final String filePath) throws IOException {
    	if(DBG) Log.d(TAG, "openInputStream()");
        final InputStream is = new FileInputStream(filePath);
        return new BinInputStream(is);
    }


    /**
     * Connects to the BLE device with given address. This method is SYNCHRONOUS,
     * it wait until the connection status change from {@link #STATE_CONNECTING} to
     * {@link #STATE_CONNECTED_AND_READY} or an error occurs.
     *
     * @param address the device address
     * @throws DfuException 
     */
    private BluetoothGatt connect(final String address) throws DfuException {
        if(DBG) Log.d(TAG, "connect(): address = " + address);
        mConnectionState = STATE_CONNECTING;
        // initial the error state
        mErrorState = 0;
        // initial the callback flag for long time no callback come
        isConnectedCallbackCome = false;
        
        if(DBG) Log.d(TAG, "Connecting to the device...");
        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Use GlobalGatt
        final BluetoothGatt gatt;
        if(mGlobalGatt != null) {
            if(DBG) Log.d(TAG, "Use GlobalGatt connect, with: " + address);
            mGlobalGatt.connect(address, mGattCallback);
            gatt = mBluetoothGatt = mGlobalGatt.getBluetoothGatt(address);
        } else {
            gatt = mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        }
        if(DBG) Log.d(TAG, "mBluetoothGatt: " + gatt);
        
        // We have to wait until the device is connected and services are discovered
        // Connection error may occur as well.
        try {
            synchronized (mLock) {
                if (isConnectedCallbackCome == false && mErrorState == 0) {
                	if(DBG) Log.d(TAG,"wait for connect gatt, wait for " + MAX_CONNECTION_LOCK_WAIT_TIME + "ms" );
                	mLock.wait(MAX_CONNECTION_LOCK_WAIT_TIME);
                }
            }
        } catch (final InterruptedException e) {
        	if(DBG) Log.e(TAG,"connect(): Sleeping interrupted, e = " + e);
        	mErrorState = ERROR_LOCK_WAIT_ERROR;
        }
        // if long time no connection state change callback, if have other error, don't care the callback come or not
        if(isConnectedCallbackCome == false && mErrorState == 0) {
        	if(DBG) Log.e(TAG, "wait for connect, but can not connect with no callback");
        	mErrorState = ERROR_CANNOT_CONNECT_WITH_NO_CALLBACK_ERROR;
        };
                
        if((gatt == null || mConnectionState != STATE_CONNECTED_AND_READY) && mErrorState == 0) {
        	if(DBG) Log.e(TAG, "connect with some error, please check. mConnectionState" + mConnectionState);
        	mErrorState = ERROR_CONNECT_ERROR;
        }
        // throw the error
        if (mErrorState != 0) {
        	if(mConnectionState == STATE_CONNECTING) {
        		mConnectionState = STATE_DISCONNECTED;
        	}
            throw new DfuException("Unable to connect with some error", mErrorState);
        }
        return gatt;
    }

    /**
     * Disconnects from the device and cleans local variables in case of error.
     * This method is SYNCHRONOUS and waits until the disconnecting process will be completed.
     *
     * @param gatt  the GATT device to be disconnected
     * @param error error number
     */
    private void terminateConnection(final BluetoothGatt gatt, final int error) {
        if(DBG) Log.d(TAG, "terminateConnection(): error = " + error);
        if (mConnectionState != STATE_DISCONNECTED && mConnectionState != STATE_CLOSED) {
        	if(DBG) Log.i(TAG, "is connected, with connect state: " + mConnectionState + ", do disconnect");
        	disconnect(gatt);
        }
        // Close the device
        refreshDeviceCache(gatt);
        // Close the device
        closeGatt(gatt);
    }

    /**
     * Disconnects from the device. This is SYNCHRONOUS method and waits until the callback returns
     * new state. Terminates immediately if device is already disconnected of long time no callback 
     * come (because, most time, we do disconnect when a error occur). Do not call this method directly,
     * use {@link #terminateConnection(BluetoothGatt, int)} instead.
     * 
     * @param gatt the GATT device that has to be disconnected
     * @throws DfuException 
     */
    private void disconnect(final BluetoothGatt gatt) {
    	if(DBG) Log.d(TAG, "disconnect()");
        if (mConnectionState == STATE_DISCONNECTED)
            return;
        
        mConnectionState = STATE_DISCONNECTING;

        if(DBG) Log.d(TAG, "Disconnecting from the device...");
        if(gatt != null) {
        	gatt.disconnect();
	        // We have to wait until device gets disconnected or an error occur
	        waitUntilDisconnected();
        }
    }

    /**
     * Wait until the connection state will change to {@link #STATE_DISCONNECTED} or until an error occurs.
     */
    private void waitUntilDisconnected() {
        if(DBG) Log.d(TAG, "waitUntilDisconnected()");
        // initial the error code.
        mErrorState = 0;
        
        try {
            synchronized (mLock) {
                if (mConnectionState != STATE_DISCONNECTED && mErrorState == 0) {
                	if(DBG) Log.d(TAG,"wait for disconnect, wait for "+ MAX_CONNECTION_LOCK_WAIT_TIME +"ms");
                	mLock.wait(MAX_CONNECTION_LOCK_WAIT_TIME);
                }
            }
        } catch (final InterruptedException e) {
        	if(DBG) Log.e(TAG, "waitUntilDisconnected(): Sleeping interrupted, e = " + e);
        }
        
        if (mErrorState != 0)
        	if(DBG) Log.e(TAG, "something error in disconnect, ignore it, error state is: " + mErrorState);
    }

    /**
     * Closes the GATT device and cleans up.
     *
     * @param gatt the GATT device to be closed
     */
    private void closeGatt(final BluetoothGatt gatt) {
    	if(DBG) Log.d(TAG, "gatt close()");
        if(gatt != null) {
            // Use GlobalGatt
            if(mGlobalGatt != null) {
                if(DBG) Log.d(TAG, "Use GlobalGatt close, with: " + gatt.getDevice().getAddress());
                mGlobalGatt.closeBluetoothGatt(gatt.getDevice().getAddress());
            } else {
                gatt.close();
            }
        }
        mConnectionState = STATE_CLOSED;
    }

    /**
     * Clears the device cache. After uploading new firmware the DFU target will have other services than before.
     *
     * @param gatt the GATT device to be refreshed
     */
    private void refreshDeviceCache(final BluetoothGatt gatt) {
    	if(DBG) Log.d(TAG, "refreshDeviceCache()");
        /*
		 * There is a refresh() method in BluetoothGatt class but for now it's hidden. We will call it using reflections.
		 */
        try {
            final Method refresh = gatt.getClass().getMethod("refresh");
            if (refresh != null) {
                final boolean success = (Boolean) refresh.invoke(gatt);
                if(DBG) Log.d(TAG, "refreshDeviceCache(): Refreshing result: " + success);
            }
        } catch (Exception e) {
        	if(DBG) Log.e(TAG,"refreshDeviceCache(): An exception occured while refreshing device, e = " + e);
        }
    }

    /**
     * Enables or disables the notifications for given characteristic. This method is SYNCHRONOUS
     * and wait until the {@link BluetoothGattCallback#onDescriptorWrite(BluetoothGatt,
     * BluetoothGattDescriptor, int)} will be called or the connection state will change from
     * {@link #STATE_CONNECTED_AND_READY}. If connection state will change,
     * or an error will occur, an exception will be thrown.
     *
     * @param gatt           the GATT device
     * @param characteristic the characteristic to enable or disable notifications for
     * @param enable         <code>true</code> to enable notifications, <code>false</code> to disable them
     * @throws DfuException
     */
    private void setCharacteristicNotification(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, final boolean enable) throws DfuException {
    	if(DBG) Log.d(TAG, "setCharacteristicNotification()");
    	
        // initial error state
        mErrorState = 0;
        
        isNotificationsSet = false;

        if(DBG) Log.i(TAG, (enable ? "Enabling " : "Disabling") + " notifications...");

        // enable notifications locally
        gatt.setCharacteristicNotification(characteristic, enable);

        // enable notifications on the device
        final BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG);
        descriptor.setValue(enable ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
        gatt.writeDescriptor(descriptor);

        // We have to wait 10 seconds.
        try {
            synchronized (mLock) {
                if (isNotificationsSet == false && mErrorState == 0) {
                	if(DBG) Log.i(TAG, "wait write Characteristic Notification " + MAX_CALLBACK_LOCK_WAIT_TIME + "ms");
                    mLock.wait(MAX_CALLBACK_LOCK_WAIT_TIME);
                }
            }
        } catch (final InterruptedException e) {
        	if(DBG) Log.e(TAG,"setCharacteristicNotification(): Sleeping interrupted, e = " + e);
        }
        // check the notify flag change or not
        if (isNotificationsSet == false && mErrorState == 0) {
        	if(DBG) Log.e(TAG, (enable ? "Enabling " : "Disabling") + " notifications failed");
        	mErrorState = ERROR_WRITE_CHARAC_NOTIFY_ERROR;
        }
        if (mErrorState != 0)
            throw new DfuException("Unable to set notifications state", mErrorState);
    }
    
    /**
     * Read the operation code to the characteristic. This method is SYNCHRONOUS and wait until the
     * {@link BluetoothGattCallback#onCharacteristicRead(BluetoothGatt, BluetoothGattCharacteristic, int)}
     *  will be called or the connection state will change from {@link #STATE_CONNECTED_AND_READY}.
     * If an error occur, an exception will be thrown.
     *
     * @param gatt           the GATT device
     * @param characteristic the characteristic to write to.
     * @throws DfuException
     */
    private void readCharac(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic)
    		throws DfuException {
    	if(DBG) Log.d(TAG, "readCharac()");
    	// initial the error state
    	mErrorState = 0;
    	// initial the receive notification
    	mReceivedReadData = null;
    	
    	mOnCharacteristicReadCalled = false;

    	gatt.readCharacteristic(characteristic);

    	synchronized (mCharacteristicReadCalledLock){
            try {
            	// here only wait for 3 seconds
                if (mOnCharacteristicReadCalled == false && mErrorState == 0 && mConnectionState == STATE_CONNECTED_AND_READY)
                	mCharacteristicReadCalledLock.wait(MAX_CALLBACK_LOCK_WAIT_TIME);
            } catch (final InterruptedException e) {
                if(DBG) Log.e(TAG,"mCharacteristicReadCalledLock Sleeping interrupted,e:" + e);
                mErrorState = ERROR_LOCK_WAIT_ERROR;
            }
        }
        // if read characteristic but no callback and the have no other error
        if(mOnCharacteristicReadCalled == false && mErrorState == 0) {
        	if(DBG) Log.e(TAG,"read value but no callback");
        	mErrorState = ERROR_CANNOT_SEND_COMMAND_WITH_NO_CALLBACK_ERROR;
        }
            
        // catch the error and throw the exception
        if(mErrorState != 0) {
        	throw new DfuException("Error while send command", mErrorState);
        }
        
    }
    
    private void writeCharac(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, final byte[] value)
    		throws DfuException {
    	writeCharac(gatt, characteristic, value, value.length);
    }
    
    /**
     * Writes the operation code to the characteristic. This method is SYNCHRONOUS and wait until the
     * {@link BluetoothGattCallback#onCharacteristicWrite(BluetoothGatt, BluetoothGattCharacteristic, int)}
     *  will be called or the connection state will change from {@link #STATE_CONNECTED_AND_READY}.
     * If an error occur, an exception will be thrown.
     *
     * @param gatt           the GATT device
     * @param characteristic the characteristic to write to.
     * @param value          the value to write to the characteristic
     * @param size 			 the length want to send
     * @throws DfuException
     */
    private void writeCharac(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, final byte[] value, int size) 
    		throws DfuException {
    	
    	if(DBG) Log.d(TAG, "writeCharac()");
    	
    	// initial the error state
    	mErrorState = 0;
    	// initial try time
    	int tryTime = 0;
    	
    	// initial the receive notification
    	mReceivedData = null;
        mWriteCharacteristicSuccess = false;
        while (!mWriteCharacteristicSuccess) {
            mOnCharacteristicWriteCalled = false;

            writePacket(gatt, characteristic, value, size);

            synchronized (mCharacteristicWriteCalledLock){
                try {
                	// here only wait for 3 seconds
                    if (mOnCharacteristicWriteCalled == false && mErrorState == 0 && mConnectionState == STATE_CONNECTED_AND_READY)
                        mCharacteristicWriteCalledLock.wait(MAX_CALLBACK_LOCK_WAIT_TIME);
                } catch (final InterruptedException e) {
                    if(DBG) Log.e(TAG,"mCharacteristicWriteCalledLock Sleeping interrupted,e:" + e);
                    mErrorState = ERROR_LOCK_WAIT_ERROR;
                }
            }
            // if write characteristic but no callback and the have no other error
            if(mOnCharacteristicWriteCalled == false && mErrorState == 0) {
            	if(DBG) Log.e(TAG,"send command but no callback");
            	mErrorState = ERROR_CANNOT_SEND_COMMAND_WITH_NO_CALLBACK_ERROR;
            }
            // reach the max try time
            if(tryTime > MAX_RESEND_TIME && mErrorState == 0) {
            	if(DBG) Log.e(TAG,"send command reach max try time");
            	mErrorState = ERROR_SEND_COMMAND_WITH_MAX_TRY_TIME_ERROR;
            }
            // try time update
            tryTime++;
            
            // catch the error and throw the exception
            if(mErrorState != 0) {
            	throw new DfuException("Error while send command", mErrorState);
            }
        }

    }
    /**
     * Start le scan, wait find the special le device. This method is SYNCHRONOUS and terminates when find the 
     * special le device. If an error occur, an exception will be thrown.
     *
     * @throws DfuException        Thrown if DFU error occur
     */
    private void scanTheOtaDevice() throws DfuException {
    	if(DBG) Log.d(TAG, "scanTheOtaDevice()");
    	// initial the variables
    	mErrorState = 0;
    	// for long time didn't find the ota device
    	isScanTheDevice = false;
    	// start le scan
    	if(DBG) Log.d(TAG, "start le scan");
        scanLeDevice(true);
        if(DBG) Log.d(TAG, "le scan started");
    	// We have to wait for connect the device
        try {
            synchronized (mLeScanLock) {
                if (isScanTheDevice == false && mErrorState == 0) {
                    //wait the special time equal the scan period + 1s
                	mLeScanLock.wait(SCAN_PERIOD + 1000);
                }
            }
        } catch (final InterruptedException e) {
        	if(DBG) Log.e(TAG,"scanTheOtaDevice(): Sleeping interrupted, e = " + e);
            mErrorState = ERROR_LOCK_WAIT_ERROR;
        }
        // if didn't find the device and the have no other error
        if(isScanTheDevice == false && mErrorState == 0) {
        	if(DBG) Log.e(TAG,"didn't find the special device");
        	mErrorState = ERROR_CANNOT_FIND_DEVICE_ERROR;
        }
        // catch the error and throw the exception
        if(mErrorState != 0) {
        	throw new DfuException("Error while send command", mErrorState);
        }        
    }
    
    /**
     * Prepare & calculate the data size to send. If an error occur, an exception will be thrown.
     *
     * @param file                 the path of OTA bin file
     * @throws DfuException        Thrown if DFU error occur
     */
    private boolean checkAndPrepareTheFile(String file) {
    	// Prepare & calculate the data size to send
        try {
        	if(DBG) Log.d(TAG, "Opening BIN file: filePath " + file);
         	// get the update file input stream
            mBinInputStream = openInputStream(file);
            mImageSizeInBytes = mBinInputStream.remainSizeInBytes();
            mImageSizeInPackets = mBinInputStream.remainNumInPackets(MAX_PACKET_SIZE);
            mImageVersion = BinInputStream.toUnsigned(mBinInputStream.binFileVersion());
            if(DBG) Log.d(TAG, "file info, mImageSizeInBytes: " + mImageSizeInBytes + 
            								"mImageSizeInPackets: " + mImageSizeInPackets +
            								"mImageVersion: " + mImageVersion);
        } catch (final IOException e) {
        	if(DBG) Log.e(TAG, "An exception occurred while opening file, e = " + e);
        	closeInputStream(mBinInputStream);
        	return false;
        }
        return true;
    }
    /**
     * Starts sending the data. This method is SYNCHRONOUS and terminates when the whole file
     * will be uploaded or the connection status will change from {@link
     * #STATE_CONNECTED_AND_READY}. If connection state will change, or an error will occur,
     * an exception will be thrown.
     *
     * @param gatt                 the GATT device (DFU target)
     * @param packetCharacteristic the characteristic to write file content to. Must be the DFU PACKET.
     * @throws DfuException                Thrown if DFU error occur
     */
    private void uploadFirmwareImage(final BluetoothGatt gatt,
                                       final BluetoothGattCharacteristic packetCharacteristic,
                                       final BinInputStream inputStream) throws DfuException {
        if(DBG) Log.d(TAG, "uploadFirmwareImage()");
        mErrorState = 0;
        // initial packet transferred flag
        lastPacketTransferred = false;
        
        packetCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);

        final byte[] buffer = mBuffer;
        byte[] beforeEncryptBytes = null;
        byte[] afterEncryptBytes = null;
        int size;

        while(!lastPacketTransferred) {
        	// to support app > 100K
            if(mImageSizeInBytes > BIG_IMAGE_LIMIT_IMAGE_SIZE) {
            	// if reach the special point, skip some packet
            	if(mBytesSent == BIG_IMAGE_SPECIAL_POINT) {
            		// start send in 140K of the file, so need minus the file header
            		mBytesSent = BIG_IMAGE_START_OFFSET_OF_BIG_IMAGE - BIG_IMAGE_HEADER_LENGTH;
            		try {
						inputStream.skip(BIG_IMAGE_START_OFFSET_OF_BIG_IMAGE - BIG_IMAGE_SPECIAL_POINT - BIG_IMAGE_HEADER_LENGTH);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
            		if(DBG) Log.i(TAG, "big image info: reach the special size, skip some packet, current mBytesSent: " + mBytesSent);
            	}
            }
         	// speed control
            mSpeedControl.StartSpeedControl();
            
            try {
        		// read special byte data to send, here can set mtu
                size = inputStream.readPacket(buffer);
                //AES encryption
                if (haveAES && size >= 16) {
                    beforeEncryptBytes = new byte[16];
                    afterEncryptBytes = new byte[16];
                    System.arraycopy(buffer, 0, beforeEncryptBytes, 0, 16);
                    try {
                        aes_encrypt(beforeEncryptBytes, afterEncryptBytes);
                        //Log.i(TAG, "uploadFirmwareImage:The encrypted data is: " + Arrays.toString(afterEncryptBytes));
                        System.arraycopy(afterEncryptBytes, 0, buffer, 0, 16);
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                } else if(size == 0) {
                	if(DBG) Log.e(TAG, "Error while reading file with size: " + size);
                	throw new DfuException("Error while reading file", ERROR_FILE_IO_EXCEPTION);
                }
            } catch (final IOException e) {
                throw new DfuException("Error while reading file", ERROR_FILE_IO_EXCEPTION);
            }
            
            // send image to remote
            writeCharac(gatt, packetCharacteristic, buffer, size);
            
            // speed control 
            mSpeedControl.WaitSpeedControl();
        }
    }

    /**
     * Writes the buffer to the characteristic. The maximum size of the buffer is 20 bytes. This
     * method is ASYNCHRONOUS and returns immediately after adding the data to TX queue.
     *
     * @param gatt           the GATT device
     * @param characteristic the characteristic to write to. Should be the DFU PACKET
     * @param buffer         the buffer with 1-20 bytes
     * @param size           the number of bytes from the buffer to send
     */
    private void writePacket(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, final byte[] buffer, final int size) {
        if(DBG) Log.d(TAG, "writePacket()");
        if(characteristic == null || gatt == null) {
    		if(DBG) Log.e(TAG, "something error.");
    		return;
    	}
        byte[] locBuffer = buffer;
        //Log.d(TAG, "writePacket:size = " + size + ";buffer.length = " + buffer.length);
        if (buffer.length != size) {
            locBuffer = new byte[size];
            System.arraycopy(buffer, 0, locBuffer, 0, size);
        }
        characteristic.setValue(locBuffer);
        gatt.writeCharacteristic(characteristic);
    }

    /**
     * Waits until the notification arrives. Returns the data from the notification. This method
     * will block the thread if response is not ready or connection state will change from
     * {@link #STATE_CONNECTED_AND_READY}. If an error occurs, an exception will be thrown.
     *
     * @return the value returned by the Control Point notification
     * @throws DfuException
     */
 	// wait for RCU send the notification
    private byte[] readNotificationResponse() throws DfuException {
        if(DBG) Log.d(TAG, "readNotificationResponse()");
        mErrorState = 0;
     	// long time no notification come flag
        isNotificationCome = true;
        try {
            synchronized (mLock) {
                if (mReceivedData == null && mConnectionState == STATE_CONNECTED_AND_READY && mErrorState == 0) {
                	// for long time no notification come
                    isNotificationCome = false;
                    if(DBG) Log.d(TAG,"wait for notification, wait for " + MAX_NOTIFICATION_LOCK_WAIT_TIME + "ms");
                	// only wait for a while (10s)
                    mLock.wait(MAX_NOTIFICATION_LOCK_WAIT_TIME);
                }
                // if long time no notification come
                if(isNotificationCome != true && mErrorState == 0) {
                	if(DBG) Log.e(TAG,"wait for notification, but not come");
                	mErrorState = ERROR_NO_NOTIFICATION_COME_ERROR;
                };
            }
        } catch (final InterruptedException e) {
        	if(DBG) Log.e(TAG,"readNotificationResponse(): Sleeping interrupted, e = " + e);
        	mErrorState = ERROR_LOCK_WAIT_ERROR;
        }
        if (mErrorState != 0)
            throw new DfuException("Unable to receive notification", mErrorState);
        return mReceivedData;
    }

    /**
     * Stores the last progress percent. Used to lower number of calls of {@link RealsilDfuCallback#onProgressChanged(int)}.
     */
    private int mLastProgress = -1;
    /**
     * Creates or updates the notification in the Notification Manager. Sends broadcast with current progress to the activity.
     */
    private void updateProgressNotification() {
        final int progress = (int) (100.0f * mBytesSent / mImageSizeInBytes);
                
        if (mLastProgress == progress)
            return;

        mLastProgress = progress;
        if(DBG) Log.d(TAG, "updateProgressNotification(): LastProgress = " + mLastProgress);
        
        // send progress information
        sendProgessBroadcast(progress);
    }
    
    private void sendProcessStateBroadcast(final int process) {
        if(DBG) Log.i(TAG, "sendProcessStateBroadcast(): process state = " + process);
        //update the process state
        mProcessState = process;

        // send process state msg to handle
        Message message = mHandle.obtainMessage(PROCESS_STATE_BROADCAST);
        message.arg1 = process;
    	mHandle.sendMessage(message);
    }
    
    private void sendSuccessBroadcast(final int success) {
    	if(DBG) Log.i(TAG, "sendSuccessBroadcast(): success = " + success);
    	// may be it didn't disconnect, just do it to make sure disconnect
    	terminateConnection(mBluetoothGatt, 0);
    	
    	closeInputStream(mBinInputStream);
    	
    	// send success msg to handle
        Message message = mHandle.obtainMessage(SUCCESS_BROADCAST);
        message.arg1 = success;
    	mHandle.sendMessage(message);
    }
    private void sendErrorBroadcast(final int error) {
    	if(DBG) Log.i(TAG, "sendErrorBroadcast(): error = " + error);
    	// may be it didn't disconnect, just do it to make sure disconnect
    	terminateConnection(mBluetoothGatt, error);
    	
        closeInputStream(mBinInputStream);
    	// send error msg to handle
        Message message = mHandle.obtainMessage(ERROR_BROADCAST);
        message.arg1 = error;
    	mHandle.sendMessage(message);
    }

    private void sendProgessBroadcast(final int progress) {
    	if(DBG) Log.i(TAG, "sendProgessBroadcast(): progress= " + progress);
    	
    	// send progress msg to handle
        Message message = mHandle.obtainMessage(PROGRESS_BROADCAST);
        message.arg1 = progress;
    	mHandle.sendMessage(message);
    }

    /**
     * Initializes bluetooth adapter
     *
     * @return <code>true</code> if initialization was successful
     */
    private boolean initialize() {
    	if(DBG) Log.d(TAG, "initialize()");
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
            	if(DBG) Log.e(TAG,"initialize(): Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
        	if(DBG) Log.e(TAG,"initialize(): Unable to obtain a BluetoothAdapter.");
            return false;
        }
        // initial the notification flag
        mBytesSent = 0;
        mErrorState = 0;
        return true;
    }
    
    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
        	// stop le scan is not stop immediate
        	if (!mScanning) {
        		if(DBG) Log.e(TAG,"is already stop the le scan, do not do anything");
        		return;
            }
        	if(scanRecord.length < 30) {
            	if(DBG) Log.e(TAG,"the scan data is not right, do nothing");
        		return;
            }
        	if(DBG) Log.d(TAG,"onLeScan() - scanRecord data is : " + Arrays.toString(scanRecord));
            
            // copy the special scan record
            byte[] manufacturerData = new byte[6];
            
            System.arraycopy(scanRecord, 25, manufacturerData, 0, 6);
            String manufacturerDataString = Bytes2HexString(manufacturerData);
            char[] tempDeviceAddress = manufacturerDataString.toCharArray();
            char[] deviceAddress = new char[17];
            for (int i = 0, j = 0; i < 17; i++) {
                if ((i + 1) % 3 == 0) {
                    deviceAddress[i] = ':';
                }
                else {
                    deviceAddress[i] = tempDeviceAddress[j++];
                }
            }
            if(DBG) Log.d(TAG,"onLeScan() - " + " mDeviceAddress = " + mDeviceAddress
                            + " manufacturerDataAddress = " + String.valueOf(deviceAddress));
            if(DBG) Log.d(TAG,"onLeScan() - " + "name = " + device.getName());
            // if the advertising data's addr equal the normal addr, or the name equal the special name "EXTRA_DEVICE_NAME"
            if (String.valueOf(deviceAddress).toUpperCase().equals(mDeviceAddress) 
                    || EXTRA_DEVICE_NAME.equals(device.getName())) {
                mOtaDeviceName = device.getName();
                mOtaDeviceAddress = device.getAddress();
             	// cancel the le scan
                if (mScanning) {
                    scanLeDevice(false);
                }
             	// change the extra addr
                if(DBG) Log.d(TAG, "onLeScan() - get BeeTgt device:" + mOtaDeviceAddress);
                
                // notify waiting thread
                synchronized (mLeScanLock) {
                	// for long time didn't find the ota device
                	isScanTheDevice = true;
                    
                	mLeScanLock.notifyAll();
                }
                
            }
        }
    };

    public static String Bytes2HexString(byte[] b) {
        String ret = "";
        for (int i = 0; i < b.length; i++) {
            String hex = Integer.toHexString(b[i] & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            ret += hex.toUpperCase();
        }
        return ret;
    }    
    
}
