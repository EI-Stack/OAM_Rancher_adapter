package solaris.nfm.exception;

import solaris.nfm.exception.base.ExceptionBase;

public class RegularExpressionException extends ExceptionBase{
	
	private static final long serialVersionUID = 1L;
	
	public RegularExpressionException()
	{
		super("Do not pass Regular Expression");
		this.setErrorCode(400);
	}
	
	public RegularExpressionException(final String ObjectName)
	{
		super(ObjectName + " is illegal !! Must match ^[0-9a-z.-]*");
		this.setErrorCode(400);
	}

}
