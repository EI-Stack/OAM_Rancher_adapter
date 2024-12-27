package solaris.nfm.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;

public class ByteUtil
{
	public static byte[] getHttpServletRequestBody(final HttpServletRequest request) throws IOException
	{
		ServletInputStream sis = request.getInputStream();
		byte[] buffer = new byte[1024];
		ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
		int bytesread = 0;
		while (true)
		{
			bytesread = sis.read(buffer);
			if (bytesread == -1) break;
			baos.write(buffer, 0, bytesread);
		}
		return baos.toByteArray();
	}

	// Converting a string of hex character to bytes
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

	// Converting a bytes array to string of hex character
	public static String byteArrayToHexString(final byte[] b)
	{
		int len = b.length;
		String data = new String();
		for (int i = 0; i < len; i++)
		{
			data += Integer.toHexString((b[i] >> 4) & 0xf);
			data += Integer.toHexString(b[i] & 0xf);
		}
		return data;
	}

	public static String toHexString(final byte[] sourceByteArray)
	{
		StringBuilder hexString = new StringBuilder();
		for (int i = 0; i < sourceByteArray.length; i++)
		{
			String hex = Integer.toHexString(0xFF & sourceByteArray[i]);
			if (hex.length() == 1)
			{
				hexString.append('0');
			}
			hexString.append(hex);
		}
		return hexString.toString().toUpperCase();
	}

	public static String toBinaryString(final byte[] sourceByteArray)
	{
		StringBuilder binaryString = new StringBuilder();
		String tmpString = null;
		for (int i = 0; i < sourceByteArray.length; i++)
		{
			binaryString.append(String.format("%8d", (i + 1) * 8 - 1));
			if (i < (sourceByteArray.length - 1)) binaryString.append(' ');
		}
		binaryString.append('\n');
		for (int i = 0; i < sourceByteArray.length; i++)
		{
			tmpString = Integer.toHexString(0xFF & sourceByteArray[i]);
			if (tmpString.length() == 1)
			{
				binaryString.append("---0---");
				binaryString.append(tmpString);
			} else
			{
				binaryString.append("---");
				binaryString.append(tmpString.charAt(0));
				binaryString.append("---");
				binaryString.append(tmpString.charAt(1));
			}
			if (i < (sourceByteArray.length - 1)) binaryString.append(' ');
		}
		binaryString.append('\n');
		for (int i = 0; i < sourceByteArray.length; i++)
		{
			tmpString = Integer.toBinaryString(0xFF & sourceByteArray[i]);
			if (tmpString.length() < 8)
			{
				for (int j = 0; j < (8 - tmpString.length()); j++)
				{
					binaryString.append('0');
				}
			}
			binaryString.append(tmpString);
			if (i < (sourceByteArray.length - 1)) binaryString.append(' ');
		}
		return binaryString.toString();
	}

	public static byte[] covertShortToByteArray(final short s)
	{
		return new byte[]
				{(byte) ((s & 0xFF00) >> 8), (byte) (s & 0x00FF)};
	}
}
