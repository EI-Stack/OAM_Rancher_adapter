package solaris.nfm.config.security;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @description 自定義 AuthenticationSuccessHandler
 * @author Holisun Wu
 * @time 2017年07月03日 上午09:39:45
 */
public class AuthenticationSuccessHandlerNeo extends SimpleUrlAuthenticationSuccessHandler
{
	@Override
	public void onAuthenticationSuccess(final HttpServletRequest request, final HttpServletResponse response, final Authentication auth) throws IOException, ServletException
	{
		final String jsonString = "{\"value\":\"" + "OK" + "\"}";
		JsonNode responseJsonNode = (new ObjectMapper()).readTree(jsonString);
		response.setContentType(MediaType.APPLICATION_JSON_UTF8_VALUE);
		response.getWriter().print(responseJsonNode.toString());
		response.getWriter().flush();
	}
}
