package com.realsil.android.wristbanddemo.ShareSdk;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;

import java.io.File;
import java.io.FileOutputStream;

import cn.sharesdk.onekeyshare.OnekeyShare;
import cn.sharesdk.onekeyshare.OnekeyShareTheme;

import static com.mob.tools.utils.BitmapHelper.captureView;

/**
 * Created by Administrator on 2016/5/3.
 */
public class MyShareSdk {
    private OnekeyShare mOnekeyShare;

    public  MyShareSdk() {
        mOnekeyShare = new OnekeyShare();
    }

    @SuppressWarnings("unchecked")
    public void show(Context context) {
        // 不显示编辑页
        mOnekeyShare.setSilent(true);
        // 设置界面为九宫格
        mOnekeyShare.setTheme(OnekeyShareTheme.CLASSIC);
        // 在自动授权时可以禁用SSO方式
        mOnekeyShare.disableSSOWhenAuthorize();
        //QZone分享完之后返回应用时提示框上显示的名称
        mOnekeyShare.setSite("RtkBand");

        mOnekeyShare.show(context);
    }

    /** address是接收人地址，仅在信息和邮件使用，否则可以不提供 */
    public void setAddress(String address) {
        mOnekeyShare.setAddress(address);
    }

    /**
     * title标题，在印象笔记、邮箱、信息、微信（包括好友、朋友圈和收藏）、
     * 易信（包括好友、朋友圈）、人人网和QQ空间使用，否则可以不提供
     */
    public void setTitle(String title) {
        mOnekeyShare.setTitle(title);
    }

    /** titleUrl是标题的网络链接，仅在人人网和QQ空间使用，否则可以不提供 */
    public void setTitleUrl(String titleUrl) {
        mOnekeyShare.setTitleUrl(titleUrl);
    }

    /** text是分享文本，所有平台都需要这个字段 */
    public void setText(String text) {
        mOnekeyShare.setText(text);
    }

    /** 获取text字段的值 */
    public String getText() {
        return mOnekeyShare.getText();
    }

    /** imagePath是本地的图片路径，除Linked-In外的所有平台都支持这个字段 */
    public void setImagePath(String imagePath) {
        mOnekeyShare.setImagePath(imagePath);
    }

    /** imageUrl是图片的网络路径，新浪微博、人人网、QQ空间和Linked-In支持此字段 */
    public void setImageUrl(String imageUrl) {
        mOnekeyShare.setImageUrl(imageUrl);
    }

    /** 设置一个将被截图分享的View , surfaceView是截不了图片的*/
    public void setViewToShare(View viewToShare) {
        mOnekeyShare.setViewToShare(viewToShare);
    }
    /*
    public void setImageUrl(Context context, int imageId) {
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(),
                imageId);
        //Log.e("123", "uri.toString(): " + uri.toString() + "uri.getPath(): " + uri.getPath());
        String savePath = writeBitmapToFile(bitmap);

        setImagePath(savePath);
    }

    private String writeBitmapToFile(Bitmap bitmap) {
        if(bitmap != null) {
            //Bitmap bitmap = Bitmap.createScaledBitmap(model.getThumpBitmap(), 100, 100, false);
            //model.getThumpBitmap().recycle();
            //model.setThumpBitmap(bitmap);
            //ConstantParam.IMAGE_SAVE_CACHE
            //考虑清空
            String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/share/";
            File file = new File(path);
            if(!file.exists()) {
                file.mkdirs();
            }

            String savePath = path + System.currentTimeMillis();
            file = new File(savePath);

            try {
                FileOutputStream e = new FileOutputStream(file);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, e);
                e.flush();
                e.close();
                return savePath;
            } catch (Exception var6) {
                var6.printStackTrace();
            }
        }

        return null;
    }*/

    /**
     * Try to return the absolute file path from the given Uri
     *
     * @param context
     * @param uri
     * @return the file path or null
     */
    public static String getRealFilePath( final Context context, final Uri uri ) {
        if ( null == uri ) return null;
        final String scheme = uri.getScheme();
        String data = null;
        if ( scheme == null )
            data = uri.getPath();
        else if ( ContentResolver.SCHEME_FILE.equals( scheme ) ) {
            data = uri.getPath();
        } else if ( ContentResolver.SCHEME_CONTENT.equals( scheme ) ) {
            Cursor cursor = context.getContentResolver().query( uri, new String[] { MediaStore.Images.ImageColumns.DATA }, null, null, null );
            if ( null != cursor ) {
                if ( cursor.moveToFirst() ) {
                    int index = cursor.getColumnIndex( MediaStore.Images.ImageColumns.DATA );
                    if ( index > -1 ) {
                        data = cursor.getString( index );
                    }
                }
                cursor.close();
            }
        }
        return data;
    }

    protected static String getAbsoluteImagePath(Context context, Uri uri)
    {
        // can post image
        String [] proj={MediaStore.Images.Media.DATA};
        Cursor cursor = context.getContentResolver().query( uri,
                proj, // Which columns to return
                null, // WHERE clause; which rows to return (all rows)
                null, // WHERE clause selection arguments (none)
                null); // Order-by clause (ascending by name)

        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();

        return cursor.getString(column_index);
    }


    /** url在微信（包括好友、朋友圈收藏）和易信（包括好友和朋友圈）中使用，否则可以不提供 */
    public void setUrl(String url) {
        mOnekeyShare.setUrl(url);
    }

    /** filePath是待分享应用程序的本地路劲，仅在微信（易信）好友和Dropbox中使用，否则可以不提供 */
    public void setFilePath(String filePath) {
        mOnekeyShare.setFilePath(filePath);
    }

    /** comment是我对这条分享的评论，仅在人人网和QQ空间使用，否则可以不提供 */
    public void setComment(String comment) {
        mOnekeyShare.setComment(comment);
    }

    /** site是分享此内容的网站名称，仅在QQ空间使用，否则可以不提供 */
    public void setSite(String site) {
        mOnekeyShare.setSite(site);
    }

    /** siteUrl是分享此内容的网站地址，仅在QQ空间使用，否则可以不提供 */
    public void setSiteUrl(String siteUrl) {
        mOnekeyShare.setSiteUrl(siteUrl);
    }

    /** 分享地纬度，新浪微博、腾讯微博和foursquare支持此字段 */
    public void setLatitude(float latitude) {
        mOnekeyShare.setLatitude(latitude);
    }

    /** 分享地经度，新浪微博、腾讯微博和foursquare支持此字段 */
    public void setLongitude(float longitude) {
        mOnekeyShare.setLatitude(longitude);
    }

    /** 设置微信分享的音乐的地址 */
    public void setMusicUrl(String musicUrl) {
        mOnekeyShare.setMusicUrl(musicUrl);
    }
}
