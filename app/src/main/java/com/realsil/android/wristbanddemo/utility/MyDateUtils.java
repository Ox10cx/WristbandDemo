package com.realsil.android.wristbanddemo.utility;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by rain1_wen on 2016/8/23.
 */
public class MyDateUtils {
    /**
     * 返回当前日期时间字符串<br>
     * 默认格式:yyyy-mm-dd hh:mm:ss
     *
     * @return String 返回当前字符串型日期时间
     */
    public static String getCurrentTime() {
        String returnStr = null;
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date();
        returnStr = f.format(date);
        return returnStr;
    }

    /**
     * 返回当前日期时间字符串<br>
     * 默认格式:yyyymmddhhmmss
     *
     * @return String 返回当前字符串型日期时间
     */
    public static BigDecimal getCurrentTimeAsNumber() {
        String returnStr = null;
        SimpleDateFormat f = new SimpleDateFormat("yyyyMMddHHmmss");
        Date date = new Date();
        returnStr = f.format(date);
        return new BigDecimal(returnStr);
    }

    /**
     * 返回自定义格式的当前日期时间字符串
     *
     * @param format
     *            格式规则
     * @return String 返回当前字符串型日期时间
     */
    public static String getCurrentTime(String format) {
        String returnStr = null;
        SimpleDateFormat f = new SimpleDateFormat(format);
        Date date = new Date();
        returnStr = f.format(date);
        return returnStr;
    }

    /**
     * 返回当前字符串型日期
     *
     * @return String 返回的字符串型日期
     */
    public static String getCurDate() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat simpledateformat = new SimpleDateFormat("yyyy-MM-dd");
        String strDate = simpledateformat.format(calendar.getTime());
        return strDate;
    }

    /**
     * 返回指定格式的字符型日期
     * @param date
     * @param formatString
     * @return
     */
    public static String Date2String(Date date, String formatString) {
        if (date == null) {
            return null;
        }
        SimpleDateFormat simpledateformat = new SimpleDateFormat(formatString);
        String strDate = simpledateformat.format(date);
        return strDate;
    }

    /**
     * 返回当前字符串型日期
     *
     * @param format
     *            格式规则
     *
     * @return String 返回的字符串型日期
     */
    public static String getCurDate(String format) {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat simpledateformat = new SimpleDateFormat(format);
        String strDate = simpledateformat.format(calendar.getTime());
        return strDate;
    }

    /**
     * 返回当前字符串型日期
     *
     * @param format
     *            格式规则
     *
     * @return String 返回的字符串型日期
     */
    public static String getDateFromCalendar(Calendar calendar, String format) {
        SimpleDateFormat simpledateformat = new SimpleDateFormat(format);
        String strDate = simpledateformat.format(calendar.getTime());
        return strDate;
    }

    /**
     * 返回当前字符串型日期指定的Calendar
     *
     * @param format
     *            格式规则
     *
     * @return String 返回的字符串型日期
     */
    public static Calendar getCalendarFromDate(String date, String format) {
        Calendar calendar = Calendar.getInstance();
        DateFormat df = new SimpleDateFormat(format);
        Date daystart = null;    //start_date是类似"2013-02-02"的字符串
        try {
            daystart = df.parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        calendar.setTime(daystart);     //得到的dayc1就是你需要的calendar了

        return calendar;
    }

    /**
     * 将字符串型日期转换为日期型
     *
     * @param strDate
     *            字符串型日期
     * @param srcDateFormat
     *            源日期格式
     * @param dstDateFormat
     *            目标日期格式
     * @return Date 返回的util.Date型日期
     */
    public static Date stringToDate(String strDate, String srcDateFormat, String dstDateFormat) {
        Date rtDate = null;
        Date tmpDate = (new SimpleDateFormat(srcDateFormat)).parse(strDate, new ParsePosition(0));
        String tmpString = null;
        if (tmpDate != null)
            tmpString = (new SimpleDateFormat(dstDateFormat)).format(tmpDate);
        if (tmpString != null)
            rtDate = (new SimpleDateFormat(dstDateFormat)).parse(tmpString, new ParsePosition(0));
        return rtDate;
    }

    public static Date stringToDate(String strDate, String srcDateFormat) {
        Date tmpDate = (new SimpleDateFormat(srcDateFormat)).parse(strDate, new ParsePosition(0));

        return tmpDate;
    }
}
