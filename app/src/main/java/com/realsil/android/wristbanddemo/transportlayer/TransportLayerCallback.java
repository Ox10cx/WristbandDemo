package com.realsil.android.wristbanddemo.transportlayer;

public class TransportLayerCallback {
	/** Callback indicating when gatt connected/disconnected to/from a remote device
     * 
     * @param status 	status
     * @param newState 	the new connection State
     */
	public void onConnectionStateChange(final boolean status, final boolean newState) {
    }
	
	/** 
	 * Callback indicating transport layer data send ok or not.
     *
     * @param status	 	the result
     * @param data			the send data
     */
	public void onDataSend(final boolean status, byte[] data) {
    }
	/** 
	 * Callback indicating a transport layer data receive.
     *
     * @param data 	the receive transport packet
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
