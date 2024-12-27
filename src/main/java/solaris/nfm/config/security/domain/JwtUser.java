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
	private final Long										id;           // 必须
	private final String									username;     // 必须
	private final String									password;     // 必须
	private final boolean									enabled;      // 必须 //表示當前这个使用者是否可以使用
	private final LocalDateTime								loginTime;
	// 授權的角色集合---不是用户的角色集合
	// 權限的類型要繼承 GrantedAuthority
	private final Collection<? extends GrantedAuthority>	authorities;       // 必须

	public JwtUser(final Long id, final String username, final String password, final boolean enabled, final LocalDateTime loginTime, final Collection<? extends GrantedAuthority> authorities)
	{
		this.id = id;
		this.username = username;
		this.password = password;
		this.enabled = enabled;
		this.loginTime = loginTime;
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
		return this.password;
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
}
