package com.realsil.android.wristbanddemo.utility;

import java.io.UnsupportedEncodingException;

import android.util.Log;

public class StringByteTrans {
	/**
	 * Convert ascii string to hexadecimal string
	 * @param str ASCII string
	 * @return String Each Byte space between space, such as: [61 6c 6b]
	 */
	public static String str2HexStr(String str)
	{

		char[] chars = "0123456789ABCDEF".toCharArray();
		StringBuilder sb = new StringBuilder("");
		byte[] bs = str.getBytes();
		int bit;

		for (int i = 0; i < bs.length; i++)
		{
			bit = (bs[i] & 0x0f0) >> 4;
			sb.append(chars[bit]);
			bit = bs[i] & 0x0f;
			sb.append(chars[bit]);
			sb.append(' ');
		}
		return sb.toString().trim();
	}

	/**
	 * Convert hexadecimal string to ascii string
	 * @param hexStr No separators between Byte string (Byte Such as: [6b616c])
	 * @return String The corresponding string
	 */
	public static String hexStr2Str(String hexStr)
	{
		String str = "0123456789ABCDEF";
		hexStr = hexStr.toUpperCase();
		char[] hexs = hexStr.toCharArray();

		byte[] bytes = new byte[hexStr.length() / 2];
		// judge the input string good or not
		if (hexStr.length() % 2 == 1) {
			return new String();
		}
		// judge the input string good or not
		for(int i = 0; i < hexStr.length(); i++) {
			if((hexs[i] >= '0' && hexs[i] <= '9') || (hexs[i] >= 'A' && hexs[i] <= 'F')) {
			} else {
				return new String();
			}
		}
		int n;

		for (int i = 0; i < bytes.length; i++)
		{
			n = str.indexOf(hexs[2 * i]) * 16;
			n += str.indexOf(hexs[2 * i + 1]);
			bytes[i] = (byte) (n & 0xff);
		}
		return new String(bytes);
	}


	/**
	 * Bytes convert to hexadecimal string
	 * @param b byte array
	 * @return String Space separated between each Byte value
	 */
	public static String byte2HexStr(byte[] b)
	{
		String stmp="";
		StringBuilder sb = new StringBuilder("");
		for (int n=0;n<b.length;n++)
		{
			stmp = Integer.toHexString(b[n] & 0xFF);
			sb.append((stmp.length()==1)? "0"+stmp : stmp);
			sb.append(" ");
		}
		return sb.toString().toUpperCase().trim();
	}
	/**
	 * Convert hexadecimal string to byte array
	 * @param src Byte string, there is no separator between each Byte
	 * @return byte[] The corresponding byte array
	 */
	public static byte[] hexStringToByteArray(String src) {
		int len = src.length();
		byte[] data = new byte[len/2];
		src = src.toUpperCase();
		char[] hexs = src.toCharArray();
		// judge the input string good or not
		if (len % 2 == 1) {
			return null;
		}
		// judge the input string good or not
		for(int i = 0; i < len; i++) {
			if((hexs[i] >= '0' && hexs[i] <= '9') || (hexs[i] >= 'A' && hexs[i] <= 'F')) {
			} else {
				return null;
			}
		}
		for(int i = 0; i < len; i+=2){
			data[i/2] = (byte) ((Character.digit(src.charAt(i), 16) << 4) + Character.digit(src.charAt(i+1), 16));
		}

		return data;
	}

	/**
	 * ASCII string convert to Byte array
	 * @param str ascii string
	 * @return byte[]
	 */
	public static byte[] Str2Bytes(String str)
	{
		if (str == null) {
			throw new IllegalArgumentException(
					"Argument str ( String ) is null! ");
		}
		byte[] b = new byte[str.length() / 2];

		try {
			b = str.getBytes("US-ASCII");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return b;
	}

	/**
	 * bytes array convert to a normal string corresponding character (ASCII)
	 *
	 * @param bytearray
	 *            byte[]
	 * @return String
	 */
	public static String Byte2String(byte[] bytearray) {
		String result = "";
		char temp;

		int length = bytearray.length;
		for (int i = 0; i < length; i++) {
			temp = (char) bytearray[i];
			result += temp;
		}
		return result;
	}

	/**
	 * String into a unicode String
	 * @param  strText The Angle of the string
	 * @return String No separator between each unicode
	 * @throws Exception
	 */
	public static String strToUnicode(String strText)
			throws Exception
	{
		char c;
		StringBuilder str = new StringBuilder();
		int intAsc;
		String strHex;
		for (int i = 0; i < strText.length(); i++)
		{
			c = strText.charAt(i);
			intAsc = (int) c;
			strHex = Integer.toHexString(intAsc);
			if (intAsc > 128)
				str.append("\\u" + strHex);
			else // 00 low in the front
				str.append("\\u00" + strHex);
		}
		return str.toString();
	}

	/**
	 * Unicode String into a String
	 * @param hex Hexadecimal values string (a unicode 2 byte)
	 * @return String The Angle of the string
	 */
	public static String unicodeToString(String hex)
	{
		int t = hex.length() / 6;
		StringBuilder str = new StringBuilder();
		for (int i = 0; i < t; i++)
		{
			String s = hex.substring(i * 6, (i + 1) * 6);
			// Need to catch up on high 00 turn again
			String s1 = s.substring(2, 4) + "00";
			// Low directly
			String s2 = s.substring(4);
			// The hexadecimal string to an int
			int n = Integer.valueOf(s1, 16) + Integer.valueOf(s2, 16);
			// To convert an int to characters
			char[] chars = Character.toChars(n);
			str.append(new String(chars));
		}
		return str.toString();
	}
}
