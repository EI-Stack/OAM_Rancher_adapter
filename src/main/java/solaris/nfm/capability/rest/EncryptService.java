package solaris.nfm.capability.rest;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Hex;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 用途：作為 APP 間 API 調用時， Header/Body 資料加解密
 * 使用 https://tool.lmeee.com/jiami/aes 此線上加解密工具可完全對應
 */
@Service
public class EncryptService
{
	@Value("${solaris.rest.encrypt.key:}")
	String			key;
	private String	iv			= "1234567890123456";                  // IV 長度為 16 個字元
	private String	paddingMode	= "PKCS5Padding";                      // or 'zero', or 'no'

	public String encrypt(String plaintext) throws Exception
	{
		SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(), "AES");
		IvParameterSpec ivParameterSpec = new IvParameterSpec(iv.getBytes());
		Cipher cipher = Cipher.getInstance("AES/CBC/" + paddingMode);
		cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);
		byte[] encryptedData = cipher.doFinal(plaintext.getBytes("UTF-8"));
		String cipherTextHexString = Hex.encodeHexString(encryptedData);  // cipherText 為 HEX 字串，不是 BASE64
		return cipherTextHexString;
	}

	public String decrypt(String encryptedDataHexString) throws Exception
	{
		byte[] encryptedData = Hex.decodeHex(encryptedDataHexString);
		SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(), "AES");
		IvParameterSpec ivParameterSpec = new IvParameterSpec(iv.getBytes());
		Cipher cipher = Cipher.getInstance("AES/CBC/" + paddingMode);
		cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);
		byte[] decryptedData = cipher.doFinal(encryptedData);
		String plaintext = new String(decryptedData, "UTF-8");  // cipherText 為 HEX 字串，不是 BASE64
		return plaintext;
	}

	/**
	 * 從 Header 取出 userId，資料校正後，進行資料解密
	 */
	@Cacheable(cacheNames = "encryptedRestHeaderUserId")
	public Long getUserId(final String userIdString)
	{
		Long userId = -3L;
		try
		{
			if (StringUtils.hasText(userIdString))
			{
				userId = Long.parseLong(decrypt(userIdString.trim()));
			}
		} catch (NumberFormatException e)
		{
			e.printStackTrace();
		} catch (Exception e)
		{
			e.printStackTrace();
		}
		return userId;
	}

	/**
	 * 從 Header 取出 userName，資料校正後，進行資料解密
	 */
	@Cacheable(cacheNames = "encryptedRestHeaderUserName")
	public String getUserName(final String userNameString)
	{
		String userName = "NO-DATA";
		try
		{
			if (StringUtils.hasText(userNameString))
			{
				userName = decrypt(userNameString.trim());
			}
		} catch (NumberFormatException e)
		{
			e.printStackTrace();
		} catch (Exception e)
		{
			e.printStackTrace();
		}
		return userName;
	}

	/**
	 * 從 Header 取出 userIp，資料校正後，進行資料解密
	 */
	@Cacheable(cacheNames = "encryptedRestHeaderUserIp")
	public String getUserIp(final String userIpString)
	{
		String userIp = "NO-DATA";
		try
		{
			if (StringUtils.hasText(userIpString))
			{
				userIp = decrypt(userIpString.trim());
			}
		} catch (NumberFormatException e)
		{
			e.printStackTrace();
		} catch (Exception e)
		{
			e.printStackTrace();
		}
		return userIp;
	}
}