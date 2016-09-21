package com.realsil.android.wristbanddemo.transportlayer;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.realsil.android.wristbanddemo.utility.StringByteTrans;
import com.realsil.android.wristbanddemo.gattlayer.GattLayer;
import com.realsil.android.wristbanddemo.gattlayer.GattLayerCallback;

import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;


public class TransportLayer {
	// Log
	private final static String TAG = "TransportLayer";
	private final static boolean D = true;
	
	// state control
	private int mState;
	private final static int STATE_NORMAL = 0;
	private final static int STATE_RX = 1;
	private final static int STATE_TX = 2;
	private final static int STATE_WAIT_ACK = 3;
	
	// packet object, use to operate the Transport Packet, only allow one packet to receive in a time.
	private TransportLayerPacket mPacket;
	
	// use to manager current transport layer sequence
	private volatile int mCurrentTxSequenceId;
	private volatile int mLastRxSuquenceId;
	
	// use to manager packet send
	private volatile boolean isAckCome;
	// save the last packet to send
	private byte[] mLastSendPacket;
	
	// retransmit control.
	private int mRetransCounter;
	private final static int MAX_RETRANSPORT_COUNT = 3;
	
	// Thread for unpack send
	private ThreadUnpackSend mUnpackThread;
	private final static int MTU_PAYLOAD_SIZE_LIMIT = 20;
	
	// Use to terminate current sent
	private volatile boolean isTerminateSent;
	private final Object mTerminateLock = new Object();
	
	// Use to manager data send
	private boolean isDataSend;
	private final Object mSendDataLock = new Object();
	private final int MAX_DATA_SEND_WAIT_TIME = 10000;
	
	// Transport Layer Call
	private TransportLayerCallback mCallback;
	
	// Gatt Layer
	private GattLayer mGattLayer;
	
	public TransportLayer(Context context, TransportLayerCallback callback) {
		if(D) Log.d(TAG, "initial");
		// register callback
		mCallback = callback;
				
		// initial receive buffer
		mPacket = new TransportLayerPacket();
		
		// initial the gatt layer
		mGattLayer = new GattLayer(context, mGattCallback);
	}

	/**
	 * Connect to the remote device.
	 * <p>This is an asynchronous operation. Once the operation has been completed, the
	 * {@link TransportLayerCallback#onConnectionStateChange} callback is invoked, reporting the result of the operation.
	 *
	 * @return the operation result
	 *
	 * */
	public boolean connect(String addr) {
		mCurrentTxSequenceId = 1;
		mRetransCounter = 0;
		mLastRxSuquenceId = -1;
		isTerminateSent = false;
		// initial state
		mState = STATE_NORMAL;
		return mGattLayer.connect(addr);
	}

	/**
	 * Close, it will disconnect to the remote.
	 *
	 * @return the operation result
	 *
	 * */
	public void close() {
		if(D) Log.d(TAG, "close()");
		// clear all the wait time.
		stopAckTimer();
		stopRxTimer();
		mGattLayer.close();
	}

	/**
	 * Disconnect, it will disconnect to the remote.
	 *
	 *
	 * */
	public void disconnect() {
		// clear all the wait time.
		stopAckTimer();
		stopRxTimer();
		mGattLayer.disconnectGatt();
	}
	/**
	 * Set the name
	 *
	 * @param name 		the name
	 */
	public void setDeviceName(String name) {
		if(D) Log.d(TAG, "set name, name: " + name);
		mGattLayer.setDeviceName(name);
	}
	/**
	 * Get the name
	 *
	 */
	public void getDeviceName() {
		if(D) Log.d(TAG, "getDeviceName");
		mGattLayer.getDeviceName();
	}
	/**
	 * When the Low Layer receive a packet, it will call this method
	 * 
	 * @param data the receive data
	 * */
	public void receiveData(byte[] data) {
		switch(mState) {
		case STATE_NORMAL:
		case STATE_RX:
			decodeReceiveData(data);
			break;
		case STATE_WAIT_ACK:
			int result = mPacket.parseHeader(data);
			if(D) Log.d(TAG, "receive Data with result: " + result + ", state: " + mState);
			
			switch(result) {
			case TransportLayerPacket.LT_ERROR_ACK:
				if(D) Log.e(TAG, "Receive a Error ACK in wait ack state");
				// update state
				mState = STATE_TX;
				isAckCome = true;
				// retransmit the packet
				retransDataPacket();
				break;
			case TransportLayerPacket.LT_SUCCESS_ACK:
				// update tx sequence id
				mCurrentTxSequenceId ++;
				if(D) Log.e(TAG, "Receive a Success ACK in wait ack state, sequence id: " + mCurrentTxSequenceId);
				// update state
				mState = STATE_NORMAL;
				// stop ack super timer
				stopAckTimer();
				isAckCome = true;
				// tell up stack a packet send success.
				tellUpstackPacketSend(true);
				break;
			case TransportLayerPacket.LT_MAGIC_ERROR:
				// set error ack
				if(D) Log.e(TAG, "receive a magic error in wait ack state.");
				// do nothing
				break;
			default: 
				if(D) Log.e(TAG, "Some error, with result: " + result);
				// send error ack
				sendAckPacketUseThread(true);
				break;
			}
			break;
		case STATE_TX:
			int res = mPacket.parseHeader(data);
			if(D) Log.d(TAG, "receive Data with result: " + res + ", state: " + mState);
			
			switch(res) {
			case TransportLayerPacket.LT_ERROR_ACK:
				if(D) Log.e(TAG, "Receive a Error ACK in tx state");
				// terminate current tx packet
				synchronized(mTerminateLock) {
					isTerminateSent = true;

					// ack is come
					isAckCome = true;
				}
				// may be the ack return very fast.
				stopAckTimer();
				isAckCome = true;

				// retrans tx
				retransDataPacket();
				break;
			case TransportLayerPacket.LT_SUCCESS_ACK:
				// terminate current tx packet
				synchronized(mTerminateLock) {
					isTerminateSent = true;
					// ack is come
					isAckCome = true;

					// update tx sequence id
					mCurrentTxSequenceId ++;
					if(D) Log.e(TAG, "Receive a Success ACK in tx state, maybe is a retransmit packet, or ack come very fast. sequence id: " + mCurrentTxSequenceId);
					// update state
					mState = STATE_NORMAL;
				}

				// may be the ack return very fast.
				// Be careful, callback may do a lot thing
				stopAckTimer();
				isAckCome = true;
				// tell up stack a packet send success.
				tellUpstackPacketSend(true);
				break;
			case TransportLayerPacket.LT_MAGIC_ERROR:
				// set error ack
				if(D) Log.e(TAG, "receive a magic error in tx state.");
				// do nothing
				break;
			default: 
				if(D) Log.e(TAG, "Some error, with result: " + res);
				// send error ack
				sendAckPacketUseThread(true);
				break;
			}
			break;
		}
		
		
		
	}
	
	/**
	 * Send Data packet to the remote. Up stack can use this method to send data.
	 * If last packet didn't send ok, didn't allow send next packet. 
	 * 
	 * @param data the send data
	 * 
	 * */
	public boolean sendData(byte[] data){
		if(D) Log.d(TAG, "send Data with state: " + mState);
		if(mState == STATE_NORMAL) {
			// update state
			mState = STATE_TX;
			// update retrans counter
			mRetransCounter = 0;
			// generate a data packet
			mLastSendPacket = TransportLayerPacket.prepareDataPacket(data, mCurrentTxSequenceId);
			// send data
			sendDataPacket();
			return true;
		}
		
		return false;
	}
	
	private void retransDataPacket() {
		// stop ack timer
		stopAckTimer();
		// check reach the max retrans time
		if(mRetransCounter < MAX_RETRANSPORT_COUNT) {
			mRetransCounter++;
			// send it
			if(D) Log.w(TAG, "retrans send it, state: " + mState + 
							", mLastSendPacket: " + Arrays.toString(mLastSendPacket));
			// send data
			sendDataPacket();
			return;
		}
		if(D) Log.e(TAG, "reach the max retransmint count, mCurrentTxSequenceId: " + mCurrentTxSequenceId);
		// update tx sequence id
		mCurrentTxSequenceId ++;
		// update state
		mState = STATE_NORMAL;
		// tell up stack error.
		tellUpstackPacketSend(false);
	}

	/**
	 * In callback, maybe it will do lot of thing, we make let it work in a thread.
	 * */
	private void tellUpstackPacketSend(final boolean sendOK) {
		if(D) Log.d(TAG, "tellUpstackPacketSend, sendOK: " + sendOK);
		final byte[] appData = new byte[mLastSendPacket.length - TransportLayerPacket.HEADER_LENGTH];
		System.arraycopy(mLastSendPacket, TransportLayerPacket.HEADER_LENGTH, appData, 0, mLastSendPacket.length - TransportLayerPacket.HEADER_LENGTH);
		new Thread(new Runnable() {
			@Override
			public void run() {
				if(D) Log.d(TAG, "tellUpstackPacketSend, call Callback.");
				mCallback.onDataSend(sendOK, appData);
			}
		}).start();
	}
	
	/**
	 * Save the receive data to receive buffer. when state is normal or rx.
	 * 
	 * @param data receive data
	 * 
	 * */
	private void decodeReceiveData(byte[] data) {
		int result;
		if(mState == STATE_NORMAL) {
			// change the state
			mState = STATE_RX;
			if(D) Log.d(TAG, "parse header.");
			// start rx timer
			startRxTimer();
			// parse the header
			result = mPacket.parseHeader(data);
		} else {
			if(D) Log.d(TAG, "parse data.");
			// parse the header
			result = mPacket.parseData(data);
		}
		
		if(D) Log.d(TAG, "receive Data with result: " + result + ", state: " + mState);
		// stop rx timer
		if(result != TransportLayerPacket.LT_SUCCESS) {
			stopRxTimer();
		}
		switch(result) {
		case TransportLayerPacket.LT_ERROR_ACK:
		case TransportLayerPacket.LT_SUCCESS_ACK:
			// something error, return normal mode
			if(D) Log.e(TAG, "Receive a ACK in normal or rx state, return");
			// update state
			mState = STATE_NORMAL;
			break;
		case TransportLayerPacket.LT_FULL_PACKET:
			// check whether a retransmit packet
			if(mPacket.getSequenceId() == mLastRxSuquenceId) {
				if(D) Log.w(TAG, "maybe a retrans packet, send success ack");
				// update state
				mState = STATE_NORMAL;//State change must before ack send.
				// send success ack to remote
				sendAckPacketUseThread(false);
				return;
			}
			// update the last sequence id
			mLastRxSuquenceId = mPacket.getSequenceId();
			// be careful here must run in a new thread, because send ack may call by the GattCallback
			// and the callback must wait the ack send ok.
			new Thread(new Runnable() {
				@Override
				public void run() {
					// tell up stack, send the packet to upstack
					int len = mPacket.getPayloadLength();
					byte[] rcv = new byte[len];
					System.arraycopy(mPacket.getRealPayload(), 0, rcv, 0, len);
					// tell up stack
					if(D) Log.e(TAG, "tell up stack, receive full packet");

					// update state
					mState = STATE_NORMAL;//State change must before ack send.
					// send success ack to remote
					sendAckPacket(false);
					mCallback.onDataReceive(rcv);
				}
			}).start();

			break;
		case TransportLayerPacket.LT_SUCCESS:
			// Only check in the end of a packet
			/*
			// check whether a retransmit packet
			if(mPacket.getSequenceId() == mLastRxSuquenceId) {
				if(D) Log.d(TAG, "Receive a retransmit packet, mPacket.getSequenceId(): " + mPacket.getSequenceId() +
											", mLastRxSuquenceId: " + mLastRxSuquenceId);
				// update state
				mState = STATE_NORMAL;//State change must before ack send.
				// send success ack to remote
				sendAckPacketUseThread(false);
				return;
			}*/
			break;
		case TransportLayerPacket.LT_MAGIC_ERROR:
			// if a magic error occur, just return
			if(D) Log.e(TAG, "Some error when receive data, with result: " + result);
			// update state
			mState = STATE_NORMAL;
			break;
		case TransportLayerPacket.LT_LENGTH_ERROR:
		case TransportLayerPacket.LT_CRC_ERROR:
			// set error ack
			if(D) Log.e(TAG, "Some error when receive data, with result: " + result);
			// update state
			mState = STATE_NORMAL;//State change must before ack send.
			sendAckPacketUseThread(true);
			break;
		default: 
			if(D) Log.e(TAG, "Some error, with result: " + result);
			break;
		}
	}

	/**
	 * Send ACK packet to the remote.
	 *
	 * @param err error ack or a success ack
	 *
	 * */
	private void sendAckPacketUseThread(boolean err){
		if(D) Log.e(TAG, "sendAckPacket, err: " + err + ", UseThread");
		// generate a ack packet
		final byte[] sendByte = TransportLayerPacket.prepareAckPacket(err, mPacket.getSequenceId());

		if(sendByte == null) {
			if(D) Log.e(TAG, "something error with null packet.");
			return;
		}
		//send it;
		if(D) Log.d(TAG, "send it, sendByte: " + Arrays.toString(sendByte));

		// send the data, here we do nothing while the data send error, we just think this operation will be done
		// be careful here must run in a new thread, because send ack may call by the GattCallback
		new Thread(new Runnable() {
			@Override
			public void run() {
				sendGattLayerData(sendByte);
			}
		}).start();

		if(D) Log.d(TAG, "send ack ok");
	}
	
	/**
	 * Send ACK packet to the remote.
	 * 
	 * @param err error ack or a success ack
	 * 
	 * */
	private void sendAckPacket(boolean err){
		if(D) Log.e(TAG, "sendAckPacket, err: " + err);
		// generate a ack packet
		final byte[] sendByte = TransportLayerPacket.prepareAckPacket(err, mPacket.getSequenceId());
		
		if(sendByte == null) {
			if(D) Log.e(TAG, "something error with null packet.");
			return;
		}
		//send it;
		if(D) Log.d(TAG, "send it, sendByte: " + Arrays.toString(sendByte));

		// send the data, here we do nothing while the data send error, we just think this operation will be done
		sendGattLayerData(sendByte);
		
        if(D) Log.d(TAG, "send ack ok");
    }
	
	
	/**
	 * Send Data packet to the remote. it will send the last packet to remote.
	 * 
	 * 
	 * */
	private void sendDataPacket(){
		
		if(mLastSendPacket == null) {
			if(D) Log.e(TAG, "something error with null packet.");
			return;
		}

		//send it;
		if(D) Log.e(TAG, "send it, mLastSendPacket: " + StringByteTrans.byte2HexStr(mLastSendPacket) );
		// send data to the remote device
		mUnpackThread = new ThreadUnpackSend(mLastSendPacket);
		mUnpackThread.start();
		
        if(D) Log.d(TAG, "send data ok");
    }
	
	/**
	 * Send transport packet to gatt layer.
	 * <p>This is an synchronous operation. It will wait the {@link GattLayerCallback#onDataSend} callback is invoked.
	 * 
	 * @param data the send data
	 * 
	 * */
    private boolean sendGattLayerData(byte[] data) {
    	isDataSend = false;
		if(!mGattLayer.sendData(data)) {
			if(D) Log.e(TAG, "sendGattLayerData error.");
			return false;
		}
		
		synchronized(mSendDataLock) {
			if(isDataSend != true) {
				try {
					mSendDataLock.wait(MAX_DATA_SEND_WAIT_TIME);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return isDataSend;
    }
	
	// unpack and send thread
    public class ThreadUnpackSend extends Thread {
    	byte[] sendData;
    	ThreadUnpackSend(byte[] data) {
    		sendData = data;
    	}
    	public void run() {
    		if(D) Log.d(TAG, "ThreadUnpackSend is run");

    		// send data to the remote device
    		if(null != mGattLayer) {
				// initial is terminate send
				isTerminateSent = false;
				// initial is ack come flag.
				isAckCome = false;
    			// unpack the send data, because of the MTU size is limit
    			int length = sendData.length;
    			int unpackCount = 0;
    			byte[] realSendData;
    			do {
    				
    				if(length <= MTU_PAYLOAD_SIZE_LIMIT) {
    					realSendData = new byte[length];
    					System.arraycopy(sendData, unpackCount * MTU_PAYLOAD_SIZE_LIMIT, realSendData, 0, length);
    					
    					// update length value
    		            length = 0;
    				} else {
    					realSendData = new byte[MTU_PAYLOAD_SIZE_LIMIT];
    					System.arraycopy(sendData, unpackCount * MTU_PAYLOAD_SIZE_LIMIT, realSendData, 0, MTU_PAYLOAD_SIZE_LIMIT);
    					
    					// update length value
    		            length = length - MTU_PAYLOAD_SIZE_LIMIT;
    				}
    				// send the data, here we do nothing while the data send error, we just think this operation will be done
    				if(!sendGattLayerData(realSendData)) {
						if(D) Log.e(TAG, "Send data error, may link is loss or gatt initial failed.");
						// update state
						mState = STATE_NORMAL;
						// tell up stack a packet send failed.
						tellUpstackPacketSend(false);
						if(D) Log.d(TAG, "ThreadUnpackSend stop");
						return;
					}
    				
    	            // unpack counter increase
    	            unpackCount++;
    	            
    	            // if need to terminate sent
    	            synchronized(mTerminateLock) {
    	            	if(isTerminateSent) {
							if (D) Log.w(TAG, "something error, terminate current sent, may be ack is come.");
        	            	isTerminateSent = false;
							if(D) Log.d(TAG, "ThreadUnpackSend stop");
							return;
        	            }
    				}
    	            
    			} while(length != 0);
    			// if reach the max tx data
    			if(length == 0) {
    				if(D) Log.d(TAG, "send packet OK, change to wait ack state");
					// make sure ack be changed in receive ack
					synchronized(mTerminateLock) {
						if (isAckCome != true) {
							// update state
							mState = STATE_WAIT_ACK;
							// start wait ack timer
							startAckTimer();
						} else {
							mState = STATE_NORMAL;
						}
					}
    			}
    		}//if(null != mGattLayer)
    		if(D) Log.d(TAG, "ThreadUnpackSend stop");
    	}//run
    }
    // Ack super timer
    private final int MAX_ACK_WAIT_TIME = 5000;
	final Handler mAckHandler = new Handler();
	Runnable mAckSuperTask = new Runnable(){
		@Override
		public void run() {
			// TODO Auto-generated method stub
			if(D) Log.w(TAG, "Wait Ack Timeout");
			// update state
			mState = STATE_TX;
			// retransmit the packet
			retransDataPacket();
		}
	};
	private void startAckTimer(){
		if(D) Log.d(TAG, "startAckTimer()");
		synchronized (mAckHandler) {
			mAckHandler.postDelayed(mAckSuperTask, MAX_ACK_WAIT_TIME);
			if(D) Log.d(TAG, "mAckHandler.postDelayed");
		}
	}
	private void stopAckTimer() {
		if(D) Log.d(TAG, "stopAckTimer()");
		synchronized (mAckHandler) {
			mAckHandler.removeCallbacks(mAckSuperTask);
			if(D) Log.d(TAG, "mAckHandler.removeCallbacks");
		}
	}
	/*
    private Timer mAckSuperTimer;
    private TimerTask mAckSuperTimerTask;
    private void startAckTimer(){
		if(D) Log.e(TAG, "startAckTimer()");
        if (mAckSuperTimer == null) {  
        	mAckSuperTimer = new Timer();  
        }  
  
        if (mAckSuperTimerTask == null) {  
        	mAckSuperTimerTask = new TimerTask() {  
                @Override  
                public void run() {  
                	if(D) Log.w(TAG, "Wait Ack Timeout");
                	// update state
                	mState = STATE_TX;
                	// retransmit the packet
                	retransDataPacket();
                }  
            };  
        }  
  
        if(mAckSuperTimer != null && mAckSuperTimerTask != null ){
        	mAckSuperTimer.schedule(mAckSuperTimerTask, MAX_ACK_WAIT_TIME); 	
        }
  
    } 
    private void stopAckTimer(){  
		if(D) Log.e(TAG, "stopAckTimer()");
        if (mAckSuperTimer != null) {
			if(D) Log.e(TAG, "mAckSuperTimer != null");
        	mAckSuperTimer.cancel(); 
        	mAckSuperTimer = null;
        }  
  
        if (mAckSuperTimerTask != null) {
			if(D) Log.e(TAG, "mAckSuperTimerTask != null");
        	mAckSuperTimerTask.cancel();
        	mAckSuperTimerTask = null;
        }
    } */


    // Rx super timer
    private final int MAX_RX_WAIT_TIME = 30000;
	final Handler mRxHandler = new Handler();
	Runnable mRxSuperTask = new Runnable(){
		@Override
		public void run() {
			// TODO Auto-generated method stub
			if(D) Log.w(TAG, "Rx Packet Timeout");
			// update state
			mState = STATE_NORMAL;
			// send error ack to remote
			sendAckPacket(true);
			// stop timer
			stopRxTimer();
		}
	};
	private void startRxTimer(){
		if(D) Log.d(TAG, "startRxTimer()");
		synchronized (mRxHandler) {
			mRxHandler.postDelayed(mRxSuperTask, MAX_RX_WAIT_TIME);
			if(D) Log.d(TAG, "mRxHandler.postDelayed");
		}
	}
	private void stopRxTimer() {
		if(D) Log.d(TAG, "stopRxTimer()");
		synchronized (mRxHandler) {
			mRxHandler.removeCallbacks(mRxSuperTask);
			if(D) Log.d(TAG, "mRxHandler.removeCallbacks");
		}
	}
	/*
	private Timer mRxSuperTimer;
	private TimerTask mRxSuperTimerTask;
    private void startRxTimer() {  
        if (mRxSuperTimer == null) {  
        	mRxSuperTimer = new Timer();  
        }  
  
        if (mRxSuperTimerTask == null) {  
        	mRxSuperTimerTask = new TimerTask() {  
                @Override  
                public void run() {  
                	if(D) Log.w(TAG, "Rx Packet Timeout");
                	// update state
                	mState = STATE_NORMAL;
                	// send error ack to remote
                	sendAckPacket(true);
                	// stop timer
                	stopRxTimer();
                }  
            };  
        }  
  
        if(mRxSuperTimer != null && mRxSuperTimerTask != null ){
        	mRxSuperTimer.schedule(mRxSuperTimerTask, MAX_RX_WAIT_TIME); 	
        }
  
    } 
    private void stopRxTimer() {  
        
        if (mRxSuperTimer != null) {  
        	mRxSuperTimer.cancel(); 
        	mRxSuperTimer = null;
        }  
  
        if (mRxSuperTimerTask != null) {  
        	mRxSuperTimerTask.cancel();
        	mRxSuperTimerTask = null;
        }
    } */
    
    
    GattLayerCallback mGattCallback = new GattLayerCallback() {
    	@Override
		public void onConnectionStateChange(final boolean status, final boolean newState) {
    		if(D) Log.d(TAG, "onConnectionStateChange, status: " + status + ", newState: " + newState);
			mCallback.onConnectionStateChange(status, newState);
        }
		@Override
    	public void onDataSend(final boolean status) {
    		if(D) Log.d(TAG, "onDataSend, status: " + status);
    		synchronized(mSendDataLock) {
    			isDataSend = true;
    			mSendDataLock.notifyAll();
			}
        }
		@Override
		public void onDataReceive(final byte[] data) {
			if(D) Log.d(TAG, "onDataReceive()");
			// be careful send ack may call by the GattCallback
			receiveData(data);
		}
		@Override
		public void onNameReceive(final String data) {
			mCallback.onNameReceive(data);
		}
	};
}
