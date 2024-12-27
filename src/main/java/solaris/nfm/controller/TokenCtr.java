package solaris.nfm.controller;

import java.util.Objects;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;
import solaris.nfm.capability.message.amqp.AmqpService;
import solaris.nfm.capability.system.aop.dto.LogBaseDto;
import solaris.nfm.config.security.domain.AuthenticationException;
import solaris.nfm.config.security.domain.JwtAuthenticationRequest;
import solaris.nfm.config.security.domain.JwtAuthenticationResponse;
import solaris.nfm.config.security.domain.JwtUser;
import solaris.nfm.config.security.util.JwtTokenUtil;

/**
 * 授权控制器
 * 用户登录控制器
 */
@Slf4j
@RestController
@RequestMapping("/v1")
public class TokenCtr
{
	// @Value("${jwt.header}")
	private String					tokenHeader	= "Bearer ";
	// WebSecurityConfig类里面配置的，用来校验用户名和密码的
	@Autowired
	private AuthenticationManager	authenticationManager;
	@Autowired
	private JwtTokenUtil			jwtTokenUtil;
	@Autowired
	@Qualifier("jwtUserDetailsService")
	private UserDetailsService		userDetailsService;

	@Autowired
	private AmqpService				amqpService;

	@GetMapping(value = "/testAmqp")
	public void testAmqp()
	{
		final LogBaseDto logBaseDto = new LogBaseDto();

		try
		{
			amqpService.sendMsg(logBaseDto);
		} catch (final Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * 创建授权令牌 (登录)
	 *
	 * @param authenticationRequest
	 * @return
	 * @throws AuthenticationException
	 */
	@PostMapping(value = "/getToken")  // 用户登录的用户名和密码已经封装到 JwtAuthenticationRequest 里面了
	public ResponseEntity<JwtAuthenticationResponse> createAuthenticationToken(@RequestBody final JwtAuthenticationRequest authenticationRequest) throws AuthenticationException
	{
		log.debug("\t [JWT] 進入取得 token 程序");
		final String username = authenticationRequest.getUsername().trim();
		final String password = authenticationRequest.getPassword().trim();
		// authenticate 校验用户名和密码（本类下面）
		authenticate(username, password);
		// 校验通过后
		// Reload password post-security so we can generate the token
		// 按用户名查用户
		final UserDetails userDetails = userDetailsService.loadUserByUsername(username);
		// 然后传入用户生成 token
		final String token = jwtTokenUtil.generateToken(userDetails);
		// 把 token 封装到 JwtAuthenticationResponse 里面返回
		return ResponseEntity.ok(new JwtAuthenticationResponse(token));
	}

	/**
	 * 刷新
	 *
	 * @param request
	 * @return
	 */
	@GetMapping(value = "/refresh")
	public ResponseEntity<?> refreshAndGetAuthenticationToken(final HttpServletRequest request)
	{
		final String authToken = request.getHeader(tokenHeader);
		final String token = authToken.substring(7);
		final String username = jwtTokenUtil.getUsernameFromToken(token);
		final JwtUser user = (JwtUser) userDetailsService.loadUserByUsername(username);
		final String refreshedToken = jwtTokenUtil.refreshToken(token);
		return ResponseEntity.ok(new JwtAuthenticationResponse(refreshedToken));
		/*
		 * if (jwtTokenUtil.canTokenBeRefreshed(token, user.getLastPasswordResetDate()))
		 * {
		 * String refreshedToken = jwtTokenUtil.refreshToken(token);
		 * return ResponseEntity.ok(new JwtAuthenticationResponse(refreshedToken));
		 * } else
		 * {
		 * return ResponseEntity.badRequest().body(null);
		 * }
		 */
	}

	@ExceptionHandler(
	{AuthenticationException.class})
	public ResponseEntity<String> handleAuthenticationException(final AuthenticationException e)
	{
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
	}

	/**
	 * Authenticates the user. If something is wrong, an {@link AuthenticationException} will be thrown
	 */
	private void authenticate(final String username, final String password)
	{
		Objects.requireNonNull(username);
		Objects.requireNonNull(password);
		log.debug("\t [JWT] username=[{}]", username);
		log.debug("\t [JWT] password=[{}]", password);
		try
		{
			authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
		} catch (final DisabledException e)
		{
			throw new AuthenticationException("User is disabled!", e);
		} catch (final BadCredentialsException e)
		{
			throw new AuthenticationException("Bad credentials!", e);
		}
	}
}
