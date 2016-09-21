package com.realsil.android.wristbanddemo.utility;

import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.ImageView;

import com.realsil.android.wristbanddemo.constant.ConstantParam;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class FileDownloadUtils {
    private static final String TAG = "FileDownloadUtils";

    /**
     * 通过comment image的网络地址get本地缓存地址
     * @param str
     * @return
     */
    public static String getUniquePath(String str){
        String path = str;
        //Log.d(TAG, "getUniqueImagePath(origin): " + str);
        try {
            path = URLDecoder.decode(str, "utf-8");
            String paths[] = path.split("/");
            String imagePath = paths[paths.length - 1];
            imagePath = ConstantParam.FILE_SAVE_CACHE + getMD5Str(imagePath);
            Log.d(TAG, "getUniquePath(return): " + imagePath);
            return imagePath;
        }catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return path;
    }

    /**
     * 音频地址转义
     * @param str
     * @return
     */
    public static String getURLDecoder(String str) throws UnsupportedEncodingException {
        return URLDecoder.decode(str, "utf-8");
    }

    /*
    * MD5加密
    */
    public static String getMD5Str(String str) {
        MessageDigest messageDigest = null;

        try {
            messageDigest = MessageDigest.getInstance("MD5");

            messageDigest.reset();

            messageDigest.update(str.getBytes("UTF-8"));
        } catch (NoSuchAlgorithmException e) {
            System.out.println("NoSuchAlgorithmException caught!");
            System.exit(-1);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        byte[] byteArray = messageDigest.digest();

        StringBuffer md5StrBuff = new StringBuffer();

        for (int i = 0; i < byteArray.length; i++) {
            if (Integer.toHexString(0xFF & byteArray[i]).length() == 1)
                md5StrBuff.append("0").append(Integer.toHexString(0xFF & byteArray[i]));
            else
                md5StrBuff.append(Integer.toHexString(0xFF & byteArray[i]));
        }
        //16位加密，从第9位到25位
        return md5StrBuff.substring(8, 24).toString().toUpperCase();
    }

    public static boolean checkIsTheHttpString(String url) {
        if(url.startsWith("http://")) {
            return true;
        } else if (url.startsWith("Http://")){
            return true;
        }
        return false;
    }

    public static boolean loadFile(final String url) {

        Log.i(TAG, "fileUrl==" + url);
        final String filePath = getUniquePath(url);
        Log.i(TAG, "file Local Path==" + filePath);
        final File file = new File(filePath);
        if (!file.exists()) {
            return DownloadHelper.download(url, filePath);
        } else {
            Log.i(TAG, "file exists");
        }

        return true;
    }
}
