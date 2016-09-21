package com.realsil.android.wristbanddemo.utility;

import android.util.Log;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by rain1_wen on 2016/8/22.
 */
public class FileUtils {
    private static final String tag = FileUtils.class.getName();

    public FileUtils() {
    }

    public static int getUrlFileSize(String urlFile) {
        try {
            URL e = new URL(urlFile);
            HttpURLConnection conn = (HttpURLConnection)e.openConnection();
            conn.setConnectTimeout(5000);
            conn.setDoInput(true);
            int size = conn.getContentLength();
            conn.disconnect();
            return size;
        } catch (Exception var4) {
            Log.e(tag, "getUrlFileSize", var4);
            return 0;
        }
    }

    public static long getFileSize(String filename) {
        File file = new File(filename);
        long size = 0L;
        if(file.isDirectory()) {
            File[] files = file.listFiles();
            File[] var8 = files;
            int var7 = files.length;

            for(int var6 = 0; var6 < var7; ++var6) {
                File f = var8[var6];
                size += getFileSize(f.getAbsolutePath());
            }
        } else {
            size = file.length();
        }

        return size;
    }

    public static File createDir(String filename) {
        File file = new File(filename);
        if(!file.exists()) {
            file.mkdirs();
        }

        return file;
    }
}
