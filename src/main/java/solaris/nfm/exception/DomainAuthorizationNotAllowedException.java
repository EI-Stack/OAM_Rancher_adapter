package solaris.nfm.exception;

import solaris.nfm.exception.base.ExceptionBase;

public class DomainAuthorizationNotAllowedException extends ExceptionBase
{
	private static final long	serialVersionUID	= -5717265450594411686L;

	public DomainAuthorizationNotAllowedException()
	{
		super("There is no domain authorization to access the entity !!");
		this.setErrorCode(105);
	}

	public DomainAuthorizationNotAllowedException(final String message)
	{
		super("There is no domain authorization to access the entity !! " + message);
		this.setErrorCode(105);
	}

	public DomainAuthorizationNotAllowedException(final String entityClassName, final String message)
	{
		super("There is no domain authorization to access the entity(" + entityClassName + ") !! " + message);
		this.setErrorCode(105);
	}
}
