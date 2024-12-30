package solaris.nfm.capability.annotation.jsonvalid.exception;

import solaris.nfm.exception.base.ExceptionBase;

public class JsonSchemaValidationException extends ExceptionBase
{
	private static final long serialVersionUID = 1L;

	public JsonSchemaValidationException()
	{
		super("JSON data is invalid !!");
		this.setErrorCode(108);
	}

	public JsonSchemaValidationException(final String message)
	{
		super("JSON data is invalid !! " + message);
		this.setErrorCode(108);
	}
}
