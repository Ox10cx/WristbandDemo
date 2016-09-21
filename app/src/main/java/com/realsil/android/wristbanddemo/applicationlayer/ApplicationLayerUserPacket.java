package com.realsil.android.wristbanddemo.applicationlayer;

public class ApplicationLayerUserPacket {

	// Parameters
	private boolean mSex;	// 1bit
	private int mAge;		// 7bits
	private int mHeight;	// 9bits
	private int mWeight;	// 10bits
	
	// Sex Flags
	public final static boolean SEX_MAN 	= true;
	public final static boolean SEX_WOMAN 	= false;
	
	// Packet Length
	private final static int USER_HEADER_LENGTH = 4;
	
	public ApplicationLayerUserPacket(boolean sex, int age, double height, double weight) {
		mSex = sex;
		mAge = age;
		mHeight = (int)(height * 10);// convert to int, Expand ten times
		mWeight = (int)(weight * 10);// convert to int, Expand ten times
	}
	public ApplicationLayerUserPacket(boolean sex, int age, int height, double weight) {
		mSex = sex;
		mAge = age;
		mHeight = (int)(height * 10);// convert to int, Expand ten times
		mWeight = (int)(weight * 10);// convert to int, Expand ten times
	}
	public ApplicationLayerUserPacket(boolean sex, int age, int height, int weight) {
		mSex = sex;
		mAge = age;
		mHeight = (int)(height * 10);// convert to int, Expand ten times
		mWeight = (int)(weight * 10);// convert to int, Expand ten times
	}
	public ApplicationLayerUserPacket(boolean sex, int age, double height, int weight) {
		mSex = sex;// year start from 2000
		mAge = age;
		mHeight = (int)(height * 10);// convert to int, Expand ten times
		mWeight = (int)(weight * 10);// convert to int, Expand ten times
	}
	
	public byte[] getPacket() {
		byte[] data = new byte[USER_HEADER_LENGTH];
		int bHeight = mHeight/5;//Accuracy of 0.5
		int bWeight = mWeight/5;//Accuracy of 0.5
		data[0] = (byte)((mSex==SEX_MAN?0x80:0x00) | mAge);
		data[1] = (byte)(bHeight>>1);
		data[2] = (byte)((bHeight<<7) | (bWeight>>3));
		data[3] = (byte)((bWeight<<5) | 0x00);
        return data;
	}
}
