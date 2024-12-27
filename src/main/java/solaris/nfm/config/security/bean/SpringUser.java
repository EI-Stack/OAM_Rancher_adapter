package solaris.nfm.config.security.bean;

import java.time.LocalDateTime;
import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;

import lombok.Getter;
import lombok.Setter;

public class SpringUser extends org.springframework.security.core.userdetails.User
{
	private static final long	serialVersionUID	= 1L;

	@Getter
	private Long				userId;
	@Getter
	private LocalDateTime		loginTime;
	@Getter
	private String				nickname;
	@Getter
	@Setter
	private Float				timeZone;

	public SpringUser(final String username, final String password, final Collection<? extends GrantedAuthority> authorities)
	{
		super(username, password, authorities);
		this.loginTime = LocalDateTime.now();
	}

	public SpringUser(final String username, final String password, final Collection<? extends GrantedAuthority> authorities, final Long userId, final String nickname)
	{
		super(username, password, authorities);
		this.userId = userId;
		this.loginTime = LocalDateTime.now();
		this.nickname = nickname;
	}

	public SpringUser(final String username, final String password, final boolean enabled, final boolean accountNonExpired, final boolean credentialsNonExpired, final boolean accountNonLocked,
			final Collection<? extends GrantedAuthority> authorities, final Long userId)
	{
		super(username, password, enabled, accountNonExpired, credentialsNonExpired, accountNonLocked, authorities);
		this.userId = userId;
		this.loginTime = LocalDateTime.now();
	}
}
