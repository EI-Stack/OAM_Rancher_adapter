package solaris.nfm.config.security;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @description 自定义AuthenticationSuccessHandler
 * @author Holisun Wu
 * @time 2017年07月03日 上午09:39:45
 */
public class AccessDeniedHandlerNeo implements AccessDeniedHandler
{
	@Override
	public void handle(final HttpServletRequest request, final HttpServletResponse response, final AccessDeniedException except) throws IOException, ServletException
	{
		final String jsonString = "{\"value\":\"" + "Access Deny" + "\"}";
		final JsonNode responseJsonNode = (new ObjectMapper()).readTree(jsonString);
		response.setContentType(MediaType.APPLICATION_JSON_UTF8_VALUE);
		response.getWriter().print(responseJsonNode.toString());
		response.getWriter().flush();
	}
}
