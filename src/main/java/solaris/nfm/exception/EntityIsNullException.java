package solaris.nfm.exception;

import solaris.nfm.exception.base.ExceptionBase;

public class EntityIsNullException extends ExceptionBase
{
	private static final long serialVersionUID = 1L;

	public EntityIsNullException()
	{
		super("The entity/bean must not be null.");
		this.setErrorCode(400);
	}

	public EntityIsNullException(final String className)
	{
		super("The entity/bean (" + className + ") must not be null.");
		this.setErrorCode(400);
	}
}
