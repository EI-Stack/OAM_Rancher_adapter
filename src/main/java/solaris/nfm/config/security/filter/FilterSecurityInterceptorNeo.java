package solaris.nfm.config.security.filter;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.SecurityMetadataSource;
import org.springframework.security.access.intercept.AbstractSecurityInterceptor;
import org.springframework.security.access.intercept.InterceptorStatusToken;
import org.springframework.security.web.FilterInvocation;
import org.springframework.security.web.access.intercept.FilterInvocationSecurityMetadataSource;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import solaris.nfm.config.security.NeoAccessDecisionManager;

//@Component
public class FilterSecurityInterceptorNeo extends AbstractSecurityInterceptor implements Filter
{
	@Autowired
	private FilterInvocationSecurityMetadataSource securityMetadataSource;

	@Override
	public void init(final FilterConfig filterConfig) throws ServletException
	{}

	/**
	 * 登录后 每次请求都会调用这个拦截器进行请求过滤
	 *
	 * @param servletRequest
	 * @param servletResponse
	 * @param filterChain
	 * @throws IOException
	 * @throws ServletException
	 */
	@Override
	public void doFilter(final ServletRequest servletRequest, final ServletResponse servletResponse, final FilterChain filterChain) throws IOException, ServletException
	{
		final FilterInvocation fi = new FilterInvocation(servletRequest, servletResponse, filterChain);
		invoke(fi);
	}

	@Override
	public void destroy()
	{}

	@Override
	public Class<?> getSecureObjectClass()
	{
		return FilterInvocation.class;
	}

	@Override
	public SecurityMetadataSource obtainSecurityMetadataSource()
	{
		return this.securityMetadataSource;
	}

	/*
	 * @Override
	 * public void setAccessDecisionManager(MyAccessDecisionManager accessDecisionManager) {
	 * super.setAccessDecisionManager(this.accessDecisionManager);
	 * }
	 */
	@Autowired
	public void setAccessDecisionManager(final NeoAccessDecisionManager myAccessDecisionManager)
	{
		super.setAccessDecisionManager(myAccessDecisionManager);
	}

	/**
	 * 拦截请求处理
	 *
	 * @param fi
	 * @throws IOException
	 * @throws ServletException
	 */
	public void invoke(final FilterInvocation fi) throws IOException, ServletException
	{
		// fi里面有一个被拦截的url
		// 里面调用MyInvocationSecurityMetadataSource的getAttributes(Object object)这个方法获取fi对应的所有权限
		// 再调用MyAccessDecisionManager的decide方法来校验用户的权限是否足够
		final InterceptorStatusToken token = super.beforeInvocation(fi);
		try
		{
			// 执行下一个拦截器
			fi.getChain().doFilter(fi.getRequest(), fi.getResponse());
		} finally
		{
			super.afterInvocation(token, null);
		}
	}
}