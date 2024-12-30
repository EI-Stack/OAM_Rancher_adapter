package solaris.nfm.util;

/**
 * 基本資料型別的判斷與處理
 *
 * @author Holisun
 */
public class PrimitiveTypeUtil
{
	/**
	 * 判斷字串是否為整數
	 */
	public static Boolean isInteger(final String strInt)
	{
		if (strInt == null) return false;

		try
		{
			Integer.parseInt(strInt);
		} catch (final NumberFormatException nfe)
		{
			return false;
		}
		return true;
	}
}
