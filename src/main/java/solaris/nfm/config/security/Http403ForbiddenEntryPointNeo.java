package solaris.nfm.config.security;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.Http403ForbiddenEntryPoint;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @description 自定义AuthenticationSuccessHandler
 * @author Holisun Wu
 * @time 2017年07月03日 上午09:39:45
 */
@Component
public class Http403ForbiddenEntryPointNeo extends Http403ForbiddenEntryPoint
{
	@Override
	public void commence(final HttpServletRequest request, final HttpServletResponse response, final AuthenticationException authenticationException) throws IOException
	{
		final String jsonString = "{\"value\":\"" + "Forbidden" + "\"}";
		final JsonNode responseJsonNode = (new ObjectMapper()).readTree(jsonString);
		response.setContentType(MediaType.APPLICATION_JSON_UTF8_VALUE);
		response.getWriter().print(responseJsonNode.toString());
		response.getWriter().flush();
	}
}
