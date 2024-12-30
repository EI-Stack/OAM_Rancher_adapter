package solaris.nfm.capability.annotation.jsonvalid;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.extern.slf4j.Slf4j;
import solaris.nfm.exception.base.ExceptionBase;

@Component
@Aspect
@Slf4j
public class JsonValidatedAop
{
	@Autowired
	private JsonValidationService JsonValidatSrv;

	@Pointcut("@annotation(solaris.nfm.capability.annotation.jsonvalid.JsonValid)")
	private void pointcut()
	{}

	/**
	 * 標定套用 @JsonValid 的 method，然後搜尋資料型別為 JsonNode 的入參，進行 JSON 資料結構檢查
	 * 使用前提：每個適用的 API request 只會有一個資料型別為 JsonNode 的入參
	 */
	@Before(value = "@annotation(solaris.nfm.capability.annotation.jsonvalid.JsonValid)")
	public void before(final JoinPoint joinPoint) throws IOException, ExceptionBase, URISyntaxException
	{
		// final String methodName = joinPoint.getSignature().getName();
		// final String simpleClassName = joinPoint.getSignature().getDeclaringType().getSimpleName();
		// final String schemaFileName = simpleClassName + "-" + methodName + ".yaml";
		// log.debug("schemaFileName={}", schemaFileName);
		final MethodSignature signature = (MethodSignature) joinPoint.getSignature();
		final Method method = signature.getMethod();
		final JsonValid jsonValid = method.getAnnotation(JsonValid.class);
		final String schemaFileUriString = jsonValid.value();

		final Object[] args = joinPoint.getArgs();
		for (final Object arg : args)
		{
			if (arg instanceof JsonNode == true)
			{
				// log.debug("arg={}", arg);
				JsonValidatSrv.validate(schemaFileUriString, (JsonNode) arg);
				break;  // 只驗證第一個資料類型是 JsonNode 的入參
			}
		}
	}
}