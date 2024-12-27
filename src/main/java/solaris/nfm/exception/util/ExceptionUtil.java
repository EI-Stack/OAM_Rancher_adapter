package solaris.nfm.exception.util;

import java.lang.reflect.InvocationTargetException;

import org.springframework.http.HttpStatus;

import lombok.extern.slf4j.Slf4j;
import solaris.nfm.exception.EntityNotFoundException;
import solaris.nfm.exception.base.ExceptionBase;

@Slf4j
public class ExceptionUtil
{
	public static String getExceptionRootCauseMessage(final Exception e)
	{
		if (e instanceof ExceptionBase) return ((ExceptionBase) e).getErrorMessage();

		if (e.getCause() == null) return e.getMessage();

		Throwable cause = e.getCause();
		Throwable rootCause = cause;
		Integer exceptionLevel = 1;

		if (e instanceof InvocationTargetException)
		{
			while (cause.getCause() != null)
			{
				rootCause = cause;
				log.error("\t[Exception] [Level {}] Exceptoin class: {}, Error message:{}", exceptionLevel++, cause.getClass().getName(), cause.getMessage());
				cause = cause.getCause();
			}
		}

		return rootCause.getMessage();
	}

	/**
	 * 依據 exception 的類型，決定回傳的 HTTP status 的類型
	 *
	 * @param e
	 * @return
	 */
	public static HttpStatus getHttpStatus(final Exception e)
	{
		if (e instanceof ExceptionBase)
		{
			if (e instanceof EntityNotFoundException) return HttpStatus.NOT_FOUND;

			return HttpStatus.BAD_REQUEST;
		}
		if (e instanceof RuntimeException)
		{
			return HttpStatus.BAD_REQUEST;
		} else if (e instanceof InvocationTargetException)
		{
			if (e.getCause() != null) return HttpStatus.BAD_REQUEST;
			return HttpStatus.INTERNAL_SERVER_ERROR;
		}
		return HttpStatus.INTERNAL_SERVER_ERROR;
	}
}