package solaris.nfm.exception;

import solaris.nfm.exception.base.ExceptionBase;

public class DeleteNotAllowedException extends ExceptionBase
{
	private static final long	serialVersionUID	= -7938544384870278294L;

	public DeleteNotAllowedException()
	{
		super("System reserved data can not be deleted !!");
		this.setErrorCode(121);
	}

	public DeleteNotAllowedException(final String message)
	{
		super(message);
		this.setErrorCode(121);
	}
}
