package solaris.nfm.exception;

import solaris.nfm.exception.base.ExceptionBase;

public class EntityIdInvalidException extends ExceptionBase
{
	private static final long serialVersionUID = 1L;

	public EntityIdInvalidException()
	{
		super("The entity id is invalid.");
		this.setErrorCode(400);
	}

	public EntityIdInvalidException(final String entityName, final Long id)
	{
		super("The " + ExceptionBase.toSimple(entityName) + " id (" + id + ") is invalid. The value of id must not be null, and should be large than 0.");
		this.setErrorCode(400);
	}

	public EntityIdInvalidException(final String message)
	{
		super(message);
		this.setErrorCode(400);
	}
}
