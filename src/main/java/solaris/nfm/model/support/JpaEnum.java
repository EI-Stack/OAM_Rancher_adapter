package solaris.nfm.model.support;

/**
 * @param <DB>
 *        : 自定義的 Enum 會實做此接口，當 Enum 資料要儲存至資料庫時，會轉換成資料庫中欄位的資料型別。DB 就是資料庫欄位的資料型別，例如 Integer 或是 String
 * @author Holisun Wu
 *         date: 2019-07-05
 **/
public interface JpaEnum<DB>
{
	DB getCode();
}