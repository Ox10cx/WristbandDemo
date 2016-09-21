package com.realsil.android.wristbanddemo.gattlayer;

public abstract class GattLayerCallback {
	/** Callback indicating when gatt connected/disconnected to/from a remote device
     * 
     * @param status 	status
     * @param newState 	the new connection State
     */
	public void onConnectionStateChange(final boolean status, final boolean newState) {
    }
	
	/** 
	 * Callback indicating gatt layer data send ok or not.
     *
     * @param status	 	the status code
     */
	public void onDataSend(final boolean status) {
    }

	/**
	 * Callback indicating gatt layer a data receive.
	 *
	 * @param data	 	the receive data
	 */
	public void onDataReceive(final byte[] data) {
	}
	/**
	 * Callback indicating name receive.
	 *
	 * @param data	 	the receive data
	 */
	public void onNameReceive(final String data) {
	}
}
