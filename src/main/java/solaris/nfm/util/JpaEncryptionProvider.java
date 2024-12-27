package solaris.nfm.util;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class JpaEncryptionProvider // implements EncryptionProvider
{
	private static final String encryptKey = "SwordFish";

	/*
	 * @Override
	 * public String decrypt(final String encryptedPassword)
	 * {
	 * if (encryptedPassword == null || encryptedPassword.equals(""))
	 * {
	 * logger.error("\t The encrypted database password is null or empty string !!");
	 * return null;
	 * }
	 * final byte[] decryptFrom = parseHexStringToByte(encryptedPassword);
	 * final byte[] decryptResult = decrypt(decryptFrom, encryptKey);
	 * return new String(decryptResult);
	 * }
	 * @Override
	 * public String encrypt(final String plainPassword)
	 * {
	 * if (plainPassword == null || plainPassword.equals(""))
	 * {
	 * logger.error("\t The plain database password is null or empty string !!");
	 * return null;
	 * }
	 * final byte[] encryptResult = encrypt(plainPassword, encryptKey);
	 * return parseByteToHexString(encryptResult);
	 * }
	 */
	/**
	 * 加密
	 *
	 * @param content
	 *        需要加密的内容
	 * @param password
	 *        加密密码
	 * @return
	 */
	public static byte[] encrypt(final String content, final String password)
	{
		try
		{
			// KeyGenerator kgen = KeyGenerator.getInstance("AES");
			// kgen.init(128, new SecureRandom(password.getBytes()));
			// SecretKey secretKey = kgen.generateKey();
			final SecretKey secretKey = getSecretKey(password);
			final byte[] enCodeFormat = secretKey.getEncoded();
			final SecretKeySpec key = new SecretKeySpec(enCodeFormat, "AES");
			final Cipher cipher = Cipher.getInstance("AES");// 创建密码器
			final byte[] byteContent = content.getBytes("utf-8");
			cipher.init(Cipher.ENCRYPT_MODE, key);// 初始化
			final byte[] result = cipher.doFinal(byteContent);
			return result; // 加密
		} catch (final NoSuchAlgorithmException e)
		{
			e.printStackTrace();
		} catch (final NoSuchPaddingException e)
		{
			e.printStackTrace();
		} catch (final InvalidKeyException e)
		{
			e.printStackTrace();
		} catch (final UnsupportedEncodingException e)
		{
			e.printStackTrace();
		} catch (final IllegalBlockSizeException e)
		{
			e.printStackTrace();
		} catch (final BadPaddingException e)
		{
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * 解密
	 *
	 * @param content
	 *        待解密内容
	 * @param password
	 *        解密密钥
	 * @return
	 */
	public static byte[] decrypt(final byte[] content, final String password)
	{
		try
		{
			final SecretKey secretKey = getSecretKey(password);
			final byte[] enCodeFormat = secretKey.getEncoded();
			final SecretKeySpec key = new SecretKeySpec(enCodeFormat, "AES");
			final Cipher cipher = Cipher.getInstance("AES");// 创建密码器
			cipher.init(Cipher.DECRYPT_MODE, key);// 初始化
			final byte[] result = cipher.doFinal(content);
			return result; // 加密
		} catch (final NoSuchAlgorithmException e)
		{
			e.printStackTrace();
		} catch (final NoSuchPaddingException e)
		{
			e.printStackTrace();
		} catch (final InvalidKeyException e)
		{
			e.printStackTrace();
		} catch (final IllegalBlockSizeException e)
		{
			e.printStackTrace();
		} catch (final BadPaddingException e)
		{
			e.printStackTrace();
		}
		return null;
	}

	private static SecretKey getSecretKey(final String keyString)
	{
		try
		{
			final KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
			final SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG");
			secureRandom.setSeed(keyString.getBytes("UTF-8"));
			keyGenerator.init(128, secureRandom);
			return keyGenerator.generateKey();
		} catch (final Exception e)
		{
			throw new RuntimeException("Initialize secret key failed !!", e);
		}
	}

	/**
	 * 将二进制转换成16进制
	 *
	 * @param buf
	 * @return
	 */
	public static String parseByteToHexString(final byte buf[])
	{
		final StringBuffer sb = new StringBuffer();
		for (int i = 0; i < buf.length; i++)
		{
			String hex = Integer.toHexString(buf[i] & 0xFF);
			if (hex.length() == 1)
			{
				hex = '0' + hex;
			}
			sb.append(hex.toUpperCase());
		}
		return sb.toString();
	}

	/**
	 * 将16进制转换为二进制
	 *
	 * @param hexStr
	 * @return
	 */
	public static byte[] parseHexStringToByte(final String hexStr)
	{
		if (hexStr.length() < 1) return null;
		final byte[] result = new byte[hexStr.length() / 2];
		for (int i = 0; i < hexStr.length() / 2; i++)
		{
			final int high = Integer.parseInt(hexStr.substring(i * 2, i * 2 + 1), 16);
			final int low = Integer.parseInt(hexStr.substring(i * 2 + 1, i * 2 + 2), 16);
			result[i] = (byte) (high * 16 + low);
		}
		return result;
	}
}
