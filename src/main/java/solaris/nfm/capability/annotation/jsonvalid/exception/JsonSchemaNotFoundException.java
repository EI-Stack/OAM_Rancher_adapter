package solaris.nfm.capability.annotation.jsonvalid.exception;

import solaris.nfm.exception.base.ExceptionBase;

public class JsonSchemaNotFoundException extends ExceptionBase
{
	private static final long serialVersionUID = 1L;

	public JsonSchemaNotFoundException()
	{
		super("JSON schema does not exist !!");
		this.setErrorCode(108);
	}

	public JsonSchemaNotFoundException(final String jsonSchemaFileName)
	{
		super("JSON schema (" + jsonSchemaFileName + ") does not exist !! ");
		this.setErrorCode(108);
	}
}
