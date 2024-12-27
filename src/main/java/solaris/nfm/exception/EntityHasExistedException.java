package solaris.nfm.exception;

import solaris.nfm.exception.base.ExceptionBase;

public class EntityHasExistedException extends ExceptionBase
{
	private static final long serialVersionUID = 1L;

	public EntityHasExistedException()
	{
		super("Entity has existed !!");
		this.setErrorCode(400);
	}

	public EntityHasExistedException(final String message)
	{
		super("Entity has existed !! " + message);
		this.setErrorCode(400);
	}

	public EntityHasExistedException(final String entityClassName, final String message)
	{
		super("Entity (" + entityClassName + ") has existed !! " + message);
		this.setErrorCode(400);
	}
}
