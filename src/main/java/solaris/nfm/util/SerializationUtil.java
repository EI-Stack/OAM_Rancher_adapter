package solaris.nfm.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SerializationUtil
{
	private static final Logger logger = LoggerFactory.getLogger(SerializationUtil.class);

	public static OutputStream serializeObject(final Object object) throws IOException
	{
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream))
		{
			objectOutputStream.writeObject(object);
		}
		return byteArrayOutputStream;
	}

	public static byte[] serializeObjectToByteArray(final Object object)
	{
		if (object == null) return null;
		byte[] byteArray = null;
		try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(); ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);)
		{
			objectOutputStream.writeObject(object);
			byteArray = byteArrayOutputStream.toByteArray();
		} catch (Exception e)
		{
			logger.error("\t Serialization failed !!", e);
		}
		return byteArray;
	}

	public static Object deserializeObject(final byte[] serializedData) throws IOException, ClassNotFoundException
	{
		if (serializedData == null) return null;
		Object object = null;
		try (ObjectInputStream objectInputStream = new ObjectInputStream(new ByteArrayInputStream(serializedData)))
		{
			object = objectInputStream.readObject();
		}
		return object;
	}
}