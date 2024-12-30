package solaris.nfm.config.security.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import solaris.nfm.config.security.domain.JwtUser;
import solaris.nfm.config.security.util.JwtTokenUtil;

/**
 * 获取已授权用户信息，相当于上个版本的userseesion方法
 */
@RestController
@RequestMapping("/api")
public class UserRestController
{
	// @Value("${jwt.header}")
	private String				tokenHeader	= "Bearer ";
	@Autowired
	private JwtTokenUtil		jwtTokenUtil;
	@Autowired
	@Qualifier("jwtUserDetailsService")
	private UserDetailsService	userDetailsService;

	@RequestMapping(value = "/user", method = RequestMethod.GET)
	public JwtUser getAuthenticatedUser(final HttpServletRequest request)
	{
		final String token = request.getHeader(tokenHeader).substring(7);
		final String username = jwtTokenUtil.getUsernameFromToken(token);    // 用户名作为用户的唯一标识（不是用户id）
		final JwtUser user = (JwtUser) userDetailsService.loadUserByUsername(username);
		return user;
	}
}