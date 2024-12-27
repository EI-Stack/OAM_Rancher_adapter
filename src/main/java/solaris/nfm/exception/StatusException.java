package solaris.nfm.exception;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.EqualsAndHashCode;
import solaris.nfm.exception.base.ExceptionBase;

@EqualsAndHashCode(callSuper = true)
public class StatusException extends ExceptionBase
{
	private static final long serialVersionUID = 1L;

	public StatusException(final Integer httpStatus, final String message)
	{
		super(httpStatus, message);
	}

	public StatusException(final Integer httpStatus, final JsonNode jsonNode)
	{
		super(httpStatus, jsonNode.toPrettyString());
	}
}