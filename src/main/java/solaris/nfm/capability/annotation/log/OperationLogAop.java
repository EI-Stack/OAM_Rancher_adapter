package solaris.nfm.capability.annotation.log;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.amqp.AmqpException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import solaris.nfm.capability.message.amqp.AmqpService;
import solaris.nfm.capability.system.aop.dto.ApiLogDto;
import solaris.nfm.capability.system.aop.util.AopUtil;
import solaris.nfm.config.security.domain.JwtUser;
import solaris.nfm.exception.util.ExceptionUtil;

/**
 * 針對指定的使用者操作指令，進行發送日誌。這些指令必須是全部都是在 *Controller 的方法
 *
 * @author Holisun Wu
 */
@Component
@Aspect
public class OperationLogAop
{
	@Autowired
	private AmqpService		amqpService;
	@Autowired
	private ObjectMapper	objectMapper;

	/**
	 * 當調用方法時，無論執行正常與否，都會送出日誌 (圍繞處理)
	 * 使用 @Around 就必須搭套 ProceedingJoinPoint
	 */
	@Around("@annotation(solaris.nfm.capability.annotation.log.OperationLog)")
	public Object sendOperationLog(final ProceedingJoinPoint joinPoint) throws Throwable
	{
		final JwtUser jwtUser = (JwtUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		try
		{
			final Object operationResult = joinPoint.proceed();
			// log.debug("\t[AOP] 出參=[{}]", objectMapper.valueToTree(operationResult).toPrettyString());
			// 發送日誌
			sendLog(joinPoint, jwtUser, operationResult);
			return operationResult;
		} catch (final Exception e)
		{
			// 發送錯誤日誌
			sendLogForException(joinPoint, jwtUser, e);
			throw e;
		}
	}

	/**
	 * 準備使用者操作日誌
	 */
	private ApiLogDto prepareUserOperationLog(final ProceedingJoinPoint joinPoint, final JwtUser jwtUser) throws NoSuchMethodException, SecurityException
	{
		final ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
		final HttpServletRequest request = attributes.getRequest();
		final String requestQueryString = (request.getQueryString() == null) ? "" : "?" + request.getQueryString();
		final String requestUri = request.getRequestURI() + requestQueryString;
		final String operationName = joinPoint.getSignature().getName();
		// final HttpServletResponse response = attributes.getResponse();
		// 這裡有個貓膩，取得的 status code 固定是 200，與實際的傳回值 201/204/400 不同
		// 原理說明：在 AOP 結束後，JVM 才會依據實際情況去設定 status code，在此之前，預設值就是 200。所以，不管怎麼抓值，都是 200
		// 像 @ResponseStatus 就是在 AOP 之後才執行。所以，理解原理後，可以改成去抓 @ResponseStatus 的 status code
		final Integer statusCode = AopUtil.getStatusCodeFromAnnotation(joinPoint);

		final ApiLogDto logBean = new ApiLogDto();
		// apiLogDto.setTenantId(jwtUser.getTenantId());
		// log.debug("={}, ={}", jwtUser.getId(), jwtUser.getUsername());
		logBean.setUserId(jwtUser.getId());
		logBean.setUsername(jwtUser.getUsername());
		logBean.setUserIp(jwtUser.getUserIp());
		logBean.setApiType(ApiLogDto.ApiType.SystemLog);
		logBean.setOperationName(operationName);
		logBean.setOperationTag(getOperationTag(joinPoint));
		logBean.setRequestMethod(request.getMethod());
		logBean.setRequestUri(requestUri);
		logBean.setResponseStatusCode(statusCode);
		logBean.setTargetId(String.join(", ", AopUtil.findParameterValueByAnnotationPathVariable(joinPoint, objectMapper)));
		logBean.setOperationInput(AopUtil.findParameterValueByAnnotationRequestBody(joinPoint, objectMapper));

		return logBean;
	}

	/**
	 * 發送使用者操作日誌
	 */
	@Async
	private void sendLog(final ProceedingJoinPoint joinPoint, final JwtUser jwtUser, final Object operationOutput)
			throws AmqpException, JsonProcessingException, InterruptedException, ExecutionException, TimeoutException, NoSuchMethodException, SecurityException
	{
		// 決定哪些 operation 不送日誌
		if (isOperationIgnore(joinPoint)) return;

		final ApiLogDto logBean = prepareUserOperationLog(joinPoint, jwtUser);
		final String operationName = logBean.getOperationName();
		final ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
		final HttpServletRequest request = attributes.getRequest();

		// ---[ 發送日誌 ]---------------------------------------------------------------------------------------------[S]
		if (operationOutput != null && operationOutput instanceof JsonNode)
		{
			logBean.setOperationOutput((JsonNode) operationOutput);
		}

		if (request.getMethod().equals("POST") && operationName.startsWith("create") && operationOutput != null && operationOutput instanceof JsonNode)
		{
			final JsonNode jsonResult = (JsonNode) operationOutput;
			logBean.setTargetId(jsonResult.path("id").asText());
		}
		logBean.setIsSuccessful(true);
		this.amqpService.sendMsgForOperationLog(logBean);
		// ---[ 發送日誌 ]---------------------------------------------------------------------------------------------[E]
	}

	/**
	 * 發送使用者操作日誌 (執行過程出錯)
	 *
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 */
	@Async
	private void sendLogForException(final ProceedingJoinPoint joinPoint, final JwtUser jwtUser, final Exception e)
			throws AmqpException, JsonProcessingException, InterruptedException, ExecutionException, TimeoutException, NoSuchMethodException, SecurityException
	{
		// 決定哪些 operation 不送日誌
		if (isOperationIgnore(joinPoint)) return;

		final ApiLogDto logBean = prepareUserOperationLog(joinPoint, jwtUser);
		final String rootCauseMessage = ExceptionUtil.getExceptionRootCauseMessage(e);
		// log.debug("\t[AOP] [發送錯誤日誌] Root Cause Message=[{}]", rootCauseMessage);
		logBean.setIsSuccessful(false);
		logBean.setOperationError(rootCauseMessage);
		logBean.setResponseStatusCode(ExceptionUtil.getHttpStatus(e).value());
		this.amqpService.sendMsgForOperationLog(logBean);
	}

	/**
	 * 先取得代理方法所屬的類完整名稱，再依此取得 class name，最後去除 Ctr，剩下來的字串當作 tag
	 */
	private String getOperationTag(final ProceedingJoinPoint joinPoint)
	{
		final String controllerClassFullName = joinPoint.getSignature().getDeclaringTypeName();
		final String[] tmpArray = controllerClassFullName.split("\\.");
		final String className = tmpArray[tmpArray.length - 1];
		final String operationTag = className.replace("Ctr", "");
		return operationTag;
	}

	/**
	 * 決定哪些 operation 強制不送日誌
	 */
	private Boolean isOperationIgnore(final ProceedingJoinPoint joinPoint)
	{
		// final ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
		// final HttpServletRequest request = attributes.getRequest();
		// // Get 類型不發送日誌
		// if (request.getMethod().equals("GET")) return true;
		//
		// final List<String> operationNames = List.of("getYangValue");
		// if (operationNames.contains(apiLogDto.getOperationName())) return true;

		return false;
	}
}
