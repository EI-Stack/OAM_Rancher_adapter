package solaris.nfm.capability.system.aop.util;

import org.apache.commons.lang3.ArrayUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Holisun Wu
 */
public class AopUtil
{
	/**
	 * 將代理方法的入參，從 Array 轉成 JsonNode
	 */
	public static JsonNode getParameters(final ProceedingJoinPoint joinPoint)
	{
		final String[] parameterNames = getParameterNames(joinPoint);
		final Object[] parameterValues = joinPoint.getArgs();
		final ObjectNode rootNode = JsonNodeFactory.instance.objectNode();

		for (int i = 0; i < parameterNames.length; i++)
		{
			rootNode.put(parameterNames[i], parameterValues[i].toString());
		}

		return rootNode;
	}

	/**
	 * 依據代理方法入參名稱 (parameterName)，取得入參物件 (Object)
	 */
	public static Object getParameterValue(final ProceedingJoinPoint joinPoint, final String parameterName)
	{
		final String[] parameterNames = getParameterNames(joinPoint);
		// 取得某個入參的順序碼
		final int index = ArrayUtils.indexOf(parameterNames, parameterName);
		final Object[] args = joinPoint.getArgs();
		// 取得入參的值
		return (index == -1) ? null : args[index];
	}

	/**
	 * 取得代理方法入參名稱 array
	 */
	public static String[] getParameterNames(final ProceedingJoinPoint joinPoint)
	{
		final MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
		final String[] parameterNames = methodSignature.getParameterNames();
		// log.debug("\t[AOP] [{}] 代理方法入參名稱集合=[{}]", parameterNames.length, parameterNames);
		return parameterNames;
	}

	/**
	 * 取得代理方法的第一個入參值
	 */
	public static Object getFirstParameterValue(final ProceedingJoinPoint joinPoint)
	{
		final Object[] args = joinPoint.getArgs();
		// 取得入參的值
		return args[0];
	}

	/**
	 * 取得代理方法的第一個入參值，然後轉成字串類
	 */
	public static String getFirstParameterValueAsString(final ProceedingJoinPoint joinPoint)
	{
		final Object[] args = joinPoint.getArgs();
		if (args.length == 0) return null;
		// 取得第一個入參的值
		final Object obj = args[0];
		if (obj instanceof String) return (String) obj;
		if (obj instanceof Long || obj instanceof Integer) return String.valueOf(obj);
		return "";
	}
}
