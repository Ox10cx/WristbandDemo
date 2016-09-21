package com.realsil.android.wristbanddemo.gattlayer;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;

import com.realsil.android.wristbanddemo.utility.GlobalGatt;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;


public class GattLayer {
	// Log
	private final static String TAG = "GattLayer";
	private final static boolean D = true;
	
	// Gatt Layer Call
	private GattLayerCallback mCallback;

	// Bluetooth Manager
	private BluetoothGatt mBluetoothGatt;


	// Device info
	private String mBluetoothDeviceAddress;

	// Context
	Context mContext;

	// Global Gatt
	GlobalGatt mGlobalGatt;

	// UUID
	private final static UUID WRISTBAND_SERVICE_UUID = UUID.fromString("000001ff-3c17-d293-8e48-14fe2e4da212");      //ff01
	private final static UUID WRISTBAND_WRITE_CHARACTERISTIC_UUID = UUID.fromString("0000ff02-0000-1000-8000-00805f9b34fb");
	private final static UUID WRISTBAND_NOTIFY_CHARACTERISTIC_UUID = UUID.fromString("0000ff03-0000-1000-8000-00805f9b34fb");
	private final static UUID WRISTBAND_NAME_CHARACTERISTIC_UUID = UUID.fromString("0000ff04-0000-1000-8000-00805f9b34fb");
	/** Client configuration descriptor that will allow us to enable notifications and indications */
	private static final UUID CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

	// Characteristic
	private BluetoothGattCharacteristic mWriteCharacteristic;
	private BluetoothGattCharacteristic mNameCharacteristic;
	private BluetoothGattCharacteristic mNotifyCharacteristic;

	public GattLayer(Context context, GattLayerCallback callback) {
		if(D) Log.d(TAG, "initial.");
		mContext = context;
		// register callback
		mCallback = callback;
		// Global Gatt
		mGlobalGatt = GlobalGatt.getInstance();
	}

	/**
	 * Set the name
	 *
	 * @param name 		the name
	 */
	public void setDeviceName(String name) {
		if(D) Log.d(TAG, "set name, name: " + name);
		if(mNameCharacteristic == null) {
			return;
		}
		// Send the data
		mNameCharacteristic.setValue(name);
		mGlobalGatt.writeCharacteristicSync(mBluetoothDeviceAddress, mNameCharacteristic);
	}
	/**
	 * Get the name
	 *
	 */
	public void getDeviceName() {
		if(D) Log.d(TAG, "getDeviceName");
		if(mNameCharacteristic == null) {
			return;
		}
		// Send the data
		mGlobalGatt.readCharacteristic(mBluetoothDeviceAddress, mNameCharacteristic);
	}
	
	/**
     * Send data
     * 
     * @param data 		the data need to send
     */
	public boolean sendData(byte[] data) {
		if(D) Log.d(TAG, "sendData, data: " + Arrays.toString(data));
		if(mWriteCharacteristic == null) {
			if(D) Log.e(TAG, "sendData error, with mWriteCharacteristic == null.");
			return false;
		}
		if(!mGlobalGatt.isConnected(mBluetoothDeviceAddress)) {
			if(D) Log.e(TAG, "sendData error, with disconnect.");
			return false;
		}
		// Send the data
		mWriteCharacteristic.setValue(data);
		mGlobalGatt.writeCharacteristic(mBluetoothDeviceAddress, mWriteCharacteristic);
		return true;
	}

	/**
	 * Connects to the GATT server hosted on the Bluetooth LE device.
	 *
	 * @param address The device address of the destination device.
	 *
	 * @return Return true if the connection is initiated successfully. The connection result
	 *         is reported asynchronously through the
	 *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
	 *         callback.
	 */
	public boolean connect(final String address) {
		if(D) Log.d(TAG, "connect(), address: " + address);
		mBluetoothDeviceAddress = address;
		return mGlobalGatt.connect(address, mGattCallback);
	}

	/**
	 * When the le services manager close, it must disconnect and close the gatt.
	 */
	public void close() {
		if(D) Log.d(TAG, "close()");
		mGlobalGatt.close(mBluetoothDeviceAddress);
	}
	/**
	 * Disconnects an existing connection or cancel a pending connection. The disconnection result
	 * is reported asynchronously through the
	 * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
	 * callback.
	 */
	public void disconnectGatt( ) {
		if(D) Log.d(TAG, "disconnect()");
		mGlobalGatt.disconnectGatt(mBluetoothDeviceAddress);
	}



	// Implements callback methods for GATT events that the app cares about.  For example,
	// connection change and services discovered.
	private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
		@Override
		public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
			if (status == BluetoothGatt.GATT_SUCCESS) {
				if (newState == BluetoothProfile.STATE_CONNECTED) {
					mBluetoothGatt = gatt;
					if(D) Log.i(TAG, "Connected to GATT server.");
					// Attempts to discover services after successful connection.
					if(D) Log.i(TAG, "Attempting to start service discovery:" +
							mBluetoothGatt.discoverServices());
				} else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
					if(D) Log.i(TAG, "Disconnected from GATT server.");
					// try to close gatt
					close();
					// tell up stack the current connect state
					mCallback.onConnectionStateChange(true, false);
				}
			}else{
				if(D) Log.e(TAG, "onConnectionStateChange error: status " + status + " newState: " + newState);
				// try to close gatt
				close();
				// tell up stack the current connect state
				mCallback.onConnectionStateChange(false, false);
			}
		}

		@Override
		public void onServicesDiscovered(BluetoothGatt gatt, int status) {
			if (status == BluetoothGatt.GATT_SUCCESS) {
				if(D) Log.d(TAG, "onServicesDiscovered success.");
				// set the characteristic
				// initial the service and characteristic
				BluetoothGattService service = gatt.getService(WRISTBAND_SERVICE_UUID);
				if(service == null) {
					// try to disconnect gatt
					disconnectGatt();
					return;
				}
				mWriteCharacteristic = service.getCharacteristic(WRISTBAND_WRITE_CHARACTERISTIC_UUID);
				if(mWriteCharacteristic == null) {
					// try to disconnect gatt
					disconnectGatt();
					return;
				}
				mNameCharacteristic = service.getCharacteristic(WRISTBAND_NAME_CHARACTERISTIC_UUID);
				if(mNameCharacteristic == null) {
					// try to disconnect gatt
					disconnectGatt();
					return;
				}
				mNotifyCharacteristic = service.getCharacteristic(WRISTBAND_NOTIFY_CHARACTERISTIC_UUID);
				if(mNotifyCharacteristic == null) {
					// try to disconnect gatt
					disconnectGatt();
					return;
				}
				// enable notification
				mGlobalGatt.setCharacteristicNotification(mBluetoothDeviceAddress, mNotifyCharacteristic, true);
				// tell up stack the current connect state
				//mCallback.onConnectionStateChange(true, true);
			} else {
				// try to disconnect gatt
				disconnectGatt();
				if(D) Log.e(TAG, "onServicesDiscovered failed: " + status);
			}
		}

		@Override
		public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
			if(D) Log.d(TAG,"onCharacteristicChanged , data length: "+characteristic.getValue().length);
			byte[] data = characteristic.getValue();

			if (characteristic.getUuid().equals(WRISTBAND_NOTIFY_CHARACTERISTIC_UUID)) {
				if(D) Log.d(TAG, "data value:" + Arrays.toString(data));
				// tell up stack a data receive
				mCallback.onDataReceive(data);
			}

		}

		@Override
		public void onCharacteristicWrite(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, final int status) {
			if(D) Log.d(TAG, "onCharacteristicWrite() - :status: " + status + " value: " + Arrays.toString(characteristic.getValue()));
			if (characteristic.getUuid().equals(WRISTBAND_WRITE_CHARACTERISTIC_UUID)) {
				if (status == BluetoothGatt.GATT_SUCCESS) {
					// tell up stack data send right
					mCallback.onDataSend(true);
				} else {
					Log.e(TAG, "Characteristic write error: " + status);     //status:6 : not support
					mCallback.onDataSend(false);
				}
			}
		}
		@Override
		public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
			if(D) Log.d(TAG, "onCharacteristicRead() - :status: " + status + " value: " + Arrays.toString(characteristic.getValue()));
			if (characteristic.getUuid().equals(WRISTBAND_NAME_CHARACTERISTIC_UUID)) {
				String name = characteristic.getStringValue(0);
				if (status == BluetoothGatt.GATT_SUCCESS) {
					// tell up stack data send right
					mCallback.onNameReceive(name);
				}
			}
		}
		@Override
		public void onDescriptorWrite(final BluetoothGatt gatt, final BluetoothGattDescriptor descriptor, final int status) {
			if (status == BluetoothGatt.GATT_SUCCESS) {
				if (CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR_UUID.equals(descriptor.getUuid())) {
					if(descriptor.getCharacteristic().getUuid().equals(mNotifyCharacteristic.getUuid())) {
						boolean enabled = descriptor.getValue()[0] == 1;
						if (enabled) {
							if (D) Log.d(TAG, "onDescriptorWrite() - Notification enabled");
							// tell up stack the current connect state
							mCallback.onConnectionStateChange(true, true);
						} else {
							if (D) Log.e(TAG, "onDescriptorWrite() - Notification  not enabled!!!");
							disconnectGatt();
						}
					}
				}
			} else {
				if(D) Log.e(TAG, "Descriptor write error: " + status);
				disconnectGatt();
			}
		};
	};
}
