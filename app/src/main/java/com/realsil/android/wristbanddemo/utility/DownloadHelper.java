package com.realsil.android.wristbanddemo.utility;

import android.util.Log;

import java.io.File;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by rain1_wen on 2016/8/22.
 */
public class DownloadHelper {
    private static final String tag = DownloadHelper.class.getName();

    public DownloadHelper() {
    }

    public static boolean download(String fileUrl, String savePath, DownLoadListener listener) {
        try {
            Log.d("123", "fileUrl: " + fileUrl);
            URL e = new URL(fileUrl);
            Log.d("123", "e.openConnection(): " + e.openConnection().toString());
            HttpURLConnection conn = (HttpURLConnection)e.openConnection();
            conn.setConnectTimeout(5000);
            conn.setDoInput(true);
            int totalSize = conn.getContentLength();
            InputStream inputStream = conn.getInputStream();
            StreamUtils.writeStreamToFile(inputStream, savePath, totalSize, listener);
            return true;
        } catch (Exception var7) {
            Log.e(tag, "download ", var7);
            return false;
        }
    }

    public static boolean download(String fileUrl, String savePath) {
        return download(fileUrl, savePath, (DownLoadListener)null);
    }

    public static boolean downloadWithTemp(String fileUrl, String savePath, DownLoadListener listener) {
        int size = FileUtils.getUrlFileSize(fileUrl);
        int downloadSize = 0;

        try {
            File e = new File(savePath + ".temp");
            if(e.exists()) {
                RandomAccessFile url = new RandomAccessFile(e, "rwd");
                if(url.length() > 0L) {
                    downloadSize = url.readInt();
                }

                url.close();
            }

            Log.i(tag, "downloadWithTemp: file size is " + size + ",download size is" + downloadSize);
            URL url1 = new URL(fileUrl);
            HttpURLConnection conn = (HttpURLConnection)url1.openConnection();
            conn.setConnectTimeout(5000);
            conn.setDoInput(true);
            conn.setRequestProperty("Range", "bytes=" + downloadSize + "-" + size);
            InputStream stream = conn.getInputStream();
            StreamUtils.writeStreamToRandomAccessFile(stream, savePath, size, listener);
            return true;
        } catch (Exception var9) {
            Log.e(tag, "downloadWithTemp", var9);
            return false;
        }
    }

    public static boolean downloadWithTemp(String fileUrl, String savePath) {
        return downloadWithTemp(fileUrl, savePath, (DownLoadListener)null);
    }

}
