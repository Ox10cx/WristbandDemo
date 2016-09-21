package com.realsil.android.wristbanddemo.dfu;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.os.Message;
import android.util.Log;

import com.realsil.android.wristbanddemo.utility.GlobalGatt;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class DfuService {
    // LOG
    private static final boolean D = true;
    private static final String TAG = "DfuService";

    // Support Service UUID and Characteristic UUID
    private final static UUID OTA_SERVICE_UUID = UUID.fromString("0000d0ff-3c17-d293-8e48-14fe2e4da212");
    private final static UUID OTA_CHARACTERISTIC_UUID = UUID.fromString("0000ffd1-0000-1000-8000-00805f9b34fb");
    private final static UUID OTA_READ_PATCH_CHARACTERISTIC_UUID = UUID.fromString("0000ffd3-0000-1000-8000-00805f9b34fb");
    private final static UUID OTA_READ_APP_CHARACTERISTIC_UUID = UUID.fromString("0000ffd4-0000-1000-8000-00805f9b34fb");

    public final static UUID CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    // Support Service object and Characteristic object
    private BluetoothGattService mService;
    private BluetoothGattCharacteristic mAppCharac;
    private BluetoothGattCharacteristic mPatchCharac;

    // Current Battery value
    private int mAppValue = -1;
    private int mPatchValue = -1;

    private GlobalGatt mGlobalGatt;

    private OnServiceListener mCallback;

    private String mBluetoothAddress;

    public DfuService(String addr, OnServiceListener callback) {
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

    public boolean setService(BluetoothGattService service) {
        if(service.getUuid().equals(OTA_SERVICE_UUID)) {
            mService = service;
            return true;
        }
        return false;
    }

    public List<BluetoothGattCharacteristic> getNotifyCharacteristic() {
        return null;
    }

    public boolean readInfo() {
        if(mAppCharac == null || mPatchCharac == null) {
            if(D) Log.e(TAG, "read Version info error with null charac");
            return false;
        }
        if(D) Log.d(TAG, "read Version info.");
        return readDeviceInfo(mAppCharac);
    }

    public String getServiceUUID() {
        return OTA_SERVICE_UUID.toString();
    }

    public String getServiceSimpleName() {
        return "Dfu";
    }

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                mService = gatt.getService(OTA_SERVICE_UUID);
                if(mService == null) {
                    Log.e(TAG, "Dfu service not found");
                    return;
                }else {
                    mPatchCharac = mService.getCharacteristic(OTA_READ_PATCH_CHARACTERISTIC_UUID);
                    if(mPatchCharac == null) {
                        if(D) Log.e(TAG, "Dfu Patch characteristic not found");
                        return;
                    }else {
                        if(D) Log.d(TAG, "Dfu Patch characteristic is found, mPatchCharac: " + mPatchCharac.getUuid());
                    }
                    mAppCharac = mService.getCharacteristic(OTA_READ_APP_CHARACTERISTIC_UUID);
                    if(mAppCharac == null) {
                        if(D) Log.e(TAG, "Dfu App characteristic not found");
                        return;
                    }else {
                        if(D) Log.d(TAG, "Dfu App characteristic is found, mAppCharac: " + mAppCharac.getUuid());
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
            if (status == BluetoothGatt.GATT_SUCCESS){
                if(characteristic.getUuid().equals(OTA_READ_APP_CHARACTERISTIC_UUID)) {
                    if(D) Log.d(TAG, "data = " + Arrays.toString(characteristic.getValue()));
                    byte[] appVersionValue = characteristic.getValue();
                    ByteBuffer wrapped = ByteBuffer.wrap(appVersionValue);
                    wrapped.order(ByteOrder.LITTLE_ENDIAN);
                    mAppValue = wrapped.getShort(0);

                    //mTargetVersionView.setText(String.valueOf(oldFwVersion));
                    if(D) Log.d(TAG, "old firmware version: " + mAppValue + " .getValue=" + Arrays.toString(characteristic.getValue()));
                    if(mPatchCharac != null) {
                        readDeviceInfo(mPatchCharac);
                    }
                }else if(characteristic.getUuid().equals(OTA_READ_PATCH_CHARACTERISTIC_UUID)){
                    byte[] patchVersionValue = characteristic.getValue();
                    ByteBuffer wrapped = ByteBuffer.wrap(patchVersionValue);
                    wrapped.order(ByteOrder.LITTLE_ENDIAN);
                    mPatchValue = wrapped.getShort(0);
                    if(D) Log.d(TAG, "old patch version: " + mPatchValue + " .getValue=" + Arrays.toString(characteristic.getValue()));
                    //here can add read other characteristic
                    mCallback.onVersionRead(mAppValue, mPatchValue);
                }
            }

        }
    };

    private boolean readDeviceInfo(BluetoothGattCharacteristic characteristic) {
        if(D) Log.d(TAG, "read readDeviceinfo:" + characteristic.getUuid().toString());
        if(characteristic != null){
            return mGlobalGatt.readCharacteristic(mBluetoothAddress, characteristic);
        } else {
            if(D) Log.e(TAG, "readDeviceinfo Characteristic is null");
        }
        return false;
    }

    public int getAppValue() {
        return mAppValue;
    }
    public int getPatchValue() {
        return mPatchValue;
    }

    /**
     * Interface required to be implemented by activity
     */
    public static interface OnServiceListener {
        /**
         * Fired when value come.
         *
         * @param appVersion      app Version value
         * @param patchVersion      patch Version value
         *
         */
        public void onVersionRead(int appVersion, int patchVersion);
    }
}
