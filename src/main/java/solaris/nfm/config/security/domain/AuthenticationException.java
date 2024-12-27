package solaris.nfm.config.security.domain;

public class AuthenticationException extends RuntimeException
{
	private static final long serialVersionUID = 1L;

	public AuthenticationException(final String message, final Throwable cause)
	{
		super(message, cause);
	}
}
