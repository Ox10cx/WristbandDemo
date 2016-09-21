package com.realsil.android.wristbanddemo.linkloss;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.realsil.android.wristbanddemo.utility.GlobalGatt;

import java.util.List;
import java.util.UUID;

/**
 * LinkLoss Service
 * reference: https://developer.bluetooth.org/gatt/services/Pages/ServiceViewer.aspx?u=org.bluetooth.service.battery_service.xml
 */
public class LinkLossService {
    // LOG
    private static final boolean D = true;
    private static final String TAG = "LinkLossService";

    // Support Service UUID and Characteristic UUID
    private final static UUID LINKLOSS_SERVICE_UUID = UUID.fromString("00001803-0000-1000-8000-00805f9b34fb");
    private static final UUID ALERT_LEVEL_CHARACTERISTIC_UUID = UUID.fromString("00002a06-0000-1000-8000-00805f9b34fb");

    public final static UUID CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    // Alert Level
    public final static byte ALERT_LEVEL_DISABLE = 0x00;
    public final static byte ALERT_LEVEL_ENABLE_WITH_LED = 0x01;
    public final static byte ALERT_LEVEL_ENABLE_WITH_LED_BUZZER = 0x02;

    // Support Service object and Characteristic object
    private BluetoothGattService mService;
    private BluetoothGattCharacteristic mAlertLevelCharac;
    private OnServiceListener mCallback;

    private GlobalGatt mGlobalGatt;

    private String mBluetoothAddress;

    private boolean mAlertValue = false;

    public LinkLossService(String addr, OnServiceListener callback) {
        mCallback = callback;
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
    public boolean readInfo() {
        if(mAlertLevelCharac == null) {
            if(D) Log.e(TAG, "read alert error with null charac");
            return false;
        }
        if(D) Log.d(TAG, "read alert info.");
        return mGlobalGatt.readCharacteristic(mBluetoothAddress, mAlertLevelCharac);
    }

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
                mService = gatt.getService(LINKLOSS_SERVICE_UUID);
                if(mService == null) {
                    Log.e(TAG, "Link Loss service not found");
                    return;
                }else {
                    mAlertLevelCharac = mService.getCharacteristic(ALERT_LEVEL_CHARACTERISTIC_UUID);
                    if(mAlertLevelCharac == null) {
                        if(D) Log.e(TAG, "Link Loss service characteristic not found");
                        return;
                    }else {
                        if(D) Log.d(TAG, "Link Loss service is found, mAlertLevelCharac: " + mAlertLevelCharac.getUuid());
                    }
                }
                //isConnected = true;
            } else {
                if(D) Log.e(TAG, "Discovery service error: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            //if(D) Log.d(TAG, "onCharacteristicRead UUID is: " + characteristic.getUuid() + ", addr: " +mBluetoothAddress);
            //if(D) Log.d(TAG, "onCharacteristicRead data value:"+ Arrays.toString(characteristic.getValue()) + ", addr: " +mBluetoothAddress);
            byte[] data = characteristic.getValue();
            if(status == BluetoothGatt.GATT_SUCCESS) {
                if(mAlertLevelCharac.getUuid().equals(characteristic.getUuid())) {
                    byte mode = data[0];
                    if(mode == ALERT_LEVEL_ENABLE_WITH_LED_BUZZER) {
                        mAlertValue = true;
                    } else {
                        mAlertValue = false;
                    }
                    // call function to deal the data
                    mCallback.onLinkLossValueReceive(mAlertValue);
                }
            } else {
                if(D) Log.e(TAG, "Characteristic read error: " + status);
            }

        }
    };

    /**
     * Interface required to be implemented by activity
     */
    public static interface OnServiceListener {
        /**
         * Fired when value come.
         *
         * @param value      receive value
         */
        public void onLinkLossValueReceive(boolean value);
    }

}
