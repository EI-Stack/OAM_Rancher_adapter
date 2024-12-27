package solaris.nfm.util;

import java.security.MessageDigest;

/**
 * @author Holisun Wu
 */
public class EncryptUtil
{
	// 設定預設的加密演算法類型。SHA3-256 將會產生長度為 64 的字串
	// 可用的類型參照 https://docs.oracle.com/en/java/javase/11/docs/specs/security/standard-names.html#messagedigest-algorithms
	private static final String	defaultEncryptType		= "SHA3-256";
	private static final String	defaultEncodeCharset	= "UTF-8";

	public static String encryptString(final String sourceString)
	{
		return encryptString(sourceString, defaultEncryptType);
	}

	public static byte[] encryptByteArray(final byte[] sourceByteArray)
	{
		return encryptByteArray(sourceByteArray, defaultEncryptType);
	}

	public static String encryptString(final String str, final String encryptType)
	{
		String result = "";
		try
		{
			result = ByteUtil.toHexString(encryptByteArray(str.getBytes(defaultEncodeCharset), encryptType));
		} catch (Exception e)
		{
			e.printStackTrace();
		}
		return result;
	}

	public static byte[] encryptByteArray(final byte[] sourceByteArray, final String encryptType)
	{
		byte[] result = null;
		try
		{
			MessageDigest messageDigest = MessageDigest.getInstance(encryptType);
			messageDigest.update(sourceByteArray);
			result = messageDigest.digest();
		} catch (Exception e)
		{
			e.printStackTrace();
		}
		return result;
	}

	public static byte[] hexStringToByteArray(final String s)
	{
		int len = s.length();
		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2)
		{
			data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
		}
		return data;
	}

	public static String bytesToHex(final byte[] bytes)
	{
		final char[] hexArray =
			{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
		char[] hexChars = new char[bytes.length * 2];
		int v;
		for (int j = 0; j < bytes.length; j++)
		{
			v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}
}