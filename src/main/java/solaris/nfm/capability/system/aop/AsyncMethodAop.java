package solaris.nfm.capability.system.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * 異步方法要使用 AOP 幫忙做統一的資料源切換，因此隱含的規則是，第一個入參必須是 tenantId
 *
 * @author Holisun Wu
 */
@Component
@Aspect
public class AsyncMethodAop
{
	/**
	 * 定義適用方法
	 * example: execution(* solaris.nfm.service.PolicyService.handleEvent*(..))
	 */
	// @Pointcut("")
	private void aspect()
	{}

	/**
	 * 圍繞處理
	 */
	// @Around("aspect()")
	public Object aroundForDeviceManager(final ProceedingJoinPoint joinPoint) throws Throwable
	{
		// 取得代理方法的入參集合
		final Object[] args = joinPoint.getArgs();
		// 從第一個入參取得 tenantId
		final Long tenantId = (Long) args[0];

		return joinPoint.proceed();
	}
}