package solaris.nfm.controller.util;

import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import solaris.nfm.exception.EntityHasExistedException;
import solaris.nfm.exception.EntityNotFoundException;
import solaris.nfm.exception.base.ExceptionBase;

/**
 * Controller 全局異常處理
 * 此處有個知識點，若 spring boot 已經定義某個 exception 的處理程序，則不能用 @ExceptionHandler 方式來自訂處理方式
 * 替代方式是在 handleExceptionInternal() 裡面自訂 exception 的處理程序 *
 *
 * @author Holisun
 * @since 2018/6/1
 */
@Slf4j
@RestControllerAdvice
public class ControllerExceptionHandler extends ResponseEntityExceptionHandler
{
	@Autowired
	private MessageSource messageSource;

	/**
	 * 處理自定義的 ExceptionBase
	 */
	@ExceptionHandler(ExceptionBase.class)
	public ControllerErrorResponseBean handleMyExceptione(final HttpServletRequest request, final ExceptionBase exception, final HttpServletResponse response)
	{
		if (exception instanceof EntityNotFoundException)
		{
			log.error("\t[CEH] EntityNotFoundException: {}", exception.getErrorMessage());
			response.setStatus(HttpStatus.NOT_FOUND.value());
		} else if (exception instanceof EntityHasExistedException)
		{
			log.error("\t[CEH] EntityHasExistedException: {}", exception.getErrorMessage());
			response.setStatus(HttpStatus.BAD_REQUEST.value());
		} else
		{
			// 當 ExceptionBase 若有設定 errorCode 時，log 無法直接顯示 message，所以必須先將 errorMessage 印出
			log.error("\t[CEH] ExceptionBase, Message: {}", exception.getErrorMessage(), exception);
			response.setStatus(HttpStatus.BAD_REQUEST.value());
		}
		return new ControllerErrorResponseBean(exception.getErrorCode(), exception.getErrorMessage());
	}

	/**
	 * 處理 RuntimeException
	 */
	@ExceptionHandler(RuntimeException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ControllerErrorResponseBean handleRuntimeExceptione(final HttpServletRequest request, final RuntimeException exception, final HttpServletResponse response)
	{
		log.error("\t[CEH] RuntimeException, Message: {}", exception.getMessage(), exception);
		response.setStatus(HttpStatus.BAD_REQUEST.value());
		return new ControllerErrorResponseBean(400, exception.getMessage());
	}

	/**
	 * 處理反射機制異常 InvocationTargetException
	 */
	@ExceptionHandler(InvocationTargetException.class)
	public ControllerErrorResponseBean handleInvocationTargetExceptione(final HttpServletRequest request, final Exception e, final HttpServletResponse response)
	{
		log.error("\t[CEH] 全局異常處理 InvocationTargetException", e);

		Throwable cause = e.getCause();
		Throwable rootCause = cause;
		Integer exceptionLevel = 1;

		if (cause != null)
		{
			while (cause != null)
			{
				rootCause = cause;
				log.error("\t[Exception] Exception level: {}, Exceptoin class: {}, Error message:{}", exceptionLevel++, rootCause.getClass().getName(), rootCause.getMessage());
				cause = cause.getCause();
			}
			// ---[ EntityNotFoundException ]--------------------------------------------------------------------------[S]
			if (rootCause instanceof EntityNotFoundException || rootCause instanceof jakarta.persistence.EntityNotFoundException)
			{
				final EntityNotFoundException exception = (EntityNotFoundException) rootCause;
				response.setStatus(HttpStatus.NOT_FOUND.value());
				return new ControllerErrorResponseBean(exception.getErrorCode(), exception.getErrorMessage());
			}
			// ---[ EntityNotFoundException ]--------------------------------------------------------------------------[E]

			response.setStatus(HttpStatus.BAD_REQUEST.value());
			return new ControllerErrorResponseBean(HttpStatus.BAD_REQUEST.value(), rootCause.getMessage());
		}

		response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
		return new ControllerErrorResponseBean(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Java Reflection operation is failed.");
	}

	/**
	 * 通用的接口映射異常處理
	 */
	protected ResponseEntity<Object> handleExceptionInternal(final Exception ex, final Object body, final HttpHeaders headers, final HttpStatus status, final WebRequest request)
	{
		// 處理資料校驗異常：MethodArgumentNotValidException
		final ResponseEntity<Object> resultForMethodArgumentNotValidException = handleMethodArgumentNotValidException(ex, status);
		if (resultForMethodArgumentNotValidException != null) return resultForMethodArgumentNotValidException;

		if (ex instanceof MethodArgumentTypeMismatchException)
		{
			ex.printStackTrace();
			final MethodArgumentTypeMismatchException exception = (MethodArgumentTypeMismatchException) ex;
			final String methodName = (exception.getParameter().getMethod() == null) ? "" : exception.getParameter().getMethod().getName();
			log.error("\t[CEH] 輸入參數的資料型別不正確。" + "\n 調用方法：" + methodName + "\n 輸入参数名稱：" + exception.getName() + "\n 錯誤的参数值：" + exception.getValue() + "\n 錯誤訊息：" + exception.getLocalizedMessage());
			final String messageString = "Convering the vaule (" + exception.getValue() + ") of input argument (" + exception.getName() + ") is failed, check using proper data type.";
			return new ResponseEntity<>(new ControllerErrorResponseBean(status.value(), messageString), status);
		}

		if (ex instanceof HttpMessageNotReadableException)
		{
			final HttpMessageNotReadableException httpMessageNotReadableException = (HttpMessageNotReadableException) ex;
			log.debug("\t httpMessageNotReadableException Specific Cause exception name=[{}]", httpMessageNotReadableException.getMostSpecificCause().getClass().getSimpleName());

			// 處理 json 格式解析的錯誤：JsonParseException
			final ResponseEntity<Object> resultForJsonParseException = handleJsonParseException(httpMessageNotReadableException, status);
			if (resultForJsonParseException != null) return resultForJsonParseException;

			// 處理資料格式錯誤：InvalidFormatException
			final ResponseEntity<Object> resultForInvalidFormatException = handleInvalidFormatException(httpMessageNotReadableException, status);
			if (resultForInvalidFormatException != null) return resultForInvalidFormatException;

			// 此異常與其子類不會被網站使用者觸發
			if (httpMessageNotReadableException.getMostSpecificCause() instanceof MismatchedInputException)
			{
				final MismatchedInputException mismatchedInputException = (MismatchedInputException) httpMessageNotReadableException.getMostSpecificCause();

				// 處理 json 資料結構出現多餘不明欄位的錯誤：UnrecognizedPropertyException
				final ResponseEntity<Object> resultForUnrecognizedPropertyException = handleUnrecognizedPropertyException(mismatchedInputException, status);
				if (resultForUnrecognizedPropertyException != null) return resultForUnrecognizedPropertyException;

				ex.printStackTrace();
				final String exceptionName = mismatchedInputException.getClass().getSimpleName();
				final String message = this.messageSource.getMessage("api-input-check.mismatched-input", null, "", LocaleContextHolder.getLocale());
				log.error("\t[CEH] [{}] Input Json cannot match to Dto. Message: {}", exceptionName, mismatchedInputException.getMessage());
				final ObjectNode json = JsonNodeFactory.instance.objectNode().put("code", status.value()).put("message", message);
				return new ResponseEntity<>(json, status);
			}

			ex.printStackTrace();
			final HttpMessageNotReadableException exception = (HttpMessageNotReadableException) ex;
			final String message = exception.getMostSpecificCause().getMessage();
			return new ResponseEntity<>(new ControllerErrorResponseBean(status.value(), "資料格式錯誤，導致解析失敗。例如，預定輸入 JSON 格式內容，但卻使用錯誤的格式。詳細錯誤訊息：" + message), status);
		}

		return new ResponseEntity<>(new ControllerErrorResponseBean(status.value(), "所輸入的參數資料型別不正確或是某些參數遺漏了。"), status);
	}
	// 如果数据校验不通过，则Spring boot会抛出BindException异常，我们可以捕获这个异常然後封装返回结果。MethodArgumentNotValidException

	/**
	 * 處理 Json 格式解析的錯誤：JsonParseException
	 */
	private ResponseEntity<Object> handleJsonParseException(final HttpMessageNotReadableException httpMessageNotReadableException, final HttpStatus status)
	{
		if (httpMessageNotReadableException.getMostSpecificCause() instanceof JsonParseException == false) return null;

		final JsonParseException ex = (JsonParseException) httpMessageNotReadableException.getMostSpecificCause();
		final String exceptionName = ex.getClass().getSimpleName();
		final Integer lineNo = ex.getLocation().getLineNr();
		final Integer columnNo = ex.getLocation().getColumnNr();
		final String message = MessageFormat.format("Inputted Json is NOT well-formated. Check line: {0}, column: {1}.", lineNo, columnNo);

		log.error("\t[CEH] [{}] Inputted Json is NOT well-formated. Message: {}", exceptionName, ex.getMessage());
		final ObjectNode json = JsonNodeFactory.instance.objectNode().put("code", status.value()).put("message", message);
		return new ResponseEntity<>(json, status);
	}

	/**
	 * 處理 json 資料結構出現多餘不明欄位的錯誤：UnrecognizedPropertyException
	 */
	private ResponseEntity<Object> handleUnrecognizedPropertyException(final MismatchedInputException mismatchedInputException, final HttpStatus status)
	{
		if (mismatchedInputException instanceof UnrecognizedPropertyException == false) return null;

		final UnrecognizedPropertyException ex = (UnrecognizedPropertyException) mismatchedInputException;
		final String exceptionName = ex.getClass().getSimpleName();
		final String unrecognizedPropertyName = ex.getPropertyName();
		final String knownPropertyNameCsv = (ex.getKnownPropertyIds() == null) ? "" : ex.getKnownPropertyIds().stream().map(Object::toString).collect(Collectors.joining(", "));
		final String message = MessageFormat.format("Field ''{0}'' is unrecognized, allowed fields are [{1}].", unrecognizedPropertyName, knownPropertyNameCsv);

		log.error("\t[CEH] [{}] Input Json cannot match to Dto. Message: {}", exceptionName, ex.getMessage());
		final ObjectNode json = JsonNodeFactory.instance.objectNode().put("code", status.value()).put("message", message);
		return new ResponseEntity<>(json, status);
	}

	/**
	 * 處理資料格式錯誤 (資料類型不匹配)：InvalidFormatException
	 */
	private ResponseEntity<Object> handleInvalidFormatException(final HttpMessageNotReadableException httpMessageNotReadableException, final HttpStatus status)
	{
		if (httpMessageNotReadableException.getMostSpecificCause() instanceof InvalidFormatException == false) return null;

		final InvalidFormatException ex = (InvalidFormatException) httpMessageNotReadableException.getMostSpecificCause();
		final String exceptionName = ex.getClass().getSimpleName();
		final String fieldName = ex.getPath().get(0).getFieldName();
		final String dataTypeName = ex.getTargetType().getSimpleName();
		final String rejectedValue = ex.getValue() == null ? "null" : ex.getValue().toString();

		// ---[ Special rule for Enum ]------------------------------------------------------------------------[S]
		final Pattern enumErrorMessagePattern = Pattern.compile("Enum class: (\\[.*\\])");
		final Matcher matcher = enumErrorMessagePattern.matcher(ex.getMessage());
		if (matcher.find())
		{
			final String enumMemberSet = matcher.group(1);
			final String[] args = {fieldName, rejectedValue, enumMemberSet};
			final String message = this.messageSource.getMessage("api-input-check.invalid-value.enum", args, "i18n No Mateched", LocaleContextHolder.getLocale());
			log.error("\t[CEH] [{}] Enum validation is failed. Message: {}", exceptionName, message);
			final ObjectNode json = JsonNodeFactory.instance.objectNode().put("code", status.value()).put("message", message);
			return new ResponseEntity<>(json, status);
		}
		// ---[ Special rule for Enum ]------------------------------------------------------------------------[E]

		final String[] args = {fieldName, dataTypeName, rejectedValue};
		final String message = this.messageSource.getMessage("api-input-check.invalid-format", args, "i18n No Mateched", LocaleContextHolder.getLocale());

		log.error("\t[CEH] [{}] Data type is not matched. Message: {}", exceptionName, message);
		final ObjectNode json = JsonNodeFactory.instance.objectNode().put("code", status.value()).put("message", message);
		return new ResponseEntity<>(json, status);
	}

	/**
	 * 處理資料校驗異常 MethodArgumentNotValidException
	 */
	private ResponseEntity<Object> handleMethodArgumentNotValidException(final Exception exception, final HttpStatus status)
	{
		if (exception instanceof MethodArgumentNotValidException == false) return null;

		final MethodArgumentNotValidException ex = (MethodArgumentNotValidException) exception;
		final ObjectNode json = JsonNodeFactory.instance.objectNode().put("code", status.value());
		final ArrayNode messages = json.putArray("message");
		final Locale locale = LocaleContextHolder.getLocale();
		ex.getBindingResult().getAllErrors().forEach(error ->
		{
			// log.debug("={}", error.getCode());
			// for (final String str : error.getCodes())
			// {
			// log.debug("code={}", str);
			// }
			// log.debug("={}", error.getObjectName());

			final String fieldName = ((FieldError) error).getField();
			final String rejectedValue = ((FieldError) error).getRejectedValue() == null ? "null" : ((FieldError) error).getRejectedValue().toString();
			final String defaultErrorMessage = error.getDefaultMessage();

			final String[] args = {rejectedValue, fieldName, defaultErrorMessage};
			final String i18nErrorMessage = this.messageSource.getMessage("api-input-check.invalid-value.common", args, "", locale);
			// final String message = MessageFormat.format("The input value ({0}) of argument ''{1}'' {2}.", rejectedValue, fieldName, errorMessage);
			log.error("\t[CEH] Data validation is failed. Message: {}", i18nErrorMessage);
			messages.add(i18nErrorMessage);
		});

		return new ResponseEntity<>(json, status);
	}

	@Getter
	@Setter
	private class ControllerErrorResponseBean
	{
		private Integer	code;
		private String	message;

		ControllerErrorResponseBean(final Integer code, final String message)
		{
			this.code = code;
			this.message = message;
		}
	}
}
