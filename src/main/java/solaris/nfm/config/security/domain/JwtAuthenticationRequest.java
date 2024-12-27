package solaris.nfm.config.security.domain;

import java.io.Serializable;

public class JwtAuthenticationRequest implements Serializable
{
	private static final long	serialVersionUID	= 1L;
	private String				username;
	private String				password;

	public JwtAuthenticationRequest()
	{
		super();
	}

	public JwtAuthenticationRequest(final String username, final String password)
	{
		this.setUsername(username);
		this.setPassword(password);
	}

	public String getUsername()
	{
		return username;
	}

	public void setUsername(final String username)
	{
		this.username = username;
	}

	public String getPassword()
	{
		return password;
	}

	public void setPassword(final String password)
	{
		this.password = password;
	}
}
