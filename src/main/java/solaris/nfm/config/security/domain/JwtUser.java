package solaris.nfm.config.security.domain;

import java.time.LocalDateTime;
import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * spring security 框架服務的使用者類別
 */
public class JwtUser implements UserDetails
{
	private static final long								serialVersionUID	= 1L;
	private final Long										id;           // 必須
	private final String									username;     // 必須
	private final String									pwpwpwpw;     // 必須
	private final boolean									enabled;      // 必須 //表示當前這個使用者是否可以使用
	private final LocalDateTime								loginTime;
	private final String									userIp;
	// 授權的角色集合---不是用户的角色集合
	// 權限的類型要繼承 GrantedAuthority
	private final Collection<? extends GrantedAuthority>	authorities;       // 必須

	public JwtUser(final Long id, final String username, final String pwpwpwpw, final boolean enabled, final LocalDateTime loginTime, final String userIp,
			final Collection<? extends GrantedAuthority> authorities)
	{
		this.id = id;
		this.username = username;
		this.pwpwpwpw = pwpwpwpw;
		this.enabled = enabled;
		this.loginTime = loginTime;
		this.userIp = userIp;
		this.authorities = authorities;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities()
	{
		return this.authorities;
	}

	@JsonIgnore   // 将 JwtUser 序列化时，有些属性的值我们是不序列化出来的，所以可以加这个注解
	@Override
	public String getPassword()
	{
		return this.pwpwpwpw;
	}

	@Override
	@JsonIgnore
	public String getUsername()
	{
		return this.username;
	}

	@JsonIgnore
	@Override
	public boolean isAccountNonExpired()
	{
		return true;
	}

	@JsonIgnore
	@Override
	public boolean isAccountNonLocked()
	{
		return true;
	}

	@JsonIgnore
	@Override
	public boolean isCredentialsNonExpired()
	{
		return true;
	}

	@JsonIgnore
	@Override
	public boolean isEnabled()
	{
		return this.enabled;
	}

	public Long getId()
	{
		return id;
	}

	public LocalDateTime getLoginTime()
	{
		return loginTime;
	}

	public String getUserIp()
	{
		return userIp;
	}
}
