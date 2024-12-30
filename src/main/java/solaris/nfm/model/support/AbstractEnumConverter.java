package solaris.nfm.model.support;

import jakarta.persistence.AttributeConverter;

/**
 * @param <ATTR>
 *        Enum 自己的類，須實做 {@link JpaEnum} 接口
 * @param <DB>
 *        Enum 保存到資料庫的資料類型
 * @author Holisun Wu
 *         date: 2019-07-05
 *         使用參考: https://blog.csdn.net/wanping321/article/details/90269057
 */
public abstract class AbstractEnumConverter<ATTR extends Enum<ATTR> & JpaEnum<DB>, DB> implements AttributeConverter<ATTR, DB>
{
	private final Class<ATTR> clazz;

	public AbstractEnumConverter(final Class<ATTR> clazz)
	{
		this.clazz = clazz;
	}

	/**
	 * 將 enum 的 code 轉換為資料庫中欄位的值 dbData，即插入和更新資料庫會使用到此方法
	 */
	@Override
	public DB convertToDatabaseColumn(final ATTR attribute)
	{
		return attribute != null ? attribute.getCode() : null;
	}

	/**
	 * 將資料庫中的欄位 dbData 轉換為 enum 的 code，即當查詢資料庫會使用到此方法
	 */
	@Override
	public ATTR convertToEntityAttribute(final DB dbColumnData)
	{
		if (dbColumnData == null) return null;

		final ATTR[] enums = clazz.getEnumConstants();

		for (final ATTR e : enums)
		{
			if (e.getCode().equals(dbColumnData)) return e;

		}
		throw new UnsupportedOperationException("Enum 資料轉換異常。Enum【" + clazz.getSimpleName() + "】,資料庫裡面的值為：【" + dbColumnData + "】");
	}
}