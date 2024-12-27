package solaris.nfm.config.security.domain;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint
{
	@Override
	public void commence(final HttpServletRequest request, final HttpServletResponse response, final AuthenticationException authenticationException) throws IOException
	{
		log.warn("\t[Auth] Request ({}) is failed, message: {}", request.getRequestURI(), authenticationException.getMessage());
		// authenticationException.printStackTrace();

		//		if (authenticationException instanceof BadCredentialsException)
		//		{
		//			log.info("用户登录时身份认证失败.");
		//		} else if (authException instanceof InsufficientAuthenticationException)
		//		{
		//			log.info("缺少请求头参数,Authorization传递是token值所以参数是必须的.");
		//		} else
		//		{
		//			log.info("用户token无效.");
		//		}

		response.setContentType("application/json");
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		response.getOutputStream().println("{ \"message\": \"" + authenticationException.getMessage() + "\" }");

		// response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
	}

	public void commenceForAccessDenied(final HttpServletRequest request, final HttpServletResponse response, final String message) throws IOException
	{
		log.warn("\t[Auth] Request ({}) is failed, message: {}", request.getRequestURI(), message);

		response.setContentType("application/json");
		response.setStatus(HttpServletResponse.SC_FORBIDDEN);
		response.getOutputStream().println("{ \"message\": \"" + message + "\" }");
	}
}
