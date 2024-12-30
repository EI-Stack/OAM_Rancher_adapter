package solaris.nfm.util;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import javax.crypto.Cipher;

/**
 * @author Holisun Wu
 *         RSA 非對稱加解密工具，適用於兩種情境
 *         1. 私鑰加密，公鑰解密
 *         2. 公鑰加密，私鑰解密
 */
public class RsaUtil
{
	// RSA 演算法常數，用於生成 RSA 金鑰對與進行加解密操作
	private static final String RSA_ALGORITHM = "RSA";
	// RSA 金鑰位元長度
	private static final Integer	RSA_KEY_SIZE	= 2048;

	/**
	 * 產生 RSA 金鑰對
	 *
	 * @return KeyPair 包含公鑰和私鑰的金鑰對
	 * @throws NoSuchAlgorithmException
	 *         如果 RSA 算法不可用，抛出此例外
	 */
	public static KeyPair createKeyPair() throws NoSuchAlgorithmException
	{
		// 實例化一個金鑰對產生器，指定算法為 RSA
		KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(RSA_ALGORITHM);
		// 初始化金鑰對生成器，指定金鑰長度
		keyPairGenerator.initialize(RSA_KEY_SIZE);
		// 生成金鑰對
		return keyPairGenerator.generateKeyPair();
	}

	public static String getPublicKeyWithBase64(KeyPair keyPair)
	{
		return getKeyWithBase64(keyPair.getPublic());
	}

	public static String getPrivateKeyWithBase64(KeyPair keyPair)
	{
		return getKeyWithBase64(keyPair.getPrivate());
	}

	public static String getKeyWithBase64(Key key)
	{
		return Base64.getEncoder().encodeToString(key.getEncoded());
	}

	public static PublicKey importPublicKey(String publicKeyWithBase64) throws NoSuchAlgorithmException, InvalidKeySpecException
	{
		X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(Base64.getDecoder().decode(publicKeyWithBase64));
		KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
		return keyFactory.generatePublic(x509EncodedKeySpec);
	}

	public static PrivateKey importPrivateKey(String privateKeyWithBase64) throws NoSuchAlgorithmException, InvalidKeySpecException
	{
		PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateKeyWithBase64));
		KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
		return keyFactory.generatePrivate(pkcs8EncodedKeySpec);
	}

	/**
	 * 使用金鑰加密字串
	 *
	 * @param key
	 *        可以是公鑰或是私鑰，用於加密字串
	 * @param plainText
	 *        待加密的明文字串
	 * @return String 加密後的字串，為 Base64 格式
	 * @throws Exception
	 *         如果加密過程中發生錯誤，抛出此例外
	 */
	public static String encryptString(Key key, String plainText) throws Exception
	{
		byte[] encryptedBytes = encryptByteArray(key, plainText.getBytes(StandardCharsets.UTF_8));
		return Base64.getEncoder().encodeToString(encryptedBytes);
	}

	public static byte[] encryptByteArray(Key key, byte[] bytes) throws Exception
	{
		Cipher cipher = Cipher.getInstance(RSA_ALGORITHM);
		cipher.init(Cipher.ENCRYPT_MODE, key);
		return cipher.doFinal(bytes);
	}

	/**
	 * 使用金鑰解密字串
	 *
	 * @param key
	 *        可以是公鑰或是私鑰，用於解密字串
	 * @param encryptedString
	 *        待解密的字串，為 Base64 格式
	 * @return String 解密後的明文字串
	 * @throws Exception
	 *         如果解密過程中發生錯誤，抛出此例外
	 */
	public static String decryptString(Key key, String encryptedString) throws Exception
	{
		byte[] decodedBytes = Base64.getDecoder().decode(encryptedString);
		byte[] decryptedBytes = decryptByteArray(key, decodedBytes);
		return new String(decryptedBytes, StandardCharsets.UTF_8);
	}

	public static byte[] decryptByteArray(Key key, byte[] encryptedBytes) throws Exception
	{
		Cipher cipher = Cipher.getInstance(RSA_ALGORITHM);
		cipher.init(Cipher.DECRYPT_MODE, key);
		return cipher.doFinal(encryptedBytes);
	}
}
