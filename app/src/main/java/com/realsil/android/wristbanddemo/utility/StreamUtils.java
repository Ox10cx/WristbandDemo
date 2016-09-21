package com.realsil.android.wristbanddemo.utility;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetManager;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by rain1_wen on 2016/8/22.
 */
public class StreamUtils {
    private static final String tag = StreamUtils.class.getName();

    public StreamUtils() {
    }

    public static void writeStreamToFile(InputStream stream, String savePath, int totalSize, DownLoadListener listener) throws Exception {
        if(stream != null && !TextUtils.isEmpty(savePath)) {
            File file = new File(savePath);
            FileOutputStream fos = new FileOutputStream(file);
            boolean len = false;
            int downloadSize = 0;
            byte[] buf = new byte[1024];

            int len1;
            while((len1 = stream.read(buf)) != -1) {
                fos.write(buf, 0, len1);
                if(listener != null && totalSize > 0) {
                    downloadSize += len1;
                    listener.onSizeChangedListener((int)((float)downloadSize / (float)totalSize * 100.0F), downloadSize);
                }
            }

            fos.flush();
            fos.close();
            stream.close();
        }
    }

    public static void writeStreamToFile(InputStream stream, String savePath) throws Exception {
        writeStreamToFile(stream, savePath, 0, (DownLoadListener)null);
    }

    public static void writeStreamToRandomAccessFile(InputStream stream, String savePath, int totalSize, DownLoadListener listener) throws Exception {
        if(stream != null && !TextUtils.isEmpty(savePath) && totalSize >= 1) {
            RandomAccessFile randomAccessFile = new RandomAccessFile(savePath, "rwd");
            randomAccessFile.setLength((long)totalSize);
            String tempFilePath = savePath + ".temp";
            File file = new File(tempFilePath);
            int downloadSize = 0;
            RandomAccessFile tempAccessFile = new RandomAccessFile(tempFilePath, "rwd");
            if(file.exists()) {
                if(tempAccessFile.length() > 0L) {
                    downloadSize = tempAccessFile.readInt();
                }

                randomAccessFile.seek((long)downloadSize);
            }

            if(listener != null) {
                listener.onSizeChangedListener((int)((float)downloadSize / (float)totalSize * 100.0F), downloadSize);
            }

            boolean len = false;
            byte[] buf = new byte[1024];

            int len1;
            while((len1 = stream.read(buf)) != -1) {
                randomAccessFile.write(buf, 0, len1);
                downloadSize += len1;
                tempAccessFile.seek(0L);
                tempAccessFile.writeInt(downloadSize);
                if(listener != null && totalSize > 0) {
                    listener.onSizeChangedListener((int)((float)downloadSize / (float)totalSize * 100.0F), downloadSize);
                }
            }

            randomAccessFile.close();
            tempAccessFile.close();
            if(downloadSize == totalSize) {
                file.delete();
                Log.i(tag, "writeStreamToRandomAccessFile:delete file " + tempFilePath);
            }

        }
    }

    public static String convertStreamToString(InputStream stream) throws IOException {
        if(stream == null) {
            return null;
        } else {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            boolean len = false;
            byte[] buf = new byte[1024];

            int len1;
            while((len1 = stream.read(buf)) != -1) {
                bos.write(buf, 0, len1);
            }

            bos.flush();
            byte[] stringInfo = bos.toByteArray();
            return new String(stringInfo);
        }
    }

    public static InputStream getWebStream(String webUrl) {
        try {
            URL e = new URL(webUrl);
            HttpURLConnection conn = (HttpURLConnection)e.openConnection();
            conn.setConnectTimeout(5000);
            conn.setDoInput(true);
            return conn.getInputStream();
        } catch (Exception var3) {
            var3.printStackTrace();
            Log.e(tag, "getWebStream", var3);
            return null;
        }
    }

    public static InputStream getInputStreamFromAssets(Context context, String name) {
        AssetManager manager = context.getAssets();
        InputStream inputStream = null;

        try {
            inputStream = manager.open(name);
        } catch (IOException var5) {
            var5.printStackTrace();
            Log.e(tag, "getInputStreamFromAssets", var5);
        }

        return inputStream;
    }

    public static InputStream getInputStreamFromUri(Context context, Uri uri) {
        ContentResolver resolver = context.getContentResolver();
        InputStream is = null;

        try {
            is = resolver.openInputStream(uri);
        } catch (FileNotFoundException var5) {
            var5.printStackTrace();
            Log.e(tag, "getInputStreamFromUri", var5);
        }

        return is;
    }
}
