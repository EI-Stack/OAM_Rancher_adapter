package solaris.nfm.capability.annotation.jsonvalid;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;

/**
 * 這是繼承 @Valid 的範例，沒有使用 AOP，此 class 沒有真正用於系統
 *
 * @author Holisun
 */
@Slf4j
public class JsonValidationImpl implements ConstraintValidator<JsonValid2, JsonNode>
{
	@Override
	public void initialize(final JsonValid2 annotation)
	{
		// initialization, probably not needed
	}

	@Override
	public boolean isValid(final JsonNode json, final ConstraintValidatorContext context)
	{
		// implementation of the url validation
		log.debug("進行驗證");
		final StackTraceElement[] stackTrace = new Throwable().getStackTrace();
		for (int stackLevel = 0; stackLevel < stackTrace.length; stackLevel++)
		{
			final String methodName = stackTrace[stackLevel].getMethodName();
			final String simpleClassName = stackTrace[stackLevel].getClassName().split("\\.")[stackTrace[stackLevel].getClassName().split("\\.").length - 1];
			log.debug("simpleClassName={}, methodName={}", simpleClassName, methodName);
			if (simpleClassName.startsWith("SliceCtr")) log.debug("stackLevel={}", stackLevel);
		}

		context.disableDefaultConstraintViolation();
		context.buildConstraintViolationWithTemplate("{solaris.nfm.capability.annotation.jsonvalid.jsonvalid2.message}").addConstraintViolation();
		return false;
	}
}