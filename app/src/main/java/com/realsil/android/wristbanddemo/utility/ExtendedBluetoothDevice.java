package com.realsil.android.wristbanddemo.utility;

import android.bluetooth.BluetoothDevice;

public class ExtendedBluetoothDevice {
	// the maximum rssi
	public static final int NO_RSSI = -1000;
	// bond state
	public static final boolean DEVICE_IS_BONDED = true;
	public static final boolean DEVICE_NOT_BONDED = false;

	public BluetoothDevice device;
	public String name;
	public int rssi;
	public boolean isBonded;
	public boolean isConnect;

	public ExtendedBluetoothDevice(BluetoothDevice device, String name, int rssi, boolean isBonded, boolean isConnect) {
		this.device = device;
		this.name = name;
		this.rssi = rssi;
		this.isBonded = isBonded;
		this.isConnect = isConnect;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof ExtendedBluetoothDevice) {
			final ExtendedBluetoothDevice that = (ExtendedBluetoothDevice) o;
			return device.getAddress().equals(that.device.getAddress());
		}
		return super.equals(o);
	}

	/**
	 * Class used as a temporary comparator to find the device in the List of {@link ExtendedBluetoothDevice}s. This must be done this way, because List#indexOf and List#contains use the parameter's
	 * equals method, not the object's from list. See {@link DeviceListAdapter#findDeviceinBondedList(String)} for example
	 */
	public static class AddressComparator {
		public String address;

		@Override
		public boolean equals(Object o) {
			if (o instanceof ExtendedBluetoothDevice) {
				final ExtendedBluetoothDevice that = (ExtendedBluetoothDevice) o;
				return address.equals(that.device.getAddress());
			}
			return super.equals(o);
		}
	}
}
