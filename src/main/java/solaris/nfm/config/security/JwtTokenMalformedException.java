package solaris.nfm.config.security;

import org.springframework.security.core.AuthenticationException;

public class JwtTokenMalformedException extends AuthenticationException
{
	private static final long serialVersionUID = 1L;

	public JwtTokenMalformedException(final String msg)
	{
		super(msg);
	}
}