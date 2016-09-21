package com.realsil.android.wristbanddemo.transportlayer;

import android.util.Log;

import java.util.Arrays;
import java.util.logging.Logger;


public class TransportLayerPacket {
	// Log
	private final static String TAG = "TransportLayerPacket";
	private final static boolean D = true;
	
	// Header
	private byte mMagic;		// 1Byte, bit(0-7)
	private boolean isError;	// 1bit
	private boolean isAck;		// 1bit
	private int mVersion;		// 4bit
	private int mPayloadLength;	// 2Byte
	private int mCRC16;			// 2Byte
	private int mSequenceId;	// 2Byte
	
	// Payload
	private final static int MAX_L1_PAYLOAD_LENGTH = 504;
	private byte[] mPayloadBuffer;
	private int mCurrentPayloadLength = 0;
	
	// Magic
	private final static byte MAGIC_BYTE = (byte) 0xAB;
	
	// Version
	private final static int VERSION_CODE = 0;
	
	// Header Length
	public final static int HEADER_LENGTH = 8;
	
	// Return Code
	public final static int LT_SUCCESS 			= 0; // means operation success 
	public final static int LT_ERROR_ACK 		= 1; // means receive a error ack packet
	public final static int LT_SUCCESS_ACK 		= 2; // means receive a success ack packet
	public final static int LT_FULL_PACKET 		= 3; // means receive a right data packet
	
	public final static int LT_ERROR_MASK 		= 0x0100;
	public final static int LT_LENGTH_ERROR 	= LT_ERROR_MASK | 0; // means the packet length is not right
	public final static int LT_MAGIC_ERROR 		= LT_ERROR_MASK | 1; // means the magic is not right
	public final static int LT_CRC_ERROR 		= LT_ERROR_MASK | 2; // means the packet data crc error
	//public final static int LT_CRC_ERROR 		= LT_ERROR_MASK | 3;
	
	TransportLayerPacket() {
		// create a payload
		mPayloadBuffer = new byte[MAX_L1_PAYLOAD_LENGTH];
		
		// initial the header variable
		mMagic 			= 0x00;
		isError 		= false;
		isAck			= false;
		mVersion		= 0;
		mPayloadLength	= 0; 
		mCRC16			= 0; 
		mSequenceId		= 0; 
	}
	
	

	/**
	 * Use to parse the Transport Layer Packet Header info.
	 * 
	 * @param data the receive packet include header
	 * @return status code
	 * */
	public int parseHeader(byte[] data){
		if(data.length < HEADER_LENGTH) {
			if(D) Log.e(TAG, "Parse Header with length error.");
			return LT_LENGTH_ERROR;
		}
		
		// initial the header variable, BIG_ENDDIAN
		mMagic 			= data[0];
		isError 		= (data[1] & 0x20) != 0;
		isAck			= (data[1] & 0x10) != 0;
		mVersion		= data[1] & 0x0f;
		mPayloadLength	= ((data[2] << 8) | (data[3] & 0xff)) & 0xffff; // here must be care shift operation of negative
		mCRC16			= ((data[4] << 8) | (data[5] & 0xff)) & 0xffff; // here must be care shift operation of negative
		mSequenceId		= ((data[6] << 8) | (data[7] & 0xff)) & 0xffff; // here must be care shift operation of negative
		
		// initial current payload length
		mCurrentPayloadLength = 0;
		
		if(D) Log.d(TAG, "LT payload header is, mMagic: " + mMagic + 
											", isError: " + String.valueOf(isError) + 
											", isAck: " + String.valueOf(isAck) +
											", mVersion: " + mVersion +
											", mPayloadLength: " + mPayloadLength +
											", mCRC16: " + mCRC16 +
											", mSequenceId: " + mSequenceId);
		
		// check magic
		if(mMagic != MAGIC_BYTE) {
			if(D) Log.e(TAG, "Magic error.");
			return LT_MAGIC_ERROR;
		}
		
		// check ack flag
		if(isAck == true) {
			// check error flag
			if(isError == true) {
				if(D) Log.d(TAG, "Receive a error ack.");
				return LT_ERROR_ACK;
			} else {
				if(D) Log.d(TAG, "Receive a success ack.");
				return LT_SUCCESS_ACK;
			}
		}
		
		// if have more data, pase data
		int remainPayloadLength = data.length - HEADER_LENGTH;
		if(remainPayloadLength > 0) {
			if(D) Log.d(TAG, "parse header with remain data");
			byte[] payload = new byte[remainPayloadLength];
			System.arraycopy(data, HEADER_LENGTH, payload, 0, remainPayloadLength);
			return parseData(payload);
		}
		return LT_SUCCESS;
	}
	
	/**
	 * Use to parse the Transport Layer Packet Payload.
	 * 
	 * @param data the receive packet of payload
	 * @return status code
	 * */
	public int parseData(byte[] data){
		int futureLength = mCurrentPayloadLength + data.length;
		// Length check
		if(futureLength > MAX_L1_PAYLOAD_LENGTH || futureLength > mPayloadLength) {
			if(D) Log.e(TAG, "Parse Payload with length error.");
			return LT_LENGTH_ERROR;
		}
		// save the payload info
		System.arraycopy(data, 0, mPayloadBuffer, mCurrentPayloadLength, data.length);
		// update current lenth
		mCurrentPayloadLength = futureLength;
		
		// check CRC only when a integrated packet receive
		if(mCurrentPayloadLength == mPayloadLength) {
			byte[] payload = getRealPayload();
			if(checkCRC(payload) != true) {
				return LT_CRC_ERROR;
			}
			return LT_FULL_PACKET;
		}
		return LT_SUCCESS;
	}
	
	/**
	 * prepare the Transport Layer Packet to send
	 * 
	 * @param data the send payload
	 * @param err error flag
	 * @param ack ack flag
	 * @param ver version info
	 * @param seq sequence number
	 * 
	 * @return the integrated Transport Layer Packet
	 * */
	private static byte[] preparePacket(byte[] data, boolean err, boolean ack, int ver, int seq) {
		int data_len;
		int crc;
		if(data != null) {
			data_len = data.length;
			crc = bd_crc16(data, data_len);
		} else {
			data_len = 0;
			crc = 0;
		}
		byte[] header = new byte[HEADER_LENGTH];
		byte[] send_data = new byte[data_len + HEADER_LENGTH];
		
		header[0] = MAGIC_BYTE;
		header[1] = (byte) ((ack ? (err ? 0x30 : 0x10): 0x00) | (ver & 0x0f));
		header[2] = (byte) ((data_len >> 8) & 0xff);
		header[3] = (byte)((data_len) & 0xff);
		header[4] = (byte)((crc >> 8) & 0xff);
		header[5] = (byte)(crc & 0xff);
		header[6] = (byte)((seq >> 8) & 0xff);
		header[7] = (byte)(seq & 0xff);

		System.arraycopy(header, 0, send_data, 0, HEADER_LENGTH);
        if(data_len > 0) {
            System.arraycopy(data, 0, send_data, HEADER_LENGTH, data_len);
        }
		return send_data;
	}
	
	/**
	 * prepare the Ack Packet to send
	 * 
	 * @param err error flag
	 * @param seq sequence number
	 * 
	 * @return the integrated Transport Layer Packet
	 * */
	public static byte[] prepareAckPacket(boolean err, int seq) {
		return preparePacket(null, err, true, getVersion(), seq);
	}
	
	/**
	 * prepare the Data Packet to send
	 * 
	 * @param data payload of send data
	 * @param seq sequence number
	 * 
	 * @return the integrated Transport Layer Packet
	 * */
	public static byte[] prepareDataPacket(byte[] data, int seq) {
		return preparePacket(data, false, false, getVersion(), seq);
	}
		
	
	public static int getVersion() {
		return VERSION_CODE;
	}
	
	/**
	 * Use to get the real payload with the header's length. only the packet is integrated
	 * 
	 * @return the integrated payload
	 * */
	public byte[] getRealPayload() {
		// check length
		if(mCurrentPayloadLength != mPayloadLength) {
			if(D) Log.e(TAG, "didn't a integrated packet, return null.");
			return null;
		}
		// save the payload info
		byte[] realPayload = new byte[mPayloadLength];
		System.arraycopy(mPayloadBuffer, 0, realPayload, 0, mPayloadLength);
		return realPayload;
	}
	
	
	private boolean checkCRC(byte[] data) {
		int crc;
		crc = bd_crc16(data, data.length);
		if(crc != mCRC16) {
			if(D) Log.e(TAG, "crc check error.");
			return false;
		}
		return true;
	}
	
	
	/*todo :crc check*/
    /** CRC table for the CRC-16. The poly is 0x8005 (x^16 + x^15 + x^2 + 1) */
    private static int[] crc16_table = {
        0x0000, 0xC0C1, 0xC181, 0x0140, 0xC301, 0x03C0, 0x0280, 0xC241,
        0xC601, 0x06C0, 0x0780, 0xC741, 0x0500, 0xC5C1, 0xC481, 0x0440,
        0xCC01, 0x0CC0, 0x0D80, 0xCD41, 0x0F00, 0xCFC1, 0xCE81, 0x0E40,
        0x0A00, 0xCAC1, 0xCB81, 0x0B40, 0xC901, 0x09C0, 0x0880, 0xC841,
        0xD801, 0x18C0, 0x1980, 0xD941, 0x1B00, 0xDBC1, 0xDA81, 0x1A40,
        0x1E00, 0xDEC1, 0xDF81, 0x1F40, 0xDD01, 0x1DC0, 0x1C80, 0xDC41,
        0x1400, 0xD4C1, 0xD581, 0x1540, 0xD701, 0x17C0, 0x1680, 0xD641,
        0xD201, 0x12C0, 0x1380, 0xD341, 0x1100, 0xD1C1, 0xD081, 0x1040,
        0xF001, 0x30C0, 0x3180, 0xF141, 0x3300, 0xF3C1, 0xF281, 0x3240,
        0x3600, 0xF6C1, 0xF781, 0x3740, 0xF501, 0x35C0, 0x3480, 0xF441,
        0x3C00, 0xFCC1, 0xFD81, 0x3D40, 0xFF01, 0x3FC0, 0x3E80, 0xFE41,
        0xFA01, 0x3AC0, 0x3B80, 0xFB41, 0x3900, 0xF9C1, 0xF881, 0x3840,
        0x2800, 0xE8C1, 0xE981, 0x2940, 0xEB01, 0x2BC0, 0x2A80, 0xEA41,
        0xEE01, 0x2EC0, 0x2F80, 0xEF41, 0x2D00, 0xEDC1, 0xEC81, 0x2C40,
        0xE401, 0x24C0, 0x2580, 0xE541, 0x2700, 0xE7C1, 0xE681, 0x2640,
        0x2200, 0xE2C1, 0xE381, 0x2340, 0xE101, 0x21C0, 0x2080, 0xE041,
        0xA001, 0x60C0, 0x6180, 0xA141, 0x6300, 0xA3C1, 0xA281, 0x6240,
        0x6600, 0xA6C1, 0xA781, 0x6740, 0xA501, 0x65C0, 0x6480, 0xA441,
        0x6C00, 0xACC1, 0xAD81, 0x6D40, 0xAF01, 0x6FC0, 0x6E80, 0xAE41,
        0xAA01, 0x6AC0, 0x6B80, 0xAB41, 0x6900, 0xA9C1, 0xA881, 0x6840,
        0x7800, 0xB8C1, 0xB981, 0x7940, 0xBB01, 0x7BC0, 0x7A80, 0xBA41,
        0xBE01, 0x7EC0, 0x7F80, 0xBF41, 0x7D00, 0xBDC1, 0xBC81, 0x7C40,
        0xB401, 0x74C0, 0x7580, 0xB541, 0x7700, 0xB7C1, 0xB681, 0x7640,
        0x7200, 0xB2C1, 0xB381, 0x7340, 0xB101, 0x71C0, 0x7080, 0xB041,
        0x5000, 0x90C1, 0x9181, 0x5140, 0x9301, 0x53C0, 0x5280, 0x9241,
        0x9601, 0x56C0, 0x5780, 0x9741, 0x5500, 0x95C1, 0x9481, 0x5440,
        0x9C01, 0x5CC0, 0x5D80, 0x9D41, 0x5F00, 0x9FC1, 0x9E81, 0x5E40,
        0x5A00, 0x9AC1, 0x9B81, 0x5B40, 0x9901, 0x59C0, 0x5880, 0x9841,
        0x8801, 0x48C0, 0x4980, 0x8941, 0x4B00, 0x8BC1, 0x8A81, 0x4A40,
        0x4E00, 0x8EC1, 0x8F81, 0x4F40, 0x8D01, 0x4DC0, 0x4C80, 0x8C41,
        0x4400, 0x84C1, 0x8581, 0x4540, 0x8701, 0x47C0, 0x4680, 0x8641,
        0x8201, 0x42C0, 0x4380, 0x8341, 0x4100, 0x81C1, 0x8081, 0x4040
    };
    private static int crc16_byte(int crc, byte data)
    {
        int temp = crc16_table[(crc ^ data) & 0xff];
        return (crc >> 8) ^ (temp);
    }

    /**
     * crc16 - compute the CRC-16 for the data buffer
     * @param buffer: data pointer
     * @param len: number of bytes in the buffer
     *
     * @return Returns the updated CRC value.
     */
    private static int bd_crc16(byte[] buffer, int len) {
    	// previous CRC value
    	int crc = 0;
        for(int i=0;i<len;i++) {
            crc = crc16_byte(crc,buffer[i]);
        }
        if(D) Log.d(TAG, "crc: " + crc);
        return crc;
    }
	
    public int getSequenceId() {
		return mSequenceId;
	}

	public void setSequenceId(int sequenceId) {
		mSequenceId = sequenceId;
	}
	
	public int getPayloadLength() {
		return mPayloadLength;
	}

	public void setPayloadLength(int payloadLength) {
		mPayloadLength = payloadLength;
	}
}
