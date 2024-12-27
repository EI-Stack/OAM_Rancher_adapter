package solaris.nfm.config.security;

import org.springframework.security.core.AuthenticationException;

public class JwtTokenMissingException extends AuthenticationException
{
	private static final long serialVersionUID = 1L;

	public JwtTokenMissingException(final String msg)
	{
		super(msg);
	}
}