package com.realsil.android.wristbanddemo.utility;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.realsil.android.wristbanddemo.constant.ConstantParam;

public class WriteLog 
{
	private boolean D = true;	
	
	private static final String TAG = "WriteLog";
	// LogWrite
	private String LOG_PATH_SDCARD_DIR = ConstantParam.LOG_SAVE_CACHE;  // log file path in sdcard

	private  String mlogName;// log neme
					
	private Process process;
	private Process process2;
	private Process process3;

	private String BTSNOOP_SET_PATH = "/etc/bluetooth/bt_stack.conf";

	private String mBtSnoopFileName;
	private Context mContext;

	//name of log
	public void setName(String name) {
		mlogName = name;
	}

	public void startLog() {
		clearLog();
		createLog();
		
	}

	public void stopLog() {
		if(D) Log.d(TAG, "stopLog()");
		if (process != null) {
			process.destroy();
		}
	}
	
	public WriteLog(Context context) {
		mContext = context;
		init();

		mBtSnoopFileName = getBtsnoopLogFilePath();
	}

	private void init() {
		createLogDir();
	}
	
	// rain1_wen add for save btsnoop log
	public void saveHciLog() {
		String btSnoopFileName = mBtSnoopFileName;
		// TODOWriteLog
		List<String> commandList = new ArrayList<String>();  
        commandList.add("cp");  
        commandList.add(btSnoopFileName);  
        commandList.add(getHcidumpPath());

        try {
			process3 = Runtime.getRuntime().exec(commandList.toArray(new String[commandList.size()]));
			if(D)  Log.v(TAG,"print the -process3--------"+ process3);
			process3.destroy();
		} 
		catch (Exception e) 
		{
			Log.e(TAG,e.getMessage(), e);
		}
        
	}
	public String getHcidumpPath() {
		String logFileName = mlogName + "_btsnoop.cfa";// name
		Log.d(TAG, "Log stored in SDcard, the path is:" + LOG_PATH_SDCARD_DIR + logFileName);
		
		return LOG_PATH_SDCARD_DIR + logFileName;
	}
	/**
	 * Clear the log
	 */
	public void  clearLog() {
		// TODOWriteLog
		List<String> commandList = new ArrayList<String>();  
        commandList.add("logcat");  
        commandList.add("-c");
			
		try {
			process2 = Runtime.getRuntime().exec(commandList.toArray(new String[commandList.size()]));	
			if(D)  Log.v(TAG,"print the -process2--------"+ process2);
			process2.destroy();
		} 
		catch (Exception e) 
		{
			Log.e(TAG,e.getMessage(), e);
		}
	}

	
	/**
	 * write the log
	 */
	public void createLog(){

		String logPath = getLogPath();

		if(D) Log.d(TAG, "createLog(), logPath: " + logPath);

		// TODOWriteLog
		List<String> commandList = new ArrayList<String>();  
        commandList.add("logcat");  
        commandList.add("-f");  
        commandList.add(logPath);
        commandList.add("-v");  
        commandList.add("time");

		try {
			process = Runtime.getRuntime().exec(commandList.toArray(new String[commandList.size()]));	
			if(D)  Log.v(TAG,"print the -process--------"+ process);
		} 
		catch (Exception e) {
			Log.e(TAG, e.getMessage(), e);
		}
	}


	/**
	 * the path of the log file
	 * 
	 * @return
	 */
	public String getLogPath() {
		String logFileName = mlogName + ".log";// name

		return LOG_PATH_SDCARD_DIR + logFileName;
	}

	private String getBtsnoopLogFilePath() {
		String fileName = BTSNOOP_SET_PATH;
		String btsnoopFilePath = "/sdcard/btsnoop_hci.cfa";
		try{

			File urlFile = new File(fileName);
			FileInputStream fin =new FileInputStream(urlFile);


			if(fin != null) {
				InputStreamReader inputStreamReader = null;
				try {
					inputStreamReader = new InputStreamReader(fin, "UTF-8");
				} catch (UnsupportedEncodingException e1) {
					e1.printStackTrace();
				}
				BufferedReader reader = new BufferedReader(inputStreamReader);
				StringBuffer sb = new StringBuffer("");
				String line;
				String specStr = "BtSnoopFileName=";
				try {
					while ((line = reader.readLine()) != null) {
						Log.d(TAG, "getBtsnoopLogFilePath, line: " + line);
						if(line.contains("BtSnoopFileName=")) {
							btsnoopFilePath = line.substring(specStr.length());
							Log.d(TAG, "getBtsnoopLogFilePath, btsnoopFilePath: " + btsnoopFilePath);
							return btsnoopFilePath;
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else {
				Log.e(TAG, "getBtsnoopLogFilePath, fin == null");
			}
		}
		catch(Exception e){
			e.printStackTrace();
		}

		return btsnoopFilePath;
	}

	public static String getString(InputStream inputStream) {
		InputStreamReader inputStreamReader = null;
		try {
			inputStreamReader = new InputStreamReader(inputStream, "UTF-8");
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}
		BufferedReader reader = new BufferedReader(inputStreamReader);
		StringBuffer sb = new StringBuffer("");
		String line;
		try {
			while ((line = reader.readLine()) != null) {
				sb.append(line);
				sb.append("\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return sb.toString();
	}

	/**
	 * make the dir
	 */
	private void createLogDir() 
	{
		File file;
		boolean mkOk;

		file = new File(LOG_PATH_SDCARD_DIR);
		Log.e(TAG, "createLogDir");
		if (!file.isDirectory()) {
			Log.e(TAG, "createLogDir start");
			mkOk = file.mkdirs();
			if (!mkOk) {
				return;
			}
			Log.e(TAG, "createLogDir OK");
		}
	}
	
	//delete log, called after stopLog()
	public boolean deleteLog()
	{
		File f = new File(getLogPath());
		//File e = new File(getHcidumpPath());
		
		return f.delete() ;
		
	}


}
