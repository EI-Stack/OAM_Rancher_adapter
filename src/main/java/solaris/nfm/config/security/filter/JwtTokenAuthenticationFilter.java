package solaris.nfm.config.security.filter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import solaris.nfm.capability.rest.EncryptService;
import solaris.nfm.config.security.bean.JwtTokenConfigBean;
import solaris.nfm.config.security.domain.JwtAuthenticationEntryPoint;
import solaris.nfm.config.security.domain.JwtUser;
import solaris.nfm.config.security.util.JwtTokenUtil;

/**
 * 用途：驗證 token 是否合法，並且能處理授權失敗的例外
 * 此類別必須配置到 WebSecurityConfig
 */
@Slf4j
public class JwtTokenAuthenticationFilter extends OncePerRequestFilter
{
	@Autowired
	private JwtTokenConfigBean			jwtTokenConfigBean;
	@Autowired
	private JwtAuthenticationEntryPoint	jwtAuthenticationEntryPoint;
	@Autowired
	private EncryptService				encryptService;

	private String getJwtToken(final HttpServletRequest request)
	{
		final String headerAuthString = request.getHeader(jwtTokenConfigBean.getTokenHeader());
		if (headerAuthString != null && headerAuthString.startsWith(jwtTokenConfigBean.getTokenPrefixString()))
		{
			return headerAuthString.substring(jwtTokenConfigBean.getTokenPrefixString().length());
		}
		return null;
	}

	@Override
	protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response, final FilterChain chain) throws ServletException, IOException
	{
		final String jwtToken = getJwtToken(request);
		String role = null;
		// Long tenantId = null;
		Long userId = null;
		String username = null;
		String userIp = null;
		try
		{
			// if (jwtToken == null)
			// {
			// throw new BadCredentialsException("There's no JWT token string in HTTP headers.");
			// }

			// 此處讓沒有 token 者也能登入，由系統賦予用戶名稱與最低權限
			if (jwtToken == null)
			{
				role = "ROLE_GUEST";
				// tenantId = 0L;
				userId = -101L;
				username = "USER_GUEST";
			} else
			{
				if (!jwtTokenConfigBean.getAuthenticatedTokens().containsValue(jwtToken))
				{
					throw new BadCredentialsException("This's an unauthenticated JWT token.");
				}

				final JsonNode tokenBodyJson = JwtTokenUtil.getJsonFromTokenPayload(jwtToken);
				if (tokenBodyJson == null)
				{
					throw new BadCredentialsException("Parsing JWT token is failed.");
				}

				role = JwtTokenUtil.getRoleFromTokenPayload(tokenBodyJson);
				if (role.equals("ROLE_Portal"))
				{
					userId = encryptService.getUserId(request.getHeader("userId"));
					// tenantId = checkTenantId(request.getHeader("tenantId"));
					// 此處要把 username 換成 header 上所帶的 username，而不是使用 token 上的 username
					username = encryptService.getUserName(request.getHeader("userName"));
					userIp = encryptService.getUserIp(request.getHeader("userIp"));
				} else if (role.equals("ROLE_LogManager"))
				{
					userId = -1L;
					username = JwtTokenUtil.getUsernameFromTokenPayload(tokenBodyJson);
				} else if (role.equals("ROLE_Tenant-Manager"))
				{
					// tenantId = Long.parseLong(request.getHeader("tenantId"));
					userId = -4L;
					username = JwtTokenUtil.getUsernameFromTokenPayload(tokenBodyJson);
				} else
				{
					// Role: Admin
					// tenantId = 0L;
					userId = 0L;
					username = JwtTokenUtil.getUsernameFromTokenPayload(tokenBodyJson);
				}
			}

			final String queryString = (request.getQueryString() == null) ? "" : "?" + request.getQueryString();
			if (!(request.getRequestURI().equalsIgnoreCase("/v1/devices") && request.getMethod().equals("GET"))
					&& !(request.getRequestURI().equalsIgnoreCase("/v1/devices/search") && request.getMethod().equals("POST")) && !(request.getRequestURI().startsWith("/v1/network")))
			{
				log.info("\t[Auth] ({} {} {}) {} {}{}", role, userId, username, request.getMethod(), request.getRequestURI(), queryString);
			}
		} catch (final AuthenticationException e)
		{
			SecurityContextHolder.clearContext();
			jwtAuthenticationEntryPoint.commence(request, response, e);
			return;
		}

		if (username != null && SecurityContextHolder.getContext().getAuthentication() == null)
		{
			final List<GrantedAuthority> authorities = new ArrayList<>(1);
			authorities.add(new SimpleGrantedAuthority(role));
			final JwtUser jwtUser = new JwtUser(userId, username, "", true, LocalDateTime.now(), userIp, authorities);

			final UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(jwtUser, null, authorities);
			authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
			SecurityContextHolder.getContext().setAuthentication(authentication);
			// log.info("\t [Token] size ({}) 的認證資料放入 SecurityContextHolder", userDetails.getAuthorities().size());
			// log.info("\t [Token] 成功將使用者 ({}) 的認證資料放入 SecurityContextHolder", username);
		}

		// 當 role 無適當權限執行請求時，會丟出 AccessDeniedException，須在此進行處理
		try
		{
			chain.doFilter(request, response);
		} catch (final AccessDeniedException e)
		{
			log.debug("xxxx1");
			SecurityContextHolder.clearContext();
			final String message = "The role (" + role + ") has no authorization to access URI (" + request.getRequestURI() + ")";
			jwtAuthenticationEntryPoint.commenceForAccessDenied(request, response, message);
			return;
		}
	}

	private Long checkTenantId(final String tenantIdString)
	{
		// 檢查不可為空
		if (!StringUtils.hasText(tenantIdString))
		{
			throw new BadCredentialsException("There is no tenant ID in request");
		}
		// 檢查合法的 Long
		Long tenantId = null;
		try
		{
			tenantId = Long.valueOf(tenantIdString.trim());
		} catch (final Exception e)
		{
			throw new BadCredentialsException("Tenant ID (" + tenantIdString.trim() + ") is NOT a number.");
		}

		if (tenantId < -1L)
		{
			throw new BadCredentialsException("Tenant ID (" + tenantId + ") is smaller than -1.");
			// 檢查是否存在於 data source pool
			// final String beanName = DynamicDataSourceUtil.getBeanName(tenantId);
			// Map<Object, Object> targetDataSources = dynamicRoutingDataSource.getNeoTargetDataSources();
			// if (!targetDataSources.containsKey(beanName))
			// throw new BadCredentialsException("Tenant ID (" + tenantId + ") does NOT exist.");
		}

		return tenantId;
	}
}