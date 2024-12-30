package solaris.nfm.capability.system.aop.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.extern.slf4j.Slf4j;

/**
 * @author Holisun Wu
 */
@Slf4j
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

	/**
	 * 尋找 method 是否有套用 @ResponseStatus，若有，傳回 status code；若沒有，則傳回 200
	 */
	public static int getStatusCodeFromAnnotation(final ProceedingJoinPoint joinPoint)
	{
		final MethodSignature signature = (MethodSignature) joinPoint.getSignature();
		final Method method = signature.getMethod();
		final ResponseStatus annoResponseStatus = AnnotationUtils.findAnnotation(method, ResponseStatus.class);
		if (annoResponseStatus == null) return 200;
		return annoResponseStatus.code().value();
	}

	public static JsonNode findParameterValueByAnnotationRequestBody(final ProceedingJoinPoint joinPoint, final ObjectMapper objectMapper) throws NoSuchMethodException, SecurityException
	{
		JsonNode result = null;

		final MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
		final Method method = methodSignature.getMethod();
		final String methodName = method.getName();
		final String[] parameterNames = methodSignature.getParameterNames();
		// 取得所有入參的值
		final Object[] args = joinPoint.getArgs();
		final Class<?>[] parameterTypes = methodSignature.getMethod().getParameterTypes();
		// log.debug("parameterTypes={}", (new ObjectMapper()).valueToTree(parameterTypes));
		final Annotation[][] parameterAnnotations = joinPoint.getTarget().getClass().getMethod(methodName, parameterTypes).getParameterAnnotations();

		for (int i = 0; i < parameterAnnotations.length; i++)
		{
			final Annotation[] subParameterAnnotations = parameterAnnotations[i];
			final RequestBody targetParameterAnnotation = getAnnotationByType(subParameterAnnotations, RequestBody.class);
			if (targetParameterAnnotation != null)
			{
				// log.debug("methodName={}, parameterName={}, parameterType={}, parameterAnnotation={}", methodName, parameterNames[i], parameterTypes[i], targetParameterAnnotation);
				// 取得某個入參的順序碼
				final int index = ArrayUtils.indexOf(parameterNames, parameterNames[i]);
				if (index == -1) break;

				if (parameterTypes[i].equals(JsonNode.class) == false)
					result = objectMapper.valueToTree(args[index]);
				else
					result = (JsonNode) args[index];

				break;
			}
		}
		return result;
	}

	public static List<String> findParameterValueByAnnotationPathVariable(final ProceedingJoinPoint joinPoint, final ObjectMapper objectMapper) throws NoSuchMethodException, SecurityException
	{
		final List<String> result = new ArrayList<>();

		final MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
		final Method method = methodSignature.getMethod();
		final String methodName = method.getName();
		final String[] parameterNames = methodSignature.getParameterNames();
		// 取得所有入參的值
		final Object[] args = joinPoint.getArgs();
		final Class<?>[] parameterTypes = methodSignature.getMethod().getParameterTypes();
		// log.debug("parameterTypes={}", (new ObjectMapper()).valueToTree(parameterTypes));
		final Annotation[][] parameterAnnotations = joinPoint.getTarget().getClass().getMethod(methodName, parameterTypes).getParameterAnnotations();

		for (int i = 0; i < parameterAnnotations.length; i++)
		{
			final Annotation[] subParameterAnnotations = parameterAnnotations[i];
			final PathVariable targetParameterAnnotation = getAnnotationByType(subParameterAnnotations, PathVariable.class);
			if (targetParameterAnnotation != null)
			{
				// log.debug("/t [@PathVariable] methodName={}, parameterName={}, parameterType={}, parameterAnnotation={}", methodName, parameterNames[i], parameterTypes[i], targetParameterAnnotation);
				// 取得某個入參的順序碼
				final int index = ArrayUtils.indexOf(parameterNames, parameterNames[i]);
				if (index == -1) continue;
				result.add(args[index].toString());
			}
		}
		return result;
	}

	/**
	 * In an array of annotations, find the annotation of the specified type, if any.
	 *
	 * @return the annotation if available, or null
	 */
	@SuppressWarnings("unchecked")
	private static <T extends Annotation> T getAnnotationByType(final Annotation[] annotations, final Class<T> clazz)
	{
		T result = null;
		for (final Annotation annotation : annotations)
		{
			if (clazz.isAssignableFrom(annotation.getClass()))
			{
				result = (T) annotation;
				break;
			}
		}
		return result;
	}
}
