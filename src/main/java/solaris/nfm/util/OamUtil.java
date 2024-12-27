package solaris.nfm.util;

import java.util.BitSet;
import java.util.Map;

public class OamUtil
{
	// ciphering-algorithm
	public static Map<Integer, String>	neaMap	= Map.of(0, "null", 1, "snow-3g", 2, "aes", 3, "zuc");
	// integrity-algorithm
	public static Map<Integer, String>	niaMap	= Map.of(0, "null", 1, "snow-3g", 2, "aes", 3, "zuc");

	/**
	 * 將 NCI (00019005e) 轉出 gnbId 與 cellId
	 * NCI 可表示成 0000 0000 0000 0001 1001 00|00 0000 0101 1110 共 36 bit
	 * 前 22 bit 表示 gNB ID, 0000 0000 0000 0000 0110 0100 = 64 HEX (補位 2 bit)
	 * 後 14 bit 表示 cell ID, 0000 0000 0101 1110 = 5E HEX (補位 2 bit)
	 *
	 * @param gnbLength:
	 *        gnbId 的 bit 數量，預設值是 22
	 * @return
	 */
	public static String[] getGnbAndCellIdFromNci(final String nci, final Integer gnbIdLength)
	{
		final String[] gnbAndCell = new String[2];
		final BitSet nciBits = BitSet.valueOf(new long[]{Long.valueOf(nci, 16)});

		BitSet gnbBits = new BitSet(gnbIdLength);
		gnbBits.set(36 - gnbIdLength, 36);
		gnbBits.and(nciBits);
		gnbBits = shiftRight(gnbBits, 36 - gnbIdLength);

		final BitSet cellBits = new BitSet(36 - gnbIdLength);
		cellBits.set(0, 36 - gnbIdLength);
		cellBits.and(nciBits);

		gnbAndCell[0] = bytesToHex(gnbBits.toByteArray());
		final int gnbStrLeng = gnbIdLength / 4 + 1;
		for (int i = 0, gnbLeng = gnbAndCell[0].length(); i < gnbStrLeng - gnbLeng; i++)
		{
			gnbAndCell[0] = "0" + gnbAndCell[0];
		}

		gnbAndCell[1] = bytesToHex(cellBits.toByteArray());
		final int cellStrLeng = (36 - gnbIdLength) / 4 + 1;
		for (int i = 0, cellLeng = gnbAndCell[1].length(); i < cellStrLeng - cellLeng; i++)
		{
			gnbAndCell[1] = "0" + gnbAndCell[1];
		}

		return gnbAndCell;
	}

	public static String[] getGnbAndCellIdFromNci(final String nci)
	{
		final Integer gnbIdLength = 22;
		return getGnbAndCellIdFromNci(nci, gnbIdLength);
	}

	/**
	 * 將 NR Cell Global ID (gNBId,001,01,64,22) 中的 gNB ID (64) 加上 cell ID (5E) 轉成 NCI (00019005e)
	 * 範例中，64 表示 gNB ID，是 HEX 格式
	 * gnbIdLength (22) 表示 gNB ID 的 bit 數量，一般都是 22
	 * NCI 是固定 9 碼的 hex
	 */
	public static String getNciWithGnbAndCellId(final String gnbId, final String cellId, final Integer gnbIdLength)
	{
		BitSet nciBits = new BitSet(36);
		final BitSet gnbBits = BitSet.valueOf(new long[]{Long.valueOf(gnbId, 16)});
		final BitSet cellBits = BitSet.valueOf(new long[]{Long.valueOf(cellId, 16)});
		nciBits.or(gnbBits);
		nciBits = shiftLeft(nciBits, 36 - gnbIdLength);
		nciBits.or(cellBits);
		final int nciStrLeng = 9;
		final StringBuilder nci = new StringBuilder(bytesToHex(nciBits.toByteArray()));

		for (int i = 0, nciLeng = nci.length(); i < nciStrLeng - nciLeng; i++)
		{
			nci.insert(0, "0");
		}

		return nci.toString();
	}

	public static String getNciWithGnbAndCellId(final String gnbId, final String cellId)
	{
		final Integer gnbIdLength = 22;
		return getNciWithGnbAndCellId(gnbId, cellId, gnbIdLength);
	}

	private static String bytesToHex(final byte[] bytes)
	{
		final StringBuilder str = new StringBuilder();
		for (int i = bytes.length - 1; i >= 0; i--)
		{
			final byte _byte = bytes[i];
			str.append(String.format("%02x", _byte));
		}
		return str.toString();
	}

	private static BitSet shiftLeft(final BitSet bitSet, final int shiftBit)
	{
		final BitSet bs = new BitSet(bitSet.size());
		for (int i = 0; i < bitSet.size(); i++)
		{
			if (i + shiftBit < bitSet.size() && bitSet.get(i))
			{
				bs.set(i + shiftBit);
			}
		}
		return bs;
	}

	private static BitSet shiftRight(final BitSet bitSet, final int shiftBit)
	{
		final BitSet bs = new BitSet(bitSet.size());
		for (int i = bitSet.length() - 1; i >= 0; i--)
		{
			if (i - shiftBit >= 0 && bitSet.get(i))
			{
				bs.set(i - shiftBit);
			}
		}
		return bs;
	}
}
