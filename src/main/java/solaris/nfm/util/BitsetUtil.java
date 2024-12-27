package solaris.nfm.util;

import java.util.BitSet;

public class BitsetUtil
{
	public static BitSet copyByteArrayToBitSet(final byte[] byteArray, final BitSet bitSet, final int startIndex)
	{
		for (int i = 0; i < byteArray.length * 8; i++)
		{
			if ((byteArray[i / 8] & (1 << (7 - (i % 8)))) > 0) bitSet.set(startIndex + i);
		}
		return bitSet;
	}

	// Returns a BitSet containing the values in bytes.
	// The byte-ordering of bytes must be big-endian which means the most significant bit is in element 0.
	public static BitSet fromByteArray(final byte[] byteArray)
	{
		BitSet bits = new BitSet();
		for (int i = 0; i < byteArray.length * 8; i++)
		{
			if ((byteArray[i / 8] & (1 << (7 - (i % 8)))) > 0) bits.set(i);
		}
		return bits;
	}

	// Returns a BitSet containing the values in bytes.
	// The byte-ordering of bytes must be big-endian which means the most significant bit is in element 0.
	// public static BitSet fromByteArray(byte[] byteArray)
	// {
	// BitSet bits = new BitSet();
	// for (int i = 0; i < byteArray.length * 8; i++)
	// {
	// if ((byteArray[byteArray.length - i / 8 - 1] & (1 << (i % 8))) > 0) bits.set(i);
	// }
	// return bits;
	// }
	// Returns a byte array of at least length 1.
	// The most significant bit in the result is guaranteed not to be a 1
	// (since BitSet does not support sign extension).
	// The byte-ordering of the result is big-endian which means the most significant bit is in element 0.
	// The bit at index 0 of the bit set is assumed to be the least significant bit.
	public static byte[] toByteArray(final BitSet bits)
	{
		int arrayLength = (bits.size() % 8 == 0) ? (bits.size() / 8) : (bits.size() / 8 + 1);
		// byte[] byteArray = new byte[arrayLength];
		// for (int i = 0; i < bits.length(); i++)
		// {
		// if (bits.get(i))
		// {
		// byteArray[i / 8] |= 1 << (7 - (i % 8));
		// }
		// }
		return toByteArray(bits, arrayLength);
	}

	public static byte[] toByteArray(final BitSet bits, final int assignedArrayLength)
	{
		int arrayLength = 0;
		int maxArrayLength = (bits.size() % 8 == 0) ? (bits.size() / 8) : (bits.size() / 8 + 1);
		if (assignedArrayLength < 0)
			arrayLength = 0;
		else if (assignedArrayLength > maxArrayLength)
			arrayLength = maxArrayLength;
		else
			arrayLength = assignedArrayLength;
		byte[] byteArray = new byte[arrayLength];
		int assignedBitSetLength = arrayLength * 8;
		for (int i = 0; i < assignedBitSetLength; i++)
		{
			if (bits.get(i))
			{
				byteArray[i / 8] |= 1 << (7 - (i % 8));
			}
		}
		return byteArray;
	}
}
