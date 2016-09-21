package com.realsil.android.wristbanddemo.utility;


import com.realsil.android.wristbanddemo.applicationlayer.ApplicationLayerAlarmPacket;
import com.realsil.android.wristbanddemo.applicationlayer.ApplicationLayerFacSensorPacket;
import com.realsil.android.wristbanddemo.greendao.SleepData;
import com.realsil.android.wristbanddemo.greendao.SportData;

public class WristbandManagerCallback {
	/**
	 * Callback indicating when gatt connected/disconnected to/from a remote device
	 *
	 * @param status status
	 */
	public void onConnectionStateChange(final boolean status) {
	}

	/**
	 * Callback indicating when login in to a wristband
	 *
	 * @param state state
	 */
	public void onLoginStateChange(final int state) {
	}

	/**
	 * Callback indicating something error
	 *
	 * @param error error code
	 */
	public void onError(final int error) {
	}

	/**
	 * Callback indicating a sport data receive.
	 *
	 * @param data the receive data
	 */
	public void onSportDataReceive(SportData data) {
	}

	/**
	 * Callback indicating a sleep data receive.
	 *
	 * @param data the receive transport packet
	 */
	public void onSleepDataReceive(SleepData data) {
	}

	/**
	 * Callback indicating a alarm data receive.
	 *
	 * @param data the receive alarm data packet
	 */
	public void onAlarmDataReceive(ApplicationLayerAlarmPacket data) {
	}

	/**
	 * Callback indicating notify mode setting receive.
	 *
	 * @param data the current notify mode
	 */
	public void onNotifyModeSettingReceive(byte data) {
	}

	/**
	 * Callback indicating notify mode setting receive.
	 *
	 * @param data the current long sit mode
	 */
	public void onLongSitSettingReceive(byte data) {
	}

	/**
	 * Callback indicating a fac sensor data receive.
	 *
	 * @param data the receive sensor data
	 */
	public void onFacSensorDataReceive(ApplicationLayerFacSensorPacket data) {
	}

	/**
	 * Callback indicating version read.
	 *
	 * @param appVersion   app Version value
	 * @param patchVersion patch Version value
	 */
	public void onVersionRead(int appVersion, int patchVersion) {
	}
	/**
	 * Callback indicating name receive.
	 *
	 * @param data	 	the receive data
	 */
	public void onNameRead(final String data) {
	}
	/**
	 * Callback indicating battery read.
	 *
	 * @param value   battery level value
	 */
	public void onBatteryRead(int value) {
	}
}
