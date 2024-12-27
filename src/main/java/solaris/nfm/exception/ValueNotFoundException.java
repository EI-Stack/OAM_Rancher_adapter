package solaris.nfm.exception;

import solaris.nfm.exception.base.ExceptionBase;

public class ValueNotFoundException extends ExceptionBase
{
	private static final long	serialVersionUID	= -1270958648601290677L;

	public ValueNotFoundException()
	{
		super("There is no value found !!");
		this.setErrorCode(108);
	}

	public ValueNotFoundException(final String message)
	{
		super(message);
		this.setErrorCode(108);
	}
}
