package com.realsil.android.wristbanddemo.applicationlayer;

import java.util.ArrayList;


public class ApplicationLayerSportPacket {

	// Header
	private int mYear;			// 6bits
	private int mMonth;			// 4bits
	private int mDay;			// 5bits
	private int mItemCount;		// 8bits
	
	// Packet Length
	private final static int SPORT_HEADER_LENGTH = 4;

	// Sport Item
	ArrayList<ApplicationLayerSportItemPacket> mSportItems = new ArrayList<ApplicationLayerSportItemPacket>();
	
	public boolean parseData(byte[] data) {
		// check header length
		if(data.length < SPORT_HEADER_LENGTH) {
			return false;
		}
		mYear = (data[0] & 0x7e) >> 1;// here must be care shift operation of negative
		mMonth = ((data[0] & 0x01) << 3)  | (data[1] >> 5) & 0x07;//here must be care shift operation of negative
		mDay = data[1] & 0x1f;//here must be care shift operation of negative
		mItemCount = data[3] & 0xff;
		// check the item length
		if((data.length - SPORT_HEADER_LENGTH) != mItemCount * ApplicationLayerSportItemPacket.SPORT_ITEM_LENGTH) {
			return false;
		}
		for(int i = 0; i < mItemCount; i ++) {
			ApplicationLayerSportItemPacket sportItem = new ApplicationLayerSportItemPacket();
			
			byte[] sportItemData = new byte[ApplicationLayerSportItemPacket.SPORT_ITEM_LENGTH];
			System.arraycopy(data, SPORT_HEADER_LENGTH + i * ApplicationLayerSportItemPacket.SPORT_ITEM_LENGTH, 
					sportItemData, 0, ApplicationLayerSportItemPacket.SPORT_ITEM_LENGTH);
			sportItem.parseData(sportItemData);
			mSportItems.add(sportItem);
		} 
		return true;
	}


	public ArrayList<ApplicationLayerSportItemPacket> getSportItems() {
		return mSportItems;
	}
	public int getYear() {
		return mYear;
	}

	public int getMonth() {
		return mMonth;
	}

	public int getDay() {
		return mDay;
	}

	public int getItemCount() {
		return mItemCount;
	}
}
