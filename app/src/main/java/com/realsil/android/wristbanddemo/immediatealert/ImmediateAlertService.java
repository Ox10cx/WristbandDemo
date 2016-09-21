package com.realsil.android.wristbanddemo.immediatealert;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.util.Log;
import com.realsil.android.wristbanddemo.utility.GlobalGatt;

import java.util.List;
import java.util.UUID;

/**
 * ImmediateAlert Service
 * reference: https://developer.bluetooth.org/gatt/services/Pages/ServiceViewer.aspx?u=org.bluetooth.service.battery_service.xml
 */
public class ImmediateAlertService {
    // LOG
    private static final boolean D = true;
    private static final String TAG = "ImmediateAlertService";

    // Support Service UUID and Characteristic UUID
    private final static UUID IMMEDIATE_ALERT_SERVICE_UUID = UUID.fromString("00001802-0000-1000-8000-00805f9b34fb");
    private static final UUID ALERT_LEVEL_CHARACTERISTIC_UUID = UUID.fromString("00002a06-0000-1000-8000-00805f9b34fb");

    // Alert Level
    public final static int ALERT_LEVEL_DISABLE = 0x00;
    public final static int ALERT_LEVEL_ENABLE_WITH_LED = 0x01;
    public final static int ALERT_LEVEL_ENABLE_WITH_LED_BUZZER = 0x02;

    // Support Service object and Characteristic object
    private BluetoothGattService mService;
    private BluetoothGattCharacteristic mAlertLevelCharac;

    // Read info Lock
    private Object mLock = new Object();
    private volatile boolean isGetInfo = false;
    private final int LOCK_WAIT_TIME = 5000;

    // Current Alert value
    private int mAlertLevel = -1;
    private GlobalGatt mGlobalGatt;

    private String mBluetoothAddress;

    public ImmediateAlertService(String addr) {
        mBluetoothAddress = addr;

        mGlobalGatt = GlobalGatt.getInstance();
        initial();
    }
    public void close() {
        mGlobalGatt.unRegisterCallback(mBluetoothAddress, mGattCallback);
    }

    private void initial() {
        // register service discovery callback
        mGlobalGatt.registerCallback(mBluetoothAddress, mGattCallback);
    }
    /*
    public boolean readInfo() {
        if(mAlertLevelCharac == null) {
            if(D) Log.e(TAG, "read Battery info error with null charac");
            return false;
        }
        if(D) Log.d(TAG, "read Battery info.");
        return mGlobalGatt.readCharacteristic(mBluetoothAddress, mAlertLevelCharac);
    }*/


    public boolean enableAlert(boolean enable) {
        if(D) Log.d(TAG, "enableAlert, enable: " + enable);
        if(mAlertLevelCharac == null) {
            if(D) Log.e(TAG, "enableAlert info error with null charac");
            return false;
        }

        if(enable) {
            byte[] data = {ALERT_LEVEL_ENABLE_WITH_LED_BUZZER};
            mAlertLevelCharac.setValue(data);
        } else {
            byte[] data = {ALERT_LEVEL_DISABLE};
            mAlertLevelCharac.setValue(data);
        }
        return mGlobalGatt.writeCharacteristicSync(mBluetoothAddress, mAlertLevelCharac);
    }

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                mService = gatt.getService(IMMEDIATE_ALERT_SERVICE_UUID);
                if(mService == null) {
                    Log.e(TAG, "Immediate service not found");
                    return;
                }else {
                    mAlertLevelCharac = mService.getCharacteristic(ALERT_LEVEL_CHARACTERISTIC_UUID);
                    if(mAlertLevelCharac == null) {
                        if(D) Log.e(TAG, "Immediate characteristic not found");
                        return;
                    }else {
                        if(D) Log.d(TAG, "Immediate is found, mAlertLevelCharac: " + mAlertLevelCharac.getUuid());
                    }
                }
                //isConnected = true;
            } else {
                if(D) Log.e(TAG, "Discovery service error: " + status);
            }
        }
    };
}
