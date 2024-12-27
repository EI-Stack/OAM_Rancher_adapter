package solaris.nfm.controller.util;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpSession;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.util.MultiValueMap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.extern.slf4j.Slf4j;
import solaris.nfm.config.security.bean.SpringUser;
import solaris.nfm.config.security.domain.JwtUser;
import solaris.nfm.exception.base.ExceptionBase;

@Slf4j
public class ControllerUtil
{
	/**
	 * 將集合類型的 Json 包裝成標準資料庫讀出規格 (含有分頁資訊)
	 */
	public static JsonNode createResponseJson(final ArrayNode arrayNode)
	{
		final Integer arraySize = arrayNode.size();
		final JsonNodeFactory jsonNodeFactory = JsonNodeFactory.instance;

		final ObjectNode pagination = JsonNodeFactory.instance.objectNode();
		pagination.put("pageNumber", 1).put("pageSize", arraySize).put("totalPages", 1).put("totalElements", arraySize);

		final ObjectNode root = jsonNodeFactory.objectNode();
		root.set("content", arrayNode);
		root.set("pagination", pagination);

		return root;
	}

	public static Object getPrincipalFromSession(final HttpSession httpSession)
	{
		Object principal = null;
		final SecurityContextImpl securityContextImpl = (SecurityContextImpl) httpSession.getAttribute("SPRING_SECURITY_CONTEXT");
		if (null != securityContextImpl && null != securityContextImpl.getAuthentication())
		{
			principal = securityContextImpl.getAuthentication().getPrincipal();
		}
		return principal;
	}

	// public static Long getTenantId() throws ExceptionBase
	// {
	// JwtUser jwtUser = null;
	// final Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
	// if (!(principal instanceof UserDetails))
	// {
	// log.error("Principal can not be cast as Jwt User, principal=[{}]", principal.toString());
	// throw new ExceptionBase("Can not get tenant ID.");
	// }
	// jwtUser = (JwtUser) principal;
	//
	// return jwtUser.getTenantId();
	// }

	public static Long getUserId() throws ExceptionBase
	{
		JwtUser jwtUser = null;
		final Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		if (!(principal instanceof UserDetails))
		{
			log.error("Principal can not be cast as Jwt User, principal=[{}]", principal.toString());
			throw new ExceptionBase("Can not get user ID.");
		}
		jwtUser = (JwtUser) principal;

		return jwtUser.getId();
	}

	public static String getUserName() throws ExceptionBase
	{
		JwtUser jwtUser = null;
		final Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		if (!(principal instanceof UserDetails))
		{
			log.error("Principal can not be cast as Jwt User, principal=[{}]", principal.toString());
			throw new ExceptionBase("Can not get user name.");
		}
		jwtUser = (JwtUser) principal;

		return jwtUser.getUsername();
	}

	public String getRequestParameter(final MultiValueMap<String, String> parameters, final String parameterName)
	{
		String result = null;
		if (parameters.get(parameterName) != null && parameters.get(parameterName).get(0) != null)
		{
			result = parameters.get(parameterName).get(0);
		}

		return result;
	}

	/*
	 * public static SpringUser getSpringUser(final HttpSession httpSession)
	 * {
	 * if (httpSession == null)
	 * {
	 * return null;
	 * }
	 * SpringUser springUser = null;
	 * SecurityContextImpl securityContextImpl = (SecurityContextImpl) httpSession.getAttribute("SPRING_SECURITY_CONTEXT");
	 * logger.debug("securityContextImpl=[{}]", securityContextImpl);
	 * if (null != securityContextImpl && null != securityContextImpl.getAuthentication())
	 * {
	 * Object principal = securityContextImpl.getAuthentication().getPrincipal();
	 * if (principal instanceof UserDetails)
	 * {
	 * springUser = (SpringUser) principal;
	 * } else
	 * {
	 * logger.error("Principal can not be cast as Spring User, principal=[" + principal.toString() + "]");
	 * }
	 * }
	 * return springUser;
	 * }
	 */

	// 此段落是 session 方案使用，保留
	/*
	 * public static SpringUser getSpringUser(final HttpSession httpSession)
	 * {
	 * logger.debug("httpSession=[{}]", httpSession);
	 * // Collection<? extends GrantedAuthority> authorities = new ArrayList<>();
	 * // SpringUser springUser = new SpringUser("holisun", "holisun", authorities, 17L, 1L, "nickname");
	 * SpringUser springUser = null;
	 * Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
	 * if (principal instanceof UserDetails) {
	 * springUser = (SpringUser) principal;
	 * logger.debug("SpringUser found, name=[{}]", springUser.getUsername());
	 * } else {
	 * logger.error("Principal can not be cast as Spring User, principal=[{}]", principal.toString());
	 * }
	 * return springUser;
	 * }
	 */

	public static SpringUser getSpringUser(final HttpSession httpSession)
	{
		JwtUser jwtUser = null;
		final Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		if (principal instanceof UserDetails)
		{
			jwtUser = (JwtUser) principal;
			// logger.debug("SpringUser found, name=[{}]", jwtUser.getUsername());
		} else
		{
			log.error("Principal can not be cast as Spring User, principal=[{}]", principal.toString());
		}

		final SpringUser springUser = new SpringUser(jwtUser.getUsername(), jwtUser.getPassword(), jwtUser.getAuthorities());

		return springUser;
	}

	public static WebAuthenticationDetails getWebAuthenticationDetailsFromSession(final HttpSession httpSession)
	{
		WebAuthenticationDetails webAuthenticationDetails = null;
		final SecurityContextImpl securityContextImpl = (SecurityContextImpl) httpSession.getAttribute("SPRING_SECURITY_CONTEXT");
		if (null != securityContextImpl && null != securityContextImpl.getAuthentication())
		{
			final Object principal = securityContextImpl.getAuthentication().getDetails();
			if (principal instanceof WebAuthenticationDetails)
			{
				webAuthenticationDetails = (WebAuthenticationDetails) principal;
			} else
			{
				log.error("Principal can not be cast as webAuthenticationDetails, principal=[" + principal.toString() + "]");
			}
		}
		return webAuthenticationDetails;
	}

	public static List<String> getRoleNameListFromSession(final HttpSession httpSession)
	{
		List<String> roleNameList = null;
		final SecurityContextImpl securityContextImpl = (SecurityContextImpl) httpSession.getAttribute("SPRING_SECURITY_CONTEXT");
		if (null != securityContextImpl && null != securityContextImpl.getAuthentication())
		{
			@SuppressWarnings("unchecked")
			final List<GrantedAuthority> authorityList = (List<GrantedAuthority>) securityContextImpl.getAuthentication().getAuthorities();
			roleNameList = new ArrayList<>(authorityList.size());
			for (final GrantedAuthority grantedAuthority : authorityList)
			{
				roleNameList.add(grantedAuthority.getAuthority());
			}
		}
		return roleNameList;
	}

	public static int safeLongToInt(final long l)
	{
		if (l < Integer.MIN_VALUE || l > Integer.MAX_VALUE)
		{
			throw new IllegalArgumentException(l + " cannot be cast to int without changing its value.");
		}
		return (int) l;
	}
}
