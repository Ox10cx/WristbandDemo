package com.realsil.android.wristbanddemo.utility;

import android.annotation.TargetApi;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * This class is use to manager gatt connect, to let all the activity have only a callback.
 */
public class GlobalGatt {
    // Log
    private final static String TAG = "GlobalGatt";
    private final static boolean D = true;

    // Callbacks
    // each address have a list of callback.
    //ArrayList<BluetoothGattCallback> mCallbacks = new ArrayList<>();
    private HashMap<String, ArrayList<BluetoothGattCallback>> mCallbacks; // Only allow one callback

    // instance
    private static GlobalGatt mInstance;

    // Bluetooth Manager
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private HashMap<String, BluetoothGatt> mBluetoothGatts;

    // Device info
    private ArrayList<String> mBluetoothDeviceAddresss;

    // for sync gatt callback
    private volatile boolean mGattCallbackCalled;
    private final Object mGattCallbackLock = new Object(); //used for gatt callback
    private static final int MAX_CALLBACK_LOCK_WAIT_TIME = 3000;

    // Connection state
    private HashMap<String, Integer> mConnectionState;
    public static final int STATE_DISCONNECTED = 0;
    public static final int STATE_CONNECTING = 1;
    public static final int STATE_CONNECTED = 2;

    private static Context mContext;

    // UUID
    public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";

    public static void initial(Context context) {
        mInstance = new GlobalGatt();
        mContext = context;

        mInstance.mBluetoothGatts = new HashMap<>();
        mInstance.mConnectionState = new HashMap<>();
        mInstance.mCallbacks = new HashMap<>();
        mInstance.mBluetoothDeviceAddresss = new ArrayList<>();
    }


    /**
     * Get the Global gatt object.
     *
     * <p>It will return a instance.
     *
     *
     * @return The GloabalGatt instance.
     */
    public static GlobalGatt getInstance() {
        return mInstance;
    }

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        if(D) Log.d(TAG, "initialize()");
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        if(mBluetoothAdapter == null) {
            mBluetoothAdapter = mBluetoothManager.getAdapter();
            if (mBluetoothAdapter == null) {
                Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
                return false;
            }
        }
        return true;
    }

    public boolean isConnected(final String address) {
        if(mConnectionState.get(address) == null) {
            if(D) Log.w(TAG, "isConnected, addr: " + address + ", mConnectionState.get(address) == null");
            return false;
        }
        if(D) Log.d(TAG, "isConnected, addr: " + address + ", mConnectionState.get(address): " + mConnectionState.get(address));
        return (mConnectionState.get(address).equals(STATE_CONNECTED));
    }

    public boolean isHostConnected(final String address) {
        if(mBluetoothManager == null) {
            if(D) Log.w(TAG, "isHostConnected, addr: " + address + ", mBluetoothManager == null");
            return false;
        }
        List<BluetoothDevice> lists = mBluetoothManager.getConnectedDevices(BluetoothProfile.GATT);
        if(lists != null) {
            for(BluetoothDevice device: lists) {
                if(device.getAddress().equals(address)) {
                    if(D) Log.d(TAG, "isHostConnected, addr: " + address + ", Connected.");
                    return true;
                }
            }
        }
        if(D) Log.w(TAG, "isHostConnected, addr: " + address + ", Disconnected.");
        return false;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     * @param callback The gatt callback.
     *
     * @return Return true if the connection is initiated successfully. The connection result
     *         is reported asynchronously through the
     *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     *         callback.
     */
    public boolean connect(final String address, final BluetoothGattCallback callback) {
        if (mBluetoothAdapter == null || address == null) {
            if(D) Log.e(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // if connect, an other want connect
        if(mBluetoothDeviceAddresss.contains(address) && isConnected(address)) {
            if(D) Log.d(TAG, "if connect, an other want connect. addr: " + address);
            // register a callback
            registerCallback(address, callback);
            // call the connection state change callback to tell it connect
            callback.onConnectionStateChange(mBluetoothGatts.get(address), BluetoothGatt.GATT_SUCCESS, BluetoothProfile.STATE_CONNECTED);
            return true;
        }

        // Previously connected device. Try to reconnect.
        if ((mBluetoothDeviceAddresss.contains(address)) && (mBluetoothGatts.get(address) != null)) {
            if(D) Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatts.get(address).connect()) {
                // update state
                mConnectionState.put(address, STATE_CONNECTING);
                return true;
            } else {
                return false;
            }
        }

        // register a callback
        registerCallback(address, callback);

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            if(D) Log.e(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        if(D) Log.d(TAG, "Trying to create a new connection.");
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mConnectionState.put(address, STATE_CONNECTING);
        BluetoothGatt gatt = device.connectGatt(mContext, false, new GattCallback());
        mBluetoothGatts.put(address, gatt);
        mBluetoothDeviceAddresss.add(address);

        return true;
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     * @param addr  The device address with want to close gatt
     */
    public void closeBluetoothGatt(final String addr) {
        if(D) Log.d(TAG, "closeBluetoothGatt, addr: " + addr + ", mBluetoothGatts.get(addr): " +  mBluetoothGatts.get(addr));
        if (mBluetoothGatts.get(addr) != null) {
            mBluetoothGatts.get(addr).close();
            mBluetoothGatts.remove(addr);
            mCallbacks.remove(addr);
            
            mBluetoothDeviceAddresss.remove(addr);
        }
    }

    /**
     * When the le services manager close, it must disconnect and close the gatt.
     * @param addr  The device address with want to close
     */
    public void close(final String addr) {
        // disconnect and close the gatt
        disconnectGatt(addr);
        closeBluetoothGatt(addr);
    }
    /**
     * Close all the connect device.
     */
    public void closeAll() {
        if(D) Log.d(TAG, "closeAll, mBluetoothDeviceAddresss.size(): " + mBluetoothDeviceAddresss.size());
        // disconnect and close the gatt
        for(String addr: mBluetoothDeviceAddresss) {
            if(D) Log.d(TAG, "close all of addr: " + addr);
            close(addr);
        }
    }
    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     * @param addr  The device address with want to disconnect
     */
    public void disconnectGatt(final String addr) {
        if(D) Log.d(TAG, "disconnect()");
        if((mBluetoothGatts.get(addr) != null) && (isConnected(addr))) {
            mBluetoothGatts.get(addr).disconnect();
            // wait 500ms for disconnect, here also can use sync method
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param addr  The device address with want to read
     * @param characteristic The characteristic to read from.
     *
     * @return Return true if the read is initiated successfully. The read result
     *         is reported asynchronously through the
     *         {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     *         callback.
     */
    public boolean readCharacteristic(final String addr, BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatts.get(addr) == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return false;
        }
        if(D) Log.d(TAG, "readCharacteristic, addr: " + addr);
        mBluetoothGatts.get(addr).readCharacteristic(characteristic);
        return true;
    }
    /**
     * Request a write on a given {@code BluetoothGattCharacteristic}. The write result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicWrite(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param addr  The device address with want to write
     * @param characteristic The characteristic to write.
     *
     * @return Return true if the write is initiated successfully. The read result
     *         is reported asynchronously through the
     *         {@code BluetoothGattCallback#onCharacteristicWrite(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     *         callback.
     */
    public boolean writeCharacteristic(final String addr, BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatts.get(addr) == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return false;
        }
        if(D) Log.d(TAG, "writeCharacteristic, addr: " + addr);
        mBluetoothGatts.get(addr).writeCharacteristic(characteristic);
        return true;
    }
    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param addr  The device address with want to control notificaiton
     * @param characteristic Characteristic to act on.
     * @param enabled If true, enable notification.  False otherwise.
     *
     * @return Return true if the read is initiated successfully. The read result
     *         is reported asynchronously through the
     *         {@code BluetoothGattCallback#onDescriptorWrite(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattDescriptor, int)}
     *         callback.
     */
    public boolean setCharacteristicNotification(final String addr, BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatts.get(addr) == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return false;
        }
        if(D) Log.d(TAG, "setCharacteristicNotification, addr: " + addr);
        // enable notifications locally
        mBluetoothGatts.get(addr).setCharacteristicNotification(characteristic, enabled);

        // enable notifications on the device
        final BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                UUID.fromString(CLIENT_CHARACTERISTIC_CONFIG));
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        mBluetoothGatts.get(addr).writeDescriptor(descriptor);
        return true;
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}.
     * This is a sync method, only the callback is called, it will return.
     *
     * @param addr  The device address with want to read
     * @param characteristic The characteristic to read from.
     *
     * @return Return true if the read is initiated successfully. The read result
     *         is reported asynchronously through the
     *         {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     *         callback.
     */
    public boolean readCharacteristicSync(final String addr, BluetoothGattCharacteristic characteristic) {
        if(D) Log.d(TAG, "readCharacteristicSync");
        mGattCallbackCalled = false;

        if(readCharacteristic(addr, characteristic) == false) {
            return false;
        }

        synchronized (mGattCallbackLock){
            try {
                // here only wait for 3 seconds
                if (mGattCallbackCalled == false) {
                    if(D) Log.d(TAG, "wait for " + MAX_CALLBACK_LOCK_WAIT_TIME + "ms");
                    mGattCallbackLock.wait(MAX_CALLBACK_LOCK_WAIT_TIME);
                    if(D) Log.d(TAG, "wait time reached");
                }
            } catch (final InterruptedException e) {
                if(D) Log.e(TAG,"readCharacteristicSync Sleeping interrupted, e:" + e);
            }
        }
        return true;
    }
    /**
     * Request a write on a given {@code BluetoothGattCharacteristic}. The write result is reported
     * This is a sync method, only the callback is called, it will return.
     *
     * @param addr  The device address with want to write
     * @param characteristic The characteristic to write.
     *
     * @return Return true if the write is initiated successfully. The read result
     *         is reported asynchronously through the
     *         {@code BluetoothGattCallback#onCharacteristicWrite(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     *         callback.
     */
    public boolean writeCharacteristicSync(final String addr, BluetoothGattCharacteristic characteristic) {
        if(D) Log.d(TAG, "writeCharacteristicSync");
        mGattCallbackCalled = false;

        if(writeCharacteristic(addr, characteristic) == false) {
            return false;
        }

        synchronized (mGattCallbackLock){
            try {
                // here only wait for 3 seconds
                if (mGattCallbackCalled == false) {
                    if(D) Log.d(TAG, "wait for " + MAX_CALLBACK_LOCK_WAIT_TIME + "ms");
                    mGattCallbackLock.wait(MAX_CALLBACK_LOCK_WAIT_TIME);
                    if(D) Log.d(TAG, "wait time reached");
                }
            } catch (final InterruptedException e) {
                if(D) Log.e(TAG,"readCharacteristicSync Sleeping interrupted, e:" + e);
            }
        }
        return true;
    }
    /**
     * Enables or disables notification on a give characteristic.
     * This is a sync method, only the callback is called, it will return.
     *
     * @param addr  The device address with want to control notificaiton
     * @param characteristic Characteristic to act on.
     * @param enabled If true, enable notification.  False otherwise.
     *
     * @return Return true if the read is initiated successfully. The read result
     *         is reported asynchronously through the
     *         {@code BluetoothGattCallback#onDescriptorWrite(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattDescriptor, int)}
     *         callback.
     */
    public boolean setCharacteristicNotificationSync(final String addr, BluetoothGattCharacteristic characteristic,
                                                 boolean enabled) {
        if(D) Log.d(TAG, "setCharacteristicNotificationSync");
        mGattCallbackCalled = false;

        if(setCharacteristicNotification(addr, characteristic, enabled) == false) {
            return false;
        }

        synchronized (mGattCallbackLock) {
            try {
                // here only wait for 3 seconds
                if (mGattCallbackCalled == false) {
                    if(D) Log.d(TAG, "wait for " + MAX_CALLBACK_LOCK_WAIT_TIME + "ms");
                    mGattCallbackLock.wait(MAX_CALLBACK_LOCK_WAIT_TIME);
                    if(D) Log.d(TAG, "wait time reached");

                }
            } catch (final InterruptedException e) {
                if(D) Log.e(TAG,"readCharacteristicSync Sleeping interrupted, e:" + e);
            }
        }
        return true;
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices(final String addr) {
        if (mBluetoothGatts.get(addr) == null)
            return null;

        return mBluetoothGatts.get(addr).getServices();
    }

    public BluetoothGatt getBluetoothGatt(final String addr) {
        return mBluetoothGatts.get(addr);
    }
    public ArrayList<BluetoothDevice> getConnectDevices() {
        ArrayList<BluetoothDevice> devices = new ArrayList<>();
        for(String addr: mBluetoothDeviceAddresss) {
            if(isConnected(addr)) {
                devices.add(getBluetoothGatt(addr).getDevice());
            }
        }
        return devices;
    }
    public String getDeviceName(final String addr) {
        if(mBluetoothGatts.get(addr) == null) {
            if(D) Log.e(TAG, "bluetooth gatt is null, addr: " + addr);
            return null;
        }
        return mBluetoothGatts.get(addr).getDevice().getName();
    }
    /**
     * Clears the device cache. After uploading new firmware the DFU target will have other services than before.
     *
     * @param gatt the GATT device to be refreshed
     */
    private void refreshDeviceCache(final BluetoothGatt gatt) {
        /*
		 * There is a refresh() method in BluetoothGatt class but for now it's hidden. We will call it using reflections.
		 */
        try {
            final Method refresh = gatt.getClass().getMethod("refresh");
            if (refresh != null) {
                final boolean success = (Boolean) refresh.invoke(gatt);
                Log.d(TAG,"Refreshing result: " + success);
            }
        } catch (Exception e) {
            Log.e(TAG, "An exception occured while refreshing device", e);
        }
    }

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    class GattCallback extends BluetoothGattCallback {
        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            if(D) Log.d(TAG, "onMtuChanged new mtu is " + mtu);
            if(D) Log.d(TAG, "onMtuChanged new status is " + String.valueOf(status));
            String addr = gatt.getDevice().getAddress();
            // tell all the callback
            for(BluetoothGattCallback callback: mCallbacks.get(addr)) {
                callback.onMtuChanged(gatt, mtu, status);
            }
        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String addr = gatt.getDevice().getAddress();
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    mConnectionState.put(addr, STATE_CONNECTED);
                    mBluetoothGatts.put(addr, gatt);
                    if(D) Log.d(TAG, "mBluetoothGatts.get(addr) = " + mBluetoothGatts.get(addr) + ". mBluetoothGatts.size(): " + mBluetoothGatts.size() + ", addr: " + addr);
                    Iterator key = mBluetoothGatts.keySet().iterator();
                    while(key.hasNext()) {
                        if(D) Log.e(TAG, "mBluetoothGatts list: " + key.next());
                    }
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    if(D) Log.i(TAG, "Disconnected from GATT server.");
                    mConnectionState.put(addr, STATE_DISCONNECTED);
                }
            }else{
                if(D) Log.e(TAG, "onConnectionStateChange error: status " + status + " newState: " + newState);
                mConnectionState.put(addr, STATE_DISCONNECTED);
            }

            // tell all the callback
            for(BluetoothGattCallback callback: mCallbacks.get(addr)) {
                callback.onConnectionStateChange(gatt, status, newState);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            String addr = gatt.getDevice().getAddress();
            // tell all the callback
            for(BluetoothGattCallback callback: mCallbacks.get(addr)) {
                callback.onServicesDiscovered(gatt, status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            String addr = gatt.getDevice().getAddress();
            // notify waiting thread
            synchronized (mGattCallbackLock){
                mGattCallbackCalled = true;
                mGattCallbackLock.notifyAll();
            }
            // tell all the callback
            for(BluetoothGattCallback callback: mCallbacks.get(addr)) {
                callback.onCharacteristicRead(gatt, characteristic, status);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            String addr = gatt.getDevice().getAddress();
            if(D) Log.d(TAG, "onCharacteristicChanged, addr: " + addr);
            if(D) Log.d(TAG, "onCharacteristicChanged, mCallbacks size: " + mCallbacks.size() + ", mCallbacks.get: " + mCallbacks.get(addr).size());
            // tell all the callback
            for(BluetoothGattCallback callback: mCallbacks.get(addr)) {
                callback.onCharacteristicChanged(gatt, characteristic);
            }
        }

        @Override
        public void onDescriptorWrite(final BluetoothGatt gatt, final BluetoothGattDescriptor descriptor, final int status) {
            String addr = gatt.getDevice().getAddress();
            // notify waiting thread
            synchronized (mGattCallbackLock){
                mGattCallbackCalled = true;
                mGattCallbackLock.notifyAll();
            }
            // tell all the callback
            for(BluetoothGattCallback callback: mCallbacks.get(addr)) {
                callback.onDescriptorWrite(gatt, descriptor, status);
            }
        }

        @Override
        public void onCharacteristicWrite(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, final int status) {
            String addr = gatt.getDevice().getAddress();
            if(D) Log.d(TAG, "onCharacteristicWrite: addr is " + addr);

            // notify waiting thread
            synchronized (mGattCallbackLock){
                mGattCallbackCalled = true;
                mGattCallbackLock.notifyAll();
            }
            // tell all the callback
            for(BluetoothGattCallback callback: mCallbacks.get(addr)) {
                callback.onCharacteristicWrite(gatt, characteristic, status);
            }
        }
    };

    public BluetoothAdapter getBluetoothAdapter() {
        return mBluetoothAdapter;
    }

    public ArrayList<BluetoothGattCallback> getCallback(final String addr) {
        return mCallbacks.get(addr);
    }

    public void registerCallback(final String addr, BluetoothGattCallback callback) {
        // register a callback
        if(mCallbacks.get(addr) == null) {
            if(D) Log.d(TAG, "mCallbacks.get(addr) == null, addr: " + addr);
            ArrayList<BluetoothGattCallback> c = new ArrayList<>();
            c.add(callback);
            mCallbacks.put(addr, c);
            if(D) Log.d(TAG, "mCallbacks.get(addr) = " + mCallbacks.get(addr) + ". mCallbacks.size(): " + mCallbacks.size() + ", addr: " + addr);
            Iterator key = mCallbacks.keySet().iterator();
            while(key.hasNext()) {
                if(D) Log.e(TAG, "mCallbacks list: " + key.next());
            }
            return;
        }

        if(!(mCallbacks.get(addr).contains(callback))) {
            // register a callback
            ArrayList<BluetoothGattCallback> c = mCallbacks.get(addr);
            c.add(callback);
            mCallbacks.put(addr, c);
            if(D) Log.d(TAG, "mCallbacks.get(addr).contains(callback). mCallbacks.size(): " + mCallbacks.size() + ", addr: " + addr);
            Iterator key = mCallbacks.keySet().iterator();
            while(key.hasNext()) {
                if(D) Log.e(TAG, "mCallbacks list: " + key.next());
            }
        }
    }

    public void unRegisterCallback(final String addr, BluetoothGattCallback callback) {
        if(D) Log.d(TAG, "unRegisterCallback, addr: " + addr);
        if(mCallbacks.get(addr) == null) {
            if(D) Log.d(TAG, "unRegisterCallback, mCallbacks.get(addr) == null");
            return;
        }
        // unregister a callback
        if(mCallbacks.get(addr).contains(callback)) {
            if(D) Log.d(TAG, "unRegisterCallback, unregister a callback");
            // unregister a callback
            ArrayList<BluetoothGattCallback> c = mCallbacks.get(addr);
            c.remove(callback);
            mCallbacks.put(addr, c);
        }
    }

    public void unRegisterAllCallback(final String addr) {
        if(D) Log.d(TAG, "unRegisterAllCallback, addr: " + addr);
        if(mCallbacks.get(addr) == null) {
            if(D) Log.d(TAG, "unRegisterAllCallback, mCallbacks.get(addr) == null");
            return;
        }
        mCallbacks.remove(addr);
    }
}
