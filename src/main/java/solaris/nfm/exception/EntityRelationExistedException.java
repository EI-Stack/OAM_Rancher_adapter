package solaris.nfm.exception;

import solaris.nfm.exception.base.ExceptionBase;

public class EntityRelationExistedException extends ExceptionBase
{
	private static final long	serialVersionUID	= -7938544384870278294L;

	public EntityRelationExistedException()
	{
		super("There are relations existed !!");
		this.setErrorCode(122);
	}

	public EntityRelationExistedException(final String message)
	{
		super(message);
		this.setErrorCode(122);
	}
}
