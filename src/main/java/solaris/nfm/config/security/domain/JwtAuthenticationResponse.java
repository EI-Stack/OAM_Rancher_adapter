package solaris.nfm.config.security.domain;

import java.io.Serializable;

public class JwtAuthenticationResponse implements Serializable
{
	private static final long	serialVersionUID	= 1L;
	private final String		token;   // 要發送回客户端的 Token

	public JwtAuthenticationResponse(final String token)
	{
		this.token = token;
	}

	public String getToken()
	{
		return this.token;
	}
}
