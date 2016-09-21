package com.realsil.android.wristbanddemo.utility;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;


import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;
import com.realsil.android.wristbanddemo.constant.ConstantParam;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ImageLoadingUtils {
    private static final String TAG = "ImageLoadingUtils";

    /**
     * 通过comment image的网络地址get本地缓存地址
     * @param str
     * @return
     */
    public static String getUniqueImagePath(String str){
        String path = str;
        //Log.d(TAG, "getUniqueImagePath(origin): " + str);
        try {
            path = URLDecoder.decode(str, "utf-8");
            String paths[] = path.split("/");
            String imagePath = paths[paths.length - 1];
            imagePath = ConstantParam.IMAGE_SAVE_CACHE + getMD5Str(imagePath) + ".jpg";
            Log.d(TAG, "getUniqueImagePath(return): " + imagePath);
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

    public static void getImage(final ImageView imageView,final String url, final int defaultImageId) {

        Drawable drawable;
        Log.i(TAG, "imgUrl==" + url);
        if(checkIsTheHttpString(url)) {
            final String imagePath = getUniqueImagePath(url);
            Log.i(TAG, "image Local Path==" + imagePath);
            final File file = new File(imagePath);
            if (!file.exists()) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        DownloadHelper.download(url, imagePath, new DownLoadListener() {
                            @Override
                            public void onSizeChangedListener(int var1, int var2) {
                                Log.i(TAG, "loading success: " + file.exists());
                                updateUi(imageView, imagePath, defaultImageId);
                            }
                        });

                    }
                }).start();
            } else {
                drawable = Drawable.createFromPath(imagePath);

                imageView.setImageDrawable(drawable);
            }
        } else {
            drawable = Drawable.createFromPath(url);
            imageView.setImageDrawable(drawable);
        }
    }

    private static void updateUi(ImageView view, String imagePath, int defaultImageId) {
        Looper looper = Looper.getMainLooper(); //主线程的Looper对象
        //这里以主线程的Looper对象创建了handler，
        //所以，这个handler发送的Message会被传递给主线程的MessageQueue。
        ImageviewUpdateHandler handler = new ImageviewUpdateHandler(looper, view);
        Message msg = handler.obtainMessage();
        msg.arg1 = defaultImageId;
        msg.obj = imagePath;
        handler.sendMessage(msg);
    }

    static class ImageviewUpdateHandler extends Handler {
        ImageView imageView;
        public ImageviewUpdateHandler(Looper looper, ImageView view) {
            super(looper);
            imageView = view;
        }
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            //Log.d("123", "handleMessage");
            String imagePath = (String)msg.obj;
            int defaultImageId = msg.arg1;

            final File file = new File(imagePath);
            if(file.exists()) {
                imageView.setImageDrawable(Drawable.createFromPath(imagePath));
            } else {
                imageView.setImageResource(defaultImageId);
                Log.i(TAG, "loading failed");
            }
        }
    }
}
