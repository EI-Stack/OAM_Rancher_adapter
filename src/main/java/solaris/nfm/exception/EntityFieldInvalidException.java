package solaris.nfm.exception;

import solaris.nfm.exception.base.ExceptionBase;

public class EntityFieldInvalidException extends ExceptionBase
{
	private static final long serialVersionUID = 1L;

	public EntityFieldInvalidException()
	{
		super("Entity field is invalid !!");
		this.setErrorCode(400);
	}

	public EntityFieldInvalidException(final String message)
	{
		super(message);
		this.setErrorCode(400);
	}

	public EntityFieldInvalidException(final String fieldName, final String message)
	{
		super("Entity field (" + fieldName + ") has existed !! " + message);
		this.setErrorCode(400);
	}
}
