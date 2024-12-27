package solaris.nfm.exception;

import solaris.nfm.exception.base.ExceptionBase;

public class PasswordIsWrongException extends ExceptionBase
{
	private static final long	serialVersionUID	= -1270958648601290677L;

	public PasswordIsWrongException()
	{
		super("Password is wrong !!");
		this.setErrorCode(109);
	}

	public PasswordIsWrongException(final String message)
	{
		super(message);
		this.setErrorCode(109);
	}
}
