package com.realsil.android.blehub.dfu;

public abstract class RealsilDfuCallback {
	/* Callback indicating when RealsilDfu client has connected/disconnected to/from a remote
     * RealsilDfu server.
     *
     * @param status the service connection state, true/false
     * @param dfu 	 RealsilDfu object
     */
	public void onServiceConnectionStateChange(boolean status, RealsilDfu dfu) {
    }
	
	/* Callback indicating when OTA process have some error.
     *
     * @param e	 	the error code
     */
	public void onError(int e) {
    }
	
	/* Callback indicating when OTA process success.
    *
    * @param s	 	the success code
    */
	public void onSucess(int s) {
	}
	
	/* Callback indicating when OTA process state changed.
    *
    * @param state	 	the current state
    */
	public void onProcessStateChanged(int state) {
	}
	
	/* Callback indicating when OTA download image progress changed.
    *
    * @param progress	 	the success code
    */
	public void onProgressChanged(int progress) {
	}
}
