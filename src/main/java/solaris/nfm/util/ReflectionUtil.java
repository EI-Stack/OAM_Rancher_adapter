package solaris.nfm.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import lombok.extern.slf4j.Slf4j;

/**
 * @author Holisun Wu
 * @date 2020-01-10
 */
@Slf4j
public class ReflectionUtil
{
	/**
	 * 藉由反射機制取得方法，重點在於消除不必要的例外 NoSuchMethodException, SecurityException
	 *
	 * @param targetClass
	 * @param methodName
	 * @param parameterTypes
	 * @return java.lang.reflect.Method
	 */
	public static Method getMethod(final Class<?> targetClass, final String methodName, final Class<?>... parameterTypes)
	{
		Method method = null;
		try
		{
			method = targetClass.getMethod(methodName, parameterTypes);
		} catch (NoSuchMethodException | SecurityException e)
		{
			log.error("\t[DMO] [Reflection] 這個例外不該發生的！！ 無法透過反射機制取得 method {}()，檢查最近是否修改了自訂方法的定義或者更動了外部函式庫的版本", methodName);
			e.printStackTrace();
		}
		return method;
	}

	/**
	 * 藉由反射機制調用方法，重點在於消除不必要的例外 IllegalAccessException, IllegalArgumentException, InvocationTargetException
	 *
	 * @param method
	 * @param targetObject
	 * @param args
	 * @return
	 * @throws Throwable
	 */
	public static Object invokeMethod(final Method method, final Object targetObject, final Object... args) throws InvocationTargetException
	{
		Object returnedObject = null;
		try
		{
			returnedObject = method.invoke(targetObject, args);
		} catch (IllegalAccessException | IllegalArgumentException e)
		{
			log.error("\t[Reflection] 這個例外不該發生的！！ 執行 {}() 失敗，檢查方法定義是否改變，入參資料類別是否正確，入參是否為空值", method.getName());
			e.printStackTrace();
		} catch (InvocationTargetException e)
		{
			throw e;
		}

		return returnedObject;
	}

	/**
	 * 顯示 targetClass 所有的方法與所附屬的入參資料型別
	 *
	 * @param targetClass
	 */
	public static void showAllMethods(final Class<?> targetClass)
	{
		for (Method method : targetClass.getMethods())
		{
			log.debug("\t Method Name=[{}]", method.getName());
			for (Class<?> parameterType : method.getParameterTypes())
			{
				log.debug("\t Parameter Data Type=[{}]", parameterType.getName());
			}
		}
	}
}
